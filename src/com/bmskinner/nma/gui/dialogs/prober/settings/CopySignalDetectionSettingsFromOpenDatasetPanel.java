package com.bmskinner.nma.gui.dialogs.prober.settings;

import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.components.signals.MissingSignalGroupException;
import com.bmskinner.nma.core.DatasetListManager;
import com.bmskinner.nma.gui.components.FileSelector;
import com.bmskinner.nma.io.XMLReader;
import com.bmskinner.nma.io.XMLReader.XMLReadingException;
import com.bmskinner.nma.logging.Loggable;

/**
 * Button that allows nuclear signal detection options to be copied from an open
 * dataset or a saved options file
 * 
 * @author Ben Skinner
 * @since 1.15.0
 *
 */
public class CopySignalDetectionSettingsFromOpenDatasetPanel extends CopyFromOpenDatasetPanel {

	private static final Logger LOGGER = Logger
			.getLogger(CopySignalDetectionSettingsFromOpenDatasetPanel.class.getName());

	private final File folder;

	/**
	 * Create with an analysis options and the detection options to copy to
	 * 
	 * @param parent
	 * @param op
	 */
	public CopySignalDetectionSettingsFromOpenDatasetPanel(@NonNull File folder,
			@NonNull IAnalysisOptions parent,
			@NonNull HashOptions op) {
		super(parent, op);
		this.folder = folder;
	}

	@Override
	protected ActionListener createCopyActionListener() {
		return e -> {

			final record Pair(IAnalysisDataset d, UUID signalGroupId) {
				@Override
				public String toString() {
					try {
						return "%s - %s".formatted(d.getName(),
								d.getCollection().getSignalGroup(signalGroupId)
										.orElseThrow(MissingSignalGroupException::new)
										.getGroupName());
					} catch (MissingSignalGroupException e) {
						LOGGER.log(Loggable.STACK,
								"No signal group with id %s".formatted(signalGroupId), e);
						return "%s - No signal id".formatted(d.getName());
					}
				}
			}

			// Get a list of all datasets and signal groups
			Pair[] signalPairs = DatasetListManager.getInstance()
					.getRootDatasets().stream().flatMap(d -> {
						List<Pair> pair = new ArrayList<>();
						d.getCollection().getSignalGroupIDs().forEach(id -> {
							pair.add(new Pair(d, id));
						});
						return pair.stream();
					}).toArray(Pair[]::new);

			Pair choice = (Pair) JOptionPane.showInputDialog(null,
					CHOOSE_DATASET_MSG_LBL, CHOOSE_DATASET_TTL_LBL, JOptionPane.QUESTION_MESSAGE,
					null, signalPairs,
					signalPairs[0]);

			if (choice != null) {

				LOGGER.fine(() -> "Copying options from: %s".formatted(choice));

				if (choice.d.getAnalysisOptions().isPresent()) {
					Optional<HashOptions> op = choice.d.getAnalysisOptions().get()
							.getNuclearSignalOptions(choice.signalGroupId());
					if (op.isPresent()) {
						options.set(op.get());
					}
				}

				// Ensure folder is not overwritten when other options were copied
				parent.setNuclearSignalDetectionFolder(choice.signalGroupId(),
						folder.getAbsoluteFile());

				fireOptionsChangeEvent();
			}
		};
	}

	@Override
	protected ActionListener createOpenActionListener() {
		return e -> {
			File detectionFolder = new File(options.getString(HashOptions.DETECTION_FOLDER));
			File f = FileSelector.chooseOptionsImportFile(detectionFolder);
			if (f == null)
				return;

			try {
				IAnalysisOptions o = XMLReader.readAnalysisOptions(f);

				// Find the channels in the imported file
				String[] choices = o.getNuclearSignalGroups()
						.stream()
						.map(id -> o.getNuclearSignalOptions(id))
						.filter(Optional::isPresent)
						.map(Optional::get)
						.map(op -> String.valueOf(op.getInt(HashOptions.CHANNEL)))
						.toArray(String[]::new);

				String choice = (String) JOptionPane.showInputDialog(null,
						"Choose the channel options to copy", "Choose channel",
						JOptionPane.QUESTION_MESSAGE, null, choices,
						choices[0]);

				if (choice == null)
					return;

				// Get the first option set matching the channel
				int channel = Integer.parseInt(choice.replace("Channel ", ""));

				o.getNuclearSignalGroups().stream()
						.map(o::getNuclearSignalOptions)
						.filter(Optional::isPresent)
						.map(Optional::get)
						.filter(op -> op.getInt(HashOptions.CHANNEL) == channel)
						.findFirst()
						.ifPresent(op -> options.set(op));

				fireOptionsChangeEvent();

			} catch (XMLReadingException | ComponentCreationException e1) {
				LOGGER.log(Loggable.STACK, e1.getMessage(), e1);
			}
		};
	}

}
