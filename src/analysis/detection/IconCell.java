/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package analysis.detection;

import gui.ImageType;

import javax.swing.ImageIcon;

public class IconCell {
	
	private ImageIcon smallIcon = null;
	private ImageIcon largeIcon = null;
	private ImageType type = null;
	private boolean enabled = true;
	
	public IconCell(ImageIcon largeIcon, ImageType type){
		this.largeIcon = largeIcon;
		this.type = type;
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

	public String toString(){
		return type==null ? "" : enabled ? type.toString() : type.toString()+" (disabled)";
	}
	
}
