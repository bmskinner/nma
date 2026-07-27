package com.bmskinner.nma.components.datasets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Element;

import com.bmskinner.nma.analysis.classification.NonunimodalRegionClusteringMethod.Barcode;
import com.bmskinner.nma.analysis.classification.NonunimodalRegionClusteringMethod.BarcodeElement;
import com.bmskinner.nma.analysis.classification.NonunimodalRegionClusteringMethod.ProfileBarcodingRegion;
import com.bmskinner.nma.analysis.nucleus.ConsensusAveragingMethod;
import com.bmskinner.nma.components.XMLNames;
import com.bmskinner.nma.components.options.HashOptions;

/**
 * Store data for clusters produced by Hamming amalgamation.
 * 
 */
public class HammingClusterGroup extends DefaultClusterGroup {

	private static final Logger LOGGER = Logger.getLogger(HammingClusterGroup.class.getName());

	// Store the visible clusters - based on hamming amalgamation of barcodes

	private Set<ProfileBarcodingRegion> barcodingRegions;

	private Map<UUID, Barcode> cellBarcodes;

	// Also store the intermediate clusters for each region

	/**
	 * Total clusters per region
	 */
	transient private Map<ProfileBarcodingRegion, Integer> clusterNumbers = new HashMap<>();

	/**
	 * Store a transient mapping of the nuclei in each PBR cluster to calculate
	 * profile medians
	 * 
	 */
	transient private Map<ProfileBarcodingRegion, Map<Integer, IAnalysisDataset>> clusterDatasets = new HashMap<>();

	/**
	 * Create a new cluster group
	 * 
	 * @param name    the group name (informal)
	 * @param options the options used to create the cluster
	 */
	public HammingClusterGroup(@NonNull String name, @NonNull HashOptions options,
			@NonNull UUID id, @NonNull Set<ProfileBarcodingRegion> barcodingRegions,
			@NonNull Map<UUID, Barcode> cellBarcodes) {
		super(name, options, id);
		this.barcodingRegions = barcodingRegions;
		this.cellBarcodes = cellBarcodes;
	}

	public HammingClusterGroup(@NonNull Element e) {
		super(e);

		barcodingRegions = new HashSet<>();
		
		for(final Element el : e.getChildren(XMLNames.XML_PROFILE_BARCODE_REGION)) {
			barcodingRegions.add(new ProfileBarcodingRegion(el));
		}

		cellBarcodes = new HashMap<>();

		for (final Element el : e.getChildren(XMLNames.XML_BARCODE)) {
			
			final UUID nucleusId = UUID.fromString(el.getAttributeValue(XMLNames.XML_ID));

			cellBarcodes.put(nucleusId,
					new Barcode(el, barcodingRegions));
			
		}

	}

	private HammingClusterGroup(HammingClusterGroup g) {
		super(g);
		barcodingRegions = g.barcodingRegions;
		cellBarcodes = g.cellBarcodes;
		clusterDatasets = g.clusterDatasets;
	}

	/**
	 * Create a cluster group from a template
	 * 
	 * @param template
	 */
	public HammingClusterGroup(@NonNull IClusterGroup template) {
		super(template);
		if (template instanceof final HammingClusterGroup g) {
			this.barcodingRegions = g.barcodingRegions;
			this.cellBarcodes = g.cellBarcodes;
			this.clusterDatasets = g.clusterDatasets;
		}
	}

	public void setBarcodingRegions(Set<ProfileBarcodingRegion> regions) {
		this.barcodingRegions = regions;
	}

	public Set<ProfileBarcodingRegion> getBarcodingRegions() {
		return barcodingRegions;
	}

	public Map<UUID, Barcode> getCellBarcodes() {
		return cellBarcodes;
	}

	public void setCellBarcodes(Map<UUID, Barcode> cellBarcodes) {
		this.cellBarcodes = cellBarcodes;
	}

	/**
	 * Create appropriate cluster datasets for each region
	 * 
	 * @param parent the dataset the nuclei came from
	 */
	public void makeVirtualClusterDatasets(IAnalysisDataset parent) {


		for (final ProfileBarcodingRegion pbr : barcodingRegions) {

			LOGGER.fine("Making virtual datasets for %s".formatted(pbr));

			// Find the number of clusters in this region
			final Map<Integer, Long> clusterCounts = cellBarcodes.values().stream()
					.flatMap(e -> e.elements().stream().filter(b -> b.pbr().equals(pbr)))
					.map(BarcodeElement::cluster)
					.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

			clusterNumbers.put(pbr, clusterCounts.size());

			// Now extract the nuclei for each cluster and assign to a virtual dataset
			for (int i = 0; i < clusterCounts.size(); i++) {

				final IAnalysisDataset clusterDataset = new VirtualDataset(parent, pbr.regionId() + "_" + i);

				for (final Entry<UUID, Barcode> entry : cellBarcodes.entrySet()) {
					final List<BarcodeElement> pbrElements = entry.getValue().elements().stream()
							.filter(e -> e.pbr().equals(pbr)).collect(Collectors.toList());
					for (final BarcodeElement be : pbrElements) {
						if (be.cluster().equals(i)) {
							clusterDataset.getCollection().add(parent.getCollection().getCell(entry.getKey()));
						}

					}
				}

				try {
					clusterDataset.getCollection().getProfileCollection().calculateProfiles();

					new ConsensusAveragingMethod(clusterDataset).call();

					clusterDatasets.computeIfAbsent(pbr, k -> new HashMap<>()).put(i, clusterDataset);

				} catch (final Exception e1) {
					LOGGER.log(Level.SEVERE,
							"Unable to create profiles or consensus in virtual dataset for Hamming group: %s"
									.formatted(e1.getMessage(), e1));
				}
			}


		}
	}

	public int getNumberOfClusters(ProfileBarcodingRegion pbr) {
		return clusterNumbers.get(pbr);
	}

	public List<IAnalysisDataset> getRegionDatasets(ProfileBarcodingRegion pbr) {
		final List<IAnalysisDataset> result = new ArrayList<>();
		result.addAll(clusterDatasets.get(pbr).values());
		return result;
	}

	public IAnalysisDataset getRegionDataset(ProfileBarcodingRegion pbr, int cluster) {
		return clusterDatasets.get(pbr).get(cluster);
	}

	@Override
	public @NonNull Element toXmlElement() {
		final Element e = super.toXmlElement();
		e.setName(XMLNames.XML_HAMMING_CLUSTER_GROUP);

		for (final ProfileBarcodingRegion pbr : barcodingRegions) {
			e.addContent(pbr.toXmlElement());
		}
		
		for (final Entry<UUID, Barcode> entry : cellBarcodes.entrySet()) {
			final Element nucleusEntry = entry.getValue().toXmlElement();
			nucleusEntry.setAttribute(XMLNames.XML_ID, entry.getKey().toString());
			e.addContent(nucleusEntry);
		}


		return e;
	}

	@Override
	public boolean hasTree() {
		return false;
	}

}
