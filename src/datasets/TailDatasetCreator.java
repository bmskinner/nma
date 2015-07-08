package datasets;

import ij.IJ;
import no.components.XYPoint;

import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import components.Flagellum;
import cell.Cell;

public class TailDatasetCreator {
	
	public static XYDataset createTailOutline(Cell cell){
		DefaultXYDataset ds = new DefaultXYDataset();
		
		Flagellum tail = cell.getTail();
		
		double[] xpoints = new double[tail.getBorder().size()];
		double[] ypoints = new double[tail.getBorder().size()];
		
		int i =0;
		for(XYPoint p : tail.getBorder()){
			xpoints[i] = p.getX();
			ypoints[i] = p.getY();
			i++;
		}
		
		double[][] data = { xpoints, ypoints };
		ds.addSeries("Border", data);
		
//		IJ.log("Created tail border dataset");
		return ds;

	}
	
	public static XYDataset createTailSkeleton(Cell cell){
		DefaultXYDataset ds = new DefaultXYDataset();
		
		Flagellum tail = cell.getTail();
		
		double[] xpoints = new double[tail.getSkeleton().size()];
		double[] ypoints = new double[tail.getSkeleton().size()];
		
		int i =0;
		for(XYPoint p : tail.getSkeleton()){
			xpoints[i] = p.getX();
			ypoints[i] = p.getY();
			i++;
		}
		
		double[][] data = { xpoints, ypoints };
		ds.addSeries("Skeleton", data);
//		IJ.log("Created tail skeleton dataset");
		return ds;
	}

}
