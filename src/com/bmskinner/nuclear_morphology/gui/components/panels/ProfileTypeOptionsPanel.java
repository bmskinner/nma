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
package com.bmskinner.nuclear_morphology.gui.components.panels;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import com.bmskinner.nuclear_morphology.components.generic.ProfileType;

@SuppressWarnings("serial")
public class ProfileTypeOptionsPanel extends EnumeratedOptionsPanel {

	private JComboBox<ProfileType> profileTypeBox; //= new JComboBox<ProfileType>(ProfileType.values());

	public ProfileTypeOptionsPanel(){
		super();
		
		// FrankenProfiles cause problems for individual nuclei, so disable for now
		DefaultComboBoxModel<ProfileType> model = new DefaultComboBoxModel<ProfileType>();
		model.addElement(ProfileType.ANGLE);
		model.addElement(ProfileType.DIAMETER);
		model.addElement(ProfileType.RADIUS);
		
		profileTypeBox = new JComboBox<ProfileType>(model);
		
		this.add(profileTypeBox);
		profileTypeBox.addActionListener(this);
		profileTypeBox.setSelectedItem(ProfileType.ANGLE);
	
	}

	public ProfileType getSelected(){
		return (ProfileType) profileTypeBox.getSelectedItem();
	}

	public void setEnabled(boolean b){
		profileTypeBox.setEnabled(b);
		
	}
}
