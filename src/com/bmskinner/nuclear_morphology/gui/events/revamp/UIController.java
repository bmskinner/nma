package com.bmskinner.nuclear_morphology.gui.events.revamp;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;

/**
 * Control dispatch of updates to UI panels
 * 
 * @author bs19022
 *
 */
public class UIController {

	private static final Logger LOGGER = Logger.getLogger(UIController.class.getName());

	private static final UIController instance = new UIController();

	private final List<ConsensusUpdatedListener> consensusListeners = new ArrayList<>();

	private final List<NuclearSignalUpdatedListener> nuclearSignalListeners = new ArrayList<>();

	private final List<ProfilesUpdatedListener> profilesListeners = new ArrayList<>();

	private final List<ScaleUpdatedListener> scaleListeners = new ArrayList<>();

	private final List<SwatchUpdatedListener> swatchListeners = new ArrayList<>();

	private final List<DatasetSelectionUpdatedListener> datasetSelectionListeners = new ArrayList<>();

	private final List<DatasetAddedListener> datasetAddedListeners = new ArrayList<>();

	private final List<FilePathUpdatedListener> filePathUpdatedListeners = new ArrayList<>();

	private UIController() {
	}

	public static UIController getInstance() {
		return instance;
	}

	public void addConsensusUpdatedListener(ConsensusUpdatedListener l) {
		consensusListeners.add(l);
	}

	public void fireConsensusNucleusChanged(@NonNull List<IAnalysisDataset> datasets) {
		for (ConsensusUpdatedListener l : consensusListeners)
			l.consensusUpdated(datasets);
	}

	public void fireConsensusNucleusChanged(@NonNull IAnalysisDataset d) {
		for (ConsensusUpdatedListener l : consensusListeners)
			l.consensusUpdated(d);
	}

	public void fireConsensusNucleusFillStateChanged() {
		for (ConsensusUpdatedListener l : consensusListeners)
			l.consensusFillStateUpdated();
	}

	public void addNuclearSignalUpdatedListener(NuclearSignalUpdatedListener l) {
		nuclearSignalListeners.add(l);
	}

	public void fireNuclearSignalUpdated(@NonNull List<IAnalysisDataset> datasets) {
		for (NuclearSignalUpdatedListener l : nuclearSignalListeners)
			l.nuclearSignalUpdated(datasets);
	}

	public void fireNuclearSignalUpdated(@NonNull IAnalysisDataset d) {
		for (NuclearSignalUpdatedListener l : nuclearSignalListeners)
			l.nuclearSignalUpdated(d);
	}

	public void addProfilesUpdatedListener(ProfilesUpdatedListener l) {
		profilesListeners.add(l);
	}

	public void fireProfilesUpdated(@NonNull List<IAnalysisDataset> datasets) {
		for (ProfilesUpdatedListener l : profilesListeners)
			l.profilesUpdated(datasets);
	}

	public void fireProfilesUpdated(@NonNull IAnalysisDataset d) {
		for (ProfilesUpdatedListener l : profilesListeners)
			l.profilesUpdated(d);
	}

	public void addScaleUpdatedListener(ScaleUpdatedListener l) {
		scaleListeners.add(l);
	}

	public void fireScaleUpdated(@NonNull List<IAnalysisDataset> datasets) {
		for (ScaleUpdatedListener l : scaleListeners)
			l.scaleUpdated(datasets);
	}

	public void fireScaleUpdated(@NonNull IAnalysisDataset d) {
		for (ScaleUpdatedListener l : scaleListeners)
			l.scaleUpdated(d);
	}

	public void fireScaleUpdated() {
		for (ScaleUpdatedListener l : scaleListeners)
			l.scaleUpdated();
	}

	public void addSwatchUpdatedListener(SwatchUpdatedListener l) {
		swatchListeners.add(l);
	}

	public void fireSwatchUpdated() {
		for (SwatchUpdatedListener l : swatchListeners)
			l.swatchUpdated();
	}

	public void addDatasetSelectionUpdatedListener(DatasetSelectionUpdatedListener l) {
		datasetSelectionListeners.add(l);
	}

	public void fireDatasetSelectionUpdated(@NonNull List<IAnalysisDataset> datasets) {
		for (DatasetSelectionUpdatedListener l : datasetSelectionListeners)
			l.datasetSelectionUpdated(datasets);
	}

	public void fireDatasetSelectionUpdated(@NonNull IAnalysisDataset d) {
		for (DatasetSelectionUpdatedListener l : datasetSelectionListeners)
			l.datasetSelectionUpdated(d);
	}

	public void addDatasetAddedListener(DatasetAddedListener l) {
		datasetAddedListeners.add(l);
	}

	public void fireDatasetAdded(@NonNull List<IAnalysisDataset> datasets) {
		for (DatasetAddedListener l : datasetAddedListeners)
			l.datasetAdded(datasets);
	}

	public void fireDatasetAdded(@NonNull IAnalysisDataset d) {
		for (DatasetAddedListener l : datasetAddedListeners)
			l.datasetAdded(d);
	}

	public void addFilePathUpdatedListener(FilePathUpdatedListener l) {
		filePathUpdatedListeners.add(l);
	}

	public void fireFilePathUpdated(@NonNull List<IAnalysisDataset> datasets) {
		for (FilePathUpdatedListener l : filePathUpdatedListeners)
			l.filePathUpdated(datasets);
	}

	public void fireFilePathUpdated(@NonNull IAnalysisDataset d) {
		for (FilePathUpdatedListener l : filePathUpdatedListeners)
			l.filePathUpdated(d);
	}

}
