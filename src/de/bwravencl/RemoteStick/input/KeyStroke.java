package de.bwravencl.RemoteStick.input;

public class KeyStroke {
	
	public static final String[] KEY_CODES = {"VK_A", "VK_B", "VK_C"};
	public static final String[] MODIFIER_CODES = {"CTRL", "ALT", "SHIFT"};

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
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		final KeyStroke keyStroke = new KeyStroke();
		
		final String[] clonedKeyCodes = new String[keyCodes.length];
		for (int i = 0; i < keyCodes.length; i++)
			clonedKeyCodes[i] = new String(keyCodes[i]);
		keyStroke.setKeyCodes(clonedKeyCodes);
		
		final String[] clonedModifierCodes = new String[modifierCodes.length];
		for (int i = 0; i < modifierCodes.length; i++)
			clonedModifierCodes[i] = new String(modifierCodes[i]);
		keyStroke.setModifierCodes(clonedModifierCodes);
		
		return keyStroke;
	}

}
