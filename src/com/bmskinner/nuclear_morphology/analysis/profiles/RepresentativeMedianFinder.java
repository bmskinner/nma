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
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.profiles.DefaultProfileAggregate;
import com.bmskinner.nuclear_morphology.components.profiles.IProfile;
import com.bmskinner.nuclear_morphology.components.profiles.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.MissingProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.Stats;

/**
 * A highly variable dataset may not produce a median profile
 * that is cleanly segmentable. This class finds the least 
 * variable subset of nuclei, and generates a segmentable
 * profile.
 * 
 * The assumption is that there is a population of profiles within
 * the collection that are more similar to each other than noisy
 * profiles.
 * @author bms41
 * @since 1.14.0
 *
 */
public class RepresentativeMedianFinder {
	
	private static final Logger LOGGER = Logger.getLogger(RepresentativeMedianFinder.class.getName());
	
	private final ICellCollection collection;
	private final List<Nucleus> nuclei;
	
	/**
	 * Construct from the nuclei in the given collection.
	 * @param c
	 */
	public RepresentativeMedianFinder(@NonNull ICellCollection c) {
		collection = c;
		nuclei = new ArrayList<>(collection.getNuclei());
	}
	
	/**
	 * Find the median that describes the largest subset of the dataset
	 * @return
	 * @throws MissingLandmarkException
	 * @throws MissingProfileException
	 * @throws ProfileException
	 */
	public IProfile findMedian() throws MissingLandmarkException, MissingProfileException, ProfileException {
    	LOGGER.fine("Beginning median finding");
    	
    	// Get normalised pairwise differences between nuclei profiles
		float[][] differences  = buildDifferenceMatrix();
		float[][] similarities = buildSimilarityMatrix(differences);
        
        // Calculate the standard deviation of each column
        float[] deviations = calculateDistanceColumnDeviation(differences);
        
        // The column with the lowest stdev has the largest number of similar nuclei
        int index = findIndexOfLowestValue(deviations);
        
        // Get this best profile
        ISegmentedProfile bestProfile = nuclei.get(index).getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT);
        
        // Find the other nuclei in the collection that are similar to this one
        List<IProfile> profiles = findBestProfiles(bestProfile);

        // Construct the representative median from the selected subset of similar nuclei
        return buildMedianFromProfiles(profiles, collection.getMedianArrayLength());
	}
	
	/**
	 * Find the median from the subset of the dataset with greatest similarity
	 * to the existing collection median
	 * @return
	 * @throws MissingLandmarkException
	 * @throws MissingProfileException
	 * @throws ProfileException
	 */
	public IProfile findCollectionMedian() throws MissingLandmarkException, MissingProfileException, ProfileException {

		try {
			
			IProfile template = collection.getProfileCollection().
					getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT, Stats.MEDIAN);
			
			float[] differences = calculateDistancesToTemplate(template);
									
			// The column with the lowest stdev has the largest number of similar nuclei
			int index = findIndexOfLowestValue(differences);
			float lowest = differences[index];
			LOGGER.finer( "Lowest difference index is "+index+" with value "+lowest);
			
			// Get this best profile
			IProfile bestProfile = nuclei.get(index).getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT);		
			
			List<IProfile> profiles = findBestProfiles(bestProfile);
			
			// We can't build a median out of zero profiles
			
			// Subset the collection to nuclei with a difference lower than the lower quartile of the variability 			
			// Return the median profile of this subset
			return buildMedianFromProfiles(profiles, collection.getMedianArrayLength());
			
		} catch (MissingLandmarkException | MissingProfileException | ProfileException e) {
			LOGGER.log(Loggable.STACK, "Error creating matrix, returning default median", e);
			return collection.getProfileCollection().getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT, Stats.MEDIAN);
		}
	}
		
	/**
	 * Find the profiles in the collection that have a below median difference to the target profile
	 * @param target
	 * @return
	 * @throws MissingLandmarkException
	 * @throws MissingProfileException
	 * @throws ProfileException
	 */
	public List<IProfile> findBestProfiles(@NonNull IProfile target) throws MissingLandmarkException, MissingProfileException, ProfileException{
		float[] differences = calculateDistancesToTemplate(target);
		float medianDiff = Stats.quartile(differences, Stats.MEDIAN);
		List<IProfile> result = new ArrayList<>();

		if(nuclei.size()<=2 || medianDiff==0) { // too few profiles or all identical
			for(Nucleus n : nuclei)  
				result.add(n.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT));
			return result;
		}

		for(int i=0; i<differences.length; i++) {
			if(differences[i]<medianDiff)
				result.add(nuclei.get(i).getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT));
		}
		return result;
	}
	
	private IProfile buildMedianFromProfiles(@NonNull final List<IProfile> profiles, int length) throws MissingLandmarkException, MissingProfileException, ProfileException {
		LOGGER.fine("Group size for new median is "+profiles.size());
		LOGGER.fine("Building aggregate of length "+length);
		DefaultProfileAggregate agg = new DefaultProfileAggregate(length, profiles.size());
		for(IProfile p : profiles)
				agg.addValues(p);
		return agg.getMedian();
		
	}
	
	private int findIndexOfLowestValue(float[] array) {
		float lowest = Float.MAX_VALUE;
		int index = 0;
		for(int i=0; i<array.length; i++) {
			if(array[i]<lowest) {
				lowest = array[i];
				index = i;
			}
		}
		return index;
	}
	
	private float[] calculateDistanceColumnDeviation(float[][] matrix) {
		float[] result = new float[matrix[0].length];
		for(int i=0; i<matrix[0].length; i++) {
			result[i] = (float)Stats.stdev(matrix[i]);
		}
		return result;
	}
	
	
	
	private float[] calculateDistancesToTemplate(IProfile template) throws MissingLandmarkException, MissingProfileException, ProfileException {		
		float[] result = new float[collection.getNucleusCount()];
		for(int i=0; i<result.length; i++) {
			result[i] = (float) nuclei.get(i).getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT).absoluteSquareDifference(template);
		}
		return result;
	}
	
	/**
	 * Create a matrix containing the pairwise differences between nuclear profiles.
	 * @return a matrix in which each nucleus profile is compared to every other nucleus profile
	 * @throws MissingLandmarkException
	 * @throws MissingProfileException
	 * @throws ProfileException
	 */
	private float[][] buildDifferenceMatrix() throws MissingLandmarkException, MissingProfileException, ProfileException{
		float[][] matrix = new float[collection.getNucleusCount()][collection.getNucleusCount()];
		
		for(int i=0; i<nuclei.size(); i++) {
			IProfile pI = nuclei.get(i).getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT);
			for(int j=0; j<nuclei.size(); j++) {
				matrix[i][j] = (float) pI.absoluteSquareDifference(nuclei.get(j).getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT));
			}
		}
		return matrix;
	}
	
	/**
	 * Calculate the similarity between the first nucleus of a set and every other nucleus. A normalised
	 * form of the input matrix.
	 * 
	 * Input [0, 4, 5] becomes: [0, 0, 0]
	 * 		 [4, 0, 3]          [-4, 4, 2]
	 *       [5, 3, 0]          [-5, 1, 5]
	 * @param matrix the matrix of pairwise differences between nuclear profiles
	 * @return a matrix showing the pairwise differences normalised to each nucleus in turn 
	 * @throws MissingLandmarkException
	 * @throws MissingProfileException
	 * @throws ProfileException
	 */
	private float[][] buildSimilarityMatrix(float[][] matrix) throws MissingLandmarkException, MissingProfileException, ProfileException{
		float[][] dist = new float[matrix[0].length][matrix[0].length];
		for(int i=0; i<matrix[0].length; i++) {
			for(int j=0; j<matrix[0].length; j++) {
				dist[i][j] = matrix[0][j] - matrix[i][j];
			}
		}		
		return dist;
	}

}
