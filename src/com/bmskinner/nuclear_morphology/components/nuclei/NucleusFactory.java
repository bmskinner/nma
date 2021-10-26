/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nuclear_morphology.components.nuclei;

import java.awt.Rectangle;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;

import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.FloatPolygon;

/**
 * Constructs nuclei for an image. Tracks the number of nuclei created.
 * 
 * @author ben
 *
 */
public class NucleusFactory {
	
	private static final Logger LOGGER = Logger.getLogger(NucleusFactory.class.getName());
	private int nucleusCount = 0; // store the number of nuclei  created by this factory
	
	private final RuleSetCollection rsc;
	
	private final double windowProp;
	private final double scale;
	
	/**
	 * Builder for nuclei, using global parameters
	 * from the enclosing factory
	 * @author ben
	 * @since 2.0.0
	 *
	 */
	public class NucleusBuilder {
		
		private Roi roi = null;
		private File file = null;
		private int channel = -1;
		private IPoint com = null;
		private UUID id = null;
		private int[] original = null;
		private int count = -1;
		private boolean isOffset = false; 
		private Map<Measurement, Double> measures = new HashMap<>();
		
		public NucleusBuilder() {}
		
		public NucleusBuilder fromRoi(Roi r) {
			roi = r;
			return this;
		}
		
		public NucleusBuilder fromPoints(List<IPoint> points) {
			roi = makRoi(points);
			return this;
		}
		
		public NucleusBuilder withFile(File f) {
			file = f;
			return this;
		}
		
		public NucleusBuilder withChannel(int i) {
			channel = i;
			return this;
		}
		
		public NucleusBuilder withCoM(IPoint i) {
			com = i;
			return this;
		}
		
		public NucleusBuilder withId(UUID u) {
			id = u;
			return this;
		}
		
		public NucleusBuilder withOriginalPos(int[] pos) {
			original = pos;
			return this;
		}
		
		public NucleusBuilder withNumber(int i) {
			count = i;
			return this;
		}
				
		public  NucleusBuilder withMeasurement(Measurement m, double s) {
			measures.put(m, s);
			return this;
		}
		
		public NucleusBuilder offsetToOrigin() {
			isOffset = true;
			return this;
		}
		
		public Nucleus build() throws ComponentCreationException {
			 Rectangle bounds = roi.getBounds();

			 if(original==null)
		        original = new int[]{ (int) roi.getXBase(), (int) roi.getYBase(), (int) bounds.getWidth(),
		                (int) bounds.getHeight() };
			 
			 if(id==null)
				 id = UUID.randomUUID();
			 
			 int number = count>-1 ? count : nucleusCount;
			 nucleusCount = count>-1 ? nucleusCount+1 : nucleusCount;
			 
			 Nucleus n = new DefaultNucleus(roi, com, file, channel, original,
					 number, rsc);
			 
			 if(isOffset) {
			        IPoint offsetCoM = IPoint.makeNew(com.getX() - (int) roi.getXBase(), com.getY() - (int) roi.getYBase());
			        n.moveCentreOfMass(offsetCoM);
			 }
			 
			 n.setScale(scale);
			 
			 for(Entry<Measurement, Double> e : measures.entrySet()) {
				 n.setStatistic(e.getKey(), e.getValue());
			 }
			 
			 n.initialise(windowProp);
			 
			 return n;
		}
		
	    private Roi makRoi(List<IPoint> list) {
	        float[] xpoints = new float[list.size()];
	        float[] ypoints = new float[list.size()];

	        for (int i = 0; i < list.size(); i++) {
	            IPoint p = list.get(i);
	            xpoints[i] = (float) p.getX();
	            ypoints[i] = (float) p.getY();
	        }

	        // If the points are closer than 1 pixel, the float polygon smoothing
	        // during object creation may disrupt the border. Ensure the spacing
	        // is corrected to something larger
	        Roi roi = new PolygonRoi(xpoints, ypoints, Roi.POLYGON);
	        FloatPolygon smoothed = roi.getInterpolatedPolygon(2, false);
	        return new PolygonRoi(smoothed.xpoints, smoothed.ypoints, Roi.POLYGON);
	    }
		
	}
	
    /**
     * Create a factory for nuclei of the given type
     * 
     * @param rsc the rulesets to use
     * @param prop the window proportion
     * @param scale the scale
     */
    public NucleusFactory(@NonNull RuleSetCollection rsc, double prop, double scale) {
    	this.rsc = rsc;
    	this.windowProp = prop;
    	this.scale = scale;
    }

    /**
     * Create a nucleus from the given list of points
     * 
     * @param points the border points of the nucleus
     * @param imageFile the image file the nucleus came from
     * @param channel the image channel of the nucleus
     * @param centreOfMass the centre of mass of the nucleus
     * @return a new nucleus of the factory NucleusType
     * @throws ComponentCreationException
     */
//    public Nucleus buildInstance(@NonNull List<IPoint> points, File imageFile, int channel, @NonNull IPoint centreOfMass)
//            throws ComponentCreationException {
//    	if(points.size()<3)
//    		throw new ComponentCreationException("Cannot create a nucleus with a border list of only "+points.size()+" points");
//    	
//        Roi roi = makRoi(points);
//        
//        Rectangle bounds = roi.getBounds();
//
//        int[] original = { (int) roi.getXBase(), (int) roi.getYBase(), (int) bounds.getWidth(),
//                (int) bounds.getHeight() };
//        return buildInstance(roi, imageFile, channel, original, centreOfMass);
//    }
    
//    public Nucleus buildInstance(@NonNull List<IPoint> points, File imageFile, int channel,
//    		@NonNull IPoint centreOfMass, @NonNull UUID id)
//            throws ComponentCreationException {
//    	if(points.size()<3)
//    		throw new ComponentCreationException("Cannot create a nucleus with a border list of only "+points.size()+" points");
//    	
//        Roi roi = makRoi(points);
//        
//        Rectangle bounds = roi.getBounds();
//
//        int[] original = { (int) roi.getXBase(), (int) roi.getYBase(), (int) bounds.getWidth(),
//                (int) bounds.getHeight() };
//        return buildInstance(roi, imageFile, channel, original, centreOfMass, id);
//    }


    
//    @Override
//    public Nucleus buildInstance(@NonNull Roi roi, File imageFile, int channel, 
//    		int[] originalPosition, @NonNull IPoint centreOfMass)
//            throws ComponentCreationException {
//    	
//        Nucleus n =  new DefaultNucleus(roi, centreOfMass, imageFile, channel, originalPosition,
//                nucleusCount, rsc);
//
//        nucleusCount++;
//        LOGGER.finer( "Created nucleus with border length "+n.getBorderLength());
//        return n;
//    }

//    @Override
//    public Nucleus buildInstance(@NonNull Roi roi, File imageFile, int channel, 
//    		int[] originalPosition, @NonNull IPoint centreOfMass, UUID id)
//            throws ComponentCreationException {
//        
//        Nucleus n = new DefaultNucleus(roi, centreOfMass, imageFile, channel, originalPosition,
//                nucleusCount, id, rsc);
//
//        nucleusCount++;
//        LOGGER.finer( "Created nucleus with border length "+n.getBorderLength());
//        return n;
//    }
    
//    public Nucleus buildInstance(@NonNull Roi roi, File imageFile, int channel, 
//    		int[] originalPosition, @NonNull IPoint centreOfMass, UUID id, int nucleusNumber) {
//        
//        Nucleus n = new DefaultNucleus(roi, centreOfMass, imageFile, channel, originalPosition,
//    			nucleusNumber, id, rsc);
//
//        LOGGER.finer( "Created nucleus with border length "+n.getBorderLength());
//        return n;
//    }

}
