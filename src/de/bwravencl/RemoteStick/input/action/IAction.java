package de.bwravencl.RemoteStick.input.action;

import java.util.Locale;
import java.util.ResourceBundle;

import net.brockmatt.util.ResourceBundleUtil;
import de.bwravencl.RemoteStick.gui.Main;
import de.bwravencl.RemoteStick.input.Input;

public interface IAction extends Cloneable {

	final ResourceBundle rb = new ResourceBundleUtil().getResourceBundle(
			Main.STRING_RESOURCE_BUNDLE_BASENAME, Locale.getDefault());

	public void doAction(Input input, float value);

	public Object clone() throws CloneNotSupportedException;
}
