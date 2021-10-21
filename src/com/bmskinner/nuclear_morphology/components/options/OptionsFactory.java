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
package com.bmskinner.nuclear_morphology.components.options;

import java.io.File;

import com.bmskinner.nuclear_morphology.analysis.classification.TsneMethod;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;

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
	private OptionsFactory() {}

    /**
     * Create the default options type for nucleus detection
     * 
     * @param folder the folder to be searched
     * @return
     */
	public static HashOptions makeNucleusDetectionOptions(File folder) {
		
		return new OptionsBuilder()
				.withValue(HashOptions.DETECTION_FOLDER, folder.getAbsolutePath())
				.withValue(HashOptions.MIN_SIZE_PIXELS, HashOptions.DEFAULT_MIN_NUCLEUS_SIZE)
				.withValue(HashOptions.MAX_SIZE_PIXELS, HashOptions.DEFAULT_MAX_NUCLEUS_SIZE)
				.withValue(HashOptions.MIN_CIRC, HashOptions.DEFAULT_MIN_NUCLEUS_CIRC)
				.withValue(HashOptions.MAX_CIRC, HashOptions.DEFAULT_MAX_NUCLEUS_CIRC)
				.withValue(HashOptions.THRESHOLD, HashOptions.DEFAULT_NUCLEUS_THRESHOLD)
				.withValue(HashOptions.SCALE, GlobalOptions.getInstance().getImageScale())
				.withValue(HashOptions.CHANNEL, HashOptions.DEFAULT_CHANNEL)
				.withValue(HashOptions.IS_NORMALISE_CONTRAST, HashOptions.DEFAULT_NORMALISE_CONTRAST)
				.withValue(HashOptions.IS_RGB, HashOptions.DEFAULT_IS_RGB)
				.setAll(OptionsFactory.makeCannyOptions())
				.setAll(OptionsFactory.makePreprocessingOptions())
				.build();
    }

    /**
     * Create the default options type for Canny edge detection
     * 
     * @return
     */
	public static HashOptions makeCannyOptions() {
		
		DefaultOptions d = new DefaultOptions();
		d.setBoolean(HashOptions.IS_USE_CANNY, HashOptions.DEFAULT_IS_USE_CANNY);
		
		d.setBoolean(HashOptions.IS_CANNY_AUTO_THRESHOLD, HashOptions.DEFAULT_IS_CANNY_AUTO_THRESHOLD);

		d.setFloat(HashOptions.CANNY_LOW_THRESHOLD_FLT, HashOptions.DEFAULT_CANNY_LOW_THRESHOLD);
		d.setFloat(HashOptions.CANNY_HIGH_THRESHOLD_FLT, HashOptions.DEFAULT_CANNY_HIGH_THRESHOLD);

		d.setFloat(HashOptions.CANNY_KERNEL_RADIUS_FLT, HashOptions.DEFAULT_CANNY_KERNEL_RADIUS);
		d.setInt(HashOptions.CANNY_KERNEL_WIDTH_INT, HashOptions.DEFAULT_CANNY_KERNEL_WIDTH);

		d.setInt(HashOptions.CANNY_CLOSING_RADIUS_INT, HashOptions.DEFAULT_CANNY_CLOSING_RADIUS);

		d.setBoolean(HashOptions.IS_CANNY_ADD_BORDER, HashOptions.DEFAULT_IS_CANNY_ADD_BORDER);
		
		return d;
    }

    /**
     * Create the default options type for image preprocessing
     * 
     * @return
     */
	public static HashOptions makePreprocessingOptions() {
		DefaultOptions d = new DefaultOptions();
		
		d.setBoolean(HashOptions.IS_USE_GAUSSIAN, HashOptions.DEFAULT_USE_GAUSSIAN);
		d.setBoolean(HashOptions.IS_USE_KUWAHARA, HashOptions.DEFAULT_USE_KUWAHARA);
		d.setBoolean(HashOptions.IS_USE_ROLLING_BALL, HashOptions.DEFAULT_USE_ROLLING_BALL);
		d.setBoolean(HashOptions.IS_USE_FLATTENING, HashOptions.DEFAULT_IS_USE_FLATTENNING);
		d.setBoolean(HashOptions.IS_USE_RAISING, HashOptions.DEFAULT_IS_USE_RAISING);
		d.setBoolean(HashOptions.IS_USE_COLOUR_THRESHOLD, HashOptions.DEFAULT_IS_USE_COLOUR_THRESHOLD);
		
		d.setInt(HashOptions.KUWAHARA_RADIUS_INT, HashOptions.DEFAULT_KUWAHARA_RADIUS);
		d.setInt(HashOptions.FLATTENING_THRESHOLD_INT, HashOptions.DEFAULT_FLATTEN_THRESHOLD);
		d.setInt(HashOptions.RAISING_THRESHOLD_INT, HashOptions.DEFAULT_RAISE_THRESHOLD);
		

//		d.setInt(HashOptions.MIN_HUE, HashOptions.DEFAULT_MIN_HUE);
//		d.setInt(HashOptions.MAX_HUE, HashOptions.DEFAULT_MAX_HUE);
//		d.setInt(HashOptions.MIN_SAT, HashOptions.DEFAULT_MIN_SAT);
//		d.setInt(HashOptions.MAX_SAT, HashOptions.DEFAULT_MAX_SAT);
//		d.setInt(HashOptions.MIN_BRI, HashOptions.DEFAULT_MIN_BRI);
//		d.setInt(HashOptions.MAX_BRI, HashOptions.DEFAULT_MAX_BRI);
		return d;
    }

    /**
     * Create the default options type for nuclear signal detection
     * 
     * @param folder the folder to be searched
     * @return
     */
	public static HashOptions makeNuclearSignalOptions(File folder) {		
		return new OptionsBuilder()
		.withValue(HashOptions.DETECTION_FOLDER, folder.getAbsolutePath())
		.withValue(HashOptions.MAX_FRACTION, HashOptions.DEFAULT_MAX_SIGNAL_FRACTION)
		.withValue(HashOptions.SIGNAL_DETECTION_MODE_KEY, HashOptions.DEFAULT_METHOD.name())
		.withValue(HashOptions.MIN_SIZE_PIXELS, HashOptions.DEFAULT_MIN_SIGNAL_SIZE)
		.withValue(HashOptions.MAX_SIZE_PIXELS, HashOptions.DEFAULT_MAX_SIGNAL_SIZE)
		.withValue(HashOptions.MIN_CIRC, HashOptions.DEFAULT_MIN_CIRC)
		.withValue(HashOptions.MAX_CIRC, HashOptions.DEFAULT_MAX_CIRC)
		.withValue(HashOptions.CHANNEL, HashOptions.DEFAULT_SIGNAL_CHANNEL)
		.withValue(HashOptions.THRESHOLD, HashOptions.DEFAULT_SIGNAL_THRESHOLD)
		.withValue(HashOptions.SCALE, GlobalOptions.getInstance().getImageScale())
		.withValue(HashOptions.IS_RGB, HashOptions.DEFAULT_IS_RGB)
		.withValue(HashOptions.IS_NORMALISE_CONTRAST, HashOptions.DEFAULT_IS_NORMALISE)
		.build();
    }
	
	public static HashOptions makeShellAnalysisOptions() {
		DefaultOptions d = new DefaultOptions();
		d.setInt(HashOptions.SHELL_COUNT_INT, HashOptions.DEFAULT_SHELL_COUNT);
		d.setString(HashOptions.SHELL_EROSION_METHOD_KEY, HashOptions.DEFAULT_EROSION_METHOD.name());
		return d;
	}


    /**
     * Create the default analysis options type
     * 
     * @return
     */
    public static IAnalysisOptions makeAnalysisOptions() {
        return new DefaultAnalysisOptions();
    }

    /**
     * Create the default analysis options type based on a template
     * 
     * @param template
     *            the template options
     * @return
     */
    public static IAnalysisOptions makeAnalysisOptions(IAnalysisOptions template) {
        return new DefaultAnalysisOptions(template);
    }
    
    /**
     * Create the default analysis options for rodent sperm detection
     * @param testFolder the folder of images to analyse
     * @return the options
     */
    public static IAnalysisOptions makeDefaultRodentAnalysisOptions(File testFolder) {
    	IAnalysisOptions op = makeAnalysisOptions();
        op.setDetectionOptions(CellularComponent.NUCLEUS, OptionsFactory.makeNucleusDetectionOptions(testFolder));
        op.setRuleSetCollection(RuleSetCollection.mouseSpermRuleSetCollection());
        return op;
    }
    
    /**
     * Create the default analysis options for pig sperm detection
     * @param testFolder the folder of images to analyse
     * @return the options
     */
    public static IAnalysisOptions makeDefaultPigAnalysisOptions(File testFolder) {
    	IAnalysisOptions op = OptionsFactory.makeAnalysisOptions();
        
        HashOptions nop = OptionsFactory.makeNucleusDetectionOptions(testFolder);
        nop.setDouble(HashOptions.MIN_CIRC, 0.1);
        nop.setDouble(HashOptions.MAX_CIRC, 0.9);        
        op.setDetectionOptions(CellularComponent.NUCLEUS, nop);
        op.setRuleSetCollection(RuleSetCollection.pigSpermRuleSetCollection());
        return op;
    }
    
    /**
     * Create the default analysis options for round nucleus detection
     * @param testFolder the folder of images to analyse
     * @return the options
     */
    public static IAnalysisOptions makeDefaultRoundAnalysisOptions(File testFolder) {
    	IAnalysisOptions op = OptionsFactory.makeAnalysisOptions();
        
        HashOptions nop = OptionsFactory.makeNucleusDetectionOptions(testFolder);
        nop.setDouble(HashOptions.MIN_CIRC, 0.6);
        nop.setDouble(HashOptions.MAX_CIRC, 1.0);   
        
        op.setDetectionOptions(CellularComponent.NUCLEUS, nop);
        op.setRuleSetCollection(RuleSetCollection.roundRuleSetCollection());
        return op;
    }

    /**
     * Create an instance of the default clustering options using
     * {@link HashOptions#DEFAULT_CLUSTER_METHOD}
     * @return
     */
    public static HashOptions makeDefaultClusteringOptions() {
    	HashOptions o = new DefaultOptions();
    	o.setString(HashOptions.CLUSTER_METHOD_KEY, HashOptions.DEFAULT_CLUSTER_METHOD.name());
		o.setString(HashOptions.CLUSTER_HIERARCHICAL_METHOD_KEY, HashOptions.DEFAULT_HIERARCHICAL_METHOD.name());

		o.setBoolean(HashOptions.CLUSTER_USE_SIMILARITY_MATRIX_KEY, HashOptions.DEFAULT_USE_SIMILARITY_MATRIX);
		o.setBoolean(HashOptions.CLUSTER_INCLUDE_MESH_KEY, HashOptions.DEFAULT_INCLUDE_MESH);
		o.setBoolean(HashOptions.CLUSTER_USE_TSNE_KEY, HashOptions.DEFAULT_USE_TSNE);

		o.setInt(HashOptions.CLUSTER_EM_ITERATIONS_KEY, HashOptions.DEFAULT_EM_ITERATIONS);
		o.setInt(HashOptions.CLUSTER_MANUAL_CLUSTER_NUMBER_KEY, HashOptions.DEFAULT_MANUAL_CLUSTER_NUMBER);

		for (Measurement stat : Measurement.getRoundNucleusStats())
			o.setBoolean(stat.toString(), false);
		
		o.setBoolean(HashOptions.DEFAULT_PROFILE_TYPE.toString(), HashOptions.DEFAULT_INCLUDE_PROFILE);
		return o;
    }
    
    /**
     * Create an instance of the default profile tSNE options
     * @return
     */
    public static HashOptions makeDefaultTsneOptions() {
    	HashOptions options = new DefaultOptions();
    	options.setDouble(TsneMethod.PERPLEXITY_KEY, 5);
    	options.setInt(TsneMethod.MAX_ITERATIONS_KEY, 1000);
    	options.setString(TsneMethod.PROFILE_TYPE_KEY, ProfileType.ANGLE.toString());
    	return options;
    }

}
