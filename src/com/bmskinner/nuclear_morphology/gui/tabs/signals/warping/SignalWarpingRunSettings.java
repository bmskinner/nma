package com.bmskinner.nuclear_morphology.gui.tabs.signals.warping;

import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.AbstractHashOptions;

/**
 * Store settings for a signal warping
 * @author ben
 * @since 1.19.4
 *
 */
public class SignalWarpingRunSettings extends AbstractHashOptions {

	private static final long serialVersionUID = 1L;
	
	private final IAnalysisDataset d1;
	private final IAnalysisDataset d2;
	private final UUID signalId;

	/**
	 * Create settings object.
	 * @param d1 the source dataset for signals
	 * @param d2 the dataset with the target consensus shape
	 * @param signalId the signal group to be warped
	 */
	public SignalWarpingRunSettings(@NonNull IAnalysisDataset d1, 
			@NonNull IAnalysisDataset d2, 
			@NonNull UUID signalId) {
		super();
		this.d1=d1;
		this.d2=d2;
		this.signalId = signalId;
	}
	
	public IAnalysisDataset datasetOne() {
		return d1;
	}

	public IAnalysisDataset datasetTwo() {
		return d2;
	}
	
	public UUID signalId() {
		return signalId;
	}
}
