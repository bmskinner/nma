package com.bmskinner.nuclear_morphology.visualisation.datasets;

import java.util.ArrayList;
import java.util.List;

import com.bmskinner.nuclear_morphology.components.cells.Nucleus;
import com.bmskinner.nuclear_morphology.components.signals.INuclearSignal;

/**
 * An XY dataset mapping signals and nuclei to their XY coordinates
 * @author ben
 *
 */
public class NuclearSignalXYDataset extends ComponentXYDataset<Nucleus> {
	
	private List<List<INuclearSignal>> signalList = new ArrayList<>();
	
	public NuclearSignalXYDataset() {
		super();
	}
	
	public void addSeries(Comparable<?> seriesKey, double[][] data, List<INuclearSignal> signals, List<Nucleus> nuclei) {
		super.addSeries(seriesKey, data, nuclei);
		signalList.add(signals);
	}
	
	public INuclearSignal getSignal(Comparable<?> seriesKey, int item) {
		int seriesIndex = indexOf(seriesKey);
		return signalList.get(seriesIndex).get(item);
	}
}
