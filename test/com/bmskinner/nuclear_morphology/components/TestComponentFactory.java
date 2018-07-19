package com.bmskinner.nuclear_morphology.components;

import java.io.File;

import com.bmskinner.nuclear_morphology.analysis.profiles.Profileable;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.DefaultCell;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;
import com.bmskinner.nuclear_morphology.components.nuclei.DefaultNucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;

import ij.gui.PolygonRoi;
import ij.gui.Roi;

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
	 * Create a cell with a square nucleus based at 10, 10 with the given width
	 * @param w
	 * @return
	 * @throws ComponentCreationException
	 */
	public static ICell squareCell(int w) throws ComponentCreationException {
		return rectangularCell(w, w);
	}
	
	/**
	 * Create a cell with a rectangular nucleus based at 10, 10 with the given width and height
	 * @param w
	 * @return
	 * @throws ComponentCreationException
	 */
	public static ICell rectangularCell(int w, int h) throws ComponentCreationException {
		return new DefaultCell(rectangularNucleus(w, h));
	}
	
	/**
	 * Create a cell with a rectangular nucleus
	 * @param w the width of the rectangle
	 * @param h the height of the rectangle
	 * @param xBase the starting x value
	 * @param yBase the starting y value
	 * @return
	 * @throws ComponentCreationException
	 */
	public static ICell rectangularCell(int w, int h, int xBase, int yBase) throws ComponentCreationException {
		return new DefaultCell(rectangularNucleus(w, h, xBase, yBase));
	}
	
	/**
	 * Create a square component based at 10, 10 with the given width
	 * @param w
	 * @return
	 * @throws ComponentCreationException 
	 */
	public static Nucleus squareNucleus(int w) throws ComponentCreationException {
		return rectangularNucleus(w, w);
	}
	
	public static Nucleus rectangularNucleus(int w,int h) throws ComponentCreationException {
		return rectangularNucleus(w, h, DEFAULT_X_BASE, DEFAULT_Y_BASE);
	}
	
	/**
	 * Create a rectangular component based at 10, 10 with the given width 
	 * and height
	 * @param w
	 * @param h
	 * @return
	 * @throws ComponentCreationException 
	 */
	public static Nucleus rectangularNucleus(int w, int h, int xBase, int yBase) throws ComponentCreationException {

		int[] xpoints = new int[(w+h)*2];
		int[] ypoints = new int[(w+h)*2];
				
		for(int i=0; i<(w+h)*2; i++) {
			
			int x = i<=w ? i : i<=w+h ? w : i<=w+h+w ? w+h+w-i: 0;
			xpoints[i] = x+xBase;
			
			int y = i<=w ? 0 : i<=w+h ? i-w : i<=w+h+w ? h : (w+h+w+h-i);
			ypoints[i] = y+yBase;
		}
		
//		for(int i=0; i<(w+h)*2; i++) {
//			System.out.println(xpoints[i]+"\t"+ypoints[i]);
//		}

		Roi roi  = new PolygonRoi(xpoints, ypoints, xpoints.length, Roi.POLYGON);
		IPoint com = IPoint.makeNew(xBase+(w/2), yBase+(h/2));
		int[] position = {xBase, yBase, w, h};		
		File f = new File("empty file");
		Nucleus n = new DefaultNucleus(roi, com, f, 0, position, 0);
		
		// The roi interpolation will smooth corners
		
//		for(IBorderPoint b : n.getBorderList()) {
//			System.out.println(b.toString());
//		}

		n.initialise(Profileable.DEFAULT_PROFILE_WINDOW_PROPORTION);
		n.findPointsAroundBorder();
		return n;
	}

}
