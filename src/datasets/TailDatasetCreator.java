package datasets;

import no.components.XYPoint;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import components.Flagellum;
import cell.Cell;

public class TailDatasetCreator {
	
	public static XYDataset createTailOutline(Cell cell){
		DefaultXYDataset ds = new DefaultXYDataset();
		
		Flagellum tail = cell.getTail();
		
		double[] xpoints = new double[tail.getOffsetBorder().size()];
		double[] ypoints = new double[tail.getOffsetBorder().size()];
		
		int i =0;
		for(XYPoint p : tail.getOffsetBorder()){
			xpoints[i] = p.getX();
			ypoints[i] = p.getY();
			i++;
		}
		
		double[][] data = { xpoints, ypoints };
		ds.addSeries("Border", data);
		
		return ds;
	}
	
	public static XYDataset createTailSkeleton(Cell cell){
		DefaultXYDataset ds = new DefaultXYDataset();
		
		Flagellum tail = cell.getTail();
		
		double[] xpoints = new double[tail.getOffsetSkeleton().size()];
		double[] ypoints = new double[tail.getOffsetSkeleton().size()];
		
		int i =0;
		for(XYPoint p : tail.getOffsetBorder()){
			xpoints[i] = p.getX();
			ypoints[i] = p.getY();
			i++;
		}
		
		double[][] data = { xpoints, ypoints };
		ds.addSeries("Skeleton", data);
		
		return ds;
	}

}
