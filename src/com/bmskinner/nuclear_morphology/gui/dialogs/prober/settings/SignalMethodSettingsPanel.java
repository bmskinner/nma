package com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import com.bmskinner.nuclear_morphology.components.options.IMutableNuclearSignalOptions;
import com.bmskinner.nuclear_morphology.components.options.INuclearSignalOptions.SignalDetectionMode;
import com.bmskinner.nuclear_morphology.gui.Labels;

@SuppressWarnings("serial")
public class SignalMethodSettingsPanel extends SettingsPanel {
	
	private static final String METHOD_LBL = "Method";
	
	private static final String FORWARD_DESC_LABEL  = Labels.FORWARD_THRESHOLDING_RADIO_LABEL;
	private static final String REVERSE_DESC_LABEL  = Labels.REVERSE_THRESHOLDING_RADIO_LABEL;
	private static final String ADAPTIVE_DESC_LABEL = Labels.ADAPTIVE_THRESHOLDING_RADIO_LABEL;

	private IMutableNuclearSignalOptions options;
	
	private JComboBox<SignalDetectionMode> box;
	
	private JPanel cardPanel;
	
	public SignalMethodSettingsPanel(IMutableNuclearSignalOptions op){
		options = op;
		
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		cardPanel = createCardPanel();
		this.add(createPanel(), BorderLayout.CENTER);
		this.add(cardPanel, BorderLayout.SOUTH);
		
	}
	
	private JPanel createCardPanel(){
		CardLayout cl = new CardLayout();
		JPanel cardPanel = new JPanel(cl);
		cardPanel.add(new JLabel(FORWARD_DESC_LABEL),     FORWARD_DESC_LABEL);
		cardPanel.add(new JLabel(REVERSE_DESC_LABEL),     REVERSE_DESC_LABEL);
		cardPanel.add(new JLabel(ADAPTIVE_DESC_LABEL),    ADAPTIVE_DESC_LABEL);

	    cl.show(cardPanel, FORWARD_DESC_LABEL);
	    
	    return cardPanel;
	}
	
	private JPanel createPanel(){
		createSpinners();
		
		JPanel panel = new JPanel();
		
		List<JLabel> labels = new ArrayList<JLabel>();
		labels.add( new JLabel(METHOD_LBL));
		
		List<JComboBox> fields = new ArrayList<JComboBox>();
		fields.add( box );

		addLabelTextRows(labels, fields, panel );
		
		// Make the description panel

		return panel;
	}
	
	private void createSpinners(){
		
		box = new JComboBox<SignalDetectionMode>(SignalDetectionMode.values());
		box.setSelectedItem(SignalDetectionMode.FORWARD);
		
		box.addActionListener( e ->{
			
			SignalDetectionMode mode = (SignalDetectionMode) box.getSelectedItem();
			options.setDetectionMode(mode);
			
			CardLayout cl = (CardLayout) cardPanel.getLayout();
			
			cl.show(cardPanel, mode.getDesc());
			
			fireOptionsChangeEvent();
		});
		
		
	}
	
	@Override
	public void setEnabled(boolean b){
		super.setEnabled(b);
		box.setEnabled(b);

	}

}
