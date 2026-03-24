package com.bmskinner.nma.visualisation.datasets;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.jfree.data.xy.DefaultXYDataset;

/**
 * An XY dataset mapping components to their XY coordinates
 * @author Ben Skinner
 * @since 1.16.0
 * @param T the type of component to be held
 *
 */
public class ComponentXYDataset<T> extends DefaultXYDataset {
	
	private static final Logger LOGGER = Logger.getLogger(ComponentXYDataset.class.getName());

	private final List<List<T>> componentList = new ArrayList<>();
	
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
		int seriesIndex = -1;
		final String keyString = seriesKey.toString();

		for (int i = 0; i < this.getSeriesCount(); i++) {
			// Force a string comparison, not the inbuilt indexOf. The input seriesKey may
			// be a Comparables or a String already which can cause issues with indexOf.
			if (this.getSeriesKey(i).toString().equals(keyString)) {
				seriesIndex = i;
			}
		}
		return seriesIndex == -1 ? null : componentList.get(seriesIndex).get(item);
	}

}
