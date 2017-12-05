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

package analysis;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.bmskinner.nuclear_morphology.analysis.MergeSourceExtractionMethod;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;

public class MergeSourceExtracterTest extends SampleDatasetReader {
    
    public static final String TEST_PATH_1 = SAMPLE_DATASET_PATH + "Merge_of_merge.nmd";
    
    @Test
    public void testMergedDatasetCopiesSegments() {
        
        File f = new File(TEST_PATH_1);
        
        try {
            IAnalysisDataset d = openDataset(f);
            
            System.out.println("Merged dataset segment ids in profile collection:");
            List<UUID> srcPcIds = d.getCollection().getProfileCollection().getSegmentIDs();
            for(UUID id : srcPcIds){
                System.out.println(id);
            }
            
            Iterator<Nucleus> it = d.getCollection().getNuclei().iterator();
            
            System.out.println("Merged dataset segment ids in nuclei:");
            List<UUID> srcNuIds = new ArrayList<>();
            while(it.hasNext()){
                Nucleus n = it.next();
                for(UUID id : n.getProfile(ProfileType.ANGLE).getSegmentIDs()){
                    System.out.println(id);
                    srcNuIds.add(id);
                }
                break;
            }
            
            List<IAnalysisDataset> idsToExtact = new ArrayList<>();
            for(IAnalysisDataset m : d.getAllMergeSources()){
                System.out.println(m.getName());
                if(m.getName().equals("Test 1a - Rodent small")){
                    System.out.println("Found merge source to extract");
                    idsToExtact.add(m);
                }
            }
            
            
            MergeSourceExtractionMethod mse = new MergeSourceExtractionMethod(idsToExtact);
            List<IAnalysisDataset> extracted = mse.call().getDatasets();
            
            System.out.println("Extracted "+extracted.size()+" datasets");
            
            for(IAnalysisDataset m : extracted){
                System.out.println("Extracted dataset segment ids in profile collection");

                List<UUID> dstPcIds = m.getCollection().getProfileCollection().getSegmentIDs();
                assertEquals(srcPcIds.size(), dstPcIds.size());
                for(int i=0; i<dstPcIds.size(); i++){
                    assertEquals("Profile collection segment id match", srcPcIds.get(i), dstPcIds.get(i));
                }
                                
                Iterator<Nucleus> im = m.getCollection().getNuclei().iterator();
                
                System.out.println("Extracted dataset segment ids in nuclei:");
                List<UUID> dstNuIds = new ArrayList<>();
                while(im.hasNext()){
                    Nucleus n = im.next();
                    for(UUID id : n.getProfile(ProfileType.ANGLE).getSegmentIDs()){
                        System.out.println(id);
                        dstNuIds.add(id);
                    }
                    break;
                }
                
                for(int i=0; i<dstPcIds.size(); i++){
                    assertEquals("Nucleus segment id match", srcNuIds.get(i), dstNuIds.get(i));
                }
            }
   
            
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        
    }

}
