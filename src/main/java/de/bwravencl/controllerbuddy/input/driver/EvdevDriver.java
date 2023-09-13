/* Copyright (C) 2022  Matteo Hausner
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.bwravencl.controllerbuddy.input.driver;

import com.sun.jna.NativeLong;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.Union;
import com.sun.jna.ptr.ShortByReference;
import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.gui.Main.ControllerInfo;
import de.bwravencl.controllerbuddy.input.Input;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import uk.co.bithatch.linuxio.CLib;
import uk.co.bithatch.linuxio.EventCode;
import uk.co.bithatch.linuxio.Input.Macros;
import uk.co.bithatch.linuxio.Input.input_id;
import uk.co.bithatch.linuxio.Ioctl;

public class EvdevDriver extends Driver {

    private static final Logger log = Logger.getLogger(EvdevDriver.class.getName());
    private static final int FF_MAX = 0x7F;
    private static final short FF_RUMBLE = (short) 0x50;
    private static final short FF_PERIODIC = (short) 0x51;
    private static final short FF_CONSTANT = (short) 0x52;
    private static final short FF_SPRING = (short) 0x53;
    private static final short FF_FRICTION = (short) 0x54;
    private static final short FF_DAMPER = (short) 0x55;
    private static final short FF_INERTIA = (short) 0x56;
    private static final short FF_RAMP = (short) 0x57;
    private static final short FF_GAIN = (short) 0x60;
    private final int EVIOCSFF = Ioctl.INSTANCE.IOW("E", 0x80, new ff_effect());
    private final short strongRumbleEffectId;
    private final short lightRumbleEffectId;
    private volatile EvdevInfo evdevInfo;

    private EvdevDriver(final Input input, final ControllerInfo controller, final EvdevInfo evdevInfo)
            throws IOException {
        super(input, controller);

        this.evdevInfo = evdevInfo;

        strongRumbleEffectId = createRumbleEffect((short) 90);
        lightRumbleEffectId = createRumbleEffect((short) 60);

        log.log(Level.INFO, Main.assembleControllerLoggingMessage("Using evdev driver for controller", controller));
    }

    private static void closeFileDescriptor(final int fd) {
        if (fd != -1) {
            CLib.INSTANCE.close(fd);
        }
    }

    private short createRumbleEffect(final short length) throws IOException {
        final var effect = new ff_effect();
        effect.type = FF_RUMBLE;
        effect.id = -1;
        effect.u.setType(effect.u.rumble.getClass());
        effect.u.rumble.strong_magnitude = Short.MAX_VALUE;
        effect.replay.length = length;

        if (CLib.INSTANCE.ioctl(evdevInfo.fd, EVIOCSFF, effect) != 0 || effect.id == -1) {
            throw new IOException();
        }

        return effect.id;
    }

    @Override
    public void deInit(final boolean disconnected) {
        super.deInit(disconnected);

        try {
            stopEffect(strongRumbleEffectId);
            stopEffect(lightRumbleEffectId);
        } catch (final IOException e) {
            log.log(Level.WARNING, e.getMessage(), e);
        } finally {
            closeFileDescriptor(evdevInfo.fd);
            evdevInfo = null;
        }
    }

    private void playEffect(final short effectId) throws IOException {
        stopEffect(effectId);
        sendEffectEvent(effectId, 1);
    }

    @Override
    public void rumbleLight() {
        try {
            setGain(0x3000);
            playEffect(lightRumbleEffectId);
        } catch (final IOException e) {
            log.log(Level.WARNING, e.getMessage(), e);
        }
    }

    @Override
    public void rumbleStrong() {
        try {
            setGain(0xFFFF);
            playEffect(strongRumbleEffectId);
        } catch (final IOException e) {
            log.log(Level.WARNING, e.getMessage(), e);
        }
    }

    private void sendEffectEvent(final short effectId, final int value) throws IOException {
        if (evdevInfo == null) {
            return;
        }

        final var playEvent = new uk.co.bithatch.linuxio.Input.input_event();
        playEvent.code = effectId;
        playEvent.type = (short) EventCode.Type.EV_FF.code();
        playEvent.value = value;
        playEvent.time.tv_sec = new NativeLong(0);
        playEvent.time.tv_usec = new NativeLong(0);

        write(playEvent);
    }

    private void setGain(final int gain) throws IOException {
        final var gainEvent = new uk.co.bithatch.linuxio.Input.input_event();
        gainEvent.code = FF_GAIN;
        gainEvent.type = (short) EventCode.Type.EV_FF.code();
        gainEvent.value = gain;
        gainEvent.time.tv_sec = new NativeLong(0);
        gainEvent.time.tv_usec = new NativeLong(0);

        write(gainEvent);
    }

    private void stopEffect(final short effectId) throws IOException {
        sendEffectEvent(effectId, 0);
    }

    private void write(final Structure pointer) throws IOException {
        if (evdevInfo == null) {
            return;
        }

        final var pointerSize = new NativeLong(pointer.size());

        if (CLib.INSTANCE.write(evdevInfo.fd, pointer, pointerSize).longValue() != pointerSize.longValue()) {
            throw new IOException();
        }
    }

    public static class EvdevDriverBuilder implements IDriverBuilder {

        private static final int BITS_PER_LONG = NativeLong.SIZE * Byte.SIZE;

        private static int EVIOCGID;

        static {
            if (Main.isLinux) {
                EVIOCGID = Ioctl.INSTANCE.IOR('E', 0x02, new input_id());
            }
        }

        private static boolean testBit(final short bit, final NativeLong[] array) {
            return (array[bit / BITS_PER_LONG].longValue() >> bit % BITS_PER_LONG & 1) != 0;
        }

        @Override
        public Driver getIfAvailable(
                final Input input,
                final List<ControllerInfo> presentControllers,
                final ControllerInfo selectedController) {
            if (Main.isLinux && input.isHapticFeedbackEnabled()) {
                final var inputDir = new File("/dev/input/");
                final var allEventFiles =
                        inputDir.listFiles((final var dir, final var name) -> name.matches("event(\\d+)"));

                if (allEventFiles == null) {
                    return null;
                }

                final var evdevInfos = Arrays.stream(allEventFiles)
                        .flatMap(eventFile -> {
                            final var fd =
                                    CLib.INSTANCE.open(eventFile.getAbsolutePath(), CLib.O_RDWR | CLib.O_NONBLOCK);
                            try {
                                if (fd != -1) {
                                    final var inputId = new input_id();
                                    if (CLib.INSTANCE.ioctl(fd, EVIOCGID, inputId) == 0) {
                                        final var bustypeUnsigned = Short.toUnsignedInt(inputId.bustype);
                                        final var vendorUnsigned = Short.toUnsignedInt(inputId.vendor);
                                        final var productUnsigned = Short.toUnsignedInt(inputId.product);
                                        final var versionUnsigned = Short.toUnsignedInt(inputId.version);

                                        String guid = null;
                                        if (vendorUnsigned != 0 && productUnsigned != 0 && versionUnsigned != 0) {
                                            guid = "%02x%02x0000%02x%02x0000%02x%02x0000%02x%02x0000"
                                                    .formatted(
                                                            bustypeUnsigned & 0xff,
                                                            bustypeUnsigned >> 8,
                                                            vendorUnsigned & 0xff,
                                                            vendorUnsigned >> 8,
                                                            productUnsigned & 0xff,
                                                            productUnsigned >> 8,
                                                            versionUnsigned & 0xff,
                                                            versionUnsigned >> 8);
                                        } else {
                                            final var nameBytes = new byte[256];
                                            if (CLib.INSTANCE.ioctl(
                                                            fd,
                                                            uk.co.bithatch.linuxio.Input.Macros.EVIOCGNAME(
                                                                    nameBytes.length),
                                                            nameBytes)
                                                    != 0) {
                                                guid = "%02x%02x0000%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x00"
                                                        .formatted(
                                                                bustypeUnsigned & 0xff,
                                                                bustypeUnsigned >> 8,
                                                                nameBytes[0],
                                                                nameBytes[1],
                                                                nameBytes[2],
                                                                nameBytes[3],
                                                                nameBytes[4],
                                                                nameBytes[5],
                                                                nameBytes[6],
                                                                nameBytes[7],
                                                                nameBytes[8],
                                                                nameBytes[9],
                                                                nameBytes[10]);
                                            }
                                        }

                                        if (selectedController.guid().equals(guid)) {
                                            final var ffFeatures = new NativeLong[FF_MAX];
                                            if (CLib.INSTANCE.ioctl(
                                                            fd,
                                                            Macros.EVIOCGBIT(
                                                                    EventCode.Type.EV_FF.code(),
                                                                    ffFeatures.length * Character.BYTES),
                                                            ffFeatures)
                                                    != -1) {
                                                if (testBit(FF_RUMBLE, ffFeatures)) {
                                                    return Stream.of(new EvdevInfo(fd, testBit(FF_GAIN, ffFeatures)));
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (final Throwable t) {
                                log.log(Level.WARNING, t.getMessage(), t);
                            }

                            closeFileDescriptor(fd);
                            return Stream.empty();
                        })
                        .toList();

                if (evdevInfos.isEmpty()) {
                    return null;
                }

                if (evdevInfos.size() > 1) {
                    log.log(
                            Level.WARNING,
                            "Found more than one controller with GUID '" + selectedController.guid()
                                    + "' - evdev driver disabled");
                }

                try {
                    return new EvdevDriver(input, selectedController, evdevInfos.get(0));
                } catch (final IOException e) {
                    log.log(Level.SEVERE, e.getMessage(), e);
                }
            }

            return null;
        }

        @Override
        public int getOrder() {
            return 1;
        }
    }

    @SuppressWarnings("unused")
    private record EvdevInfo(int fd, boolean hasGain) {}

    @SuppressWarnings("unused")
    @FieldOrder({"right_saturation", "left_saturation", "right_coeff", "left_coeff", "deadband", "center"})
    public static class ff_condition_effect extends Structure {

        public short right_saturation;
        public short left_saturation;
        public short right_coeff;
        public short left_coeff;
        public short deadband;
        public short center;
    }

    @SuppressWarnings("unused")
    @FieldOrder({"level", "envelope"})
    public static class ff_constant_effect extends Structure {

        public short level;
        public ff_envelope envelope;
    }

    @SuppressWarnings("unused")
    @FieldOrder({"type", "id", "direction", "trigger", "replay", "u"})
    public static class ff_effect extends Structure {

        public short type;
        public short id;
        public short direction;
        public ff_trigger trigger;
        public ff_replay replay;
        public U u;

        @Override
        public void read() {
            super.read();

            switch (type) {
                case FF_RUMBLE -> u.setType(u.rumble.getClass());
                case FF_PERIODIC -> u.setType(u.periodic.getClass());
                case FF_CONSTANT -> u.setType(u.constant.getClass());
                case FF_SPRING, FF_FRICTION, FF_DAMPER, FF_INERTIA -> u.setType(u.condition.getClass());
                case FF_RAMP -> u.setType(u.ramp.getClass());
                default -> throw new IllegalStateException("Unknown type " + type);
            }

            u.read();
        }

        public static class U extends Union {

            public final ff_condition_effect[] condition = new ff_condition_effect[2];
            public ff_constant_effect constant;
            public ff_ramp_effect ramp;
            public ff_periodic_effect periodic;
            public ff_rumble_effect rumble;

            public U() {}
        }
    }

    @SuppressWarnings("unused")
    @FieldOrder({"attack_length", "attack_level", "fade_length", "fade_level"})
    public static class ff_envelope extends Structure {

        public short attack_length;
        public short attack_level;
        public short fade_length;
        public short fade_level;
    }

    @SuppressWarnings("unused")
    @FieldOrder({"waveform", "period", "magnitude", "offset", "phase", "envelope", "custom_len", "custom_data"})
    public static class ff_periodic_effect extends Structure {

        public short waveform;
        public short period;
        public short magnitude;
        public short offset;
        public short phase;
        public ff_envelope envelope;
        public int custom_len;
        public ShortByReference custom_data;
    }

    @SuppressWarnings("unused")
    @FieldOrder({"start_level", "end_level", "envelope"})
    public static class ff_ramp_effect extends Structure {

        public short start_level;
        public short end_level;
        public ff_envelope envelope;
    }

    @SuppressWarnings("unused")
    @FieldOrder({"length", "delay"})
    public static class ff_replay extends Structure {

        public short length;
        public short delay;
    }

    @SuppressWarnings("unused")
    @FieldOrder({"strong_magnitude", "weak_magnitude"})
    public static class ff_rumble_effect extends Structure {

        public short strong_magnitude;
        public short weak_magnitude;
    }

    @SuppressWarnings("unused")
    @FieldOrder({"button", "interval"})
    public static class ff_trigger extends Structure {

        public short button;
        public short interval;
    }
}
