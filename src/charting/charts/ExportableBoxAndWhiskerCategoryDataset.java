package charting.charts;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;

public class ExportableBoxAndWhiskerCategoryDataset 
extends DefaultBoxAndWhiskerCategoryDataset{

	private Map<Comparable, Map<Comparable, List>> rawData = new HashMap<Comparable, Map<Comparable, List>>();
	
	public ExportableBoxAndWhiskerCategoryDataset(){
		super();
	}
	
	@Override
	public void add(List list, Comparable rowKey, Comparable columnKey){
		super.add(list, rowKey, columnKey);
		Map<Comparable, List> row;
		if(rawData.get(rowKey)==null){
			row = new HashMap<Comparable, List>();
			rawData.put(rowKey, row);
		} 
		row = rawData.get(rowKey);
		row.put(columnKey, list);
	}
	
	public List getRawData(Comparable rowKey, Comparable columnKey){
		if(rawData.get(rowKey)==null){
			return null;
		} else {
			return rawData.get(rowKey).get(columnKey);
		}
	}

}
