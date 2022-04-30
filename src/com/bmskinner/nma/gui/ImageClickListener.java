package com.bmskinner.nma.gui;

public interface ImageClickListener {
	
	/**Notify listeners that an image has been clicked 
	 * @param x the x position in the clicked image
	 * @param y the y position in the clicked image
	 */
	void imageClicked(int x, int y);

}
