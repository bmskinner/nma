package com.bmskinner.nma.gui;

import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

public class CtrlPressedListener {
	private static final Logger LOGGER = Logger.getLogger(CtrlPressedListener.class.getName());

	private boolean ctrlPressed = false;

	public CtrlPressedListener(Object obj) {
		// Track when the Ctrl key is down
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(ke -> {

			synchronized (obj.getClass()) {
				switch (ke.getID()) {
				case KeyEvent.KEY_PRESSED:
					if (ke.getKeyCode() == KeyEvent.VK_CONTROL) {
						ctrlPressed = true;
//						LOGGER.fine("Ctrl pressed in " + obj.getClass().getSimpleName());
					}

					break;
				case KeyEvent.KEY_RELEASED:
					if (ke.getKeyCode() == KeyEvent.VK_CONTROL)
						ctrlPressed = false;
					break;
				default:
					break;
				}

				return false;
			}
		});
	}

	public boolean isCtrlPressed() {
		return ctrlPressed;
	}
}
