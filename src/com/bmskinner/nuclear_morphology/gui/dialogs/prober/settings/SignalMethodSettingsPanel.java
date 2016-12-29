package com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import com.bmskinner.nuclear_morphology.components.options.IMutableNuclearSignalOptions;
import com.bmskinner.nuclear_morphology.components.options.INuclearSignalOptions.SignalDetectionMode;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.sun.xml.internal.ws.api.Component;

@SuppressWarnings("serial")
public class SignalMethodSettingsPanel extends SettingsPanel {
	
	private static final String METHOD_LBL = "Method";
	
	private IMutableNuclearSignalOptions options;
	
	private JComboBox<SignalDetectionMode> box;
	
	public SignalMethodSettingsPanel(IMutableNuclearSignalOptions op){
		options = op;
		this.add(createPanel(), BorderLayout.CENTER);
		
	}
	
	private JPanel createPanel(){
		createSpinners();
		
		JPanel panel = new JPanel();
		
		List<JLabel> labels = new ArrayList<JLabel>();
		labels.add( new JLabel(METHOD_LBL));
		
		List<JComboBox> fields = new ArrayList<JComboBox>();
		fields.add( box );

		addLabelTextRows(labels, fields, panel );
		
		return panel;
		
	}
	
	private void createSpinners(){
		
		box = new JComboBox<SignalDetectionMode>(SignalDetectionMode.values());
		box.setSelectedItem(SignalDetectionMode.FORWARD);
		
		box.addActionListener( e ->{
			options.setDetectionMode((SignalDetectionMode) box.getSelectedItem());
			fireOptionsChangeEvent();
		});
		
		
	}


}
