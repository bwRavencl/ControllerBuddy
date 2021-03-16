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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.RoundRectangle2D;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;

import de.bwravencl.controllerbuddy.gui.GuiUtils.FrameDragListener;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.KeyStroke;
import de.bwravencl.controllerbuddy.input.LockKey;
import de.bwravencl.controllerbuddy.input.Mode;
import de.bwravencl.controllerbuddy.input.ScanCode;

@SuppressWarnings("serial")
public final class OnScreenKeyboard extends JFrame {

	private abstract class AbstractKeyboardButton extends JButton {

		private static final long serialVersionUID = 4567858619453576258L;

		private static final int BASE_BUTTON_SIZE = 55;

		volatile boolean changed;
		volatile boolean pressed;

		Color defaultBackground;
		Color defaultForeground;

		Border defaultButtonBorder;
		Border focusedButtonBorder;

		private AbstractKeyboardButton(final String text) {
			super(text);

			updateTheme();
			setMargin(new Insets(1, 1, 1, 1));
			setFont(getFont().deriveFont(Font.BOLD));
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(BASE_BUTTON_SIZE, BASE_BUTTON_SIZE);
		}

		abstract boolean poll(final Input input);

		abstract void press(final boolean lock);

		abstract void release();

		private void setFocus(final boolean focus) {
			setBorder(focus ? focusedButtonBorder : defaultButtonBorder);
			if (!focus)
				OnScreenKeyboard.this.repaint();
		}

		abstract void toggleLock();

		private void updateTheme() {
			defaultBackground = UIManager.getColor("Button.background");
			defaultForeground = UIManager.getColor("Button.foreground");

			defaultButtonBorder = UIManager.getBorder("Button.border");
			focusedButtonBorder = BorderFactory.createLineBorder(Color.RED, 2);
		}

		@Override
		public void updateUI() {
			super.updateUI();
			updateTheme();
		}
	}

	private final class DefaultKeyboardButton extends AbstractKeyboardButton {

		private static final long serialVersionUID = -1739002089027358633L;

		private static final long MIN_REPEAT_PRESS_TIME = 150L;

		private volatile boolean mouseDown;

		private volatile boolean doDownUp;

		private volatile long beginPress;

		private final String directInputKeyCodeName;

		private final KeyStroke keyStroke;

		private DefaultKeyboardButton(final String directInputKeyCodeName) {
			super(getShortDefaultKeyName(directInputKeyCodeName));

			this.directInputKeyCodeName = directInputKeyCodeName;

			final Integer[] keyCodes;
			final Integer[] modifierCodes;

			if (ScanCode.DIK_LMENU.equals(directInputKeyCodeName) || ScanCode.DIK_RMENU.equals(directInputKeyCodeName)
					|| ScanCode.DIK_LSHIFT.equals(directInputKeyCodeName)
					|| ScanCode.DIK_RSHIFT.equals(directInputKeyCodeName)
					|| ScanCode.DIK_LCONTROL.equals(directInputKeyCodeName)
					|| ScanCode.DIK_RCONTROL.equals(directInputKeyCodeName)
					|| ScanCode.DIK_LWIN.equals(directInputKeyCodeName)
					|| ScanCode.DIK_RWIN.equals(directInputKeyCodeName)) {
				keyCodes = new Integer[0];
				modifierCodes = new Integer[] { ScanCode.nameToKeyCodeMap.get(directInputKeyCodeName) };
			} else {
				keyCodes = new Integer[] { ScanCode.nameToKeyCodeMap.get(directInputKeyCodeName) };
				modifierCodes = new Integer[0];
			}

			keyStroke = new KeyStroke(keyCodes, modifierCodes);

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
					} else if (SwingUtilities.isRightMouseButton(e))
						toggleLock();
				}
			});
		}

		@Override
		public Dimension getPreferredSize() {
			final var preferredSize = super.getPreferredSize();

			if (ScanCode.DIK_INSERT.equals(directInputKeyCodeName) || ScanCode.DIK_DELETE.equals(directInputKeyCodeName)
					|| ScanCode.DIK_HOME.equals(directInputKeyCodeName)
					|| ScanCode.DIK_END.equals(directInputKeyCodeName))
				preferredSize.width *= 0.88;
			else if (ScanCode.DIK_TAB.equals(directInputKeyCodeName))
				preferredSize.width *= 1.5;
			else if (ScanCode.DIK_BACKSLASH.equals(directInputKeyCodeName)
					|| ScanCode.DIK_NUMPAD0.equals(directInputKeyCodeName))
				preferredSize.width *= 2;
			else if (ScanCode.DIK_LSHIFT.equals(directInputKeyCodeName)
					|| ScanCode.DIK_RETURN.equals(directInputKeyCodeName)
					|| ScanCode.DIK_BACK.equals(directInputKeyCodeName))
				preferredSize.width *= 2.5;
			else if (ScanCode.DIK_RSHIFT.equals(directInputKeyCodeName))
				preferredSize.width *= 3;
			else if (ScanCode.DIK_SPACE.equals(directInputKeyCodeName))
				preferredSize.width *= 4.5;

			return preferredSize;
		}

		@Override
		boolean poll(final Input input) {
			if (mouseDown || changed) {

				if (doDownUp) {
					input.getDownUpKeyStrokes().add(keyStroke);
					doDownUp = false;
				} else {
					final var downKeyStrokes = input.getDownKeyStrokes();

					if (heldButtons.contains(this)) {
						if (System.currentTimeMillis() - beginPress >= MIN_REPEAT_PRESS_TIME)
							downKeyStrokes.add(keyStroke);
					} else
						downKeyStrokes.remove(keyStroke);
				}

				changed = false;
			}
			return mouseDown;
		}

		@Override
		void press(final boolean lock) {
			GuiUtils.invokeOnEventDispatchThreadIfRequired(() -> setBackground(KEYBOARD_BUTTON_HELD_BACKGROUND));

			if (heldButtons.add(this))
				beginPress = System.currentTimeMillis();

			changed = true;
			anyChanges = true;
			pressed = !lock;
		}

		@Override
		void release() {
			GuiUtils.invokeOnEventDispatchThreadIfRequired(() -> setBackground(defaultBackground));

			if (heldButtons.remove(this)) {
				if (System.currentTimeMillis() - beginPress < MIN_REPEAT_PRESS_TIME)
					doDownUp = true;

				beginPress = 0L;
				changed = true;
				anyChanges = true;
			}

			pressed = false;
		}

		@Override
		void toggleLock() {
			if (heldButtons.contains(this))
				release();
			else {
				press(true);
				beginPress = 0L;
			}
		}
	}

	private final class LockKeyButton extends AbstractKeyboardButton {

		private static final long serialVersionUID = 4014130700331413635L;

		private volatile boolean locked;

		private final int virtualKeyCode;

		private boolean wasUp = true;

		private LockKeyButton(final int virtualKeyCode) {
			super(getShortLockKeyName(LockKey.virtualKeyCodeToLockKeyMap.get(virtualKeyCode).name));
			this.virtualKeyCode = virtualKeyCode;

			addActionListener(arg0 -> {
				toggleLock();
			});
		}

		@Override
		public Dimension getPreferredSize() {
			final var preferredSize = super.getPreferredSize();

			if (virtualKeyCode == KeyEvent.VK_CAPS_LOCK)
				preferredSize.width *= 2;

			return preferredSize;
		}

		@Override
		boolean poll(final Input input) {
			if (changed) {
				if (locked)
					input.getOnLockKeys().add(virtualKeyCode);
				else
					input.getOffLockKeys().add(virtualKeyCode);

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

			pressed = true;
		}

		@Override
		void release() {
			wasUp = true;
			pressed = false;
		}

		@Override
		void toggleLock() {
			locked = !locked;
			GuiUtils.invokeOnEventDispatchThreadIfRequired(() -> {
				setForeground(locked ? Color.GREEN : defaultForeground);
			});
			changed = true;
			anyChanges = true;
		}
	}

	private static final UUID ON_SCREEN_KEYBOARD_MODE_UUID = UUID.fromString("daf53639-9518-48db-bd63-19cde7bf9a96");

	private static final Color KEYBOARD_BUTTON_HELD_BACKGROUND = new Color(128, 128, 128);

	private static final Set<AbstractKeyboardButton> heldButtons = ConcurrentHashMap.newKeySet();

	public static final Mode onScreenKeyboardMode;

	private static final int ROW_BORDER_WIDTH = 15;

	private static final Color ROW_BACKGROUND = new Color(128, 128, 128, 64);

	static {
		onScreenKeyboardMode = new Mode(ON_SCREEN_KEYBOARD_MODE_UUID);
		onScreenKeyboardMode.setDescription(Main.strings.getString("ON_SCREEN_KEYBOARD_MODE_DESCRIPTION"));
	}

	private static String getShortDefaultKeyName(final String directInputKeyCodeName) {
		var shortName = directInputKeyCodeName;

		shortName = shortName.replaceFirst("L ", "");
		shortName = shortName.replaceFirst("R ", "");
		shortName = shortName.replaceFirst("Num", "");
		shortName = shortName.replaceFirst("App ", "");
		if (shortName.endsWith("Arrow"))
			if (shortName.startsWith("Up"))
				shortName = "\u2191";
			else if (shortName.startsWith("Down"))
				shortName = "\u2193";
			else if (shortName.startsWith("Left"))
				shortName = "\u2190";
			else if (shortName.startsWith("Right"))
				shortName = "\u2192";

		return shortName;
	}

	private static String getShortLockKeyName(final String lockKeyName) {
		var shortName = lockKeyName;

		if (!LockKey.CAPS_LOCK.equals(lockKeyName))
			shortName = shortName.replaceFirst(LockKey.LOCK_SUFFIX, "Lk");

		return shortName;
	}

	private final AbstractKeyboardButton[][] keyboardButtons = {
			{ new DefaultKeyboardButton(ScanCode.DIK_ESCAPE), new DefaultKeyboardButton(ScanCode.DIK_F1),
					new DefaultKeyboardButton(ScanCode.DIK_F2), new DefaultKeyboardButton(ScanCode.DIK_F3),
					new DefaultKeyboardButton(ScanCode.DIK_F4), new DefaultKeyboardButton(ScanCode.DIK_F5),
					new DefaultKeyboardButton(ScanCode.DIK_F6), new DefaultKeyboardButton(ScanCode.DIK_F7),
					new DefaultKeyboardButton(ScanCode.DIK_F8), new DefaultKeyboardButton(ScanCode.DIK_F9),
					new DefaultKeyboardButton(ScanCode.DIK_F10), new DefaultKeyboardButton(ScanCode.DIK_F11),
					new DefaultKeyboardButton(ScanCode.DIK_F12), new DefaultKeyboardButton(ScanCode.DIK_SYSRQ),
					new LockKeyButton(KeyEvent.VK_SCROLL_LOCK), new DefaultKeyboardButton(ScanCode.DIK_PAUSE),
					new DefaultKeyboardButton(ScanCode.DIK_INSERT), new DefaultKeyboardButton(ScanCode.DIK_DELETE),
					new DefaultKeyboardButton(ScanCode.DIK_HOME), new DefaultKeyboardButton(ScanCode.DIK_END) },
			{ new DefaultKeyboardButton(ScanCode.DIK_GRAVE), new DefaultKeyboardButton(ScanCode.DIK_1),
					new DefaultKeyboardButton(ScanCode.DIK_2), new DefaultKeyboardButton(ScanCode.DIK_3),
					new DefaultKeyboardButton(ScanCode.DIK_4), new DefaultKeyboardButton(ScanCode.DIK_5),
					new DefaultKeyboardButton(ScanCode.DIK_6), new DefaultKeyboardButton(ScanCode.DIK_7),
					new DefaultKeyboardButton(ScanCode.DIK_8), new DefaultKeyboardButton(ScanCode.DIK_9),
					new DefaultKeyboardButton(ScanCode.DIK_0), new DefaultKeyboardButton(ScanCode.DIK_MINUS),
					new DefaultKeyboardButton(ScanCode.DIK_EQUALS), new DefaultKeyboardButton(ScanCode.DIK_BACK),
					new LockKeyButton(KeyEvent.VK_NUM_LOCK), new DefaultKeyboardButton(ScanCode.DIK_DIVIDE),
					new DefaultKeyboardButton(ScanCode.DIK_MULTIPLY),
					new DefaultKeyboardButton(ScanCode.DIK_SUBTRACT) },
			{ new DefaultKeyboardButton(ScanCode.DIK_TAB), new DefaultKeyboardButton(ScanCode.DIK_Q),
					new DefaultKeyboardButton(ScanCode.DIK_W), new DefaultKeyboardButton(ScanCode.DIK_E),
					new DefaultKeyboardButton(ScanCode.DIK_R), new DefaultKeyboardButton(ScanCode.DIK_T),
					new DefaultKeyboardButton(ScanCode.DIK_Y), new DefaultKeyboardButton(ScanCode.DIK_U),
					new DefaultKeyboardButton(ScanCode.DIK_I), new DefaultKeyboardButton(ScanCode.DIK_O),
					new DefaultKeyboardButton(ScanCode.DIK_P), new DefaultKeyboardButton(ScanCode.DIK_LBRACKET),
					new DefaultKeyboardButton(ScanCode.DIK_RBRACKET), new DefaultKeyboardButton(ScanCode.DIK_BACKSLASH),
					new DefaultKeyboardButton(ScanCode.DIK_NUMPAD7), new DefaultKeyboardButton(ScanCode.DIK_NUMPAD8),
					new DefaultKeyboardButton(ScanCode.DIK_NUMPAD9), new DefaultKeyboardButton(ScanCode.DIK_ADD) },
			{ new LockKeyButton(KeyEvent.VK_CAPS_LOCK), new DefaultKeyboardButton(ScanCode.DIK_A),
					new DefaultKeyboardButton(ScanCode.DIK_S), new DefaultKeyboardButton(ScanCode.DIK_D),
					new DefaultKeyboardButton(ScanCode.DIK_F), new DefaultKeyboardButton(ScanCode.DIK_G),
					new DefaultKeyboardButton(ScanCode.DIK_H), new DefaultKeyboardButton(ScanCode.DIK_J),
					new DefaultKeyboardButton(ScanCode.DIK_K), new DefaultKeyboardButton(ScanCode.DIK_L),
					new DefaultKeyboardButton(ScanCode.DIK_SEMICOLON),
					new DefaultKeyboardButton(ScanCode.DIK_APOSTROPHE), new DefaultKeyboardButton(ScanCode.DIK_RETURN),
					new DefaultKeyboardButton(ScanCode.DIK_NUMPAD4), new DefaultKeyboardButton(ScanCode.DIK_NUMPAD5),
					new DefaultKeyboardButton(ScanCode.DIK_NUMPAD6), new DefaultKeyboardButton(ScanCode.DIK_PRIOR) },
			{ new DefaultKeyboardButton(ScanCode.DIK_LSHIFT), new DefaultKeyboardButton(ScanCode.DIK_Z),
					new DefaultKeyboardButton(ScanCode.DIK_X), new DefaultKeyboardButton(ScanCode.DIK_C),
					new DefaultKeyboardButton(ScanCode.DIK_V), new DefaultKeyboardButton(ScanCode.DIK_B),
					new DefaultKeyboardButton(ScanCode.DIK_N), new DefaultKeyboardButton(ScanCode.DIK_M),
					new DefaultKeyboardButton(ScanCode.DIK_COMMA), new DefaultKeyboardButton(ScanCode.DIK_PERIOD),
					new DefaultKeyboardButton(ScanCode.DIK_SLASH), new DefaultKeyboardButton(ScanCode.DIK_RSHIFT),
					new DefaultKeyboardButton(ScanCode.DIK_NUMPAD1), new DefaultKeyboardButton(ScanCode.DIK_NUMPAD2),
					new DefaultKeyboardButton(ScanCode.DIK_NUMPAD3), new DefaultKeyboardButton(ScanCode.DIK_NEXT) },
			{ new DefaultKeyboardButton(ScanCode.DIK_LCONTROL), new DefaultKeyboardButton(ScanCode.DIK_LWIN),
					new DefaultKeyboardButton(ScanCode.DIK_LMENU), new DefaultKeyboardButton(ScanCode.DIK_SPACE),
					new DefaultKeyboardButton(ScanCode.DIK_RMENU), new DefaultKeyboardButton(ScanCode.DIK_RWIN),
					new DefaultKeyboardButton(ScanCode.DIK_APPS), new DefaultKeyboardButton(ScanCode.DIK_RCONTROL),
					new DefaultKeyboardButton(ScanCode.DIK_UP), new DefaultKeyboardButton(ScanCode.DIK_DOWN),
					new DefaultKeyboardButton(ScanCode.DIK_LEFT), new DefaultKeyboardButton(ScanCode.DIK_RIGHT),
					new DefaultKeyboardButton(ScanCode.DIK_NUMPAD0),
					new DefaultKeyboardButton(ScanCode.DIK_NUMPADCOMMA),
					new DefaultKeyboardButton(ScanCode.DIK_NUMPADENTER) } };

	private final Main main;
	private final FrameDragListener frameDragListener;
	private volatile boolean anyChanges;
	private int selectedRow = keyboardButtons.length / 2;
	private int selectedColumn = keyboardButtons[selectedRow].length / 2;

	OnScreenKeyboard(final Main main) {
		this.main = main;
		frameDragListener = new FrameDragListener(main, this);

		rootPane.setWindowDecorationStyle(JRootPane.NONE);
		setUndecorated(true);
		setTitle(OnScreenKeyboard.class.getSimpleName());
		setType(JFrame.Type.UTILITY);
		setFocusableWindowState(false);
		setBackground(Main.TRANSPARENT);
		setAlwaysOnTop(true);

		addMouseListener(frameDragListener);
		addMouseMotionListener(frameDragListener);

		final var parentPanel = new JPanel();
		parentPanel.setLayout(new BoxLayout(parentPanel, BoxLayout.Y_AXIS));
		parentPanel.setBackground(Main.TRANSPARENT);

		for (var row = 0; row < keyboardButtons.length; row++) {
			final var flowLayout = new FlowLayout(FlowLayout.LEFT);
			flowLayout.setHgap(0);
			flowLayout.setVgap(0);
			final var rowPanel = new JPanel(flowLayout);
			rowPanel.setBackground(ROW_BACKGROUND);
			rowPanel.setBorder(BorderFactory.createEmptyBorder(row == 0 ? ROW_BORDER_WIDTH : 0, ROW_BORDER_WIDTH,
					row == keyboardButtons.length - 1 ? ROW_BORDER_WIDTH : 0, ROW_BORDER_WIDTH));

			for (var column = 0; column < keyboardButtons[row].length; column++)
				rowPanel.add(keyboardButtons[row][column]);

			parentPanel.add(rowPanel);
		}

		focusCurrentButton();
		add(parentPanel);
		updateLocation();
	}

	private void focusCurrentButton() {
		keyboardButtons[selectedRow][selectedColumn].setFocus(true);
	}

	public void forceRepoll() {
		anyChanges = true;

		for (final var row : keyboardButtons)
			for (final var keyboardButton : row)
				keyboardButton.changed = true;
	}

	private int getCurrentButtonX() {
		var x = keyboardButtons[selectedRow][selectedColumn].getPreferredSize().width / 2;
		for (var i = 0; i < selectedColumn; i++)
			x += keyboardButtons[selectedRow][i].getPreferredSize().width;

		return x;
	}

	public void moveSelectorDown() {
		EventQueue.invokeLater(() -> {
			unselectCurrentButton();

			final var x = getCurrentButtonX();
			if (selectedRow < keyboardButtons.length - 1)
				selectedRow++;
			else
				selectedRow = 0;
			selectButtonByX(x);
		});
	}

	public void moveSelectorLeft() {
		EventQueue.invokeLater(() -> {
			unselectCurrentButton();

			if (selectedColumn > 0)
				selectedColumn--;
			else
				selectedColumn = keyboardButtons[selectedRow].length - 1;
			focusCurrentButton();
		});
	}

	public void moveSelectorRight() {
		EventQueue.invokeLater(() -> {
			unselectCurrentButton();

			if (selectedColumn < keyboardButtons[selectedRow].length - 1)
				selectedColumn++;
			else
				selectedColumn = 0;
			focusCurrentButton();
		});
	}

	public void moveSelectorUp() {
		EventQueue.invokeLater(() -> {
			unselectCurrentButton();

			final var x = getCurrentButtonX();
			if (selectedRow > 0)
				selectedRow--;
			else
				selectedRow = keyboardButtons.length - 1;
			selectButtonByX(x);
		});
	}

	public void poll(final Input input) {
		synchronized (keyboardButtons) {
			if (anyChanges) {
				anyChanges = false;

				for (final var row : keyboardButtons)
					for (final var keyboardButton : row)
						anyChanges |= keyboardButton.poll(input);
			}
		}
	}

	public void pressSelected() {
		keyboardButtons[selectedRow][selectedColumn].press(false);
	}

	private void releaseAll() {
		for (final var row : keyboardButtons)
			for (final var keyboardButton : row)
				keyboardButton.release();
	}

	public void releaseSelected() {
		keyboardButtons[selectedRow][selectedColumn].release();
	}

	private void selectButtonByX(final int targetX) {
		var x = 0;
		var minDelta = Integer.MAX_VALUE;
		for (var i = 0; i < keyboardButtons[selectedRow].length; i++) {
			final var width = keyboardButtons[selectedRow][i].getPreferredSize().width;
			final var delta = Math.abs(targetX - (x + width / 2));

			if (delta > minDelta)
				break;
			selectedColumn = Math.min(i, keyboardButtons[selectedRow].length - 1);
			minDelta = delta;

			x += width;
		}

		focusCurrentButton();
	}

	@Override
	public void setVisible(final boolean b) {
		synchronized (keyboardButtons) {
			super.setVisible(b);

			if (b)
				setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));
			else
				releaseAll();
		}
	}

	public void toggleLock() {
		keyboardButtons[selectedRow][selectedColumn].toggleLock();
	}

	private void unselectCurrentButton() {
		final var currentButton = keyboardButtons[selectedRow][selectedColumn];

		if (currentButton.pressed)
			currentButton.release();

		currentButton.setFocus(false);
	}

	void updateLocation() {
		if (frameDragListener.isDragging())
			return;

		pack();

		final var maxWindowBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		final var x = (int) maxWindowBounds.getMaxX() / 2 - getWidth() / 2;
		final var y = (int) maxWindowBounds.getMaxY() - getHeight();
		final var defaultLocation = new Point(x, y);
		GuiUtils.loadFrameLocation(main.getPreferences(), this, defaultLocation, maxWindowBounds);
	}
}
