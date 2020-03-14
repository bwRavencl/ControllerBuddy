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

import static de.bwravencl.controllerbuddy.gui.GuiUtils.invokeOnEventDispatchThreadIfRequired;
import static de.bwravencl.controllerbuddy.gui.GuiUtils.loadFrameLocation;
import static de.bwravencl.controllerbuddy.gui.Main.TRANSPARENT;
import static de.bwravencl.controllerbuddy.gui.Main.strings;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.Set;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import de.bwravencl.controllerbuddy.gui.GuiUtils.FrameDragListener;
import de.bwravencl.controllerbuddy.input.DirectInputKeyCode;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.KeyStroke;
import de.bwravencl.controllerbuddy.input.LockKey;
import de.bwravencl.controllerbuddy.input.Mode;

@SuppressWarnings("serial")
public final class OnScreenKeyboard extends JFrame {

	private abstract class AbstractKeyboardButton extends JButton {

		private static final long serialVersionUID = 4567858619453576258L;

		private static final int BASE_BUTTON_SIZE = 55;

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

		abstract void poll(final Input input);

		abstract void press();

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

		private volatile boolean changed;

		private volatile boolean doDownUp;

		private volatile long beginPress;

		private final String directInputKeyCodeName;

		private final KeyStroke keyStroke;

		private TimerTask lockTimerTask;

		private DefaultKeyboardButton(final String directInputKeyCodeName) {
			super(getShortDefaultKeyName(directInputKeyCodeName));

			this.directInputKeyCodeName = directInputKeyCodeName;

			final Integer[] keyCodes;
			final Integer[] modifierCodes;

			if (DirectInputKeyCode.DIK_LMENU.equals(directInputKeyCodeName)
					|| DirectInputKeyCode.DIK_RMENU.equals(directInputKeyCodeName)
					|| DirectInputKeyCode.DIK_LSHIFT.equals(directInputKeyCodeName)
					|| DirectInputKeyCode.DIK_RSHIFT.equals(directInputKeyCodeName)
					|| DirectInputKeyCode.DIK_LCONTROL.equals(directInputKeyCodeName)
					|| DirectInputKeyCode.DIK_RCONTROL.equals(directInputKeyCodeName)
					|| DirectInputKeyCode.DIK_LWIN.equals(directInputKeyCodeName)
					|| DirectInputKeyCode.DIK_RWIN.equals(directInputKeyCodeName)) {
				keyCodes = new Integer[0];
				modifierCodes = new Integer[] { DirectInputKeyCode.nameToKeyCodeMap.get(directInputKeyCodeName) };
			} else {
				keyCodes = new Integer[] { DirectInputKeyCode.nameToKeyCodeMap.get(directInputKeyCodeName) };
				modifierCodes = new Integer[0];
			}

			keyStroke = new KeyStroke(keyCodes, modifierCodes);

			addChangeListener(new ChangeListener() {

				private boolean lastPressed;

				@Override
				public void stateChanged(final ChangeEvent e) {
					final var pressed = getModel().isPressed();
					if (pressed != lastPressed) {
						if (pressed) {
							beginPress = System.currentTimeMillis();
							press();

							if (lockTimerTask != null)
								lockTimerTask.cancel();
							lockTimerTask = new TimerTask() {

								@Override
								public void run() {
									if (heldButtons.contains(DefaultKeyboardButton.this)) {
										SwingUtilities.invokeLater(() -> {
											DefaultKeyboardButton.this.setForeground(Color.GRAY);
										});
										press();
									}
								}
							};
							main.getTimer().schedule(lockTimerTask, MIN_REPEAT_PRESS_TIME);
						} else {
							if (System.currentTimeMillis() - beginPress < MIN_REPEAT_PRESS_TIME)
								release();
							else
								DefaultKeyboardButton.this.setForeground(defaultForeground);

							lockTimerTask.cancel();
						}

						lastPressed = pressed;
					}
				}
			});
		}

		@Override
		public Dimension getPreferredSize() {
			final var preferredSize = super.getPreferredSize();

			if (DirectInputKeyCode.DIK_INSERT.equals(directInputKeyCodeName)
					|| DirectInputKeyCode.DIK_DELETE.equals(directInputKeyCodeName)
					|| DirectInputKeyCode.DIK_HOME.equals(directInputKeyCodeName)
					|| DirectInputKeyCode.DIK_END.equals(directInputKeyCodeName))
				preferredSize.width *= 0.88;
			else if (DirectInputKeyCode.DIK_TAB.equals(directInputKeyCodeName))
				preferredSize.width *= 1.5;
			else if (DirectInputKeyCode.DIK_BACKSLASH.equals(directInputKeyCodeName)
					|| DirectInputKeyCode.DIK_NUMPAD0.equals(directInputKeyCodeName))
				preferredSize.width *= 2;
			else if (DirectInputKeyCode.DIK_LSHIFT.equals(directInputKeyCodeName)
					|| DirectInputKeyCode.DIK_RETURN.equals(directInputKeyCodeName)
					|| DirectInputKeyCode.DIK_BACK.equals(directInputKeyCodeName))
				preferredSize.width *= 2.5;
			else if (DirectInputKeyCode.DIK_RSHIFT.equals(directInputKeyCodeName))
				preferredSize.width *= 3;
			else if (DirectInputKeyCode.DIK_SPACE.equals(directInputKeyCodeName))
				preferredSize.width *= 4.5;

			return preferredSize;
		}

		@Override
		void poll(final Input input) {
			if (!changed)
				return;

			if (doDownUp) {
				input.getDownUpKeyStrokes().add(keyStroke);
				doDownUp = false;
			}

			final var downKeyStrokes = input.getDownKeyStrokes();

			if (heldButtons.contains(this)) {
				if (System.currentTimeMillis() - beginPress >= MIN_REPEAT_PRESS_TIME)
					downKeyStrokes.add(keyStroke);
			} else
				downKeyStrokes.remove(keyStroke);

			changed = false;
		}

		@Override
		void press() {
			invokeOnEventDispatchThreadIfRequired(() -> setBackground(KEYBOARD_BUTTON_HELD_BACKGROUND));

			if (heldButtons.add(this))
				beginPress = System.currentTimeMillis();

			changed = true;
			anyChanges = true;
		}

		@Override
		void release() {
			invokeOnEventDispatchThreadIfRequired(() -> setBackground(defaultBackground));

			if (heldButtons.remove(this)) {
				if (System.currentTimeMillis() - beginPress < MIN_REPEAT_PRESS_TIME)
					doDownUp = true;

				beginPress = 0L;
				changed = true;
				anyChanges = true;
			}
		}

		@Override
		void toggleLock() {
			if (heldButtons.contains(this))
				release();
			else {
				press();
				beginPress = 0L;
			}
		}
	}

	private final class LockKeyButton extends AbstractKeyboardButton {

		private static final long serialVersionUID = 4014130700331413635L;

		private volatile boolean changed;

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
		void poll(final Input input) {
			if (!changed)
				return;

			if (locked)
				input.getOnLockKeys().add(virtualKeyCode);
			else
				input.getOffLockKeys().add(virtualKeyCode);

			changed = false;
		}

		@Override
		void press() {
			if (wasUp) {
				toggleLock();
				wasUp = false;
			}
		}

		@Override
		void release() {
			wasUp = true;
		}

		@Override
		void toggleLock() {
			locked = !locked;
			invokeOnEventDispatchThreadIfRequired(() -> {
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
		onScreenKeyboardMode.setDescription(strings.getString("ON_SCREEN_KEYBOARD_MODE_DESCRIPTION"));
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

		if (!lockKeyName.equals(LockKey.CAPS_LOCK))
			shortName = shortName.replaceFirst(LockKey.LOCK_SUFFIX, "Lk");

		return shortName;
	}

	private final AbstractKeyboardButton[][] keyboardButtons = { {
			new DefaultKeyboardButton(DirectInputKeyCode.DIK_ESCAPE),
			new DefaultKeyboardButton(DirectInputKeyCode.DIK_F1), new DefaultKeyboardButton(DirectInputKeyCode.DIK_F2),
			new DefaultKeyboardButton(DirectInputKeyCode.DIK_F3), new DefaultKeyboardButton(DirectInputKeyCode.DIK_F4),
			new DefaultKeyboardButton(DirectInputKeyCode.DIK_F5), new DefaultKeyboardButton(DirectInputKeyCode.DIK_F6),
			new DefaultKeyboardButton(DirectInputKeyCode.DIK_F7), new DefaultKeyboardButton(DirectInputKeyCode.DIK_F8),
			new DefaultKeyboardButton(DirectInputKeyCode.DIK_F9), new DefaultKeyboardButton(DirectInputKeyCode.DIK_F10),
			new DefaultKeyboardButton(DirectInputKeyCode.DIK_F11),
			new DefaultKeyboardButton(DirectInputKeyCode.DIK_F12),
			new DefaultKeyboardButton(DirectInputKeyCode.DIK_SYSRQ), new LockKeyButton(KeyEvent.VK_SCROLL_LOCK),
			new DefaultKeyboardButton(DirectInputKeyCode.DIK_PAUSE),
			new DefaultKeyboardButton(DirectInputKeyCode.DIK_INSERT),
			new DefaultKeyboardButton(DirectInputKeyCode.DIK_DELETE),
			new DefaultKeyboardButton(DirectInputKeyCode.DIK_HOME),
			new DefaultKeyboardButton(DirectInputKeyCode.DIK_END) },
			{ new DefaultKeyboardButton(DirectInputKeyCode.DIK_GRAVE),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_1),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_2),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_3),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_4),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_5),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_6),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_7),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_8),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_9),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_0),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_MINUS),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_EQUALS),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_BACK), new LockKeyButton(KeyEvent.VK_NUM_LOCK),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_DIVIDE),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_MULTIPLY),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_SUBTRACT) },
			{ new DefaultKeyboardButton(DirectInputKeyCode.DIK_TAB),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_Q),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_W),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_E),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_R),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_T),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_Y),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_U),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_I),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_O),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_P),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_LBRACKET),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_RBRACKET),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_BACKSLASH),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_NUMPAD7),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_NUMPAD8),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_NUMPAD9),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_ADD) },
			{ new LockKeyButton(KeyEvent.VK_CAPS_LOCK), new DefaultKeyboardButton(DirectInputKeyCode.DIK_A),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_S),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_D),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_F),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_G),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_H),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_J),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_K),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_L),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_SEMICOLON),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_APOSTROPHE),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_RETURN),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_NUMPAD4),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_NUMPAD5),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_NUMPAD6),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_PRIOR) },
			{ new DefaultKeyboardButton(DirectInputKeyCode.DIK_LSHIFT),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_Z),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_X),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_C),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_V),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_B),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_N),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_M),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_COMMA),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_PERIOD),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_SLASH),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_RSHIFT),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_NUMPAD1),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_NUMPAD2),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_NUMPAD3),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_NEXT) },
			{ new DefaultKeyboardButton(DirectInputKeyCode.DIK_LCONTROL),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_LWIN),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_LMENU),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_SPACE),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_RMENU),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_RWIN),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_APPS),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_RCONTROL),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_UP),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_DOWN),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_LEFT),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_RIGHT),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_NUMPAD0),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_NUMPADCOMMA),
					new DefaultKeyboardButton(DirectInputKeyCode.DIK_NUMPADENTER) } };

	private final Main main;
	private final FrameDragListener frameDragListener;
	private volatile boolean anyChanges;
	private int selectedRow = keyboardButtons.length / 2;
	private int selectedColumn = keyboardButtons[selectedRow].length / 2;

	OnScreenKeyboard(final Main main) {
		this.main = main;
		frameDragListener = new FrameDragListener(main, this);

		setTitle(OnScreenKeyboard.class.getSimpleName());
		setType(JFrame.Type.UTILITY);
		setFocusableWindowState(false);
		setUndecorated(true);
		setBackground(TRANSPARENT);
		setAlwaysOnTop(true);

		addMouseListener(frameDragListener);
		addMouseMotionListener(frameDragListener);

		final var parentPanel = new JPanel();
		parentPanel.setLayout(new BoxLayout(parentPanel, BoxLayout.Y_AXIS));
		parentPanel.setBackground(TRANSPARENT);

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

	private int getCurrentButtonX() {
		var x = keyboardButtons[selectedRow][selectedColumn].getPreferredSize().width / 2;
		for (var i = 0; i < selectedColumn; i++)
			x += keyboardButtons[selectedRow][i].getPreferredSize().width;

		return x;
	}

	public void moveSelectorDown() {
		SwingUtilities.invokeLater(() -> {
			unfocusCurrentButton();

			final var x = getCurrentButtonX();
			if (selectedRow < keyboardButtons.length - 1)
				selectedRow++;
			else
				selectedRow = 0;
			selectButtonByX(x);
		});
	}

	public void moveSelectorLeft() {
		SwingUtilities.invokeLater(() -> {
			unfocusCurrentButton();
			if (selectedColumn > 0)
				selectedColumn--;
			else
				selectedColumn = keyboardButtons[selectedRow].length - 1;
			focusCurrentButton();
		});
	}

	public void moveSelectorRight() {
		SwingUtilities.invokeLater(() -> {
			unfocusCurrentButton();
			if (selectedColumn < keyboardButtons[selectedRow].length - 1)
				selectedColumn++;
			else
				selectedColumn = 0;
			focusCurrentButton();
		});
	}

	public void moveSelectorUp() {
		SwingUtilities.invokeLater(() -> {
			unfocusCurrentButton();

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
						keyboardButton.poll(input);
			}
		}
	}

	public void pressSelected() {
		keyboardButtons[selectedRow][selectedColumn].press();
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
			else {
				selectedColumn = Math.min(i, keyboardButtons[selectedRow].length - 1);
				minDelta = delta;
			}

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

	private void unfocusCurrentButton() {
		keyboardButtons[selectedRow][selectedColumn].setFocus(false);
	}

	void updateLocation() {
		if (frameDragListener.isDragging())
			return;

		pack();

		final var maxWindowBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		final var x = (int) maxWindowBounds.getMaxX() / 2 - getWidth() / 2;
		final var y = (int) maxWindowBounds.getMaxY() - getHeight();
		final var defaultLocation = new Point(x, y);
		loadFrameLocation(main.getPreferences(), this, defaultLocation, maxWindowBounds);
	}
}
