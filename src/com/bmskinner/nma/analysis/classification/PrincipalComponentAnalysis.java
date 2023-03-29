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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.DefaultAnalysisResult;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nma.components.Taggable;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.measure.MeasurementScale;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.profiles.IProfile;
import com.bmskinner.nma.components.profiles.MissingLandmarkException;
import com.bmskinner.nma.components.profiles.MissingProfileException;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;

import weka.attributeSelection.PrincipalComponents;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;

/**
 * An implementation of principle component analysis
 * 
 * @author ben
 * @since 1.14.0
 *
 */
public class PrincipalComponentAnalysis extends SingleDatasetAnalysisMethod {

	private static final Logger LOGGER = Logger
			.getLogger(PrincipalComponentAnalysis.class.getName());

	private final HashOptions options;

	public static final String PROPORTION_VARIANCE_KEY = "Variance";

	private final Map<Integer, UUID> nucleusToInstanceMap = new HashMap<>();

	public PrincipalComponentAnalysis(@NonNull IAnalysisDataset dataset,
			@NonNull HashOptions options) {
		super(dataset);
		this.options = options;
	}

	@Override
	public IAnalysisResult call() throws Exception {
		Instances inst = createInstances();
		PrincipalComponents pca = new PrincipalComponents();
		pca.setVarianceCovered(options.getDouble(PROPORTION_VARIANCE_KEY));
		pca.buildEvaluator(inst);

		// Calculate variance for each eigenvector
		double variance = pca.getVarianceCovered();
		LOGGER.fine(() -> "Variance covered: %s".formatted(variance));

		int expectedPcs = 0;

		double[] eigenValues = pca.getEigenValues();

		double totalEigenValues = Arrays.stream(eigenValues).sum();
		double[] varianceExplained = Arrays.stream(eigenValues)
				.map(d -> d / totalEigenValues)
				.sorted()
				.toArray();
		LOGGER.finer(() -> "Variance explained by each eigenvector: %s"
				.formatted(Arrays.toString(varianceExplained)));

		for (int i = 0; i < inst.numInstances(); i++) {
			Instance instance = inst.instance(i);
			Instance converted = pca.convertInstance(instance);
			double[] values = converted.toDoubleArray();
			UUID nucleusId = nucleusToInstanceMap.get(i);
			Optional<Nucleus> nucl = dataset.getCollection().getNucleus(nucleusId);

			if (nucl.isPresent()) {

				Measurement pcn = Measurement.makePrincipalComponentNumber(
						options.getUUID(HashOptions.CLUSTER_GROUP_ID_KEY));

				nucl.get().setMeasurement(pcn, values.length); // Store the number of
																// expected PCs

				for (int pc = 0; pc < values.length; pc++) {
					int readableIndex = pc + 1; // start from PC1, not PC0

					Measurement stat = Measurement.makePrincipalComponent(readableIndex,
							options.getUUID(HashOptions.CLUSTER_GROUP_ID_KEY));
					nucl.get().setMeasurement(stat, values[pc]);
				}
				if (i == 0) {
					expectedPcs = values.length;
					LOGGER.fine("Detected %d PCs".formatted(expectedPcs));
				} else {
					if (values.length != expectedPcs)
						LOGGER.fine("Different number of PCs (%d) to expected (%d)"
								.formatted(values.length, expectedPcs));
				}
			} else
				LOGGER.fine("No nucleus in collection for instance %d with id %s".formatted(i,
						nucleusId));
		}

		options.setInt(HashOptions.CLUSTER_NUM_PCS_KEY, expectedPcs);

		return new DefaultAnalysisResult(dataset);
	}

	private ArrayList<Attribute> createAttributes() {

		double profileWindow = Taggable.DEFAULT_PROFILE_WINDOW_PROPORTION;
		if (dataset.hasAnalysisOptions())
			profileWindow = dataset.getAnalysisOptions().get().getProfileWindowProportion();

		ArrayList<Attribute> attributes = new ArrayList<>();

		for (ProfileType t : ProfileType.displayValues()) {
			if (options.getBoolean(t.toString())) {
				int nProfileAtttributes = (int) Math.floor(1d / profileWindow);
				for (int i = 0; i < nProfileAtttributes; i++) {
					attributes.add(new Attribute(t.toString() + i));
				}
			}
		}

		for (Measurement stat : Measurement.getNucleusStats()) {
			if (options.getBoolean(stat.toString())) {
				Attribute a = new Attribute(stat.toString());
				attributes.add(a);
			}
		}
		return attributes;
	}

	private Instances createInstances() throws ClusteringMethodException {

		double windowProportion = Taggable.DEFAULT_PROFILE_WINDOW_PROPORTION;
		if (dataset.hasAnalysisOptions())// Merged datasets may not have options
			windowProportion = dataset.getAnalysisOptions().get().getProfileWindowProportion();

		ArrayList<Attribute> attributes = createAttributes();

		Instances instances = new Instances(dataset.getName(),
				attributes,
				dataset.getCollection().size());

		for (ICell c : dataset.getCollection()) {
			for (Nucleus n : c.getNuclei()) {
				try {
					addNucleus(n, attributes, instances, windowProportion);
				} catch (MissingLandmarkException
						| MissingProfileException
						| ProfileException e) {
					LOGGER.log(Level.SEVERE, "Unable to add nucleus to instances", e);
				}
			}
		}
		return instances;

	}

	private void addNucleus(Nucleus n, ArrayList<Attribute> attributes, Instances instances,
			double windowProportion) throws MissingLandmarkException, MissingProfileException,
			ProfileException {

		Instance inst = new SparseInstance(attributes.size());

		int attNumber = 0;

		int pointsToSample = (int) Math.floor(1d / windowProportion);

		for (ProfileType t : ProfileType.displayValues()) {
			if (options.getBoolean(t.toString())) {
				IProfile p = n.getProfile(t, OrientationMark.REFERENCE);
				for (int i = 0; i < pointsToSample; i++) {
					Attribute att = attributes.get(i);
					inst.setValue(att, p.get(i * windowProportion));
					attNumber++;
				}
			}
		}

		for (Measurement stat : Measurement.getNucleusStats()) {

			if (options.getBoolean(stat.toString())) {
				Attribute att = attributes.get(attNumber++);
				inst.setValue(att, n.getMeasurement(stat, MeasurementScale.MICRONS));
			}
		}

		instances.add(inst);
		int index = instances.numInstances();
		nucleusToInstanceMap.put(index - 1, n.getID());
		fireProgressEvent();
	}

}
