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
package gui.components.panels;

import java.util.HashMap;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;

import components.generic.ProfileType;

@SuppressWarnings("serial")
public class ProfileCollectionTypeSettingsPanel extends EnumeratedOptionsPanel {
	
	private Map<ProfileType, JRadioButton> map  = new  HashMap<ProfileType, JRadioButton>();
	
	public ProfileCollectionTypeSettingsPanel(){
		
		super();
		final ButtonGroup group = new ButtonGroup();
		
		for(ProfileType type : ProfileType.values()){
			JRadioButton button = new JRadioButton(type.toString());
			button.setActionCommand(type.toString());
			button.addActionListener(this);
			this.add(button);
			group.add(button);
			map.put(type, button);
		}
		// Set the default
		map.get(ProfileType.FRANKEN).setSelected(true);
		
	}
	
	public void setEnabled(boolean b){

		for(ProfileType type : ProfileType.values()){
			map.get(type).setEnabled(b);
		}
	}
	
	/**
	 * Get the selected profile type, or null
	 * @return
	 */
	public ProfileType getSelected(){
		for(ProfileType type : ProfileType.values()){
			JRadioButton button = map.get(type);
			if(button.isSelected()){
				return type;
			}
		}
		return null;
	}

}
