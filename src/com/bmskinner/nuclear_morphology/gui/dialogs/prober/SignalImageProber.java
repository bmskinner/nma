package com.bmskinner.nuclear_morphology.gui.dialogs.prober;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.File;
import java.util.UUID;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.bmskinner.nuclear_morphology.analysis.detection.pipelines.Finder;
import com.bmskinner.nuclear_morphology.analysis.signals.SignalFinder;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclear.SignalGroup;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableNuclearSignalOptions;
import com.bmskinner.nuclear_morphology.components.options.INuclearSignalOptions;
import com.bmskinner.nuclear_morphology.components.options.MissingOptionException;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings.SignalDetectionSettingsPanel;

/**
 * Show the results of a signal detection with given options
 * @author ben
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class SignalImageProber extends IntegratedImageProber {
	
	private static final String DIALOG_TITLE_BAR_LBL = "Signal detection settings";
	private static final String NEW_NAME_LBL = "Enter a signal group name";
	
	private final IMutableNuclearSignalOptions options;
	final IAnalysisDataset dataset;
	private UUID id;

	/**
	 * Create with a dataset (from which nuclei will be drawn) and a folder of images to
	 * be analysed
	 * @param dataset the analysis dataset
	 * @param folder the folder of images
	 */
	public SignalImageProber(final IAnalysisDataset dataset, final File folder){
		this.dataset = dataset;
		options = OptionsFactory.makeNuclearSignalOptions(folder);
		
		double scale;
		try {
			scale = dataset.getAnalysisOptions()
					.getDetectionOptions(IAnalysisOptions.NUCLEUS)
					.getScale();
			
			options.setScale(scale);
		} catch (MissingOptionException e1) {
			warn("Cannot set scale");
			stack(e1.getMessage(), e1);
		}
		
		
		try {
			
			// make the panel
			optionsSettingsPanel = new SignalDetectionSettingsPanel(options);
			
			Finder<?> finder = new SignalFinder(dataset.getAnalysisOptions(), options, dataset.getCollection());
			imageProberPanel = new GenericImageProberPanel(folder, finder, this);
//			imageProberPanel     = new SignalImageProberPanel(this, options, ImageSet.SIGNAL_IMAGE_SET, dataset);
			JPanel footerPanel   = createFooter();
			
			this.add(optionsSettingsPanel, BorderLayout.WEST);
			this.add(imageProberPanel,     BorderLayout.CENTER);
			this.add(footerPanel,          BorderLayout.SOUTH);

			this.setTitle(DIALOG_TITLE_BAR_LBL);
			
			optionsSettingsPanel.addProberReloadEventListener(imageProberPanel);
			imageProberPanel.addPanelUpdatingEventListener(optionsSettingsPanel);
			
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
	
	/**
	 * Get the ID of the newly created signal group
	 * @return
	 */
	public UUID getId(){
		return id;
	}
	
	@Override
	protected void okButtonClicked(){
		
		String name = getGroupName();
		
		UUID signalGroup = java.util.UUID.randomUUID();
		id = signalGroup;
		
		// get the group name
        
        SignalGroup group = new SignalGroup();
  
        group.setChannel(options.getChannel());
        group.setFolder(options.getFolder());
        group.setGroupName(name);
        
        dataset.getCollection().addSignalGroup(signalGroup, group);
        
        // Set the default colour for the signal group
        int totalGroups = dataset.getCollection().getSignalGroups().size();
        Color colour = (Color) ColourSelecter.getColor(totalGroups);
        group.setGroupColour(colour);
        
        
        try {
			dataset.getAnalysisOptions().setDetectionOptions(signalGroup.toString(), options);
		} catch (MissingOptionException e) {
			warn("Error getting dataset options");
			stack(e.getMessage(), e);
		}

	}

	/**
	 * Get the name of a signal group
	 * If blank, requests a new name 
	 * @return a valid name
	 */
	private String getGroupName(){

		String name = (String) JOptionPane.showInputDialog(NEW_NAME_LBL);


		return name;

	}

}
