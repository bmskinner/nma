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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Shape;
import java.awt.geom.Path2D;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.signals.shells.ShellAnalysisMethod.ShellAnalysisException;
import com.bmskinner.nuclear_morphology.analysis.signals.shells.ShellDetector.Shell;
import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.Imageable;
import com.bmskinner.nuclear_morphology.components.nuclear.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult.ShrinkType;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.DefaultShellOptions;
import com.bmskinner.nuclear_morphology.io.DatasetExportMethod;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;
import com.bmskinner.nuclear_morphology.io.SampleDatasetReader;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;

import ij.IJ;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

/**
 * Test the shell detector is functioning
 * @author ben
 *
 */
public class ShellDetectorTest {
    
	private static final UUID signalGroup = UUID.fromString("00000000-0000-0000-0000-100000000001");
    private ShellDetector sd;
    private Nucleus testNucleus;
    private INuclearSignal testSignal;

    @Before
    public void setUp() throws Exception {
    	testNucleus = createMockNucleus(400);
    	testSignal = createMockSignal(400, 60);
    }
    
    private Nucleus createMockNucleus(int diameter) throws UnloadableImageException {
    	Roi r = new OvalRoi(50, 50, diameter, diameter);
    	ImageProcessor ip = createRoundImage(r, diameter+100);
    	Nucleus n = mock(Nucleus.class);
    	when(n.getID()).thenReturn(signalGroup);
    	when(n.toRoi()).thenReturn(r);
    	when(n.toPolygon()).thenReturn(r.getFloatPolygon());    
    	when(n.toOriginalPolygon()).thenReturn(r.getFloatPolygon());    	
    	when(n.toShape()).thenReturn(createShape(r));
    	when(n.toOriginalShape()).thenReturn(createShape(r));
    	when(n.getBounds()).thenReturn(r.getBounds());
    	when(n.getImage()).thenReturn(ip);
    	return n;
    }
        
    /**
     * Create a signal
     * @param ndiameter the nucleus diameter
     * @param sDiamater the signal diameter
     * @return
     * @throws UnloadableImageException
     */
    private INuclearSignal createMockSignal(int nDiameter, int sDiameter) throws UnloadableImageException {
    	
    	INuclearSignal s = mock(INuclearSignal.class);
    	Roi signalRoi = new OvalRoi(nDiameter/3-20, nDiameter/3, sDiameter, sDiameter);
    	ImageProcessor ip = createRoundImage(signalRoi, nDiameter+100);
    	when(s.getID()).thenReturn(signalGroup);
    	when(s.toRoi()).thenReturn(signalRoi);
    	when(s.toShape()).thenReturn(createShape(signalRoi));
    	when(s.toOriginalShape()).thenReturn(createShape(signalRoi));
    	when(s.getImage()).thenReturn(ip);
    	return s;
    }
    
    private Shape createShape(Roi r) {
    	float[] xpoints = r.getFloatPolygon().xpoints;
    	float[] ypoints = r.getFloatPolygon().ypoints;
    	Path2D.Double path = new Path2D.Double();
    	path.moveTo(xpoints[0], ypoints[0]);
    	for(int i=0; i<r.getFloatPolygon().npoints; i++) {
    		path.lineTo(xpoints[i], ypoints[i]);
    	}
    	path.closePath();
    	return path;
    }
    
    private ImageProcessor createRoundImage(Roi r, int diameter) {
    	ImageProcessor ip = new ByteProcessor(diameter, diameter);
    	ip.setColor(255);
    	ip.fill(r);
    	return ip;
    }
    
    /**
     * Draw the shells on the source image of the given template 
     * @param template
     * @param sd
     * @return
     * @throws UnloadableImageException
     */
    private ImageProcessor drawShells(Imageable template, ShellDetector sd) throws UnloadableImageException{
    	ImageProcessor ip = template.getImage();
    	ip.setColor(128);
    	ip.setLineWidth(2);
    	for(Shell s : sd.getShells()) {
    		ip.draw(s.toRoi());
    	}
    	return ip;
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
    	ImageProcessor ip = drawShells(testNucleus, sd);
//    	showImage(type.toString(), ip);
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
    		ImageProcessor ip = drawShells(n, sd);
//        	showImage(i+": "+type.toString(), ip);
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
        long[] exp = new long[obs.length];
        Arrays.fill(exp, total/5);
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
    public void testFindPixelIntensityPerShellCellularComponentArea() throws ShellAnalysisException {
    	 sd = new ShellDetector(testNucleus, ShrinkType.AREA, false);
         long[] obs = sd.findPixelIntensities(testNucleus);
         long total = sd.getShells().get(0).getPixelCount();
         long[] exp = new long[obs.length];
         Arrays.fill(exp, (total/5)*255);
         testRoughly(exp, obs);
    }

    @Test
    public void testFindPixelIntensityPerShellCellularComponentRadius() throws ComponentCreationException, ShellAnalysisException {
    	sd = new ShellDetector(testNucleus, ShrinkType.RADIUS, false);
        long[] obs = sd.findPixelIntensities(testNucleus);
        long[] exp = {44572*255, 35388*255, 24796*255, 15492*255, 5428*255 };
        assertTrue(testEquals(exp, obs));
    }
    
    @Test
    public void testFindPixelIntensityPerShellCellularComponentWorksForSignals() throws ComponentCreationException, ShellAnalysisException, UnloadableImageException, InterruptedException {
    	sd = new ShellDetector(testNucleus, ShrinkType.RADIUS, false);
        long[] obs = sd.findPixelIntensities(testSignal);
        ImageProcessor ip = drawShells(testSignal, sd);
//    	showImage("Signals", ip);
        long[] exp = {53040, 565590, 102510, 0, 0 };
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
        
    private class ShellRangeValidator {
        
        public ShellRangeValidator(){
            
        }
        
        public void testValueRangesForRoundNucleusShellDetection(ShrinkType type) throws Exception {
            int maxShells = 10;
            int minShells = 2;
            int maxDiam = 300;
            int minDiam = 10;
            ICell c = mock(ICell.class);
            when(c.getId()).thenReturn(signalGroup);
            
            for(int shell=minShells; shell<=maxShells; shell++) {
                StringBuilder sb = new StringBuilder();
                sb.append("Shells\tDiam\tArea\tDiff\t");
                String s = IntStream.range(0, shell).mapToObj(i-> "Shell_"+i).collect(Collectors.joining("\t"));
                sb.append(s+System.getProperty("line.separator"));
                for(int diam=minDiam; diam<=maxDiam; diam+=2) {  
                    
                    Nucleus n = createMockNucleus(diam);
                    sd = new ShellDetector(n, shell, type, false);
                    long[] counts = sd.findPixelCounts(n);
                    long sum = LongStream.of(counts).sum();
                    sb.append(shell+"\t"+diam+"\t"+sum+"\t"+formatArray(counts, sum)+System.getProperty("line.separator"));
                    System.out.println(shell+"\t"+diam+"\t"+sum+"\t"+formatArray(counts, sum));
                }
                IJ.append(sb.toString(), "test/"+type+"_"+shell+"_shellReport.txt");
            }
            
        }
        
        private String formatArray(long[] obs, long total) {
            StringBuilder sb = new StringBuilder();
            
            double[] obsRatios = getRatios(obs, total);

            double diff = getDifferencesInRatioFromIdeal(obsRatios);   

            sb.append(diff+"\t"+LongStream.of(obs).mapToObj(l->String.valueOf(l)).collect(Collectors.joining("\t")));
            return sb.toString();
        }
        
        private double getDifferencesInRatioFromIdeal(double[] obs){
            double sumDiffs = 0;
            double[] diffs = new double[obs.length];
            double fraction = 1d/obs.length;
            for(int i=0; i<obs.length; i++) {
                diffs[i] = Math.abs( (fraction*i)-obs[i]);
                sumDiffs += diffs[i];
            }
            return sumDiffs;
        }
        
        private double[] getRatios(long[] counts, long sum){
            double[] ratios = new double[counts.length];
            for(int i=0; i<counts.length; i++) {
                ratios[i] = (double)counts[i]/sum;

            }
            return ratios;
        }
        
        private double[] getExpected(int nShells, long sum){
            double[] exp = new double[nShells];
            double expVal = (double) (1d/nShells) *sum;
            Arrays.fill(exp, expVal);
            return exp;
        }

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
