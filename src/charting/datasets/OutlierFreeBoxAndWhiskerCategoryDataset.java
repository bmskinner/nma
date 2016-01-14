package charting.datasets;

import java.util.ArrayList;
import java.util.List;

import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;


/**
 * Use this to draw boxplots with no outlier circles
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class OutlierFreeBoxAndWhiskerCategoryDataset extends DefaultBoxAndWhiskerCategoryDataset {
	
	public OutlierFreeBoxAndWhiskerCategoryDataset(){
		super();
	}
	
	@Override
	public List<?> getOutliers(int row, int column){
		return new ArrayList();
		
	}

}
