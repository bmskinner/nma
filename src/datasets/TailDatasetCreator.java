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
