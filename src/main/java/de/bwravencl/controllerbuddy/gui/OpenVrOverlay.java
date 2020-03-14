/* Copyright (C) 2019  Matteo Hausner
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

package de.bwravencl.controllerbuddy.gui;

import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_RGBA8;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.WGL.wglCreateContext;
import static org.lwjgl.opengl.WGL.wglGetCurrentContext;
import static org.lwjgl.opengl.WGL.wglMakeCurrent;
import static org.lwjgl.openvr.VR.EColorSpace_ColorSpace_Auto;
import static org.lwjgl.openvr.VR.ETextureType_TextureType_OpenGL;
import static org.lwjgl.openvr.VR.ETrackingUniverseOrigin_TrackingUniverseSeated;
import static org.lwjgl.openvr.VR.EVRApplicationType_VRApplication_Background;
import static org.lwjgl.openvr.VR.EVREventType_VREvent_Quit;
import static org.lwjgl.openvr.VR.EVRInitError_VRInitError_None;
import static org.lwjgl.openvr.VR.EVROverlayError_VROverlayError_None;
import static org.lwjgl.openvr.VR.VR_GetVRInitErrorAsEnglishDescription;
import static org.lwjgl.openvr.VR.VR_InitInternal;
import static org.lwjgl.openvr.VR.VR_ShutdownInternal;
import static org.lwjgl.openvr.VROverlay.VROverlay_CreateOverlay;
import static org.lwjgl.openvr.VROverlay.VROverlay_GetOverlayErrorNameFromEnum;
import static org.lwjgl.openvr.VROverlay.VROverlay_HideOverlay;
import static org.lwjgl.openvr.VROverlay.VROverlay_IsOverlayVisible;
import static org.lwjgl.openvr.VROverlay.VROverlay_PollNextOverlayEvent;
import static org.lwjgl.openvr.VROverlay.VROverlay_SetOverlayTexture;
import static org.lwjgl.openvr.VROverlay.VROverlay_SetOverlayTextureBounds;
import static org.lwjgl.openvr.VROverlay.VROverlay_SetOverlayTransformAbsolute;
import static org.lwjgl.openvr.VROverlay.VROverlay_SetOverlayWidthInMeters;
import static org.lwjgl.openvr.VROverlay.VROverlay_ShowOverlay;
import static org.lwjgl.system.Checks.check;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memPutAddress;
import static org.lwjgl.system.windows.GDI32.ChoosePixelFormat;
import static org.lwjgl.system.windows.GDI32.DescribePixelFormat;
import static org.lwjgl.system.windows.GDI32.PFD_SUPPORT_OPENGL;
import static org.lwjgl.system.windows.GDI32.SetPixelFormat;
import static org.lwjgl.system.windows.User32.CS_HREDRAW;
import static org.lwjgl.system.windows.User32.CS_VREDRAW;
import static org.lwjgl.system.windows.User32.DestroyWindow;
import static org.lwjgl.system.windows.User32.GetDC;
import static org.lwjgl.system.windows.User32.RegisterClassEx;
import static org.lwjgl.system.windows.User32.WS_CLIPCHILDREN;
import static org.lwjgl.system.windows.User32.WS_CLIPSIBLINGS;
import static org.lwjgl.system.windows.User32.WS_OVERLAPPEDWINDOW;
import static org.lwjgl.system.windows.User32.nCreateWindowEx;
import static org.lwjgl.system.windows.User32.nUnregisterClass;
import static org.lwjgl.system.windows.WindowsUtil.windowsThrowException;

import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.openvr.HmdMatrix34;
import org.lwjgl.openvr.OpenVR;
import org.lwjgl.openvr.Texture;
import org.lwjgl.openvr.VREvent;
import org.lwjgl.openvr.VRTextureBounds;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.windows.PIXELFORMATDESCRIPTOR;
import org.lwjgl.system.windows.User32;
import org.lwjgl.system.windows.WNDCLASSEX;
import org.lwjgl.system.windows.WindowsLibrary;

class OpenVrOverlay {

	private static final class TextureData {

		private BufferedImage image;
		private int textureObject;
		private Graphics2D g2d;
	}

	private static final Logger log = Logger.getLogger(OpenVrOverlay.class.getName());

	private static final String OVERLAY_KEY_PREFIX = OpenVrOverlay.class.getPackageName() + ".";
	private static final long OVERLAY_FPS = 25L;
	private static final float STATUS_OVERLAY_WIDTH = 0.08f;
	private static final float STATUS_OVERLAY_POSITION_X = 0.2f;
	private static final float STATUS_OVERLAY_POSITION_Y = -0.1f;
	private static final float STATUS_OVERLAY_POSITION_Z = -0.4f;
	private static final float ON_SCREEN_KEYBOARD_WIDTH = 0.4f;
	private static final float ON_SCREEN_KEYBOARD_OVERLAY_POSITION_X = 0f;
	private static final float ON_SCREEN_KEYBOARD_OVERLAY_POSITION_Y = -0.3f;
	private static final float ON_SCREEN_KEYBOARD_OVERLAY_POSITION_Z = -0.4f;

	private static ByteBuffer bufferedImageToByteBuffer(final BufferedImage image, final MemoryStack stack) {
		final var width = image.getWidth();
		final var height = image.getHeight();

		final var pixels = new int[width * height];
		image.getRGB(0, 0, width, height, pixels, 0, width);

		final var buffer = stack.malloc(width * height * 4);

		for (var y = 0; y < height; y++)
			for (var x = 0; x < width; x++) {
				final var pixel = pixels[y * width + x];
				buffer.put((byte) (pixel >> 16 & 0xFF));
				buffer.put((byte) (pixel >> 8 & 0xFF));
				buffer.put((byte) (pixel & 0xFF));
				buffer.put((byte) (pixel >> 24 & 0xFF));
			}

		return buffer.flip();
	}

	private static void createGLCapabilitiesIfRequired() {
		GLCapabilities capabilities = null;

		try {
			capabilities = GL.getCapabilities();
		} catch (final IllegalStateException e) {
		}

		if (capabilities == null)
			GL.createCapabilities();
	}

	private static HmdMatrix34 createIdentityHmdMatrix34(final MemoryStack stack) {
		final var mat = HmdMatrix34.callocStack(stack);

		mat.m(0, 1f);
		mat.m(5, 1f);
		mat.m(10, 1f);

		return mat;
	}

	private static void makeTransformFacing(final HmdMatrix34 mat) {
		rotate(mat, (float) Math.atan(mat.m(3) / mat.m(11)), 0f, 1f, 0f);
		rotate(mat, (float) -Math.atan(mat.m(7) / mat.m(11)), 1f, 0f, 0f);
	}

	private static void rotate(final HmdMatrix34 mat, final float angle, final float x, final float y, final float z) {
		final var c = (float) Math.cos(angle);
		final var s = (float) Math.sin(angle);

		final var dot = x * x + y * y + z * z;
		final var inv = 1f / (float) Math.sqrt(dot);

		final var aX = x * inv;
		final var aY = y * inv;
		final var aZ = z * inv;

		final var tempX = (1f - c) * aX;
		final var tempY = (1f - c) * aY;
		final var tempZ = (1f - c) * aZ;

		final var rotate00 = c + tempX * aX;
		final var rotate01 = tempX * aY + s * aZ;
		final var rotate02 = tempX * aZ - s * aY;

		final var rotate10 = tempY * aX - s * aZ;
		final var rotate11 = c + tempY * aY;
		final var rotate12 = tempY * aZ + s * aX;

		final var rotate20 = tempZ * aX + s * aY;
		final var rotate21 = tempZ * aY - s * aX;
		final var rotate22 = c + tempZ * aZ;

		final var res00 = mat.m(0) * rotate00 + mat.m(1) * rotate01 + mat.m(2) * rotate02;
		final var res01 = mat.m(4) * rotate00 + mat.m(5) * rotate01 + mat.m(6) * rotate02;
		final var res02 = mat.m(8) * rotate00 + mat.m(9) * rotate01 + mat.m(10) * rotate02;

		final var res10 = mat.m(0) * rotate10 + mat.m(1) * rotate11 + mat.m(2) * rotate12;
		final var res11 = mat.m(4) * rotate10 + mat.m(5) * rotate11 + mat.m(6) * rotate12;
		final var res12 = mat.m(8) * rotate10 + mat.m(9) * rotate11 + mat.m(10) * rotate12;

		final var res20 = mat.m(0) * rotate20 + mat.m(1) * rotate21 + mat.m(2) * rotate22;
		final var res21 = mat.m(4) * rotate20 + mat.m(5) * rotate21 + mat.m(6) * rotate22;
		final var res22 = mat.m(8) * rotate20 + mat.m(9) * rotate21 + mat.m(10) * rotate22;

		mat.m(0, res00);
		mat.m(4, res01);
		mat.m(8, res02);

		mat.m(1, res10);
		mat.m(5, res11);
		mat.m(9, res12);

		mat.m(2, res20);
		mat.m(6, res21);
		mat.m(10, res22);
	}

	private static void translate(final HmdMatrix34 mat, final float x, final float y, final float z) {
		mat.m(3, x);
		mat.m(7, y);
		mat.m(11, z);
	}

	private final Main main;
	private final OnScreenKeyboard onScreenKeyboard;
	private long statusOverlayHandle;
	private final long onScreenKeyboardOverlayHandle;
	private final Map<Long, TextureData> textureDataCache = new HashMap<>();
	private long hdc = NULL;
	private long hglrc = NULL;
	private short classAtom = 0;
	private long hwnd = NULL;
	private MemoryStack renderingMemoryStack;
	private final ScheduledExecutorService executorService;

	OpenVrOverlay(final Main main) throws Exception {
		this.main = main;
		onScreenKeyboard = main.getOnScreenKeyboard();

		try (var stack = stackPush()) {
			final var peError = stack.mallocInt(1);
			final var token = VR_InitInternal(peError, EVRApplicationType_VRApplication_Background);
			final var initError = peError.get();
			if (initError != EVRInitError_VRInitError_None)
				throw new Exception(getClass().getName() + ": " + VR_GetVRInitErrorAsEnglishDescription(initError));

			try {
				OpenVR.create(token);
				final var overlayTextureBounds = VRTextureBounds.mallocStack(stack);
				overlayTextureBounds.set(0f, 1f, 1f, 0f);

				final var overlayFrame = main.getOverlayFrame();
				if (overlayFrame != null) {
					final var statusOverlayHandleBuffer = stack.mallocLong(1);
					checkOverlayError(VROverlay_CreateOverlay(OVERLAY_KEY_PREFIX + overlayFrame.getTitle(),
							overlayFrame.getTitle(), statusOverlayHandleBuffer));
					statusOverlayHandle = statusOverlayHandleBuffer.get();

					checkOverlayError(VROverlay_SetOverlayWidthInMeters(statusOverlayHandle, STATUS_OVERLAY_WIDTH));

					final var statusOverlayTransform = createIdentityHmdMatrix34(stack);
					translate(statusOverlayTransform, STATUS_OVERLAY_POSITION_X, STATUS_OVERLAY_POSITION_Y,
							STATUS_OVERLAY_POSITION_Z);
					makeTransformFacing(statusOverlayTransform);
					checkOverlayError(VROverlay_SetOverlayTransformAbsolute(statusOverlayHandle,
							ETrackingUniverseOrigin_TrackingUniverseSeated, statusOverlayTransform));

					checkOverlayError(VROverlay_SetOverlayTextureBounds(statusOverlayHandle, overlayTextureBounds));
				}

				final var onScreenKeyboardOverlayHandleBuffer = stack.mallocLong(1);
				checkOverlayError(VROverlay_CreateOverlay(OVERLAY_KEY_PREFIX + onScreenKeyboard.getTitle(),
						onScreenKeyboard.getTitle(), onScreenKeyboardOverlayHandleBuffer));
				onScreenKeyboardOverlayHandle = onScreenKeyboardOverlayHandleBuffer.get();

				checkOverlayError(
						VROverlay_SetOverlayWidthInMeters(onScreenKeyboardOverlayHandle, ON_SCREEN_KEYBOARD_WIDTH));

				final var onScreenKeyboardOverlayTransform = createIdentityHmdMatrix34(stack);
				translate(onScreenKeyboardOverlayTransform, ON_SCREEN_KEYBOARD_OVERLAY_POSITION_X,
						ON_SCREEN_KEYBOARD_OVERLAY_POSITION_Y, ON_SCREEN_KEYBOARD_OVERLAY_POSITION_Z);
				makeTransformFacing(onScreenKeyboardOverlayTransform);
				checkOverlayError(VROverlay_SetOverlayTransformAbsolute(onScreenKeyboardOverlayHandle,
						ETrackingUniverseOrigin_TrackingUniverseSeated, onScreenKeyboardOverlayTransform));

				checkOverlayError(
						VROverlay_SetOverlayTextureBounds(onScreenKeyboardOverlayHandle, overlayTextureBounds));

				final var wc = WNDCLASSEX.callocStack(stack).cbSize(WNDCLASSEX.SIZEOF).style(CS_HREDRAW | CS_VREDRAW)
						.hInstance(WindowsLibrary.HINSTANCE).lpszClassName(stack.UTF16("WGL"));

				memPutAddress(wc.address() + WNDCLASSEX.LPFNWNDPROC, User32.Functions.DefWindowProc);

				classAtom = RegisterClassEx(wc);
				if (classAtom == 0)
					throw new IllegalStateException(getClass().getName() + ": failed to register WGL window class");

				hwnd = check(nCreateWindowEx(0, classAtom & 0xFFFF, NULL,
						WS_OVERLAPPEDWINDOW | WS_CLIPCHILDREN | WS_CLIPSIBLINGS, 0, 0, 1, 1, NULL, NULL, NULL, NULL));

				hdc = check(GetDC(hwnd));

				final var pfd = PIXELFORMATDESCRIPTOR.callocStack(stack).nSize((short) PIXELFORMATDESCRIPTOR.SIZEOF)
						.nVersion((short) 1).dwFlags(PFD_SUPPORT_OPENGL);
				final var pixelFormat = ChoosePixelFormat(hdc, pfd);
				if (pixelFormat == 0)
					windowsThrowException(
							getClass().getName() + ": failed to choose an OpenGL-compatible pixel format");

				if (DescribePixelFormat(hdc, pixelFormat, pfd) == 0)
					windowsThrowException(getClass().getName() + ": failed to obtain pixel format information");

				if (!SetPixelFormat(hdc, pixelFormat, pfd))
					windowsThrowException(getClass().getName() + ": failed to set the pixel format");

				hglrc = check(wglCreateContext(hdc));

				renderingMemoryStack = MemoryStack.create(2048000);

				executorService = Executors.newSingleThreadScheduledExecutor();
				executorService.scheduleAtFixedRate(this::render, 0L, 1000L / OVERLAY_FPS, TimeUnit.MILLISECONDS);
			} catch (final Throwable t) {
				log.log(Level.SEVERE, t.getMessage(), t);
				deInit();
				throw t;
			}
		}
	}

	private void checkOverlayError(final int overlayError) throws Exception {
		if (overlayError != EVROverlayError_VROverlayError_None)
			throw new Exception(getClass().getName() + ": " + VROverlay_GetOverlayErrorNameFromEnum(overlayError));
	}

	private void deInit() {
		VR_ShutdownInternal();

		if (hwnd != NULL)
			DestroyWindow(hwnd);

		if (classAtom != 0)
			nUnregisterClass(classAtom & 0xFFFF, WindowsLibrary.HINSTANCE);
	}

	private void render() {
		renderingMemoryStack.push();
		try {
			final var vrEvent = VREvent.mallocStack(renderingMemoryStack);
			while (VROverlay_PollNextOverlayEvent(onScreenKeyboardOverlayHandle, vrEvent))
				if (vrEvent.eventType() == EVREventType_VREvent_Quit)
					SwingUtilities.invokeLater(() -> stop());

			final var overlayFrame = main.getOverlayFrame();
			if (overlayFrame != null)
				updateOverlay(statusOverlayHandle, overlayFrame);

			updateOverlay(onScreenKeyboardOverlayHandle, onScreenKeyboard);
		} catch (final Throwable t) {
			log.log(Level.SEVERE, t.getMessage(), t);
		} finally {
			if (wglGetCurrentContext() == hglrc)
				wglMakeCurrent(NULL, NULL);

			renderingMemoryStack.pop();
		}
	}

	void stop() {
		executorService.shutdown();
		try {
			if (executorService.awaitTermination(2L, TimeUnit.SECONDS))
				deInit();
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private void updateOverlay(final long overlayHandle, final Frame frame) throws Exception {
		renderingMemoryStack.push();
		try {
			if (frame.isVisible()) {
				checkOverlayError(VROverlay_ShowOverlay(overlayHandle));

				if (VROverlay_IsOverlayVisible(overlayHandle)) {
					var textureData = textureDataCache.get(overlayHandle);
					if (textureData == null) {
						textureData = new TextureData();
						textureDataCache.put(overlayHandle, textureData);
					}

					final var imageResized = textureData.image == null
							|| textureData.image.getWidth() != frame.getWidth()
							|| textureData.image.getHeight() != frame.getHeight();

					if (imageResized) {
						textureData.image = new BufferedImage(frame.getWidth(), frame.getHeight(),
								BufferedImage.TYPE_INT_ARGB_PRE);
						textureData.g2d = textureData.image.createGraphics();
					}

					frame.paint(textureData.g2d);
					final var byteBuffer = bufferedImageToByteBuffer(textureData.image, renderingMemoryStack);

					if (wglGetCurrentContext() != hglrc) {
						if (!wglMakeCurrent(hdc, hglrc))
							throw new Exception(getClass().getName() + ": could not acquire OpenGL context");
						createGLCapabilitiesIfRequired();
					}

					if (imageResized)
						textureData.textureObject = glGenTextures();

					glBindTexture(GL_TEXTURE_2D, textureData.textureObject);
					glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, textureData.image.getWidth(),
							textureData.image.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, byteBuffer);

					glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
					glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

					final var texture = Texture.mallocStack(renderingMemoryStack);
					texture.set(textureData.textureObject, ETextureType_TextureType_OpenGL,
							EColorSpace_ColorSpace_Auto);
					checkOverlayError(VROverlay_SetOverlayTexture(overlayHandle, texture));
				}
			} else
				checkOverlayError(VROverlay_HideOverlay(overlayHandle));
		} finally {
			renderingMemoryStack.pop();
		}
	}
}
