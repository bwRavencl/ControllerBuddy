/* Copyright (C) 2018  Matteo Hausner
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
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.Locale;
import java.util.ResourceBundle;
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
import javax.swing.border.CompoundBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import de.bwravencl.controllerbuddy.input.DirectInputKeyCode;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.KeyStroke;
import de.bwravencl.controllerbuddy.input.LockKey;
import de.bwravencl.controllerbuddy.input.Mode;
import net.brockmatt.util.ResourceBundleUtil;

public class OnScreenKeyboard extends JFrame {

	private abstract class AbstractKeyboardButton extends JButton {

		private static final long serialVersionUID = 4567858619453576258L;

		private static final int BASE_BUTTON_SIZE = 55;

		public AbstractKeyboardButton(final String text) {
			super(text);

			setMargin(new Insets(1, 1, 1, 1));
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(BASE_BUTTON_SIZE, BASE_BUTTON_SIZE);
		}

		protected abstract void poll(final Input input);

		protected abstract void press();

		protected abstract void release();

		protected abstract void toggleLock();

	}

	private class DefaultKeyboardButton extends AbstractKeyboardButton {

		private static final long serialVersionUID = -1739002089027358633L;

		private static final long MIN_REPEAT_PRESS_TIME = 150L;

		private volatile boolean changed;

		private volatile boolean doDownUp;

		private volatile long beginPress;

		private final String directInputKeyCodeName;

		private final KeyStroke keyStroke;

		private TimerTask lockTimerTask;

		public DefaultKeyboardButton(final String directInputKeyCodeName) {
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
					final boolean pressed = getModel().isPressed();
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
							Main.getTimer().schedule(lockTimerTask, MIN_REPEAT_PRESS_TIME);
						} else {
							if (System.currentTimeMillis() - beginPress < MIN_REPEAT_PRESS_TIME)
								release();
							else
								DefaultKeyboardButton.this.setForeground(KEYBOARD_BUTTON_DEFAULT_FOREGROUND);

							lockTimerTask.cancel();
						}

						lastPressed = pressed;
					}
				}
			});
		}

		@Override
		public Dimension getPreferredSize() {
			final Dimension preferredSize = super.getPreferredSize();

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
		protected void poll(final Input input) {
			if (!changed)
				return;

			if (doDownUp) {
				input.getDownUpKeyStrokes().add(keyStroke);
				doDownUp = false;
			}

			final Set<KeyStroke> downKeyStrokes = input.getDownKeyStrokes();

			if (heldButtons.contains(this)) {
				if (System.currentTimeMillis() - beginPress >= MIN_REPEAT_PRESS_TIME)
					downKeyStrokes.add(keyStroke);
			} else
				downKeyStrokes.remove(keyStroke);

			changed = false;
		}

		@Override
		protected void press() {
			Main.invokeOnEventDispatchThreadIfRequired(() -> setBackground(KEYBOARD_BUTTON_HELD_BACKGROUND));

			if (heldButtons.add(this))
				beginPress = System.currentTimeMillis();

			changed = true;
			anyChanges = true;
		}

		@Override
		protected void release() {
			Main.invokeOnEventDispatchThreadIfRequired(() -> setBackground(KEYBOARD_BUTTON_DEFAULT_BACKGROUND));

			if (heldButtons.remove(this)) {
				if (System.currentTimeMillis() - beginPress < MIN_REPEAT_PRESS_TIME)
					doDownUp = true;

				beginPress = 0L;
				changed = true;
				anyChanges = true;
			}
		}

		@Override
		protected void toggleLock() {
			if (heldButtons.contains(this))
				release();
			else {
				press();
				beginPress = 0L;
			}
		}

	}

	private class LockKeyButton extends AbstractKeyboardButton {

		private static final long serialVersionUID = 4014130700331413635L;

		private volatile boolean changed;

		private volatile boolean locked;

		private final int virtualKeyCode;

		private boolean wasUp = true;

		public LockKeyButton(final int virtualKeyCode) {
			super(getShortLockKeyName(LockKey.virtualKeyCodeToLockKeyMap.get(virtualKeyCode).name));
			this.virtualKeyCode = virtualKeyCode;

			addActionListener(arg0 -> {
				toggleLock();
			});
		}

		@Override
		public Dimension getPreferredSize() {
			final Dimension preferredSize = super.getPreferredSize();

			if (virtualKeyCode == KeyEvent.VK_CAPS_LOCK)
				preferredSize.width *= 2;

			return preferredSize;
		}

		@Override
		protected void poll(final Input input) {
			if (!changed)
				return;

			if (locked)
				input.getOnLockKeys().add(virtualKeyCode);
			else
				input.getOffLockKeys().add(virtualKeyCode);

			changed = false;
		}

		@Override
		protected void press() {
			if (wasUp) {
				toggleLock();
				wasUp = false;
			}
		}

		@Override
		protected void release() {
			wasUp = true;
		}

		@Override
		protected void toggleLock() {
			locked = !locked;
			Main.invokeOnEventDispatchThreadIfRequired(() -> {
				setForeground(locked ? Color.GREEN : KEYBOARD_BUTTON_DEFAULT_FOREGROUND);
			});
			changed = true;
			anyChanges = true;
		}

	}

	private static final long serialVersionUID = -111088315813179371L;

	private static final Color KEYBOARD_BUTTON_DEFAULT_BACKGROUND = new JButton().getBackground();

	private static final Color KEYBOARD_BUTTON_DEFAULT_FOREGROUND = new JButton().getForeground();

	private static final Color KEYBOARD_BUTTON_HELD_BACKGROUND = new Color(128, 128, 128);

	private static final Set<AbstractKeyboardButton> heldButtons = ConcurrentHashMap.newKeySet();

	protected static final UUID ON_SCREEN_KEYBOARD_MODE_UUID = UUID.fromString("daf53639-9518-48db-bd63-19cde7bf9a96");

	public static final Mode onScreenKeyboardMode;

	private static final int ROW_BORDER_WIDTH = 10;

	private static final Color ROW_BACKGROUND = new Color(128, 128, 128, 64);

	private static final Border defaultButtonBorder = UIManager.getBorder("Button.border");

	private static final Border focusedButtonBorder = BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(Color.RED, 3), ((CompoundBorder) defaultButtonBorder).getInsideBorder());

	static {
		onScreenKeyboardMode = new Mode(ON_SCREEN_KEYBOARD_MODE_UUID);
		final ResourceBundle rb = new ResourceBundleUtil().getResourceBundle(Main.STRING_RESOURCE_BUNDLE_BASENAME,
				Locale.getDefault());
		onScreenKeyboardMode.setDescription(rb.getString("ON_SCREEN_KEYBOARD_MODE_DESCRIPTION"));
	}

	private static String getShortDefaultKeyName(final String directInputKeyCodeName) {
		String shortName = directInputKeyCodeName;

		shortName = shortName.replaceFirst("L ", "");
		shortName = shortName.replaceFirst("R ", "");
		shortName = shortName.replaceFirst("Num", "");
		shortName = shortName.replaceFirst("App ", "");
		shortName = shortName.replaceAll(" Arrow", "");

		return shortName;
	}

	private static String getShortLockKeyName(final String lockKeyName) {
		String shortName = lockKeyName;

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

	private volatile boolean anyChanges;

	private int selectedRow = keyboardButtons.length / 2;
	private int selectedColumn = keyboardButtons[selectedRow].length / 2;

	public OnScreenKeyboard() {
		setType(JFrame.Type.UTILITY);
		setFocusableWindowState(false);
		setUndecorated(true);
		setBackground(Main.TRANSPARENT);
		setAlwaysOnTop(true);

		final JPanel parentPanel = new JPanel();
		parentPanel.setLayout(new BoxLayout(parentPanel, BoxLayout.Y_AXIS));
		parentPanel.setBackground(Main.TRANSPARENT);

		for (int row = 0; row < keyboardButtons.length; row++) {
			final FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT);
			flowLayout.setHgap(0);
			flowLayout.setVgap(0);
			final JPanel rowPanel = new JPanel(flowLayout);
			rowPanel.setBackground(ROW_BACKGROUND);
			rowPanel.setBorder(BorderFactory.createEmptyBorder(row == 0 ? ROW_BORDER_WIDTH : 0, ROW_BORDER_WIDTH,
					row == keyboardButtons.length - 1 ? ROW_BORDER_WIDTH : 0, ROW_BORDER_WIDTH));

			for (int column = 0; column < keyboardButtons[row].length; column++)
				rowPanel.add(keyboardButtons[row][column]);

			parentPanel.add(rowPanel);
		}

		focusCurrentButton();
		add(parentPanel);
		updateLocation();
	}

	private void focusCurrentButton() {
		keyboardButtons[selectedRow][selectedColumn].setBorder(focusedButtonBorder);
	}

	private int getCurrentButtonX() {
		int x = keyboardButtons[selectedRow][selectedColumn].getPreferredSize().width / 2;
		for (int i = 0; i < selectedColumn; i++)
			x += keyboardButtons[selectedRow][i].getPreferredSize().width;

		return x;
	}

	public void moveSelectorDown() {
		SwingUtilities.invokeLater(() -> {
			unfocusCurrentButton();

			final int x = getCurrentButtonX();
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

			final int x = getCurrentButtonX();
			if (selectedRow > 0)
				selectedRow--;
			else
				selectedRow = keyboardButtons.length - 1;
			selectButtonByX(x);
		});
	}

	public void poll(final Input input) {
		if (anyChanges) {
			anyChanges = false;

			for (final AbstractKeyboardButton[] row : keyboardButtons)
				for (final AbstractKeyboardButton kb : row)
					kb.poll(input);
		}
	}

	public void pressSelected() {
		keyboardButtons[selectedRow][selectedColumn].press();
	}

	private void releaseAll() {
		for (final AbstractKeyboardButton[] row : keyboardButtons)
			for (final AbstractKeyboardButton kb : row)
				kb.release();
	}

	public void releaseSelected() {
		keyboardButtons[selectedRow][selectedColumn].release();
	}

	private void selectButtonByX(final int targetX) {
		int x = 0;
		int minDelta = Integer.MAX_VALUE;
		for (int i = 0; i < keyboardButtons[selectedRow].length; i++) {
			final int width = keyboardButtons[selectedRow][i].getPreferredSize().width;
			final int delta = Math.abs(targetX - (x + width / 2));

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
		super.setVisible(b);

		if (b)
			setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));
		else
			releaseAll();
	}

	public void toggleLock() {
		keyboardButtons[selectedRow][selectedColumn].toggleLock();
	}

	private void unfocusCurrentButton() {
		keyboardButtons[selectedRow][selectedColumn].setBorder(defaultButtonBorder);
	}

	protected void updateLocation() {
		pack();

		final Rectangle rectangle = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		final int x = (int) rectangle.getMaxX() / 2 - getWidth() / 2;
		final int y = (int) rectangle.getMaxY() - getHeight();
		setLocation(x, y);
	}

}