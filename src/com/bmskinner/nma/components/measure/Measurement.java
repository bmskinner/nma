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
package com.bmskinner.nma.components.measure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.image.GLCM.GLCMParameter;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.io.XmlSerializable;

/**
 * This interface describes statistical measures that can be plotted in charts.
 * 
 * @author Ben Skinner
 *
 */
public interface Measurement extends XmlSerializable, Comparable<Measurement> {

	/**
	 * The names of the measured statistics
	 * 
	 * @author Ben Skinner
	 *
	 */
	public static class Names {

		public static final String AREA = "Area";
		public static final String PERIMETER = "Perimeter";
		public static final String MIN_DIAMETER = "Min diameter";
		public static final String ELLIPTICITY = "Ellipticity";
		public static final String ASPECT = "Aspect ratio";
		public static final String CIRCULARITY = "Circularity";
		public static final String ELONGATION = "Elongation";
		public static final String REGULARITY = "Regularity";
		public static final String VARIABILITY = "Difference from median";
		public static final String BOUNDING_HEIGHT = "Bounding height";
		public static final String BOUNDING_WIDTH = "Bounding width";
		public static final String OP_RP_ANGLE = "Angle between reference points";
		public static final String HOOK_LENGTH = "Length of hook";
		public static final String BODY_WIDTH = "Width of body";
		public static final String PATH_LENGTH = "Path length";
		public static final String CELL_NUCLEUS_COUNT = "Nuclei per cell";
		public static final String CELL_NUCLEAR_AREA = "Nuclear area";
		public static final String CELL_NUCLEAR_RATIO = "Nucleus : Cytoplasm area ratio";
		public static final String NUCLEUS_SIGNAL_COUNT = "Signals per nucleus";
		public static final String ANGLE = "Angle";
		public static final String DISTANCE_FROM_COM = "Distance from CoM";
		public static final String FRACT_DISTANCE_FROM_COM = "Fractional distance from CoM";
		public static final String RADIUS = "Radius";
		public static final String LENGTH = "Length";
		public static final String DISPLACEMENT = "Displacement";
		public static final String TSNE = "TSNE";
		@Deprecated
		public static final String TSNE_1 = "TSNE_1";
		@Deprecated
		public static final String TSNE_2 = "TSNE_2";
		public static final String UMAP = "UMAP";
		@Deprecated
		public static final String UMAP_1 = "UMAP_1";
		@Deprecated
		public static final String UMAP_2 = "UMAP_2";
		@Deprecated
		public static final String PC = "PC";
		public static final String PCA = "Principal_components";
		public static final String PCA_N = "Number of PCs";
		@Deprecated
		public static final String PCA_1 = "PC1";
		@Deprecated
		public static final String PCA_2 = "PC2";
		public static final String PIXEL_HISTOGRAM = "Pixel_histogram";

	}

	// General component statistics
	@NonNull
	Measurement AREA = new DefaultMeasurement(Names.AREA, MeasurementDimension.AREA);
	@NonNull
	Measurement PERIMETER = new DefaultMeasurement(Names.PERIMETER, MeasurementDimension.LENGTH);
	@NonNull
	Measurement MIN_DIAMETER = new DefaultMeasurement(Names.MIN_DIAMETER,
			MeasurementDimension.LENGTH);
	@NonNull
	Measurement ELLIPTICITY = new DefaultMeasurement(Names.ELLIPTICITY, MeasurementDimension.NONE);
	@NonNull
	Measurement ASPECT = new DefaultMeasurement(Names.ASPECT, MeasurementDimension.NONE);
	@NonNull
	Measurement CIRCULARITY = new DefaultMeasurement(Names.CIRCULARITY, MeasurementDimension.NONE);
	@NonNull
	Measurement VARIABILITY = new DefaultMeasurement(Names.VARIABILITY, MeasurementDimension.NONE);
	@NonNull
	Measurement ELONGATION = new DefaultMeasurement(Names.ELONGATION, MeasurementDimension.NONE);
	@NonNull
	Measurement REGULARITY = new DefaultMeasurement(Names.REGULARITY, MeasurementDimension.NONE);
	@NonNull
	Measurement BOUNDING_HEIGHT = new DefaultMeasurement(Names.BOUNDING_HEIGHT,
			MeasurementDimension.LENGTH);
	@NonNull
	Measurement BOUNDING_WIDTH = new DefaultMeasurement(Names.BOUNDING_WIDTH,
			MeasurementDimension.LENGTH);
	@NonNull
	Measurement HOOK_LENGTH = new DefaultMeasurement(Names.HOOK_LENGTH,
			MeasurementDimension.LENGTH);
	@NonNull
	Measurement BODY_WIDTH = new DefaultMeasurement(Names.BODY_WIDTH, MeasurementDimension.LENGTH);

	// Stats for the whole cell, aggregated across sub-components
	@NonNull
	Measurement CELL_NUCLEUS_COUNT = new DefaultMeasurement(Names.CELL_NUCLEUS_COUNT,
			MeasurementDimension.NONE);
	@NonNull
	Measurement CELL_NUCLEAR_AREA = new DefaultMeasurement(Names.CELL_NUCLEAR_AREA,
			MeasurementDimension.AREA);
	@NonNull
	Measurement CELL_NUCLEAR_RATIO = new DefaultMeasurement(Names.CELL_NUCLEAR_RATIO,
			MeasurementDimension.NONE);

	// Signal count in nuclei
	@NonNull
	Measurement NUCLEUS_SIGNAL_COUNT = new DefaultMeasurement(Names.NUCLEUS_SIGNAL_COUNT,
			MeasurementDimension.NONE);

	/**
	 * The angle of the signal, calculated clockwise from a points directly below
	 * the centre of mass of the nucleus
	 */
	@NonNull
	Measurement ANGLE = new DefaultMeasurement(Names.ANGLE, MeasurementDimension.ANGLE);
	@NonNull
	Measurement DISTANCE_FROM_COM = new DefaultMeasurement(Names.DISTANCE_FROM_COM,
			MeasurementDimension.LENGTH);
	@NonNull
	Measurement FRACT_DISTANCE_FROM_COM = new DefaultMeasurement(Names.FRACT_DISTANCE_FROM_COM,
			MeasurementDimension.NONE);
	@NonNull
	Measurement RADIUS = new DefaultMeasurement(Names.RADIUS, MeasurementDimension.LENGTH);
	@NonNull
	Measurement LENGTH = new DefaultMeasurement(Names.LENGTH, MeasurementDimension.LENGTH);
	@NonNull
	Measurement DISPLACEMENT = new DefaultMeasurement(Names.DISPLACEMENT,
			MeasurementDimension.ANGLE);

	// Special stats. These should not be included in default charts - they are used
	// as hidden data stores
	@NonNull
	@Deprecated
	Measurement TSNE_1 = new DefaultMeasurement(Names.TSNE_1, MeasurementDimension.NONE);
	@NonNull
	@Deprecated
	Measurement TSNE_2 = new DefaultMeasurement(Names.TSNE_2, MeasurementDimension.NONE);

	@NonNull
	@Deprecated
	Measurement UMAP_1 = new DefaultMeasurement(Names.UMAP_1, MeasurementDimension.NONE);
	@NonNull
	@Deprecated
	Measurement UMAP_2 = new DefaultMeasurement(Names.UMAP_2, MeasurementDimension.NONE);

	@NonNull
	@Deprecated
	Measurement PCA_1 = new DefaultMeasurement(Names.PCA_1, MeasurementDimension.NONE);
	@NonNull
	@Deprecated
	Measurement PCA_2 = new DefaultMeasurement(Names.PCA_2, MeasurementDimension.NONE);
	@NonNull
	Measurement PCA_N = new DefaultMeasurement(Names.PCA_N, MeasurementDimension.NONE); // Number of
																						// PCs

	/**
	 * Get stats for the given component. Use the keys in {@link CellularComponent}
	 * 
	 * @param component the component to get stats for
	 * @return applicable stats, or an empty array if the component was not
	 *         recognised
	 */
	static Measurement[] getStats(String component) {
		if (CellularComponent.NUCLEUS.equals(component))
			return getNucleusStats().toArray(new Measurement[0]);
		if (CellularComponent.NUCLEAR_SIGNAL.equals(component))
			return getSignalStats().toArray(new Measurement[0]);
		if (CellularComponent.NUCLEAR_BORDER_SEGMENT.equals(component))
			return getSegmentStats().toArray(new Measurement[0]);
		return new Measurement[0];
	}

	/**
	 * Create a statistic for the number of principal components with a cluster
	 * group
	 * 
	 * @param id a cluster group id
	 * @return the stat for the component
	 * @deprecated since 2.3.0 to use array measurements instead. Retained to allow
	 *             conversion of datasets
	 */
	@Deprecated
	static Measurement makePrincipalComponentNumber(UUID id) {
		return new DefaultMeasurement(Names.PCA_N + "_" + id, MeasurementDimension.NONE);
	}

	/**
	 * Create a measurement for a principal component with a cluster group
	 * 
	 * @param pc the principal component
	 * @param id a group id
	 * @return the measurement for the component
	 * @deprecated since 2.3.0 to use array measurements instead. Retained to allow
	 *             conversion of datasets
	 */
	@Deprecated
	static Measurement makePrincipalComponent(int pc, UUID id) {
		return new DefaultMeasurement(Names.PC + pc + "_" + id, MeasurementDimension.NONE);
	}

	/**
	 * Create a measurement for principal components within a cluster group
	 * 
	 * @param clusterGroupId a cluster group id
	 * @return the measurement for the component
	 */
	static Measurement makePrincipalComponent(UUID clusterGroupId) {
		return new ArrayMeasurement(Names.PCA + "_" + clusterGroupId, MeasurementDimension.NONE);
	}

	/**
	 * Create a measurement for the given tSNE dimension and cluster id
	 * 
	 * @param dim
	 * @param id
	 * @return
	 */
	@Deprecated
	static Measurement makeTSNE(int dim, UUID id) {
		return new DefaultMeasurement(Names.TSNE + "_" + dim + "_" + id, MeasurementDimension.NONE);
	}

	/**
	 * Create a measurement for the given tSNE cluster id
	 * 
	 * @param id
	 * @return
	 */
	static Measurement makeTSNE(UUID id) {
		return new ArrayMeasurement(Names.TSNE + "_" + id, MeasurementDimension.NONE);
	}

	/**
	 * Create a measurement for the given UMAP dimension and cluster id
	 * 
	 * @param dim
	 * @param id
	 * @return
	 */
	@Deprecated
	static Measurement makeUMAP(int dim, UUID id) {
		return new DefaultMeasurement(Names.UMAP + "_" + dim + "_" + id, MeasurementDimension.NONE);
	}

	/**
	 * Create a measurement for the given UMAP cluster id
	 * 
	 * @param id
	 * @return
	 */
	static Measurement makeUMAP(UUID id) {
		return new DefaultMeasurement(Names.UMAP + "_" + id, MeasurementDimension.NONE);
	}


	/**
	 * Create a measurement for a level of the pixel histogram.
	 * 
	 * @param object  the name of the object being measured
	 * @return
	 */
	static Measurement makeImageHistogram(String objectName) {
		return new ArrayMeasurement(Names.PIXEL_HISTOGRAM + "_" + objectName,
				MeasurementDimension.NONE);
	}

	/**
	 * All available stats
	 * 
	 * @return
	 */
	static List<Measurement> getAllStatsTypes() {
		final List<Measurement> list = new ArrayList<>();
		list.add(AREA);
		list.add(PERIMETER);
		list.add(MIN_DIAMETER);
		list.add(ELLIPTICITY);
		list.add(ASPECT);
		list.add(CIRCULARITY);
		list.add(VARIABILITY);
		list.add(ELONGATION);
		list.add(REGULARITY);
		list.add(BOUNDING_HEIGHT);
		list.add(BOUNDING_WIDTH);
		list.add(HOOK_LENGTH);
		list.add(BODY_WIDTH);
		list.add(CELL_NUCLEUS_COUNT);
		list.add(CELL_NUCLEAR_AREA);
		list.add(CELL_NUCLEAR_RATIO);
		list.add(NUCLEUS_SIGNAL_COUNT);
		list.add(ANGLE);
		list.add(DISTANCE_FROM_COM);
		list.add(FRACT_DISTANCE_FROM_COM);
		list.add(RADIUS);
		list.add(LENGTH);
		list.add(DISPLACEMENT);

		return list;
	}

	/**
	 * Fetch the stat with the given name, if available.
	 * 
	 * @param name the name of the stat
	 * @return the stat
	 */
	static Measurement of(String name) {

		final List<Measurement> all = getAllStatsTypes();

		for (final Measurement stat : all) {
			if (stat.name().equals(name))
				return stat;
		}
		return new DefaultMeasurement(name, MeasurementDimension.NONE);
	}

	/**
	 * Get stats for generic cellular components.
	 * 
	 * @return
	 */
	static List<Measurement> getComponentStats() {
		final List<Measurement> list = new ArrayList<>();
		list.add(AREA);
		list.add(PERIMETER);
		list.add(CIRCULARITY);
		return list;
	}

	/**
	 * Get measurement types that are active for a whole cell.
	 * 
	 * @return
	 */
	static List<Measurement> getCellStats() {
		final List<Measurement> list = new ArrayList<>();
//		list.add(CELL_NUCLEUS_COUNT);
//		list.add(CELL_NUCLEAR_AREA);
//		list.add(CELL_NUCLEAR_RATIO);
		return list;
	}

	/**
	 * Get default type of nucleus stats; these are for mouse sperm nuclei
	 * 
	 * @return
	 */
	static List<Measurement> getNucleusStats() {
		return getRodentSpermNucleusStats();
	}

	static List<Measurement> getRoundNucleusStats() {

		final List<Measurement> list = getComponentStats();
		list.add(MIN_DIAMETER);
		list.add(ELLIPTICITY);
		list.add(ASPECT);
		list.add(ELONGATION);
		list.add(REGULARITY);
		list.add(VARIABILITY);
		list.add(BOUNDING_HEIGHT);
		list.add(BOUNDING_WIDTH);
		return list;
	}

	static List<Measurement> getGlcmStats() {
		final List<Measurement> list = new ArrayList<>();
		Collections.addAll(list, GLCMParameter.toStats());
		return list;
	}

	/**
	 * Get the measurement keys for each grey level of an image histogram
	 * 
	 * @return the 256 grey level measurements
	 * @deprecated since 2.3.0 to use array measurements instead. Retained to allow
	 *             conversion of datasets
	 */
	@Deprecated
	static List<Measurement> getPixelHistogramMeasurements(int channel) {
		final List<Measurement> list = new ArrayList<>();
		for (int i = 0; i < 256; i++) {
			list.add(Measurement.makePixelHistogram(channel, i));
		}
		return list;
	}

	/**
	 * Create a measurement for a pixel count at a given level
	 * 
	 * @param channel the image RGB channel
	 * @param dim     the pixel value (0-255)
	 * @return
	 * @deprecated since 2.3.0 to use array measurements instead. Retained to allow
	 *             conversion of datasets
	 */
	@Deprecated
	static Measurement makePixelHistogram(int channel, int dim) {
		return new DefaultMeasurement(Names.PIXEL_HISTOGRAM + "_" + dim + "_channel_" + channel,
				MeasurementDimension.NONE);
	}

	/**
	 * Get stats for rodent sperm nuclei
	 * 
	 * @return
	 */
	static List<Measurement> getRodentSpermNucleusStats() {
		final List<Measurement> list = getRoundNucleusStats();
		list.add(HOOK_LENGTH);
		list.add(BODY_WIDTH);
		return list;
	}

	/**
	 * Get stats for nuclear signals
	 * 
	 * @return
	 */
	static List<Measurement> getSignalStats() {
		final List<Measurement> list = getComponentStats();
		list.add(ANGLE);
		list.add(DISTANCE_FROM_COM);
		list.add(FRACT_DISTANCE_FROM_COM);
		list.add(RADIUS);
		return list;
	}

	/**
	 * Get stats for nuclear border segments
	 * 
	 * @return
	 */
	static List<Measurement> getSegmentStats() {
		final List<Measurement> list = new ArrayList<>(2);
		list.add(LENGTH);
		return list;
	}

	/**
	 * Get the nuclear measurements in common between the given datasets
	 * 
	 * @param datasets
	 * @return
	 */
	static List<Measurement> commonMeasurements(List<IAnalysisDataset> datasets) {
		final List<Measurement> result = new ArrayList<>();

		// Get measurements in first dataset
		final Set<Measurement> d1 = datasets.get(0).getCollection().getRuleSetCollection()
				.getMeasurableValues();
		for (final Measurement m : d1) {
			// Keep if they are in all other datasets
			if (datasets.stream()
					.allMatch(d -> d.getCollection().getRuleSetCollection().getMeasurableValues()
							.contains(m))) {
				result.add(m);
			}
		}
		return result;
	}

	/**
	 * Get the name of the stat
	 * 
	 * @return
	 */
	String name();

	/**
	 * Test if the statistic has units
	 * 
	 * @return
	 */
	boolean isDimensionless();

	/**
	 * Test if the statistic is an angle
	 * 
	 * @return
	 */
	boolean isAngle();

	/**
	 * Test if this measurement is associated with an array of values
	 * 
	 * @return
	 */
	boolean isArrayMeasurement();

	/**
	 * Get the dimension of the statistic (area, length, angle, none)
	 * 
	 * @return
	 */
	MeasurementDimension getDimension();

	/**
	 * Get the label (name and units) for the stat
	 * 
	 * @return
	 */
	String label(MeasurementScale scale);

	/**
	 * Convert the input value (assumed to be pixels) using the given factor (
	 * Nucleus.getScale() ) into the appropriate scale
	 * 
	 * @param value  the pixel measure
	 * @param factor the conversion factor to microns
	 * @param scale  the desired scale
	 * @return
	 */
	double convert(double value, double factor, MeasurementScale scale);

	/**
	 * Convert the input value (assumed to be pixels) using the given factor (
	 * Nucleus.getScale() ) into the appropriate scale
	 * 
	 * @param value  the pixel measure
	 * @param factor the conversion factor to microns
	 * @param scale  the desired scale
	 * @return
	 */
	List<Double> convert(List<Double> value, double factor, MeasurementScale scale);

	/**
	 * Get the appropriate units label for the statistic, based on its dimension.
	 * Eg. square units, units or nothing
	 * 
	 * @param scale
	 * @return
	 */
	String units(MeasurementScale scale);

	/**
	 * Convert the length in pixels into a length in microns. Assumes that the scale
	 * is in pixels per micron
	 * 
	 * @param pixels the number of pixels
	 * @param scale  the size of a pixel in microns
	 * @return
	 */
	static double lengthToMicrons(double pixels, double scale) {
		return pixels / scale;
	}

	/**
	 * Convert the area in pixels into an area in microns. Assumes that the scale is
	 * in pixels per micron
	 * 
	 * @param pixels the number of pixels
	 * @param scale  the size of a pixel in microns
	 * @return
	 */
	static double areaToMicrons(double pixels, double scale) {
		return pixels / (scale * scale);
	}

	/**
	 * Convert the input value (assumed to be pixels) using the given factor (
	 * CellularComponent.getScale() ) into the appropriate scale
	 * 
	 * @param value  the pixel measure
	 * @param factor the conversion factor to microns
	 * @param scale  the desired scale
	 * @param dim    the dimension of the statistic
	 * @return the converted value
	 */
	static double convert(double value, double factor, MeasurementScale scale,
			MeasurementDimension dim) {
		switch (scale) {
		case MICRONS: {
			switch (dim) {
			case AREA:
				return Measurement.areaToMicrons(value, factor);
			case LENGTH:
				return Measurement.lengthToMicrons(value, factor);
			case NONE:
			case ANGLE:
			default:
				return value;
			}
		}

		case PIXELS:
			return value;
		default:
			return value;
		}

	}

	/**
	 * Convert the input value (assumed to be pixels) using the given factor (
	 * CellularComponent.getScale() ) into the appropriate scale
	 * 
	 * @param value  the pixel measure
	 * @param factor the conversion factor to microns
	 * @param scale  the desired scale
	 * @param dim    the dimension of the statistic
	 * @return the converted value
	 */
	static List<Double> convertArray(List<Double> value, double factor, MeasurementScale scale,
			MeasurementDimension dim) {

		final List<Double> result = new ArrayList<>();
		for (final Double d : value) {
			final double r = switch (scale) {
			case MICRONS -> {
				yield switch (dim) {
				case AREA -> Measurement.areaToMicrons(d, factor);
				case LENGTH -> Measurement.lengthToMicrons(d, factor);
				case NONE, ANGLE -> d;
				default -> d;
				};
			}

			case PIXELS -> d;
			default -> d;
			};
			
			result.add(r);
		};
		return result;
	}

	/**
	 * Create a units label for the given scale and dimension
	 * 
	 * @param scale
	 * @param dim
	 * @return
	 */
	static String units(MeasurementScale scale, MeasurementDimension dim) {
		switch (dim) {

		case AREA:
			return "square " + scale.toString().toLowerCase();
		case LENGTH:
			return scale.toString().toLowerCase();
		case ANGLE:
			return "degrees";
		case NONE:
		default:
			return "";
		}
	}

}
