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
package no.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

@SuppressWarnings("serial")
public class ProflleDisplaySettingsPanel extends JPanel {
	
	public JRadioButton rawProfileLeftButton  = new JRadioButton("Left"); // left align raw profiles in rawChartPanel
	public JRadioButton rawProfileRightButton = new JRadioButton("Right"); // right align raw profiles in rawChartPan
	public JCheckBox    normCheckBox 	= new JCheckBox("Normalised");	// to toggle raw or normalised segment profiles in segmentsProfileChartPanel
	
	public JRadioButton referenceButton  = new JRadioButton("Reference point"); // start drawing from reference
	public JRadioButton orientationButton = new JRadioButton("Orientation point"); // start drawing from orientation
	
	public JCheckBox showMarkersCheckBox = new JCheckBox("Show markers");	// to toggle OP and RP lines
	
	public ProflleDisplaySettingsPanel(){
		this.setLayout(new FlowLayout());
		
		rawProfileLeftButton.setSelected(true);
		rawProfileLeftButton.setActionCommand("LeftAlignRawProfile");
		rawProfileRightButton.setActionCommand("RightAlignRawProfile");
				
		
		// checkbox to select raw or normalised profiles
		normCheckBox.setSelected(true);
		normCheckBox.setActionCommand("NormalisedProfile");
		
		
		JPanel alignPanel = makealignPanel();
		this.add(alignPanel);
		
		// checkbox to show markers on profiles
		showMarkersCheckBox.setSelected(true);
		showMarkersCheckBox.setActionCommand("ToggleMarkers");
		this.add(showMarkersCheckBox);
		
		this.setEnabled(false);
	}
	
	private JPanel makealignPanel(){
		//Group the radio buttons.
		JPanel panel = new JPanel(new FlowLayout()){
			@Override
			public void setEnabled(boolean enabled){
				for(Component c : this.getComponents()){
					c.setEnabled(enabled);
				}
			}
		};
		final ButtonGroup alignGroup = new ButtonGroup();
		alignGroup.add(rawProfileLeftButton);
		alignGroup.add(rawProfileRightButton);

		panel.add(normCheckBox);
		panel.add(rawProfileLeftButton);
		panel.add(rawProfileRightButton);


		// Add the radio buttons to choose between reference and orientation drawing
		referenceButton.setSelected(false);
		referenceButton.setActionCommand("DrawReferencePoint");

		orientationButton.setSelected(true);
		orientationButton.setActionCommand("DrawOrientationPoint");

		final ButtonGroup drawPointGroup = new ButtonGroup();
		drawPointGroup.add(referenceButton);
		drawPointGroup.add(orientationButton);

		panel.add(referenceButton);
		panel.add(orientationButton);
		return panel;
	}
	
	@Override
	public void setEnabled(boolean enabled){
		for(Component c : this.getComponents()){
			c.setEnabled(enabled);
		}
	}
	
}