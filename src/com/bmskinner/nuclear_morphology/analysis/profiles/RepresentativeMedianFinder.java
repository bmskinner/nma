package com.bmskinner.nuclear_morphology.analysis.profiles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
 * profile.
 * 
 * The assumption is that there is a population of profiles within
 * the collection that are more similar to each other than noisy
 * profiles.
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
		float[][] differences  = buildDifferenceMatrix();
		float[][] similarities = buildSimilarityMatrix(differences);
        
        // Calculate the median of each column
        float[] deviations = calculateDistanceColumnDeviation(differences);
        
        // The column with the lowest median has the largest number of similar nuclei
        int index = findIndexOfLowestValue(deviations);
        
        // Get this best profile
        ISegmentedProfile bestProfile = nuclei.get(index).getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
        List<IProfile> profiles = findBestProfiles(bestProfile);

        return buildMedianFromProfiles(profiles, collection.getMedianArrayLength());
	}
	
	/**
	 * Find the median from the subset of the dataset with greatest similarity
	 * to the existing collection median
	 * @return
	 * @throws UnavailableBorderTagException
	 * @throws UnavailableProfileTypeException
	 * @throws ProfileException
	 */
	public IProfile findCollectionMedian() throws UnavailableBorderTagException, UnavailableProfileTypeException, ProfileException {

		try {
			
			IProfile template = collection.getProfileCollection().
					getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);
			
			float[] differences = calculateDistancesToTemplate(template);
			
			float medianDiff = Stats.quartile(differences, Stats.MEDIAN);
						
			// The column with the lowest stdev has the largest number of similar nuclei
			int index = findIndexOfLowestValue(differences);
			float lowest = differences[index];
			fine("Lowest difference index is "+index+" with value "+lowest);
			
			// Get this best profile
			IProfile bestProfile = nuclei.get(index).getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);		
			
			List<IProfile> profiles = findBestProfiles(bestProfile);
			
			// We can't build a median out of zero profiles
			
			// Subset the collection to nuclei with a difference lower than the lower quartile of the variability 			
			// Return the median profile of this subset
			return buildMedianFromProfiles(profiles, collection.getMedianArrayLength());
			
		} catch (UnavailableBorderTagException | UnavailableProfileTypeException | ProfileException e) {
			error("Error creating matrix, returning default median", e);
			return collection.getProfileCollection().getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);
		}
	}
		
	/**
	 * Find the profiles in the collection that have a below median difference to the target profile
	 * @param target
	 * @return
	 * @throws UnavailableBorderTagException
	 * @throws UnavailableProfileTypeException
	 * @throws ProfileException
	 */
	public List<IProfile> findBestProfiles(@NonNull IProfile target) throws UnavailableBorderTagException, UnavailableProfileTypeException, ProfileException{
		float[] differences = calculateDistancesToTemplate(target);
		float medianDiff = Stats.quartile(differences, Stats.MEDIAN);
		List<IProfile> result = new ArrayList<>();

		if(nuclei.size()<=2 || medianDiff==0) { // too few profiles or all identical
			for(Nucleus n : nuclei)  
				result.add(n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT));
			return result;
		}

		for(int i=0; i<differences.length; i++) {
			if(differences[i]<medianDiff)
				result.add(nuclei.get(i).getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT));
		}
		return result;
	}
	
	private IProfile buildMedianFromProfiles(@NonNull final List<IProfile> profiles, int length) throws UnavailableBorderTagException, UnavailableProfileTypeException, ProfileException {
		fine("Group size for new median is "+profiles.size());
		fine("Building aggregate of length "+length);
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
	
	
	
	private float[] calculateDistancesToTemplate(IProfile template) throws UnavailableBorderTagException, UnavailableProfileTypeException, ProfileException {		
		float[] result = new float[collection.getNucleusCount()];
		for(int i=0; i<result.length; i++) {
			result[i] = (float) nuclei.get(i).getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT).absoluteSquareDifference(template);
		}
		return result;
	}
	
	/**
	 * Create a matrix containing the pairwise differences between nuclear profiles
	 * @return
	 * @throws UnavailableBorderTagException
	 * @throws UnavailableProfileTypeException
	 * @throws ProfileException
	 */
	private float[][] buildDifferenceMatrix() throws UnavailableBorderTagException, UnavailableProfileTypeException, ProfileException{
		float[][] matrix = new float[collection.getNucleusCount()][collection.getNucleusCount()];
		
		for(int i=0; i<nuclei.size(); i++) {
			IProfile pI = nuclei.get(i).getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
			for(int j=0; j<nuclei.size(); j++) {
				matrix[i][j] = (float) pI.absoluteSquareDifference(nuclei.get(j).getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT));
			}
		}
		return matrix;
	}
	
	private float[][] buildSimilarityMatrix(float[][] matrix) throws UnavailableBorderTagException, UnavailableProfileTypeException, ProfileException{
		float[][] dist = new float[matrix[0].length][matrix[0].length];
		for(int i=0; i<matrix[0].length; i++) {
			for(int j=0; j<matrix[0].length; j++) {
				dist[i][j] = matrix[0][j] - matrix[i][j];
			}
		}		
		return dist;
	}

}
