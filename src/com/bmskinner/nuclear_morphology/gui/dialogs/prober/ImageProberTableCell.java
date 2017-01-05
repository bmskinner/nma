package com.bmskinner.nuclear_morphology.gui.dialogs.prober;

import java.awt.Dimension;
import java.awt.Image;

import javax.swing.ImageIcon;

import com.bmskinner.nuclear_morphology.analysis.image.ImageFilterer;

import ij.process.ImageProcessor;

public class ImageProberTableCell {
	private ImageIcon smallIcon;
	private ImageIcon largeIcon;
	private ImageType type;
	private boolean enabled;
	private int position;
	
	public ImageProberTableCell(ImageIcon largeIcon, ImageType type, boolean enabled, int position){
		
		this.largeIcon = largeIcon;
		this.type = type;
		this.position = position;
		this.enabled = enabled;
	}

	public ImageIcon getSmallIcon() {
		return smallIcon;
	}

	public ImageIcon getLargeIcon() {
		return largeIcon;
	}
	
	/**
	 * Create a new image icon from scaling the large image to a given
	 * fraction of the screen size and maintaining aspect ratio. I.E.:
	 * if resizing the width to <i>fraction</i> of the screen width results in the height begin greater
	 * than <i>fraction</i> of the screen height, the width will be reduced so 
	 * height equals <i>fraction<i> of the screen height
	 * @param fraction the fraction, from 0-1
	 * @return
	 */
	public ImageIcon getLargeIconFitToScreen(double fraction) {
		
		if(largeIcon==null){
			throw new IllegalArgumentException("Large icon is null");
		}
		
		int originalWidth  = largeIcon.getImage().getWidth(null);
		int originalHeight = largeIcon.getImage().getHeight(null);

		// keep the image aspect ratio
		double ratio = (double) originalWidth / (double) originalHeight;
		
		Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();

		// set the new width
		int newWidth = (int) ( screenSize.getWidth() * fraction);
		int newHeight = (int) (   (double) newWidth / ratio);
		
		// Check height is OK. If not, recalculate sizes
		if(newHeight >= screenSize.getHeight()){
			newHeight = (int) ( screenSize.getHeight() * fraction);
			newWidth = (int) (   (double) newHeight * ratio);
		}

		// Create the image

		Image result = largeIcon.getImage().getScaledInstance(newWidth, newHeight, Image.SCALE_FAST);

		return new ImageIcon(result);
	}

	public ImageType getType() {
		return type;
	}
	
	public int getPosition(){
		return position;
	}
	
//	public String getLabel(){
//		return label;
//	}
	
	public boolean hasType(){
		return type!=null;
	}

	public void setSmallIcon(ImageIcon smallIcon) {
		this.smallIcon = smallIcon;
	}
	
	
	public boolean hasSmallIcon(){
		return smallIcon!=null;
	}
	
	public boolean hasLargeIcon(){
		return largeIcon!=null;
	}
	
	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	public double getFactor(){
		// Translate coordinates back to large image
		double factor = (double) largeIcon.getIconWidth() / (double)smallIcon.getIconWidth();
		return factor;
	}

	public String toString(){
		return type==null ? "" : enabled ? type.toString() : type.toString()+" (disabled)";
	}
}
