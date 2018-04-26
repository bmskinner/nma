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

package com.bmskinner.nuclear_morphology.issues;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import com.bmskinner.nuclear_morphology.analysis.DatasetValidator;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.MergeSourceExtractionMethod;
import com.bmskinner.nuclear_morphology.analysis.nucleus.ConsensusAveragingMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetProfilingMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod.MorphologyAnalysisMode;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.io.DatasetExportMethod;
import com.bmskinner.nuclear_morphology.io.SampleDatasetReader;


/**
 * This is to reproduce and test fixes for issues identified
 * during use
 * @author bms41
 * @since 1.13.8
 *
 */
public class IssueFixes {
    
    private static final String DATASET_FOLDER = "test/com/bmskinner/nuclear_morphology/samples/datasets/";
    private static final String MERGE_DATASET  = "Merge_of_merge.nmd";
    
    /**
     * Original issue:
     * Open a merged dataset file
     * Recover merge source
     * Stats are fine
     * Refold consensus
     * Hook and body remain at -3 (STAT_NOT_CALCULATED)
     * Error updating the stats after consensus refolding.
     * @throws Exception 
     */
    @Test
    public void testHookBodyCalculationIssueFixed() throws Exception{
        
        File f = new File(DATASET_FOLDER, MERGE_DATASET);
        IAnalysisDataset exp = SampleDatasetReader.openDataset(f);
        assertFalse(exp.getCollection().hasConsensus());
        
        // Ensure consensus nuclei are refolded, and save to a temp file
        IAnalysisMethod m = new ConsensusAveragingMethod(exp);
        m.call();
        File tempFile = new File(DATASET_FOLDER, "IssueX.nmd");
        m = new DatasetExportMethod(exp, tempFile);
        m.call();
        
        // Reopen the temp file and check stats
        exp = SampleDatasetReader.openDataset(tempFile);
        assertTrue(exp.getCollection().hasConsensus());

        double body = exp.getCollection().getMedian(PlottableStatistic.BODY_WIDTH, CellularComponent.NUCLEUS, MeasurementScale.PIXELS);
        double hook = exp.getCollection().getMedian(PlottableStatistic.HOOK_LENGTH, CellularComponent.NUCLEUS, MeasurementScale.PIXELS);
        
        assertTrue(body>0);
        assertTrue(hook>0);

        System.out.println(exp.getName());
        System.out.println("Body: "+body);
        System.out.println("Hook: "+hook);
        System.out.println("");
        
        // Extract merge sources and check the merge source stats                
        IAnalysisMethod m2 = new MergeSourceExtractionMethod(new ArrayList<>(exp.getAllMergeSources()));
        IAnalysisResult r  = m2.call();
        List<IAnalysisDataset> sources = r.getDatasets();
        
        for(IAnalysisDataset d : sources){
            
            double dBodyBefore = d.getCollection().getMedian(PlottableStatistic.BODY_WIDTH, CellularComponent.NUCLEUS, MeasurementScale.PIXELS);
            double dHookBefore = d.getCollection().getMedian(PlottableStatistic.HOOK_LENGTH, CellularComponent.NUCLEUS, MeasurementScale.PIXELS);
            
            assertTrue(dBodyBefore>0);
            assertTrue(dHookBefore>0);
            assertFalse(d.getCollection().hasConsensus());
            System.out.println(d.getName());
            
            System.out.println("Before refolding:");
            
            System.out.println("Body: "+dBodyBefore);
            System.out.println("Hook: "+dHookBefore);
         
            // Refold merge source consensus and check stats are the same
            IAnalysisMethod m1 = new ConsensusAveragingMethod(d);
            m1.call();
            assertTrue(d.getCollection().hasConsensus());
            
            
            double dBodyAfter = d.getCollection().getMedian(PlottableStatistic.BODY_WIDTH, CellularComponent.NUCLEUS, MeasurementScale.PIXELS);
            double dHookAfter = d.getCollection().getMedian(PlottableStatistic.HOOK_LENGTH, CellularComponent.NUCLEUS, MeasurementScale.PIXELS);
            
            assertThat(dBodyAfter, is(dBodyBefore));
            assertThat(dHookAfter, is(dHookBefore));

            System.out.println("");
        }

        // Clean up
        tempFile.delete();
        
    }
    
    
    
    @Test
    public void testPigSpermSegmentationIssueFixed() throws Exception{
                
        File okFile = new File(DATASET_FOLDER, "IssuePigOk.nmd");
        File failFile = new File(DATASET_FOLDER, "IssuePigFail.nmd");
        
        assertTrue(okFile.exists());
        assertTrue(failFile.exists());
        
        UUID signalID = UUID.fromString("6738cc78-1b48-4fe8-8d72-15b31b305d7c");
        Map<UUID, File> signalMap = new HashMap<>();
        signalMap.put(signalID, new File(DATASET_FOLDER));

        IAnalysisDataset okDataset = SampleDatasetReader.openDataset(okFile, signalMap);
        
        DatasetValidator v = new DatasetValidator();
        assertFalse(v.validate(okDataset));
        
        okDataset.getCollection().setConsensus(null);
        // Try rerunning the analysis            
        IAnalysisMethod p = new DatasetProfilingMethod(okDataset);
        p.call();
        
        System.out.println("Completed profiling");

        IAnalysisMethod seg = new DatasetSegmentationMethod(okDataset, MorphologyAnalysisMode.NEW);
        seg.call();
        

        for(IAnalysisDataset d : okDataset.getAllChildDatasets()){
            okDataset.getCollection().getProfileManager().copyCollectionOffsets(d.getCollection());
            d.getCollection().setConsensus(null);
        }

        // Refold the consensus
        new ConsensusAveragingMethod(okDataset).call();

        
        boolean ok = v.validate(okDataset);
        for(String s : v.getErrors()){
            System.out.println(s);
        }

        assertTrue(ok);
        
        // Save the fixed file back out
        File saveFile = new File(DATASET_FOLDER, "IssuePigOk_Fixed.nmd");
        new DatasetExportMethod(okDataset, saveFile).call();
        
        
        
    }
    
    @Test
    public void testPigSpermFailSegmentationIssueFixed() throws Exception{

        File failFile = new File(DATASET_FOLDER, "IssuePigFail.nmd");
        UUID signalID = UUID.fromString("e66ae80b-e2bf-480a-ab61-2e0daf0478c7");
        Map<UUID, File> signalMap = new HashMap<>();
        signalMap.put(signalID, new File(DATASET_FOLDER));
        
        IAnalysisDataset failDataset = SampleDatasetReader.openDataset(failFile, signalMap);
        
        DatasetValidator v = new DatasetValidator();
        assertFalse(v.validate(failDataset));
        
        failDataset.getCollection().setConsensus(null);
        // Try rerunning the analysis            
        IAnalysisMethod p = new DatasetProfilingMethod(failDataset);
        p.call();
        
        System.out.println("Completed profiling");

        IAnalysisMethod seg = new DatasetSegmentationMethod(failDataset, MorphologyAnalysisMode.NEW);
        seg.call();
        

        for(IAnalysisDataset d : failDataset.getAllChildDatasets()){
            failDataset.getCollection().getProfileManager().copyCollectionOffsets(d.getCollection());
            d.getCollection().setConsensus(null);
        }

        // Refold the consensus
        new ConsensusAveragingMethod(failDataset).call();

        
        boolean ok = v.validate(failDataset);
        assertFalse(ok);
        for(String s : v.getErrors()){
            System.out.println(s);
        }
        
        for(ICell c : v.getErrorCells()){
            failDataset.getCollection().removeCell(c);
            for(IAnalysisDataset d : failDataset.getAllChildDatasets()){
                d.getCollection().removeCell(c);
            }
        }
        
        ok = v.validate(failDataset);
        assertTrue(ok);
        
        // Save the fixed file back out
        File saveFile = new File(DATASET_FOLDER, "IssuePigFail_Fixed.nmd");
        new DatasetExportMethod(failDataset, saveFile).call();
    }
    
    @Test
    public void testAvianSignalFolderIssueFixed() throws Exception{

    }
}
