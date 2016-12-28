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
	
	private static final String PROCEED_LBL          = "Proceed with detection";
	private static final String DIALOG_TITLE_BAR_LBL = "Nucleus detection settings";

	
	private IMutableAnalysisOptions options; // the active options
	
	private SettingsPanel optionsSettingsPanel; // settings
	
	private ImageProberPanel imageProberPanel; // result
	
	private boolean ok = false;
	
	private JButton okButton     = new JButton(PROCEED_LBL);

	
	public IntegratedImageProber(final File folder){
		try {
			options = new DefaultAnalysisOptions();

			IMutableDetectionOptions nucleusOptions = new DefaultNucleusDetectionOptions(folder);
			options.setDetectionOptions(IAnalysisOptions.NUCLEUS, nucleusOptions);

			// make the panel
			optionsSettingsPanel = new NucleusDetectionSettingsPanel(options);
			imageProberPanel     = new NucleusImageProberPanel(this, nucleusOptions, new NucleusImageSet());
			JPanel footerPanel   = createFooter();
			
			this.add(optionsSettingsPanel, BorderLayout.WEST);
			this.add(imageProberPanel,     BorderLayout.CENTER);
			this.add(footerPanel,          BorderLayout.SOUTH);

			this.setTitle(DIALOG_TITLE_BAR_LBL);
			
			optionsSettingsPanel.addProberReloadEventListener(imageProberPanel);
			
			
		} catch (Exception e){
			warn("Error launching analysis window");
			stack(e.getMessage(), e);
			this.dispose();
		}	
		// add a listener to the options settings panel - when they change, refresh the
		// panel and update the analysis options
		
		// Detect which stage of the process has been altered, so only the necessary 
		// table cells are updated
		
		// Need to be able to cancel updating panels when options change
		this.pack();
		this.setModal(true);
		this.setLocationRelativeTo(null); // centre on screen
		this.setVisible(true);
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
