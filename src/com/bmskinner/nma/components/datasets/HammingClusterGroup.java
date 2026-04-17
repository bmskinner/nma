package com.bmskinner.nma.components.datasets;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Element;

import com.bmskinner.nma.analysis.classification.NonunimodalRegionClusteringMethod.Barcode;
import com.bmskinner.nma.analysis.classification.NonunimodalRegionClusteringMethod.ProfileBarcodingRegion;
import com.bmskinner.nma.components.XMLNames;
import com.bmskinner.nma.components.options.HashOptions;

/**
 * Store data for clusters produced by Hamming amalgamation.
 * 
 */
public class HammingClusterGroup extends DefaultClusterGroup {

	private Set<ProfileBarcodingRegion> barcodingRegions;

	private Map<UUID, Barcode> nucleusBarcodes;

	/**
	 * Create a new cluster group
	 * 
	 * @param name    the group name (informal)
	 * @param options the options used to create the cluster
	 */
	public HammingClusterGroup(@NonNull String name, @NonNull HashOptions options,
			@NonNull UUID id) {
		super(name, options, id);
	}

	public HammingClusterGroup(@NonNull Element e) {
		super(e);

		barcodingRegions = new HashSet<>();
		
		for(final Element el : e.getChildren(XMLNames.XML_PROFILE_BARCODE_REGION)) {
			barcodingRegions.add(new ProfileBarcodingRegion(el));
		}

		nucleusBarcodes = new HashMap<>();

		for (final Element el : e.getChildren(XMLNames.XML_BARCODE)) {
			
			final UUID nucleusId = UUID.fromString(el.getAttributeValue(XMLNames.XML_ID));

			nucleusBarcodes.put(nucleusId,
					new Barcode(el, barcodingRegions));
			
		}

	}

	private HammingClusterGroup(HammingClusterGroup g) {
		super(g);
	}

	/**
	 * Create a new cluster group with a tree
	 * 
	 * @param name    the group name (informal)
	 * @param options the options used to create the cluster
	 * @param tree    the Newick tree for the cluster as a String
	 */
	public HammingClusterGroup(@NonNull String name, @NonNull HashOptions options,
			@NonNull String tree, @NonNull UUID id) {
		super(name, options, tree, id);
	}

	/**
	 * Create a cluster group from a template
	 * 
	 * @param template
	 */
	public HammingClusterGroup(@NonNull IClusterGroup template) {
		super(template);
	}

	public void setBarcodingRegions(Set<ProfileBarcodingRegion> regions) {
		this.barcodingRegions = regions;
	}

	public Set<ProfileBarcodingRegion> getBarcodingRegions() {
		return barcodingRegions;
	}

	public Map<UUID, Barcode> getNucleusBarcodes() {
		return nucleusBarcodes;
	}

	public void setNucleusBarcodes(Map<UUID, Barcode> nucleusBarcodes) {
		this.nucleusBarcodes = nucleusBarcodes;
	}

	@Override
	public @NonNull Element toXmlElement() {
		final Element e = super.toXmlElement();
		e.setName(XMLNames.XML_HAMMING_CLUSTER_GROUP);

		for (final ProfileBarcodingRegion pbr : barcodingRegions) {
			e.addContent(pbr.toXmlElement());
		}
		
		for (final Entry<UUID, Barcode> entry : nucleusBarcodes.entrySet()) {
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
