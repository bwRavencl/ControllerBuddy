package de.bwravencl.controllerbuddy.input.action.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(TYPE)
public @interface Action {

	public enum ActionCategory {
		ALL, AXIS, BUTTON, BUTTON_AND_CYCLES, ON_SCREEN_KEYBOARD_MODE
	}

	ActionCategory category();

	String label();

}
