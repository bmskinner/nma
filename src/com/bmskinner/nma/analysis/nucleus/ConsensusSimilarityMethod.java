package com.bmskinner.nma.analysis.nucleus;

import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.DefaultAnalysisResult;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nma.components.MissingComponentException;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.Consensus;
import com.bmskinner.nma.components.cells.DefaultConsensusNucleus;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.generic.FloatPoint;
import com.bmskinner.nma.components.options.MissingOptionException;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.components.profiles.UnprofilableObjectException;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.gui.events.UIController;

/**
 * Choose the nucleus most similar to a dataset median profile as the consensus
 * 
 * @author bs19022
 *
 */
public class ConsensusSimilarityMethod extends SingleDatasetAnalysisMethod {

	private static final Logger LOGGER = Logger.getLogger(ConsensusAveragingMethod.class.getName());

	private static final String EMPTY_FILE = "Empty";

	/** This length was chosen to avoid issues copying segments */
	private static final double PROFILE_LENGTH = 1000d;

	public ConsensusSimilarityMethod(@NonNull final IAnalysisDataset dataset) {
		super(dataset);
	}

	@Override
	public IAnalysisResult call() throws Exception {
		run();
		return new DefaultAnalysisResult(dataset);
	}

	private void run() throws MissingComponentException, UnprofilableObjectException,
			ComponentCreationException,
			ProfileException, MissingOptionException, SegmentUpdateException {
		LOGGER.finer("Running consensus similarity on " + dataset.getName());

		Nucleus template = dataset.getCollection()
				.getNucleusMostSimilarToMedian(OrientationMark.REFERENCE);

		// Build a consensus nucleus from the template
		Consensus cons = new DefaultConsensusNucleus(template);
		cons.getSignalCollection().removeSignals();
		cons.moveCentreOfMass(new FloatPoint(0, 0));

		// Calculate any other stats that need the vertical alignment
		cons.getOrientedNucleus();

		dataset.getCollection().setConsensus(cons);
		fireProgressEvent();

		UIController.getInstance().fireConsensusNucleusChanged(dataset);
	}

}
