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
package com.bmskinner.nma.components.options;

import java.io.File;
import java.util.UUID;

import com.bmskinner.nma.analysis.classification.TsneMethod;
import com.bmskinner.nma.analysis.classification.UMAPMethod;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.RuleSetCollection;
import com.bmskinner.nma.core.GlobalOptions;

/**
 * Provides default options types.
 * 
 * @author ben
 * @since 1.13.4
 *
 */
public class OptionsFactory {

	/**
	 * We only use static methods
	 */
	private OptionsFactory() {
	}

	/**
	 * Create the default options type for nucleus detection
	 * 
	 * @param folder the folder to be searched
	 * @return
	 */
	public static OptionsBuilder makeNucleusDetectionOptions() {

		return new OptionsBuilder()
				.withValue(HashOptions.MIN_SIZE_PIXELS, HashOptions.DEFAULT_MIN_NUCLEUS_SIZE)
				.withValue(HashOptions.MAX_SIZE_PIXELS, HashOptions.DEFAULT_MAX_NUCLEUS_SIZE)
				.withValue(HashOptions.MIN_CIRC, HashOptions.DEFAULT_MIN_NUCLEUS_CIRC)
				.withValue(HashOptions.MAX_CIRC, HashOptions.DEFAULT_MAX_NUCLEUS_CIRC)
				.withValue(HashOptions.THRESHOLD, HashOptions.DEFAULT_NUCLEUS_THRESHOLD)
				.withValue(HashOptions.SCALE, GlobalOptions.getInstance().getImageScale())
				.withValue(HashOptions.CHANNEL, HashOptions.DEFAULT_CHANNEL)
				.withValue(HashOptions.IS_NORMALISE_CONTRAST,
						HashOptions.DEFAULT_NORMALISE_CONTRAST)
				.withValue(HashOptions.IS_RGB, HashOptions.DEFAULT_IS_RGB)
				.withValue(HashOptions.IS_USE_WATERSHED, false)
				.withValue(HashOptions.IS_USE_GAP_CLOSING, HashOptions.DEFAULT_IS_USE_GAP_CLOSING)
				.withValue(HashOptions.GAP_CLOSING_RADIUS_INT,
						HashOptions.DEFAULT_GAP_CLOSING_RADIUS)
				.setAll(OptionsFactory.makeCannyOptions().build())
				.setAll(OptionsFactory.makePreprocessingOptions().build());
	}

	/**
	 * Create the default options type for Canny edge detection
	 * 
	 * @return
	 */
	public static OptionsBuilder makeCannyOptions() {
		return new OptionsBuilder()
				.withValue(HashOptions.IS_USE_CANNY, HashOptions.DEFAULT_IS_USE_CANNY)
				.withValue(HashOptions.CANNY_IS_AUTO_THRESHOLD,
						HashOptions.DEFAULT_IS_CANNY_AUTO_THRESHOLD)

				.withValue(HashOptions.CANNY_LOW_THRESHOLD_FLT,
						HashOptions.DEFAULT_CANNY_LOW_THRESHOLD)
				.withValue(HashOptions.CANNY_HIGH_THRESHOLD_FLT,
						HashOptions.DEFAULT_CANNY_HIGH_THRESHOLD)

				.withValue(HashOptions.CANNY_KERNEL_RADIUS_FLT,
						HashOptions.DEFAULT_CANNY_KERNEL_RADIUS)
				.withValue(HashOptions.CANNY_KERNEL_WIDTH_INT,
						HashOptions.DEFAULT_CANNY_KERNEL_WIDTH)

				.withValue(HashOptions.IS_USE_GAP_CLOSING, HashOptions.DEFAULT_IS_USE_GAP_CLOSING)
				.withValue(HashOptions.GAP_CLOSING_RADIUS_INT,
						HashOptions.DEFAULT_GAP_CLOSING_RADIUS)
				.withValue(HashOptions.CANNY_IS_ADD_BORDER,
						HashOptions.DEFAULT_IS_CANNY_ADD_BORDER);
	}

	/**
	 * Create the default options type for image preprocessing
	 * 
	 * @return
	 */
	public static OptionsBuilder makePreprocessingOptions() {
		return new OptionsBuilder()
				.withValue(HashOptions.IS_USE_GAUSSIAN, HashOptions.DEFAULT_USE_GAUSSIAN)
				.withValue(HashOptions.IS_USE_KUWAHARA, HashOptions.DEFAULT_USE_KUWAHARA)
				.withValue(HashOptions.IS_USE_ROLLING_BALL, HashOptions.DEFAULT_USE_ROLLING_BALL)
				.withValue(HashOptions.IS_USE_FLATTENING, HashOptions.DEFAULT_IS_USE_FLATTENNING)
				.withValue(HashOptions.IS_USE_RAISING, HashOptions.DEFAULT_IS_USE_RAISING)
				.withValue(HashOptions.IS_USE_COLOUR_THRESHOLD,
						HashOptions.DEFAULT_IS_USE_COLOUR_THRESHOLD)

				.withValue(HashOptions.KUWAHARA_RADIUS_INT, HashOptions.DEFAULT_KUWAHARA_RADIUS)
				.withValue(HashOptions.FLATTENING_THRESHOLD_INT,
						HashOptions.DEFAULT_FLATTEN_THRESHOLD)
				.withValue(HashOptions.RAISING_THRESHOLD_INT, HashOptions.DEFAULT_RAISE_THRESHOLD);
	}

	/**
	 * Create the default options type for nuclear signal detection
	 * 
	 * @param folder the folder to be searched
	 * @return
	 */
	public static OptionsBuilder makeNuclearSignalOptions() {
		return new OptionsBuilder()
				.withValue(HashOptions.SIGNAL_MAX_FRACTION, HashOptions.DEFAULT_SIGNAL_MAX_FRACTION)
				.withValue(HashOptions.SIGNAL_DETECTION_MODE_KEY,
						HashOptions.DEFAULT_SIGNAL_DETECTION_METHOD.name())
				.withValue(HashOptions.MIN_SIZE_PIXELS, HashOptions.DEFAULT_SIGNAL_MIN_SIZE)
				.withValue(HashOptions.MIN_CIRC, HashOptions.DEFAULT_MIN_CIRC)
				.withValue(HashOptions.MAX_CIRC, HashOptions.DEFAULT_MAX_CIRC)
				.withValue(HashOptions.CHANNEL, HashOptions.DEFAULT_SIGNAL_CHANNEL)
				.withValue(HashOptions.THRESHOLD, HashOptions.DEFAULT_SIGNAL_THRESHOLD)
				.withValue(HashOptions.IS_USE_GAP_CLOSING, false)
				.withValue(HashOptions.GAP_CLOSING_RADIUS_INT,
						HashOptions.DEFAULT_GAP_CLOSING_RADIUS)
				.withValue(HashOptions.SCALE, GlobalOptions.getInstance().getImageScale());
	}

	/**
	 * Create the default options for shell analysis
	 * 
	 * @return
	 */
	public static OptionsBuilder makeShellAnalysisOptions() {
		return new OptionsBuilder()
				.withValue(HashOptions.SHELL_COUNT_INT, HashOptions.DEFAULT_SHELL_COUNT)
				.withValue(HashOptions.SHELL_EROSION_METHOD_KEY,
						HashOptions.DEFAULT_EROSION_METHOD.name());
	}

	/**
	 * Create the default analysis options type
	 * 
	 * @return
	 */
	public static IAnalysisOptions makeAnalysisOptions(RuleSetCollection rsc) {
		IAnalysisOptions op = new DefaultAnalysisOptions();
		op.setRuleSetCollection(rsc);
		return op;
	}

	/**
	 * Create the default analysis options type based on a template
	 * 
	 * @param template the template options
	 * @return
	 */
	public static IAnalysisOptions makeAnalysisOptions(IAnalysisOptions template) {
		return new DefaultAnalysisOptions(template);
	}

	/**
	 * Create the default analysis options for rodent sperm detection
	 * 
	 * @param testFolder the folder of images to analyse
	 * @return the options
	 */
	public static IAnalysisOptions makeDefaultRodentAnalysisOptions(File testFolder) {
		IAnalysisOptions op = makeAnalysisOptions(RuleSetCollection.mouseSpermRuleSetCollection());
		op.setDetectionFolder(CellularComponent.NUCLEUS, testFolder.getAbsoluteFile());
		op.setDetectionOptions(CellularComponent.NUCLEUS,
				OptionsFactory.makeNucleusDetectionOptions().build());
		return op;
	}

	/**
	 * Create the default analysis options for pig sperm detection
	 * 
	 * @param testFolder the folder of images to analyse
	 * @return the options
	 */
	public static IAnalysisOptions makeDefaultPigAnalysisOptions(File testFolder) {
		IAnalysisOptions op = OptionsFactory
				.makeAnalysisOptions(RuleSetCollection.pigSpermRuleSetCollection());
		op.setDetectionFolder(CellularComponent.NUCLEUS, testFolder.getAbsoluteFile());
		HashOptions nop = OptionsFactory.makeNucleusDetectionOptions()
				.withValue(HashOptions.MIN_CIRC, 0.1)
				.withValue(HashOptions.MAX_CIRC, 0.9).build();

		op.setDetectionOptions(CellularComponent.NUCLEUS, nop);
		return op;
	}

	/**
	 * Create the default analysis options for round nucleus detection
	 * 
	 * @param testFolder the folder of images to analyse
	 * @return the options
	 */
	public static IAnalysisOptions makeDefaultRoundAnalysisOptions(File testFolder) {
		IAnalysisOptions op = OptionsFactory
				.makeAnalysisOptions(RuleSetCollection.roundRuleSetCollection());
		op.setDetectionFolder(CellularComponent.NUCLEUS, testFolder.getAbsoluteFile());
		HashOptions nop = OptionsFactory.makeNucleusDetectionOptions()
				.withValue(HashOptions.MIN_CIRC, 0.6)
				.withValue(HashOptions.MAX_CIRC, 0.9).build();

		op.setDetectionOptions(CellularComponent.NUCLEUS, nop);

		op.getProfilingOptions().setBoolean(HashOptions.IS_SEGMENT_PROFILES, false);
		return op;
	}

	/**
	 * Create an instance of the default clustering options using
	 * {@link HashOptions#DEFAULT_CLUSTER_METHOD}
	 * 
	 * @return
	 */
	public static OptionsBuilder makeDefaultClusteringOptions() {
		OptionsBuilder ob = new OptionsBuilder()
				.withValue(HashOptions.CLUSTER_GROUP_ID_KEY, UUID.randomUUID())
				.withValue(HashOptions.CLUSTER_METHOD_KEY,
						HashOptions.DEFAULT_CLUSTER_METHOD.name())
				.withValue(HashOptions.CLUSTER_HIERARCHICAL_METHOD_KEY,
						HashOptions.DEFAULT_HIERARCHICAL_METHOD.name())

				.withValue(HashOptions.CLUSTER_USE_SIMILARITY_MATRIX_KEY,
						HashOptions.DEFAULT_USE_SIMILARITY_MATRIX)
				.withValue(HashOptions.CLUSTER_INCLUDE_MESH_KEY, HashOptions.DEFAULT_INCLUDE_MESH)
				.withValue(HashOptions.CLUSTER_USE_TSNE_KEY, HashOptions.DEFAULT_USE_TSNE)

				.withValue(HashOptions.CLUSTER_EM_ITERATIONS_KEY, HashOptions.DEFAULT_EM_ITERATIONS)
				.withValue(HashOptions.CLUSTER_MANUAL_CLUSTER_NUMBER_KEY,
						HashOptions.DEFAULT_MANUAL_CLUSTER_NUMBER);

		for (Measurement stat : Measurement.getRoundNucleusStats())
			ob = ob.withValue(stat.toString(), false);

		ob = ob.withValue(HashOptions.DEFAULT_PROFILE_TYPE.toString(),
				HashOptions.DEFAULT_INCLUDE_PROFILE);
		return ob;
	}

	/**
	 * Create an instance of the default profile tSNE options
	 * 
	 * @return
	 */
	public static OptionsBuilder makeDefaultTsneOptions() {
		return new OptionsBuilder().withValue(TsneMethod.PERPLEXITY_KEY, 5)
				.withValue(TsneMethod.MAX_ITERATIONS_KEY, 1000)
				.withValue(TsneMethod.PROFILE_TYPE_KEY, ProfileType.ANGLE.toString());
	}

	public static OptionsBuilder makeDefaultUmapOptions() {
		return new OptionsBuilder().withValue(UMAPMethod.N_NEIGHBOUR_KEY, 15)
				.withValue(UMAPMethod.PROFILE_TYPE_KEY, ProfileType.ANGLE.toString())
				.withValue(UMAPMethod.MIN_DISTANCE_KEY, 0.1f);
	}

}
