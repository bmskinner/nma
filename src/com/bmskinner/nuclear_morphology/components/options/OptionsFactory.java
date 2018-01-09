/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.components.options;

import java.io.File;

import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions.IMutableClusteringOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions.IDetectionSubOptions;

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
	public static IMutableDetectionOptions makeNucleusDetectionOptions(File folder) {
        return new DefaultNucleusHashDetectionOptions(folder);
    }

    /**
     * Create the default options type for nucleus detection based on a template
     * 
     * @param template
     *            the template options
     * @return
     */
	public static IMutableDetectionOptions makeNucleusDetectionOptions(IDetectionOptions template) {
        return new DefaultNucleusHashDetectionOptions(template);
    }

    /**
     * Create the default options type for Canny edge detection
     * 
     * @return
     */
	public static IMutableCannyOptions makeCannyOptions() {
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
	public static IMutableCannyOptions makeCannyOptions(ICannyOptions template) {
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
	public static IMutableNuclearSignalOptions makeNuclearSignalOptions(File folder) {
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
	public static IMutableNuclearSignalOptions makeNuclearSignalOptions(INuclearSignalOptions template) {
        return new DefaultNuclearSignalHashOptions(template);
    }

    /**
     * Create the default analysis options type
     * 
     * @return
     */
    public static IMutableAnalysisOptions makeAnalysisOptions() {
        return new DefaultAnalysisOptions();
    }

    /**
     * Create the default analysis options type based on a template
     * 
     * @param template
     *            the template options
     * @return
     */
    public static IMutableAnalysisOptions makeAnalysisOptions(IAnalysisOptions template) {
        return new DefaultAnalysisOptions(template);
    }
    
    /**
     * Create the default analysis options for rodent sperm detection
     * @param testFolder the folder of images to analyse
     * @return the options
     */
    public static IMutableAnalysisOptions makeDefaultRodentAnalysisOptions(File testFolder) {
        IMutableAnalysisOptions op = makeAnalysisOptions();
        op.setDetectionOptions(IAnalysisOptions.NUCLEUS, OptionsFactory.makeNucleusDetectionOptions(testFolder));
        return op;
    }
    
    /**
     * Create the default analysis options for pig sperm detection
     * @param testFolder the folder of images to analyse
     * @return the options
     */
    public static IMutableAnalysisOptions makeDefaultPigAnalysisOptions(File testFolder) {
    	IMutableAnalysisOptions op = OptionsFactory.makeAnalysisOptions();
        op.setNucleusType(NucleusType.PIG_SPERM);
        
        IMutableDetectionOptions nop = OptionsFactory.makeNucleusDetectionOptions(testFolder);
        nop.setMinCirc(0.1);
        nop.setMaxCirc(0.9);
        
        op.setDetectionOptions(IAnalysisOptions.NUCLEUS, nop);
        return op;
    }
    
    /**
     * Create the default analysis options for round nucleus detection
     * @param testFolder the folder of images to analyse
     * @return the options
     */
    public static IMutableAnalysisOptions makeDefaulRoundAnalysisOptions(File testFolder) {
    	IMutableAnalysisOptions op = OptionsFactory.makeAnalysisOptions();
        op.setNucleusType(NucleusType.ROUND);
        
        IMutableDetectionOptions nop = OptionsFactory.makeNucleusDetectionOptions(testFolder);
        nop.setMinCirc(0.0);
        nop.setMaxCirc(1.0);
        
        op.setDetectionOptions(IAnalysisOptions.NUCLEUS, nop);
        return op;
    }

    public static IMutableClusteringOptions makeClusteringOptions() {
        return new ClusteringOptions(IClusteringOptions.DEFAULT_CLUSTER_METHOD);
    }

    public static IMutableClusteringOptions makeClusteringOptions(IClusteringOptions template) {
        return new ClusteringOptions(template);
    }

    /**
     * Create default options for cytoplasm detection in neutrophils using
     * colour thresholding
     * 
     * @param folder
     * @return
     * @throws MissingOptionException
     */
    public static IMutableDetectionOptions makeDefaultNeutrophilCytoplasmDetectionOptions(File folder)
            throws MissingOptionException {

        IMutableDetectionOptions cytoOptions = OptionsFactory.makeNucleusDetectionOptions(folder);

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
    public static IMutableDetectionOptions makeDefaultNeutrophilNucleusDetectionOptions(File folder)
            throws MissingOptionException {

        IMutableDetectionOptions nucleusOptions = OptionsFactory.makeNucleusDetectionOptions(folder);
        nucleusOptions.setInt(IMutableDetectionOptions.TOP_HAT_RADIUS, 20);
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

    /**
     * Create default options for neutrophil detection using colour thresholding
     * 
     * @param folder
     * @return
     * @throws MissingOptionException
     */
    public static IMutableAnalysisOptions makeDefaultNeutrophilDetectionOptions(File folder) throws MissingOptionException {

        IMutableAnalysisOptions options = OptionsFactory.makeAnalysisOptions();
        options.setNucleusType(NucleusType.NEUTROPHIL);

        IMutableDetectionOptions cytoOptions = OptionsFactory.makeDefaultNeutrophilCytoplasmDetectionOptions(folder);
        IMutableDetectionOptions nucleusOptions = OptionsFactory.makeDefaultNeutrophilNucleusDetectionOptions(folder);

        options.setDetectionOptions(IAnalysisOptions.NUCLEUS, nucleusOptions);
        options.setDetectionOptions(IAnalysisOptions.CYTOPLASM, cytoOptions);
        return options;

    }
}
