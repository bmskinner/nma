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
package datasets;

import ij.IJ;
import no.components.XYPoint;

import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import components.Flagellum;
import cell.Cell;

public class TailDatasetCreator {
	
	/**
	 * Get a dataset contining the outlines of all tails within the cell
	 * as series
	 * @param cell the cell
	 * @return an XY dataset
	 */
	public static XYDataset createTailOutline(Cell cell){
		DefaultXYDataset ds = new DefaultXYDataset();
		
		int j = 0;
		for(Flagellum tail : cell.getTails()){

			double[] xpoints = new double[tail.getBorder().size()];
			double[] ypoints = new double[tail.getBorder().size()];

			int i =0;
			for(XYPoint p : tail.getBorder()){
				xpoints[i] = p.getX();
				ypoints[i] = p.getY();
				i++;
			}

			double[][] data = { xpoints, ypoints };
			ds.addSeries("Border_"+j, data);
			j++;
		}
		
		return ds;

	}
	
	/**
	 * Get a dataset contining the skeletons of all tails within the cell
	 * as series
	 * @param cell the cell
	 * @return an XY dataset
	 */
	public static XYDataset createTailSkeleton(Cell cell){
		DefaultXYDataset ds = new DefaultXYDataset();

		int j = 0;
		for(Flagellum tail : cell.getTails()){

			double[] xpoints = new double[tail.getSkeleton().size()];
			double[] ypoints = new double[tail.getSkeleton().size()];

			int i =0;
			for(XYPoint p : tail.getSkeleton()){
				xpoints[i] = p.getX();
				ypoints[i] = p.getY();
				i++;
			}

			double[][] data = { xpoints, ypoints };
			ds.addSeries("Skeleton_"+j, data);
			j++;
		}
		return ds;
	}

}
