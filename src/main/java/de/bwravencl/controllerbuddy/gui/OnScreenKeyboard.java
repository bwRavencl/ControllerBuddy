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

import de.bwravencl.controllerbuddy.gui.GuiUtils.FrameDragListener;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.Keystroke;
import de.bwravencl.controllerbuddy.input.LockKey;
import de.bwravencl.controllerbuddy.input.Mode;
import de.bwravencl.controllerbuddy.input.Scancode;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.RoundRectangle2D;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;

/// An on-screen virtual keyboard displayed as a translucent, always-on-top
/// popup window.
///
/// The keyboard is navigable via gamepad input and supports key press, release,
/// hold, and lock operations. It renders a standard QWERTY layout with function
/// keys, numpad, and modifier keys.
@SuppressWarnings("exports")
public final class OnScreenKeyboard extends JFrame {

	/// The mode associated with the on-screen keyboard.
	public static final Mode ON_SCREEN_KEYBOARD_MODE;

	/// Background color used for held keyboard buttons.
	private static final Color KEYBOARD_BUTTON_HELD_BACKGROUND = new Color(128, 128, 128);

	/// UUID used to identify the on-screen keyboard mode across serialization.
	private static final UUID ON_SCREEN_KEYBOARD_MODE_UUID = UUID.fromString("daf53639-9518-48db-bd63-19cde7bf9a96");

	/// Background color used for key row panels.
	private static final Color ROW_BACKGROUND = new Color(128, 128, 128, 64);

	/// Padding width in pixels applied to key row borders.
	private static final int ROW_BORDER_WIDTH = 15;

	@Serial
	private static final long serialVersionUID = -5061347351151925461L;

	static {
		ON_SCREEN_KEYBOARD_MODE = new Mode(ON_SCREEN_KEYBOARD_MODE_UUID);
		ON_SCREEN_KEYBOARD_MODE.setDescription(Main.STRINGS.getString("ON_SCREEN_KEYBOARD_MODE_DESCRIPTION"));
	}

	/// The Caps Lock key button on this keyboard.
	private final CapsLockKeyButton capsLockKeyButton;

	/// Listener that allows dragging the keyboard window by its background.
	@SuppressWarnings({ "serial", "RedundantSuppression" })
	private final FrameDragListener frameDragListener;

	/// Set of keyboard buttons currently held down (locked).
	@SuppressWarnings({ "serial", "RedundantSuppression" })
	private final Set<AbstractKeyboardButton> heldButtons = Collections
			.newSetFromMap(Collections.synchronizedMap(new IdentityHashMap<>()));

	/// Two-dimensional array of keyboard buttons arranged in rows and columns.
	private final AbstractKeyboardButton[][] keyboardButtons;

	/// The left Shift key button on this keyboard.
	private final ShiftKeyboardButton leftShiftKeyboardButton;

	/// The main application instance.
	@SuppressWarnings({ "serial", "RedundantSuppression" })
	private final Main main;

	/// The right Shift key button on this keyboard.
	private final ShiftKeyboardButton rightShiftKeyboardButton;

	/// Whether any keyboard button state changed since the last poll.
	private volatile boolean anyChanges;

	/// Column index of the currently selected keyboard button.
	private volatile int selectedColumn;

	/// Row index of the currently selected keyboard button.
	private volatile int selectedRow;

	/// Creates the on-screen keyboard and lays out all key rows.
	///
	/// @param main the main application instance
	OnScreenKeyboard(final Main main) {
		this.main = main;

		capsLockKeyButton = new CapsLockKeyButton();

		leftShiftKeyboardButton = new ShiftKeyboardButton(ShiftKeyboardButton.ShiftKeyboardButtonType.LEFT);
		rightShiftKeyboardButton = new ShiftKeyboardButton(ShiftKeyboardButton.ShiftKeyboardButtonType.RIGHT);

		keyboardButtons = new AbstractKeyboardButton[][] {
				{ new DefaultKeyboardButton(Scancode.DIK_ESCAPE), new DefaultKeyboardButton(Scancode.DIK_F1),
						new DefaultKeyboardButton(Scancode.DIK_F2), new DefaultKeyboardButton(Scancode.DIK_F3),
						new DefaultKeyboardButton(Scancode.DIK_F4), new DefaultKeyboardButton(Scancode.DIK_F5),
						new DefaultKeyboardButton(Scancode.DIK_F6), new DefaultKeyboardButton(Scancode.DIK_F7),
						new DefaultKeyboardButton(Scancode.DIK_F8), new DefaultKeyboardButton(Scancode.DIK_F9),
						new DefaultKeyboardButton(Scancode.DIK_F10), new DefaultKeyboardButton(Scancode.DIK_F11),
						new DefaultKeyboardButton(Scancode.DIK_F12), new DefaultKeyboardButton(Scancode.DIK_SYSRQ),
						new LockKeyButton(LockKey.SCROLL_LOCK_LOCK_KEY), new DefaultKeyboardButton(Scancode.DIK_PAUSE),
						new DefaultKeyboardButton(Scancode.DIK_INSERT), new DefaultKeyboardButton(Scancode.DIK_DELETE),
						new DefaultKeyboardButton(Scancode.DIK_HOME), new DefaultKeyboardButton(Scancode.DIK_END) },
				{ new ShiftableKeyboardButton(Scancode.DIK_GRAVE, "~"),
						new ShiftableKeyboardButton(Scancode.DIK_1, "!"),
						new ShiftableKeyboardButton(Scancode.DIK_2, "@"),
						new ShiftableKeyboardButton(Scancode.DIK_3, "#"),
						new ShiftableKeyboardButton(Scancode.DIK_4, "$"),
						new ShiftableKeyboardButton(Scancode.DIK_5, "%"),
						new ShiftableKeyboardButton(Scancode.DIK_6, "^"),
						new ShiftableKeyboardButton(Scancode.DIK_7, "&"),
						new ShiftableKeyboardButton(Scancode.DIK_8, "*"),
						new ShiftableKeyboardButton(Scancode.DIK_9, "("),
						new ShiftableKeyboardButton(Scancode.DIK_0, ")"),
						new ShiftableKeyboardButton(Scancode.DIK_MINUS, "_"),
						new ShiftableKeyboardButton(Scancode.DIK_EQUALS, "+"),
						new DefaultKeyboardButton(Scancode.DIK_BACK), new NumLockKeyButton(),
						new DefaultKeyboardButton(Scancode.DIK_DIVIDE),
						new DefaultKeyboardButton(Scancode.DIK_MULTIPLY),
						new DefaultKeyboardButton(Scancode.DIK_SUBTRACT) },
				{ new DefaultKeyboardButton(Scancode.DIK_TAB), new AlphabeticKeyboardButton(Scancode.DIK_Q),
						new AlphabeticKeyboardButton(Scancode.DIK_W), new AlphabeticKeyboardButton(Scancode.DIK_E),
						new AlphabeticKeyboardButton(Scancode.DIK_R), new AlphabeticKeyboardButton(Scancode.DIK_T),
						new AlphabeticKeyboardButton(Scancode.DIK_Y), new AlphabeticKeyboardButton(Scancode.DIK_U),
						new AlphabeticKeyboardButton(Scancode.DIK_I), new AlphabeticKeyboardButton(Scancode.DIK_O),
						new AlphabeticKeyboardButton(Scancode.DIK_P),
						new ShiftableKeyboardButton(Scancode.DIK_LBRACKET, "{"),
						new ShiftableKeyboardButton(Scancode.DIK_RBRACKET, "}"),
						new ShiftableKeyboardButton(Scancode.DIK_BACKSLASH, "|"),
						new NumPadKeyboardButton(Scancode.DIK_NUMPAD7, Scancode.DIK_HOME),
						new NumPadKeyboardButton(Scancode.DIK_NUMPAD8, Scancode.DIK_UP),
						new NumPadKeyboardButton(Scancode.DIK_NUMPAD9, Scancode.DIK_PRIOR),
						new DefaultKeyboardButton(Scancode.DIK_ADD) },
				{ capsLockKeyButton, new AlphabeticKeyboardButton(Scancode.DIK_A),
						new AlphabeticKeyboardButton(Scancode.DIK_S), new AlphabeticKeyboardButton(Scancode.DIK_D),
						new AlphabeticKeyboardButton(Scancode.DIK_F), new AlphabeticKeyboardButton(Scancode.DIK_G),
						new AlphabeticKeyboardButton(Scancode.DIK_H), new AlphabeticKeyboardButton(Scancode.DIK_J),
						new AlphabeticKeyboardButton(Scancode.DIK_K), new AlphabeticKeyboardButton(Scancode.DIK_L),
						new ShiftableKeyboardButton(Scancode.DIK_SEMICOLON, ":"),
						new ShiftableKeyboardButton(Scancode.DIK_APOSTROPHE, "\""),
						new DefaultKeyboardButton(Scancode.DIK_RETURN),
						new NumPadKeyboardButton(Scancode.DIK_NUMPAD4, Scancode.DIK_LEFT),
						new NumPadKeyboardButton(Scancode.DIK_NUMPAD5, ""),
						new NumPadKeyboardButton(Scancode.DIK_NUMPAD6, Scancode.DIK_RIGHT),
						new DefaultKeyboardButton(Scancode.DIK_PRIOR) },
				{ leftShiftKeyboardButton, new AlphabeticKeyboardButton(Scancode.DIK_Z),
						new AlphabeticKeyboardButton(Scancode.DIK_X), new AlphabeticKeyboardButton(Scancode.DIK_C),
						new AlphabeticKeyboardButton(Scancode.DIK_V), new AlphabeticKeyboardButton(Scancode.DIK_B),
						new AlphabeticKeyboardButton(Scancode.DIK_N), new AlphabeticKeyboardButton(Scancode.DIK_M),
						new ShiftableKeyboardButton(Scancode.DIK_COMMA, "<"),
						new ShiftableKeyboardButton(Scancode.DIK_PERIOD, ">"),
						new ShiftableKeyboardButton(Scancode.DIK_SLASH, "?"), rightShiftKeyboardButton,
						new NumPadKeyboardButton(Scancode.DIK_NUMPAD1, Scancode.DIK_END),
						new NumPadKeyboardButton(Scancode.DIK_NUMPAD2, Scancode.DIK_DOWN),
						new NumPadKeyboardButton(Scancode.DIK_NUMPAD3, Scancode.DIK_NEXT),
						new DefaultKeyboardButton(Scancode.DIK_NEXT) },
				{ new DefaultKeyboardButton(Scancode.DIK_LCONTROL), new DefaultKeyboardButton(Scancode.DIK_LWIN),
						new DefaultKeyboardButton(Scancode.DIK_LMENU), new DefaultKeyboardButton(Scancode.DIK_SPACE),
						new DefaultKeyboardButton(Scancode.DIK_RMENU), new DefaultKeyboardButton(Scancode.DIK_RWIN),
						new DefaultKeyboardButton(Scancode.DIK_RCONTROL), new DefaultKeyboardButton(Scancode.DIK_UP),
						new DefaultKeyboardButton(Scancode.DIK_DOWN), new DefaultKeyboardButton(Scancode.DIK_LEFT),
						new DefaultKeyboardButton(Scancode.DIK_RIGHT),
						new NumPadKeyboardButton(Scancode.DIK_NUMPAD0, Scancode.DIK_INSERT),
						new NumPadKeyboardButton(Scancode.DIK_DECIMAL, Scancode.DIK_DELETE),
						new DefaultKeyboardButton(Scancode.DIK_NUMPADENTER) } };

		selectedRow = keyboardButtons.length / 2;
		selectedColumn = keyboardButtons[selectedRow].length / 2;

		frameDragListener = new FrameDragListener(main, this);

		rootPane.setWindowDecorationStyle(JRootPane.NONE);
		setUndecorated(true);
		setResizable(false);
		setTitle(OnScreenKeyboard.class.getSimpleName());
		setType(Window.Type.POPUP);
		setFocusableWindowState(false);
		setBackground(Main.TRANSPARENT);
		rootPane.setBackground(Main.TRANSPARENT);
		setAlwaysOnTop(true);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		addMouseListener(frameDragListener);
		addMouseMotionListener(frameDragListener);

		final var parentPanel = new JPanel();
		parentPanel.setLayout(new BoxLayout(parentPanel, BoxLayout.Y_AXIS));
		parentPanel.setBackground(Main.TRANSPARENT);

		for (var row = 0; row < keyboardButtons.length; row++) {
			final var flowLayout = new FlowLayout(FlowLayout.LEFT, 0, 0);
			final var rowPanel = new JPanel(flowLayout);
			rowPanel.setBackground(ROW_BACKGROUND);
			rowPanel.setBorder(BorderFactory.createEmptyBorder(row == 0 ? ROW_BORDER_WIDTH : 0, ROW_BORDER_WIDTH,
					row == keyboardButtons.length - 1 ? ROW_BORDER_WIDTH : 0, ROW_BORDER_WIDTH));

			for (var column = 0; column < keyboardButtons[row].length; column++) {
				rowPanel.add(keyboardButtons[row][column]);
			}

			parentPanel.add(rowPanel);
		}

		focusSelectedButton();
		add(parentPanel);
	}

	/// Returns a shortened display name for a standard (non-lock) key based on its
	/// DirectInput key code name.
	///
	/// Strips leading directional prefixes ("L ", "R ") and the "Num" prefix,
	/// converts single uppercase letters to lowercase, and replaces arrow-key names
	/// with Unicode arrow characters.
	///
	/// @param directInputKeyCodeName the DirectInput key code name
	/// @return the display name for the key
	private static String getDefaultKeyDisplayName(final String directInputKeyCodeName) {
		if (Scancode.DIK_SYSRQ.equals(directInputKeyCodeName)) {
			return multilineDisplayName("Sys Rq");
		}

		var shortName = directInputKeyCodeName;

		shortName = shortName.replaceFirst("L ", "");
		shortName = shortName.replaceFirst("R ", "");
		shortName = shortName.replaceFirst("Num", "");

		if (directInputKeyCodeName.matches("[A-Z]")) {
			shortName = shortName.toLowerCase(Locale.ROOT);
		} else if (shortName.endsWith("Arrow")) {
			if (shortName.startsWith("Up")) {
				shortName = "↑";
			} else if (shortName.startsWith("Down")) {
				shortName = "↓";
			} else if (shortName.startsWith("Left")) {
				shortName = "←";
			} else if (shortName.startsWith("Right")) {
				shortName = "→";
			}
		}

		return shortName;
	}

	/// Returns the display name for a lock key. Returns the key name unchanged for
	/// Caps Lock; wraps other lock key names in a multiline HTML label.
	///
	/// @param lockKeyName the lock key name
	/// @return the display name for the lock key
	private static String getLockKeyDisplayName(final String lockKeyName) {
		if (LockKey.CAPS_LOCK.equals(lockKeyName)) {
			return lockKeyName;
		}

		return multilineDisplayName(lockKeyName);
	}

	/// Wraps a display name in centered HTML so that the first space becomes a line
	/// break, producing a two-line button label.
	///
	/// @param displayName the display name to wrap
	/// @return an HTML string with the display name split across two centered lines
	private static String multilineDisplayName(final String displayName) {
		return "<html><center>" + displayName.replaceFirst(" ", "<br>") + "</center></html>";
	}

	/// Deselects the currently selected button by releasing it if pressed and
	/// removing its focus highlight.
	///
	/// @return an optional containing the button that was deselected or empty if
	/// no button was selected
	private Optional<AbstractKeyboardButton> deselectButton() {
		final var optionalSelectedButton = getSelectedButton();

		optionalSelectedButton.ifPresent(selectedButton -> {
			if (selectedButton.pressed) {
				selectedButton.release();
			}

			selectedButton.setFocus(false);
		});

		return optionalSelectedButton;
	}

	/// Applies the focus highlight to the currently selected button.
	private void focusSelectedButton() {
		getSelectedButton().ifPresent(selectedButton -> selectedButton.setFocus(true));
	}

	/// Marks all non-lock key buttons as changed, forcing them to be repolled on
	/// the next input cycle.
	public void forceRepoll() {
		anyChanges = true;

		for (final var row : keyboardButtons) {
			for (final var keyboardButton : row) {
				if (keyboardButton instanceof LockKeyButton) {
					continue;
				}

				keyboardButton.changed = true;
			}
		}
	}

	/// Returns the currently selected keyboard button.
	///
	/// @return an optional containing the selected button, or empty if the keyboard
	/// grid has not yet been initialized
	private Optional<AbstractKeyboardButton> getSelectedButton() {
		if (keyboardButtons == null) {
			return Optional.empty();
		}

		return Optional.of(keyboardButtons[selectedRow][selectedColumn]);
	}

	/// Returns whether the keyboard is currently in a shifted state.
	///
	/// @return `true` if either the left or right Shift key is active
	private boolean isKeyboardShifted() {
		return leftShiftKeyboardButton.isShifting() || rightShiftKeyboardButton.isShifting();
	}

	/// Moves the keyboard selector highlight in the specified direction, wrapping
	/// around at edges.
	///
	/// @param direction the direction to move
	@SuppressWarnings({ "NonAtomicOperationOnVolatileField", "NonAtomicVolatileUpdate" })
	public void moveSelector(final Direction direction) {
		EventQueue.invokeLater(() -> {
			final var optionalPreviousButton = deselectButton();

			switch (direction) {
			case UP -> {
				if (selectedRow > 0) {
					selectedRow--;
				} else {
					selectedRow = keyboardButtons.length - 1;
				}

				optionalPreviousButton.ifPresent(this::updateSelectedColumn);
			}
			case DOWN -> {
				if (selectedRow < keyboardButtons.length - 1) {
					selectedRow++;
				} else {
					selectedRow = 0;
				}

				optionalPreviousButton.ifPresent(this::updateSelectedColumn);
			}
			case LEFT -> {
				if (selectedColumn > 0) {
					selectedColumn--;
				} else {
					selectedColumn = keyboardButtons[selectedRow].length - 1;
				}
			}
			case RIGHT -> {
				if (selectedColumn < keyboardButtons[selectedRow].length - 1) {
					selectedColumn++;
				} else {
					selectedColumn = 0;
				}
			}
			}

			focusSelectedButton();
		});
	}

	/// Polls all keyboard buttons and applies their pending key events to the given
	/// input state.
	///
	/// @param input the input state to update
	public void poll(@SuppressWarnings("exports") final Input input) {
		synchronized (keyboardButtons) {
			if (anyChanges) {
				anyChanges = false;

				for (final var row : keyboardButtons) {
					for (final var keyboardButton : row) {
						anyChanges |= keyboardButton.poll(input);
					}
				}
			}
		}
	}

	/// Presses the currently selected keyboard button.
	public void pressSelectedButton() {
		getSelectedButton().ifPresent(selectedButton -> selectedButton.press(false));
	}

	/// Prevents deserialization.
	///
	/// @param ignoredStream ignored
	/// @throws NotSerializableException always
	@Serial
	private void readObject(final ObjectInputStream ignoredStream) throws NotSerializableException {
		throw new NotSerializableException(OnScreenKeyboard.class.getName());
	}

	/// Releases all currently held keyboard buttons.
	public void releaseAllButtons() {
		for (final var row : keyboardButtons) {
			for (final var keyboardButton : row) {
				keyboardButton.release();
			}
		}
	}

	/// Releases the currently selected keyboard button.
	public void releaseSelectedButton() {
		getSelectedButton().ifPresent(AbstractKeyboardButton::release);
	}

	/// Shows or hides the on-screen keyboard. When showing, updates scaling and
	/// applies a rounded window shape if supported. When hiding, releases all held
	/// buttons.
	///
	/// @param b whether to show or hide the keyboard
	@Override
	public void setVisible(final boolean b) {
		synchronized (keyboardButtons) {
			if (b) {
				updateScaling();

				final var graphicsConfiguration = getGraphicsConfiguration();
				final var graphicsDevice = graphicsConfiguration.getDevice();
				if (graphicsDevice
						.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSPARENT)) {
					setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));
				}
			} else {
				releaseAllButtons();
			}
		}

		EventQueue.invokeLater(() -> super.setVisible(b));
	}

	/// Toggles the lock state of the currently selected keyboard button.
	public void toggleLock() {
		getSelectedButton().ifPresent(AbstractKeyboardButton::toggleLock);
	}

	/// Updates the on-screen keyboard's position. Packs the window to its preferred
	/// size and then restores any previously saved location from preferences,
	/// defaulting to the bottom-center of the screen. Does nothing while a drag
	/// operation is in progress.
	void updateLocation() {
		if (frameDragListener.isDragging()) {
			return;
		}

		pack();

		final var maximumWindowBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		final var defaultLocation = new Point();
		defaultLocation.x = (int) maximumWindowBounds.getMaxX() / 2 - getWidth() / 2;
		defaultLocation.y = (int) maximumWindowBounds.getMaxY() - getHeight();

		final var totalDisplayBounds = GuiUtils.getAndStoreTotalDisplayBounds(main);
		GuiUtils.loadFrameLocation(main.getPreferences(), this, defaultLocation, totalDisplayBounds);
	}

	/// Updates button sizes and font sizes to match the current overlay scaling
	/// factor, then repacks and repositions the window.
	private void updateScaling() {
		final var overlayScaling = main.getOverlayScaling();
		final var buttonSideLength = Math.round(AbstractKeyboardButton.BASE_BUTTON_SIZE * overlayScaling);
		final var buttonPreferredSize = new Dimension(buttonSideLength, buttonSideLength);

		final var buttonFont = new Font(Font.SANS_SERIF, Font.BOLD,
				Math.round(AbstractKeyboardButton.BUTTON_FONT_SIZE * overlayScaling));
		for (final var row : keyboardButtons) {
			for (final var keyboardButton : row) {
				keyboardButton.setFont(buttonFont);
				keyboardButton.setPreferredSize(buttonPreferredSize);
				keyboardButton.updateTheme();
			}
		}

		pack();
		updateLocation();
	}

	/// Updates the selected column in the new row to best align with the horizontal
	/// position of the previously selected button.
	///
	/// Prefers a button that fully contains the previous button's x-range;
	/// otherwise selects the button whose center is closest to the previous
	/// button's center.
	///
	/// @param previousButton the button that was selected before the row change
	private void updateSelectedColumn(final AbstractKeyboardButton previousButton) {
		final var previousButtonX = previousButton.getX();
		final var previousButtonWidth = previousButton.getPreferredSize().width;
		final var previousButtonCenter = previousButtonX + previousButtonWidth / 2;
		final var previousButtonMaxX = previousButtonX + previousButtonWidth;

		var minDelta = Integer.MAX_VALUE;
		for (var i = 0; i < keyboardButtons[selectedRow].length; i++) {
			final var iButton = keyboardButtons[selectedRow][i];

			final var iX = iButton.getX();
			final var iWidth = iButton.getPreferredSize().width;
			final var iCenter = iX + iWidth / 2;
			final var iMaxX = iX + iWidth;

			if (previousButtonX >= iX && previousButtonMaxX <= iMaxX) {
				selectedColumn = Math.min(i, keyboardButtons[selectedRow].length - 1);
				break;
			}

			final var delta = Math.abs(previousButtonCenter - iCenter);
			if (delta > minDelta) {
				break;
			}

			selectedColumn = Math.min(i, keyboardButtons[selectedRow].length - 1);
			minDelta = delta;
		}
	}

	/// Prevents serialization.
	///
	/// @param ignoredStream ignored
	/// @throws NotSerializableException always
	@Serial
	private void writeObject(final ObjectOutputStream ignoredStream) throws NotSerializableException {
		throw new NotSerializableException(OnScreenKeyboard.class.getName());
	}

	/// Navigation directions for moving the keyboard selector.
	///
	/// Used with [#moveSelector] to control which key receives focus next
	/// during gamepad-driven navigation of the on-screen keyboard.
	public enum Direction {

		/// Upward navigation.
		UP("DIRECTION_UP"),

		/// Downward navigation.
		DOWN("DIRECTION_DOWN"),

		/// Leftward navigation.
		LEFT("DIRECTION_LEFT"),

		/// Rightward navigation.
		RIGHT("DIRECTION_RIGHT");

		/// Localized display label for this direction.
		private final String label;

		/// Creates a [Direction] constant with the given localization key.
		///
		/// @param labelKey the resource bundle key used to look up the display label
		Direction(final String labelKey) {
			label = Main.STRINGS.getString(labelKey);
		}

		/// Returns the localized label for this direction.
		///
		/// @return the display label
		@Override
		public String toString() {
			return label;
		}
	}

	/// Abstract base class for all on-screen keyboard buttons. Handles focus border
	/// rendering, theme updates, and defines the press/release/poll/toggleLock
	/// contract.
	///
	/// Concrete subclasses implement [#press], [#release], [#poll], and
	/// [#toggleLock] to provide key-type-specific behavior. Focus is indicated
	/// by a red compound border applied by [#setFocus].
	private abstract class AbstractKeyboardButton extends JButton {

		/// Base size in pixels for each keyboard button.
		private static final int BASE_BUTTON_SIZE = 55;

		/// Font size in points used for keyboard button labels.
		private static final int BUTTON_FONT_SIZE = 15;

		/// Thickness in pixels of the focus indicator border.
		private static final int FOCUSED_BUTTON_BORDER_THICKNESS = 2;

		@Serial
		private static final long serialVersionUID = 4567858619453576258L;

		/// Whether this button's visual state changed since the last repaint.
		volatile boolean changed;

		/// Default background color of this button.
		Color defaultBackground;

		/// Default border applied to this button when it is not focused.
		@SuppressWarnings({ "serial", "RedundantSuppression" })
		Border defaultButtonBorder;

		/// Default foreground color of this button.
		Color defaultForeground;

		/// Border applied to this button when it has focus.
		@SuppressWarnings({ "serial", "RedundantSuppression" })
		Border focusedButtonBorder;

		/// Whether this button is currently in a pressed state.
		private volatile boolean pressed;

		/// Creates an [AbstractKeyboardButton] with the given label text.
		///
		/// @param text the button label
		private AbstractKeyboardButton(final String text) {
			super(text);

			updateTheme();
			setMargin(new Insets(1, 1, 1, 1));
		}

		/// Returns whether this button is currently pressed.
		///
		/// @return `true` if the button is pressed
		boolean isPressed() {
			return pressed;
		}

		/// Polls this button's state and applies any pending key events to the given
		/// input.
		///
		/// @param input the input state to update
		/// @return `true` if the button requires continued polling on the next cycle
		abstract boolean poll(final Input input);

		/// Presses this button.
		///
		/// @param lock `true` if the button should be locked down (held indefinitely)
		/// rather than treated as a momentary press
		abstract void press(final boolean lock);

		/// Prevents deserialization.
		///
		/// @param ignoredStream ignored
		/// @throws NotSerializableException always
		@Serial
		private void readObject(final ObjectInputStream ignoredStream) throws NotSerializableException {
			throw new NotSerializableException(AbstractKeyboardButton.class.getName());
		}

		/// Releases this button.
		abstract void release();

		/// Sets the focus highlight on this button. Applies a red compound border when
		/// focused and restores the default border otherwise.
		///
		/// @param focus `true` to apply the focus border, `false` to restore the
		/// default border
		private void setFocus(final boolean focus) {
			setBorder(focus ? focusedButtonBorder : defaultButtonBorder);
		}

		/// Sets whether this button is pressed.
		///
		/// @param pressed `true` to mark the button as pressed, `false` to mark it as
		/// released
		void setPressed(final boolean pressed) {
			this.pressed = pressed;
		}

		/// Toggles the lock state of this button.
		abstract void toggleLock();

		/// Refreshes theme colors and border objects from the current UI manager
		/// settings and reapplies the focus border if this button is currently
		/// selected.
		private void updateTheme() {
			defaultBackground = UIManager.getColor("Button.background");
			defaultForeground = UIManager.getColor("Button.foreground");

			defaultButtonBorder = UIManager.getBorder("Button.border");
			focusedButtonBorder = BorderFactory.createCompoundBorder(defaultButtonBorder,
					BorderFactory.createLineBorder(Color.RED,
							Math.round(FOCUSED_BUTTON_BORDER_THICKNESS * main.getOverlayScaling())));

			if (this == getSelectedButton().orElse(null)) {
				setFocus(true);
			}
		}

		/// Updates the UI delegate and refreshes theme colors and borders.
		@Override
		public void updateUI() {
			super.updateUI();
			updateTheme();
		}

		/// Prevents serialization.
		///
		/// @param ignoredStream ignored
		/// @throws NotSerializableException always
		@Serial
		private void writeObject(final ObjectOutputStream ignoredStream) throws NotSerializableException {
			throw new NotSerializableException(AbstractKeyboardButton.class.getName());
		}
	}

	/// A keyboard button for alphabetic keys whose display toggles between
	/// lowercase and uppercase based on the Shift /Caps Lock state.
	///
	/// The label is shown in uppercase when either Shift is held or Caps Lock
	/// is active, and in lowercase otherwise.
	private final class AlphabeticKeyboardButton extends ShiftableKeyboardButton {

		@Serial
		private static final long serialVersionUID = -43249779147068577L;

		/// Creates an [AlphabeticKeyboardButton] for the key identified by the given
		/// DirectInput key code name.
		///
		/// @param directInputKeyCodeName the DirectInput key code name
		private AlphabeticKeyboardButton(final String directInputKeyCodeName) {
			super(directInputKeyCodeName, directInputKeyCodeName);
		}
	}

	/// The Caps Lock button. Toggling this lock updates all shiftable key labels to
	/// reflect the current case.
	///
	/// When Caps Lock is toggled, each [ShiftableKeyboardButton] in the keyboard
	/// grid is updated to show either its default or its alternative label,
	/// taking the current Shift key state into account.
	private final class CapsLockKeyButton extends LockKeyButton {

		@Serial
		private static final long serialVersionUID = 6891401614243607392L;

		/// Creates a [CapsLockKeyButton].
		private CapsLockKeyButton() {
			super(LockKey.CAPS_LOCK_LOCK_KEY);
		}

		@Override
		void toggleLock() {
			super.toggleLock();

			final var showAlternativeKeyName = locked.get() ^ isKeyboardShifted();

			for (final var row : keyboardButtons) {
				for (final var keyboardButton : row) {
					if (keyboardButton instanceof final ShiftableKeyboardButton shiftableKeyboardButton) {
						shiftableKeyboardButton.setShowAlternativeKeyName(showAlternativeKeyName);
					}
				}
			}
		}
	}

	/// Standard keyboard button that sends key-down and key-up events. Supports
	/// mouse interaction, hold-to-repeat behavior after a minimum press time, and
	/// toggle locking.
	///
	/// Short presses produce a down-up event pair; holding the button beyond
	/// `MIN_REPEAT_PRESS_TIME` keeps the key in the down state. Right-clicking
	/// the button toggles its lock state.
	private class DefaultKeyboardButton extends AbstractKeyboardButton {

		/// Minimum time in milliseconds a button must be held before repeating.
		private static final long MIN_REPEAT_PRESS_TIME = 150L;

		@Serial
		private static final long serialVersionUID = -1739002089027358633L;

		/// DirectInput key code name identifying the key this button represents.
		final String directInputKeyCodeName;

		/// Keystroke sent to the input layer when this button is pressed.
		private final Keystroke keystroke;

		/// Timestamp in milliseconds when the current press began.
		private volatile long beginPressTime;

		/// Whether a down-up event pair should be sent on the next poll cycle.
		private volatile boolean doDownUp;

		/// Whether the mouse button is currently held down over this button.
		private volatile boolean mouseDown;

		/// Creates a [DefaultKeyboardButton] for the key identified by the given
		/// DirectInput key code name.
		///
		/// @param directInputKeyCodeName the DirectInput key code name
		private DefaultKeyboardButton(final String directInputKeyCodeName) {
			super(getDefaultKeyDisplayName(directInputKeyCodeName));

			this.directInputKeyCodeName = directInputKeyCodeName;

			final Scancode[] keyScancodes;
			final Scancode[] modifierScancodes;

			final var scancode = Scancode.NAME_TO_SCAN_CODE_MAP.get(directInputKeyCodeName);

			if (Scancode.DIK_LMENU.equals(directInputKeyCodeName) || Scancode.DIK_RMENU.equals(directInputKeyCodeName)
					|| Scancode.DIK_LSHIFT.equals(directInputKeyCodeName)
					|| Scancode.DIK_RSHIFT.equals(directInputKeyCodeName)
					|| Scancode.DIK_LCONTROL.equals(directInputKeyCodeName)
					|| Scancode.DIK_RCONTROL.equals(directInputKeyCodeName)
					|| Scancode.DIK_LWIN.equals(directInputKeyCodeName)
					|| Scancode.DIK_RWIN.equals(directInputKeyCodeName)) {
				keyScancodes = new Scancode[0];
				modifierScancodes = new Scancode[] { scancode };
			} else {
				keyScancodes = new Scancode[] { scancode };
				modifierScancodes = new Scancode[0];
			}

			keystroke = new Keystroke(keyScancodes, modifierScancodes);

			addMouseListener(new MouseListener() {

				@Override
				public void mouseClicked(final MouseEvent e) {
				}

				@Override
				public void mouseEntered(final MouseEvent e) {
				}

				@Override
				public void mouseExited(final MouseEvent e) {
				}

				@Override
				public void mousePressed(final MouseEvent e) {
					if (SwingUtilities.isLeftMouseButton(e)) {
						mouseDown = true;
						press(false);
					}
				}

				@Override
				public void mouseReleased(final MouseEvent e) {
					if (SwingUtilities.isLeftMouseButton(e)) {
						mouseDown = false;
						release();
					} else if (SwingUtilities.isRightMouseButton(e)) {
						toggleLock();
					}
				}
			});
		}

		/// Returns the preferred size, adjusted by key-specific width multipliers
		/// (e.g., greater width for Space, Backspace, Shift).
		///
		/// @return the preferred size
		@SuppressWarnings({ "NarrowingCompoundAssignment", "lossy-conversions" })
		@Override
		public Dimension getPreferredSize() {
			final var preferredSize = super.getPreferredSize();

			if (this instanceof NumPadKeyboardButton) {
				if (Scancode.DIK_NUMPAD0.equals(directInputKeyCodeName)) {
					preferredSize.width *= 2;
				}
			} else if (Scancode.DIK_INSERT.equals(directInputKeyCodeName)
					|| Scancode.DIK_DELETE.equals(directInputKeyCodeName)) {
				preferredSize.width *= 0.75;
			} else if (Scancode.DIK_TAB.equals(directInputKeyCodeName)) {
				preferredSize.width *= 1.5;
			} else if (Scancode.DIK_BACKSLASH.equals(directInputKeyCodeName)) {
				preferredSize.width *= 2;
			} else if (Scancode.DIK_LSHIFT.equals(directInputKeyCodeName)
					|| Scancode.DIK_RETURN.equals(directInputKeyCodeName)
					|| Scancode.DIK_BACK.equals(directInputKeyCodeName)) {
				preferredSize.width *= 2.5;
			} else if (Scancode.DIK_RSHIFT.equals(directInputKeyCodeName)) {
				preferredSize.width *= 3;
			} else if (Scancode.DIK_SPACE.equals(directInputKeyCodeName)) {
				preferredSize.width *= 5.5;
			}

			return preferredSize;
		}

		@Override
		boolean poll(final Input input) {
			if (mouseDown || changed) {
				if (doDownUp) {
					input.getDownUpKeystrokes().add(keystroke);
					doDownUp = false;
				} else {
					final var downKeystrokes = input.getDownKeystrokes();

					if (heldButtons.contains(this)) {
						if (System.currentTimeMillis() - beginPressTime >= MIN_REPEAT_PRESS_TIME) {
							downKeystrokes.add(keystroke);
						}
					} else {
						downKeystrokes.remove(keystroke);
					}
				}

				changed = false;
			}
			return mouseDown;
		}

		@Override
		void press(final boolean lock) {
			GuiUtils.invokeOnEventDispatchThreadIfRequired(() -> setBackground(KEYBOARD_BUTTON_HELD_BACKGROUND));

			if (heldButtons.add(this)) {
				beginPressTime = System.currentTimeMillis();
			}

			changed = true;
			anyChanges = true;

			setPressed(!lock);
		}

		@Override
		void release() {
			GuiUtils.invokeOnEventDispatchThreadIfRequired(() -> setBackground(defaultBackground));

			if (heldButtons.remove(this)) {
				if (System.currentTimeMillis() - beginPressTime < MIN_REPEAT_PRESS_TIME) {
					doDownUp = true;
				}

				beginPressTime = 0L;
				changed = true;
				anyChanges = true;
			}

			setPressed(false);
		}

		@Override
		void toggleLock() {
			if (heldButtons.contains(this)) {
				release();
			} else {
				press(true);
				beginPressTime = 0L;
			}
		}
	}

	/// A keyboard button that can display one of two labels depending on a toggle
	/// state (e.g., shifted or Num Lock active).
	///
	/// Subclasses provide the two label values and call
	/// [#setShowAlternativeKeyName] to switch between them in response to
	/// modifier key or lock key state changes.
	private abstract class DualPurposeKeyboardButton extends DefaultKeyboardButton {

		@Serial
		private static final long serialVersionUID = -722664487208124876L;

		/// Alternative label shown when the keyboard is in shifted or lock state.
		String alternativeKeyName;

		/// Default label shown in the normal (unshifted) keyboard state.
		String defaultKeyName;

		/// Creates a [DualPurposeKeyboardButton] with a default and an alternative
		/// label.
		///
		/// @param directInputKeyCodeName the DirectInput key code name used for the
		/// default label
		/// @param shiftedKeyName the alternative label shown when the keyboard is in
		/// the shifted or alternative state
		private DualPurposeKeyboardButton(final String directInputKeyCodeName, final String shiftedKeyName) {
			super(directInputKeyCodeName);

			defaultKeyName = getText();
			alternativeKeyName = shiftedKeyName;
		}

		/// Sets whether to display the alternative key name instead of the default
		/// label.
		///
		/// @param showAlternativeKeyName `true` to display the alternative label,
		/// `false` to display the default label
		void setShowAlternativeKeyName(final boolean showAlternativeKeyName) {
			setText(showAlternativeKeyName ? alternativeKeyName : defaultKeyName);
		}
	}

	/// A keyboard button representing a lock key (Caps Lock, Num Lock, Scroll
	/// Lock). Toggling changes the button's foreground color to green when locked.
	///
	/// The lock state is stored in an [java.util.concurrent.atomic.AtomicBoolean]
	/// and propagated to the input layer via `onLockKeys` or `offLockKeys`
	/// during the next [#poll] cycle.
	private class LockKeyButton extends AbstractKeyboardButton {

		@Serial
		private static final long serialVersionUID = 4014130700331413635L;

		/// Current lock state of this button.
		final AtomicBoolean locked = new AtomicBoolean();

		/// The lock key that this button controls.
		@SuppressWarnings({ "serial", "RedundantSuppression" })
		private final LockKey lockKey;

		/// Whether the button was in the up (unpressed) state during the previous poll.
		private volatile boolean wasUp = true;

		/// Creates a [LockKeyButton] for the given lock key.
		///
		/// @param lockKey the lock key represented by this button
		private LockKeyButton(final LockKey lockKey) {
			super(getLockKeyDisplayName(lockKey.name()));
			this.lockKey = lockKey;

			addActionListener(_ -> toggleLock());
		}

		/// Returns the preferred size, doubled in width for the Caps Lock key.
		///
		/// @return the preferred size
		@Override
		public Dimension getPreferredSize() {
			final var preferredSize = super.getPreferredSize();

			if (lockKey.virtualKeyCode() == KeyEvent.VK_CAPS_LOCK) {
				preferredSize.width *= 2;
			}

			return preferredSize;
		}

		@Override
		boolean poll(final Input input) {
			if (changed) {
				if (locked.get()) {
					input.getOnLockKeys().add(lockKey);
				} else {
					input.getOffLockKeys().add(lockKey);
				}

				changed = false;
			}

			return false;
		}

		@Override
		void press(final boolean lock) {
			if (wasUp) {
				toggleLock();
				wasUp = false;
			}

			setPressed(true);
		}

		@Override
		void release() {
			wasUp = true;
			setPressed(false);
		}

		@Override
		void toggleLock() {
			boolean temp;
			do {
				temp = locked.get();
			} while (!locked.compareAndSet(temp, !temp));

			final var locked = !temp;

			GuiUtils.invokeOnEventDispatchThreadIfRequired(
					() -> setForeground(locked ? Color.GREEN : defaultForeground));
			changed = true;
			anyChanges = true;
		}
	}

	/// The Num Lock button. Toggling this lock switches all numpad buttons between
	/// their numeric and navigation labels.
	///
	/// When Num Lock is toggled, every [NumPadKeyboardButton] in the keyboard
	/// grid is updated to show either its numeric or its navigation label.
	private final class NumLockKeyButton extends LockKeyButton {

		@Serial
		private static final long serialVersionUID = 296846375213986255L;

		/// Creates a [NumLockKeyButton].
		private NumLockKeyButton() {
			super(LockKey.NUM_LOCK_LOCK_KEY);
		}

		@Override
		void toggleLock() {
			super.toggleLock();

			for (final var row : keyboardButtons) {
				for (final var keyboardButton : row) {
					if (keyboardButton instanceof final NumPadKeyboardButton numPadKeyboardButton) {
						numPadKeyboardButton.setShowAlternativeKeyName(locked.get());
					}
				}
			}
		}
	}

	/// A numpad keyboard button that displays its numeric label when Num Lock is
	/// on and a navigation label (e.g., Home, End, arrows) when Num Lock is off.
	///
	/// The label roles are swapped relative to [DualPurposeKeyboardButton]
	/// convention so that the navigation label is the default and the digit
	/// label is the alternative, matching standard Num Lock-off behavior.
	private final class NumPadKeyboardButton extends DualPurposeKeyboardButton {

		@Serial
		private static final long serialVersionUID = -460568797568937461L;

		/// Creates a [NumPadKeyboardButton] for the given numpad key.
		///
		/// The default label shows the navigation symbol (Num Lock off) and the
		/// alternative label shows the digit (Num Lock on).
		///
		/// @param directInputKeyCodeName the DirectInput key code name for the digit
		/// key
		/// @param numLockOffDirectInputKeyCodeName the DirectInput key code name for
		/// the navigation key shown when Num Lock is off
		private NumPadKeyboardButton(final String directInputKeyCodeName,
				final String numLockOffDirectInputKeyCodeName) {
			super(directInputKeyCodeName, getDefaultKeyDisplayName(numLockOffDirectInputKeyCodeName));

			final var tempAlternativeName = alternativeKeyName;
			alternativeKeyName = defaultKeyName;
			defaultKeyName = tempAlternativeName;

			setText(defaultKeyName);
		}
	}

	/// A Shift key button. When pressed or held, it toggles the display of all
	/// shiftable keyboard buttons between their default and shifted labels.
	///
	/// Pressing either the left or right Shift button updates every
	/// [ShiftableKeyboardButton] in the grid, taking the Caps Lock state into
	/// account to determine whether to show the shifted or unshifted label.
	private final class ShiftKeyboardButton extends DefaultKeyboardButton {

		@Serial
		private static final long serialVersionUID = -1789796245988164919L;

		/// Creates a [ShiftKeyboardButton] for the given Shift key type.
		///
		/// @param type the type identifying whether this is the left or right Shift key
		private ShiftKeyboardButton(final ShiftKeyboardButtonType type) {
			super(type.directInputKeyCodeName);
		}

		/// Returns whether this Shift key is currently active (pressed or held).
		///
		/// @return `true` if the Shift key is pressed or held down
		private boolean isShifting() {
			return isPressed() || heldButtons.contains(this);
		}

		@Override
		void setPressed(final boolean pressed) {
			super.setPressed(pressed);

			final var showAlternativeKeyName = isKeyboardShifted() ^ capsLockKeyButton.locked.get();

			for (final var row : keyboardButtons) {
				for (final var keyboardButton : row) {
					if (keyboardButton instanceof final ShiftableKeyboardButton shiftableKeyboardButton) {
						shiftableKeyboardButton.setShowAlternativeKeyName(showAlternativeKeyName);
					}
				}
			}
		}

		/// Identifies the left or right Shift key by its DirectInput scancode.
		///
		/// Each constant carries the scancode name used to look up the key's
		/// [Keystroke] when constructing a [ShiftKeyboardButton].
		private enum ShiftKeyboardButtonType {

			/// Left Shift key.
			LEFT(Scancode.DIK_LSHIFT),
			/// Right Shift key.
			RIGHT(Scancode.DIK_RSHIFT);

			/// DirectInput key code name for this Shift key.
			private final String directInputKeyCodeName;

			/// Creates a [ShiftKeyboardButtonType] constant with the given DirectInput
			/// key code name.
			///
			/// @param directInputKeyCodeName the DirectInput key code name for this
			/// Shift key
			ShiftKeyboardButtonType(final String directInputKeyCodeName) {
				this.directInputKeyCodeName = directInputKeyCodeName;
			}
		}
	}

	/// A keyboard button whose label changes based on Shift/Caps Lock state (e.g.,
	/// `1` becomes `!` when shifted).
	///
	/// The default label is shown when neither Shift is held nor Caps Lock is
	/// active; the alternative label is shown when the keyboard is in a shifted
	/// state.
	private class ShiftableKeyboardButton extends DualPurposeKeyboardButton {

		@Serial
		private static final long serialVersionUID = -106361505843077547L;

		/// Creates a [ShiftableKeyboardButton] with a default and a shifted label.
		///
		/// @param directInputKeyCodeName the DirectInput key code name used for the
		/// default (unshifted) label
		/// @param shiftedKeyName the label shown when the keyboard is in the shifted
		/// state
		private ShiftableKeyboardButton(final String directInputKeyCodeName, final String shiftedKeyName) {
			super(directInputKeyCodeName, shiftedKeyName);
		}
	}
}
