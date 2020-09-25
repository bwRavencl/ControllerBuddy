/* Copyright (C) 2020  Matteo Hausner
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package de.bwravencl.controllerbuddy.input;

import static com.sun.jna.platform.win32.Ole32.COINIT_APARTMENTTHREADED;
import static com.sun.jna.platform.win32.WTypes.CLSCTX_INPROC_SERVER;
import static com.sun.jna.platform.win32.WinError.S_FALSE;
import static com.sun.jna.platform.win32.WinError.S_OK;
import static de.bwravencl.controllerbuddy.gui.GuiUtils.showMessageDialog;
import static de.bwravencl.controllerbuddy.gui.Main.isWindows;
import static de.bwravencl.controllerbuddy.gui.Main.strings;
import static de.bwravencl.controllerbuddy.input.Input.normalize;
import static java.lang.Math.abs;
import static java.lang.Math.min;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toUnmodifiableList;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.WARNING_MESSAGE;
import static org.lwjgl.glfw.GLFW.glfwGetJoystickGUID;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.COM.Unknown;
import com.sun.jna.platform.win32.Guid.CLSID;
import com.sun.jna.platform.win32.Guid.GUID;
import com.sun.jna.platform.win32.Guid.IID;
import com.sun.jna.platform.win32.Ole32;
import com.sun.jna.platform.win32.Variant;
import com.sun.jna.platform.win32.WinDef.BOOLByReference;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.DWORDByReference;
import com.sun.jna.platform.win32.WinDef.UINT;
import com.sun.jna.platform.win32.WinDef.UINTByReference;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.ptr.FloatByReference;
import com.sun.jna.ptr.PointerByReference;

import purejavahidapi.HidDevice;
import purejavahidapi.HidDeviceInfo;
import purejavahidapi.InputReportListener;
import purejavahidapi.PureJavaHidApi;

public final class DualShock4Extension {

	private static final class IAudioEndpointVolume extends Unknown {

		private IAudioEndpointVolume(final Pointer pvInstance) {
			super(pvInstance);
		}

		private HRESULT GetMasterVolumeLevel(final FloatByReference pfLevel) {
			return (HRESULT) _invokeNativeObject(8, new Object[] { getPointer(), pfLevel }, HRESULT.class);
		}

		private HRESULT GetMute(final BOOLByReference pbMute) {
			return (HRESULT) _invokeNativeObject(15, new Object[] { getPointer(), pbMute }, HRESULT.class);
		}

		private HRESULT GetVolumeRange(final FloatByReference pflVolumeMindB, final FloatByReference pflVolumeMaxdB,
				final FloatByReference pflVolumeIncrementdB) {
			return (HRESULT) _invokeNativeObject(20,
					new Object[] { getPointer(), pflVolumeMindB, pflVolumeMaxdB, pflVolumeIncrementdB }, HRESULT.class);
		}
	}

	private static final class IMMDevice extends Unknown {

		private IMMDevice(final Pointer pvInstance) {
			super(pvInstance);
		}

		private HRESULT Activate(final IID iid, final int dwClsCtx, final Pointer pActivationParams,
				final PointerByReference ppInterface) {
			return (HRESULT) _invokeNativeObject(3,
					new Object[] { getPointer(), iid, dwClsCtx, pActivationParams, ppInterface }, HRESULT.class);
		}

		private HRESULT GetId(final PointerByReference ppstrId) {
			return (HRESULT) _invokeNativeObject(5, new Object[] { getPointer(), ppstrId }, HRESULT.class);
		}

		private HRESULT GetState(final DWORDByReference pdwState) {
			return (HRESULT) _invokeNativeObject(6, new Object[] { getPointer(), pdwState }, HRESULT.class);
		}

		private HRESULT OpenPropertyStore(final int stgmAccess, final PointerByReference ppProperties) {
			return (HRESULT) _invokeNativeObject(4, new Object[] { getPointer(), stgmAccess, ppProperties },
					HRESULT.class);
		}

		@Override
		public String toString() {
			final var ppstrId = new PointerByReference();

			if (!S_OK.equals(GetId(ppstrId))) {
				log.log(SEVERE, "IMMDevice::GetId failed");
				return super.toString();
			}

			final var pstrId = ppstrId.getValue();
			try {
				return pstrId.getWideString(0);
			} finally {
				Ole32.INSTANCE.CoTaskMemFree(pstrId);
			}
		}
	}

	private static final class IMMDeviceCollection extends Unknown {

		private IMMDeviceCollection(final Pointer pvInstance) {
			super(pvInstance);
		}

		private HRESULT GetCount(final UINTByReference pcDevices) {
			return (HRESULT) _invokeNativeObject(3, new Object[] { getPointer(), pcDevices }, HRESULT.class);
		}

		private HRESULT Item(final UINT nDevice, final PointerByReference ppDevice) {
			return (HRESULT) _invokeNativeObject(4, new Object[] { getPointer(), nDevice, ppDevice }, HRESULT.class);
		}
	}

	private static final class IMMDeviceEnumerator extends Unknown {

		private IMMDeviceEnumerator(final Pointer pvInstance) {
			super(pvInstance);
		}

		private HRESULT EnumAudioEndpoints(final int dataFlow, final int dwStateMask,
				final PointerByReference ppDevices) {
			return (HRESULT) _invokeNativeObject(3, new Object[] { getPointer(), dataFlow, dwStateMask, ppDevices },
					HRESULT.class);
		}
	}

	private static final class IPropertyStore extends Unknown {

		private IPropertyStore(final Pointer pvInstance) {
			super(pvInstance);
		}

		private HRESULT GetValue(final PROPERTYKEY.ByReference key, final PROPVARIANT.ByReference pv) {
			return (HRESULT) _invokeNativeObject(5, new Object[] { getPointer(), key, pv }, HRESULT.class);
		}
	}

	public static class PROPERTYKEY extends Structure {

		private static class ByReference extends PROPERTYKEY implements Structure.ByReference {
		}

		public GUID fmtid;

		public DWORD pid;

		@Override
		protected List<String> getFieldOrder() {
			return Arrays.asList("fmtid", "pid");
		}
	}

	public static class PROPVARIANT extends Structure {

		private static class ByReference extends PROPVARIANT implements Structure.ByReference {
		}

		public int vt;
		public byte r1;
		public byte r2;
		public byte r3;

		public WString pwszVal;

		@Override
		protected List<String> getFieldOrder() {
			return Arrays.asList("vt", "r1", "r2", "r3", "pwszVal");
		}
	}

	private static final Logger log = Logger.getLogger(DualShock4Extension.class.getName());

	private static final int eRender = 0;
	private static final int eCapture = 1;

	private static final int DEVICE_STATE_ACTIVE = 0x1;
	private static final int DEVICE_STATE_DISABLED = 0x2;

	private static final int STGM_READ = 0x0;

	private static final CLSID MMDeviceEnumeratorCLSID = new CLSID("BCDE0395-E52F-467C-8E3D-C4579291692E");
	private static final IID IMMDeviceEnumeratorIID = new IID("A95664D2-9614-4F35-A746-DE8DB63617E6");
	private static final IID IAudioEndpointVolumeIID = new IID("5CDF2C82-841E-4546-9722-0CF74078229A");

	private static final PROPERTYKEY.ByReference PKEY_Device_FriendlyName;

	static {
		PKEY_Device_FriendlyName = new PROPERTYKEY.ByReference();
		PKEY_Device_FriendlyName.fmtid.Data1 = 0xA45C254E;
		PKEY_Device_FriendlyName.fmtid.Data2 = (short) 0xDF1C;
		PKEY_Device_FriendlyName.fmtid.Data3 = 0x4EFD;
		PKEY_Device_FriendlyName.fmtid.Data4 = new byte[] { (byte) 0x80, 0x20, 0x67, (byte) 0xD1, 0x46, (byte) 0xA8,
				0x50, (byte) 0xE0 };
		PKEY_Device_FriendlyName.pid = new DWORD(14);
	}

	private static final int LOW_BATTERY_WARNING = 20;

	private static final String FRIENDLY_NAME = "DUALSHOCK\u00AE4 USB Wireless Adaptor";

	private static final byte[] DEFAULT_HID_REPORT = new byte[] { (byte) 0x05, (byte) 0xFF, 0x0, 0x0, 0x0, 0x0,
			(byte) 0x0C, (byte) 0x18, (byte) 0x1C, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, (byte) 0x50,
			(byte) 0x50, (byte) 0x40, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0 };

	private static final int hidReportOffset = isWindows ? 1 : 0;

	private static IMMDevice getFirstMatchingIMMDevice(final IMMDeviceEnumerator deviceEnumerator, final int dataFlow)
			throws Exception {
		final var ppDevices = new PointerByReference();
		if (!S_OK.equals(
				deviceEnumerator.EnumAudioEndpoints(dataFlow, DEVICE_STATE_ACTIVE | DEVICE_STATE_DISABLED, ppDevices)))
			throw new Exception("IMMDeviceEnumerator::EnumAudioEndpoints failed");

		final var deviceCollection = new IMMDeviceCollection(ppDevices.getValue());
		try {
			final var pcDevices = new UINTByReference();
			if (!S_OK.equals(deviceCollection.GetCount(pcDevices)))
				throw new Exception("IMMDeviceCollection::GetCount failed");

			final var cDevices = pcDevices.getValue();

			for (var i = 0; i < cDevices.intValue(); i++) {
				final var ppDevice = new PointerByReference();
				if (!S_OK.equals(deviceCollection.Item(new UINT(i), ppDevice))) {
					log.log(SEVERE, "IMMDeviceCollection::Item failed");
					continue;
				}

				final var device = new IMMDevice(ppDevice.getValue());
				try {
					final var ppProperties = new PointerByReference();
					if (!S_OK.equals(device.OpenPropertyStore(STGM_READ, ppProperties))) {
						log.log(SEVERE, "IMMDevice::OpenPropertyStore failed");
						continue;
					}

					final var propertyStore = new IPropertyStore(ppProperties.getValue());
					try {

						final var pv = new PROPVARIANT.ByReference();
						if (!S_OK.equals(propertyStore.GetValue(PKEY_Device_FriendlyName, pv))) {
							log.log(SEVERE, "IPropertyStore::GetValue failed");
							continue;
						}

						if (pv.vt == Variant.VT_EMPTY) {
							log.log(SEVERE, "PROPERTYKEY not present");
							continue;
						}

						if (pv.vt != Variant.VT_LPWSTR) {
							log.log(SEVERE, "PROPERTYKEY has wrong variant");
							continue;
						}

						final var currentFriendlyName = pv.pwszVal.toString();
						if (currentFriendlyName.contains(FRIENDLY_NAME))
							return device;
					} finally {
						propertyStore.Release();
					}

					device.Release();
				} catch (final Throwable t) {
					device.Release();
					throw t;
				}
			}

			return null;
		} finally {
			deviceCollection.Release();
		}
	}

	private static byte getNormalizedVolumeValue(final IMMDevice device, final int maxOutputValueIndex)
			throws Exception {
		final var ppAudioEndpointVolume = new PointerByReference();

		final var pdwState = new DWORDByReference();
		if (!S_OK.equals(device.GetState(pdwState)))
			throw new Exception("IMMDevice::GetState failed");

		if (pdwState.getValue().intValue() != DEVICE_STATE_ACTIVE)
			throw new Exception("Device is not active");

		if (!S_OK.equals(device.Activate(IAudioEndpointVolumeIID, CLSCTX_INPROC_SERVER, null, ppAudioEndpointVolume)))
			throw new Exception("IMMDevice::Activate failed");

		final var audioEndpointVolume = new IAudioEndpointVolume(ppAudioEndpointVolume.getValue());

		final var pbMute = new BOOLByReference();
		if (!S_OK.equals(audioEndpointVolume.GetMute(pbMute)))
			throw new Exception("IAudioEndpointVolume::GetMute failed");

		if (pbMute.getValue().booleanValue())
			return 0x0;

		final var pflVolumeMindB = new FloatByReference();
		final var pflVolumeMaxdB = new FloatByReference();
		if (!S_OK.equals(audioEndpointVolume.GetVolumeRange(pflVolumeMindB, pflVolumeMaxdB, new FloatByReference())))
			throw new Exception("IAudioEndpointVolume::GetVolumeRange failed");

		final var pfLevel = new FloatByReference();
		if (!S_OK.equals(audioEndpointVolume.GetMasterVolumeLevel(pfLevel)))
			throw new Exception("IAudioEndpointVolume::GetMasterVolumeLevel failed");

		final var flVolumeMindB = pflVolumeMindB.getValue();
		final var fLevel = pfLevel.getValue();

		final var alignedMaxOutputValue = DEFAULT_HID_REPORT[maxOutputValueIndex] + 0.01f;

		return (byte) Math
				.round(normalize(fLevel, flVolumeMindB, pflVolumeMaxdB.getValue(), 1.0f, alignedMaxOutputValue));
	}

	static DualShock4Extension handleDualShock4(final Input input, final int jid) {
		final var guid = glfwGetJoystickGUID(jid);
		if (guid == null)
			return null;

		final short productId;
		var isDongle = false;
		if (guid.startsWith("030000004c050000c405"))
			productId = 0x5C4;
		else if (guid.startsWith("030000004c050000cc09"))
			productId = 0x9CC;
		else if (guid.startsWith("030000004c050000a00b")) {
			productId = 0xBA0;
			isDongle = true;
		} else
			return null;

		final var dualShock4Devices = PureJavaHidApi.enumerateDevices().stream()
				.filter(hidDeviceInfo -> hidDeviceInfo.getVendorId() == (short) 0x54C
						&& hidDeviceInfo.getProductId() == productId)
				.collect(toUnmodifiableList());
		final var count = dualShock4Devices.size();

		if (count < 1)
			return null;

		log.log(INFO, "Found " + count + " DualShock 4 controller(s): "
				+ dualShock4Devices.stream().map(HidDeviceInfo::getDeviceId).collect(joining(", ")));

		if (count > 1)
			showMessageDialog(input.getMain().getFrame(),
					strings.getString("MULTIPLE_DUAL_SHOCK_4_CONTROLLERS_CONNECTED_DIALOG_TEXT"),
					strings.getString("WARNING_DIALOG_TITLE"), WARNING_MESSAGE);

		final var hidDeviceInfo = dualShock4Devices.get(0);
		log.log(INFO, "Using DualShock 4 controller " + hidDeviceInfo.getDeviceId());

		try {
			return new DualShock4Extension(input, hidDeviceInfo, isDongle);
		} catch (final IOException e) {
			log.log(SEVERE, e.getMessage(), e);
		}

		return null;
	}

	private final Input input;
	private HidDevice hidDevice;
	private byte[] hidReport;
	private volatile boolean charging = true;
	private volatile int batteryState;
	private IMMDevice earphoneDevice;
	private IMMDevice microphoneDevice;
	private boolean comLibraryInitialized;

	private DualShock4Extension(final Input input, final HidDeviceInfo hidDeviceInfo, final boolean isDongle)
			throws IOException {
		this.input = input;

		try {
			if (isWindows && isDongle) {
				final var coInitializeExResult = Ole32.INSTANCE.CoInitializeEx(null, COINIT_APARTMENTTHREADED);

				if (S_OK.equals(coInitializeExResult))
					comLibraryInitialized = true;
				else if (S_FALSE.equals(coInitializeExResult)) {
					comLibraryInitialized = true;
					log.log(WARNING, "COM library was already initialized");
				} else {
					comLibraryInitialized = false;
					log.log(SEVERE, "CoInitializeEx failed");
				}

				if (comLibraryInitialized) {
					final var ppDeviceEnumerator = new PointerByReference();
					if (!S_OK.equals(Ole32.INSTANCE.CoCreateInstance(MMDeviceEnumeratorCLSID, null,
							CLSCTX_INPROC_SERVER, IMMDeviceEnumeratorIID, ppDeviceEnumerator)))
						log.log(SEVERE, "CoCreateInstance failed");
					else {
						final var deviceEnumerator = new IMMDeviceEnumerator(ppDeviceEnumerator.getValue());
						try {
							try {
								earphoneDevice = getFirstMatchingIMMDevice(deviceEnumerator, eRender);
								if (earphoneDevice != null)
									log.log(INFO, "Using DualShock 4 earphone device " + earphoneDevice);
								else
									log.log(WARNING, "DualShock 4 earphone not device found");
							} catch (final Exception e) {
								log.log(SEVERE, e.getMessage(), e);
							}

							microphoneDevice = getFirstMatchingIMMDevice(deviceEnumerator, eCapture);
							if (microphoneDevice != null)
								log.log(INFO, "Using DualShock 4 microphone device " + microphoneDevice);
							else
								log.log(WARNING, "DualShock 4 microphone not device found");
						} catch (final Exception e) {
							log.log(SEVERE, e.getMessage(), e);
						} finally {
							deviceEnumerator.Release();

							if (earphoneDevice == null && microphoneDevice == null) {
								Ole32.INSTANCE.CoUninitialize();
								comLibraryInitialized = false;
							}
						}
					}

					if (earphoneDevice == null || microphoneDevice == null)
						showMessageDialog(input.getMain().getFrame(),
								strings.getString("COULD_NOT_FIND_DUAL_SHOCK_4_AUDIO_DEVICE_DIALOG_TEXT"),
								strings.getString("WARNING_DIALOG_TITLE"), ERROR_MESSAGE);
				} else
					showMessageDialog(input.getMain().getFrame(),
							strings.getString("COULD_NOT_INITIALIZE_COM_LIBRARY_DIALOG_TEXT"),
							strings.getString("WARNING_DIALOG_TITLE"), ERROR_MESSAGE);
			}

			hidDevice = PureJavaHidApi.openDevice(hidDeviceInfo);
			reset();

			hidDevice.setInputReportListener(new InputReportListener() {

				private static final int TOUCHPAD_MAX_DELTA = 150;
				private static final float TOUCHPAD_CURSOR_SENSITIVITY = 1.25f;
				private static final float TOUCHPAD_SCROLL_SENSITIVITY = 0.25f;

				private boolean prevTouchpadButtonDown;
				private boolean prevDown1;
				private boolean prevDown2;
				private int prevX1;
				private int prevY1;

				@Override
				public void onInputReport(final HidDevice source, final byte reportID, final byte[] reportData,
						final int reportLength) {
					if (reportID != 0x01 || reportData.length != 64) {
						log.log(WARNING, "Received unknown HID input report with ID " + reportID + " and length "
								+ reportLength);
						return;
					}

					final var cableConnected = (reportData[30] >> 4 & 0x01) != 0;
					var battery = reportData[30] & 0x0F;

					setCharging(cableConnected);

					if (!cableConnected)
						battery++;

					battery = min(battery, 10);
					battery *= 10;

					setBatteryState(battery);

					final var main = input.getMain();
					if (!main.isLocalThreadActive() && !main.isServerThreadActive())
						return;

					final var touchpadButtonDown = (reportData[7] & 1 << 2 - 1) != 0;
					final var down1 = reportData[35] >> 7 != 0 ? false : true;
					final var down2 = reportData[39] >> 7 != 0 ? false : true;
					final var x1 = reportData[36] + (reportData[37] & 0xF) * 255;
					final var y1 = ((reportData[37] & 0xF0) >> 4) + reportData[38] * 16;

					final var downMouseButtons = input.getDownMouseButtons();
					if (touchpadButtonDown)
						synchronized (downMouseButtons) {
							downMouseButtons.add(down2 ? 2 : 1);
						}
					else if (prevTouchpadButtonDown)
						synchronized (downMouseButtons) {
							downMouseButtons.clear();
						}

					if (down1 && prevDown1) {
						final var dX1 = x1 - prevX1;
						final var dY1 = y1 - prevY1;

						if (!prevDown2 || touchpadButtonDown) {
							if (prevX1 > 0 && abs(dX1) < TOUCHPAD_MAX_DELTA)
								input.setCursorDeltaX((int) (dX1 * TOUCHPAD_CURSOR_SENSITIVITY));

							if (prevY1 > 0 && abs(dY1) < TOUCHPAD_MAX_DELTA)
								input.setCursorDeltaY((int) (dY1 * TOUCHPAD_CURSOR_SENSITIVITY));
						} else if (prevY1 > 0 && abs(dY1) < TOUCHPAD_MAX_DELTA)
							input.setScrollClicks((int) (-dY1 * TOUCHPAD_SCROLL_SENSITIVITY));
					}

					prevTouchpadButtonDown = touchpadButtonDown;
					prevDown1 = down1;
					prevDown2 = down2;
					prevX1 = x1;
					prevY1 = y1;
				}
			});
		} catch (final Throwable t) {
			deInit();
			throw t;
		}
	}

	void deInit() {
		try {
			if (hidDevice != null) {
				reset();
				try {
					hidDevice.close();
				} catch (final IllegalStateException e) {
				}
				hidDevice = null;
			}
		} finally {
			try {
				if (earphoneDevice != null) {
					earphoneDevice.Release();
					earphoneDevice = null;
				}
			} finally {
				try {
					if (microphoneDevice != null) {
						microphoneDevice.Release();
						microphoneDevice = null;
					}
				} finally {
					if (comLibraryInitialized)
						Ole32.INSTANCE.CoUninitialize();
				}
			}
		}
	}

	public int getBatteryState() {
		return batteryState;
	}

	public boolean isCharging() {
		return charging;
	}

	private void reset() {
		hidReport = Arrays.copyOf(DEFAULT_HID_REPORT, DEFAULT_HID_REPORT.length);
		sendHidReport();
	}

	void rumble(final long duration, final byte strength) {
		new Thread(() -> {
			synchronized (hidReport) {
				hidReport[5] = strength;
				if (sendHidReport()) {
					try {
						Thread.sleep(duration);
					} catch (final InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					hidReport[5] = 0;
					sendHidReport();
				}
			}
		}).start();
	}

	private boolean sendHidReport() {
		if (hidDevice == null)
			return false;

		if (earphoneDevice != null)
			try {
				hidReport[19] = hidReport[20] = getNormalizedVolumeValue(earphoneDevice, 19);
			} catch (final Exception e) {
				log.log(SEVERE, e.getMessage(), e);
			}

		if (microphoneDevice != null)
			try {
				hidReport[21] = getNormalizedVolumeValue(microphoneDevice, 21);
			} catch (final Exception e) {
				log.log(SEVERE, e.getMessage(), e);
			}

		final var dataLength = hidReport.length - hidReportOffset;

		try {
			for (var i = 0; i < 5; i++) {
				final var dataSent = hidDevice.setOutputReport(hidReport[0],
						Arrays.copyOfRange(hidReport, 0 + hidReportOffset, hidReport.length), dataLength);
				if (dataSent == dataLength)
					return true;
			}
		} catch (final IllegalStateException e) {
		}

		return false;
	}

	private void setBatteryState(final int batteryState) {
		if (this.batteryState != batteryState) {
			this.batteryState = batteryState;

			updateLightbarColor();

			final var main = input.getMain();
			if (main != null)
				SwingUtilities.invokeLater(() -> {
					main.updateTitleAndTooltip();

					if (batteryState == LOW_BATTERY_WARNING)
						main.displayLowBatteryWarning(batteryState / 100f);
				});
		}
	}

	private void setCharging(final boolean charging) {
		if (this.charging != charging) {
			this.charging = charging;

			updateLightbarColor();

			final var main = input.getMain();
			SwingUtilities.invokeLater(() -> {
				main.updateTitleAndTooltip();
				main.displayChargingStateInfo(charging);
			});
		}
	}

	private void updateLightbarColor() {
		synchronized (hidReport) {
			if (charging) {
				hidReport[6] = (byte) (batteryState >= 100 ? 0x0 : 0x1C);
				hidReport[7] = (byte) 0x1C;
				hidReport[8] = 0x0;
			} else {
				hidReport[6] = (byte) (batteryState <= LOW_BATTERY_WARNING ? 0x1C : 0x0);
				hidReport[7] = 0;
				hidReport[8] = (byte) (batteryState <= LOW_BATTERY_WARNING ? 0x0 : 0x1C);
			}

			sendHidReport();
		}
	}
}
