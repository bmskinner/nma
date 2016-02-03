package analysis;

import java.util.List;

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
	 * build the median profiles for all profile types.
	 * @param template
	 * @param destination
	 * @throws Exception 
	 */
	public static void copyCollectionOffsets(final CellCollection template, final CellCollection destination) throws Exception{

		//		ProfileCollection pc = template.getProfileCollection(ProfileType.REGULAR);
		List<NucleusBorderSegment> segments = template.getProfileCollection(ProfileType.REGULAR)
				.getSegments(BorderTag.REFERENCE_POINT);



		for(ProfileType type : ProfileType.values()){

			ProfileCollection newPC = destination.getProfileCollection(type);
			ProfileCollection oldPC =    template.getProfileCollection(type);

			newPC.createProfileAggregate(template, 
					type, 
					(int) template.getMedianArrayLength());

			for(BorderTag key : oldPC.getOffsetKeys()){
				newPC.addOffset(key, oldPC.getOffset(key));
			}
			newPC.addSegments(BorderTag.REFERENCE_POINT, segments);

		}
	}
	
}
