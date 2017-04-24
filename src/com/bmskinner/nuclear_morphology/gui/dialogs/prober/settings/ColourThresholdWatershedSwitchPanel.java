package com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import com.bmskinner.nuclear_morphology.components.options.ICannyOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.MissingOptionException;

public class ColourThresholdWatershedSwitchPanel  extends DetectionSettingsPanel implements ActionListener {
	
	private static final String THRESHOLD_LBL = "Colour Threshold";
	private static final String WATERSHED_LBL      = "Watershed";
	
	private JPanel cardPanel;

	private JRadioButton thresholdBtn = new JRadioButton(THRESHOLD_LBL);
	private JRadioButton waterBtn     = new JRadioButton(WATERSHED_LBL);
	private ButtonGroup  group        = new ButtonGroup();
	
	public ColourThresholdWatershedSwitchPanel(final IMutableDetectionOptions options){
		super(options);		
		this.add(createPanel(), BorderLayout.CENTER);
		
	}
	
	private JPanel createPanel(){
		
		JPanel panel = new JPanel();

		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		
		JPanel switchPanel = makeSwitchPanel();		
		cardPanel   = makeCardPanel();
		
		panel.add(switchPanel);
	    panel.add(cardPanel);
			
		return panel;
	}
	
	private JPanel makeCardPanel(){
		JPanel cardPanel = new JPanel(new CardLayout());
		try {
			ICannyOptions canny = options.getCannyOptions();
			
			SettingsPanel thresholdPanel = new ColourThresholdingSettingsPanel(options);
			SettingsPanel watershedPanel = new WatershedSettingsPanel(options);
			
			this.addSubPanel(thresholdPanel);
			this.addSubPanel(watershedPanel);
			
			
			cardPanel.add(thresholdPanel, THRESHOLD_LBL);
			cardPanel.add(watershedPanel, WATERSHED_LBL);
			CardLayout cl = (CardLayout)(cardPanel.getLayout());
		    cl.show(cardPanel, WATERSHED_LBL);
		} catch (MissingOptionException e) {
			warn("Missing options");
			stack(e);
		}
		
		
		
	    return cardPanel;
	}
	
	
	/**
	 * A panel with the radio buttons to choose edge detection or
	 * threshold for the nucleus
	 * @return
	 */
	private JPanel makeSwitchPanel(){
		JPanel panel = new JPanel(new FlowLayout());
		
		thresholdBtn.setSelected(false);
		waterBtn.setSelected(true);
		thresholdBtn.setActionCommand(THRESHOLD_LBL);
		waterBtn.setActionCommand(WATERSHED_LBL);

		//Group the radio buttons.
		group.add(thresholdBtn);
		group.add(waterBtn);
		
		thresholdBtn.addActionListener(this);
		waterBtn.addActionListener(this);
		
		
		panel.add(thresholdBtn);
		panel.add(waterBtn);
		
		return panel;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals(THRESHOLD_LBL)){

			
			CardLayout cl = (CardLayout)(cardPanel.getLayout());
		    cl.show(cardPanel, THRESHOLD_LBL);

		}
		
		if(e.getActionCommand().equals(WATERSHED_LBL)){

			CardLayout cl = (CardLayout)(cardPanel.getLayout());
		    cl.show(cardPanel, WATERSHED_LBL);
		}
		fireOptionsChangeEvent();
				
	}


	@Override
	public void update() {
		super.update();

		
		CardLayout cl = (CardLayout)(cardPanel.getLayout());
		
		if(showCanny){
			cl.show(cardPanel, WATERSHED_LBL);
		} else {
			cl.show(cardPanel, THRESHOLD_LBL);
		}
		
	}
	
	@Override
	public void setEnabled(boolean b){
		super.setEnabled(b);
		thresholdBtn.setEnabled(b);
		waterBtn.setEnabled(b);

	}

	@Override
	public void set(IDetectionOptions options) {
		this.options.set(options);
		update();
		
	}

}
