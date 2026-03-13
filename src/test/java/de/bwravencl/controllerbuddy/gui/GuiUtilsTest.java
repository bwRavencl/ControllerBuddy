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

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GuiUtilsTest {

	@Mock
	JFrame mockFrame;

	@Mock
	Main mockMain;

	@Mock
	Preferences mockPreferences;

	@SuppressWarnings("unchecked")
	private Optional<String> invokeGetFrameLocationPreferencesKey() throws ReflectiveOperationException {
		final var method = GuiUtils.class.getDeclaredMethod("getFrameLocationPreferencesKey", JFrame.class);
		method.setAccessible(true);
		return (Optional<String>) method.invoke(null, mockFrame);
	}

	private void invokeSetFrameLocationRespectingBounds(final Frame frame, final Point location, final Rectangle bounds)
			throws ReflectiveOperationException {
		final var method = GuiUtils.class.getDeclaredMethod("setFrameLocationRespectingBounds", Frame.class,
				Point.class, Rectangle.class);
		method.setAccessible(true);
		method.invoke(null, frame, location, bounds);
	}

	@Nested
	@DisplayName("createTextFieldWithMenu()")
	class CreateTextFieldWithMenuTests {

		private static void triggerPopupShow(final JPopupMenu popup) {
			try {
				popup.show(null, 0, 0);
			} catch (final Exception _) {
				// super.show() throws in headless mode; action states are set before it
			}
		}

		@ Test
		@DisplayName("cut and copy actions are disabled when no text is selected")
		void cutCopyDisabledWithoutSelection() {
			final var textField = GuiUtils.createTextFieldWithMenu("hello", 10);
			final var popup = textField.getComponentPopupMenu();
			triggerPopupShow(popup);
			// index 1 = separator; 2 = cut, 3 = copy
			Assertions.assertFalse(((JMenuItem) popup.getComponent(2)).getAction().isEnabled());
			Assertions.assertFalse(((JMenuItem) popup.getComponent(3)).getAction().isEnabled());
		}

		@Test
		@DisplayName("cut and copy actions are enabled when text is selected")
		void cutCopyEnabledWithSelection() {
			final var textField = new JTextField("hello");
			final var wrappedField = GuiUtils.createTextFieldWithMenu(textField.getDocument(), null, 10);
			wrappedField.select(0, 5);
			final var popup = wrappedField.getComponentPopupMenu();
			triggerPopupShow(popup);
			Assertions.assertTrue(((JMenuItem) popup.getComponent(2)).getAction().isEnabled());
			Assertions.assertTrue(((JMenuItem) popup.getComponent(3)).getAction().isEnabled());
		}

		@Test
		@DisplayName("undo action is disabled when no edits have been made")
		void undoDisabledBeforeAnyEdit() {
			final var textField = GuiUtils.createTextFieldWithMenu("initial", 10);
			final var popup = textField.getComponentPopupMenu();
			triggerPopupShow(popup);
			Assertions.assertFalse(((JMenuItem) popup.getComponent(0)).getAction().isEnabled());
		}

		@Test
		@DisplayName("undo action is enabled after the document has been edited")
		void undoEnabledAfterEdit() throws Exception {
			final var textField = GuiUtils.createTextFieldWithMenu("", 10);
			textField.getDocument().insertString(0, "hello", null);
			final var popup = textField.getComponentPopupMenu();
			triggerPopupShow(popup);
			Assertions.assertTrue(((JMenuItem) popup.getComponent(0)).getAction().isEnabled());
		}
	}

	@Nested
	@DisplayName("FrameDragListener")
	class FrameDragListenerTests {

		private GuiUtils.FrameDragListener listener;

		@Test
		@DisplayName("isDragging() returns true after mousePressed()")
		void isDraggingAfterMousePressed() {
			final var mockEvent = Mockito.mock(java.awt.event.MouseEvent.class);
			Mockito.when(mockEvent.getPoint()).thenReturn(new Point(10, 20));
			listener.mousePressed(mockEvent);
			Assertions.assertTrue(listener.isDragging());
		}

		@Test
		@DisplayName("isDragging() returns false before any mouse event")
		void isNotDraggingInitially() {
			Assertions.assertFalse(listener.isDragging());
		}

		@BeforeEach
		void setUp() {
			listener = new GuiUtils.FrameDragListener(mockMain, mockFrame);
		}
	}

	@Nested
	@DisplayName("getFrameLocationPreferencesKey()")
	class GetFrameLocationPreferencesKeyTests {

		@Test
		@DisplayName("converts CamelCase to underscored lowercase and strips a leading underscore")
		void convertsCamelCaseTitleStrippingLeadingUnderscore() throws ReflectiveOperationException {
			// 'M' → "_m", 'a','i','n' → "ain" → "_main"; strip leading _ → "main_location"
			Mockito.when(mockFrame.getTitle()).thenReturn("Main");
			Assertions.assertEquals(Optional.of("main_location"), invokeGetFrameLocationPreferencesKey());
		}

		@Test
		@DisplayName("converts a lowercase title to a preferences key with '_location' suffix")
		void convertsLowercaseTitleToKey() throws ReflectiveOperationException {
			Mockito.when(mockFrame.getTitle()).thenReturn("main");
			Assertions.assertEquals(Optional.of("main_location"), invokeGetFrameLocationPreferencesKey());
		}

		@Test
		@DisplayName("converts each uppercase letter to an underscore-prefixed lowercase letter")
		void convertsMultipleUppercaseLetters() throws ReflectiveOperationException {
			// 'M'→"_m", 'a','i','n'→"ain", 'F'→"_f", 'r','a','m','e'→"rame"
			// → "_main_frame"; strip leading _ → "main_frame_location"
			Mockito.when(mockFrame.getTitle()).thenReturn("MainFrame");
			Assertions.assertEquals(Optional.of("main_frame_location"), invokeGetFrameLocationPreferencesKey());
		}

		@Test
		@DisplayName("converts spaces to underscores in the preferences key")
		void convertsSpacesToUnderscores() throws ReflectiveOperationException {
			// 'o','n'→"on", ' '→"_", 's'→"s",'c'→"c",'r'→"r",'e'→"e",'e'→"e",'n'→"n"
			// → "on_screen_location"
			Mockito.when(mockFrame.getTitle()).thenReturn("on screen");
			Assertions.assertEquals(Optional.of("on_screen_location"), invokeGetFrameLocationPreferencesKey());
		}

		@Test
		@DisplayName("returns empty Optional when the title is blank")
		void returnsEmptyForBlankTitle() throws ReflectiveOperationException {
			Mockito.when(mockFrame.getTitle()).thenReturn("   ");
			Assertions.assertEquals(Optional.empty(), invokeGetFrameLocationPreferencesKey());
		}

		@Test
		@DisplayName("returns empty Optional when the title is null")
		void returnsEmptyForNullTitle() throws ReflectiveOperationException {
			Mockito.when(mockFrame.getTitle()).thenReturn(null);
			Assertions.assertEquals(Optional.empty(), invokeGetFrameLocationPreferencesKey());
		}
	}

	@Nested
	@DisplayName("invokeOnEventDispatchThreadIfRequired()")
	class InvokeOnEventDispatchThreadIfRequiredTests {

		@Test
		@DisplayName("queues the runnable for later execution when called off the EDT")
		void queuesRunnableWhenOffEdt() throws Exception {
			Assertions.assertFalse(EventQueue.isDispatchThread());
			final var ran = new AtomicBoolean(false);
			GuiUtils.invokeOnEventDispatchThreadIfRequired(() -> ran.set(true));
			// flush the EDT queue
			EventQueue.invokeAndWait(() -> {
			});
			Assertions.assertTrue(ran.get());
		}

		@Test
		@DisplayName("runs the runnable immediately when called on the EDT")
		void runsImmediatelyOnEdt() throws Exception {
			final var ran = new AtomicBoolean(false);
			EventQueue.invokeAndWait(() -> {
				GuiUtils.invokeOnEventDispatchThreadIfRequired(() -> ran.set(true));
				Assertions.assertTrue(ran.get());
			});
		}
	}

	@Nested
	@DisplayName("loadFrameLocation()")
	class LoadFrameLocationTests {

		private static final Rectangle BOUNDS = new Rectangle(0, 0, 1920, 1080);

		@Test
		@DisplayName("computes position from normalised coordinates stored in preferences")
		void computesPositionFromNormalisedCoordinates() {
			// title "Main" → key "main_location"
			Mockito.when(mockFrame.getTitle()).thenReturn("Main");
			Mockito.when(mockPreferences.get("main_location", null)).thenReturn("0.5,0.5");

			GuiUtils.loadFrameLocation(mockPreferences, mockFrame, new Point(0, 0), BOUNDS);

			final var captor = ArgumentCaptor.forClass(Point.class);
			Mockito.verify(mockFrame).setLocation(captor.capture());
			// 0.5 * 1920 = 960, 0.5 * 1080 = 540
			Assertions.assertEquals(960, captor.getValue().x);
			Assertions.assertEquals(540, captor.getValue().y);
		}

		@Test
		@DisplayName("falls back to the default location when the stored string is not numeric")
		void fallsBackToDefaultOnMalformedCoordinates() {
			Mockito.when(mockFrame.getTitle()).thenReturn("Main");
			Mockito.when(mockPreferences.get("main_location", null)).thenReturn("not,numbers");

			final var defaultLocation = new Point(100, 200);
			GuiUtils.loadFrameLocation(mockPreferences, mockFrame, defaultLocation, BOUNDS);

			final var captor = ArgumentCaptor.forClass(Point.class);
			Mockito.verify(mockFrame).setLocation(captor.capture());
			Assertions.assertEquals(100, captor.getValue().x);
			Assertions.assertEquals(200, captor.getValue().y);
		}

		@Test
		@DisplayName("falls back to the default location when the stored string has no comma separator")
		void fallsBackToDefaultOnMissingSeparator() {
			Mockito.when(mockFrame.getTitle()).thenReturn("Main");
			Mockito.when(mockPreferences.get("main_location", null)).thenReturn("singlevalue");

			final var defaultLocation = new Point(100, 200);
			GuiUtils.loadFrameLocation(mockPreferences, mockFrame, defaultLocation, BOUNDS);

			final var captor = ArgumentCaptor.forClass(Point.class);
			Mockito.verify(mockFrame).setLocation(captor.capture());
			Assertions.assertEquals(100, captor.getValue().x);
			Assertions.assertEquals(200, captor.getValue().y);
		}

		@Test
		@DisplayName("falls back to the default location when preferences has no entry for the key")
		void fallsBackToDefaultWhenNoPreferencesEntry() {
			Mockito.when(mockFrame.getTitle()).thenReturn("Main");
			Mockito.when(mockPreferences.get("main_location", null)).thenReturn(null);

			final var defaultLocation = new Point(100, 200);
			GuiUtils.loadFrameLocation(mockPreferences, mockFrame, defaultLocation, BOUNDS);

			final var captor = ArgumentCaptor.forClass(Point.class);
			Mockito.verify(mockFrame).setLocation(captor.capture());
			Assertions.assertEquals(100, captor.getValue().x);
			Assertions.assertEquals(200, captor.getValue().y);
		}

		@BeforeEach
		void setUp() {
			// Frame dimensions return 0 so clamping doesn't offset results
			Mockito.when(mockFrame.getWidth()).thenReturn(0);
			Mockito.when(mockFrame.getHeight()).thenReturn(0);
		}

		@Test
		@DisplayName("applies the default location when the frame has no title (no preferences lookup)")
		void usesDefaultLocationWhenNoTitle() {
			Mockito.when(mockFrame.getTitle()).thenReturn(null);
			final var defaultLocation = new Point(100, 200);

			GuiUtils.loadFrameLocation(mockPreferences, mockFrame, defaultLocation, BOUNDS);

			final var captor = ArgumentCaptor.forClass(Point.class);
			Mockito.verify(mockFrame).setLocation(captor.capture());
			Assertions.assertEquals(100, captor.getValue().x);
			Assertions.assertEquals(200, captor.getValue().y);
		}
	}

	@Nested
	@DisplayName("setBoundsWithMinimum()")
	class SetBoundsWithMinimumTests {

		@Test
		@DisplayName("sets the component bounds and half-size minimum dimensions")
		void setsBoundsAndMinimumSize() {
			final var label = new JLabel();
			final var bounds = new Rectangle(10, 20, 400, 200);
			GuiUtils.setBoundsWithMinimum(label, bounds);
			Assertions.assertEquals(bounds, label.getBounds());
			Assertions.assertEquals(new Dimension(200, 100), label.getMinimumSize());
		}
	}

	@Nested
	@DisplayName("setEnabledRecursive()")
	class SetEnabledRecursiveTests {

		@Test
		@DisplayName("disables a component that has no children")
		void disablesLeafComponent() {
			Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
					"Skipping: AWT components cannot be created in headless mode");

			final var label = new JLabel("test");
			label.setEnabled(true);
			GuiUtils.setEnabledRecursive(label, false);
			Assertions.assertFalse(label.isEnabled());
		}

		@Test
		@DisplayName("does not throw when called with a null component")
		void doesNotThrowForNullComponent() {
			Assertions.assertDoesNotThrow(() -> GuiUtils.setEnabledRecursive(null, false));
		}

		@Test
		@DisplayName("recursively disables a container and all its children")
		void recursivelyDisablesContainerAndChildren() {
			Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
					"Skipping: AWT components cannot be created in headless mode");

			final var child1 = new JLabel("a");
			final var child2 = new JLabel("b");
			final var panel = new JPanel();
			panel.add(child1);
			panel.add(child2);

			GuiUtils.setEnabledRecursive(panel, false);

			Assertions.assertFalse(panel.isEnabled());
			Assertions.assertFalse(child1.isEnabled());
			Assertions.assertFalse(child2.isEnabled());
		}

		@Test
		@DisplayName("recursively enables a container and all its children")
		void recursivelyEnablesContainerAndChildren() {
			Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
					"Skipping: AWT components cannot be created in headless mode");

			final var child = new JLabel("x");
			child.setEnabled(false);
			final var panel = new JPanel();
			panel.setEnabled(false);
			panel.add(child);

			GuiUtils.setEnabledRecursive(panel, true);

			Assertions.assertTrue(panel.isEnabled());
			Assertions.assertTrue(child.isEnabled());
		}

		@Test
		@DisplayName("recursively processes deeply nested containers")
		void recursivelyProcessesDeeplyNestedContainers() {
			Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
					"Skipping: AWT components cannot be created in headless mode");

			final var deepChild = new JLabel("deep");
			final var innerPanel = new JPanel();
			innerPanel.add(deepChild);
			final var outerPanel = new JPanel();
			outerPanel.add(innerPanel);

			GuiUtils.setEnabledRecursive(outerPanel, false);

			Assertions.assertFalse(deepChild.isEnabled());
		}
	}

	@Nested
	@DisplayName("setFrameLocationRespectingBounds()")
	class SetFrameLocationRespectingBoundsTests {

		private static final Rectangle BOUNDS = new Rectangle(0, 0, 1920, 1080);

		@Test
		@DisplayName("clamps x to the left edge when the location is too far left")
		void clampsXToLeftEdge() throws ReflectiveOperationException {
			final var location = new Point(-50, 400);
			invokeSetFrameLocationRespectingBounds(mockFrame, location, BOUNDS);
			// max(0, min(1720, -50)) = 0
			Assertions.assertEquals(0, location.x);
		}

		@Test
		@DisplayName("clamps x to the right edge when the location is too far right")
		void clampsXToRightEdge() throws ReflectiveOperationException {
			final var location = new Point(2000, 400);
			invokeSetFrameLocationRespectingBounds(mockFrame, location, BOUNDS);
			// max(0, min(1720, 2000)) = 1720
			Assertions.assertEquals(1720, location.x);
		}

		@Test
		@DisplayName("clamps y to the bottom edge when the location is too far down")
		void clampsYToBottomEdge() throws ReflectiveOperationException {
			final var location = new Point(500, 2000);
			invokeSetFrameLocationRespectingBounds(mockFrame, location, BOUNDS);
			// max(0, min(980, 2000)) = 980
			Assertions.assertEquals(980, location.y);
		}

		@Test
		@DisplayName("clamps y to the top edge when the location is above the bounds")
		void clampsYToTopEdge() throws ReflectiveOperationException {
			final var location = new Point(500, -50);
			invokeSetFrameLocationRespectingBounds(mockFrame, location, BOUNDS);
			// max(0, min(980, -50)) = 0
			Assertions.assertEquals(0, location.y);
		}

		@Test
		@DisplayName("leaves a location that is already within bounds unchanged")
		void leavesInBoundsLocationUnchanged() throws ReflectiveOperationException {
			final var location = new Point(500, 400);
			invokeSetFrameLocationRespectingBounds(mockFrame, location, BOUNDS);
			// max(0, min(1920-200, 500)) = 500; max(0, min(1080-100, 400)) = 400
			Assertions.assertEquals(500, location.x);
			Assertions.assertEquals(400, location.y);
		}

		@Test
		@DisplayName("respects non-zero bounds origin when clamping")
		void respectsNonZeroBoundsOrigin() throws ReflectiveOperationException {
			// Bounds starting at (-100, -200), width/height 1920/1080
			final var bounds = new Rectangle(-100, -200, 1920, 1080);
			final var location = new Point(-200, -300);
			invokeSetFrameLocationRespectingBounds(mockFrame, location, bounds);
			// x: max(-100, min(1820-200, -200)) = max(-100, -200) = -100
			// y: max(-200, min(1080-100, -300)) = max(-200, -300) = -200
			Assertions.assertEquals(-100, location.x);
			Assertions.assertEquals(-200, location.y);
		}

		@BeforeEach
		void setUp() {
			Mockito.when(mockFrame.getWidth()).thenReturn(200);
			Mockito.when(mockFrame.getHeight()).thenReturn(100);
		}
	}

	@Nested
	@DisplayName("wrapComponentInScrollPane()")
	class WrapComponentInScrollPaneTests {

		@Test
		@DisplayName("preferred size is not explicitly set when null is passed")
		void noPreferredSizeWhenNull() {
			final var scrollPane = GuiUtils.wrapComponentInScrollPane(new JLabel(), null);
			Assertions.assertFalse(scrollPane.isPreferredSizeSet());
		}

		@Test
		@DisplayName("preferred size matches the provided Dimension")
		void preferredSizeAppliedWhenProvided() {
			final var dim = new Dimension(300, 200);
			final var scrollPane = GuiUtils.wrapComponentInScrollPane(new JLabel(), dim);
			Assertions.assertEquals(dim, scrollPane.getPreferredSize());
		}

		@Test
		@DisplayName("the wrapped component is the viewport's view")
		void wrappedComponentIsViewportView() {
			final var label = new JLabel("content");
			final var scrollPane = GuiUtils.wrapComponentInScrollPane(label);
			Assertions.assertSame(label, scrollPane.getViewport().getView());
		}
	}
}
