/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
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
package components;

import ij.gui.Roi;
import ij.process.FloatPolygon;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import components.generic.XYPoint;

/**
 * The sperm tail is a specialised type of flagellum. It is anchored at
 * the tail end of the sperm nucleus, and contains a midpiece (with mitochondria
 * attached) and a long thin tail. Cytoplasmic droplets may be present. Imaged
 * tails often overlap themselves and other tails. Common stain - anti-tubulin.
 * @author bms41
 *
 */
public class SpermTail  implements Serializable, CellularComponent, Flagellum {
	
	// indices in  the originalPositions array
		public static final int X_BASE 	= 0;
		public static final int Y_BASE 	= 1;
		public static final int WIDTH 	= 2;
		public static final int HEIGHT 	= 3;
		
		private static final long serialVersionUID = 1L;
		
		protected UUID uuid;
		
		protected File sourceFile;    // the image from which the tail came
		protected int sourceChannel; // the channel in the source image
		
		protected double length; // the length of the skeleton
		protected double[] orignalPosition; // the xbase, ybase, width and height of the original bounding rectangle
		
		protected XYPoint nucleusIntersection;
		
		protected List<XYPoint> skeletonPoints = new ArrayList<XYPoint>(0); 
		protected List<XYPoint> borderPoints   = new ArrayList<XYPoint>(0); 
		
		public SpermTail(File source, int channel, Roi skeleton, Roi border){
			this.uuid = java.util.UUID.randomUUID();
			this.sourceFile = source;
			this.sourceChannel = channel;
			
			this.orignalPosition = new double[] { border.getPolygon().getBounds().getMinX(),
					 border.getPolygon().getBounds().getMinY(),
					 border.getPolygon().getBounds().getWidth(),
					 border.getPolygon().getBounds().getHeight()};
			
			
			FloatPolygon skeletonPolygon = skeleton.getInterpolatedPolygon(1, true);
			for(int i=0; i<skeletonPolygon.npoints; i++){
				skeletonPoints.add(new XYPoint( skeletonPolygon.xpoints[i], skeletonPolygon.ypoints[i]));
			}
			
			FloatPolygon borderPolygon = border.getInterpolatedPolygon(1, true);
			for(int i=0; i<borderPolygon.npoints; i++){
				borderPoints.add(new XYPoint( borderPolygon.xpoints[i], borderPolygon.ypoints[i]));
			}
						
			this.length = skeleton.getLength();
			
		}
		
		public SpermTail(SpermTail t){
			this.uuid = java.util.UUID.randomUUID();
			this.sourceFile = t.getSourceFile();
			this.sourceChannel = t.getSourceChannel();
			
			this.orignalPosition = t.getPosition();
			
			this.borderPoints = t.getBorder();
			this.skeletonPoints = t.getSkeleton();
									
			this.length = t.getLength();
		}
		
		public UUID getID(){
			return this.uuid;
		}
		
		public List<XYPoint> getSkeleton(){
			return this.skeletonPoints;
		}
		
		/**
		 * Fetch the skeleton offset to zero
		 * @return
		 */
		public List<XYPoint> getOffsetSkeleton(){
			List<XYPoint> result = new ArrayList<XYPoint>(0);
			for(XYPoint p : skeletonPoints){
				result.add(new XYPoint( p.getX() - orignalPosition[X_BASE], p.getY() - orignalPosition[Y_BASE]));
			}
			return result;
		}
		
		public List<XYPoint> getBorder(){
			return this.borderPoints;
		}
		
		// positions are offset by the bounding rectangle for easier plotting
		public List<XYPoint> getOffsetBorder(){
			List<XYPoint> result = new ArrayList<XYPoint>(0);
			for(XYPoint p : borderPoints){
				result.add(new XYPoint( p.getX() - orignalPosition[X_BASE], p.getY() - orignalPosition[Y_BASE]));
			}
			return result;
		}
		
		public double getLength(){
			return this.length;
		}
		
		public File getSourceFile(){
			return this.sourceFile;
		}
		
		public int getSourceChannel(){
			return this.sourceChannel;
		}
		
		public double[] getPosition(){
			return this.orignalPosition;
		}

		@Override
		public double getArea() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public boolean equals(CellularComponent c) {
			if(c.getClass()==SpermTail.class){
				return true;
			} else {
				return false;
			}
		}

}
