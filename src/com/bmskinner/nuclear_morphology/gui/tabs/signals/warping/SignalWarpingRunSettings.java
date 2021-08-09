package com.bmskinner.nuclear_morphology.gui.tabs.signals.warping;

import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.options.AbstractHashOptions;

/**
 * Store settings for a signal warping
 * @author ben
 * @since 1.19.4
 *
 */
public class SignalWarpingRunSettings extends AbstractHashOptions {

	private static final long serialVersionUID = 1L;
	
	public static final String IS_ONLY_CELLS_WITH_SIGNALS_KEY = "Only cells with signals";
	public static final String IS_BINARISE_SIGNALS_KEY = "Binarise signals";
	public static final String MIN_THRESHOLD_KEY = "Min threshold";
	public static final String IS_NORMALISE_TO_COUNTERSTAIN_KEY = "Normalise to counterstain";
	
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
	
	public IAnalysisDataset templateDataset() {
		return d1;
	}

	public IAnalysisDataset targetDataset() {
		return d2;
	}
	
	public CellularComponent targetShape() {
		return d2.getCollection().getConsensus();
	}
	
	public UUID signalId() {
		return signalId;
	}
	
	public ISignalGroup templateSignalGroup() {
		return templateDataset().getCollection()
				.getSignalGroup(signalId).get();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((d1 == null) ? 0 : d1.hashCode());
		result = prime * result + ((d2 == null) ? 0 : d2.hashCode());
		result = prime * result + ((signalId == null) ? 0 : signalId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		SignalWarpingRunSettings other = (SignalWarpingRunSettings) obj;
		if (d1 == null) {
			if (other.d1 != null)
				return false;
		} else if (!d1.equals(other.d1))
			return false;
		if (d2 == null) {
			if (other.d2 != null)
				return false;
		} else if (!d2.equals(other.d2))
			return false;
		if (signalId == null) {
			if (other.signalId != null)
				return false;
		} else if (!signalId.equals(other.signalId))
			return false;
		return true;
	}
	
	
}
