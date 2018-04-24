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
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.geom.Path2D;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.analysis.signals.shells.ShellAnalysisMethod.ShellAnalysisException;
import com.bmskinner.nuclear_morphology.analysis.signals.shells.ShellDetector.Shell;
import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult.ShrinkType;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;
import com.bmskinner.nuclear_morphology.samples.dummy.DummyRodentSpermNucleus;

import ij.ImagePlus;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

public class ShellDetectorTest {
    
    private ShellDetector sd;
    private Nucleus testNucleus;

    @Before
    public void setUp() throws Exception {
    	testNucleus = createMockNucleus(400);
    }
    
    private Nucleus createMockNucleus(int diameter) throws UnloadableImageException {
    	Roi r = new OvalRoi(50, 50, diameter, diameter);
    	ImageProcessor ip = createRoundImage(r, diameter+100);
    	Nucleus n = mock(Nucleus.class);
    	when(n.toRoi()).thenReturn(r);
    	when(n.toOriginalPolygon()).thenReturn(r.getFloatPolygon());
    	
    	float[] xpoints = r.getFloatPolygon().xpoints;
    	float[] ypoints = r.getFloatPolygon().ypoints;
    	Path2D.Double path = new Path2D.Double();
    	path.moveTo(xpoints[0], ypoints[0]);
    	for(int i=0; i<r.getFloatPolygon().npoints; i++) {
    		path.lineTo(xpoints[i], ypoints[i]);
    	}
    	path.closePath();
    	
    	when(n.toOriginalShape()).thenReturn(path);
    	when(n.getBounds()).thenReturn(r.getBounds());
    	when(n.getImage()).thenReturn(ip);
    	return n;
    }
    
    private ImageProcessor createRoundImage(Roi r, int diameter) {
    	ImageProcessor ip = new ByteProcessor(diameter, diameter);
    	ip.setColor(255);
    	ip.fill(r);
    	return ip;
    }
    
    private void showImage(String title, ImageProcessor ip) throws InterruptedException {
    	ImagePlus img = new ImagePlus(title, ip);
    	img.show();
    	while(img.isVisible()) {
    		Thread.sleep(1000);
    	}
    }

    @Test
    public void testShellDetectorCellularComponentShrinkType() throws ShellAnalysisException, ComponentCreationException {
        
        Nucleus n = new DummyRodentSpermNucleus();
        new ShellDetector(n, ShrinkType.AREA, false);
        fail("Not yet implemented");
    }

    @Test
    public void testShellDetectorCellularComponentIntShrinkType() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetShellsByRadius() throws ComponentCreationException, ShellAnalysisException, ImageImportException, UnloadableImageException, InterruptedException {
    	testGetShells(ShrinkType.RADIUS);
    }
    
    @Test
    public void testGetShellsByArea() throws ComponentCreationException, ShellAnalysisException, ImageImportException, UnloadableImageException, InterruptedException {
    	testGetShells(ShrinkType.AREA);
    }
    
    private void testGetShells(ShrinkType type) throws ComponentCreationException, ShellAnalysisException, ImageImportException, UnloadableImageException, InterruptedException {
    	sd = new ShellDetector(testNucleus, type, false);
    	
    	ImageProcessor ip = testNucleus.getImage();
    	ip.setColor(128);
    	ip.setLineWidth(2);
    	for(Shell s : sd.getShells()) {
    		ip.draw(s.toRoi());
    	}
    	showImage(type.toString(), ip);
    }
    
    @Test
    public void testGetShellsInDecreasingSizeByRadius() throws UnloadableImageException, ShellAnalysisException, InterruptedException {
    	testGetShellsOfDecreasingSize(ShrinkType.RADIUS);
    }
    
    @Test
    public void testGetShellsInDecreasingSizeByArea() throws UnloadableImageException, ShellAnalysisException, InterruptedException {
    	testGetShellsOfDecreasingSize(ShrinkType.AREA);
    }
    
    public void testGetShellsOfDecreasingSize(ShrinkType type) throws UnloadableImageException, ShellAnalysisException, InterruptedException {
    	for(int i=400; i>40; i-=10) {
    		Nucleus n = createMockNucleus(i);
    		sd = new ShellDetector(n, type, false);
    		ImageProcessor ip = n.getImage();
        	ip.setColor(128);
        	ip.setLineWidth(2);
        	for(Shell s : sd.getShells()) {
        		ip.draw(s.toRoi());
        	}
        	showImage(i+": "+type.toString(), ip);
    	}
    	
    }

    @Test
    public void testFindPixelCountPerShellCellularComponentByArea() throws ComponentCreationException, ShellAnalysisException {
    	testFindPixelCountPerShellCellularComponent(ShrinkType.AREA);
    }
    
    @Test
    public void testFindPixelCountPerShellCellularComponentByRadius() throws ComponentCreationException, ShellAnalysisException {
    	testFindPixelCountPerShellCellularComponent(ShrinkType.RADIUS);
    }
    
    private void testFindPixelCountPerShellCellularComponent(ShrinkType type) throws ComponentCreationException, ShellAnalysisException {
    	sd = new ShellDetector(testNucleus, type, false);
        long[] obs = sd.findPixelCounts(testNucleus);
        long[] exp = sd.findPixelCounts();
        assertTrue(testEquals(exp, obs));
    }
    
    @Test
    public void testFindPixelCountPerShellByArea() throws ComponentCreationException, ShellAnalysisException {

        sd = new ShellDetector(testNucleus, ShrinkType.AREA, true);
        long[] obs = sd.findPixelCounts();
        
        long total = sd.getShells().get(0).getPixelCount();
        
        
        long[] exp = {total/5, total/5, total/5, total/5, total/5 };
        testRoughly(exp, obs);
    }

    @Test
    public void testFindPixelCountPerShellRadius() throws ComponentCreationException, ShellAnalysisException {
    	sd = new ShellDetector(testNucleus, ShrinkType.RADIUS, false);
        long[] obs = sd.findPixelCounts();
        long[] exp = {44572, 35388, 24796, 15492, 5428 };
        assertTrue(testEquals(exp, obs));
    }
    
    @Test
    public void testFindPixelIntensityPerShellCellularComponentArea() {
        fail("Not yet implemented");
    }

    @Test
    public void testFindPixelIntensityPerShellCellularComponentRadius() throws ComponentCreationException, ShellAnalysisException {
    	sd = new ShellDetector(testNucleus, ShrinkType.RADIUS, false);
        long[] obs = sd.findPixelIntensities(testNucleus);
        long[] exp = {44572*255, 35388*255, 24796*255, 15492*255, 5428*255 };
        assertTrue(testEquals(exp, obs));
    }

    @Test
    public void testFindPixelIntensityPerShellImageStackInt() {
        fail("Not yet implemented");
    }
    
    private boolean testEquals(long[] exp, long[ ]obs){
        assertEquals(exp.length, obs.length);
        for(int i=0; i<obs.length; i++) {
            assertEquals("Shell "+i,exp[i], obs[i]);
        }
        return Arrays.equals(obs, exp);
    }
    
    private void testRoughly(long[] exp, long[ ]obs){
        assertEquals(exp.length, obs.length);
        for(int i=0; i<obs.length; i++) {
            assertEquals("Shell "+i,exp[i], obs[i], 250);
        }
    }

}
