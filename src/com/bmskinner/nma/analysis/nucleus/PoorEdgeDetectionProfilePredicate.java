package com.bmskinner.nma.analysis.nucleus;

import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.MissingOptionException;
import com.bmskinner.nma.components.profiles.IProfile;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.RuleSetCollection;

/**
 * Test a profile of the nuclei in a cell for proper edge detection. Extreme
 * profile values or rapid changes in value are signs of poor edge detection.
 * 
 * @author bs19022
 * @since 2.2.0
 *
 */
public class PoorEdgeDetectionProfilePredicate implements Predicate<ICell> {

	private static final Logger LOGGER = Logger
			.getLogger(PoorEdgeDetectionProfilePredicate.class.getName());

	private ProfileType profileType;
	private float min;
	private float max;
	private float deltaMax;

	/**
	 * If we can't create the filter, pass everything
	 */
	private boolean passAll = false;

	/**
	 * Create from options. This options will usually be the 'other options' from a
	 * RuleSetCollection
	 * 
	 * @param options
	 * @throws MissingOptionException
	 */
	public PoorEdgeDetectionProfilePredicate(@NonNull HashOptions options) {

		try {

			this.profileType = ProfileType
					.fromString(options.get(RuleSetCollection.RULESET_EDGE_FILTER_PROFILE));

			this.min = options.get(RuleSetCollection.RULESET_EDGE_FILTER_THRESHOLD_MIN);
			this.max = options.get(RuleSetCollection.RULESET_EDGE_FILTER_THRESHOLD_MAX);
			this.deltaMax = options.get(RuleSetCollection.RULESET_EDGE_FILTER_THRESHOLD_DELTA_MAX);
		} catch (MissingOptionException e) {
			LOGGER.log(Level.WARNING, "No rulesets with edge filtering, skipping");
			passAll = true;
		}

	}

	@Override
	public boolean test(ICell t) {
		if (passAll)
			return true;

		boolean cellPasses = true;
		for (Nucleus n : t.getNuclei()) {
			try {

				// Check profile values are within tolerances
				IProfile p = n.getProfile(profileType);
				cellPasses &= p.getMin() >= min && p.getMax() <= max;
				if (!cellPasses)
					return false;

				// Calculate first derivative and take absolute value
				// Poor edge detection should show as rapid changes in angle
				IProfile deltas = p.calculateDerivative().absolute();
				cellPasses &= deltas.getMax() <= deltaMax;

			} catch (MissingDataException | SegmentUpdateException e) {
				LOGGER.log(Level.SEVERE, "Unable to get profile in nucleus", e);
				return false;
			}
		}

		return cellPasses;
	}

	@Override
	public String toString() {
		return "ProfileFilter [profileType=" + profileType + ", min=" + min + ", max=" + max
				+ "]";
	}

}
