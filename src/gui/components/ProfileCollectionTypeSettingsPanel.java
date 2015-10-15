/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
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
package gui.components;

import java.util.HashMap;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;

import components.CellCollection.ProfileCollectionType;

@SuppressWarnings("serial")
public class ProfileCollectionTypeSettingsPanel extends EnumeratedOptionsPanel {
	
	private Map<ProfileCollectionType, JRadioButton> map  = new  HashMap<ProfileCollectionType, JRadioButton>();
	
	public ProfileCollectionTypeSettingsPanel(){
		
		super();
		final ButtonGroup group = new ButtonGroup();
		
		for(ProfileCollectionType type : ProfileCollectionType.values()){
			JRadioButton button = new JRadioButton(type.toString());
			button.setActionCommand(type.toString());
			button.addActionListener(this);
			this.add(button);
			group.add(button);
			map.put(type, button);
		}
		// Set the default
		map.get(ProfileCollectionType.FRANKEN).setSelected(true);
		
	}
	
	/**
	 * Get the selected profile type, or null
	 * @return
	 */
	public ProfileCollectionType getSelected(){
		for(ProfileCollectionType type : ProfileCollectionType.values()){
			JRadioButton button = map.get(type);
			if(button.isSelected()){
				return type;
			}
		}
		return null;
	}

}
