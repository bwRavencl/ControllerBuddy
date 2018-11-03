package de.bwravencl.controllerbuddy.gui;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.System.Logger;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;
import com.sun.jna.ptr.LongByReference;

import glm_.mat4x4.Mat4;
import openvr.lib.EColorSpace;
import openvr.lib.ETextureType;
import openvr.lib.ETrackingUniverseOrigin;
import openvr.lib.EVRApplicationType;
import openvr.lib.EVREventType;
import openvr.lib.EVRInitError;
import openvr.lib.EVRInitError_ByReference;
import openvr.lib.EVROverlayError;
import openvr.lib.HmdMat34;
import openvr.lib.IVROverlay;
import openvr.lib.OpenvrKt;
import openvr.lib.VREvent;
import openvr.lib.VRTextureBounds;

class OpenVrOverlay {

	private static final System.Logger log = System.getLogger(OpenVrOverlay.class.getName());

	private static final String OVERLAY_KEY_PREFIX = OpenVrOverlay.class.getPackageName() + ".";

	private static final long OVERLAY_FPS = 25L;

	private static final float STATUS_OVERLAY_WIDTH = 0.08f;
	private static final float STATUS_OVERLAY_POSITION_X = 0.2f;
	private static final float STATUS_OVERLAY_POSITION_Y = -0.1f;
	private static final float STATUS_OVERLAY_POSITION_Z = -0.4f;

	private static final float ON_SCREEN_KEYBOARD_WIDTH = 0.4f;
	private static final float ON_SCREEN_KEYBOARD_OVERLAY_POSITION_X = 0.0f;
	private static final float ON_SCREEN_KEYBOARD_OVERLAY_POSITION_Y = -0.3f;
	private static final float ON_SCREEN_KEYBOARD_OVERLAY_POSITION_Z = -0.4f;

	private static final void makeTransformFacing(final Mat4 mat4) {
		mat4.rotateAssign((float) Math.atan(mat4.v30() / mat4.v32()), 0.0f, 1.0f, 0.0f);
		mat4.rotateAssign((float) -Math.atan(mat4.v31() / mat4.v32()), 1.0f, 0.0f, 0.0f);
	}

	private static final HmdMat34.ByReference mat4ToHmdMat34ByReference(final Mat4 mat4) {
		final HmdMat34.ByReference hmdMat34 = new HmdMat34.ByReference();

		hmdMat34.m[0] = mat4.v00();
		hmdMat34.m[1] = mat4.v10();
		hmdMat34.m[2] = mat4.v20();
		hmdMat34.m[3] = mat4.v30();
		hmdMat34.m[4] = mat4.v01();
		hmdMat34.m[5] = mat4.v11();
		hmdMat34.m[6] = mat4.v21();
		hmdMat34.m[7] = mat4.v31();
		hmdMat34.m[8] = mat4.v02();
		hmdMat34.m[9] = mat4.v12();
		hmdMat34.m[10] = mat4.v22();
		hmdMat34.m[11] = mat4.v32();

		return hmdMat34;
	}

	private final Main main;
	private final OnScreenKeyboard onScreenKeyboard;
	private final IVROverlay vrOverlay;
	private long statusOverlayHandle;
	private final long onScreenKeyboardOverlayHandle;
	private BufferedImage statusOverlayImage;
	private Graphics2D statusOverlayGraphics2d;
	private Texture statusOverlayTexture;
	private BufferedImage onScreenKeyboardOverlayImage;
	private Graphics2D onScreenKeyboardOverlayGraphics2d;
	private Texture onScreenKeyboardOverlayTexture;
	private final GLProfile glProfile;
	private final GLContext glContext;
	private final ScheduledExecutorService executorService;

	OpenVrOverlay(final Main main) throws Exception {
		this.main = main;
		onScreenKeyboard = main.getOnScreenKeyboard();

		final EVRInitError_ByReference initError = new EVRInitError_ByReference();
		OpenvrKt.vrInit(initError, EVRApplicationType.Background, null);
		if (initError.value != EVRInitError.None)
			throw new Exception(
					getClass().getName() + ": " + OpenvrKt.vrGetVRInitErrorAsEnglishDescription(initError.value));

		try {
			vrOverlay = OpenvrKt.getVrOverlay();
			if (vrOverlay == null)
				throw new Exception(getClass().getName() + ": could not acquire vrOverlay");

			final VRTextureBounds.ByReference overlayTextureBounds = new VRTextureBounds.ByReference();
			overlayTextureBounds.uMax = 1.0f;
			overlayTextureBounds.uMin = 0.0f;
			overlayTextureBounds.vMax = 0.0f;
			overlayTextureBounds.vMin = 1.0f;

			final JFrame overlayFrame = main.getOverlayFrame();
			if (overlayFrame != null) {
				main.setOnScreenKeyboardButtonVisible(false);

				final LongByReference statusOverlayHandleReference = new LongByReference();
				checkOverlayError(vrOverlay.createOverlay(OVERLAY_KEY_PREFIX + overlayFrame.getTitle(),
						overlayFrame.getTitle(), statusOverlayHandleReference));
				statusOverlayHandle = statusOverlayHandleReference.getValue();

				checkOverlayError(vrOverlay.setOverlayWidthInMeters(statusOverlayHandle, STATUS_OVERLAY_WIDTH));

				final Mat4 statusOverlayTransform = new Mat4();
				statusOverlayTransform.translateAssign(STATUS_OVERLAY_POSITION_X, STATUS_OVERLAY_POSITION_Y,
						STATUS_OVERLAY_POSITION_Z);
				makeTransformFacing(statusOverlayTransform);

				checkOverlayError(vrOverlay.setOverlayTransformAbsolute(statusOverlayHandle,
						ETrackingUniverseOrigin.Seated, mat4ToHmdMat34ByReference(statusOverlayTransform)));

				checkOverlayError(vrOverlay.setOverlayTextureBounds(statusOverlayHandle, overlayTextureBounds));

				checkOverlayError(vrOverlay.showOverlay(statusOverlayHandle));
			}

			final LongByReference onScreenKeyboardOverlayHandleReference = new LongByReference();
			checkOverlayError(vrOverlay.createOverlay(OVERLAY_KEY_PREFIX + onScreenKeyboard.getTitle(),
					onScreenKeyboard.getTitle(), onScreenKeyboardOverlayHandleReference));
			onScreenKeyboardOverlayHandle = onScreenKeyboardOverlayHandleReference.getValue();

			checkOverlayError(
					vrOverlay.setOverlayWidthInMeters(onScreenKeyboardOverlayHandle, ON_SCREEN_KEYBOARD_WIDTH));

			final Mat4 onScreenKeyboardOverlayTransform = new Mat4();
			onScreenKeyboardOverlayTransform.translateAssign(ON_SCREEN_KEYBOARD_OVERLAY_POSITION_X,
					ON_SCREEN_KEYBOARD_OVERLAY_POSITION_Y, ON_SCREEN_KEYBOARD_OVERLAY_POSITION_Z);
			makeTransformFacing(onScreenKeyboardOverlayTransform);

			checkOverlayError(vrOverlay.setOverlayTransformAbsolute(onScreenKeyboardOverlayHandle,
					ETrackingUniverseOrigin.Seated, mat4ToHmdMat34ByReference(onScreenKeyboardOverlayTransform)));

			checkOverlayError(vrOverlay.setOverlayTextureBounds(onScreenKeyboardOverlayHandle, overlayTextureBounds));

			glProfile = GLProfile.get(GLProfile.GL2GL3);
			final GLCapabilities glCapabilities = new GLCapabilities(glProfile);
			glCapabilities.setDoubleBuffered(false);
			glCapabilities.setOnscreen(false);

			final GLDrawable dummyDrawable = GLDrawableFactory.getDesktopFactory().createDummyDrawable(null, true,
					glCapabilities, null);
			dummyDrawable.setRealized(true);
			glContext = dummyDrawable.createContext(null);

			executorService = Executors.newSingleThreadScheduledExecutor();
			executorService.scheduleAtFixedRate(this::render, 0L, 1000L / OVERLAY_FPS, TimeUnit.MILLISECONDS);
		} catch (final Throwable t) {
			deInit();
			throw t;
		}
	}

	private void checkOverlayError(final EVROverlayError overlayError) throws Exception {
		if (overlayError != EVROverlayError.None)
			throw new Exception(getClass().getName() + ": " + vrOverlay.getOverlayErrorNameFromEnum(overlayError));
	}

	private void deInit() {
		OpenvrKt.vrShutdown();

		if (glContext != null)
			glContext.destroy();

		main.setOnScreenKeyboardButtonVisible(true);
	}

	private void render() {
		try {
			final VREvent.ByReference vrEvent = new openvr.lib.VREvent.ByReference();
			while (vrOverlay.pollNextOverlayEvent(onScreenKeyboardOverlayHandle, vrEvent, vrEvent.size()))
				if (vrEvent.eEventType == EVREventType.Quit.i)
					SwingUtilities.invokeLater(() -> stop());

			final JFrame overlayFrame = main.getOverlayFrame();
			if (overlayFrame != null)
				if (overlayFrame.isVisible()) {
					checkOverlayError(vrOverlay.showOverlay(statusOverlayHandle));

					if (vrOverlay.isOverlayVisible(statusOverlayHandle)) {
						final boolean freshImage = statusOverlayImage == null
								|| statusOverlayImage.getWidth() != overlayFrame.getWidth()
								|| statusOverlayImage.getHeight() != overlayFrame.getHeight();

						if (freshImage) {
							statusOverlayImage = new BufferedImage(overlayFrame.getWidth(), overlayFrame.getHeight(),
									BufferedImage.TYPE_INT_ARGB_PRE);
							statusOverlayGraphics2d = statusOverlayImage.createGraphics();
						}

						overlayFrame.paint(statusOverlayGraphics2d);

						glContext.makeCurrent();

						if (freshImage)
							statusOverlayTexture = AWTTextureIO.newTexture(glProfile, statusOverlayImage, false);
						else
							statusOverlayTexture.updateImage(glContext.getGL(),
									AWTTextureIO.newTextureData(glProfile, statusOverlayImage, false));

						final openvr.lib.Texture.ByReference textureReference = new openvr.lib.Texture.ByReference(
								statusOverlayTexture.getTextureObject(), ETextureType.OpenGL, EColorSpace.Auto);
						checkOverlayError(vrOverlay.setOverlayTexture(statusOverlayHandle, textureReference));
					}
				} else
					checkOverlayError(vrOverlay.hideOverlay(statusOverlayHandle));

			if (onScreenKeyboard.isVisible()) {
				checkOverlayError(vrOverlay.showOverlay(onScreenKeyboardOverlayHandle));

				if (vrOverlay.isOverlayVisible(onScreenKeyboardOverlayHandle)) {
					final boolean freshImage = onScreenKeyboardOverlayImage == null
							|| onScreenKeyboardOverlayImage.getWidth() != onScreenKeyboard.getWidth()
							|| onScreenKeyboardOverlayImage.getHeight() != onScreenKeyboard.getHeight();

					if (freshImage) {
						onScreenKeyboardOverlayImage = new BufferedImage(onScreenKeyboard.getWidth(),
								onScreenKeyboard.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
						onScreenKeyboardOverlayGraphics2d = onScreenKeyboardOverlayImage.createGraphics();
					}

					onScreenKeyboard.paint(onScreenKeyboardOverlayGraphics2d);

					if (!glContext.isCurrent())
						glContext.makeCurrent();

					if (freshImage)
						onScreenKeyboardOverlayTexture = AWTTextureIO.newTexture(glProfile,
								onScreenKeyboardOverlayImage, false);
					else
						onScreenKeyboardOverlayTexture.updateImage(glContext.getGL(),
								AWTTextureIO.newTextureData(glProfile, onScreenKeyboardOverlayImage, false));

					final openvr.lib.Texture.ByReference textureReference = new openvr.lib.Texture.ByReference(
							onScreenKeyboardOverlayTexture.getTextureObject(), ETextureType.OpenGL, EColorSpace.Auto);
					checkOverlayError(vrOverlay.setOverlayTexture(onScreenKeyboardOverlayHandle, textureReference));
				}
			} else
				checkOverlayError(vrOverlay.hideOverlay(onScreenKeyboardOverlayHandle));
		} catch (final Exception e) {
			log.log(Logger.Level.ERROR, e.getMessage(), e);
		} finally {
			if (glContext.isCurrent())
				glContext.release();
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

}
