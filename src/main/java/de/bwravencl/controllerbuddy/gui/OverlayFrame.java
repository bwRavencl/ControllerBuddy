/*
 * Copyright (C) 2026 Matteo Hausner
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 */

package de.bwravencl.controllerbuddy.gui;

import de.bwravencl.controllerbuddy.ffi.Linux;
import de.bwravencl.controllerbuddy.ffi.Wayland;
import de.bwravencl.controllerbuddy.ffi.Xlib;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.dnd.DropTarget;
import java.awt.dnd.InvalidDnDOperationException;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.beans.PropertyChangeListener;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import jdk.swing.interop.DragSourceContextWrapper;
import jdk.swing.interop.LightweightContentWrapper;
import jdk.swing.interop.LightweightFrameWrapper;
import jdk.swing.interop.SwingInterOpUtils;
import org.jspecify.annotations.Nullable;

/// A [JFrame] that is shown as an overlay above all windows, including
/// fullscreen applications, when running on JBR's Wayland toolkit with a
/// compositor that offers wlr-layer-shell.
///
/// Everywhere else (X11, XWayland, GNOME, Windows, macOS) it behaves like a
/// regular [JFrame].
///
/// In overlay mode the real window stays hidden and its content pane is
/// rendered into a layer surface instead. The usual frame API keeps working:
///
/// - `setVisible`, `setLocation`, `setSize`, `setBounds`, `pack`,
/// `setMinimumSize`, `setMaximumSize`, `getLocationOnScreen`, `isVisible`,
/// `dispose`
/// - `getContentPane`, `add`, `remove`, `setLayout`, `setContentPane`,
/// `validate`, `repaint`
/// - mouse listeners on the frame itself and on its components
/// - a border on the root pane
/// - a translucent frame background
///
/// The overlay stays on the monitor it was opened on; locations are clamped to
/// that monitor. It never takes keyboard focus. All Wayland work happens on the
/// event dispatch thread: JBR dispatches the default event queue there, and
/// that is where the overlay's objects live.
@SuppressWarnings("restricted")
class OverlayFrame extends JFrame {

	private static final Logger logger = Logger.getLogger(OverlayFrame.class.getName());

	@Serial
	private static final long serialVersionUID = 42924347868473420L;

	/// A closed overlay kept for the next [#setVisible(boolean)], so that its
	/// lightweight frame and image buffer are not created again on every show.
	@SuppressWarnings({ "serial", "RedundantSuppression" })
	private @Nullable Overlay idleOverlay;

	/// The overlay while this frame is shown in overlay mode, otherwise `null`.
	@SuppressWarnings({ "serial", "RedundantSuppression" })
	private @Nullable Overlay overlay;

	/// The real content pane while it is rendered by the overlay, otherwise `null`.
	private @Nullable Container overlayContent;

	/// Copies border changes of the root pane to the overlay while it is open.
	@SuppressWarnings({ "serial", "RedundantSuppression" })
	private @Nullable PropertyChangeListener rootBorderMirror;

	/// Constructs an [OverlayFrame] with a title.
	///
	/// @param title the title
	OverlayFrame(final String title) {
		super(title);
		GuiUtils.setWindowIcon(this);
	}

	/// Runs a Wayland event handler and logs any exception, because an exception
	/// escaping an upcall terminates the JVM.
	///
	/// @param eventName the name of the event, used in the log message
	/// @param handler the event handler
	private static void handleEvent(final String eventName, final Runnable handler) {
		try {
			handler.run();
		} catch (final Throwable t) {
			logger.log(Level.SEVERE, "Error in " + eventName, t);
		}
	}

	@Override
	public synchronized void addMouseListener(final MouseListener listener) {
		super.addMouseListener(listener);
		if (overlay != null) {
			overlay.surfacePanel.addMouseListener(listener);
		}
	}

	@Override
	public synchronized void addMouseMotionListener(final MouseMotionListener listener) {
		super.addMouseMotionListener(listener);
		if (overlay != null) {
			overlay.surfacePanel.addMouseMotionListener(listener);
		}
	}

	@Override
	public synchronized void addMouseWheelListener(final MouseWheelListener listener) {
		super.addMouseWheelListener(listener);
		if (overlay != null) {
			overlay.surfacePanel.addMouseWheelListener(listener);
		}
	}

	@Override
	public void addNotify() {
		super.addNotify();

		if (!MainFrame.IS_X11_TOOLKIT) {
			return;
		}

		if (!isDisplayable()) {
			throw new IllegalStateException("Frame is not displayable");
		}

		try {
			@SuppressWarnings({ "Java9ReflectionClassVisibility", "RedundantSuppression" })
			final var awtAccessorClass = Class.forName("sun.awt.AWTAccessor", false, null);

			final var getComponentAccessor = awtAccessorClass.getMethod("getComponentAccessor");
			final var componentAccessor = getComponentAccessor.invoke(null);

			final var getPeerMethod = componentAccessor.getClass().getMethod("getPeer", Component.class);
			getPeerMethod.setAccessible(true);
			final var peer = getPeerMethod.invoke(componentAccessor, this);

			if (peer == null) {
				throw new IllegalStateException("Could not retrieve native Xlib window peer");
			}

			final var getWindowMethod = peer.getClass().getMethod("getWindow");
			final var windowId = (long) getWindowMethod.invoke(peer);
			if (windowId == 0L) {
				throw new IllegalStateException("Could not retrieve native Xlib window ID");
			}

			try (final var arena = Arena.ofConfined()) {
				final var display = Xlib.XOpenDisplay(MemorySegment.NULL);
				if (MemorySegment.NULL.equals(display)) {
					throw new RuntimeException("Failed to open X11 display");
				}

				try {
					final var attributes = arena.allocate(Xlib.XSetWindowAttributes.LAYOUT);
					Xlib.XSetWindowAttributes.OVERRIDE_REDIRECT_VAR_HANDLE.set(attributes, 0L, 1);
					Xlib.XChangeWindowAttributes(display, windowId, Xlib.CWOverrideRedirect, attributes);

					final var steamAppId = System.getenv("SteamAppId");
					if (steamAppId != null && !steamAppId.isBlank()) {
						try {
							final var propNameSeg = arena.allocateFrom("STEAM_GAME", StandardCharsets.UTF_8);
							final var typeNameSeg = arena.allocateFrom("CARDINAL", StandardCharsets.UTF_8);

							final var propertyAtom = Xlib.XInternAtom(display, propNameSeg, 0);
							final var cardinalTypeAtom = Xlib.XInternAtom(display, typeNameSeg, 0);

							final var dataBuffer = arena.allocate(ValueLayout.JAVA_LONG);
							dataBuffer.set(ValueLayout.JAVA_LONG, 0L, Long.parseLong(steamAppId));

							Xlib.XChangeProperty(display, windowId, propertyAtom, cardinalTypeAtom, 32, 0, dataBuffer,
									1);
						} catch (final NumberFormatException e) {
							logger.log(Level.WARNING, e.getMessage(), e);
						}
					}

					Xlib.XFlush(display);
				} finally {
					Xlib.XCloseDisplay(display);
				}
			}
		} catch (final ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	/// Closes the overlay and gives the content pane back to the frame.
	private void closeOverlay() {
		final var currentOverlay = overlay;
		if (currentOverlay != null) {
			currentOverlay.close();
		}
		overlayClosed();
	}

	/// Applies the maximum and minimum height, as the window peer does in regular
	/// mode.
	///
	/// @param height the requested height
	/// @return the constrained height
	private int constrainHeight(final int height) {
		final var max = isMaximumSizeSet() ? Math.min(height, getMaximumSize().height) : height;
		return isMinimumSizeSet() ? Math.max(max, getMinimumSize().height) : max;
	}

	/// Applies the maximum and minimum width, as the window peer does in regular
	/// mode.
	///
	/// @param width the requested width
	/// @return the constrained width
	private int constrainWidth(final int width) {
		final var max = isMaximumSizeSet() ? Math.min(width, getMaximumSize().width) : width;
		return isMinimumSizeSet() ? Math.max(max, getMinimumSize().width) : max;
	}

	@Override
	public void dispose() {
		closeOverlay();
		if (idleOverlay != null) {
			idleOverlay.dispose();
			idleOverlay = null;
		}
		super.dispose();
	}

	@Override
	public Container getContentPane() {
		return overlayContent != null ? overlayContent : super.getContentPane();
	}

	@Override
	public Point getLocationOnScreen() {
		return overlay != null ? getLocation() : super.getLocationOnScreen();
	}

	@Override
	public boolean isShowing() {
		return overlay != null || super.isShowing();
	}

	@Override
	public boolean isVisible() {
		return overlay != null || super.isVisible();
	}

	/// Moves the content pane into a new overlay; falls back to a regular window on
	/// failure.
	///
	/// @param connection the Wayland connection
	/// @param pane the content pane
	private void openOverlay(final Connection connection, final JComponent pane) {
		super.setVisible(false);
		overlayContent = pane;
		super.setContentPane(new JPanel());

		try {
			final var initialBounds = getBounds();
			initialBounds.setSize(constrainWidth(initialBounds.width), constrainHeight(initialBounds.height));
			final var newOverlay = takeIdleOverlay();
			overlay = newOverlay;
			newOverlay.open(connection, pane, initialBounds);

			for (final var listener : getMouseListeners()) {
				newOverlay.surfacePanel.addMouseListener(listener);
			}
			for (final var listener : getMouseMotionListeners()) {
				newOverlay.surfacePanel.addMouseMotionListener(listener);
			}
			for (final var listener : getMouseWheelListeners()) {
				newOverlay.surfacePanel.addMouseWheelListener(listener);
			}

			newOverlay.surfacePanel.setBorder(getRootPane().getBorder());
			rootBorderMirror = (final var event) -> newOverlay.surfacePanel.setBorder((Border) event.getNewValue());
			getRootPane().addPropertyChangeListener("border", rootBorderMirror);

			super.setBounds(newOverlay.bounds.x, newOverlay.bounds.y, newOverlay.bounds.width,
					newOverlay.bounds.height);
		} catch (final RuntimeException e) {
			logger.log(Level.WARNING, "Could not open overlay, showing a regular window", e);
			closeOverlay();
			super.setVisible(true);
		}
	}

	/// Detaches the frame from the overlay and gives the content pane back to the
	/// frame; the frame stays hidden.
	private void overlayClosed() {
		final var closedOverlay = overlay;
		overlay = null;
		if (closedOverlay != null) {
			for (final var listener : getMouseListeners()) {
				closedOverlay.surfacePanel.removeMouseListener(listener);
			}
			for (final var listener : getMouseMotionListeners()) {
				closedOverlay.surfacePanel.removeMouseMotionListener(listener);
			}
			for (final var listener : getMouseWheelListeners()) {
				closedOverlay.surfacePanel.removeMouseWheelListener(listener);
			}
			idleOverlay = closedOverlay;
		}

		if (rootBorderMirror != null) {
			getRootPane().removePropertyChangeListener("border", rootBorderMirror);
			rootBorderMirror = null;
		}

		final var content = overlayContent;
		overlayContent = null;
		if (content != null) {
			super.setContentPane(content);
		}
	}

	@Override
	public void pack() {
		if (overlay == null) {
			super.pack();
			return;
		}

		final var preferredSize = overlay.surfacePanel.getPreferredSize();
		setSize(preferredSize.width, preferredSize.height);
		overlay.validateContent();
	}

	/// Prevents deserialization.
	///
	/// @param ignoredStream unused stream parameter
	/// @throws NotSerializableException always
	@Serial
	private void readObject(final ObjectInputStream ignoredStream) throws NotSerializableException {
		throw new NotSerializableException(OverlayFrame.class.getName());
	}

	@Override
	public synchronized void removeMouseListener(final MouseListener listener) {
		super.removeMouseListener(listener);
		if (overlay != null) {
			overlay.surfacePanel.removeMouseListener(listener);
		}
	}

	@Override
	public synchronized void removeMouseMotionListener(final MouseMotionListener listener) {
		super.removeMouseMotionListener(listener);
		if (overlay != null) {
			overlay.surfacePanel.removeMouseMotionListener(listener);
		}
	}

	@Override
	public synchronized void removeMouseWheelListener(final MouseWheelListener listener) {
		super.removeMouseWheelListener(listener);
		if (overlay != null) {
			overlay.surfacePanel.removeMouseWheelListener(listener);
		}
	}

	@Override
	public void repaint(final long time, final int x, final int y, final int width, final int height) {
		if (overlayContent != null) {
			overlayContent.repaint(time, x, y, width, height);
		} else {
			super.repaint(time, x, y, width, height);
		}
	}

	@Override
	public void setBounds(final int x, final int y, final int width, final int height) {
		if (overlay == null) {
			super.setBounds(x, y, width, height);
			return;
		}

		final var actualBounds = overlay.setBounds(new Rectangle(x, y, constrainWidth(width), constrainHeight(height)));
		super.setBounds(actualBounds.x, actualBounds.y, actualBounds.width, actualBounds.height);
	}

	@Override
	public void setContentPane(final Container contentPane) {
		if (overlay == null) {
			super.setContentPane(contentPane);
			return;
		}

		closeOverlay();
		super.setContentPane(contentPane);
		setVisible(true);
	}

	@Override
	public void setVisible(final boolean visible) {
		if (!visible) {
			if (overlay != null) {
				closeOverlay();
			} else {
				super.setVisible(false);
			}
			return;
		}

		if (overlay != null) {
			return;
		}

		if (MainFrame.IS_WAYLAND_TOOLKIT) {
			final var connection = Connection.getInstance();
			if (connection == null || Wayland.isNull(connection.layerShell)
					|| !(super.getContentPane() instanceof final JComponent pane)) {
				super.setVisible(true);
				return;
			}

			openOverlay(connection, pane);
			return;
		}

		super.setVisible(true);
	}

	/// Returns the idle overlay if it matches the current background, otherwise
	/// disposes it and creates a new one.
	///
	/// @return a closed overlay
	private Overlay takeIdleOverlay() {
		final var reusableOverlay = idleOverlay;
		idleOverlay = null;
		if (reusableOverlay != null) {
			if (Objects.equals(reusableOverlay.background, getBackground())) {
				return reusableOverlay;
			}
			reusableOverlay.dispose();
		}

		return new Overlay();
	}

	@Override
	public void validate() {
		if (overlay != null) {
			overlay.validateContent();
		} else {
			super.validate();
		}
	}

	/// Prevents serialization.
	///
	/// @param ignoredStream unused stream parameter
	/// @throws NotSerializableException always
	@Serial
	private void writeObject(final ObjectOutputStream ignoredStream) throws NotSerializableException {
		throw new NotSerializableException(OverlayFrame.class.getName());
	}

	/// The Wayland globals used by overlays, bound on JBR's `wl_display`.
	///
	/// The globals are bound on a temporary event queue, so the blocking roundtrips
	/// do not dispatch JBR's own events. Then all proxies are moved to the default
	/// queue, which JBR dispatches on the event dispatch thread; all listeners
	/// therefore run on the EDT.
	private static final class Connection {

		/// Guards [#instance] and [#initialized].
		private static final Object INSTANCE_LOCK = new Object();

		/// Highest supported version of `wl_compositor`.
		private static final int MAX_COMPOSITOR_VERSION = 4;

		/// Highest supported version of `zwlr_layer_shell_v1`.
		private static final int MAX_LAYER_SHELL_VERSION = 4;

		/// Highest supported version of `wl_output`; version 4 adds the
		/// `wl_output.name` event.
		private static final int MAX_OUTPUT_VERSION = 4;

		/// Highest supported version of `wl_seat`.
		private static final int MAX_SEAT_VERSION = 5;

		/// Whether the first attempt to create [#instance] has been made.
		private static boolean initialized;

		/// The connection on JBR's display, or `null` if it could not be created.
		private static @Nullable Connection instance;

		/// The display.
		private final MemorySegment display;

		/// Listener table for `wl_output`.
		private final MemorySegment outputListener;

		/// Names of the outputs, keyed by proxy address.
		private final Map<Long, String> outputNames = new HashMap<>();

		/// All bound outputs.
		private final List<MemorySegment> outputs = new ArrayList<>();

		/// Listener table for `wl_pointer`.
		private final MemorySegment pointerListener;

		/// Listener table for `wl_seat`.
		private final MemorySegment seatListener;

		/// Open layer surfaces, keyed by `wl_surface` address, for pointer routing.
		private final Map<Long, Overlay.LayerSurface> surfaces = new HashMap<>();

		/// The bound `wl_compositor`.
		private MemorySegment compositor = MemorySegment.NULL;

		/// The bound version of `wl_compositor`.
		private int compositorVersion;

		/// The bound `zwlr_layer_shell_v1`.
		private MemorySegment layerShell = MemorySegment.NULL;

		/// The pointer of [#seat].
		private MemorySegment pointer = MemorySegment.NULL;

		/// The layer surface under the pointer, or `null`.
		private Overlay.@Nullable LayerSurface pointerFocus;

		/// The bound `wl_seat`.
		private MemorySegment seat = MemorySegment.NULL;

		/// The bound version of `wl_seat`.
		private int seatVersion;

		/// The bound `wl_shm`.
		private MemorySegment shm = MemorySegment.NULL;

		/// Binds the globals and moves all proxies to the default queue.
		///
		/// @param display the display
		/// @throws IllegalStateException if a Wayland call fails or a required global
		/// is missing
		private Connection(final MemorySegment display) {
			this.display = display;

			final var lookup = MethodHandles.lookup();
			final var arena = Arena.global();
			final var registryListener = Wayland.listener(arena,
					Wayland.stub(lookup, this, "onGlobal", Wayland.V_PPIPI, arena), Wayland.noop(Wayland.V_PPI, arena));
			outputListener = Wayland.listener(arena, Wayland.noop(Wayland.V_PPIIIIIPPI, arena),
					Wayland.noop(Wayland.V_PPIIII, arena), Wayland.noop(Wayland.V_PP, arena),
					Wayland.noop(Wayland.V_PPI, arena),
					Wayland.stub(lookup, this, "onOutputName", Wayland.V_PPP, arena),
					Wayland.noop(Wayland.V_PPP, arena));
			seatListener = Wayland.listener(arena,
					Wayland.stub(lookup, this, "onSeatCapabilities", Wayland.V_PPI, arena),
					Wayland.noop(Wayland.V_PPP, arena));
			pointerListener = Wayland.listener(arena,
					Wayland.stub(lookup, this, "onPointerEnter", Wayland.V_PPIPII, arena),
					Wayland.stub(lookup, this, "onPointerLeave", Wayland.V_PPIP, arena),
					Wayland.stub(lookup, this, "onPointerMotion", Wayland.V_PPIII, arena),
					Wayland.stub(lookup, this, "onPointerButton", Wayland.V_PPIIII, arena),
					Wayland.stub(lookup, this, "onPointerAxis", Wayland.V_PPIII, arena),
					Wayland.stub(lookup, this, "onPointerFrame", Wayland.V_PP, arena),
					Wayland.noop(Wayland.V_PPI, arena), Wayland.noop(Wayland.V_PPII, arena),
					Wayland.stub(lookup, this, "onPointerAxisDiscrete", Wayland.V_PPII, arena));

			final var setupQueue = Wayland.wl_display_create_queue(display);
			if (Wayland.isNull(setupQueue)) {
				throw new IllegalStateException("wl_display_create_queue() failed");
			}

			final var wrapper = Wayland.wl_proxy_create_wrapper(display);
			Wayland.wl_proxy_set_queue(wrapper, setupQueue);
			final var registry = Wayland.create(wrapper, Wayland.WL_DISPLAY_GET_REGISTRY, Wayland.WL_REGISTRY_INTERFACE,
					Wayland.M_P, MemorySegment.NULL);
			Wayland.wl_proxy_add_listener(registry, registryListener, MemorySegment.NULL);
			roundtrip(setupQueue);
			roundtrip(setupQueue);

			final var proxies = new ArrayList<>(List.of(registry, compositor, shm, layerShell, seat, pointer));
			proxies.addAll(outputs);
			for (final var proxy : proxies) {
				if (!Wayland.isNull(proxy)) {
					Wayland.wl_proxy_set_queue(proxy, MemorySegment.NULL);
				}
			}

			if (Wayland.wl_display_dispatch_queue_pending(display, setupQueue) == -1) {
				throw new IllegalStateException("wl_display_dispatch_queue_pending() failed");
			}
			Wayland.wl_proxy_wrapper_destroy(wrapper);
			Wayland.wl_event_queue_destroy(setupQueue);

			if (Wayland.isNull(compositor) || Wayland.isNull(shm)) {
				throw new IllegalStateException("The compositor offers no wl_compositor or wl_shm");
			}
		}

		/// Returns the connection on JBR's display, creating it on first use.
		///
		/// A failure is logged once, after which null is returned without retrying.
		///
		/// @return the connection, or `null` if it could not be created
		private static @Nullable Connection getInstance() {
			synchronized (INSTANCE_LOCK) {
				if (!initialized) {
					initialized = true;
					try {
						instance = new Connection(MemorySegment.ofAddress(getJbrDisplayPointer()));
					} catch (final RuntimeException e) {
						logger.log(Level.WARNING, "Could not connect to JBR's Wayland display", e);
					}
				}

				return instance;
			}
		}

		/// Returns the `wl_display*` of JBR's Wayland toolkit.
		///
		/// Requires `--add-opens java.desktop/sun.awt.wl=ALL-UNNAMED`.
		///
		/// @return the display pointer
		/// @throws IllegalStateException if the pointer cannot be obtained
		private static long getJbrDisplayPointer() {
			try {
				@SuppressWarnings({ "Java9ReflectionClassVisibility", "RedundantSuppression" })
				final var displayClass = Class.forName("sun.awt.wl.WLDisplay", false, null);
				final var getInstanceMethod = displayClass.getDeclaredMethod("getInstance");
				getInstanceMethod.setAccessible(true);
				final var getDisplayPtrMethod = displayClass.getDeclaredMethod("getDisplayPtr");
				getDisplayPtrMethod.setAccessible(true);

				return (long) getDisplayPtrMethod.invoke(getInstanceMethod.invoke(null));
			} catch (final ReflectiveOperationException | RuntimeException e) {
				throw new IllegalStateException("Could not obtain the wl_display of JBR", e);
			}
		}

		/// Sends pending requests now instead of waiting for JBR's next flush.
		private void flush() {
			Wayland.wl_display_flush(display);
		}

		/// Handles `wl_registry.global` and binds the needed globals.
		///
		/// @param data unused user data
		/// @param registry the registry
		/// @param name the numeric name of the global
		/// @param iface the interface name as C string
		/// @param version the interface version
		@SuppressWarnings("unused")
		private void onGlobal(final MemorySegment data, final MemorySegment registry, final int name,
				final MemorySegment iface, final int version) {
			handleEvent("wl_registry.global", () -> {
				switch (iface.reinterpret(Long.MAX_VALUE).getString(0)) {
				case "wl_compositor" -> {
					if (Wayland.isNull(compositor)) {
						compositorVersion = Math.min(version, MAX_COMPOSITOR_VERSION);
						compositor = Wayland.bind(registry, name, Wayland.WL_COMPOSITOR_INTERFACE, compositorVersion);
					}
				}
				case "wl_output" -> {
					final var output = Wayland.bind(registry, name, Wayland.WL_OUTPUT_INTERFACE,
							Math.min(version, MAX_OUTPUT_VERSION));
					Wayland.wl_proxy_add_listener(output, outputListener, MemorySegment.NULL);
					outputs.add(output);
				}
				case "wl_seat" -> {
					if (Wayland.isNull(seat)) {
						seatVersion = Math.min(version, MAX_SEAT_VERSION);
						seat = Wayland.bind(registry, name, Wayland.WL_SEAT_INTERFACE, seatVersion);
						Wayland.wl_proxy_add_listener(seat, seatListener, MemorySegment.NULL);
					}
				}
				case "wl_shm" -> {
					if (Wayland.isNull(shm)) {
						shm = Wayland.bind(registry, name, Wayland.WL_SHM_INTERFACE, 1);
					}
				}
				case Wayland.ZWLR_LAYER_SHELL_V1_NAME -> {
					if (Wayland.isNull(layerShell)) {
						layerShell = Wayland.bind(registry, name, Wayland.ZWLR_LAYER_SHELL_V1_INTERFACE,
								Math.min(version, MAX_LAYER_SHELL_VERSION));
					}
				}
				default -> {
				}
				}
			});
		}

		/// Handles `wl_output.name`.
		///
		/// @param data unused user data
		/// @param output the output
		/// @param name the name as C string
		@SuppressWarnings("unused")
		private void onOutputName(final MemorySegment data, final MemorySegment output, final MemorySegment name) {
			handleEvent("wl_output.name",
					() -> outputNames.put(output.address(), name.reinterpret(Long.MAX_VALUE).getString(0)));
		}

		/// Handles `wl_pointer.axis`; only the vertical axis is used.
		///
		/// @param data unused user data
		/// @param wlPointer the pointer
		/// @param time the timestamp
		/// @param axis the axis
		/// @param value the value as `wl_fixed_t`
		@SuppressWarnings("unused")
		private void onPointerAxis(final MemorySegment data, final MemorySegment wlPointer, final int time,
				final int axis, final int value) {
			handleEvent("wl_pointer.axis", () -> {
				if (pointerFocus != null && axis == Wayland.WL_POINTER_AXIS_VERTICAL_SCROLL) {
					pointerFocus.pointerAxis(Wayland.wl_fixed_to_double(value));
				}
			});
		}

		/// Handles `wl_pointer.axis_discrete`; only the vertical axis is used.
		///
		/// @param data unused user data
		/// @param wlPointer the pointer
		/// @param axis the axis
		/// @param discrete the number of wheel steps
		@SuppressWarnings("unused")
		private void onPointerAxisDiscrete(final MemorySegment data, final MemorySegment wlPointer, final int axis,
				final int discrete) {
			handleEvent("wl_pointer.axis_discrete", () -> {
				if (pointerFocus != null && axis == Wayland.WL_POINTER_AXIS_VERTICAL_SCROLL) {
					pointerFocus.pointerAxisDiscrete(discrete);
				}
			});
		}

		/// Handles `wl_pointer.button`.
		///
		/// @param data unused user data
		/// @param wlPointer the pointer
		/// @param serial the serial
		/// @param time the timestamp
		/// @param button the Linux button code
		/// @param state the button state
		@SuppressWarnings("unused")
		private void onPointerButton(final MemorySegment data, final MemorySegment wlPointer, final int serial,
				final int time, final int button, final int state) {
			handleEvent("wl_pointer.button", () -> {
				if (pointerFocus != null) {
					pointerFocus.pointerButton(button, state == Wayland.WL_POINTER_BUTTON_STATE_PRESSED);
				}
			});
		}

		/// Handles `wl_pointer.enter`; only surfaces of overlays are of interest.
		///
		/// @param data unused user data
		/// @param wlPointer the pointer
		/// @param serial the serial
		/// @param surface the entered surface
		/// @param x the surface x coordinate as `wl_fixed_t`
		/// @param y the surface y coordinate as `wl_fixed_t`
		@SuppressWarnings("unused")
		private void onPointerEnter(final MemorySegment data, final MemorySegment wlPointer, final int serial,
				final MemorySegment surface, final int x, final int y) {
			handleEvent("wl_pointer.enter", () -> {
				pointerFocus = Wayland.isNull(surface) ? null : surfaces.get(surface.address());
				if (pointerFocus != null) {
					pointerFocus.pointerEnter(Wayland.wl_fixed_to_double(x), Wayland.wl_fixed_to_double(y));
				}
			});
		}

		/// Handles `wl_pointer.frame`.
		///
		/// @param data unused user data
		/// @param wlPointer the pointer
		@SuppressWarnings("unused")
		private void onPointerFrame(final MemorySegment data, final MemorySegment wlPointer) {
			handleEvent("wl_pointer.frame", () -> {
				if (pointerFocus != null) {
					pointerFocus.pointerFrame();
				}
			});
		}

		/// Handles `wl_pointer.leave`.
		///
		/// @param data unused user data
		/// @param wlPointer the pointer
		/// @param serial the serial
		/// @param surface the left surface
		@SuppressWarnings("unused")
		private void onPointerLeave(final MemorySegment data, final MemorySegment wlPointer, final int serial,
				final MemorySegment surface) {
			handleEvent("wl_pointer.leave", () -> {
				final var previousFocus = pointerFocus;
				pointerFocus = null;
				if (previousFocus != null) {
					previousFocus.pointerLeave();
				}
			});
		}

		/// Handles `wl_pointer.motion`.
		///
		/// @param data unused user data
		/// @param wlPointer the pointer
		/// @param time the timestamp
		/// @param x the surface x coordinate as `wl_fixed_t`
		/// @param y the surface y coordinate as `wl_fixed_t`
		@SuppressWarnings("unused")
		private void onPointerMotion(final MemorySegment data, final MemorySegment wlPointer, final int time,
				final int x, final int y) {
			handleEvent("wl_pointer.motion", () -> {
				if (pointerFocus != null) {
					pointerFocus.pointerMotion(Wayland.wl_fixed_to_double(x), Wayland.wl_fixed_to_double(y));
				}
			});
		}

		/// Handles `wl_seat.capabilities` and creates or releases the pointer.
		///
		/// @param data unused user data
		/// @param wlSeat the seat
		/// @param capabilities the capability bits
		@SuppressWarnings("unused")
		private void onSeatCapabilities(final MemorySegment data, final MemorySegment wlSeat, final int capabilities) {
			handleEvent("wl_seat.capabilities", () -> {
				final var hasPointer = (capabilities & Wayland.WL_SEAT_CAPABILITY_POINTER) != 0;
				if (hasPointer && Wayland.isNull(pointer)) {
					pointer = Wayland.create(wlSeat, Wayland.WL_SEAT_GET_POINTER, Wayland.WL_POINTER_INTERFACE,
							Wayland.M_P, MemorySegment.NULL);
					Wayland.wl_proxy_add_listener(pointer, pointerListener, MemorySegment.NULL);
				} else if (!hasPointer && !Wayland.isNull(pointer)) {
					if (seatVersion >= 3) {
						Wayland.destroy(pointer, Wayland.WL_POINTER_RELEASE);
					} else {
						Wayland.wl_proxy_destroy(pointer);
					}
					pointer = MemorySegment.NULL;
					pointerFocus = null;
				}
			});
		}

		/// Performs a blocking roundtrip on the given queue.
		///
		/// @param queue the event queue
		/// @throws IllegalStateException if the roundtrip fails
		private void roundtrip(final MemorySegment queue) {
			if (Wayland.wl_display_roundtrip_queue(display, queue) < 0) {
				throw new IllegalStateException(
						"wl_display_roundtrip_queue() failed, errno " + Wayland.wl_display_get_error(display));
			}
		}
	}

	/// Renders the content pane into a layer surface and feeds pointer input back
	/// as Swing events.
	///
	/// The content pane is placed in a lightweight frame, whose pixels are copied
	/// into the layer surface. Pointer events on the overlay are delivered as
	/// regular [MouseEvent]s with correct screen coordinates.
	private final class Overlay {

		/// The frame background the overlay was created for.
		private final @Nullable Color background;

		/// The current screen bounds of the overlay.
		private final Rectangle bounds = new Rectangle();

		/// Pixel source backed by the lightweight frame's buffer.
		private final Content contentWrapper;

		/// The bounds of the monitor the overlay lives on.
		private final Rectangle deviceBounds = new Rectangle();

		/// The lightweight frame that renders the content.
		private final LightweightFrameWrapper lightweightFrame;

		/// The root of the overlay content; target for mouse listeners of the frame
		/// itself.
		private final SurfacePanel surfacePanel;

		/// Whether the overlay is closed.
		private boolean closed = true;

		/// The content pane shown in the overlay, or `null` while it is closed.
		private @Nullable JComponent content;

		/// The layer surface, or `null` while the overlay is closed.
		private @Nullable LayerSurface layer;

		/// Translates pointer events into Swing events.
		private MouseTranslator mouseTranslator = new MouseTranslator();

		/// Creates a closed overlay with its lightweight frame; [#open] shows it.
		///
		/// @throws IllegalStateException if not called on the event dispatch thread
		private Overlay() {
			if (!EventQueue.isDispatchThread()) {
				throw new IllegalStateException("Must be called on the event dispatch thread");
			}

			background = getBackground();
			surfacePanel = new SurfacePanel(background);

			lightweightFrame = new LightweightFrameWrapper();
			contentWrapper = new Content();
			lightweightFrame.setContent(contentWrapper);
			if (background != null && background.getAlpha() < 255) {
				final var window = SwingUtilities.getWindowAncestor(surfacePanel);
				if (window != null) {
					try {
						window.setBackground(background);
					} catch (final Exception e) {
						logger.log(Level.WARNING, "Translucency is not available in the overlay", e);
					}
				}
			}
			lightweightFrame.setVisible(true);
		}

		/// Finds the `wl_output` for a JBR graphics device by name.
		///
		/// @param connection the connection
		/// @param name the device name ([GraphicsDevice#getIDstring()])
		/// @return the output, the only output if there is just one, or `NULL` to let
		/// the compositor choose
		private static MemorySegment findOutput(final Connection connection, final @Nullable String name) {
			for (final var output : connection.outputs) {
				if (name != null && name.equals(connection.outputNames.get(output.address()))) {
					return output;
				}
			}

			if (connection.outputs.size() == 1) {
				return connection.outputs.getFirst();
			}

			logger.warning("No wl_output named " + name);
			return MemorySegment.NULL;
		}

		/// Returns the monitor containing a point, or the default monitor.
		///
		/// @param point the point
		/// @return the graphics device
		private static GraphicsDevice getDeviceAt(final Point point) {
			final var graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
			for (final var device : graphicsEnvironment.getScreenDevices()) {
				if (device.getDefaultConfiguration().getBounds().contains(point)) {
					return device;
				}
			}

			return graphicsEnvironment.getDefaultScreenDevice();
		}

		/// Clamps a location so the overlay stays on its monitor.
		///
		/// @param location the requested location
		/// @return the clamped location
		private Point clamp(final Point location) {
			final var maxX = deviceBounds.x + Math.max(0, deviceBounds.width - bounds.width);
			final var maxY = deviceBounds.y + Math.max(0, deviceBounds.height - bounds.height);

			return new Point(Math.clamp(location.x, deviceBounds.x, maxX),
					Math.clamp(location.y, deviceBounds.y, maxY));
		}

		/// Closes the overlay and releases the content pane; the overlay can be opened
		/// again.
		private void close() {
			if (closed) {
				return;
			}

			if (layer != null) {
				layer.destroy();
			}
			release();
		}

		/// Closes the overlay and disposes the lightweight frame.
		private void dispose() {
			close();
			lightweightFrame.dispose();
		}

		/// Called when the compositor closed the layer surface: releases the content
		/// pane and detaches the frame, which stays hidden.
		private void layerClosed() {
			if (closed) {
				return;
			}

			release();
			overlayClosed();
		}

		/// Shows the content pane on the monitor containing
		/// `requestedBounds.getLocation()`.
		///
		/// @param connection the Wayland connection
		/// @param newContent the content pane to show
		/// @param requestedBounds the requested screen bounds; an empty size means the
		/// preferred size
		private void open(final Connection connection, final JComponent newContent, final Rectangle requestedBounds) {
			content = newContent;
			closed = false;
			mouseTranslator = new MouseTranslator();
			surfacePanel.add(newContent, BorderLayout.CENTER);

			final var preferredSize = newContent.getPreferredSize();
			final var width = requestedBounds.width > 0 ? requestedBounds.width : Math.max(1, preferredSize.width);
			final var height = requestedBounds.height > 0 ? requestedBounds.height : Math.max(1, preferredSize.height);
			final var device = getDeviceAt(requestedBounds.getLocation());
			final var graphicsConfiguration = device.getDefaultConfiguration();
			deviceBounds.setBounds(graphicsConfiguration.getBounds());
			final var scale = (int) Math.max(1, Math.round(graphicsConfiguration.getDefaultTransform().getScaleX()));
			bounds.setBounds(0, 0, width, height);
			bounds.setLocation(clamp(requestedBounds.getLocation()));

			lightweightFrame.setBounds(0, 0, width, height);
			lightweightFrame.notifyDisplayChanged(scale, scale);
			lightweightFrame.setHostBounds(bounds.x, bounds.y, width, height);
			validateContent();

			layer = new LayerSurface(connection, findOutput(connection, device.getIDstring()),
					bounds.x - deviceBounds.x, bounds.y - deviceBounds.y, width, height);
		}

		/// Detaches the content pane and forgets the layer surface.
		private void release() {
			closed = true;
			layer = null;
			if (content != null) {
				surfacePanel.remove(content);
				content = null;
			}
		}

		/// Moves and resizes the overlay; the location is clamped to the overlay's
		/// monitor.
		///
		/// @param requestedBounds the requested screen bounds
		/// @return the bounds that apply now
		private Rectangle setBounds(final Rectangle requestedBounds) {
			if (closed || layer == null) {
				return new Rectangle(bounds);
			}

			final var width = Math.max(1, requestedBounds.width);
			final var height = Math.max(1, requestedBounds.height);
			if (width != bounds.width || height != bounds.height) {
				bounds.setSize(width, height);
				lightweightFrame.setBounds(0, 0, width, height);
				layer.setSize(width, height);
			}

			final var location = clamp(requestedBounds.getLocation());
			if (!location.equals(bounds.getLocation())) {
				bounds.setLocation(location);
				layer.setMargins(location.x - deviceBounds.x, location.y - deviceBounds.y);
			}

			lightweightFrame.setHostBounds(bounds.x, bounds.y, bounds.width, bounds.height);

			return new Rectangle(bounds);
		}

		/// Lays out and repaints the content, as [java.awt.Window#pack()] and
		/// [java.awt.Container#validate()] do for a real window.
		private void validateContent() {
			final var rootPane = SwingUtilities.getRootPane(surfacePanel);
			final var target = rootPane != null ? rootPane : surfacePanel;
			target.validate();
			target.repaint();
		}

		/// Root of the overlay content.
		///
		/// Covers every painted area with the frame background (alpha 0 for a
		/// translucent frame) and thereby honors the opaque contract. Swing therefore
		/// starts painting here, and no stale pixels of earlier frames remain in the
		/// buffer.
		private static final class SurfacePanel extends JPanel {

			@Serial
			private static final long serialVersionUID = -3150547441946637004L;

			/// The fill color.
			private final Color fill;

			/// Creates the panel.
			///
			/// @param background the frame background, or `null` for transparent
			private SurfacePanel(final @Nullable Color background) {
				super(new BorderLayout());
				fill = background != null ? background : new Color(0, 0, 0, 0);
				setOpaque(true);
			}

			@Override
			protected void paintComponent(final Graphics g) {
				final var g2d = (Graphics2D) g.create();
				try {
					g2d.setComposite(AlphaComposite.Src);
					g2d.setColor(fill);
					g2d.fillRect(0, 0, getWidth(), getHeight());
				} finally {
					g2d.dispose();
				}
			}
		}

		/// Pixel source backed by the buffer of the lightweight frame.
		///
		/// All callbacks of the lightweight frame and all reads by the layer surface
		/// happen on the event dispatch thread, so no locking is needed.
		private final class Content extends LightweightContentWrapper {

			/// The scale the buffer was rendered at.
			private double bufferScale = 1;

			/// The pixels in `TYPE_INT_ARGB_PRE`, or `null` before the first paint.
			private int @Nullable [] data;

			/// The logical height of the image.
			private int logicalHeight;

			/// The logical width of the image.
			private int logicalWidth;

			/// The logical x offset of the image in the buffer.
			private int offsetX;

			/// The logical y offset of the image in the buffer.
			private int offsetY;

			/// The number of pixels per buffer row.
			private int stride;

			/// Creates the pixel source.
			private Content() {
			}

			/// Drag and drop is not supported in the overlay.
			///
			/// @param target the drop target
			@Override
			public void addDropTarget(final DropTarget target) {
			}

			/// Drag and drop is not supported in the overlay.
			///
			/// @param type the recognizer type
			/// @param dragSource the drag source
			/// @param component the component
			/// @param actions the allowed actions
			/// @param listener the listener
			/// @param <T> the recognizer type
			/// @return always `null`
			@Override
			public <T extends DragGestureRecognizer> @Nullable T createDragGestureRecognizer(final Class<T> type,
					final DragSource dragSource, final Component component, final int actions,
					final DragGestureListener listener) {
				return null;
			}

			/// Drag and drop is not supported in the overlay.
			///
			/// @param event the drag gesture
			/// @return never returns normally
			/// @throws InvalidDnDOperationException always
			@Override
			public DragSourceContextWrapper createDragSourceContext(final DragGestureEvent event)
					throws InvalidDnDOperationException {
				throw new InvalidDnDOperationException("Drag and drop is not supported in the overlay");
			}

			/// Focus changes are not relevant: the overlay never takes keyboard focus.
			@Override
			public void focusGrabbed() {
			}

			/// Focus changes are not relevant: the overlay never takes keyboard focus.
			@Override
			public void focusUngrabbed() {
			}

			/// Returns the root of the overlay content.
			///
			/// @return the component to render
			@Override
			public JComponent getComponent() {
				return surfacePanel;
			}

			/// Receives a new buffer at scale 1.
			///
			/// @param newData the pixels
			/// @param x the logical x offset
			/// @param y the logical y offset
			/// @param width the logical width
			/// @param height the logical height
			/// @param linestride the number of pixels per row
			@Override
			public void imageBufferReset(final int[] newData, final int x, final int y, final int width,
					final int height, final int linestride) {
				imageBufferReset(newData, x, y, width, height, linestride, 1, 1);
			}

			/// Receives a new buffer.
			///
			/// @param newData the pixels
			/// @param x the logical x offset
			/// @param y the logical y offset
			/// @param width the logical width
			/// @param height the logical height
			/// @param linestride the number of pixels per row
			/// @param scaleX the horizontal scale
			/// @param scaleY the vertical scale
			@Override
			public void imageBufferReset(final int[] newData, final int x, final int y, final int width,
					final int height, final int linestride, final double scaleX, final double scaleY) {
				data = newData;
				offsetX = x;
				offsetY = y;
				logicalWidth = width;
				logicalHeight = height;
				stride = linestride;
				bufferScale = scaleX;
				requestFrame();
			}

			/// Receives a new image size within the existing buffer.
			///
			/// @param x the logical x offset
			/// @param y the logical y offset
			/// @param width the logical width
			/// @param height the logical height
			@Override
			public void imageReshaped(final int x, final int y, final int width, final int height) {
				offsetX = x;
				offsetY = y;
				logicalWidth = width;
				logicalHeight = height;
				requestFrame();
			}

			/// Schedules a new frame after a repaint.
			///
			/// @param dirtyX the x coordinate of the dirty region
			/// @param dirtyY the y coordinate of the dirty region
			/// @param dirtyWidth the width of the dirty region
			/// @param dirtyHeight the height of the dirty region
			@Override
			public void imageUpdated(final int dirtyX, final int dirtyY, final int dirtyWidth, final int dirtyHeight) {
				requestFrame();
			}

			/// Size hints are not used: the frame owns the overlay size.
			///
			/// @param width the maximum width
			/// @param height the maximum height
			@Override
			public void maximumSizeChanged(final int width, final int height) {
			}

			/// Size hints are not used: the frame owns the overlay size.
			///
			/// @param width the minimum width
			/// @param height the minimum height
			@Override
			public void minimumSizeChanged(final int width, final int height) {
			}

			/// Not needed: painting and copying both happen on the event dispatch thread.
			@Override
			public void paintLock() {
			}

			/// Not needed: painting and copying both happen on the event dispatch thread.
			@Override
			public void paintUnlock() {
			}

			/// Size hints are not used: the frame owns the overlay size.
			///
			/// @param width the preferred width
			/// @param height the preferred height
			@Override
			public void preferredSizeChanged(final int width, final int height) {
			}

			/// Hands the current image to a layer surface.
			///
			/// The reported scale is the one the buffer was actually rendered at; reporting
			/// the intended scale instead would give the surface a wrong size.
			///
			/// @param target the layer surface that receives the image
			private void presentTo(final LayerSurface target) {
				final var currentData = data;
				if (currentData == null) {
					return;
				}

				final var physicalWidth = Math.min(stride, (int) Math.round(logicalWidth * bufferScale));
				final var physicalHeight = (int) Math.round(logicalHeight * bufferScale);
				final var offset = (int) Math.round(offsetY * bufferScale) * stride
						+ (int) Math.round(offsetX * bufferScale);
				if (physicalHeight <= 0
						|| offset + (long) (physicalHeight - 1) * stride + physicalWidth > currentData.length) {
					return;
				}

				final var actualScale = (int) Math.round(bufferScale);
				if (actualScale < 1 || Math.abs(bufferScale - actualScale) > 0.01) {
					logger.warning("Skipping frame with non-integer buffer scale " + bufferScale);
					return;
				}

				target.present(currentData, offset, stride, physicalWidth, physicalHeight, actualScale);
			}

			/// Drag and drop is not supported in the overlay.
			///
			/// @param target the drop target
			@Override
			public void removeDropTarget(final DropTarget target) {
			}

			/// Schedules a new frame unless the layer surface does not exist yet or is
			/// closed.
			private void requestFrame() {
				if (layer != null && !closed) {
					layer.requestFrame();
				}
			}
		}

		/// A `wl_surface` with the role `zwlr_layer_surface_v1` in the overlay layer,
		/// anchored in the top left corner of its monitor and positioned through its
		/// margins.
		///
		/// All methods run on the event dispatch thread.
		private final class LayerSurface {

			/// Number of buffers for double buffering.
			private static final int MAX_BUFFERS = 2;

			/// Arena for the listener tables of this surface.
			private final Arena arena = Arena.ofShared();

			/// Listener table for `wl_buffer`.
			private final MemorySegment bufferListener;

			/// The allocated buffers.
			private final List<ShmBuffer> buffers = new ArrayList<>();

			/// The connection.
			private final Connection connection;

			/// Listener table for the `wl_callback` of `wl_surface.frame`.
			private final MemorySegment frameListener;

			/// The `zwlr_layer_surface_v1`.
			private final MemorySegment layerSurface;

			/// Margin changes in the order they were sent.
			private final ArrayDeque<PendingMargins> pendingMargins = new ArrayDeque<>();

			/// The `wl_surface`.
			private final MemorySegment surface;

			/// Listener table for `wl_callback`.
			private final MemorySegment syncListener;

			/// The left margin that incoming pointer coordinates refer to.
			private int appliedMarginX;

			/// The top margin that incoming pointer coordinates refer to.
			private int appliedMarginY;

			/// The buffer scale last sent to the compositor.
			private int bufferScale = 1;

			/// Whether the first configure has been acknowledged.
			private boolean configured;

			/// Whether the surface has been destroyed.
			private boolean destroyed;

			/// The `wl_callback` of the last `wl_surface.frame` request, or `null` if the
			/// compositor is ready for the next frame.
			private @Nullable MemorySegment frameCallback;

			/// Whether a frame is waiting for the compositor or for a free buffer.
			private boolean framePending;

			/// Whether a frame is scheduled with [SwingUtilities#invokeLater(Runnable)].
			private boolean frameScheduled;

			/// Creates the surface and performs the initial commit without a buffer.
			///
			/// @param connection the connection
			/// @param output the monitor, or `NULL` to let the compositor choose
			/// @param marginX the initial left margin
			/// @param marginY the initial top margin
			/// @param width the width
			/// @param height the height
			private LayerSurface(final Connection connection, final MemorySegment output, final int marginX,
					final int marginY, final int width, final int height) {
				this.connection = connection;
				appliedMarginX = marginX;
				appliedMarginY = marginY;

				final var lookup = MethodHandles.lookup();
				final var layerSurfaceListener = Wayland.listener(arena,
						Wayland.stub(lookup, this, "onConfigure", Wayland.V_PPIII, arena),
						Wayland.stub(lookup, this, "onClosed", Wayland.V_PP, arena));
				bufferListener = Wayland.listener(arena,
						Wayland.stub(lookup, this, "onBufferRelease", Wayland.V_PP, arena));
				syncListener = Wayland.listener(arena, Wayland.stub(lookup, this, "onSyncDone", Wayland.V_PPI, arena));
				frameListener = Wayland.listener(arena,
						Wayland.stub(lookup, this, "onFrameDone", Wayland.V_PPI, arena));

				surface = Wayland.create(connection.compositor, Wayland.WL_COMPOSITOR_CREATE_SURFACE,
						Wayland.WL_SURFACE_INTERFACE, Wayland.M_P, MemorySegment.NULL);
				layerSurface = Wayland.create(connection.layerShell, Wayland.ZWLR_LAYER_SHELL_V1_GET_LAYER_SURFACE,
						Wayland.ZWLR_LAYER_SURFACE_V1_INTERFACE, Wayland.M_PPPIP, MemorySegment.NULL, surface, output,
						Wayland.ZWLR_LAYER_SHELL_V1_LAYER_OVERLAY,
						arena.allocateFrom(System.getProperty("awt.app.id", "java-overlay")));
				Wayland.wl_proxy_add_listener(layerSurface, layerSurfaceListener, MemorySegment.NULL);

				Wayland.send(layerSurface, Wayland.ZWLR_LAYER_SURFACE_V1_SET_SIZE, Wayland.M_II, width, height);
				Wayland.send(layerSurface, Wayland.ZWLR_LAYER_SURFACE_V1_SET_ANCHOR, Wayland.M_I,
						Wayland.ZWLR_LAYER_SURFACE_V1_ANCHOR_TOP | Wayland.ZWLR_LAYER_SURFACE_V1_ANCHOR_LEFT);
				Wayland.send(layerSurface, Wayland.ZWLR_LAYER_SURFACE_V1_SET_EXCLUSIVE_ZONE, Wayland.M_I, -1);
				Wayland.send(layerSurface, Wayland.ZWLR_LAYER_SURFACE_V1_SET_MARGIN, Wayland.M_IIII, marginY, 0, 0,
						marginX);
				Wayland.send(layerSurface, Wayland.ZWLR_LAYER_SURFACE_V1_SET_KEYBOARD_INTERACTIVITY, Wayland.M_I,
						Wayland.ZWLR_LAYER_SURFACE_V1_KEYBOARD_INTERACTIVITY_NONE);

				Wayland.send(surface, Wayland.WL_SURFACE_COMMIT, Wayland.M_P, MemorySegment.NULL);

				connection.surfaces.put(surface.address(), this);
				connection.flush();
			}

			/// Allocates and registers a new buffer.
			///
			/// @param width the width
			/// @param height the height
			/// @return the buffer
			private ShmBuffer allocateBuffer(final int width, final int height) {
				final var buffer = new ShmBuffer(connection.shm, width, height, bufferListener);
				buffers.add(buffer);

				return buffer;
			}

			/// Destroys the surface.
			private void destroy() {
				if (destroyed) {
					return;
				}

				destroyProxies();
				connection.flush();
			}

			/// Destroys all Wayland objects of this surface.
			///
			/// The arena is closed later, because this may run inside one of its own upcall
			/// stubs (for example `onClosed`).
			private void destroyProxies() {
				destroyed = true;

				connection.surfaces.remove(surface.address());
				if (connection.pointerFocus == this) {
					connection.pointerFocus = null;
				}

				if (frameCallback != null) {
					Wayland.wl_proxy_destroy(frameCallback);
					frameCallback = null;
				}

				for (final var pendingMargin : pendingMargins) {
					Wayland.wl_proxy_destroy(pendingMargin.callback());
				}
				pendingMargins.clear();

				Wayland.destroy(layerSurface, Wayland.ZWLR_LAYER_SURFACE_V1_DESTROY);
				for (final var buffer : buffers) {
					buffer.destroy();
				}
				buffers.clear();
				Wayland.destroy(surface, Wayland.WL_SURFACE_DESTROY);

				SwingUtilities.invokeLater(arena::close);
			}

			/// Returns a free buffer of the given size.
			///
			/// @param width the width
			/// @param height the height
			/// @return the buffer, or `null` if there is none
			private @Nullable ShmBuffer findFreeBuffer(final int width, final int height) {
				for (final var buffer : buffers) {
					if (!buffer.busy && buffer.width == width && buffer.height == height) {
						return buffer;
					}
				}

				return null;
			}

			/// Handles `wl_buffer.release` and renders a pending frame.
			///
			/// @param data unused user data
			/// @param wlBuffer the released buffer
			@SuppressWarnings("unused")
			private void onBufferRelease(final MemorySegment data, final MemorySegment wlBuffer) {
				handleEvent("wl_buffer.release", () -> {
					for (final var buffer : buffers) {
						if (buffer.wlBuffer.address() == wlBuffer.address()) {
							buffer.busy = false;
						}
					}

					if (framePending) {
						render();
					}
				});
			}

			/// Handles `zwlr_layer_surface_v1.closed`.
			///
			/// @param data unused user data
			/// @param zwlrLayerSurfaceV1 the layer surface
			@SuppressWarnings("unused")
			private void onClosed(final MemorySegment data, final MemorySegment zwlrLayerSurfaceV1) {
				handleEvent("zwlr_layer_surface_v1.closed", () -> {
					if (destroyed) {
						return;
					}

					destroyProxies();
					connection.flush();
					layerClosed();
				});
			}

			/// Handles `zwlr_layer_surface_v1.configure`.
			///
			/// The configured size is ignored: Swing owns the size, and a differing size is
			/// either outdated or a consequence of a wrong buffer scale.
			///
			/// @param data unused user data
			/// @param zwlrLayerSurfaceV1 the layer surface
			/// @param serial the serial to acknowledge
			/// @param width the width, `0` meaning the client decides
			/// @param height the height, `0` meaning the client decides
			@SuppressWarnings("unused")
			private void onConfigure(final MemorySegment data, final MemorySegment zwlrLayerSurfaceV1, final int serial,
					final int width, final int height) {
				handleEvent("zwlr_layer_surface_v1.configure", () -> {
					if (destroyed) {
						return;
					}

					Wayland.send(layerSurface, Wayland.ZWLR_LAYER_SURFACE_V1_ACK_CONFIGURE, Wayland.M_I, serial);

					configured = true;
					render();
					connection.flush();
				});
			}

			/// Handles `wl_callback.done` of a `wl_surface.frame` request: the compositor
			/// is ready for the next frame, so a pending frame is rendered now.
			///
			/// @param data unused user data
			/// @param wlCallback the callback
			/// @param callbackData the presentation time in milliseconds
			@SuppressWarnings("unused")
			private void onFrameDone(final MemorySegment data, final MemorySegment wlCallback, final int callbackData) {
				handleEvent("wl_callback.done", () -> {
					Wayland.wl_proxy_destroy(wlCallback);
					frameCallback = null;

					if (framePending) {
						render();
					}
				});
			}

			/// Handles `wl_callback.done` of a margin change: from now on pointer
			/// coordinates refer to the new position.
			///
			/// @param data unused user data
			/// @param wlCallback the callback
			/// @param callbackData unused callback data
			@SuppressWarnings("unused")
			private void onSyncDone(final MemorySegment data, final MemorySegment wlCallback, final int callbackData) {
				handleEvent("wl_callback.done", () -> {
					final var pendingMargin = pendingMargins.poll();
					if (pendingMargin != null) {
						appliedMarginX = pendingMargin.x();
						appliedMarginY = pendingMargin.y();
					}

					Wayland.wl_proxy_destroy(wlCallback);
				});
			}

			/// Accumulates continuous scrolling until the end of the pointer frame.
			///
			/// @param value the axis value
			private void pointerAxis(final double value) {
				mouseTranslator.axis(value);
			}

			/// Accumulates wheel steps until the end of the pointer frame.
			///
			/// @param discrete the number of steps
			private void pointerAxisDiscrete(final int discrete) {
				mouseTranslator.axisDiscrete(discrete);
			}

			/// Forwards a button change.
			///
			/// @param button the Linux button code
			/// @param pressed whether the button was pressed
			private void pointerButton(final int button, final boolean pressed) {
				mouseTranslator.button(button, pressed);
			}

			/// Forwards pointer enter with screen coordinates.
			///
			/// @param x the surface x coordinate
			/// @param y the surface y coordinate
			private void pointerEnter(final double x, final double y) {
				mouseTranslator.enter(x, y, toScreenX(x), toScreenY(y));
			}

			/// Forwards the end of a pointer frame.
			private void pointerFrame() {
				mouseTranslator.frame();
			}

			/// Forwards pointer leave.
			private void pointerLeave() {
				mouseTranslator.leave();
			}

			/// Forwards pointer motion with screen coordinates.
			///
			/// @param x the surface x coordinate
			/// @param y the surface y coordinate
			private void pointerMotion(final double x, final double y) {
				mouseTranslator.motion(x, y, toScreenX(x), toScreenY(y));
			}

			/// Copies an image into a free buffer and commits it.
			///
			/// @param pixels the pixels
			/// @param offset the index of the first pixel
			/// @param stride the number of pixels per row
			/// @param width the width in physical pixels
			/// @param height the height in physical pixels
			/// @param scale the integer buffer scale
			private void present(final int[] pixels, final int offset, final int stride, final int width,
					final int height, final int scale) {
				if (width <= 0 || height <= 0) {
					return;
				}

				buffers.removeIf((final var buffer) -> {
					final var unusable = !buffer.busy && (buffer.width != width || buffer.height != height);
					if (unusable) {
						buffer.destroy();
					}
					return unusable;
				});

				final var freeBuffer = findFreeBuffer(width, height);
				if (freeBuffer == null && buffers.size() >= MAX_BUFFERS) {
					framePending = true;
					return;
				}
				final var target = freeBuffer != null ? freeBuffer : allocateBuffer(width, height);

				if (stride == width) {
					MemorySegment.copy(pixels, offset, target.memory, ValueLayout.JAVA_INT, 0, width * height);
				} else {
					for (var row = 0; row < height; row++) {
						MemorySegment.copy(pixels, offset + row * stride, target.memory, ValueLayout.JAVA_INT,
								(long) row * width * Integer.BYTES, width);
					}
				}

				if (scale != bufferScale && connection.compositorVersion >= 3) {
					bufferScale = scale;
					Wayland.send(surface, Wayland.WL_SURFACE_SET_BUFFER_SCALE, Wayland.M_I, scale);
				}

				Wayland.send(surface, Wayland.WL_SURFACE_ATTACH, Wayland.M_PII, target.wlBuffer, 0, 0);
				if (connection.compositorVersion >= 4) {
					Wayland.send(surface, Wayland.WL_SURFACE_DAMAGE_BUFFER, Wayland.M_IIII, 0, 0, width, height);
				} else {
					Wayland.send(surface, Wayland.WL_SURFACE_DAMAGE, Wayland.M_IIII, 0, 0, width / scale,
							height / scale);
				}

				final var newFrameCallback = Wayland.create(surface, Wayland.WL_SURFACE_FRAME,
						Wayland.WL_CALLBACK_INTERFACE, Wayland.M_P, MemorySegment.NULL);
				Wayland.wl_proxy_add_listener(newFrameCallback, frameListener, MemorySegment.NULL);
				frameCallback = newFrameCallback;

				Wayland.send(surface, Wayland.WL_SURFACE_COMMIT, Wayland.M_P, MemorySegment.NULL);
				target.busy = true;

				connection.flush();
			}

			/// Copies the current image into a free buffer and commits it, unless the
			/// compositor has not yet asked for the next frame; then the frame is rendered
			/// when it does.
			private void render() {
				if (destroyed || !configured) {
					return;
				}

				if (frameCallback != null) {
					framePending = true;
					return;
				}

				framePending = false;
				contentWrapper.presentTo(this);
			}

			/// Schedules a new frame; multiple requests before it is rendered are merged.
			private void requestFrame() {
				if (frameScheduled || destroyed) {
					return;
				}

				frameScheduled = true;
				SwingUtilities.invokeLater(() -> {
					frameScheduled = false;
					render();
				});
			}

			/// Moves the surface by changing its margins.
			///
			/// Pointer events that arrive before the `done` of the following
			/// `wl_display.sync` were still generated for the old position; without this
			/// distinction a drag would jump back and forth between the old and the new
			/// position.
			///
			/// @param marginX the left margin
			/// @param marginY the top margin
			private void setMargins(final int marginX, final int marginY) {
				if (destroyed) {
					return;
				}

				Wayland.send(layerSurface, Wayland.ZWLR_LAYER_SURFACE_V1_SET_MARGIN, Wayland.M_IIII, marginY, 0, 0,
						marginX);
				Wayland.send(surface, Wayland.WL_SURFACE_COMMIT, Wayland.M_P, MemorySegment.NULL);

				final var callback = Wayland.create(connection.display, Wayland.WL_DISPLAY_SYNC,
						Wayland.WL_CALLBACK_INTERFACE, Wayland.M_P, MemorySegment.NULL);
				Wayland.wl_proxy_add_listener(callback, syncListener, MemorySegment.NULL);
				pendingMargins.add(new PendingMargins(callback, marginX, marginY));

				connection.flush();
			}

			/// Changes the size of the surface.
			///
			/// @param width the width
			/// @param height the height
			private void setSize(final int width, final int height) {
				if (destroyed) {
					return;
				}

				Wayland.send(layerSurface, Wayland.ZWLR_LAYER_SURFACE_V1_SET_SIZE, Wayland.M_II, width, height);
				Wayland.send(surface, Wayland.WL_SURFACE_COMMIT, Wayland.M_P, MemorySegment.NULL);

				connection.flush();
			}

			/// Converts a surface x coordinate to a screen coordinate.
			///
			/// @param x the surface coordinate
			/// @return the screen coordinate
			private double toScreenX(final double x) {
				return deviceBounds.x + appliedMarginX + x;
			}

			/// Converts a surface y coordinate to a screen coordinate.
			///
			/// @param y the surface coordinate
			/// @return the screen coordinate
			private double toScreenY(final double y) {
				return deviceBounds.y + appliedMarginY + y;
			}

			/// A margin change waiting for its `wl_display.sync` to complete.
			///
			/// @param callback the sync callback
			/// @param x the left margin
			/// @param y the top margin
			private record PendingMargins(MemorySegment callback, int x, int y) {
			}

			/// A `wl_buffer` on a `wl_shm_pool` backed by a memfd.
			private static final class ShmBuffer {

				/// The height in pixels.
				private final int height;

				/// The mapped pixel memory.
				private final MemorySegment memory;

				/// The size in bytes.
				private final long size;

				/// The width in pixels.
				private final int width;

				/// The `wl_buffer`.
				private final MemorySegment wlBuffer;

				/// Whether the compositor still uses the buffer.
				private boolean busy;

				/// Allocates the shared memory and creates the buffer.
				///
				/// `libwayland-client` duplicates the file descriptor while marshaling, so it
				/// is closed right away; the buffer keeps the pool alive after the pool itself
				/// is destroyed.
				///
				/// @param shm the `wl_shm` global
				/// @param width the width
				/// @param height the height
				/// @param listener the buffer listener table
				/// @throws IllegalStateException if the shared memory cannot be created
				private ShmBuffer(final MemorySegment shm, final int width, final int height,
						final MemorySegment listener) {
					this.width = width;
					this.height = height;
					final var stride = width * Integer.BYTES;
					size = (long) stride * height;

					final var fd = createMemfd(size);
					try {
						memory = map(fd, size);
						final var pool = Wayland.create(shm, Wayland.WL_SHM_CREATE_POOL, Wayland.WL_SHM_POOL_INTERFACE,
								Wayland.M_PII, MemorySegment.NULL, fd, (int) size);
						wlBuffer = Wayland.create(pool, Wayland.WL_SHM_POOL_CREATE_BUFFER, Wayland.WL_BUFFER_INTERFACE,
								Wayland.M_PIIIII, MemorySegment.NULL, 0, width, height, stride,
								Wayland.WL_SHM_FORMAT_ARGB8888);
						Wayland.destroy(pool, Wayland.WL_SHM_POOL_DESTROY);
						Wayland.wl_proxy_add_listener(wlBuffer, listener, MemorySegment.NULL);
					} finally {
						Linux.close(fd);
					}
				}

				/// Creates an anonymous memory file of the given size.
				///
				/// @param size the size in bytes
				/// @return the file descriptor
				/// @throws IllegalStateException if the file cannot be created
				private static int createMemfd(final long size) {
					try (final var arena = Arena.ofConfined()) {
						final var fd = Linux.memfd_create(arena.allocateFrom("controllerbuddy-overlay"),
								Linux.MFD_CLOEXEC);
						if (fd < 0) {
							throw new IllegalStateException("memfd_create() failed");
						}

						if (Linux.ftruncate(fd, size) != 0) {
							Linux.close(fd);
							throw new IllegalStateException("ftruncate() failed");
						}

						return fd;
					}
				}

				/// Maps a memory file.
				///
				/// @param fd the file descriptor
				/// @param size the size in bytes
				/// @return the mapping
				/// @throws IllegalStateException if the mapping fails
				private static MemorySegment map(final int fd, final long size) {
					final var mapping = Linux.mmap(MemorySegment.NULL, size, Linux.PROT_READ | Linux.PROT_WRITE,
							Linux.MAP_SHARED, fd, 0L);
					if (mapping.address() == -1L) {
						throw new IllegalStateException("mmap() failed");
					}

					return mapping.reinterpret(size);
				}

				/// Destroys the buffer and unmaps its memory.
				private void destroy() {
					Wayland.destroy(wlBuffer, Wayland.WL_BUFFER_DESTROY);
					Linux.munmap(memory, size);
				}
			}
		}

		/// Translates Wayland pointer input into Swing mouse events.
		private final class MouseTranslator {

			/// Axis units per wheel step for continuous scrolling (libinput reports 15 per
			/// detent).
			private static final double AXIS_UNITS_PER_STEP = 15.0;

			/// Linux code of the left button.
			private static final int BTN_LEFT = 0x110;

			/// Linux code of the middle button.
			private static final int BTN_MIDDLE = 0x112;

			/// Linux code of the right button.
			private static final int BTN_RIGHT = 0x111;

			/// Movement in pixels that still counts as a click.
			private static final int CLICK_TOLERANCE = 5;

			/// Maximum delay between presses of a multi-click.
			private static final long MULTI_CLICK_MILLIS = 500;

			/// Accumulated discrete wheel steps of the current pointer frame.
			private int axisDiscrete;

			/// Remainder of continuous scrolling that has not yet formed a full step.
			private double axisRemainder;

			/// Accumulated continuous axis value of the current pointer frame.
			private double axisValue;

			/// The `BUTTONx_DOWN_MASK` bits of the pressed buttons.
			private int buttonsDown;

			/// The click count of the current press.
			private int clickCount;

			/// The AWT button of the last press.
			private int lastButton = MouseEvent.NOBUTTON;

			/// The time of the last press.
			private long lastPressTime;

			/// Whether the pointer moved beyond the click tolerance since the last press.
			private boolean movedSincePress;

			/// The screen x coordinate of the last press.
			private int pressScreenX;

			/// The screen y coordinate of the last press.
			private int pressScreenY;

			/// The last screen x coordinate.
			private int screenX;

			/// The last screen y coordinate.
			private int screenY;

			/// The last surface x coordinate.
			private int x;

			/// The last surface y coordinate.
			private int y;

			/// Creates the translator.
			private MouseTranslator() {
			}

			/// Accumulates continuous scrolling until the end of the pointer frame.
			///
			/// @param value the axis value
			private void axis(final double value) {
				axisValue += value;
			}

			/// Accumulates wheel steps until the end of the pointer frame.
			///
			/// @param discrete the number of steps
			private void axisDiscrete(final int discrete) {
				axisDiscrete += discrete;
			}

			/// Handles a button change, including click counting and the click event.
			///
			/// @param linuxButton the Linux button code
			/// @param pressed whether the button was pressed
			private void button(final int linuxButton, final boolean pressed) {
				final var awtButton = switch (linuxButton) {
				case BTN_LEFT -> MouseEvent.BUTTON1;
				case BTN_MIDDLE -> MouseEvent.BUTTON2;
				case BTN_RIGHT -> MouseEvent.BUTTON3;
				default -> MouseEvent.NOBUTTON;
				};
				if (awtButton == MouseEvent.NOBUTTON) {
					return;
				}

				final var mask = InputEvent.getMaskForButton(awtButton);
				if (pressed) {
					final var now = System.currentTimeMillis();
					final var sameSpot = Math.abs(screenX - pressScreenX) <= CLICK_TOLERANCE
							&& Math.abs(screenY - pressScreenY) <= CLICK_TOLERANCE;
					clickCount = awtButton == lastButton && now - lastPressTime <= MULTI_CLICK_MILLIS && sameSpot
							? clickCount + 1
							: 1;
					lastButton = awtButton;
					lastPressTime = now;
					pressScreenX = screenX;
					pressScreenY = screenY;
					movedSincePress = false;
					buttonsDown |= mask;
					post(MouseEvent.MOUSE_PRESSED, awtButton, clickCount, awtButton == MouseEvent.BUTTON3);
				} else {
					buttonsDown &= ~mask;
					post(MouseEvent.MOUSE_RELEASED, awtButton, clickCount, false);
					if (!movedSincePress) {
						post(MouseEvent.MOUSE_CLICKED, awtButton, clickCount, false);
					}
				}
			}

			/// Handles pointer enter.
			///
			/// @param newX the surface x coordinate
			/// @param newY the surface y coordinate
			/// @param newScreenX the screen x coordinate
			/// @param newScreenY the screen y coordinate
			private void enter(final double newX, final double newY, final double newScreenX, final double newScreenY) {
				update(newX, newY, newScreenX, newScreenY);
				post(MouseEvent.MOUSE_ENTERED, MouseEvent.NOBUTTON, 0, false);
			}

			/// Emits the accumulated scrolling of a pointer frame as wheel steps; real
			/// wheel detents take precedence over continuous values.
			private void frame() {
				final var discrete = axisDiscrete != 0;
				final var continuous = axisRemainder + axisValue;
				final var rotation = discrete ? axisDiscrete : (int) (continuous / AXIS_UNITS_PER_STEP);
				axisRemainder = discrete ? 0 : continuous - rotation * AXIS_UNITS_PER_STEP;
				axisValue = 0;
				axisDiscrete = 0;

				if (rotation != 0 && !closed) {
					final var event = lightweightFrame.createMouseWheelEvent(lightweightFrame, buttonsDown, x, y,
							rotation);
					SwingInterOpUtils.postEvent(event.getSource(), event);
				}
			}

			/// Handles pointer leave.
			private void leave() {
				post(MouseEvent.MOUSE_EXITED, MouseEvent.NOBUTTON, 0, false);
			}

			/// Handles pointer motion; uses screen coordinates for the click tolerance,
			/// because the overlay moves along with the pointer while it is dragged.
			///
			/// @param newX the surface x coordinate
			/// @param newY the surface y coordinate
			/// @param newScreenX the screen x coordinate
			/// @param newScreenY the screen y coordinate
			private void motion(final double newX, final double newY, final double newScreenX,
					final double newScreenY) {
				update(newX, newY, newScreenX, newScreenY);

				if (buttonsDown == 0) {
					post(MouseEvent.MOUSE_MOVED, MouseEvent.NOBUTTON, 0, false);
					return;
				}

				if (Math.abs(screenX - pressScreenX) > CLICK_TOLERANCE
						|| Math.abs(screenY - pressScreenY) > CLICK_TOLERANCE) {
					movedSincePress = true;
				}
				post(MouseEvent.MOUSE_DRAGGED, MouseEvent.NOBUTTON, clickCount, false);
			}

			/// Posts a mouse event to the lightweight frame.
			///
			/// The target passed to `postEvent` must be the event source (the lightweight
			/// frame), not the wrapper: `postEvent` derives the app context from it and
			/// drops the event otherwise.
			///
			/// @param id the event id
			/// @param button the AWT button
			/// @param count the click count
			/// @param popupTrigger whether the event is a popup trigger
			private void post(final int id, final int button, final int count, final boolean popupTrigger) {
				if (closed) {
					return;
				}

				final var event = lightweightFrame.createMouseEvent(lightweightFrame, id, System.currentTimeMillis(),
						buttonsDown, x, y, screenX, screenY, count, popupTrigger, button);
				SwingInterOpUtils.postEvent(event.getSource(), event);
			}

			/// Stores the current coordinates.
			///
			/// @param newX the surface x coordinate
			/// @param newY the surface y coordinate
			/// @param newScreenX the screen x coordinate
			/// @param newScreenY the screen y coordinate
			private void update(final double newX, final double newY, final double newScreenX,
					final double newScreenY) {
				x = (int) Math.floor(newX);
				y = (int) Math.floor(newY);
				screenX = (int) Math.floor(newScreenX);
				screenY = (int) Math.floor(newScreenY);
			}
		}
	}
}
