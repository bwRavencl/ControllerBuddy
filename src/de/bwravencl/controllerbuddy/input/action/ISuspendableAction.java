package de.bwravencl.controllerbuddy.input.action;

import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public interface ISuspendableAction extends IAction {

	static final long SUSPEND_TIME = 750L;

	static final Set<ISuspendableAction> suspendedActions = new HashSet<ISuspendableAction>();

	default boolean isSuspended() {
		return suspendedActions.contains(this);
	}

	public default void suspend() {
		suspendedActions.add(this);

		new Timer().schedule(new TimerTask() {

			@Override
			public void run() {
				suspendedActions.remove(ISuspendableAction.this);
			}

		}, SUSPEND_TIME);
	}

}
