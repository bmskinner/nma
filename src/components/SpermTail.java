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
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import components.generic.XYPoint;

/**
 * The sperm tail is a specialised type of flagellum. It is anchored at
 * the tail end of the sperm nucleus, and contains a midpiece (with mitochondria
 * attached) and a long thin tail. Cytoplasmic droplets may be present. Imaged
 * tails often overlap themselves and other tails. Common stain - anti-tubulin.
 * @author bms41
 *
 */
public class SpermTail extends AbstractCellularComponent implements Serializable, Flagellum {
			
		private static final long serialVersionUID = 1L;

		protected double length; // the length of the skeleton
		
		protected XYPoint nucleusIntersection; // the position where the tail intersects the nucleus
		
		protected List<XYPoint> skeletonPoints = new ArrayList<XYPoint>(0); 
		protected List<XYPoint> borderPoints   = new ArrayList<XYPoint>(0); 
		
		public SpermTail(File source, int channel, Roi skeleton, Roi border){
			super();

			this.setSourceFolder(source.getParentFile());
			this.setSourceFileName(source.getName());
			this.setChannel(channel);
			
			this.setPosition( new double[] { border.getPolygon().getBounds().getMinX(),
					 border.getPolygon().getBounds().getMinY(),
					 border.getPolygon().getBounds().getWidth(),
					 border.getPolygon().getBounds().getHeight()});
			
			
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
		
		public SpermTail(final SpermTail t){
			super(t);			
			this.borderPoints = t.getBorder();
			this.skeletonPoints = t.getSkeleton();
									
			this.length = t.getLength();
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
				result.add(new XYPoint( p.getX() - this.getPosition()[X_BASE], p.getY() - this.getPosition()[Y_BASE]));
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
				result.add(new XYPoint( p.getX() - this.getPosition()[X_BASE], p.getY() - this.getPosition()[Y_BASE]));
			}
			return result;
		}
		
		public double getLength(){
			return this.length;
		}
		

		@Override
		public boolean equals(CellularComponent c) {
			if(c.getClass()==SpermTail.class){
				return true;
			} else {
				return false;
			}
		}
		
		private void writeObject(java.io.ObjectOutputStream out) throws IOException {
			finest("\tWriting sperm tail");
			out.defaultWriteObject();
			finest("\tWrote sperm tail");
		}

		private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
			finest("\tReading sperm tail");
			in.defaultReadObject();
			finest("\tRead sperm tail"); 
		}

		@Override
		public void alignVertically() {
			// TODO Auto-generated method stub
			
		}

}
