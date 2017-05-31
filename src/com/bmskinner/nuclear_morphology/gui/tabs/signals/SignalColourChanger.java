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

package com.bmskinner.nuclear_morphology.gui.tabs.signals;

import java.awt.Color;
import java.util.UUID;

import javax.swing.JColorChooser;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclear.UnavailableSignalGroupException;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Handles updating of signal group colours via the UI, and triggers a chart recache
 * @author bms41
 * @since 1.13.7
 *
 */
public class SignalColourChanger implements Loggable {
	
	private final DetailPanel p;
	
	public SignalColourChanger(final DetailPanel parent){
		p = parent;
	}
	
	/**
	 * Update the colour of the clicked signal group
	 * @param row the row selected (the colour bar, one above the group name)
	 */
    public void updateSignalColour(IAnalysisDataset d, Color oldColour, UUID signalGroupId){
		
		try {

			Color newColor = JColorChooser.showDialog(
					p,
					Labels.CHOOSE_SIGNAL_COLOUR,
					oldColour);

			if(newColor != null){
				d.getCollection().getSignalGroup(signalGroupId).setGroupColour(newColor);
				
				p.update();
			}
		} catch(UnavailableSignalGroupException e){
			warn("Cannot change signal colour");
			stack("Error getting signal group", e);
		}
	}

}
