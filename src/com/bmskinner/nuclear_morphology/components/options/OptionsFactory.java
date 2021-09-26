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
import com.bmskinner.nuclear_morphology.components.nuclei.NucleusType;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions.IDetectionSubOptions;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;

/**
 * Provides default options types.
 * 
 * @author ben
 * @since 1.13.4
 *
 */
public class OptionsFactory {

    /**
     * Create the default options type for nucleus detection
     * 
     * @param folder
     *            the folder to be searched
     * @return
     */
	public static IDetectionOptions makeNucleusDetectionOptions(File folder) {
        return new DefaultNucleusHashDetectionOptions(folder);
    }

    /**
     * Create the default options type for nucleus detection based on a template
     * 
     * @param template
     *            the template options
     * @return
     */
	public static IDetectionOptions makeNucleusDetectionOptions(IDetectionOptions template) {
        return new DefaultNucleusHashDetectionOptions(template);
    }

    /**
     * Create the default options type for Canny edge detection
     * 
     * @return
     */
	public static ICannyOptions makeCannyOptions() {
        return new DefaultCannyHashOptions();
    }

    /**
     * Create the default options type for Canny edge detection based on a
     * template
     * 
     * @param template
     *            the template options
     * @return
     */
	public static ICannyOptions makeCannyOptions(ICannyOptions template) {
        return new DefaultCannyHashOptions(template);
    }

    /**
     * Create the default options type for circle detection
     * 
     * @return
     */
	public static IHoughDetectionOptions makeHoughOptions() {
        return new DefaultHoughOptions();
    }

    /**
     * Create the default options type for circle detection based on a template
     * 
     * @param template
     *            the template options
     * @return
     */
	public static IHoughDetectionOptions makeHoughOptions(IHoughDetectionOptions template) {
        return new DefaultHoughOptions(template);
    }

    /**
     * Create the default options type for image preprocessing
     * 
     * @return
     */
	public static IDetectionSubOptions makePreprocessingOptions() {
        return new PreprocessingOptions();
    }

    /**
     * Create the default options type for nuclear signal detection
     * 
     * @param folder
     *            the folder to be searched
     * @return
     */
	public static INuclearSignalOptions makeNuclearSignalOptions(File folder) {
        return new DefaultNuclearSignalHashOptions(folder);
    }

    /**
     * Create the default options type for nuclear signal detection based on a
     * template
     * 
     * @param template
     *            the template options
     * @return
     */
	public static INuclearSignalOptions makeNuclearSignalOptions(INuclearSignalOptions template) {
        return new DefaultNuclearSignalHashOptions(template);
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
        return op;
    }
    
    /**
     * Create the default analysis options for pig sperm detection
     * @param testFolder the folder of images to analyse
     * @return the options
     */
    public static IAnalysisOptions makeDefaultPigAnalysisOptions(File testFolder) {
    	IAnalysisOptions op = OptionsFactory.makeAnalysisOptions();
        op.setNucleusType(NucleusType.PIG_SPERM);
        
        IDetectionOptions nop = OptionsFactory.makeNucleusDetectionOptions(testFolder);
        nop.setMinCirc(0.1);
        nop.setMaxCirc(0.9);
        
        op.setDetectionOptions(CellularComponent.NUCLEUS, nop);
        return op;
    }
    
    /**
     * Create the default analysis options for round nucleus detection
     * @param testFolder the folder of images to analyse
     * @return the options
     */
    public static IAnalysisOptions makeDefaultRoundAnalysisOptions(File testFolder) {
    	IAnalysisOptions op = OptionsFactory.makeAnalysisOptions();
        op.setNucleusType(NucleusType.ROUND);
        
        IDetectionOptions nop = OptionsFactory.makeNucleusDetectionOptions(testFolder);
        nop.setMinCirc(0.6);
        nop.setMaxCirc(1.0);
        
        op.setDetectionOptions(CellularComponent.NUCLEUS, nop);
        return op;
    }

    /**
     * Create an instance of the default clustering options using
     * {@link IClusteringOptions#DEFAULT_CLUSTER_METHOD}
     * @return
     */
    public static IClusteringOptions makeClusteringOptions() {
        return new DefaultClusteringOptions(IClusteringOptions.DEFAULT_CLUSTER_METHOD);
    }

    /**
     * Create an instance of clustering options based on the given template
     * @param template the tamplate options
     * @return
     */
    public static IClusteringOptions makeClusteringOptions(IClusteringOptions template) {
        return new DefaultClusteringOptions(template);
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

    /**
     * Create default options for cytoplasm detection in neutrophils using
     * colour thresholding
     * 
     * @param folder
     * @return
     * @throws MissingOptionException
     */
    public static IDetectionOptions makeDefaultNeutrophilCytoplasmDetectionOptions(File folder)
            throws MissingOptionException {

    	IDetectionOptions cytoOptions = OptionsFactory.makeNucleusDetectionOptions(folder);

        cytoOptions.setBoolean(IDetectionOptions.IS_USE_WATERSHED, false);
        cytoOptions.setInt(IDetectionOptions.EROSION, IDetectionOptions.DEFAULT_EROSION);
        cytoOptions.setInt(IDetectionOptions.DYNAMIC, IDetectionOptions.DEFAULT_DYNAMIC);

        cytoOptions.setRGB(true);

        cytoOptions.setMinCirc(0);
        cytoOptions.setMaxCirc(1);
        cytoOptions.setMinSize(3000);
        cytoOptions.setMaxSize(12000); // for 20x images
        PreprocessingOptions pre = (PreprocessingOptions) cytoOptions
                .getSubOptions(IDetectionSubOptions.BACKGROUND_OPTIONS);
        pre.setUseColourThreshold(true);
        pre.setHueThreshold(0, 104);
        pre.setSaturationThreshold(0, 50);
        pre.setBrightnessThreshold(142, 255);
        cytoOptions.getCannyOptions().setUseKuwahara(false);
        ;
        cytoOptions.getCannyOptions().setFlattenImage(false);
        cytoOptions.getCannyOptions().setUseCanny(false);
        return cytoOptions;
    }

    /**
     * Create default options for nucleus detection in neutrophils using colour
     * thresholding
     * 
     * @param folder
     * @return
     * @throws MissingOptionException
     */
    public static IDetectionOptions makeDefaultNeutrophilNucleusDetectionOptions(File folder)
            throws MissingOptionException {

    	IDetectionOptions nucleusOptions = OptionsFactory.makeNucleusDetectionOptions(folder);
        nucleusOptions.setInt(IDetectionOptions.TOP_HAT_RADIUS, 20);
        nucleusOptions.setBoolean(IDetectionOptions.IS_USE_WATERSHED, true);
        nucleusOptions.setInt(IDetectionOptions.EROSION, IDetectionOptions.DEFAULT_EROSION);
        nucleusOptions.setInt(IDetectionOptions.DYNAMIC, IDetectionOptions.DEFAULT_DYNAMIC);

        nucleusOptions.setRGB(true);
        nucleusOptions.setThreshold(20);

        nucleusOptions.setMinCirc(0);
        nucleusOptions.setMaxCirc(1);
        nucleusOptions.setMinSize(500);
        nucleusOptions.setMaxSize(3000);
        PreprocessingOptions preN = (PreprocessingOptions) nucleusOptions
                .getSubOptions(IDetectionSubOptions.BACKGROUND_OPTIONS);
        preN.setUseColourThreshold(true);
        preN.setHueThreshold(0, 255);
        preN.setSaturationThreshold(4, 120);
        preN.setBrightnessThreshold(90, 250);
        nucleusOptions.getCannyOptions().setUseKuwahara(false);
        ;
        nucleusOptions.getCannyOptions().setFlattenImage(false);
        nucleusOptions.getCannyOptions().setUseCanny(false);
        return nucleusOptions;

    }
}
