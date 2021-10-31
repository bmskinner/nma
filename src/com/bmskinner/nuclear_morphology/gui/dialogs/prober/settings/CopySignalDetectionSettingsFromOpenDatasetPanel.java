package com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings;

import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.MissingOptionException;
import com.bmskinner.nuclear_morphology.core.DatasetListManager;
import com.bmskinner.nuclear_morphology.gui.components.FileSelector;
import com.bmskinner.nuclear_morphology.io.xml.XMLReader;
import com.bmskinner.nuclear_morphology.io.xml.XMLReader.XMLReadingException;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Button that allows nuclear signal detection options to be copied from an open
 * dataset or a saved options file
 * 
 * @author ben
 * @since 1.15.0
 *
 */
public class CopySignalDetectionSettingsFromOpenDatasetPanel extends CopyFromOpenDatasetPanel {
	
	private static final Logger LOGGER = Logger.getLogger(CopySignalDetectionSettingsFromOpenDatasetPanel.class.getName());

    /**
     * Create with an analysis options and the detection options to copy to
     * @param parent
     * @param op
     */
    public CopySignalDetectionSettingsFromOpenDatasetPanel(HashOptions op) {
        super(null, op);
    }

    @Override
	protected ActionListener createCopyActionListener() {
		return (e) -> {
			
			final class Pair{
				IAnalysisDataset d;
				UUID signalGroupId;
				public Pair(IAnalysisDataset d, UUID signalGroupId) {
					this.d = d;
					this.signalGroupId = signalGroupId;
				}
				
				@Override
				public String toString() {
					return d.getName()+" - "+d.getCollection().getSignalGroup(signalGroupId).get().getGroupName();
				}
			}
			
			// Get a list of all datasets and signal groups
			Pair[] signalPairs = DatasetListManager.getInstance()
					.getRootDatasets().stream().flatMap(d-> {
						List<Pair> pair = new ArrayList<>();
						d.getCollection().getSignalGroupIDs().forEach(id->{
							pair.add(new Pair(d, id));
						});
						return pair.stream();
					}).toArray(Pair[]::new);
			
			
			Pair choice = (Pair) JOptionPane.showInputDialog(null,
                    CHOOSE_DATASET_MSG_LBL, CHOOSE_DATASET_TTL_LBL, JOptionPane.QUESTION_MESSAGE, null, signalPairs,
                    signalPairs[0]);

            if (choice != null) {

            	LOGGER.fine("Copying options from: " + choice);
            	File folder = new File(options.getString(HashOptions.DETECTION_FOLDER));
            	
            	if(choice.d.getAnalysisOptions().isPresent()) {
            		Optional<HashOptions> op = choice.d.getAnalysisOptions().get()
            				.getNuclearSignalOptions(choice.signalGroupId);
            		if(op.isPresent()) {
            			options.set(op.get());
            		}
            	}
            	// Ensure folder is not overwritten when other options were copied
				options.setString(HashOptions.DETECTION_FOLDER, folder.getAbsolutePath());
                fireOptionsChangeEvent();
            }
		};
	}

	@Override
	protected ActionListener createOpenActionListener() {
		return (e) -> {
			File folder = new File(options.getString(HashOptions.DETECTION_FOLDER));
			File f = FileSelector.chooseOptionsImportFile(folder);
			if(f==null)
				return;

			try {
				IAnalysisOptions o = XMLReader.readAnalysisOptions(f);
				
				// Find the channels in the imported file
				String[] choices = o.getNuclearSignalGroups()
						.stream()
						.map(id->o.getNuclearSignalOptions(id))
						.filter(Optional::isPresent)
						.map(Optional::get)
						.map(op->String.valueOf(op.getInt(HashOptions.CHANNEL)))
						.toArray(String[]::new);
				
				String choice = (String) JOptionPane.showInputDialog(null,
	                    "Choose the channel options to copy", "Choose channel", JOptionPane.QUESTION_MESSAGE, null, choices,
	                    choices[0]);
				
				if(choice==null)
					return;
				
				// Get the first option set matching the channel
				int channel = Integer.parseInt(choice.replaceAll("Channel ", ""));

				o.getNuclearSignalGroups().stream()
					.map(id->o.getNuclearSignalOptions(id))
					.filter(Optional::isPresent)
					.map(Optional::get)
					.filter(op->op.getInt(HashOptions.CHANNEL)==channel)
					.findFirst()
					.ifPresent(op->options.set(op));
				// Ensure folder is not overwritted when other options were copied
				options.setString(HashOptions.DETECTION_FOLDER, folder.getAbsolutePath());
				fireOptionsChangeEvent();

			} catch (XMLReadingException | ComponentCreationException e1) {
				LOGGER.log(Loggable.STACK, e1.getMessage(), e1);
			}
		};
	}

}
