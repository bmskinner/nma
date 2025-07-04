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

package com.bmskinner.nma.analysis.signals.shells;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nma.ComponentTester;
import com.bmskinner.nma.TestDatasetBuilder;
import com.bmskinner.nma.TestDatasetBuilder.TestComponentShape;
import com.bmskinner.nma.TestImageDatasetCreator;
import com.bmskinner.nma.analysis.signals.shells.ShellAnalysisMethod.ShellAnalysisException;
import com.bmskinner.nma.analysis.signals.shells.ShellDetector.Shell;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.OptionsFactory;
import com.bmskinner.nma.components.rules.RuleSetCollection;
import com.bmskinner.nma.components.signals.INuclearSignal;
import com.bmskinner.nma.components.signals.IShellResult.ShrinkType;
import com.bmskinner.nma.components.signals.ISignalGroup;
import com.bmskinner.nma.gui.components.ColourSelecter;
import com.bmskinner.nma.io.ImageImporter;
import com.bmskinner.nma.io.SampleDatasetReader;
import com.bmskinner.nma.io.UnloadableImageException;

import ij.process.ImageProcessor;

/**
 * Test the shell detector is functioning
 * @author ben
 *
 */
public class ShellDetectorTest {

    private ShellDetector sd;
    private Nucleus testNucleus;
    private INuclearSignal testSignal;
    
    private static final int OBJECT_WIDTH = 200;
    private static final int OBJECT_HEIGHT = 200;

	@Before
    public void setUp() throws Exception {
    	IAnalysisDataset d = new TestDatasetBuilder(ComponentTester.RNG_SEED).cellCount(1)
        		.xBase(50).yBase(50)
        		.baseWidth(OBJECT_WIDTH).baseHeight(OBJECT_HEIGHT)
        		.withMaxSizeVariation(0)
        		.ofType(RuleSetCollection.roundRuleSetCollection())
        		.withNucleusShape(TestComponentShape.SQUARE)
        		.addSignalsInChannel(0)
        		.build();

    	testNucleus = d.getCollection().streamCells().findFirst().get().getPrimaryNucleus();
    	testSignal  = testNucleus.getSignalCollection().getSignals(TestDatasetBuilder.RED_SIGNAL_GROUP).get(0);
    	
    }
    
    /**
     * Ensure that nested shells values are corrected properly
     * @throws ShellAnalysisException
     */
    @Test
    public void testNestedShellCorrection() throws ShellAnalysisException {
    	sd = new ShellDetector(testNucleus, ShrinkType.RADIUS);
    	long[] testValues = { 10000, 8000, 6000, 4000, 2000 };
    	long[] exp = { 2000, 2000, 2000, 2000, 2000 };
        long[] obs = sd.correctNestedValues(testValues);
        testEquals(exp, obs);
    }
        
    /**
     * Draw the shells on the source image of the given template 
     * @param template
     * @param sd
     * @return
     * @throws UnloadableImageException
     */
    private static ImageProcessor drawShells(CellularComponent template, ShellDetector sd) throws UnloadableImageException{
    	ImageProcessor ip = ImageImporter.importFullImageTo24bitGreyscale(template);
    	
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
    
    /**
     * Test that the correct number of shells are generated
     * with the default settings
     * @param type
     * @throws Exception
     */
    private void testGetShells(ShrinkType type) throws Exception {
    	sd = new ShellDetector(testNucleus, type);
    	assertEquals("Shell count should be default", 
    			ShellDetector.DEFAULT_SHELL_COUNT, 
    			sd.getShells().size());
    }
    
    @Test
    public void testFindPixelCountPerShellCellularComponentByArea() throws Exception {
    	testFindPixelCountPerShellCellularComponent(ShrinkType.AREA);
    }
    
    @Test
    public void testFindPixelCountPerShellCellularComponentByRadius() throws Exception {
    	testFindPixelCountPerShellCellularComponent(ShrinkType.RADIUS);
    }
    
    /**
     * Test that the number of pixels per shell in a cellular component matches the number
     * of pixels per shell in the entire nucleus, when the cellular component is the nucleus
     * @param type the type of shrinking to use to define shells
     * @throws Exception
     */
    private void testFindPixelCountPerShellCellularComponent(ShrinkType type) throws Exception {
    	sd = new ShellDetector(testNucleus, type);
        long[] inComponentPixels = sd.findPixelCounts(testNucleus);
        long totalInComponent = sum(inComponentPixels);

        long[] inNucleusPixels = sd.findPixelCounts();
        long totalInObject = sum(inNucleusPixels);
        
        // Check all pixels in the object are covered by a shell
        assertEquals("Total pixels covered by shells", 
        		OBJECT_HEIGHT*OBJECT_WIDTH, 
        		totalInObject, 
        		OBJECT_HEIGHT*OBJECT_WIDTH*0.01);
        
//         Check all pixels in the component are covered by a shell
        assertEquals("Total pixels covered by shells", 
        		OBJECT_HEIGHT*OBJECT_WIDTH, 
        		totalInComponent, 
        		OBJECT_HEIGHT*OBJECT_WIDTH*0.01);
        
//        ImageViewer.showImage(drawShells(testNucleus, sd), "Shells");

        assertEquals("Same number of pixels in component as object", 
        		totalInObject, 
        		totalInComponent);
                
        assertTrue(testEquals(inNucleusPixels, inComponentPixels));
    }
        
    /**
     * Detect signals in a real image set, and check that shells are 
     * created appropriately
     * @throws Exception
     */
    @Test
    public void testRealSignalsDetectedInMouseSpermDataset() throws Exception {
        IAnalysisDataset dataset = SampleDatasetReader.openTestMouseSignalsDataset();
        for(ISignalGroup s : dataset.getCollection().getSignalGroups()) {
        	s.clearShellResult();
        	assertFalse("Shells should not exist on first open", s.hasShellResult());
        }
        
        assertTrue("Red signal should be present in mouse dataset", dataset.getAnalysisOptions().get().getNuclearSignalGroups().contains(TestImageDatasetCreator.RED_SIGNAL_ID));
        
        new ShellAnalysisMethod(dataset, OptionsFactory.makeShellAnalysisOptions()
        		.withValue(HashOptions.SIGNAL_GROUP_ID, TestImageDatasetCreator.RED_SIGNAL_ID.toString())
        		.build())
        .call();
        
        for(ISignalGroup s : dataset.getCollection().getSignalGroups()) {
        	assertTrue("Shells should be created for "+s.getGroupName(), s.hasShellResult());
        }
    }
          
    
    private boolean testEquals(long[] exp, long[ ]obs){
        assertEquals(exp.length, obs.length);
        for(int i=0; i<obs.length; i++) {
            assertEquals("Shell "+i,exp[i], obs[i]);
        }
        return Arrays.equals(obs, exp);
    }
    
    private long sum(long[] values) {
    	long r = 0;
    	for(long l : values)
    		r+=l;
    	return r;
    }
}
