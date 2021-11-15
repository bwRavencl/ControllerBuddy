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

package de.bwravencl.controllerbuddy.gui;

import java.awt.EventQueue;
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

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.WGL;
import org.lwjgl.openvr.HmdMatrix34;
import org.lwjgl.openvr.OpenVR;
import org.lwjgl.openvr.Texture;
import org.lwjgl.openvr.VR;
import org.lwjgl.openvr.VREvent;
import org.lwjgl.openvr.VROverlay;
import org.lwjgl.openvr.VRTextureBounds;
import org.lwjgl.system.Checks;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.windows.GDI32;
import org.lwjgl.system.windows.PIXELFORMATDESCRIPTOR;
import org.lwjgl.system.windows.User32;
import org.lwjgl.system.windows.WNDCLASSEX;
import org.lwjgl.system.windows.WindowsLibrary;
import org.lwjgl.system.windows.WindowsUtil;

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
		final var mat = HmdMatrix34.calloc(stack);

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
	private long hdc = MemoryUtil.NULL;
	private long hglrc = MemoryUtil.NULL;
	private short classAtom = 0;
	private long hwnd = MemoryUtil.NULL;
	private MemoryStack renderingMemoryStack;
	private final ScheduledExecutorService executorService;

	OpenVrOverlay(final Main main) throws Exception {
		this.main = main;
		onScreenKeyboard = main.getOnScreenKeyboard();

		try (var stack = MemoryStack.stackPush()) {
			final var peError = stack.mallocInt(1);
			final var token = VR.VR_InitInternal(peError, VR.EVRApplicationType_VRApplication_Background);
			final var initError = peError.get();
			if (initError != VR.EVRInitError_VRInitError_None)
				throw new Exception(getClass().getName() + ": " + VR.VR_GetVRInitErrorAsEnglishDescription(initError));

			try {
				OpenVR.create(token);
				final var overlayTextureBounds = VRTextureBounds.malloc(stack);
				overlayTextureBounds.set(0f, 1f, 1f, 0f);

				final var overlayFrame = main.getOverlayFrame();
				if (overlayFrame != null) {
					final var statusOverlayHandleBuffer = stack.mallocLong(1);
					checkOverlayError(VROverlay.VROverlay_CreateOverlay(OVERLAY_KEY_PREFIX + overlayFrame.getTitle(),
							overlayFrame.getTitle(), statusOverlayHandleBuffer));
					statusOverlayHandle = statusOverlayHandleBuffer.get();

					checkOverlayError(
							VROverlay.VROverlay_SetOverlayWidthInMeters(statusOverlayHandle, STATUS_OVERLAY_WIDTH));

					final var statusOverlayTransform = createIdentityHmdMatrix34(stack);
					translate(statusOverlayTransform, STATUS_OVERLAY_POSITION_X, STATUS_OVERLAY_POSITION_Y,
							STATUS_OVERLAY_POSITION_Z);
					makeTransformFacing(statusOverlayTransform);
					checkOverlayError(VROverlay.VROverlay_SetOverlayTransformAbsolute(statusOverlayHandle,
							VR.ETrackingUniverseOrigin_TrackingUniverseSeated, statusOverlayTransform));

					checkOverlayError(
							VROverlay.VROverlay_SetOverlayTextureBounds(statusOverlayHandle, overlayTextureBounds));
				}

				final var onScreenKeyboardOverlayHandleBuffer = stack.mallocLong(1);
				checkOverlayError(VROverlay.VROverlay_CreateOverlay(OVERLAY_KEY_PREFIX + onScreenKeyboard.getTitle(),
						onScreenKeyboard.getTitle(), onScreenKeyboardOverlayHandleBuffer));
				onScreenKeyboardOverlayHandle = onScreenKeyboardOverlayHandleBuffer.get();

				checkOverlayError(VROverlay.VROverlay_SetOverlayWidthInMeters(onScreenKeyboardOverlayHandle,
						ON_SCREEN_KEYBOARD_WIDTH));

				final var onScreenKeyboardOverlayTransform = createIdentityHmdMatrix34(stack);
				translate(onScreenKeyboardOverlayTransform, ON_SCREEN_KEYBOARD_OVERLAY_POSITION_X,
						ON_SCREEN_KEYBOARD_OVERLAY_POSITION_Y, ON_SCREEN_KEYBOARD_OVERLAY_POSITION_Z);
				makeTransformFacing(onScreenKeyboardOverlayTransform);
				checkOverlayError(VROverlay.VROverlay_SetOverlayTransformAbsolute(onScreenKeyboardOverlayHandle,
						VR.ETrackingUniverseOrigin_TrackingUniverseSeated, onScreenKeyboardOverlayTransform));

				checkOverlayError(VROverlay.VROverlay_SetOverlayTextureBounds(onScreenKeyboardOverlayHandle,
						overlayTextureBounds));

				final var wc = WNDCLASSEX.calloc(stack).cbSize(WNDCLASSEX.SIZEOF)
						.style(User32.CS_HREDRAW | User32.CS_VREDRAW).hInstance(WindowsLibrary.HINSTANCE)
						.lpszClassName(stack.UTF16("WGL"));

				MemoryUtil.memPutAddress(wc.address() + WNDCLASSEX.LPFNWNDPROC, User32.Functions.DefWindowProc);

				classAtom = User32.RegisterClassEx(wc);
				if (classAtom == 0)
					throw new IllegalStateException(getClass().getName() + ": failed to register WGL window class");

				hwnd = Checks.check(User32.nCreateWindowEx(0, classAtom & 0xFFFF, MemoryUtil.NULL,
						User32.WS_OVERLAPPEDWINDOW | User32.WS_CLIPCHILDREN | User32.WS_CLIPSIBLINGS, 0, 0, 1, 1,
						MemoryUtil.NULL, MemoryUtil.NULL, MemoryUtil.NULL, MemoryUtil.NULL));

				hdc = Checks.check(User32.GetDC(hwnd));

				final var pfd = PIXELFORMATDESCRIPTOR.calloc(stack).nSize((short) PIXELFORMATDESCRIPTOR.SIZEOF)
						.nVersion((short) 1).dwFlags(GDI32.PFD_SUPPORT_OPENGL);
				final var pixelFormat = GDI32.ChoosePixelFormat(hdc, pfd);
				if (pixelFormat == 0)
					WindowsUtil.windowsThrowException(
							getClass().getName() + ": failed to choose an OpenGL-compatible pixel format");

				if (GDI32.DescribePixelFormat(hdc, pixelFormat, pfd) == 0)
					WindowsUtil.windowsThrowException(
							getClass().getName() + ": failed to obtain pixel format information");

				if (!GDI32.SetPixelFormat(hdc, pixelFormat, pfd))
					WindowsUtil.windowsThrowException(getClass().getName() + ": failed to set the pixel format");

				hglrc = Checks.check(WGL.wglCreateContext(hdc));

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
		if (overlayError != VR.EVROverlayError_VROverlayError_None)
			throw new Exception(
					getClass().getName() + ": " + VROverlay.VROverlay_GetOverlayErrorNameFromEnum(overlayError));
	}

	private void deInit() {
		VR.VR_ShutdownInternal();

		if (hwnd != MemoryUtil.NULL)
			User32.DestroyWindow(hwnd);

		if (classAtom != 0)
			User32.nUnregisterClass(classAtom & 0xFFFF, WindowsLibrary.HINSTANCE);
	}

	private void render() {
		renderingMemoryStack.push();
		try {
			final var vrEvent = VREvent.malloc(renderingMemoryStack);
			while (VROverlay.VROverlay_PollNextOverlayEvent(onScreenKeyboardOverlayHandle, vrEvent))
				if (vrEvent.eventType() == VR.EVREventType_VREvent_Quit)
					EventQueue.invokeLater(this::stop);

			final var overlayFrame = main.getOverlayFrame();
			if (overlayFrame != null)
				updateOverlay(statusOverlayHandle, overlayFrame);

			updateOverlay(onScreenKeyboardOverlayHandle, onScreenKeyboard);
		} catch (final Throwable t) {
			log.log(Level.SEVERE, t.getMessage(), t);
		} finally {
			if (WGL.wglGetCurrentContext() == hglrc)
				WGL.wglMakeCurrent(MemoryUtil.NULL, MemoryUtil.NULL);

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
				checkOverlayError(VROverlay.VROverlay_ShowOverlay(overlayHandle));

				if (VROverlay.VROverlay_IsOverlayVisible(overlayHandle)) {
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

					if (WGL.wglGetCurrentContext() != hglrc) {
						if (!WGL.wglMakeCurrent(hdc, hglrc))
							throw new Exception(getClass().getName() + ": could not acquire OpenGL context");
						createGLCapabilitiesIfRequired();
					}

					if (imageResized)
						textureData.textureObject = GL11.glGenTextures();

					GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureData.textureObject);
					GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, textureData.image.getWidth(),
							textureData.image.getHeight(), 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, byteBuffer);

					GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
					GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

					final var texture = Texture.malloc(renderingMemoryStack);
					texture.set(textureData.textureObject, VR.ETextureType_TextureType_OpenGL,
							VR.EColorSpace_ColorSpace_Auto);
					checkOverlayError(VROverlay.VROverlay_SetOverlayTexture(overlayHandle, texture));
				}
			} else
				checkOverlayError(VROverlay.VROverlay_HideOverlay(overlayHandle));
		} finally {
			renderingMemoryStack.pop();
		}
	}
}
