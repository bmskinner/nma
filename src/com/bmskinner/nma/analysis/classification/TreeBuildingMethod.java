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
package com.bmskinner.nma.analysis.classification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.AnalysisMethodException;
import com.bmskinner.nma.analysis.ClusterAnalysisResult;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.components.MissingComponentException;
import com.bmskinner.nma.components.MissingLandmarkException;
import com.bmskinner.nma.components.Taggable;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.DefaultClusterGroup;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.IClusterGroup;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.measure.MeasurementScale;
import com.bmskinner.nma.components.mesh.DefaultMesh;
import com.bmskinner.nma.components.mesh.Mesh;
import com.bmskinner.nma.components.mesh.MeshCreationException;
import com.bmskinner.nma.components.mesh.MeshFace;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.profiles.IProfile;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.logging.Loggable;

import weka.clusterers.HierarchicalClusterer;
import weka.core.Attribute;
import weka.core.EuclideanDistance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;

public class TreeBuildingMethod extends CellClusteringMethod {

	private static final Logger LOGGER = Logger.getLogger(TreeBuildingMethod.class.getName());

	protected String newickTree = null;

	/**
	 * Construct from a dataset with a set of clustering options
	 * 
	 * @param dataset
	 * @param options
	 */
	public TreeBuildingMethod(@NonNull IAnalysisDataset dataset, @NonNull HashOptions options) {
		super(dataset, options);
	}

	@Override
	public IAnalysisResult call() throws Exception {

		makeTree();
		int clusterNumber = dataset.getMaxClusterGroupNumber() + 1;
		IClusterGroup group = new DefaultClusterGroup(
				IClusterGroup.CLUSTER_GROUP_PREFIX + "_" + clusterNumber, options,
				newickTree);
		return new ClusterAnalysisResult(dataset, group);
	}

	/**
	 * If a tree is present (i.e clustering was hierarchical), return the string of
	 * the tree, otherwise return null
	 * 
	 * @return
	 */
	public String getNewickTree() {
		return newickTree;
	}

	protected String[] createClustererOptions() {
		ClusteringMethod cm = ClusteringMethod.from(options);
		if (cm.equals(ClusteringMethod.HIERARCHICAL)) {
			String[] o = new String[4];
			o[0] = "-N"; // number of clusters
			o[1] = String.valueOf(options.getInt(HashOptions.CLUSTER_MANUAL_CLUSTER_NUMBER_KEY));
			o[2] = "-L"; // algorithm
			HierarchicalClusterMethod hm = HierarchicalClusterMethod
					.valueOf(options.getString(HashOptions.CLUSTER_HIERARCHICAL_METHOD_KEY));
			o[3] = hm.code();
			return o;
		}

		if (cm.equals(ClusteringMethod.EM)) {
			String[] o = new String[2];
			o[0] = "-I"; // max. iterations
			o[1] = String.valueOf(options.getInt(HashOptions.CLUSTER_EM_ITERATIONS_KEY));
			return o;
		}
		return null;
	}

	/**
	 * Run the clustering on a collection
	 * 
	 * @param collection
	 * @return success or fail
	 * @throws Exception
	 */
	protected boolean makeTree() throws Exception {
		// create Instances to hold Instance
		Instances instances = makeInstances();

		// create the clusterer to run on the Instances
		String[] optionArray = createClustererOptions();

		HierarchicalClusterer clusterer = new HierarchicalClusterer();

		clusterer.setOptions(optionArray); // set the options
		clusterer.setDistanceFunction(new EuclideanDistance());
		clusterer.setDistanceIsBranchLength(true);
		clusterer.setNumClusters(1);
		clusterer.setDebug(true);

		LOGGER.finest("Building clusterer for tree");
		clusterer.buildClusterer(instances); // build the clusterer with
		// one cluster for the tree
		clusterer.setPrintNewick(true);

		this.newickTree = clusterer.graph();

		return true;
	}

	private ArrayList<Attribute> makeTsneAttributes() {
		ArrayList<Attribute> attributes = new ArrayList<>();
		attributes.add(new Attribute("tSNE_X"));
		attributes.add(new Attribute("tSNE_Y"));
		if (ClusteringMethod.from(options).equals(ClusteringMethod.HIERARCHICAL)) {
			Attribute name = new Attribute("name", (List<String>) null);
			attributes.add(name);
		}
		return attributes;
	}

	private ArrayList<Attribute> makePCAttributes() {

		// From the first nucleus, find the number of PCs to cluster on
		Nucleus n = dataset.getCollection().getCells().stream().findFirst()
				.orElseThrow(NullPointerException::new).getPrimaryNucleus();
		int nPcs = (int) n.getMeasurement(Measurement.PCA_N);

		ArrayList<Attribute> attributes = new ArrayList<>();
		for (int i = 1; i <= nPcs; i++)
			attributes.add(new Attribute("PC_" + i));

		if (ClusteringMethod.from(options).equals(ClusteringMethod.HIERARCHICAL)) {
			Attribute name = new Attribute("name", (List<String>) null);
			attributes.add(name);
		}
		return attributes;
	}

	@Override
	protected ArrayList<Attribute> makeAttributes() throws AnalysisMethodException {

		// Shortcuts if dimensional reduction is chosen
		LOGGER.finer("Checking if tSNE clustering is set");
		if (options.getBoolean(HashOptions.CLUSTER_USE_TSNE_KEY))
			return makeTsneAttributes();

		LOGGER.finer("Checking if PCA clustering is set");
		if (options.getBoolean(HashOptions.CLUSTER_USE_PCA_KEY))
			return makePCAttributes();

		LOGGER.finer("Creating attribute count on values directly");
		// Determine the number of attributes required
		int attributeCount = 0;
		int profileAttributeCount = 0;

		if (!options.hasString(HashOptions.CLUSTER_METHOD_KEY))
			throw new AnalysisMethodException("No clustering method in options");

		// How many attributes per profile?
		double profileWindow = Taggable.DEFAULT_PROFILE_WINDOW_PROPORTION;
		if (dataset.hasAnalysisOptions())
			profileWindow = dataset.getAnalysisOptions().orElseThrow(NullPointerException::new)
					.getProfileWindowProportion();

		profileAttributeCount = (int) Math.floor(1d / profileWindow);

		// An attribute for each index in each selected profile, spaced <windowSize>
		// apart
		for (ProfileType t : ProfileType.displayValues()) {
			if (options.getBoolean(t.toString())) {
				LOGGER.finer("Creating attribute count for " + t.toString());
				attributeCount += profileAttributeCount;
			}
		}

		for (Measurement stat : Measurement.getNucleusStats()) {
			if (options.getBoolean(stat.toString())) {
				LOGGER.finer("Adding attribute count for " + stat);
				attributeCount++;
			}
		}

		for (Measurement stat : Measurement.getGlcmStats()) {
			if (options.getBoolean(stat.toString())) {
				LOGGER.finer("Adding attribute count for " + stat);
				attributeCount++;
			}
		}

		Mesh mesh = null;

		if (options.getBoolean(HashOptions.CLUSTER_INCLUDE_MESH_KEY) && collection.hasConsensus()) {
			LOGGER.finer("Adding attribute count for mesh");
			try {
				mesh = new DefaultMesh(collection.getConsensus());
				attributeCount += mesh.getFaces().size();
			} catch (MeshCreationException | MissingLandmarkException
					| ComponentCreationException e) {
				LOGGER.log(Loggable.STACK, "Cannot create mesh", e);
				throw new AnalysisMethodException(e);
			}

		}

		if (ClusteringMethod.from(options).equals(ClusteringMethod.HIERARCHICAL)) {
			LOGGER.finer("Adding attribute count for name - hierarchical only");
			attributeCount++;
		}

		// Create the attributes
		LOGGER.finer("Creating attributes");
		ArrayList<Attribute> attributes = new ArrayList<>(attributeCount);
		int profileAttCounter = 0;
		for (ProfileType t : ProfileType.displayValues()) {
			if (options.getBoolean(t.toString())) {
				LOGGER.finer("Creating attributes for profile " + t);
				for (int i = 0; i < profileAttributeCount; i++) {
					Attribute a = new Attribute("att_" + profileAttCounter);
					attributes.add(a);
					profileAttCounter++;
				}
			}
		}

		for (Measurement stat : Measurement.getNucleusStats()) {
			if (options.getBoolean(stat.toString())) {
				LOGGER.finer("Creating attribute for " + stat);
				Attribute a = new Attribute(stat.toString());
				attributes.add(a);
			}
		}

		for (Measurement stat : Measurement.getGlcmStats()) {
			if (options.getBoolean(stat.toString())) {
				LOGGER.finer("Creating attribute for " + stat);
				Attribute a = new Attribute(stat.toString());
				attributes.add(a);
			}
		}

		for (UUID segID : getSegmentIds()) {
			if (options.getBoolean(segID.toString())) {
				LOGGER.finer("Creating attribute for segment" + segID.toString());
				Attribute a = new Attribute("att_" + segID.toString());
				attributes.add(a);
			}
		}

		if (options.getBoolean(HashOptions.CLUSTER_INCLUDE_MESH_KEY) && collection.hasConsensus()
				&& mesh != null) {

			for (MeshFace face : mesh.getFaces()) {
				LOGGER.finer("Creating attribute for face " + face.toString());
				Attribute a = new Attribute("mesh_" + face.toString());
				attributes.add(a);
			}
		}

		if (ClusteringMethod.from(options).equals(ClusteringMethod.HIERARCHICAL)) {
			LOGGER.finer("Creating attribute for name - hierarchical only");
			Attribute name = new Attribute("name", (List<String>) null);
			attributes.add(name);
		}
		return attributes;
	}

	private List<UUID> getSegmentIds() {
		List<UUID> list = new ArrayList<>();
		for (String s : options.getStringKeys()) {
			try {
				list.add(UUID.fromString(s));
			} catch (IllegalArgumentException e) {
				// not a UUID
			}
		}
		return list;
	}

	/**
	 * Create Instances using the interpolated profiles of nuclei
	 * 
	 * @return
	 * @throws MeshCreationException
	 * @throws ProfileException
	 * @throws MissingComponentException
	 * @throws ComponentCreationException
	 * @throws ClusteringMethodException
	 */
	@Override
	protected Instances makeInstances() throws AnalysisMethodException, MeshCreationException,
			ProfileException, MissingComponentException, ComponentCreationException {
		LOGGER.finer("Creating clusterable instances");
		double windowProportion = Taggable.DEFAULT_PROFILE_WINDOW_PROPORTION;
		if (dataset.hasAnalysisOptions())// Merged datasets may not have options
			windowProportion = dataset.getAnalysisOptions().orElseThrow(NullPointerException::new)
					.getProfileWindowProportion();

		// Weka clustering uses a table in which columns are attributes and rows are
		// instances
		ArrayList<Attribute> attributes = makeAttributes();

		Instances instances = new Instances(collection.getName(), attributes, collection.size());

		Mesh template = null;
		if (options.getBoolean(HashOptions.CLUSTER_INCLUDE_MESH_KEY) && collection.hasConsensus()) {
			template = new DefaultMesh(collection.getConsensus());
		}

		for (ICell c : collection) {
			for (Nucleus n : c.getNuclei()) {
				addNucleus(c, n, attributes, instances, template, windowProportion);
			}
		}
		return instances;

	}

	private void addTsneNucleus(ICell c, Nucleus n, ArrayList<Attribute> attributes,
			Instances instances) {
		int attNumber = 0;
		Instance inst = new SparseInstance(attributes.size());
		Attribute attX = attributes.get(attNumber++);
		inst.setValue(attX, n.getMeasurement(Measurement.TSNE_1));
		Attribute attY = attributes.get(attNumber++);
		inst.setValue(attY, n.getMeasurement(Measurement.TSNE_2));

		if (ClusteringMethod.from(options).equals(ClusteringMethod.HIERARCHICAL)) {
			String uniqueName = c.getId().toString();
			Attribute att = attributes.get(attNumber++);
			inst.setValue(att, uniqueName);
		}
		instances.add(inst);
		cellToInstanceMap.put(inst, c.getId());
		fireProgressEvent();
	}

	private void addPCANucleus(ICell c, Nucleus n, ArrayList<Attribute> attributes,
			Instances instances) {
		int attNumber = 0;
		Instance inst = new SparseInstance(attributes.size());

		int nPcs = (int) n.getMeasurement(Measurement.PCA_N);
		for (int i = 1; i <= nPcs; i++) {
			Attribute att = attributes.get(attNumber++);
			double pc = n.getMeasurement(Measurement.makePrincipalComponent(i));
			inst.setValue(att, pc);
		}

		if (ClusteringMethod.from(options).equals(ClusteringMethod.HIERARCHICAL)) {
			String uniqueName = c.getId().toString();
			Attribute att = attributes.get(attNumber++);
			inst.setValue(att, uniqueName);
		}
		instances.add(inst);
		cellToInstanceMap.put(inst, c.getId());
		fireProgressEvent();
	}

	private void addNucleus(ICell c, Nucleus n, ArrayList<Attribute> attributes,
			Instances instances, Mesh template,
			double windowProportion)
			throws ProfileException, MeshCreationException, MissingComponentException {

		if (options.getBoolean(HashOptions.CLUSTER_USE_TSNE_KEY)) {
			addTsneNucleus(c, n, attributes, instances);
			return;
		}

		if (options.getBoolean(HashOptions.CLUSTER_USE_PCA_KEY)) {
			addPCANucleus(c, n, attributes, instances);
			return;
		}

		int attNumber = 0;
		Instance inst = new SparseInstance(attributes.size());

		int pointsToSample = (int) Math.floor(1d / windowProportion);

		for (ProfileType t : ProfileType.displayValues()) {

			if (options.getBoolean(t.toString())) {
				LOGGER.finer("Adding attribute for " + t.toString());
				// Interpolate the profile to the median length
				IProfile p = n.getProfile(t, OrientationMark.REFERENCE);

				for (int i = 0; i < pointsToSample; i++) {
					Attribute att = attributes.get(attNumber);
					inst.setValue(att, p.get(i * windowProportion));
					attNumber++;
				}
			}
		}

		for (Measurement stat : Measurement.getNucleusStats()) {
			if (options.getBoolean(stat.toString())) {
				Attribute att = attributes.get(attNumber++);

				if (Measurement.VARIABILITY.equals(stat)) {
					inst.setValue(att, collection
							.getNormalisedDifferenceToMedian(OrientationMark.REFERENCE, n));
				} else {
					inst.setValue(att, n.getMeasurement(stat, MeasurementScale.MICRONS));
				}

			}
		}

		for (Measurement stat : Measurement.getGlcmStats()) {
			if (options.getBoolean(stat.toString())) {
				Attribute att = attributes.get(attNumber++);
				inst.setValue(att, n.getMeasurement(stat, MeasurementScale.MICRONS));
			}
		}

		for (UUID segID : getSegmentIds()) {
			if (options.getBoolean(segID.toString())) {
				Attribute att = attributes.get(attNumber++);
				double length = n.getProfile(ProfileType.ANGLE).getSegment(segID).length();
				inst.setValue(att, length);
			}
		}

		if (options.getBoolean(HashOptions.CLUSTER_INCLUDE_MESH_KEY) && collection.hasConsensus()) {

			Mesh mesh = new DefaultMesh(n, template);
			for (MeshFace face : mesh.getFaces()) {
				Attribute att = attributes.get(attNumber++);
				inst.setValue(att, face.getArea());
			}
		}

		if (ClusteringMethod.from(options).equals(ClusteringMethod.HIERARCHICAL)) {
			String uniqueName = c.getId().toString();
			Attribute att = attributes.get(attNumber++);
			inst.setValue(att, uniqueName);
		}

		instances.add(inst);
		cellToInstanceMap.put(inst, c.getId());
		fireProgressEvent();
	}

}
