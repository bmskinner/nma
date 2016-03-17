package charting.datasets;

import java.util.HashMap;
import java.util.Map;

import org.jfree.data.xy.DefaultXYDataset;

@SuppressWarnings("serial")
public class NucleusMeshXYDataset extends DefaultXYDataset {
	
	private Map<Comparable, Double> ratioMap = new HashMap<Comparable, Double>();
	
	public NucleusMeshXYDataset(){
		super();
	}

	public double getRatio(Comparable seriesKey) {
		return ratioMap.get(seriesKey);
	}

	public void setRatio(Comparable seriesKey, double ratio) {
		this.ratioMap.put(seriesKey, ratio); 
	}
	
	

}
