package de.bwravencl.RemoteStick;

public class KeyStroke {

	private String[] keyCodes = new String[0];
	private String[] modifierCodes = new String[0];

	public String[] getKeyCodes() {
		return keyCodes;
	}

	public void setKeyCodes(String[] keyCodes) {
		this.keyCodes = keyCodes;
	}

	public String[] getModifierCodes() {
		return modifierCodes;
	}

	public void setModifierCodes(String[] modifierCodes) {
		this.modifierCodes = modifierCodes;
	}

}
