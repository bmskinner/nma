package com.bmskinner.nuclear_morphology.gui.dialogs.prober;

import javax.swing.ImageIcon;

public class ImageProberTableCell {
	private ImageIcon smallIcon = null;
	private ImageIcon largeIcon = null;
	private ImageSet type = null;
	private String label = "";
	private boolean enabled = true;
	private int position = 0;
	
	public ImageProberTableCell(ImageIcon largeIcon, ImageSet type, String label, int position){
		this.largeIcon = largeIcon;
		this.type = type;
		this.position = position;
	}

	public ImageIcon getSmallIcon() {
		return smallIcon;
	}

	public ImageIcon getLargeIcon() {
		return largeIcon;
	}

	public ImageSet getType() {
		return type;
	}
	
	public String getLabel(){
		return label;
	}
	
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
		return type==null ? "" : enabled ? label : label+" (disabled)";
	}
}
