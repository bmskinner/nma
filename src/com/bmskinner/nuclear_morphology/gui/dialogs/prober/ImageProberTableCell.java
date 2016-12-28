package com.bmskinner.nuclear_morphology.gui.dialogs.prober;

import javax.swing.ImageIcon;

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
