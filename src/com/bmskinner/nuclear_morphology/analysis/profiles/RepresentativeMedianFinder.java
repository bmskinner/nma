package com.bmskinner.nuclear_morphology.analysis.profiles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.DefaultProfileAggregate;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.Stats;

/**
 * A highly variable dataset may not produce a median profile
 * that is cleanly segmentable. This class finds the least 
 * variable subset of nuclei, and generates a segmentable
 * profile
 * @author bms41
 * @since 1.14.0
 *
 */
public class RepresentativeMedianFinder implements Loggable {
	
	private final ICellCollection collection;
	private final List<Nucleus> nuclei;
	
	public RepresentativeMedianFinder(@NonNull ICellCollection c) {
		collection = c;
		nuclei = new ArrayList<>(collection.getNuclei());
	}
	
	/**
	 * Find the median that describes the largest subset of the dataset
	 * @return
	 * @throws UnavailableBorderTagException
	 * @throws UnavailableProfileTypeException
	 * @throws ProfileException
	 */
	public IProfile findMedian() throws UnavailableBorderTagException, UnavailableProfileTypeException, ProfileException {
		fine("-------------------------");
    	fine("Beginning median finding");
    	fine("-------------------------");
		
		try {
			// Find the pairwise differences between all nucleus profiles
			float[][] matrix = buildDifferenceMatrix();
			
			// Convert to a distance matrix of all profiles to the first of each column
			float[][] dist   = buildDistanceMatrix(matrix);
			
			// Calculate the median of each column
			float[] medians = calculateDistanceColumnMedian(dist);
			
			// The column with the lowest median has the largest number of similar nuclei
			int index = findIndexOfLowestValue(medians);
			
			// Get this best profile
			ISegmentedProfile bestProfile = nuclei.get(index).getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
			
			// Subset the collection to nuclei with a difference lower than that median 
			float median = medians[index];
			fine("Identified best profile to base median on");
			
			// Return the median profile of this subset
			return buildProfileFromValuesBelowThreshold(medians, median, bestProfile.size());
			
		} catch (UnavailableBorderTagException | UnavailableProfileTypeException | ProfileException e) {
			error("Error creating matrix, returning default median", e);
			return collection.getProfileCollection().getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);
		}
	}
	
	private IProfile buildProfileFromValuesBelowThreshold(float[] array, float threshold, int length) throws UnavailableBorderTagException, UnavailableProfileTypeException, ProfileException {

		int expectedProfiles =0;
		for(int i=0; i<array.length; i++) {
			if(array[i]<=threshold)
				expectedProfiles++;
		}		
		DefaultProfileAggregate agg = new DefaultProfileAggregate(collection.getMedianArrayLength(), expectedProfiles);
		
		for(int i=0; i<array.length; i++) {
			if(array[i]<=threshold) {
				agg.addValues(nuclei.get(i).getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT));
			}
		}
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
	
	private float[] calculateDistanceColumnMedian(float[][] matrix) {
		float[] result = new float[matrix[0].length];
		for(int i=0; i<matrix[0].length; i++) {
			result[i] = Stats.quartile(matrix[i], Stats.MEDIAN);
		}
		return result;
	}
	
	private float[][] buildDifferenceMatrix() throws UnavailableBorderTagException, UnavailableProfileTypeException, ProfileException{
		float[][] matrix = new float[collection.getNucleusCount()][collection.getNucleusCount()];
		
		for(int i=0; i<nuclei.size(); i++) {
			for(int j=0; j<nuclei.size(); j++) {
				matrix[i][j] = (float) nuclei.get(i).getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT)
						.absoluteSquareDifference(nuclei.get(j).getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT));
			}
		}
		return matrix;
	}
	
	private float[][] buildDistanceMatrix(float[][] matrix) throws UnavailableBorderTagException, UnavailableProfileTypeException, ProfileException{
		float[][] dist = new float[matrix[0].length][matrix[0].length];
		for(int i=0; i<matrix[0].length; i++) {
			for(int j=0; j<matrix[0].length; j++) {
				dist[i][j] = matrix[i][0] - matrix[i][j];
			}
		}		
		return dist;
	}

}
