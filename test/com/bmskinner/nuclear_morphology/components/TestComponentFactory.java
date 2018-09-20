package com.bmskinner.nuclear_morphology.components;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.awt.Color;
import java.io.File;
import java.util.UUID;

import org.assertj.swing.util.Arrays;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.DefaultCell;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.DefaultNuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.DefaultSignalCollection;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalCollection;
import com.bmskinner.nuclear_morphology.components.nuclei.DefaultNucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;

import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

/**
 * Construct test components for unit testing
 * @author bms41
 * @since 1.14.0
 *
 */
public class TestComponentFactory {
	
	public static final int DEFAULT_X_BASE = 10;
	public static final int DEFAULT_Y_BASE = 10;
	
	/**
	 * Create a cell with a rectangular nucleus
	 * @param w the width of the rectangle
	 * @param h the height of the rectangle
	 * @param xBase the starting x value
	 * @param yBase the starting y value
	 * @param rotation the rotation of the nuclei
	 * @param fixedStartOffset an offset to apply to the border array
	 * @return
	 * @throws ComponentCreationException
	 */
	public static ICell rectangularCell(int w, int h, int xBase, int yBase, double rotation, int fixedStartOffset) throws ComponentCreationException {
		return new DefaultCell(rectangularNucleus(w, h, xBase, yBase, rotation, fixedStartOffset));
	}
		
	/**
	 * Create a rectangular component based at 10, 10 with the given width 
	 * and height. The border may be drawn from any starting position in the object
	 * @param w
	 * @param h
	 * @param fixedStartOffset an offset to apply to the border array
	 * @return a Nucleus
	 * @throws ComponentCreationException 
	 */
	public static Nucleus rectangularNucleus(int w, int h, int xBase, int yBase, double rotation, int fixedStartOffset) throws ComponentCreationException {

		if(fixedStartOffset<0 || fixedStartOffset>=(w+h)*2)
			throw new ComponentCreationException("Offset cannot be less than zero or more than perimeter: "+fixedStartOffset);
		
		int[] xpoints = new int[(w+h)*2];
		int[] ypoints = new int[(w+h)*2];
				
		for(int i=0; i<(w+h)*2; i++) {
			
			int x = i<=w ? i : i<=w+h ? w : i<=w+h+w ? w+h+w-i: 0;
			xpoints[i] = x+xBase;
			
			int y = i<=w ? 0 : i<=w+h ? i-w : i<=w+h+w ? h : (w+h+w+h-i);
			ypoints[i] = y+yBase;
		}

		// Choose offset so not all objects will start on the RP
			xpoints = offsetArray(xpoints, fixedStartOffset);
			ypoints = offsetArray(ypoints, fixedStartOffset);
				
		Roi roi  = new PolygonRoi(xpoints, ypoints, xpoints.length, Roi.POLYGON);
		IPoint com = IPoint.makeNew(xBase+(w/2), yBase+(h/2));
		int[] position = {xBase, yBase, w, h};		
		File f = new File("empty file");
		Nucleus n = new DefaultNucleus(roi, com, f, 0, position, 0) {
			
			private static final long serialVersionUID = 1L;
			private ISignalCollection overrideCollection = new DefaultSignalCollection() {
				private static final long serialVersionUID = 1L;

				@Override
				    public ImageProcessor getImage(@NonNull final UUID signalGroup) throws UnloadableImageException {
					 if (this.getSignals(signalGroup).size() == 0)
				            throw new UnloadableImageException("No signals in group");
					 return this.getSignals(signalGroup).get(0).getImage();
				 }
			};
			
			@Override
			public ImageProcessor getGreyscaleImage() {
				return getImage();
			}

			@Override
			public ImageProcessor getImage() {
				Roi finalRoi = toOriginalRoi();
				
				int[] position = getPosition(); 
				int xmax = position[Imageable.X_BASE]+position[Imageable.WIDTH]+Imageable.COMPONENT_BUFFER;
				int ymax = position[Imageable.Y_BASE]+position[Imageable.HEIGHT]+Imageable.COMPONENT_BUFFER;
				
				ImageProcessor ip = new ColorProcessor(xmax, ymax);
		    	ip.setColor(Color.WHITE);
		    	ip.fill(finalRoi);
		    	return ip;
			}
			
			@Override
			public ISignalCollection getSignalCollection() {
				return overrideCollection;
			}
			
		};
		
		n.rotate(rotation);
		
		// Note - the roi interpolation will smooth corners
		n.initialise(Profileable.DEFAULT_PROFILE_WINDOW_PROPORTION);
		n.findPointsAroundBorder();		
		return n;
	}
	
	/**
	 * Create a signal within the bounds of the given component, occupying at most
	 * the maxPropotion of the component
	 * @param c the component to create a signal within  
	 * @param maxProportion the maximum proportion of the component area to fill, from 0-1
	 * @param channel the signal channel
	 * @return
	 */
	public static INuclearSignal createSignal(CellularComponent c, double maxProportion, int channel) {
		Roi templateRoi = c.toOriginalRoi();
		
		int w = (int) (templateRoi.getFloatWidth()*maxProportion);
		int h = (int) (templateRoi.getFloatHeight()*maxProportion);
		
		int[] xpoints = new int[(w+h)*2];
		int[] ypoints = new int[(w+h)*2];
		int xBase = (int) templateRoi.getXBase();
		int yBase = (int) templateRoi.getYBase();
		
		for(int i=0; i<(w+h)*2; i++) {
			
			int x = i<=w ? i : i<=w+h ? w : i<=w+h+w ? w+h+w-i: 0;
			xpoints[i] = x+xBase;
			
			int y = i<=w ? 0 : i<=w+h ? i-w : i<=w+h+w ? h : (w+h+w+h-i);
			ypoints[i] = y+yBase;
		}
		
		Roi roi  = new PolygonRoi(xpoints, ypoints, xpoints.length, Roi.POLYGON);
		IPoint com = IPoint.makeNew(xBase+(w/2), yBase+(h/2));
		int[] position = {xBase, yBase, w, h};		
		File f = new File("empty file");
		
		INuclearSignal s = new DefaultNuclearSignal(roi, com, f, channel, position) {
			
			@Override
			public ImageProcessor getGreyscaleImage() {
				return getImage();
			}
			
			@Override
			public ImageProcessor getImage() {
				Roi finalRoi = toOriginalRoi();
				
				int[] position = getPosition(); 
				int xmax = position[Imageable.X_BASE]+position[Imageable.WIDTH]+Imageable.COMPONENT_BUFFER;
				int ymax = position[Imageable.Y_BASE]+position[Imageable.HEIGHT]+Imageable.COMPONENT_BUFFER;
				
				ImageProcessor ip = new ColorProcessor(xmax, ymax);
		    	ip.setColor(Color.WHITE);
		    	ip.fill(finalRoi);
		    	return ip;
			}
		};
		return s;
	}
		
	/**
	 * Offset the int array by the given amount with wrapping
	 * @param array
	 * @param offset the offset. Positive integer only between 0 and array length.
	 * @return
	 */
	private static int[] offsetArray(int[] array, int offset) {
		int[] result = new int[array.length];
		System.arraycopy(array, offset, result, 0, array.length-offset);
		System.arraycopy(array, 0, result, array.length-offset, offset);
		return result;
	}
	
	@Test
	public void testOffsettingWithZeroOffset() {
		int[] arr = { 1, 2, 3, 4, 5 };
		int offset = 0;
		int[] exp = { 1, 2, 3, 4, 5 };
		
		int[] res = offsetArray(arr, offset);
		assertArrayEquals(exp, res);
	}
	
	@Test
	public void testOffsettingWithSingleOffset() {
		int[] arr = { 1, 2, 3, 4, 5 };
		int offset = 1;
		int[] exp = { 2, 3, 4, 5, 1 };
		
		int[] res = offsetArray(arr, offset);
		assertArrayEquals(exp, res);
	}
	
	@Test
	public void testOffsettingWitArrayLengthOffset() {
		int[] arr = { 1, 2, 3, 4, 5 };
		int offset = arr.length;
		int[] exp = { 1, 2, 3, 4, 5 };
		
		int[] res = offsetArray(arr, offset);
		assertArrayEquals(exp, res);
	}

}
