/* Copyright (C) 2020  Matteo Hausner
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.bwravencl.controllerbuddy.gui;

import de.bwravencl.controllerbuddy.gui.GuiUtils.FrameDragListener;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.KeyStroke;
import de.bwravencl.controllerbuddy.input.LockKey;
import de.bwravencl.controllerbuddy.input.Mode;
import de.bwravencl.controllerbuddy.input.ScanCode;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.RoundRectangle2D;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.Border;

public final class OnScreenKeyboard extends JFrame {

    @SuppressWarnings("exports")
    public static final Mode onScreenKeyboardMode;

    @Serial
    private static final long serialVersionUID = -5061347351151925461L;

    private static final UUID ON_SCREEN_KEYBOARD_MODE_UUID = UUID.fromString("daf53639-9518-48db-bd63-19cde7bf9a96");
    private static final Color KEYBOARD_BUTTON_HELD_BACKGROUND = new Color(128, 128, 128);
    private static final Set<AbstractKeyboardButton> heldButtons = ConcurrentHashMap.newKeySet();
    private static final int ROW_BORDER_WIDTH = 15;
    private static final Color ROW_BACKGROUND = new Color(128, 128, 128, 64);

    static {
        onScreenKeyboardMode = new Mode(ON_SCREEN_KEYBOARD_MODE_UUID);
        onScreenKeyboardMode.setDescription(Main.strings.getString("ON_SCREEN_KEYBOARD_MODE_DESCRIPTION"));
    }

    private final Main main;
    private final CapsLockKeyButton capsLockKeyButton;
    private final ShiftKeyboardButton leftShiftKeyboardButton;
    private final ShiftKeyboardButton rightShiftKeyboardButton;
    private final AbstractKeyboardButton[][] keyboardButtons;
    private final FrameDragListener frameDragListener;
    private volatile boolean anyChanges;
    private int selectedRow;
    private int selectedColumn;

    OnScreenKeyboard(final Main main) {
        this.main = main;

        capsLockKeyButton = new CapsLockKeyButton();

        leftShiftKeyboardButton = new ShiftKeyboardButton(ShiftKeyboardButton.ShiftKeyboardButtonType.LEFT);
        rightShiftKeyboardButton = new ShiftKeyboardButton(ShiftKeyboardButton.ShiftKeyboardButtonType.RIGHT);

        keyboardButtons = new AbstractKeyboardButton[][] {
            {
                new DefaultKeyboardButton(ScanCode.DIK_ESCAPE), new DefaultKeyboardButton(ScanCode.DIK_F1),
                new DefaultKeyboardButton(ScanCode.DIK_F2), new DefaultKeyboardButton(ScanCode.DIK_F3),
                new DefaultKeyboardButton(ScanCode.DIK_F4), new DefaultKeyboardButton(ScanCode.DIK_F5),
                new DefaultKeyboardButton(ScanCode.DIK_F6), new DefaultKeyboardButton(ScanCode.DIK_F7),
                new DefaultKeyboardButton(ScanCode.DIK_F8), new DefaultKeyboardButton(ScanCode.DIK_F9),
                new DefaultKeyboardButton(ScanCode.DIK_F10), new DefaultKeyboardButton(ScanCode.DIK_F11),
                new DefaultKeyboardButton(ScanCode.DIK_F12), new DefaultKeyboardButton(ScanCode.DIK_SYSRQ),
                new LockKeyButton(LockKey.ScrollLockLockKey), new DefaultKeyboardButton(ScanCode.DIK_PAUSE),
                new DefaultKeyboardButton(ScanCode.DIK_INSERT), new DefaultKeyboardButton(ScanCode.DIK_DELETE),
                new DefaultKeyboardButton(ScanCode.DIK_HOME), new DefaultKeyboardButton(ScanCode.DIK_END)
            },
            {
                new ShiftableKeyboardButton(ScanCode.DIK_GRAVE, "~"),
                new ShiftableKeyboardButton(ScanCode.DIK_1, "!"),
                new ShiftableKeyboardButton(ScanCode.DIK_2, "@"),
                new ShiftableKeyboardButton(ScanCode.DIK_3, "#"),
                new ShiftableKeyboardButton(ScanCode.DIK_4, "$"),
                new ShiftableKeyboardButton(ScanCode.DIK_5, "%"),
                new ShiftableKeyboardButton(ScanCode.DIK_6, "^"),
                new ShiftableKeyboardButton(ScanCode.DIK_7, "&"),
                new ShiftableKeyboardButton(ScanCode.DIK_8, "*"),
                new ShiftableKeyboardButton(ScanCode.DIK_9, "("),
                new ShiftableKeyboardButton(ScanCode.DIK_0, ")"),
                new ShiftableKeyboardButton(ScanCode.DIK_MINUS, "_"),
                new ShiftableKeyboardButton(ScanCode.DIK_EQUALS, "+"),
                new DefaultKeyboardButton(ScanCode.DIK_BACK),
                new NumLockKeyButton(),
                new DefaultKeyboardButton(ScanCode.DIK_DIVIDE),
                new DefaultKeyboardButton(ScanCode.DIK_MULTIPLY),
                new DefaultKeyboardButton(ScanCode.DIK_SUBTRACT)
            },
            {
                new DefaultKeyboardButton(ScanCode.DIK_TAB),
                new AlphabeticKeyboardButton(ScanCode.DIK_Q),
                new AlphabeticKeyboardButton(ScanCode.DIK_W),
                new AlphabeticKeyboardButton(ScanCode.DIK_E),
                new AlphabeticKeyboardButton(ScanCode.DIK_R),
                new AlphabeticKeyboardButton(ScanCode.DIK_T),
                new AlphabeticKeyboardButton(ScanCode.DIK_Y),
                new AlphabeticKeyboardButton(ScanCode.DIK_U),
                new AlphabeticKeyboardButton(ScanCode.DIK_I),
                new AlphabeticKeyboardButton(ScanCode.DIK_O),
                new AlphabeticKeyboardButton(ScanCode.DIK_P),
                new ShiftableKeyboardButton(ScanCode.DIK_LBRACKET, "{"),
                new ShiftableKeyboardButton(ScanCode.DIK_RBRACKET, "}"),
                new ShiftableKeyboardButton(ScanCode.DIK_BACKSLASH, "|"),
                new NumPadKeyboardButton(ScanCode.DIK_NUMPAD7, ScanCode.DIK_HOME),
                new NumPadKeyboardButton(ScanCode.DIK_NUMPAD8, ScanCode.DIK_UP),
                new NumPadKeyboardButton(ScanCode.DIK_NUMPAD9, ScanCode.DIK_PRIOR),
                new DefaultKeyboardButton(ScanCode.DIK_ADD)
            },
            {
                capsLockKeyButton,
                new AlphabeticKeyboardButton(ScanCode.DIK_A),
                new AlphabeticKeyboardButton(ScanCode.DIK_S),
                new AlphabeticKeyboardButton(ScanCode.DIK_D),
                new AlphabeticKeyboardButton(ScanCode.DIK_F),
                new AlphabeticKeyboardButton(ScanCode.DIK_G),
                new AlphabeticKeyboardButton(ScanCode.DIK_H),
                new AlphabeticKeyboardButton(ScanCode.DIK_J),
                new AlphabeticKeyboardButton(ScanCode.DIK_K),
                new AlphabeticKeyboardButton(ScanCode.DIK_L),
                new ShiftableKeyboardButton(ScanCode.DIK_SEMICOLON, ":"),
                new ShiftableKeyboardButton(ScanCode.DIK_APOSTROPHE, "\""),
                new DefaultKeyboardButton(ScanCode.DIK_RETURN),
                new NumPadKeyboardButton(ScanCode.DIK_NUMPAD4, ScanCode.DIK_LEFT),
                new NumPadKeyboardButton(ScanCode.DIK_NUMPAD5, ""),
                new NumPadKeyboardButton(ScanCode.DIK_NUMPAD6, ScanCode.DIK_RIGHT),
                new DefaultKeyboardButton(ScanCode.DIK_PRIOR)
            },
            {
                leftShiftKeyboardButton,
                new AlphabeticKeyboardButton(ScanCode.DIK_Z),
                new AlphabeticKeyboardButton(ScanCode.DIK_X),
                new AlphabeticKeyboardButton(ScanCode.DIK_C),
                new AlphabeticKeyboardButton(ScanCode.DIK_V),
                new AlphabeticKeyboardButton(ScanCode.DIK_B),
                new AlphabeticKeyboardButton(ScanCode.DIK_N),
                new AlphabeticKeyboardButton(ScanCode.DIK_M),
                new ShiftableKeyboardButton(ScanCode.DIK_COMMA, "<"),
                new ShiftableKeyboardButton(ScanCode.DIK_PERIOD, ">"),
                new ShiftableKeyboardButton(ScanCode.DIK_SLASH, "?"),
                rightShiftKeyboardButton,
                new NumPadKeyboardButton(ScanCode.DIK_NUMPAD1, ScanCode.DIK_END),
                new NumPadKeyboardButton(ScanCode.DIK_NUMPAD2, ScanCode.DIK_DOWN),
                new NumPadKeyboardButton(ScanCode.DIK_NUMPAD3, ScanCode.DIK_NEXT),
                new DefaultKeyboardButton(ScanCode.DIK_NEXT)
            },
            {
                new DefaultKeyboardButton(ScanCode.DIK_LCONTROL),
                new DefaultKeyboardButton(ScanCode.DIK_LWIN),
                new DefaultKeyboardButton(ScanCode.DIK_LMENU),
                new DefaultKeyboardButton(ScanCode.DIK_SPACE),
                new DefaultKeyboardButton(ScanCode.DIK_RMENU),
                new DefaultKeyboardButton(ScanCode.DIK_RWIN),
                new DefaultKeyboardButton(ScanCode.DIK_RCONTROL),
                new DefaultKeyboardButton(ScanCode.DIK_UP),
                new DefaultKeyboardButton(ScanCode.DIK_DOWN),
                new DefaultKeyboardButton(ScanCode.DIK_LEFT),
                new DefaultKeyboardButton(ScanCode.DIK_RIGHT),
                new NumPadKeyboardButton(ScanCode.DIK_NUMPAD0, ScanCode.DIK_INSERT),
                new NumPadKeyboardButton(ScanCode.DIK_DECIMAL, ScanCode.DIK_DELETE),
                new DefaultKeyboardButton(ScanCode.DIK_NUMPADENTER)
            }
        };

        selectedRow = keyboardButtons.length / 2;
        selectedColumn = keyboardButtons[selectedRow].length / 2;

        frameDragListener = new FrameDragListener(main, this);

        rootPane.setWindowDecorationStyle(JRootPane.NONE);
        setUndecorated(true);
        setTitle(OnScreenKeyboard.class.getSimpleName());
        setType(JFrame.Type.POPUP);
        setFocusableWindowState(false);
        if (Main.isWindows) {
            setBackground(Main.TRANSPARENT);
        }
        setAlwaysOnTop(true);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        addMouseListener(frameDragListener);
        addMouseMotionListener(frameDragListener);

        final var parentPanel = new JPanel();
        parentPanel.setLayout(new BoxLayout(parentPanel, BoxLayout.Y_AXIS));
        parentPanel.setBackground(Main.TRANSPARENT);

        for (var row = 0; row < keyboardButtons.length; row++) {
            final var flowLayout = new FlowLayout(FlowLayout.LEFT, 0, 0);
            final var rowPanel = new JPanel(flowLayout);
            rowPanel.setBackground(ROW_BACKGROUND);
            rowPanel.setBorder(BorderFactory.createEmptyBorder(
                    row == 0 ? ROW_BORDER_WIDTH : 0,
                    ROW_BORDER_WIDTH,
                    row == keyboardButtons.length - 1 ? ROW_BORDER_WIDTH : 0,
                    ROW_BORDER_WIDTH));

            for (var column = 0; column < keyboardButtons[row].length; column++) {
                rowPanel.add(keyboardButtons[row][column]);
            }

            parentPanel.add(rowPanel);
        }

        focusCurrentButton();
        add(parentPanel);
    }

    private static String getDefaultKeyDisplayName(final String directInputKeyCodeName) {
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

    private static String getLockKeyDisplayName(final String lockKeyName) {
        var shortName = lockKeyName;

        if (!LockKey.CAPS_LOCK.equals(lockKeyName)) {
            shortName = "<html><center>" + lockKeyName.replaceFirst(" ", "<br>") + "</center></html>";
        }

        return shortName;
    }

    private void focusCurrentButton() {
        keyboardButtons[selectedRow][selectedColumn].setFocus(true);
    }

    public void forceRepoll() {
        anyChanges = true;

        for (final var row : keyboardButtons) {
            for (final var keyboardButton : row) {
                keyboardButton.changed = true;
            }
        }
    }

    private int getCurrentButtonX() {
        var x = keyboardButtons[selectedRow][selectedColumn].getPreferredSize().width / 2;
        for (var i = 0; i < selectedColumn; i++) {
            x += keyboardButtons[selectedRow][i].getPreferredSize().width;
        }

        return x;
    }

    private boolean isKeyboardShifted() {
        return leftShiftKeyboardButton.isShifting() || rightShiftKeyboardButton.isShifting();
    }

    public void moveSelectorDown() {
        EventQueue.invokeLater(() -> {
            unselectCurrentButton();

            final var x = getCurrentButtonX();
            if (selectedRow < keyboardButtons.length - 1) {
                selectedRow++;
            } else {
                selectedRow = 0;
            }
            selectButtonByX(x);
        });
    }

    public void moveSelectorLeft() {
        EventQueue.invokeLater(() -> {
            unselectCurrentButton();

            if (selectedColumn > 0) {
                selectedColumn--;
            } else {
                selectedColumn = keyboardButtons[selectedRow].length - 1;
            }
            focusCurrentButton();
        });
    }

    public void moveSelectorRight() {
        EventQueue.invokeLater(() -> {
            unselectCurrentButton();

            if (selectedColumn < keyboardButtons[selectedRow].length - 1) {
                selectedColumn++;
            } else {
                selectedColumn = 0;
            }
            focusCurrentButton();
        });
    }

    public void moveSelectorUp() {
        EventQueue.invokeLater(() -> {
            unselectCurrentButton();

            final var x = getCurrentButtonX();
            if (selectedRow > 0) {
                selectedRow--;
            } else {
                selectedRow = keyboardButtons.length - 1;
            }
            selectButtonByX(x);
        });
    }

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

    public void pressSelected() {
        keyboardButtons[selectedRow][selectedColumn].press(false);
    }

    @Serial
    private void readObject(final ObjectInputStream ignoredStream) throws NotSerializableException {
        throw new NotSerializableException(OnScreenKeyboard.class.getName());
    }

    public void releaseAll() {
        for (final var row : keyboardButtons) {
            for (final var keyboardButton : row) {
                keyboardButton.release();
            }
        }
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

            if (delta > minDelta) {
                break;
            }
            selectedColumn = Math.min(i, keyboardButtons[selectedRow].length - 1);
            minDelta = delta;

            x += width;
        }

        focusCurrentButton();
    }

    @Override
    public void setVisible(final boolean b) {
        synchronized (keyboardButtons) {
            if (b) {
                updateScaling();
                setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));
            } else {
                releaseAll();
            }
        }

        EventQueue.invokeLater(() -> {
            super.setVisible(b);
        });
    }

    public void toggleLock() {
        keyboardButtons[selectedRow][selectedColumn].toggleLock();
    }

    private void unselectCurrentButton() {
        final var currentButton = keyboardButtons[selectedRow][selectedColumn];

        if (currentButton.pressed) {
            currentButton.release();
        }

        currentButton.setFocus(false);
    }

    void updateLocation() {
        if (frameDragListener.isDragging()) {
            return;
        }

        pack();

        final var maximumWindowBounds =
                GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        final var defaultLocation = new Point();
        defaultLocation.x = (int) maximumWindowBounds.getMaxX() / 2 - getWidth() / 2;
        defaultLocation.y = (int) maximumWindowBounds.getMaxY() - getHeight();

        final var totalDisplayBounds = GuiUtils.getAndStoreTotalDisplayBounds(main);
        GuiUtils.loadFrameLocation(main.getPreferences(), this, defaultLocation, totalDisplayBounds);
    }

    private void updateScaling() {
        final var overlayScaling = main.getOverlayScaling();
        final var buttonSideLength = Math.round(AbstractKeyboardButton.BASE_BUTTON_SIZE * overlayScaling);
        final var buttonPreferredSize = new Dimension(buttonSideLength, buttonSideLength);

        final var buttonFont = new Font(
                Font.SANS_SERIF, Font.BOLD, Math.round(AbstractKeyboardButton.BUTTON_FONT_SIZE * overlayScaling));
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

    @Serial
    private void writeObject(final ObjectOutputStream ignoredStream) throws NotSerializableException {
        throw new NotSerializableException(OnScreenKeyboard.class.getName());
    }

    private abstract class AbstractKeyboardButton extends JButton {

        @Serial
        private static final long serialVersionUID = 4567858619453576258L;

        private static final int BASE_BUTTON_SIZE = 55;
        private static final int BUTTON_FONT_SIZE = 15;
        private static final int FOCUSED_BUTTON_BORDER_THICKNESS = 2;

        volatile boolean changed;
        Color defaultBackground;
        Color defaultForeground;
        Border defaultButtonBorder;
        Border focusedButtonBorder;
        private volatile boolean pressed;

        private AbstractKeyboardButton(final String text) {
            super(text);

            updateTheme();
            setMargin(new Insets(1, 1, 1, 1));
        }

        boolean isPressed() {
            return pressed;
        }

        abstract boolean poll(final Input input);

        abstract void press(final boolean lock);

        @Serial
        private void readObject(final ObjectInputStream ignoredStream) throws NotSerializableException {
            throw new NotSerializableException(AbstractKeyboardButton.class.getName());
        }

        abstract void release();

        private void setFocus(final boolean focus) {
            setBorder(focus ? focusedButtonBorder : defaultButtonBorder);
            if (!focus) {
                OnScreenKeyboard.this.repaint();
            }

            Toolkit.getDefaultToolkit().sync();
        }

        void setPressed(final boolean pressed) {
            this.pressed = pressed;
        }

        abstract void toggleLock();

        private void updateTheme() {
            defaultBackground = UIManager.getColor("Button.background");
            defaultForeground = UIManager.getColor("Button.foreground");

            defaultButtonBorder = UIManager.getBorder("Button.border");
            focusedButtonBorder = BorderFactory.createLineBorder(
                    Color.RED, Math.round(FOCUSED_BUTTON_BORDER_THICKNESS * main.getOverlayScaling()));
        }

        @Override
        public void updateUI() {
            super.updateUI();
            updateTheme();
        }

        @Serial
        private void writeObject(final ObjectOutputStream ignoredStream) throws NotSerializableException {
            throw new NotSerializableException(AbstractKeyboardButton.class.getName());
        }
    }

    private final class AlphabeticKeyboardButton extends ShiftableKeyboardButton {

        @Serial
        private static final long serialVersionUID = -43249779147068577L;

        private AlphabeticKeyboardButton(final String directInputKeyCodeName) {
            super(directInputKeyCodeName, directInputKeyCodeName);
        }
    }

    private final class CapsLockKeyButton extends LockKeyButton {

        @Serial
        private static final long serialVersionUID = 6891401614243607392L;

        private CapsLockKeyButton() {
            super(LockKey.CapsLockLockKey);
        }

        @Override
        void toggleLock() {
            super.toggleLock();

            final var showAlternativeKeyName = locked.get() ^ isKeyboardShifted();

            for (final AbstractKeyboardButton[] row : keyboardButtons) {
                for (final AbstractKeyboardButton keyboardButton : row) {
                    if (keyboardButton instanceof final ShiftableKeyboardButton shiftableKeyboardButton) {
                        shiftableKeyboardButton.setShowAlternativeKeyName(showAlternativeKeyName);
                    }
                }
            }
        }
    }

    private class DefaultKeyboardButton extends AbstractKeyboardButton {

        @Serial
        private static final long serialVersionUID = -1739002089027358633L;

        private static final long MIN_REPEAT_PRESS_TIME = 150L;
        final String directInputKeyCodeName;
        private final KeyStroke keyStroke;
        private volatile boolean mouseDown;
        private volatile boolean doDownUp;
        private volatile long beginPress;

        private DefaultKeyboardButton(final String directInputKeyCodeName) {
            super(getDefaultKeyDisplayName(directInputKeyCodeName));

            this.directInputKeyCodeName = directInputKeyCodeName;

            final ScanCode[] keyScanCodes;
            final ScanCode[] modifierScanCodes;

            final var scanCode = ScanCode.nameToScanCodeMap.get(directInputKeyCodeName);

            if (ScanCode.DIK_LMENU.equals(directInputKeyCodeName)
                    || ScanCode.DIK_RMENU.equals(directInputKeyCodeName)
                    || ScanCode.DIK_LSHIFT.equals(directInputKeyCodeName)
                    || ScanCode.DIK_RSHIFT.equals(directInputKeyCodeName)
                    || ScanCode.DIK_LCONTROL.equals(directInputKeyCodeName)
                    || ScanCode.DIK_RCONTROL.equals(directInputKeyCodeName)
                    || ScanCode.DIK_LWIN.equals(directInputKeyCodeName)
                    || ScanCode.DIK_RWIN.equals(directInputKeyCodeName)) {
                keyScanCodes = new ScanCode[0];
                modifierScanCodes = new ScanCode[] {scanCode};
            } else {
                keyScanCodes = new ScanCode[] {scanCode};
                modifierScanCodes = new ScanCode[0];
            }

            keyStroke = new KeyStroke(keyScanCodes, modifierScanCodes);

            addMouseListener(new MouseListener() {

                @Override
                public void mouseClicked(final MouseEvent e) {}

                @Override
                public void mouseEntered(final MouseEvent e) {}

                @Override
                public void mouseExited(final MouseEvent e) {}

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

        @SuppressWarnings({"NarrowingCompoundAssignment", "lossy-conversions"})
        @Override
        public Dimension getPreferredSize() {
            final var preferredSize = super.getPreferredSize();

            if (this instanceof NumPadKeyboardButton) {
                if (ScanCode.DIK_NUMPAD0.equals(directInputKeyCodeName)) {
                    preferredSize.width *= 2;
                }
            } else if (ScanCode.DIK_INSERT.equals(directInputKeyCodeName)
                    || ScanCode.DIK_DELETE.equals(directInputKeyCodeName)
                    || ScanCode.DIK_HOME.equals(directInputKeyCodeName)
                    || ScanCode.DIK_END.equals(directInputKeyCodeName)) {
                preferredSize.width *= 0.88;
            } else if (ScanCode.DIK_TAB.equals(directInputKeyCodeName)) {
                preferredSize.width *= 1.5;
            } else if (ScanCode.DIK_BACKSLASH.equals(directInputKeyCodeName)) {
                preferredSize.width *= 2;
            } else if (ScanCode.DIK_LSHIFT.equals(directInputKeyCodeName)
                    || ScanCode.DIK_RETURN.equals(directInputKeyCodeName)
                    || ScanCode.DIK_BACK.equals(directInputKeyCodeName)) {
                preferredSize.width *= 2.5;
            } else if (ScanCode.DIK_RSHIFT.equals(directInputKeyCodeName)) {
                preferredSize.width *= 3;
            } else if (ScanCode.DIK_SPACE.equals(directInputKeyCodeName)) {
                preferredSize.width *= 5.5;
            }

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
                        if (System.currentTimeMillis() - beginPress >= MIN_REPEAT_PRESS_TIME) {
                            downKeyStrokes.add(keyStroke);
                        }
                    } else {
                        downKeyStrokes.remove(keyStroke);
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
                beginPress = System.currentTimeMillis();
            }

            changed = true;
            anyChanges = true;

            setPressed(!lock);
        }

        @Override
        void release() {
            GuiUtils.invokeOnEventDispatchThreadIfRequired(() -> setBackground(defaultBackground));

            if (heldButtons.remove(this)) {
                if (System.currentTimeMillis() - beginPress < MIN_REPEAT_PRESS_TIME) {
                    doDownUp = true;
                }

                beginPress = 0L;
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
                beginPress = 0L;
            }
        }
    }

    private abstract class DualPurposeKeyboardButton extends DefaultKeyboardButton {

        @Serial
        private static final long serialVersionUID = -722664487208124876L;

        String defaultKeyName;
        String alternativeKeyName;

        private DualPurposeKeyboardButton(final String directInputKeyCodeName, final String shiftedKeyName) {
            super(directInputKeyCodeName);

            defaultKeyName = getText();
            alternativeKeyName = shiftedKeyName;
        }

        void setShowAlternativeKeyName(final boolean showAlternativeKeyName) {
            setText(showAlternativeKeyName ? alternativeKeyName : defaultKeyName);
        }
    }

    private class LockKeyButton extends AbstractKeyboardButton {

        @Serial
        private static final long serialVersionUID = 4014130700331413635L;

        final AtomicBoolean locked = new AtomicBoolean();
        private final LockKey lockKey;
        private boolean wasUp = true;

        private LockKeyButton(final LockKey lockKey) {
            super(getLockKeyDisplayName(lockKey.name()));
            this.lockKey = lockKey;

            addActionListener(arg0 -> toggleLock());
        }

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

    private final class NumLockKeyButton extends LockKeyButton {

        @Serial
        private static final long serialVersionUID = 296846375213986255L;

        public NumLockKeyButton() {
            super(LockKey.NumLockLockKey);
        }

        @Override
        void toggleLock() {
            super.toggleLock();

            for (final AbstractKeyboardButton[] row : keyboardButtons) {
                for (final AbstractKeyboardButton keyboardButton : row) {
                    if (keyboardButton instanceof final NumPadKeyboardButton numPadKeyboardButton) {
                        numPadKeyboardButton.setShowAlternativeKeyName(locked.get());
                    }
                }
            }
        }
    }

    private final class NumPadKeyboardButton extends DualPurposeKeyboardButton {

        @Serial
        private static final long serialVersionUID = -460568797568937461L;

        private NumPadKeyboardButton(
                final String directInputKeyCodeName, final String numLockOffDirectInputKeyCodeName) {
            super(directInputKeyCodeName, getDefaultKeyDisplayName(numLockOffDirectInputKeyCodeName));

            final var tempAlternativeName = alternativeKeyName;
            alternativeKeyName = defaultKeyName;
            defaultKeyName = tempAlternativeName;

            setText(defaultKeyName);
        }
    }

    private final class ShiftKeyboardButton extends DefaultKeyboardButton {

        @Serial
        private static final long serialVersionUID = -1789796245988164919L;

        private ShiftKeyboardButton(final ShiftKeyboardButtonType type) {
            super(type.directInputKeyCodeName);
        }

        private boolean isShifting() {
            return isPressed() || heldButtons.contains(this);
        }

        @Override
        void setPressed(final boolean pressed) {
            super.setPressed(pressed);

            final var showAlternativeKeyName = isKeyboardShifted() ^ capsLockKeyButton.locked.get();

            for (final AbstractKeyboardButton[] row : keyboardButtons) {
                for (final AbstractKeyboardButton keyboardButton : row) {
                    if (keyboardButton instanceof final ShiftableKeyboardButton shiftableKeyboardButton) {
                        shiftableKeyboardButton.setShowAlternativeKeyName(showAlternativeKeyName);
                    }
                }
            }
        }

        private enum ShiftKeyboardButtonType {
            LEFT(ScanCode.DIK_LSHIFT),
            RIGHT(ScanCode.DIK_RSHIFT);

            private final String directInputKeyCodeName;

            ShiftKeyboardButtonType(final String directInputKeyCodeName) {
                this.directInputKeyCodeName = directInputKeyCodeName;
            }
        }
    }

    private class ShiftableKeyboardButton extends DualPurposeKeyboardButton {

        @Serial
        private static final long serialVersionUID = -106361505843077547L;

        private ShiftableKeyboardButton(final String directInputKeyCodeName, final String shiftedKeyName) {
            super(directInputKeyCodeName, shiftedKeyName);
        }
    }
}
