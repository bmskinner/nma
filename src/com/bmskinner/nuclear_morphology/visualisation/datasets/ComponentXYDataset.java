package com.bmskinner.nuclear_morphology.visualisation.datasets;

import java.util.ArrayList;
import java.util.List;

import org.jfree.data.xy.DefaultXYDataset;

/**
 * An XY dataset mapping components to their XY coordinates
 * @author ben
 * @since 1.16.0
 * @param T the type of component to be held
 *
 */
public class ComponentXYDataset<T> extends DefaultXYDataset {
	
	private List<List<T>> componentList = new ArrayList<>();
	
	public ComponentXYDataset() {
		super();
	}
	
	/**
	 * Add a series	
	 * @param seriesKey the series key
	 * @param data the XY coordinates
	 * @param cells the components in the same order as the data
	 */
	public void addSeries(Comparable<?> seriesKey, double[][] data, List<T> components) {
		super.addSeries(seriesKey, data);
		componentList.add(components);
	}
	
	/**
	 * Get the component of the given item
	 * @param seriesKey
	 * @param item
	 * @return
	 */
	public T getComponent(Comparable<?> seriesKey, int item) {
		int seriesIndex = indexOf(seriesKey);
		return componentList.get(seriesIndex).get(item);
	}

}
