package com.bmskinner.nuclear_morphology.gui.dialogs.prober;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JPanel;

import com.bmskinner.nuclear_morphology.analysis.nucleus.DefaultNucleusDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.DefaultAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableDetectionOptions;
import com.bmskinner.nuclear_morphology.gui.LoadingIconDialog;

/**
 * Integrates the analysis setup dialog with the image prober.
 * Experimental.
 * @author ben
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class IntegratedImageProber extends LoadingIconDialog {
	
	private static final String PROCEED_LBL          = "Proceed with analysis";
	private static final String DIALOG_TITLE_BAR_LBL = "Nucleus detection settings";

	
	private IMutableAnalysisOptions options; // the active options
	
	private JPanel optionsSettingsPanel; // settings
	
	private JPanel imageProberPanel; // result
	
	private boolean ok = false;
	
	private JButton okButton     = new JButton(PROCEED_LBL);

	
	public IntegratedImageProber(final File folder){
		
		options = new DefaultAnalysisOptions();

		IMutableDetectionOptions nucleusOptions = new DefaultNucleusDetectionOptions(folder);
		options.setDetectionOptions(IAnalysisOptions.NUCLEUS, nucleusOptions);

		// make the panel
		optionsSettingsPanel = new NucleusDetectionSettingsPanel(options);
		this.add(optionsSettingsPanel, BorderLayout.WEST);
		
		try {
		imageProberPanel = new ImageProberPanel(options.getDetectionOptions(IAnalysisOptions.NUCLEUS), new NucleusImageSet());
		} catch (Exception e){
			error("Error", e);
			imageProberPanel = new JPanel();
		}
		
		this.add(imageProberPanel,     BorderLayout.CENTER);
		
		JPanel footerPanel = createFooter();
		this.add(footerPanel,     BorderLayout.SOUTH);
		
		this.setTitle(DIALOG_TITLE_BAR_LBL);
		this.setModal(true);
		this.pack();
		this.setVisible(true);
				
		// add a listener to the options settings panel - when they change, refresh the
		// panel and update the analysis options
		
		// Detect which stage of the process has been altered, so only the necessary 
		// table cells are updated
		
		// Need to be able to cancel updating panels when options change
	}
	
	/**
	 * Make the footer panel, with ok and cancel buttons
	 * @return
	 */
	private JPanel createFooter(){
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.RIGHT));
				
		okButton.addActionListener( e -> {
				ok = true;
				setVisible(false);
		});
		panel.add(okButton);

		getRootPane().setDefaultButton(okButton);

		return panel;
	}
	
	/**
	 * Get the current options
	 * @return
	 */
	public IMutableAnalysisOptions getOptions(){
		return options;
	}
	
	/**
	 * Check if the analysis is ready to run
	 * @return
	 */
	public boolean isOk(){
		return ok;
	}

}
