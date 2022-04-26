/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nuclear_morphology.analysis.profiles;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
import com.bmskinner.nuclear_morphology.components.cells.Nucleus;
import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;
import com.bmskinner.nuclear_morphology.components.profiles.DefaultProfileAggregate;
import com.bmskinner.nuclear_morphology.components.profiles.IProfile;
import com.bmskinner.nuclear_morphology.components.profiles.MissingProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.components.rules.OrientationMark;
import com.bmskinner.nuclear_morphology.stats.Stats;

/**
 * A highly variable dataset may not produce a median profile that is cleanly
 * segmentable. This class finds the least variable subset of nuclei, and
 * generates a segmentable profile.
 * 
 * The assumption is that there is a population of profiles within the
 * collection that are more similar to each other than noisy profiles.
 * 
 * @author bms41
 * @since 1.14.0
 *
 */
public class RepresentativeMedianFinder {

	private static final Logger LOGGER = Logger.getLogger(RepresentativeMedianFinder.class.getName());

	private static final int SAMPLE_LIMIT = 2000;
	private final ICellCollection collection;
	private final List<Nucleus> nuclei;

	/**
	 * Construct from the nuclei in the given collection.
	 * 
	 * @param c
	 */
	public RepresentativeMedianFinder(@NonNull ICellCollection c) {
		collection = c;
		// We need consistent ordering, but a random sample of at least
		// 200 nuclei (if present) should be enough
//		nuclei = getSubList(new ArrayList<>(collection.getNuclei()));
		nuclei = conventionalSelectN(new ArrayList<>(collection.getNuclei()), SAMPLE_LIMIT);
	}

	/**
	 * Get a random subset of the collection's nuclei, to a maximum of
	 * {@code SAMPLE_LIMIT}
	 * 
	 * @param n
	 * @return
	 */
	private List<Nucleus> getSubList(List<Nucleus> n) {
		if (n.size() <= SAMPLE_LIMIT)
			return n;
		Collections.shuffle(n);
		return n.subList(0, 200);
	}

	/**
	 * Get a random sample from a collection of the given size. Uses Knuth's
	 * Algorithm S
	 * 
	 * @see https://stackoverflow.com/questions/28651908/perform-operation-on-n-random-distinct-elements-from-collection-using-streams-ap/28655112#28655112
	 * @param <E>
	 * @param coll
	 * @param remain
	 * @return
	 */
	private static <E> List<E> conventionalSelectN(Collection<? extends E> coll, int remain) {
		int total = coll.size();
		List<E> result = new ArrayList<>(remain);
		Random random = new Random();

		for (E e : coll) {
			if (random.nextInt(total--) < remain) {
				remain--;
				result.add(e);
			}
		}

		return result;
	}

	/**
	 * Find the median that describes the largest subset of the dataset
	 * 
	 * @return
	 * @throws MissingLandmarkException
	 * @throws MissingProfileException
	 * @throws ProfileException
	 */
	public IProfile findMedian() throws MissingLandmarkException, MissingProfileException, ProfileException {
		// Get normalised pairwise differences between nuclei profiles
		float[][] differences = buildDifferenceMatrix();

		// Calculate the standard deviation of each column
		float[] deviations = calculateDistanceColumnDeviation(differences);

		// The column with the lowest stdev has the largest number of similar nuclei
		int index = findIndexOfLowestValue(deviations);

		// Get this best profile
		IProfile bestProfile = nuclei.get(index).getUnsegmentedProfile(ProfileType.ANGLE, OrientationMark.REFERENCE);

		// Find the other nuclei in the collection that are similar to this one
		List<IProfile> profiles = findBestProfiles(bestProfile);

		// Construct the representative median from the selected subset of similar
		// nuclei
		return buildMedianFromProfiles(profiles, collection.getMedianArrayLength());
	}

	/**
	 * Find the profiles in the collection that have a below median difference to
	 * the target profile
	 * 
	 * @param target
	 * @return
	 * @throws MissingLandmarkException
	 * @throws MissingProfileException
	 * @throws ProfileException
	 */
	private List<IProfile> findBestProfiles(@NonNull IProfile target)
			throws MissingLandmarkException, MissingProfileException, ProfileException {
		float[] differences = calculateDistancesToTemplate(target);
		float medianDiff = Stats.quartile(differences, Stats.MEDIAN);
		List<IProfile> result = new ArrayList<>();

		if (nuclei.size() <= 2 || medianDiff == 0) { // too few profiles or all identical
			for (Nucleus n : nuclei)
				result.add(n.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE));
			return result;
		}

		for (int i = 0; i < differences.length; i++) {
			if (differences[i] < medianDiff)
				result.add(nuclei.get(i).getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE));
		}
		return result;
	}

	private IProfile buildMedianFromProfiles(@NonNull final List<IProfile> profiles, int length)
			throws MissingLandmarkException, MissingProfileException, ProfileException {
		DefaultProfileAggregate agg = new DefaultProfileAggregate(length, profiles.size());
		for (IProfile p : profiles)
			agg.addValues(p);
		return agg.getMedian();

	}

	private int findIndexOfLowestValue(float[] array) {
		float lowest = Float.MAX_VALUE;
		int index = 0;
		for (int i = 0; i < array.length; i++) {
			if (array[i] < lowest) {
				lowest = array[i];
				index = i;
			}
		}
		return index;
	}

	private float[] calculateDistanceColumnDeviation(float[][] matrix) {
		float[] result = new float[matrix[0].length];
		for (int i = 0; i < matrix[0].length; i++) {
			result[i] = (float) Stats.stdev(matrix[i]);
		}
		return result;
	}

	private float[] calculateDistancesToTemplate(IProfile template)
			throws MissingLandmarkException, MissingProfileException, ProfileException {
		float[] result = new float[nuclei.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = (float) nuclei.get(i).getUnsegmentedProfile(ProfileType.ANGLE, OrientationMark.REFERENCE)
					.absoluteSquareDifference(template);
		}
		return result;
	}

	/**
	 * Create a matrix containing the pairwise differences between nuclear profiles.
	 * 
	 * @return a matrix in which each nucleus profile is compared to every other
	 *         nucleus profile
	 * @throws MissingLandmarkException
	 * @throws MissingProfileException
	 * @throws ProfileException
	 */
	private float[][] buildDifferenceMatrix()
			throws MissingLandmarkException, MissingProfileException, ProfileException {
		float[][] matrix = new float[nuclei.size()][nuclei.size()];

//		for (int i = 0; i < nuclei.size(); i++) {
//			IProfile pI = nuclei.get(i).getUnsegmentedProfile(ProfileType.ANGLE, OrientationMark.REFERENCE);
//			for (int j = 0; j < nuclei.size(); j++) {
//				matrix[i][j] = (float) pI.absoluteSquareDifference(
//						nuclei.get(j).getUnsegmentedProfile(ProfileType.ANGLE, OrientationMark.REFERENCE));
//			}
//		}

		// Handle the two diagonals of the matrix simultaneously
		for (int i = 0; i < nuclei.size(); i++) {
			IProfile pI = nuclei.get(i).getUnsegmentedProfile(ProfileType.ANGLE, OrientationMark.REFERENCE);
			for (int j = 0; j < nuclei.size(); j++) {
				if (j <= i) {
					matrix[i][j] = matrix[j][i];
				} else {
					float v = (float) pI.absoluteSquareDifference(
							nuclei.get(j).getUnsegmentedProfile(ProfileType.ANGLE, OrientationMark.REFERENCE));
					matrix[i][j] = v;
				}
			}
		}
		return matrix;
	}
}
