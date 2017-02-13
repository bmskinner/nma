package com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.ICannyOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.MissingOptionException;

/**
 * Holds a canny settings panel and a threshold settings panel. Uses
 * a card layout to switch between them
 * @author ben
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class EdgeThresholdSwitchPanel extends DetectionSettingsPanel implements ActionListener {
	
	private static final String THRESHOLD_LBL = "Threshold";
	private static final String EDGE_LBL      = "Edge detection";
	
	private JPanel cardPanel;

	private JRadioButton thresholdBtn = new JRadioButton(THRESHOLD_LBL);
	private JRadioButton edgeBtn      = new JRadioButton(EDGE_LBL);
	private ButtonGroup  group        = new ButtonGroup();
	
	public EdgeThresholdSwitchPanel(final IMutableDetectionOptions options){
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
			
			SettingsPanel cannyPanel     = new CannySettingsPanel(canny.unlock());
			SettingsPanel thresholdPanel = new ThresholdSettingsPanel(options);
			
			this.addSubPanel(cannyPanel);
			this.addSubPanel(thresholdPanel);
			
			
			cardPanel.add(cannyPanel,     EDGE_LBL);
			cardPanel.add(thresholdPanel, THRESHOLD_LBL);
			CardLayout cl = (CardLayout)(cardPanel.getLayout());
		    cl.show(cardPanel, EDGE_LBL);
		} catch (MissingOptionException e) {
			warn("Missing canny options");
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
		edgeBtn.setSelected(true);
		thresholdBtn.setActionCommand(THRESHOLD_LBL);
		edgeBtn.setActionCommand(EDGE_LBL);

		//Group the radio buttons.
		group.add(thresholdBtn);
		group.add(edgeBtn);
		
		thresholdBtn.addActionListener(this);
		edgeBtn.addActionListener(this);
		
		
		panel.add(thresholdBtn);
		panel.add(edgeBtn);
		
		return panel;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals(THRESHOLD_LBL)){
			try {
				options.getCannyOptions().setUseCanny(false);
			} catch (MissingOptionException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			CardLayout cl = (CardLayout)(cardPanel.getLayout());
		    cl.show(cardPanel, THRESHOLD_LBL);

		}
		
		if(e.getActionCommand().equals(EDGE_LBL)){
			try {
				options.getCannyOptions().setUseCanny(true);
			} catch (MissingOptionException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			CardLayout cl = (CardLayout)(cardPanel.getLayout());
		    cl.show(cardPanel, EDGE_LBL);
		}
		fireOptionsChangeEvent();
				
	}


	@Override
	public void update() {
		super.update();
		boolean showCanny;
		try {
			showCanny = options.getCannyOptions().isUseCanny();
		} catch (MissingOptionException e) {
			showCanny = false;
		}
		
		CardLayout cl = (CardLayout)(cardPanel.getLayout());
		
		if(showCanny){
			cl.show(cardPanel, EDGE_LBL);
		} else {
			cl.show(cardPanel, THRESHOLD_LBL);
		}
		
	}
	
	@Override
	public void setEnabled(boolean b){
		super.setEnabled(b);
		thresholdBtn.setEnabled(b);
		edgeBtn.setEnabled(b);

	}

	@Override
	public void set(IDetectionOptions options) {
		this.options.set(options);
		update();
		
	}

}
