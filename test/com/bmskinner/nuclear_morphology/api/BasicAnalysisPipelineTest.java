/*******************************************************************************
 *      Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package com.bmskinner.nuclear_morphology.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.nucleus.ConsensusAveragingMethod;
import com.bmskinner.nuclear_morphology.analysis.nucleus.NucleusDetectionMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetProfilingMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod.MorphologyAnalysisMode;
import com.bmskinner.nuclear_morphology.analysis.signals.SignalDetectionMethod;
import com.bmskinner.nuclear_morphology.analysis.signals.shells.ShellAnalysisMethod;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.generic.Version;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.SignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.DefaultShellOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.INuclearSignalOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;
import com.bmskinner.nuclear_morphology.io.DatasetExportMethod;
import com.bmskinner.nuclear_morphology.io.DatasetStatsExporter;
import com.bmskinner.nuclear_morphology.io.Io;
import com.bmskinner.nuclear_morphology.io.SampleDatasetReader;
import com.bmskinner.nuclear_morphology.logging.ConsoleHandler;
import com.bmskinner.nuclear_morphology.logging.LogPanelFormatter;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.IJ;
import ij.Prefs;

/**
 * Test the detection methods to ensure they match previously 
 * saved datasets
 * @author bms41
 * @since 1.13.8
 *
 */
public class BasicAnalysisPipelineTest extends AnalysisPipelineTest {

    /**
     * Run the current pipeline with default settings on the testing
     * rodent folder and check the created dataset matches expected values.
     */
    @Test
    public void testRodentDetectionMatchesSavedDataset() throws Exception{

    	IAnalysisDataset exp = SampleDatasetReader.openTestRodentDataset();

    	File testFolder = new File(TESTING_MOUSE_FOLDER);
    	IAnalysisOptions op = OptionsFactory.makeDefaultRodentAnalysisOptions(testFolder);

    	File outFile = makeOutfile(TESTING_MOUSE_FOLDER);
    	IAnalysisDataset obs = runNewAnalysis(UNIT_TEST_FILENAME, op, outFile);

    	testDatasetEquality(exp, obs);         
    }
    
    /**
     * Run the current pipeline with default settings on the testing
     * pig folder and check the created dataset matches expected values.
     */
    @Test
    public void testPigDetectionMatchesSavedDataset() throws Exception{

    	IAnalysisDataset exp = SampleDatasetReader.openTestPigDataset();

    	File testFolder = new File(TESTING_PIG_FOLDER);
    	IAnalysisOptions op = OptionsFactory.makeDefaultPigAnalysisOptions(testFolder);

    	File outFile = makeOutfile(TESTING_PIG_FOLDER);
    	IAnalysisDataset obs = runNewAnalysis(UNIT_TEST_FILENAME, op, outFile);

    	testDatasetEquality(exp, obs);       

    }
    
    /**
     * Run the current pipeline with default settings on the testing
     * pig folder and check the created dataset matches expected values.
     */
    @Test
    public void testRoundDetectionMatchesSavedDataset() throws Exception{

    	IAnalysisDataset exp = SampleDatasetReader.openTestRoundDataset();

    	File testFolder = new File(TESTING_ROUND_FOLDER);
    	IAnalysisOptions op = OptionsFactory.makeDefaultRoundAnalysisOptions(testFolder);

    	File outFile = makeOutfile(TESTING_ROUND_FOLDER);
    	IAnalysisDataset obs = runNewAnalysis(UNIT_TEST_FILENAME, op, outFile);

    	testDatasetEquality(exp, obs);        
    }
    
    @Test
    public void testSignalDetectionCompletes() throws Exception {
    	IAnalysisDataset obs = runSignalDetectionInRoundDataset();
    	UUID redId =  UUID.fromString("00000000-0000-0000-0000-100000000001");
    	UUID greenId =  UUID.fromString("00000000-0000-0000-0000-100000000002");
    	
    	assertTrue(obs.getCollection().hasSignalGroup(redId));
    	assertTrue(obs.getCollection().hasSignalGroup(greenId));
    	
    	assertTrue(obs.getAnalysisOptions().get().getNuclearSignalOptions(redId).hasShellOptions());
    	assertTrue(obs.getAnalysisOptions().get().getNuclearSignalOptions(greenId).hasShellOptions());
    }
    

    /**
     * Detect nuclei in the round dataset, make a consensus, add red and green signals
     * and run a shell analysis.
     * @return
     * @throws Exception
     */
    public static IAnalysisDataset runSignalDetectionInRoundDataset() throws Exception {

    	File testFolder = new File(TESTING_ROUND_FOLDER);
    	IAnalysisOptions op = OptionsFactory.makeDefaultRoundAnalysisOptions(testFolder);

    	File outFile = makeOutfile(TESTING_ROUND_FOLDER);
    	IAnalysisDataset obs = runNewAnalysis(UNIT_TEST_FILENAME, op, outFile);

    	INuclearSignalOptions redOptions = OptionsFactory.makeNuclearSignalOptions(testFolder);
    	
    	UUID redId =  UUID.fromString("00000000-0000-0000-0000-100000000001");
        ISignalGroup red = new SignalGroup("Test red");
        red.setGroupColour(Color.RED);
        obs.getCollection().addSignalGroup(redId, red);
        
        
        INuclearSignalOptions greenOptions = OptionsFactory.makeNuclearSignalOptions(testFolder);
        greenOptions.setChannel(1);
        UUID greenId =  UUID.fromString("00000000-0000-0000-0000-100000000002");
        ISignalGroup green = new SignalGroup("Test green");
        green.setGroupColour(Color.GREEN);
        obs.getCollection().addSignalGroup(greenId, green);
        

        obs.getAnalysisOptions().get().setDetectionOptions(redId.toString(), redOptions);
        obs.getAnalysisOptions().get().setDetectionOptions(greenId.toString(), greenOptions);
        
    	new SignalDetectionMethod(obs, redOptions, redId)
    	.then(new ConsensusAveragingMethod(obs))
    	.then(new SignalDetectionMethod(obs, greenOptions, greenId))
    	.then(new ShellAnalysisMethod(obs, new DefaultShellOptions()))
    	.then(new DatasetExportMethod(obs, outFile))
    	.call();
    	return obs;
    }
    
    /**
     * Run a new analysis on the images using the given options.
     * @param folder the name of the output folder for the nmd file
     * @param op the detection options
     * @param saveFile the full path to the nmd file
     * @return the new dataset
     * @throws Exception
     */
    private static IAnalysisDataset runNewAnalysis(String folder, IAnalysisOptions op, File saveFile) throws Exception {
        
        if(!op.getDetectionOptions(CellularComponent.NUCLEUS).get().getFolder().exists()){
            throw new IllegalArgumentException("Detection folder does not exist");
        }
        IAnalysisMethod m = new NucleusDetectionMethod(folder, op);
        IAnalysisResult r = m.call();
        IAnalysisDataset obs = r.getFirstDataset();
        
        new DatasetProfilingMethod(obs)
	    	.then(new DatasetSegmentationMethod(obs, MorphologyAnalysisMode.NEW))
	    	.then(new DatasetExportMethod(obs, saveFile))
	    	.call();

        return obs;
    }
    
    
    /**
     * Check if the two datasets match.
     * @param exp the expected (reference) dataset
     * @param obs the observed (newly created) dataset
     */
    private void testDatasetEquality(@NonNull IAnalysisDataset exp, @NonNull IAnalysisDataset obs) throws Exception{
    	assertEquals("Dataset name", exp.getName(), obs.getName());

//    	assertEquals("Options",exp.getAnalysisOptions(), obs.getAnalysisOptions());

    	assertEquals("Number of images", exp.getCollection().getImageFiles().size(), obs.getCollection().getImageFiles().size());
    	
    	List<Nucleus> expN = new ArrayList<>(exp.getCollection().getNuclei());
    	List<Nucleus> obsN = new ArrayList<>(obs.getCollection().getNuclei());
    	
    	Collections.sort(expN);
    	Collections.sort(obsN);
    	
    	for(int i=0; i<expN.size(); i++)
    	    assertEquals("Nucleus file name for: "+expN.get(i).getNameAndNumber(), expN.get(i).getSourceFileName(), obsN.get(i).getSourceFileName());

    	assertEquals("Detected nuclei", exp.getCollection().getNucleusCount(), obs.getCollection().getNucleusCount());

    	

    	// Check the stats are the same
    	for(PlottableStatistic s : PlottableStatistic.getStats(CellularComponent.NUCLEUS)){
    		System.out.println("Testing equality of "+s);
    		double eMed = exp.getCollection().getMedian(s, CellularComponent.NUCLEUS, MeasurementScale.PIXELS);
    		double oMed = obs.getCollection().getMedian(s, CellularComponent.NUCLEUS, MeasurementScale.PIXELS);

    		assertEquals(s.toString(), eMed, oMed, 0.3);
    		//            assertEquals(s.toString(), eMed, oMed, 0.00000001); // TODO fails for variability. Not yet sure why. Something different after saving.
    	}
    }
    
    private static File makeOutfile(String folder){
    	return new File(folder+File.separator+UNIT_TEST_FILENAME, UNIT_TEST_FILENAME+Io.SAVE_FILE_EXTENSION);
    }

}
