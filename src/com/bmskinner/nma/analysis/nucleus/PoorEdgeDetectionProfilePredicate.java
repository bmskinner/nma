package com.bmskinner.nma.analysis.nucleus;

import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.MissingOptionException;
import com.bmskinner.nma.components.profiles.IProfile;
import com.bmskinner.nma.components.profiles.MissingLandmarkException;
import com.bmskinner.nma.components.profiles.MissingProfileException;
import com.bmskinner.nma.components.profiles.ProfileException;
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
	 * Create from options. This options will usually be the 'other options' from a
	 * RuleSetCollection
	 * 
	 * @param options
	 * @throws MissingOptionException
	 */
	public PoorEdgeDetectionProfilePredicate(@NonNull HashOptions options)
			throws MissingOptionException {

		this.profileType = ProfileType
				.fromString(options.get(RuleSetCollection.RULESET_EDGE_FILTER_PROFILE));

		this.min = options.get(RuleSetCollection.RULESET_EDGE_FILTER_THRESHOLD_MIN);
		this.max = options.get(RuleSetCollection.RULESET_EDGE_FILTER_THRESHOLD_MAX);
		this.deltaMax = options.get(RuleSetCollection.RULESET_EDGE_FILTER_THRESHOLD_DELTA_MAX);

	}

	@Override
	public boolean test(ICell t) {

		boolean allPass = true;
		for (Nucleus n : t.getNuclei()) {
			try {

				// Check profile values are within tolerances
				IProfile p = n.getProfile(profileType);
				allPass &= p.getMin() >= min && p.getMax() <= max;
				if (!allPass)
					return false;

				// Calculate changes over a small window size
				// A poor edge detection can vary wildly
				IProfile deltas = p.calculateDerivative();
				allPass &= deltas.getMax() <= deltaMax && deltas.getMin() >= -deltaMax;

			} catch (MissingProfileException | MissingLandmarkException | ProfileException e) {
				LOGGER.log(Level.SEVERE, "Unable to get profile in nucleus", e);
				return false;
			}
		}

		return allPass;
	}

	@Override
	public String toString() {
		return "ProfileFilter [profileType=" + profileType + ", min=" + min + ", max=" + max
				+ "]";
	}

}
