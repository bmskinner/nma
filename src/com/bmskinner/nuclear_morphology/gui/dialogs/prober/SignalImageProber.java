package com.bmskinner.nuclear_morphology.gui.dialogs.prober;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.File;
import java.util.UUID;

import javax.swing.JPanel;

import com.bmskinner.nuclear_morphology.analysis.nucleus.DefaultNucleusDetectionOptions;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclear.SignalGroup;
import com.bmskinner.nuclear_morphology.components.options.DefaultNuclearSignalOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableNuclearSignalOptions;
import com.bmskinner.nuclear_morphology.components.options.INuclearSignalOptions;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;

public class SignalImageProber extends IntegratedImageProber {
	
	private static final String DIALOG_TITLE_BAR_LBL = "Signal detection settings";
	
	private final IMutableNuclearSignalOptions options;

	public SignalImageProber(final IAnalysisDataset dataset, final File folder){
		
		options = new DefaultNuclearSignalOptions(folder);
		double scale = dataset.getAnalysisOptions().getDetectionOptions(IAnalysisOptions.NUCLEUS).getScale();
		options.setScale(scale);
		
		try {
			
			// make the panel
			optionsSettingsPanel = new SignalDetectionSettingsPanel(options);
			imageProberPanel     = new SignalImageProberPanel(this, options, ImageSet.SIGNAL_IMAGE_SET, dataset);
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

		this.pack();
		this.setModal(true);
		this.setLocationRelativeTo(null); // centre on screen
		this.setVisible(true);
	}
	
	public INuclearSignalOptions getOptions(){
		return options;
	}
	
	protected void okButtonClicked(){
//		UUID signalGroup = java.util.UUID.randomUUID();
//		
//		// get the group name
//        
//        SignalGroup group = new SignalGroup();
//  
//        group.setChannel(signalOptions.getChannel());
//        group.setFolder(signalOptions.getFolder());
//        
//       
//        
//        dataset.getCollection().addSignalGroup(signalGroup, group);
//        
//        // Set the default colour for the signal group
//        int totalGroups = dataset.getCollection().getSignalGroups().size();
//        Color colour = (Color) ColourSelecter.getColor(totalGroups);
//        group.setGroupColour(colour);
//        
//        
//        options.setDetectionOptions(signalGroup.toString(), testOptions);//.addNuclearSignalOptions( signalGroup, testOptions);

	}

}
