package analysis;

import java.util.List;

import utility.Constants;
import components.CellCollection;
import components.generic.BorderTag;
import components.generic.ProfileCollection;
import components.generic.ProfileType;
import components.nuclear.NucleusBorderSegment;

/**
 * This class is designed to simplify operations on CellCollections
 * involving copying and refreshing of ProfileCollections and 
 * ProfileAggregates
 * @author bms41
 *
 */
public class ProfileManager {

	/**
	 * Copy profile offsets from the template collection, and 
	 * build the median profiles for all profile types. Also
	 * copy the segments from the regular angle profile onto
	 * all other profile types
	 * @param template
	 * @param destination
	 * @throws Exception 
	 */
	public static void copyCollectionOffsets(final CellCollection template, final CellCollection destination) throws Exception{
		
		List<NucleusBorderSegment> segments = template.getProfileCollection(ProfileType.REGULAR)
				.getSegments(BorderTag.REFERENCE_POINT);


		// use the same array length as the source collection to avoid segment slippage
		int profileLength = template.getProfileCollection(ProfileType.REGULAR)
				.getProfile(BorderTag.REFERENCE_POINT, Constants.MEDIAN) 
				.size(); 

		for(ProfileType type : ProfileType.values()){
			
			
			/*
			 * Get the empty profile collection from the new CellCollection
			 */
			ProfileCollection newPC = destination.getProfileCollection(type);
			
			/*
			 * Get the corresponding profile collection from the tempalte
			 */
			ProfileCollection oldPC =    template.getProfileCollection(type);
			
			/*
			 * Create an aggregate from the nuclei in the collection. 
			 * A new median profile will result.
			 * By default, the aggregates are created from the reference point
			 */
			newPC.createProfileAggregate(template, 
					type, 
					profileLength);
			
			/*
			 * Copy the offset keys from the source collection
			 */

			for(BorderTag key : oldPC.getOffsetKeys()){
				newPC.addOffset(key, oldPC.getOffset(key));
			}
			newPC.addSegments(BorderTag.REFERENCE_POINT, segments);

		}
	}
	
}
