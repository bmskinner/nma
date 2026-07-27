package com.bmskinner.nma.analysis.classification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Element;

import com.bmskinner.nma.analysis.AnalysisMethodException;
import com.bmskinner.nma.analysis.ClusterAnalysisResult;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.XMLNames;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.HammingClusterGroup;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.IClusterGroup;
import com.bmskinner.nma.components.datasets.VirtualDataset;
import com.bmskinner.nma.components.measure.MissingMeasurementException;
import com.bmskinner.nma.components.mesh.MeshCreationException;
import com.bmskinner.nma.components.options.DefaultOptions;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.profiles.BooleanProfile;
import com.bmskinner.nma.components.profiles.IProfile;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.io.XmlSerializable;
import com.bmskinner.nma.stats.MvalueModalityDetector;

import weka.clusterers.EM;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;

/**
 * This class implements clustering on non-unimodal regions of profiles. This is
 * used for object barcoding and Hamming amalgamation.
 */
public class NonunimodalRegionClusteringMethod extends SingleDatasetAnalysisMethod {

	/**
	 * The minimum number of contiguous non-unimodal indexes to declare a region of
	 * interest
	 */
	private static final int MIN_REGION_INDEX_LENGTH = 3;

	/**
	 * The minimum mvalue to declare an index to be potentially non-unimodal.
	 */
	private static final double MIN_MVALUE_THRESHOLD = 2.4;

	/**
	 * The number of indexes to consider when gap filling. Should be odd. A value of
	 * 3 would fill 1 index either side of the index under consideration.
	 */
	private static final int GAP_FILL_WINDOW_SIZE = 3;

	/**
	 * Fixed seed for the RNG for reproducible clustering
	 * 
	 */
	private static final int RNG_SEED = 42;

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
	public record ProfileBarcodingRegion(UUID regionId, ProfileType type, int startIndex, int endIndex)
			implements XmlSerializable {

		/**
		 * Create from an XML element
		 * 
		 * @param e
		 */
		public ProfileBarcodingRegion(Element e){
			this(UUID.fromString(e.getAttributeValue(XMLNames.XML_ID)),
					ProfileType.fromString(e.getAttributeValue(XMLNames.XML_PROFILE_TYPE)),
					Integer.valueOf(e.getAttributeValue(XMLNames.XML_START_INDEX)),
					Integer.valueOf(e.getAttributeValue(XMLNames.XML_END_INDEX)));
		}

		/**
		 * The number of indexes within this region
		 * 
		 * @return
		 */
		public int length() {
			return endIndex - startIndex + 1;
		}

		@Override
		public String toString() {
			return "Barcode region: type=%s, length=%s, start=%s, end=%s".formatted(type, length(), startIndex,
					endIndex);
		}

		public Iterator<Integer> iterator() {
			return IntStream.rangeClosed(startIndex, endIndex).iterator();
		}

		@Override
		public @NonNull Element toXmlElement() {
			return new Element(XMLNames.XML_PROFILE_BARCODE_REGION)
					.setAttribute(XMLNames.XML_ID, regionId.toString())
					.setAttribute(XMLNames.XML_PROFILE_TYPE, type.toString())
					.setAttribute(XMLNames.XML_START_INDEX, String.valueOf(startIndex))
					.setAttribute(XMLNames.XML_END_INDEX, String.valueOf(endIndex));
		}
		


	}
	
	/**
	 * Store the Weka instances data with the map of nucleus to specific instance
	 * 
	 */
	public record InstancesData(Instances instances, List<CellInstance> cellInstances) {

	}

	/**
	 * Store a nucleus with the clustering instance
	 * 
	 */
	public record CellInstance(ICell cell, Instance instance) {

	}
	
	/**
	 * For a single cell, maps a barcoding region of a profile to the cluster
	 * identifier that cell belongs to.
	 * 
	 * @param pbr       a barcoding region
	 * @param clusterId the cluster that this cell belongs to.
	 * 
	 */
	public record BarcodeElement(ProfileBarcodingRegion pbr, Integer cluster) implements XmlSerializable {

		@Override
		public @NonNull Element toXmlElement() {
			// TODO Auto-generated method stub
			return new Element(XMLNames.XML_BARCODE_ELEMENT)
					.setAttribute(XMLNames.XML_ID, pbr.regionId().toString())
					.setAttribute(XMLNames.XML_CLUSTER_NUMBER, cluster.toString());
		}

	}

	public record Barcode(List<BarcodeElement> elements) implements XmlSerializable {

		public Barcode(Element e, Set<ProfileBarcodingRegion> barcodingRegions) {
			this(new ArrayList<>());
			for(final Element e2 : e.getChildren(XMLNames.XML_BARCODE_ELEMENT)) {

				final UUID pbrId = UUID.fromString(e2.getAttributeValue(XMLNames.XML_ID));
				final ProfileBarcodingRegion pbr = barcodingRegions.stream().filter(p -> p.regionId.equals(pbrId))
						.findFirst().get();
				elements.add(
						new BarcodeElement(pbr, Integer.valueOf(e2.getAttributeValue(XMLNames.XML_CLUSTER_NUMBER))));
			}
		}


		@Override
		public String toString() {
			return elements.stream().sorted((p, q) -> Integer.compare(p.pbr.startIndex(), q.pbr.startIndex()))
					.map(e -> String.format("%01x", e.cluster)) // hex format
					.collect(Collectors.joining());
		}


		/**
		 * Calculate the Hamming distance between this barcode and another barcode
		 * 
		 * @param other
		 * @return
		 */
		public int hammingDistance(Barcode other) {
			if (other.elements.size() != elements.size())
				throw new IllegalArgumentException("Cannot compare barcodes of different size (%s versus %s)"
						.formatted(other.elements.size(), elements.size()));
			boolean hasRegion = true;
			int hammingDistance = 0;
			for (final BarcodeElement be : elements) {
				boolean hasElement = false;
				for (final BarcodeElement be2 : other.elements) {
					if (be2.pbr.equals(be.pbr)) {
						hasElement = true;
						if (!be.cluster.equals(be2.cluster)) {
							hammingDistance++;
						}
					}
				}
				hasRegion &= hasElement;
			}

			if (!hasRegion)
				throw new IllegalArgumentException("Not all barcode element regions are comparable between barcodes");

			return hammingDistance;

		}

		@Override
		public @NonNull Element toXmlElement() {

			final Element e = new Element(XMLNames.XML_BARCODE);

			for (final BarcodeElement be : elements) {
				e.addContent(be.toXmlElement());
			}

			return e;
		}

	}

	public NonunimodalRegionClusteringMethod(@NonNull IAnalysisDataset dataset) {
		super(dataset);

	}

	@Override
	public IAnalysisResult call() throws Exception {
		this.fireUpdateProgressTotalLength(
				(ProfileType.values().length *
						dataset.getCollection().getMedianArrayLength())

						+ (ProfileType.values().length * dataset.getCollection().size()));

		try {

			final Set<ProfileBarcodingRegion> pbrs = findNonUnimodalProfileRegions(dataset);

			final IClusterGroup g = clusterDatasetOnNonUnimodalRegions(dataset, pbrs);
			return new ClusterAnalysisResult(dataset, g);
		} catch (final Exception e) {
			LOGGER.log(Level.SEVERE, "Error running hamming amalgamation: %s".formatted(e.getMessage()), e);
		}
		return null;
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

		LOGGER.fine("Detecting barcoding regions");

		final Set<ProfileBarcodingRegion> result = new HashSet<>();

		for (final ProfileType pt : ProfileType.values()) {
			
			LOGGER.fine("Testing modality of %s at %s indexes in %s cells".formatted(pt,
					dataset.getCollection().getMedianArrayLength(), dataset.getCollection().size()));

			BooleanProfile multimodalIndexes = new BooleanProfile(dataset.getCollection().getMedianArrayLength(),
					false);

			final double[] mvalues = new double[dataset.getCollection().getMedianArrayLength()];

			// Precompute interpolated profiles for each cell
			final Map<UUID, IProfile> interpolatedProfiles = new HashMap<>();
			for (final ICell c : dataset.getCollection().getCells()) {
				final Nucleus n = c.getPrimaryNucleus();
				final IProfile p = n.getProfile(pt, OrientationMark.REFERENCE)
						.interpolate(dataset.getCollection().getMedianArrayLength());
				interpolatedProfiles.put(n.getId(), p);
				this.fireProgressEvent();
			}
			LOGGER.fine("Interpolated %s for all cells".formatted(pt));

			for (int i = 0; i < dataset.getCollection().getMedianArrayLength(); i++) {
				
				double minValue = Double.MAX_VALUE;
				double maxValue = -Double.MAX_VALUE;
				
				final List<ICell> cells = dataset.getCollection().getCells();

				// Hold value at this index for all nuclei
				final double[] profileValues = new double[cells.size()];

				// Get the current index value from interpolated nucleus profile
				for (int j = 0; j < cells.size(); j++) {
					final Nucleus n = cells.get(j).getPrimaryNucleus();
					final IProfile p = interpolatedProfiles.get(n.getId());
					profileValues[j] = p.get(i);
					
					if(p.get(i)<minValue) {
						minValue = p.get(i);
					}
					if(p.get(i)>maxValue) {
						maxValue = p.get(i);
					}
				}
				
				LOGGER.finer("Fetched profile values from nuclei at index %s of %s".formatted(i + 1,
						dataset.getCollection().getMedianArrayLength()));
				
				// Perform modality test on profile values
				// Dynamic values for each profile type based on range
				final double range = maxValue - minValue;
				final double minbinWidth = range / 20;
				final double maxbinWidth = range / 10;
				final double stepSize = range / 40;


				final MvalueModalityDetector mt = new MvalueModalityDetector(profileValues, minbinWidth, maxbinWidth, stepSize);
				final double mvalue = mt.getMValue();
				
				mvalues[i] = mvalue;
				LOGGER.finer("Index %s: mValue %s, bins %s - %s, step %s".formatted(i, mvalue, minbinWidth, maxbinWidth,
						stepSize));

				// Store mvalue state of each index in a boolean profile
				if (mvalue >= MIN_MVALUE_THRESHOLD) {
					multimodalIndexes.set(i, true);
				}

				this.fireProgressEvent();

			}
			LOGGER.fine(Arrays.toString(mvalues));
			LOGGER.fine("Detecting contiguous regions of interest in %s".formatted(pt));
			// Gap fill adjacent indexes to make contiguous regions
			multimodalIndexes = multimodalIndexes
					.dilate(GAP_FILL_WINDOW_SIZE)
					.erode(GAP_FILL_WINDOW_SIZE);

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
					if (pbr.length() >= MIN_REGION_INDEX_LENGTH) {
						LOGGER.fine(pbr.toString());
						result.add(pbr);
					}
					isWithinRegion = false;
					endIndex = -1;
					startIndex = -1;

				} else { // is not part of region, is not continuing to add. No action

				}
			}


		}

		return result;
	}

	public IClusterGroup clusterDatasetOnNonUnimodalRegions(IAnalysisDataset dataset,
			Set<ProfileBarcodingRegion> regions)
			throws Exception {
		LOGGER.fine("Clustering dataset on barcoding regions");
		// For each of the index ranges in the regions provided, cluster the cells.
		// Assign each cell to a cluster.
		Map<ICell, List<BarcodeElement>> cellBarcodes = new HashMap<>();
		
		for (final ProfileBarcodingRegion pbr : regions) {

			LOGGER.finer("Creating clusters for %s".formatted(pbr.toString()));

			final ArrayList<Attribute> attributes = makeAttributes(pbr);
			// Either EM or elbow method+kmeans
			final InstancesData instanceData = makeInstances(pbr, attributes, dataset);
			

			cellBarcodes = assignClusters(pbr, instanceData, dataset, cellBarcodes);
		}

		// Aggregate the barcode elements into cell barcodes
		final Map<ICell, Barcode> barcodes = new HashMap<>();
		for (final Entry<ICell, List<BarcodeElement>> entry : cellBarcodes.entrySet()) {
			barcodes.put(entry.getKey(), new Barcode(entry.getValue()));
		}

		for (final Entry<ICell, Barcode> entry : barcodes.entrySet()) {
			LOGGER.finer("Cell %s has barcode %s".formatted(entry.getKey().getId(), entry.getValue()));
		}

		// Invert this data - count nuclei per barcode

		final Map<Barcode, Set<ICell>> barcodeMap = new HashMap<>();
		for (final Entry<ICell, Barcode> entry : barcodes.entrySet()) {
			barcodeMap.computeIfAbsent(entry.getValue(), n -> new HashSet<ICell>()).add(entry.getKey());
		}
		LOGGER.fine("Total of %s unique barcodes".formatted(barcodeMap.size()));
		for (final Entry<Barcode, Set<ICell>> entry : barcodeMap.entrySet()) {
			LOGGER.fine("Barcode %s has %s cells".formatted(entry.getKey(), entry.getValue().size()));
		}

		LOGGER.fine("Amalgamating barcodes");
		final Map<Barcode, Set<Barcode>> amalgamatedClusters = new HashMap<>();
//		while (barcodeMap.size() + amalgamatedClusters.size() > 16) {

			for (int maxDistance = 1; maxDistance < 10; maxDistance++) {
				amalgamatedClusters.putAll(runHammingAmalgamation(barcodeMap, maxDistance));
				LOGGER.fine("Consolidated to %s solo barcodes and %s barcodes at hamming distance %s".formatted(
						barcodeMap.size(),
						amalgamatedClusters.size(), maxDistance));

				if (barcodeMap.size() + amalgamatedClusters.size() <= 16) {
					break;
				}
			}


//		}
		for (final Barcode b : barcodeMap.keySet()) {
			amalgamatedClusters.put(b, new HashSet<>());
		}

		LOGGER.fine("We have amalgamated barcodes into %s clusters".formatted(amalgamatedClusters.size()));

		for (final Entry<Barcode, Set<Barcode>> entry : amalgamatedClusters.entrySet()) {

			LOGGER.fine("Cluster: %s with %s".formatted(entry.getKey(),
					entry.getValue().stream().map(Barcode::toString).collect(Collectors.joining(", "))));

			// Ensure we have all barcodes for this cluster in one set
			entry.getValue().add(entry.getKey());
		}

		final Map<UUID, Barcode> nucleusIDsToBarcodes = barcodes.entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey().getId(), Map.Entry::getValue));

		final HammingClusterGroup clusterGroup = createClusterGroup(amalgamatedClusters, barcodes, dataset, regions,
				nucleusIDsToBarcodes);

		return clusterGroup;

	}

	private HammingClusterGroup createClusterGroup(Map<Barcode, Set<Barcode>> amalgamatedClusters,
			Map<ICell, Barcode> barcodes,
			IAnalysisDataset dataset, Set<ProfileBarcodingRegion> regions, Map<UUID, Barcode> nucleusIDsToBarcodes)
			throws MissingDataException, SegmentUpdateException {

		// Create a group to store the clustered cells
		final HashOptions clusterOptions = new DefaultOptions();
		clusterOptions.setInt(HashOptions.CLUSTER_EM_ITERATIONS_KEY, 100);
		clusterOptions.setString(HashOptions.CLUSTER_METHOD_KEY, ClusteringMethod.EM.toString());

		final HammingClusterGroup group = new HammingClusterGroup(
				"Hamming cluster", clusterOptions,
				UUID.randomUUID(), regions, nucleusIDsToBarcodes);

		// Make the child datasets for each cluster
		// Make dataset clusters from the amalgamation
		for (final Entry<Barcode, Set<Barcode>> entry : amalgamatedClusters.entrySet()) {
			
			final List<ICell> cellsToAdd = new ArrayList<>();

			
			for(final Barcode bar : entry.getValue()) {

				for (final Entry<ICell, Barcode> cellMap : barcodes.entrySet()) {

					if (cellMap.getValue().equals(bar)) {
						cellsToAdd.add(cellMap.getKey());
					}
				}
			}


			final IAnalysisDataset v = new VirtualDataset(dataset,
					"%s_cluster_%s".formatted(group.getName(), entry.getKey()),
					null, cellsToAdd);

			group.addDataset(v);

			// attach the clusters to their parent collection
			dataset.addChildDataset(v);

			// set shared counts
			v.getCollection().setSharedCount(dataset.getCollection(), entry.getValue().size());
			dataset.getCollection().setSharedCount(v.getCollection(), entry.getValue().size());

		}

		if (regions.size() > 0) {

			dataset.addClusterGroup(group);

			group.makeVirtualClusterDatasets(dataset);
		}

		return group;

	}

	private Map<Barcode, Set<Barcode>> runHammingAmalgamation(Map<Barcode, Set<ICell>> barcodeMap, int maxDistance) {

		// Display summary counts
		int largestClusterSize = 0;
		Barcode largestCluster = null;
		LOGGER.finer("Detected %s barcodes in total".formatted(barcodeMap.keySet().size()));
		for (final Entry<Barcode, Set<ICell>> entry : barcodeMap.entrySet()) {
//			LOGGER.fine("Barcode %s has %s nuclei".formatted(entry.getKey(), entry.getValue().size()));

			if (entry.getValue().size() > largestClusterSize) {
				largestClusterSize = entry.getValue().size();
				largestCluster = entry.getKey();
			}
		}

		final Map<Barcode, Set<Barcode>> amalgamatedClusters = new HashMap<>();

		// Hamming amalgamation of nearby variants
		// Find the largest cluster, amalgamate barcodes with only 1 distance
		final Set<Barcode> amalgamatedBarcodes = new HashSet<>();
		for (final Entry<Barcode, Set<ICell>> entry : barcodeMap.entrySet()) {
			final int hammingDistance = largestCluster.hammingDistance(entry.getKey());
			LOGGER.finer("Barcode %s has distance %s to barcode %s".formatted(largestCluster, hammingDistance,
					entry.getKey()));
			if (hammingDistance == maxDistance) {
				amalgamatedBarcodes.add(entry.getKey());
			}
		}
		amalgamatedClusters.put(largestCluster, amalgamatedBarcodes);

		// Remove any processed barcods from consideration in the next iteration
		// Note this mutation will apply to the object in the calling function
		barcodeMap.remove(largestCluster);
		for (final Barcode b : amalgamatedBarcodes) {
			barcodeMap.remove(b);
		}

		LOGGER.finer(
				"Amalgamated %s barcodes to largest barcode %s".formatted(amalgamatedBarcodes.size(), largestCluster));

		return amalgamatedClusters;
	}


	/**
	 * Create attributes for each index of a profile region of interest
	 * 
	 * @param pbr
	 * @return
	 * @throws AnalysisMethodException
	 */
	private ArrayList<Attribute> makeAttributes(ProfileBarcodingRegion pbr) throws AnalysisMethodException {

		final ArrayList<Attribute> attributes = new ArrayList<>();

		final Iterator<Integer> it = pbr.iterator();
		while (it.hasNext()) {
			final int i = it.next();
			final Attribute a = new Attribute("%s_index_%s".formatted(pbr.type, i));
			attributes.add(a);
			LOGGER.finer("Created attribute %s".formatted(a.toString()));
		}
		return attributes;
	}


	/**
	 * Create Instances using the interpolated profiles of nuclei
	 * 
	 * @return
	 * @throws MeshCreationException
	 * @throws ProfileException
	 * @throws MissingDataException
	 * @throws ComponentCreationException
	 * @throws MissingMeasurementException
	 * @throws SegmentUpdateException
	 * @throws ClusteringMethodException
	 */
	private InstancesData makeInstances(ProfileBarcodingRegion pbr,
			ArrayList<Attribute> attributes,
			IAnalysisDataset dataset)
			throws AnalysisMethodException, MeshCreationException,
			ProfileException, MissingDataException, ComponentCreationException,
			SegmentUpdateException {
		LOGGER.finer("Creating clusterable instances");

		final List<CellInstance> instanceList = new ArrayList<>();

		// Weka clustering uses a table in which columns are attributes and rows are
		// instances

		final Instances instances = new Instances(dataset.getName(), attributes, dataset.getCollection().size());

		for (final ICell c : dataset.getCollection()) {
			final Nucleus n = c.getPrimaryNucleus();

			final Instance inst = new SparseInstance(attributes.size());

			final IProfile p = n.getProfile(pbr.type(), OrientationMark.REFERENCE)
					.interpolate(dataset.getCollection().getMaxProfileLength());

			int attIndex = 0;
			final Iterator<Integer> it = pbr.iterator();
			while (it.hasNext()) {
				final int i = it.next();
				final Attribute att = attributes.get(attIndex);
				inst.setValue(att, p.get(i));
				attIndex++;
				LOGGER.finer("Created instance for %s with value %s".formatted(att.toString(), p.get(i)));
			}

			instanceList.add(new CellInstance(c, inst));
			instances.add(inst);
			inst.setDataset(instances);

		}
		return new InstancesData(instances, instanceList);
	}

	/**
	 * Given a trained clusterer, put each nucleus within the collection into a
	 * cluster
	 * 
	 * @param clusterer the clusterer to use
	 * @throws Exception
	 */
	private Map<ICell, List<BarcodeElement>> assignClusters(ProfileBarcodingRegion pbr, InstancesData instanceData,
			IAnalysisDataset dataset, Map<ICell, List<BarcodeElement>> cellBarcodes)
			throws Exception {

		// create the clusterer to run on the Instances
		final EM clusterer = new EM();
		clusterer.setSeed(RNG_SEED);
		clusterer.setMaxIterations(100);
		clusterer.setNumClusters(3);
		clusterer.buildClusterer(instanceData.instances());

		final int numberOfClusters = clusterer.numberOfClusters();

		if (numberOfClusters > 1) {

			LOGGER.fine("%s has %s clusters".formatted(pbr, numberOfClusters));

			for (final CellInstance ni : instanceData.cellInstances) {
				final int clusterNumber = clusterer.clusterInstance(ni.instance);
				final BarcodeElement barcode = new BarcodeElement(pbr, clusterNumber);
				cellBarcodes.computeIfAbsent(ni.cell, n -> new ArrayList<BarcodeElement>()).add(barcode);
				LOGGER.finer("Nucleus %s in region %s is cluster %s".formatted(ni.cell.getId(), pbr, clusterNumber));

			}
		}
		return cellBarcodes;
	}

}
