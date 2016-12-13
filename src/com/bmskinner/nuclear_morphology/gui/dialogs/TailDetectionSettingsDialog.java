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
package com.bmskinner.nuclear_morphology.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.utility.Constants;

public class TailDetectionSettingsDialog extends SettingsDialog implements ActionListener {

	private static final long serialVersionUID = 1L;
	
	private JPanel contentPanel;
	
	private JComboBox<String> channelSelection;
	
	private CannyPanel cannyPanel;
	
	private IAnalysisOptions options;
	
	private int channel;

	/**
	 * Create the dialog.
	 */
	public TailDetectionSettingsDialog(final IAnalysisOptions a) {
		super();
		setModal(true);
		this.options = a;
		createGUI();
		
		pack();
		setVisible(true);
	}
	private void createGUI(){
		setTitle("Tail detection");
		setBounds(100, 100, 450, 300);
		
		getContentPane().setLayout(new BorderLayout());
		
		contentPanel = new JPanel();

		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		
		channelSelection = new JComboBox<String>(channelOptionStrings);
		channelSelection.addActionListener(this);
		contentPanel.add(channelSelection);
		
		cannyPanel = new CannyPanel(options.getDetectionOptions(IAnalysisOptions.SPERM_TAIL).getCannyOptions());
		contentPanel.add(cannyPanel);
		
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		
		getContentPane().add(makeLowerButtonPanel(), BorderLayout.SOUTH);
	}
	
	/**
	 * Create the panel with ok and cancel buttons
	 * @return the panel
	 */
	private JPanel makeLowerButtonPanel(){
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout());

		JButton btnOk = new JButton("OK");
		btnOk.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				TailDetectionSettingsDialog.this.setVisible(false);
			}
		});

		panel.add(btnOk);

		JButton btnCancel = new JButton("Cancel");
		btnCancel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				TailDetectionSettingsDialog.this.dispose();
			}
		});
		panel.add(btnCancel);
		return panel;
	}
	@Override
	public void actionPerformed(ActionEvent e) {

		if(e.getSource() instanceof JComboBox<?>){
			
			JComboBox<?> cb = (JComboBox<?>)e.getSource();
	        
			String channelName = cb.getSelectedItem().toString();
	        
	        channel = channelName.equals("Red") 
					? Constants.RGB_RED
							: channelName.equals("Green") 
							? Constants.RGB_GREEN
									: Constants.RGB_BLUE;
		}
		
	}
		
	public int getChannel(){
		return this.channel;
	}

}
