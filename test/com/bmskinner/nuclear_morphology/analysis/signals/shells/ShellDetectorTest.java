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

package com.bmskinner.nuclear_morphology.analysis.signals.shells;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.ComponentTester;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.signals.shells.ShellAnalysisMethod.ShellAnalysisException;
import com.bmskinner.nuclear_morphology.analysis.signals.shells.ShellDetector.Shell;
import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.Imageable;
import com.bmskinner.nuclear_morphology.components.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.components.TestDatasetBuilder.TestComponentShape;
import com.bmskinner.nuclear_morphology.components.nuclear.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult.ShrinkType;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.DefaultShellOptions;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;
import com.bmskinner.nuclear_morphology.io.DatasetExportMethod;
import com.bmskinner.nuclear_morphology.io.SampleDatasetReader;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;

import ij.process.ImageProcessor;

/**
 * Test the shell detector is functioning
 * @author ben
 *
 */
public class ShellDetectorTest extends ComponentTester {

    private ShellDetector sd;
    private Nucleus testNucleus;
    private INuclearSignal testSignal;

    @Override
	@Before
    public void setUp() throws Exception {
    	super.setUp();
    	IAnalysisDataset d = new TestDatasetBuilder(RNG_SEED).cellCount(1)
        		.xBase(50).yBase(50)
        		.baseWidth(200).baseHeight(200)
        		.ofType(NucleusType.ROUND)
        		.withNucleusShape(TestComponentShape.SQUARE)
        		.addSignalsInChannel(0)
        		.build();

    	testNucleus = d.getCollection().streamCells().findFirst().get().getNucleus();
    	testSignal  = testNucleus.getSignalCollection().getSignals(TestDatasetBuilder.RED_SIGNAL_GROUP).get(0);
    	
    }
    
    /**
     * Draw the shells on the source image of the given template 
     * @param template
     * @param sd
     * @return
     * @throws UnloadableImageException
     */
    private static ImageProcessor drawShells(Imageable template, ShellDetector sd) throws UnloadableImageException{
    	ImageProcessor ip = template.getImage();
    	
    	ip.setLineWidth(2);
    	List<Shell> shells = sd.getShells();
    	for(int i=0; i<shells.size(); i++) {
    		Shell s = shells.get(i);
    		ip.setColor(ColourSelecter.getColor(i));
    		ip.draw(s.toRoi());
    	}
    	return ip;
    }
        
    @Test
    public void testGetShellsByRadius() throws Exception {
    	testGetShells(ShrinkType.RADIUS);
    }
    
    @Test
    public void testGetShellsByArea() throws Exception {
    	testGetShells(ShrinkType.AREA);
    }
    
    private void testGetShells(ShrinkType type) throws Exception {
    	sd = new ShellDetector(testNucleus, type, false);
    	ImageProcessor ip = drawShells(testNucleus, sd);
//        ImageViewer.showImage(ip, "Nucleus shells");
    }
    
    @Test
    public void testFindPixelCountPerShellCellularComponentByArea() throws Exception {
    	testFindPixelCountPerShellCellularComponent(ShrinkType.AREA);
    }
    
    @Test
    public void testFindPixelCountPerShellCellularComponentByRadius() throws Exception {
    	testFindPixelCountPerShellCellularComponent(ShrinkType.RADIUS);
    }
    
    private void testFindPixelCountPerShellCellularComponent(ShrinkType type) throws Exception {
    	sd = new ShellDetector(testNucleus, type, false);
        long[] obs = sd.findPixelCounts(testNucleus);
        long[] exp = sd.findPixelCounts();
        assertTrue(testEquals(exp, obs));
    }
        
    @Test
    public void testRealSignalsDetectedInMouseSpermDataset() throws Exception{
        IAnalysisDataset dataset = SampleDatasetReader.openTestMouseSignalsDataset();
        IAnalysisMethod m = new ShellAnalysisMethod(dataset, new DefaultShellOptions());
        m.call();
        IAnalysisMethod s = new DatasetExportMethod(dataset, dataset.getSavePath());
        s.call();
    }
          
    
    private boolean testEquals(long[] exp, long[ ]obs){
        assertEquals(exp.length, obs.length);
        for(int i=0; i<obs.length; i++) {
            assertEquals("Shell "+i,exp[i], obs[i]);
        }
        return Arrays.equals(obs, exp);
    }
    
    /**
     * Test that the values are within 250 of the expected
     * @param exp
     * @param obs
     */
    private void testRoughly(long[] exp, long[ ]obs){
        assertEquals(exp.length, obs.length);
        for(int i=0; i<obs.length; i++) {
            assertEquals("Shell "+i,exp[i], obs[i], 250);
        }
    }

}
