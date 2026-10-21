/*
 * Copyright (C) 2018 Matteo Hausner
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

import de.bwravencl.controllerbuddy.Main;
import de.bwravencl.controllerbuddy.ffi.Kernel32;
import de.bwravencl.controllerbuddy.ffi.User32;
import de.bwravencl.controllerbuddy.input.Mode;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.Serial;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSpinner.NumberEditor;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import javax.swing.text.DefaultFormatter;
import javax.swing.text.Document;
import javax.swing.undo.UndoManager;
import org.jspecify.annotations.Nullable;

/// Utility class providing common GUI helper methods for window management,
/// component creation, display bounds calculation, and frame persistence.
///
/// All methods are static; the class cannot be instantiated. It covers tasks
/// such as wrapping components in scroll panes, creating text fields with
/// context menus, positioning frames across multiple displays, and showing
/// message dialogs.
public final class GuiUtils {

	/// Reflective field accessor for the native window handle on Windows.
	private static final @Nullable Field hwndField;

	private static final Logger logger = Logger.getLogger(GuiUtils.class.getName());

	/// Reflective field accessor for the AWT component peer on Windows.
	private static final @Nullable Field peerField;

	/// Whether message dialogs should be suppressed during automated runs.
	public static boolean skipMessageDialogs;

	static {
		if (Main.IS_WINDOWS) {
			try {
				peerField = Component.class.getDeclaredField("peer");
				peerField.setAccessible(true);

				@SuppressWarnings({ "Java9ReflectionClassVisibility", "RedundantSuppression" })
				final var wComponentPeerClass = Class.forName("sun.awt.windows.WComponentPeer", false, null);
				hwndField = wComponentPeerClass.getDeclaredField("hwnd");
				hwndField.setAccessible(true);
			} catch (final ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		} else {
			peerField = null;
			hwndField = null;
		}
	}

	/// Prevents instantiation.
	private GuiUtils() {
	}

	/// Creates a mode selection panel with a combo box and adds it to the given
	/// container.
	///
	/// @param container the container to add the mode panel to
	/// @param modes the list of available modes
	/// @param actionListener the action to invoke when the selected mode changes
	/// @return the mode combo box
	static JComboBox<Mode> addModePanel(final Container container, final List<Mode> modes,
			final AbstractAction actionListener) {
		final var modePanel = new JPanel(
				new FlowLayout(FlowLayout.CENTER, MainFrame.DEFAULT_HGAP, MainFrame.DEFAULT_VGAP));
		container.add(modePanel, BorderLayout.NORTH);

		modePanel.add(new JLabel(Main.strings.getString("MODE_LABEL")));

		final var modeComboBox = new JComboBox<>(modes.toArray(Mode[]::new));
		modeComboBox.addActionListener(actionListener);
		modePanel.add(modeComboBox);

		return modeComboBox;
	}

	/// Displays a [JOptionPane] dialog with the window icon images specified in
	/// [Main#ICON_RESOURCE_PATHS].
	///
	/// @param parentComponent the parent component for the dialog
	/// @param message the message to display
	/// @param title the dialog title
	/// @param optionType the options to display in the pane: `DEFAULT_OPTION`,
	/// `YES_NO_OPTION`, `YES_NO_CANCEL_OPTION`, or `OK_CANCEL_OPTION`
	/// @param messageType the type of message to be displayed: `ERROR_MESSAGE`,
	/// `INFORMATION_MESSAGE`, `WARNING_MESSAGE`, `QUESTION_MESSAGE`, or
	/// `PLAIN_MESSAGE`
	/// @param icon the [Icon] image to display within the dialog
	/// @return the underlying [JOptionPane]
	/// @throws HeadlessException if the environment is headless
	private static JOptionPane createDialog(final @Nullable Component parentComponent, final Object message,
			final String title, final int optionType, final int messageType, final @Nullable Icon icon)
			throws HeadlessException {
		@SuppressWarnings("MagicConstant")
		final var optionPane = new JOptionPane(message, messageType, optionType, icon);
		final var dialog = optionPane.createDialog(parentComponent, title);
		setWindowIcon(dialog);
		dialog.setVisible(true);
		dialog.dispose();

		return optionPane;
	}

	/// Creates a non-editable, non-focusable editor pane configured for displaying
	/// HTML content.
	///
	/// @return a new HTML viewer editor pane
	static JEditorPane createHtmlViewerEditorPane() {
		final var editorPane = new JEditorPane();
		editorPane.setContentType("text/html");
		editorPane.setEditable(false);
		editorPane.setFocusable(false);
		editorPane.setCaretColor(editorPane.getBackground());

		return editorPane;
	}

	/// Constructs a one-pixel border styled with the current UI theme's component
	/// border color, used for overlays.
	///
	/// @return a new [LineBorder] instance
	static LineBorder createOverlayBorder() {
		return new LineBorder(UIManager.getColor("Component.borderColor"), 1);
	}

	/// Creates a text field with a right-click context menu providing undo, cut,
	/// copy, paste, and select-all actions.
	///
	/// @param text the initial text
	/// @param columns the number of columns for sizing
	/// @return a new text field with a context menu
	public static JTextField createTextFieldWithMenu(final @Nullable String text, final int columns) {
		return createTextFieldWithMenu(null, text, columns);
	}

	/// Creates a text field backed by the given document model, with a right-click
	/// context menu providing undo, cut, copy, paste, and select-all actions.
	///
	/// @param doc the document model, or `null` to use the default
	/// @param text the initial text
	/// @param columns the number of columns for sizing
	/// @return a new text field with a context menu
	public static JTextField createTextFieldWithMenu(final @Nullable Document doc, final @Nullable String text,
			final int columns) {
		final var textField = new JTextField(doc, text, columns);

		final var undoManager = new UndoManager();
		textField.getDocument().addUndoableEditListener(undoManager);

		final var undoAction = new AbstractAction(Main.strings.getString("UNDO_ACTION_NAME")) {

			@Serial
			private static final long serialVersionUID = 283480113359047860L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				if (!undoManager.canUndo()) {
					return;
				}

				undoManager.undo();
				setEnabled(undoManager.canUndo());
			}
		};

		final var cutAction = new AbstractAction(Main.strings.getString("CUT_ACTION_NAME")) {

			@Serial
			private static final long serialVersionUID = 166873451012920651L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				textField.cut();
			}
		};

		final var copyAction = new AbstractAction(Main.strings.getString("COPY_ACTION_NAME")) {

			@Serial
			private static final long serialVersionUID = 4845008543826860777L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				textField.copy();
			}
		};

		final var pasteAction = new AbstractAction(Main.strings.getString("PASTE_ACTION_NAME")) {

			@Serial
			private static final long serialVersionUID = -669017641654371170L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				textField.paste();
			}
		};

		final var selectAllAction = new AbstractAction(Main.strings.getString("SELECT_ALL_ACTION_NAME")) {

			@Serial
			private static final long serialVersionUID = -6215392890571518146L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				textField.selectAll();
			}
		};

		cutAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control X"));
		copyAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control C"));
		pasteAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control V"));
		selectAllAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control A"));

		final var popup = new JPopupMenu() {

			@Serial
			private static final long serialVersionUID = -7112090718851303887L;

			@Override
			public void show(final Component invoker, final int x, final int y) {
				undoAction.setEnabled(undoManager.canUndo());

				final var selectedText = textField.getSelectedText();
				final var canCopyOrCut = selectedText != null && !selectedText.isEmpty();
				copyAction.setEnabled(canCopyOrCut);
				cutAction.setEnabled(canCopyOrCut);

				super.show(invoker, x, y);
			}
		};

		popup.add(undoAction);
		popup.addSeparator();
		popup.add(cutAction);
		popup.add(copyAction);
		popup.add(pasteAction);
		popup.addSeparator();
		popup.add(selectAllAction);

		textField.setComponentPopupMenu(popup);

		return textField;
	}

	/// Computes the bounding rectangle that encompasses all connected displays and
	/// optionally stores the result in the main application instance.
	///
	/// @param mainFrame the main frame instance
	/// @return the total display bounds rectangle
	static Rectangle getAndStoreTotalDisplayBounds(final @Nullable MainFrame mainFrame) {
		final var totalDisplayBounds = new Rectangle();

		for (final var graphicsDevice : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
			final var graphicsConfiguration = graphicsDevice.getConfigurations()[0];
			final var bounds = graphicsConfiguration.getBounds();

			final var maxX = bounds.x + bounds.width;
			final var maxY = bounds.y + bounds.height;

			totalDisplayBounds.x = Math.min(totalDisplayBounds.x, bounds.x);
			totalDisplayBounds.x = Math.min(totalDisplayBounds.x, maxX);
			totalDisplayBounds.y = Math.min(totalDisplayBounds.y, bounds.y);
			totalDisplayBounds.y = Math.min(totalDisplayBounds.y, maxY);

			totalDisplayBounds.width = Math.max(totalDisplayBounds.width, bounds.x);
			totalDisplayBounds.width = Math.max(totalDisplayBounds.width, maxX);
			totalDisplayBounds.height = Math.max(totalDisplayBounds.height, bounds.y);
			totalDisplayBounds.height = Math.max(totalDisplayBounds.height, maxY);
		}

		if (mainFrame != null) {
			mainFrame.setTotalDisplayBounds(totalDisplayBounds);
		}

		return totalDisplayBounds;
	}

	/// Returns the preferences key used to persist the location of the given frame.
	///
	/// The key is derived from the frame's title by converting it to
	/// underscore-separated lowercase and appending `_location`. Returns an empty
	/// optional if the frame has no title.
	///
	/// @param frame the frame whose preferences key to compute
	/// @return an optional containing the preferences key, or empty if the frame
	/// has no usable title
	static Optional<String> getFrameLocationPreferencesKey(final JFrame frame) {
		final var title = frame.getTitle();
		if (title == null || title.isBlank()) {
			return Optional.empty();
		}

		var underscoreTitle = title.codePoints().mapToObj(c -> {
			if (c == ' ') {
				return "_";
			}
			return (Character.isUpperCase(c) ? "_" : "") + Character.toLowerCase((char) c);
		}).collect(Collectors.joining());
		underscoreTitle = underscoreTitle.startsWith("_") ? underscoreTitle.substring(1) : underscoreTitle;

		return Optional.of(underscoreTitle + "_location");
	}

	/// Returns the current Linux desktop environment.
	///
	/// Returns `"gnome"` for any GNOME session, otherwise the raw value of
	/// `XDG_CURRENT_DESKTOP`.
	///
	/// @return the desktop name or `null` if it could not be determined
	public static @Nullable String getLinuxDesktop() {
		if (!Main.IS_LINUX) {
			throw new UnsupportedOperationException();
		}

		final var gnome = "gnome";
		if (System.getenv("GNOME_DESKTOP_SESSION_ID") != null) {
			return gnome;
		}

		final var xdgCurrentDesktop = System.getenv("XDG_CURRENT_DESKTOP");
		return (xdgCurrentDesktop != null && xdgCurrentDesktop.toLowerCase(Locale.ROOT).contains(gnome)) ? gnome
				: xdgCurrentDesktop;
	}

	/// Computes the bounding rectangle that encompasses all connected displays.
	///
	/// @return the total display bounds rectangle
	static Rectangle getTotalDisplayBounds() {
		return getAndStoreTotalDisplayBounds(null);
	}

	/// Runs the given task on the AWT Event Dispatch Thread. If already on the EDT,
	/// the task executes immediately; otherwise it is scheduled via
	/// [EventQueue#invokeLater].
	///
	/// @param runnable the task to run
	public static void invokeOnEventDispatchThreadIfRequired(final Runnable runnable) {
		if (EventQueue.isDispatchThread()) {
			runnable.run();
		} else {
			EventQueue.invokeLater(runnable);
		}
	}

	/// Loads a previously persisted frame location from preferences and applies it
	/// to the frame, clamping the position to stay within the total display bounds.
	///
	/// @param frame the frame to position
	/// @param defaultLocation the fallback location if no preference exists
	/// @param totalDisplayBounds the bounding rectangle of all displays
	static void loadFrameLocation(final JFrame frame, final Point defaultLocation, final Rectangle totalDisplayBounds) {
		final var location = new Point(defaultLocation);

		getFrameLocationPreferencesKey(frame).ifPresent(preferencesKey -> {
			final var locationString = Main.preferences.get(preferencesKey, null);
			if (locationString != null) {
				final var parts = locationString.split(",", -1);

				if (parts.length == 2) {
					try {
						location.x = Math.round(Float.parseFloat(parts[0]) * totalDisplayBounds.width);
						location.y = Math.round(Float.parseFloat(parts[1]) * totalDisplayBounds.height);
					} catch (NumberFormatException _) {
						// ignore an invalid location string that does not contain numeric values
					}
				}
			}
		});

		setFrameLocationRespectingBounds(frame, location, totalDisplayBounds);
	}

	/// Configures a spinner to display its value with a hertz unit suffix.
	///
	/// @param spinner the spinner to configure
	public static void makeHertzSpinner(final JSpinner spinner) {
		final var numberEditor = new NumberEditor(spinner, "# " + Main.strings.getString("HERTZ_SYMBOL"));
		((DefaultFormatter) numberEditor.getTextField().getFormatter()).setCommitsOnValidEdit(true);

		final var textField = numberEditor.getTextField();
		textField.setColumns(6);

		spinner.setEditor(numberEditor);
	}

	/// Configures a spinner to display its value with a millisecond unit suffix.
	///
	/// @param spinner the spinner to configure
	public static void makeMillisecondSpinner(final JSpinner spinner) {
		final var numberEditor = new NumberEditor(spinner, "# " + Main.strings.getString("MILLISECOND_SYMBOL"));
		((DefaultFormatter) numberEditor.getTextField().getFormatter()).setCommitsOnValidEdit(true);

		final var textField = numberEditor.getTextField();
		textField.setColumns(6);

		spinner.setEditor(numberEditor);
	}

	/// Makes the given window topmost. On Windows, uses native `SetWindowPos` with
	/// `HWND_TOPMOST`; on other platforms, toggles `alwaysOnTop` as a fallback.
	///
	/// @param window the window to make topmost
	static void makeWindowTopmost(final Window window) {
		if (Main.IS_WINDOWS && peerField != null && hwndField != null) {
			try {
				final var windowPeer = peerField.get(window);
				final var windowHwnd = (long) hwndField.get(windowPeer);
				final var hWnd = MemorySegment.ofAddress(windowHwnd);

				if (User32.SetWindowPos(hWnd, User32.HWND_TOPMOST, 0, 0, 0, 0,
						User32.SWP_NOMOVE | User32.SWP_NOSIZE) != 0) {
					return;
				}
				logger.severe("SetWindowPos failed: " + Kernel32.GetLastError());
			} catch (final IllegalAccessException e) {
				logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}

		window.setAlwaysOnTop(false);
		window.setAlwaysOnTop(true);
	}

	/// Sets the bounds of a component and applies a minimum size of half the given
	/// bounds dimensions.
	///
	/// @param component the component to resize
	/// @param bounds the bounds to set
	static void setBoundsWithMinimum(final Component component, final Rectangle bounds) {
		component.setBounds(bounds);
		component.setMinimumSize(new Dimension(bounds.width / 2, bounds.height / 2));
	}

	/// Recursively enables or disables a component and all of its children.
	///
	/// @param component the root component
	/// @param enabled whether to enable or disable
	static void setEnabledRecursive(final Component component, final boolean enabled) {
		component.setEnabled(enabled);

		if (component instanceof final Container container) {
			for (final var child : container.getComponents()) {
				setEnabledRecursive(child, enabled);
			}
		}
	}

	/// Sets the frame's location, clamping it so the frame stays fully within the
	/// given display bounds.
	///
	/// @param frame the frame to position
	/// @param location the desired location, modified in place to satisfy the
	/// bounds constraint
	/// @param totalDisplayBounds the bounding rectangle of all displays
	static void setFrameLocationRespectingBounds(final Frame frame, final Point location,
			final Rectangle totalDisplayBounds) {
		location.x = Math.clamp(location.x, totalDisplayBounds.x, totalDisplayBounds.width - frame.getWidth());
		location.y = Math.clamp(location.y, totalDisplayBounds.y, totalDisplayBounds.height - frame.getHeight());
		frame.setLocation(location);
	}

	/// Sets the window icon to the images specified in [Main#ICON_RESOURCE_PATHS].
	///
	/// @param window the window to set the icon for
	static void setWindowIcon(final Window window) {
		window.setIconImages(Main.ICON_RESOURCE_PATHS.stream()
				.map(path -> new ImageIcon(Main.getResourceLocation(path)).getImage()).toList());
	}

	/// Displays a confirm dialog, ensuring the main window is shown first.
	///
	/// @param mainFrame the main frame instance, or `null`
	/// @param parentComponent the parent component for the dialog
	/// @param message the message to display
	/// @param title the dialog title
	/// @param optionType the options to display in the pane: `DEFAULT_OPTION`,
	/// `YES_NO_OPTION`, `YES_NO_CANCEL_OPTION`, or `OK_CANCEL_OPTION`
	/// @param messageType the type of message to be displayed: `ERROR_MESSAGE`,
	/// `INFORMATION_MESSAGE`, `WARNING_MESSAGE`, `QUESTION_MESSAGE`, or
	/// `PLAIN_MESSAGE`
	/// @return the underlying [JOptionPane]
	/// @throws HeadlessException if the environment is headless
	public static int showConfirmDialog(final @Nullable MainFrame mainFrame, final Component parentComponent,
			final Object message, final String title, final int optionType, final int messageType)
			throws HeadlessException {
		if (mainFrame != null) {
			mainFrame.setVisible(true);
		}

		final var optionPane = createDialog(parentComponent, message, title, optionType, messageType, null);

		if (optionPane.getValue() instanceof final Integer value) {
			return value;
		}
		return JOptionPane.CLOSED_OPTION;
	}

	/// Displays a confirm dialog, ensuring the main window is shown first.
	///
	/// @param mainFrame the main frame instance, or `null`
	/// @param parentComponent the parent component for the dialog
	/// @param message the message to display
	/// @param title the dialog title
	/// @param optionType the options to display in the pane: `DEFAULT_OPTION`,
	/// `YES_NO_OPTION`, `YES_NO_CANCEL_OPTION`, or `OK_CANCEL_OPTION`
	/// @return the underlying [JOptionPane]
	/// @throws HeadlessException if the environment is headless
	public static int showConfirmDialog(final @Nullable MainFrame mainFrame, final Component parentComponent,
			final Object message, final String title, final int optionType) throws HeadlessException {
		return showConfirmDialog(mainFrame, parentComponent, message, title, optionType, JOptionPane.QUESTION_MESSAGE);
	}

	/// Displays a message dialog, ensuring the main window is shown first. Does
	/// nothing if message dialogs are currently suppressed and `skippable` is
	/// `true`.
	///
	/// @param mainFrame the main frame instance, or `null`
	/// @param parentComponent the parent component for the dialog
	/// @param message the message to display
	/// @param title the dialog title
	/// @param messageType the type of message to be displayed: `ERROR_MESSAGE`,
	/// `INFORMATION_MESSAGE`, `WARNING_MESSAGE`, `QUESTION_MESSAGE`, or
	/// `PLAIN_MESSAGE`
	/// @param skippable whether the dialog can be skipped
	/// @throws HeadlessException if the environment is headless
	public static void showMessageDialog(final @Nullable MainFrame mainFrame, final @Nullable Component parentComponent,
			final Object message, final String title, final int messageType, final boolean skippable)
			throws HeadlessException {
		showMessageDialog(mainFrame, parentComponent, message, title, messageType, null, skippable);
	}

	/// Displays a message dialog with a custom icon, ensuring the main window is
	/// shown first. Does nothing if message dialogs are currently suppressed and
	/// `skippable` is `true`.
	///
	/// @param mainFrame the main frame instance, or `null`
	/// @param parentComponent the parent component for the dialog
	/// @param message the message to display
	/// @param title the dialog title
	/// @param messageType the type of message to be displayed: `ERROR_MESSAGE`,
	/// `INFORMATION_MESSAGE`, `WARNING_MESSAGE`, `QUESTION_MESSAGE`, or
	/// `PLAIN_MESSAGE`
	/// @param icon the icon to display, or `null` for the default
	/// @param skippable whether the dialog can be skipped
	/// @throws HeadlessException if the environment is headless
	public static void showMessageDialog(final @Nullable MainFrame mainFrame, final @Nullable Component parentComponent,
			final Object message, final String title, final int messageType, final @Nullable Icon icon,
			final boolean skippable) throws HeadlessException {
		if (skippable && skipMessageDialogs) {
			return;
		}

		if (mainFrame != null) {
			mainFrame.setVisible(true);
		}

		createDialog(parentComponent, message, title, JOptionPane.DEFAULT_OPTION, messageType, icon);
	}

	/// Wraps a component in a scroll pane with default sizing.
	///
	/// @param component the component to wrap
	/// @return a new scroll pane containing the component
	static JScrollPane wrapComponentInScrollPane(final Component component) {
		return wrapComponentInScrollPane(component, null);
	}

	/// Wraps a component in a scroll pane with an optional preferred size.
	///
	/// @param component the component to wrap
	/// @param preferredSize the preferred size for the scroll pane, or `null` for
	/// default sizing
	/// @return a new scroll pane containing the component
	public static JScrollPane wrapComponentInScrollPane(final Component component,
			final @Nullable Dimension preferredSize) {
		final var scrollPane = new JScrollPane(component);

		if (preferredSize != null) {
			scrollPane.setPreferredSize(preferredSize);
		}

		return scrollPane;
	}
}
