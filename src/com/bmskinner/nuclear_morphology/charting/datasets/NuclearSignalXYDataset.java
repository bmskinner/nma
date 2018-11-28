package com.bmskinner.nuclear_morphology.charting.datasets;

import java.util.ArrayList;
import java.util.List;

import org.jfree.data.xy.DefaultXYDataset;

import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.nuclear.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;

public class NuclearSignalXYDataset extends DefaultXYDataset {
	
	private List<List<INuclearSignal>> signalList = new ArrayList<>();
	private List<List<Nucleus>> nucleusList = new ArrayList<>();
	
	public NuclearSignalXYDataset() {
		super();
	}
	
	public void addSeries(Comparable seriesKey, double[][] data, List<INuclearSignal> signals, List<Nucleus> nuclei) {
		super.addSeries(seriesKey, data);
		signalList.add(signals);
		nucleusList.add(nuclei);
	}
	
	public INuclearSignal getSignal(Comparable seriesKey, int item) {
		int seriesIndex = indexOf(seriesKey);
		return signalList.get(seriesIndex).get(item);
	}
	
	public Nucleus getNucleus(Comparable seriesKey, int item) {
		int seriesIndex = indexOf(seriesKey);
		return nucleusList.get(seriesIndex).get(item);
	}

}
