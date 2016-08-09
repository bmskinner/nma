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

import components.generic.BorderTag;
import components.generic.BorderTagObject;
import components.generic.BorderTag.BorderTagType;

@SuppressWarnings("serial")
public class BorderTagOptionsPanel extends EnumeratedOptionsPanel {
	
	private Map<BorderTagObject, JRadioButton> map  = new  HashMap<BorderTagObject, JRadioButton>();
	
	public BorderTagOptionsPanel(){
		
		super();
		final ButtonGroup group = new ButtonGroup();
		
		for(BorderTagObject type : BorderTagObject.values(BorderTagType.CORE)){
			JRadioButton button = new JRadioButton(type.toString());
			button.setActionCommand(type.toString());
			button.addActionListener(this);
			this.add(button);
			group.add(button);
			map.put(type, button);
		}
		// Set the default
		map.get(BorderTagObject.REFERENCE_POINT).setSelected(true);
		
	}
	
	public void setEnabled(boolean b){
		for(BorderTagObject type : BorderTagObject.values(BorderTagType.CORE)){
			map.get(type).setEnabled(b);
		}
	}

	/**
	 * Get the selected profile type, or null
	 * @return
	 */
	public BorderTagObject getSelected(){
		for(BorderTagObject type : BorderTagObject.values(BorderTagType.CORE)){
			JRadioButton button = map.get(type);
			if(button.isSelected()){
				return type;
			}
		}
		return null;
	}

}
