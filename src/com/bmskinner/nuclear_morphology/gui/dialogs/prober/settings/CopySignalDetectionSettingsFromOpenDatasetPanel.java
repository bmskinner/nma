package com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.swing.JOptionPane;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.core.DatasetListManager;
import com.bmskinner.nuclear_morphology.gui.components.FileSelector;
import com.bmskinner.nuclear_morphology.io.xml.OptionsXMLReader;
import com.bmskinner.nuclear_morphology.io.xml.XMLReader.XMLReadingException;

/**
 * Button that allows nuclear signal detection options to be copied from an open
 * dataset or a saved options file
 * 
 * @author ben
 * @since 1.15.0
 *
 */
public class CopySignalDetectionSettingsFromOpenDatasetPanel extends CopyFromOpenDatasetPanel {

    /**
     * Create with an analysis options and the detection options to copy to
     * @param parent
     * @param op
     */
    public CopySignalDetectionSettingsFromOpenDatasetPanel(IDetectionOptions op) {
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

            	fine("Copying options from: " + choice);
//            	// Ensure the folder is not overwritten by the new options
            	File folder = options.getFolder();
            	IDetectionOptions template = choice.d.getAnalysisOptions().get().getNuclearSignalOptions(choice.signalGroupId);
            	options.set(template);
            	options.setFolder(folder);
                fireOptionsChangeEvent();
            }
		};
	}

	@Override
	protected ActionListener createOpenActionListener() {
		return (e) -> {
//			File folder = options.getFolder();
//			File f = FileSelector.chooseOptionsImportFile(folder);
//			if(f==null)
//				return;
//
//			try {
//				IAnalysisOptions o = new OptionsXMLReader(f).read();
//				options.set(o.getDetectionOptions(IAnalysisOptions.SIGNAL_GROUP).get());
//				parent.setNucleusType(o.getNucleusType());
//				parent.setAngleWindowProportion(o.getProfileWindowProportion());
//				options.setFolder(folder);
//				fireOptionsChangeEvent();
//
//			} catch (XMLReadingException e1) {
//				stack(e1);
//			}
		};
	}

}
