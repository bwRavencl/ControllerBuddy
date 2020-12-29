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

package de.bwravencl.controllerbuddy.input.sony;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import org.lwjgl.glfw.GLFW;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.COM.Unknown;
import com.sun.jna.platform.win32.Guid.CLSID;
import com.sun.jna.platform.win32.Guid.GUID;
import com.sun.jna.platform.win32.Guid.IID;
import com.sun.jna.platform.win32.Ole32;
import com.sun.jna.platform.win32.Variant;
import com.sun.jna.platform.win32.WTypes;
import com.sun.jna.platform.win32.WinDef.BOOLByReference;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.DWORDByReference;
import com.sun.jna.platform.win32.WinDef.UINT;
import com.sun.jna.platform.win32.WinDef.UINTByReference;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.ptr.FloatByReference;
import com.sun.jna.ptr.PointerByReference;

import de.bwravencl.controllerbuddy.gui.GuiUtils;
import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;
import purejavahidapi.HidDeviceInfo;
import purejavahidapi.PureJavaHidApi;

final class DualShock4Extension extends SonyExtension {

	private static final class DualShock4Connection extends Connection {

		final String friendlyName;

		private DualShock4Connection(final int offset, final byte inputReportId, final String friendlyName) {
			super(offset, inputReportId);

			this.friendlyName = friendlyName;
		}
	}

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

			if (!WinError.S_OK.equals(GetId(ppstrId))) {
				log.log(Level.SEVERE, "IMMDevice::GetId failed");
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

	private static final byte USB_INPUT_REPORT_ID = 0x1;
	private static final byte BLUETOOTH_INPUT_REPORT_ID = 0x11;

	private static final DualShock4Connection UsbConnection = new DualShock4Connection(0, USB_INPUT_REPORT_ID,
			"Wireless Controller");
	private static final DualShock4Connection DongleConnection = new DualShock4Connection(0, USB_INPUT_REPORT_ID,
			"DUALSHOCK\u00AE4 USB Wireless Adaptor");
	private static final DualShock4Connection BluetoothConnection = new DualShock4Connection(2,
			BLUETOOTH_INPUT_REPORT_ID, null);

	private static final byte MAX_EARPHONE_VOLUME = 0x50;
	private static final byte MAX_MICROPHONE_VOLUME = 0x40;

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

	private static IMMDevice getFirstMatchingIMMDevice(final IMMDeviceEnumerator deviceEnumerator, final int dataFlow,
			final DualShock4Connection connection) throws Exception {
		final var ppDevices = new PointerByReference();
		if (!WinError.S_OK.equals(
				deviceEnumerator.EnumAudioEndpoints(dataFlow, DEVICE_STATE_ACTIVE | DEVICE_STATE_DISABLED, ppDevices)))
			throw new Exception("IMMDeviceEnumerator::EnumAudioEndpoints failed");

		final var deviceCollection = new IMMDeviceCollection(ppDevices.getValue());
		try {
			final var pcDevices = new UINTByReference();
			if (!WinError.S_OK.equals(deviceCollection.GetCount(pcDevices)))
				throw new Exception("IMMDeviceCollection::GetCount failed");

			final var cDevices = pcDevices.getValue();

			for (var i = 0; i < cDevices.intValue(); i++) {
				final var ppDevice = new PointerByReference();
				if (!WinError.S_OK.equals(deviceCollection.Item(new UINT(i), ppDevice))) {
					log.log(Level.SEVERE, "IMMDeviceCollection::Item failed");
					continue;
				}

				final var device = new IMMDevice(ppDevice.getValue());
				try {
					final var ppProperties = new PointerByReference();
					if (!WinError.S_OK.equals(device.OpenPropertyStore(STGM_READ, ppProperties))) {
						log.log(Level.SEVERE, "IMMDevice::OpenPropertyStore failed");
						continue;
					}

					final var propertyStore = new IPropertyStore(ppProperties.getValue());
					try {

						final var pv = new PROPVARIANT.ByReference();
						if (!WinError.S_OK.equals(propertyStore.GetValue(PKEY_Device_FriendlyName, pv))) {
							log.log(Level.SEVERE, "IPropertyStore::GetValue failed");
							continue;
						}

						if (pv.vt == Variant.VT_EMPTY) {
							log.log(Level.SEVERE, "PROPERTYKEY not present");
							continue;
						}

						if (pv.vt != Variant.VT_LPWSTR) {
							log.log(Level.SEVERE, "PROPERTYKEY has wrong variant");
							continue;
						}

						final var currentFriendlyName = pv.pwszVal.toString();
						if (currentFriendlyName.contains(connection.friendlyName))
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

	public static DualShock4Extension getIfAvailable(final Input input, final int jid) {
		final var guid = GLFW.glfwGetJoystickGUID(jid);
		if (guid == null)
			return null;

		final short productId;
		DualShock4Connection connection = null;
		if (guid.startsWith("030000004c050000c405"))
			productId = 0x5C4;
		else if (guid.startsWith("030000004c050000cc09"))
			productId = 0x9CC;
		else if (guid.startsWith("030000004c050000a00b")) {
			productId = 0xBA0;
			connection = DongleConnection;
		} else
			return null;

		final var hidDeviceInfo = getHidDeviceInfo(jid, guid, productId, "DualShock 4", log);
		if (hidDeviceInfo != null)
			try {
				return new DualShock4Extension(input, hidDeviceInfo, connection);
			} catch (final IOException e) {
				log.log(Level.SEVERE, e.getMessage(), e);
			}

		return null;
	}

	private static byte getNormalizedVolumeValue(final IMMDevice device, final byte maxOutputValue) throws Exception {
		final var ppAudioEndpointVolume = new PointerByReference();

		final var pdwState = new DWORDByReference();
		if (!WinError.S_OK.equals(device.GetState(pdwState)))
			throw new Exception("IMMDevice::GetState failed");

		if (pdwState.getValue().intValue() != DEVICE_STATE_ACTIVE)
			throw new Exception("Device is not active");

		if (!WinError.S_OK.equals(
				device.Activate(IAudioEndpointVolumeIID, WTypes.CLSCTX_INPROC_SERVER, null, ppAudioEndpointVolume)))
			throw new Exception("IMMDevice::Activate failed");

		final var audioEndpointVolume = new IAudioEndpointVolume(ppAudioEndpointVolume.getValue());

		final var pbMute = new BOOLByReference();
		if (!WinError.S_OK.equals(audioEndpointVolume.GetMute(pbMute)))
			throw new Exception("IAudioEndpointVolume::GetMute failed");

		if (pbMute.getValue().booleanValue())
			return 0x0;

		final var pflVolumeMindB = new FloatByReference();
		final var pflVolumeMaxdB = new FloatByReference();
		if (!WinError.S_OK
				.equals(audioEndpointVolume.GetVolumeRange(pflVolumeMindB, pflVolumeMaxdB, new FloatByReference())))
			throw new Exception("IAudioEndpointVolume::GetVolumeRange failed");

		final var pfLevel = new FloatByReference();
		if (!WinError.S_OK.equals(audioEndpointVolume.GetMasterVolumeLevel(pfLevel)))
			throw new Exception("IAudioEndpointVolume::GetMasterVolumeLevel failed");

		final var flVolumeMindB = pflVolumeMindB.getValue();
		final var fLevel = pfLevel.getValue();

		final var alignedMaxOutputValue = maxOutputValue + 0.01f;

		return (byte) Math
				.round(Input.normalize(fLevel, flVolumeMindB, pflVolumeMaxdB.getValue(), 1.0f, alignedMaxOutputValue));
	}

	private IMMDevice earphoneDevice;
	private IMMDevice microphoneDevice;
	private boolean comLibraryInitialized;

	private DualShock4Extension(final Input input, final HidDeviceInfo hidDeviceInfo,
			final DualShock4Connection connection) throws IOException {
		super(input);

		try {
			hidDevice = PureJavaHidApi.openDevice(hidDeviceInfo);

			hidDevice.setInputReportListener(new SonyInputReportListener() {

				@Override
				void handleBattery(final byte[] reportData) {
					final var cableConnected = (reportData[30 + DualShock4Extension.this.connection.offset] >> 4
							& 0x1) != 0;
					var battery = reportData[30 + DualShock4Extension.this.connection.offset] & 0xF;

					setCharging(cableConnected);

					if (!cableConnected)
						battery++;

					battery = Math.min(battery, 10);
					battery *= 10;

					setBatteryState(battery);
				}

				@Override
				void handleNewConnection(final int reportLength) {
					DualShock4Extension.this.connection = connection != null ? connection
							: isBluetoothConnection(reportLength) ? BluetoothConnection : UsbConnection;

					if (Main.isWindows && !DualShock4Extension.this.connection.isBluetooth()) {
						final var coInitializeExResult = Ole32.INSTANCE.CoInitializeEx(null,
								Ole32.COINIT_APARTMENTTHREADED);

						if (WinError.S_OK.equals(coInitializeExResult))
							comLibraryInitialized = true;
						else if (WinError.S_FALSE.equals(coInitializeExResult)) {
							comLibraryInitialized = true;
							log.log(Level.WARNING, "COM library was already initialized");
						} else {
							comLibraryInitialized = false;
							log.log(Level.SEVERE, "CoInitializeEx failed");
						}

						if (comLibraryInitialized) {
							final var ppDeviceEnumerator = new PointerByReference();
							if (!WinError.S_OK.equals(Ole32.INSTANCE.CoCreateInstance(MMDeviceEnumeratorCLSID, null,
									WTypes.CLSCTX_INPROC_SERVER, IMMDeviceEnumeratorIID, ppDeviceEnumerator)))
								log.log(Level.SEVERE, "CoCreateInstance failed");
							else {
								final var deviceEnumerator = new IMMDeviceEnumerator(ppDeviceEnumerator.getValue());
								try {
									try {
										earphoneDevice = getFirstMatchingIMMDevice(deviceEnumerator, eRender,
												(DualShock4Connection) DualShock4Extension.this.connection);
										if (earphoneDevice != null)
											log.log(Level.INFO, "Using DualShock 4 earphone device: " + earphoneDevice);
										else
											log.log(Level.WARNING, "DualShock 4 earphone not device found");
									} catch (final Exception e) {
										log.log(Level.SEVERE, e.getMessage(), e);
									}

									microphoneDevice = getFirstMatchingIMMDevice(deviceEnumerator, eCapture,
											(DualShock4Connection) DualShock4Extension.this.connection);
									if (microphoneDevice != null)
										log.log(Level.INFO, "Using DualShock 4 microphone device: " + microphoneDevice);
									else
										log.log(Level.WARNING, "DualShock 4 microphone not device found");
								} catch (final Exception e) {
									log.log(Level.SEVERE, e.getMessage(), e);
								} finally {
									deviceEnumerator.Release();

									if (earphoneDevice == null && microphoneDevice == null) {
										Ole32.INSTANCE.CoUninitialize();
										comLibraryInitialized = false;
									}
								}
							}

							if (earphoneDevice == null || microphoneDevice == null)
								GuiUtils.showMessageDialog(input.getMain().getFrame(),
										Main.strings.getString("COULD_NOT_FIND_DUAL_SHOCK_4_AUDIO_DEVICE_DIALOG_TEXT"),
										Main.strings.getString("WARNING_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
						} else
							GuiUtils.showMessageDialog(input.getMain().getFrame(),
									Main.strings.getString("COULD_NOT_INITIALIZE_COM_LIBRARY_DIALOG_TEXT"),
									Main.strings.getString("WARNING_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
					}
				}
			});
		} catch (final Throwable t) {
			deInit(false);
			throw t;
		}
	}

	@Override
	public void deInit(final boolean disconnected) {
		try {
			super.deInit(disconnected);
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

	@Override
	int getButtonsOffset() {
		return 5;
	}

	@Override
	byte[] getDefaultHidReport() {
		if (connection == null)
			return null;

		final byte[] defaultHidReport;
		if (connection.isBluetooth()) {
			defaultHidReport = new byte[334];

			defaultHidReport[0] = 0x15;
			defaultHidReport[1] = (byte) 0xC0;
			defaultHidReport[3] = (byte) 0xF7;
		} else {
			defaultHidReport = new byte[32];

			defaultHidReport[0] = (byte) 0x5;
			defaultHidReport[1] = (byte) 0xF;
		}

		defaultHidReport[6 + connection.offset] = (byte) 0xC;
		defaultHidReport[7 + connection.offset] = (byte) 0x18;
		defaultHidReport[8 + connection.offset] = (byte) 0x1C;
		defaultHidReport[19 + connection.offset] = MAX_EARPHONE_VOLUME;
		defaultHidReport[20 + connection.offset] = MAX_EARPHONE_VOLUME;
		defaultHidReport[21 + connection.offset] = MAX_MICROPHONE_VOLUME;

		return defaultHidReport;
	}

	@Override
	int getL2Offset() {
		return 8;
	}

	@Override
	int getLightbarOffset() {
		return 6;
	}

	@Override
	long getLightRumbleDuration() {
		return 20L;
	}

	@Override
	byte getLightRumbleStrength() {
		return Byte.MAX_VALUE;
	}

	@Override
	Logger getLogger() {
		return log;
	}

	@Override
	int getRumbleOffset() {
		return 5;
	}

	@Override
	long getStrongRumbleDuration() {
		return 80L;
	}

	@Override
	byte getStrongRumbleStrength() {
		return Byte.MAX_VALUE;
	}

	@Override
	int getTouchpadOffset() {
		return 35;
	}

	@Override
	void sendHidReport() {
		if (connection == null)
			return;

		if (earphoneDevice != null)
			try {
				hidReport[19] = hidReport[20] = getNormalizedVolumeValue(earphoneDevice, MAX_EARPHONE_VOLUME);
			} catch (final Exception e) {
				log.log(Level.SEVERE, e.getMessage(), e);
			}

		if (microphoneDevice != null)
			try {
				hidReport[21] = getNormalizedVolumeValue(microphoneDevice, MAX_MICROPHONE_VOLUME);
			} catch (final Exception e) {
				log.log(Level.SEVERE, e.getMessage(), e);
			}

		super.sendHidReport();
	}
}
