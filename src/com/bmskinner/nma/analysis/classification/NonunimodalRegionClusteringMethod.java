package com.bmskinner.nma.analysis.classification;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.profiles.BooleanProfile;
import com.bmskinner.nma.components.profiles.IProfile;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.stats.ModalityTest;

/**
 * This class implements clustering on non-unimodal regions of profiles. This is
 * used for object barcoding and Hamming amalgamation.
 */
public class NonunimodalRegionClusteringMethod {

	private static final Logger LOGGER = Logger.getLogger(NonunimodalRegionClusteringMethod.class.getName());

	/**
	 * We identify all non-contiguous regions of non-unimodality in all profile
	 * types. Nuclei are assigned to a cluster for each region.
	 * 
	 * do we create all region clustering as real cluster groups in a dataset, or
	 * just transiently create them and assign the barcodes in this method?
	 * 
	 * Would we ever want to make the clusters without barcoding?
	 * 
	 * Data types needed: - in a dataset, the clustering overview: set of : a
	 * cluster id, profile, index range, and total number of clusters for that range
	 * - in a nucleus, the cluster memberships. cluster id and cluster number - or,
	 * a virtual dataset per group
	 * 
	 * Transient is far more efficient. Then store the overview of cluster regions,
	 * and the barcode
	 * 
	 */

	/**
	 * A region of a profile that forms part of a cell barcode. Tracks the location
	 * of the region in a profile, and the number of clusters identified at this
	 * region.
	 * 
	 * @param regionId   the id for this region
	 * @param type       the profile type that this region applies to
	 * @param startIndex the first index of the region (inclusive)
	 * @param endIndex   the last index of the region (inclusive, may wrap)
	 */
	public record ProfileBarcodingRegion(UUID regionId, ProfileType type, int startIndex, int endIndex) {

	}
	
	/**
	 * For a single cell, maps a barcoding region of a profile to the cluster
	 * identifier that cell belongs to.
	 * 
	 * @param regionId  a barcoding region
	 * @param clusterId the cluster that this cell belongs to. Cannot be greater
	 *                  than or equal to the total number of clusters at this
	 *                  region.
	 * 
	 */
	public record CellRegionCluster(ProfileBarcodingRegion regionId, int clusterId) {

	}

	/**
	 * A full barcode for a cell based on all clusterable regions in all profiles.
	 * 
	 * @param regions the individual regions and this cell's cluster membership
	 * 
	 */
	public record CellBarcode(List<CellRegionCluster> regions) {

	}

	/**
	 * Detect the profile regions that are not unimodal within a dataset.
	 * 
	 * @param dataset the dataset of cells to test
	 * @return
	 * @throws MissingDataException
	 * @throws SegmentUpdateException
	 */
	public Set<ProfileBarcodingRegion> findNonUnimodalProfileRegions(IAnalysisDataset dataset)
			throws SegmentUpdateException, MissingDataException {

		final Set<ProfileBarcodingRegion> result = new HashSet<>();

		for (final ProfileType pt : ProfileType.values()) {
			
			LOGGER.fine("Testing modality of %s at %s indexes in %s nuclei".formatted(pt,
					dataset.getCollection().getMedianArrayLength(), dataset.getCollection().size()));

			BooleanProfile multimodalIndexes = new BooleanProfile(dataset.getCollection().getMedianArrayLength(),
					false);

			for (int i = 0; i < dataset.getCollection().getMedianArrayLength(); i++) {
				
				double minValue = Double.MAX_VALUE;
				double maxValue = -Double.MAX_VALUE;
				
				final List<Nucleus> nuclei = dataset.getCollection().getNuclei();

				// Hold value at this index for all nuclei
				final double[] profileValues = new double[nuclei.size()];

				// Interpolate nucleus profile to median length, get the current index value
				for (int j = 0; j < nuclei.size(); j++) {
					final Nucleus n = nuclei.get(j);
					final IProfile p = n.getProfile(pt, OrientationMark.REFERENCE)
							.interpolate(dataset.getCollection().getMedianArrayLength());
					profileValues[j] = p.get(i);
					
					if(p.get(i)<minValue) {
						minValue = p.get(i);
					}
					if(p.get(i)>maxValue) {
						maxValue = p.get(i);
					}
				}
				
				
				// Perform modality test on profile values
//				final double minbinWidth = (maxValue - minValue) / 10;
//				final double maxbinWidth = (maxValue - minValue) / 5;
//				final double stepSize = (maxValue - minValue) / 20;

				final double minbinWidth = 5;
				final double maxbinWidth = 10;
				final double stepSize = 1;
				


				final ModalityTest mt = new ModalityTest(profileValues, minbinWidth, maxbinWidth, stepSize);
				final double mvalue = mt.getMValue();
				
				LOGGER.finer("Index %s: mValue %s, bins %s - %s, step %s".formatted(i, mvalue, minbinWidth, maxbinWidth,
						stepSize));

				// We now have a list of non-unimodal indexes for a profile.
				if (mvalue > 3) {
					multimodalIndexes.set(i, true);
				}

			}

			// Gap fill regions via erosion/dilation
			multimodalIndexes = multimodalIndexes.dilate(3).erode(3);
			// Find the contiguous regions within the profile

			int startIndex = -1;
			int endIndex = -1;
			boolean isWithinRegion = false;
			for (int i = 0; i < multimodalIndexes.size(); i++) {
				if (multimodalIndexes.get(i)) {

					if (isWithinRegion) { // is multimodal, and we are continuing a region. Update endpoint.
						endIndex = i;
					} else { // is multimodal, but we are not continuing a region. Start a new region.
						startIndex = i;
						endIndex = i;
						isWithinRegion = true;
					}

				} else if (isWithinRegion) { // is not multimodal, but we were continuing a region. End of region.

					final ProfileBarcodingRegion pbr = new ProfileBarcodingRegion(UUID.randomUUID(), pt, startIndex,
							endIndex);
					result.add(pbr);
					isWithinRegion = false;
					endIndex = -1;
					startIndex = -1;
					LOGGER.info(pbr.toString());
				} else { // is not part of region, is not continuing to add. No action

				}
			}


		}

		return result;
	}

	public void clusterDatasetOnNonUnimodalRegions(IAnalysisDataset dataset, Set<ProfileBarcodingRegion> regions) {

		// For each of the index ranges in the regions provided, cluster the cells.
		// Assign each cell to a cluster.

	}
}
