/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nma.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import com.bmskinner.nma.components.MissingComponentException;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.ICellCollection;
import com.bmskinner.nma.components.datasets.VirtualDataset;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.measure.MeasurementScale;
import com.bmskinner.nma.components.profiles.ProfileException;

public class RandomSamplingMethod extends SingleDatasetAnalysisMethod {

	private static final Logger LOGGER = Logger.getLogger(RandomSamplingMethod.class.getName());

	private List<Double> magnitudes = new ArrayList<>();
	private int iterations;
	private Measurement stat;

	// the number of cells in the first subset
	private int first;

	// the number of cells in the second subset
	private int second;

	/**
	 * Constructor
	 * 
	 * @param dataset    the dataset to investigate
	 * @param stat       the stat to measure
	 * @param iterations the number of iterations to run
	 * @param first      the size of the first subgroup
	 * @param second     the size of the second subgroup
	 */
	public RandomSamplingMethod(IAnalysisDataset dataset, Measurement stat, int iterations,
			int first, int second) {
		super(dataset);
		this.stat = stat;
		this.iterations = iterations;
		this.first = first;
		this.second = second;

	}

	@Override
	public IAnalysisResult call() throws Exception {

		run();
		return new RandomSamplingResult(dataset, magnitudes);
	}

	public void run() throws Exception {

		// for each iteration
		LOGGER.fine("Beginning sampling");
		for (int i = 0; i < iterations; i++) {
			LOGGER.finest("Sample " + i);
			// make a new collection randomly sampled to teh correct proportion
			ICellCollection[] collections = makeRandomSampledCollection(first, second);
			LOGGER.finest("Made collection");

			// get the stat magnitude
			double value1 = collections[0].getMedian(stat, CellularComponent.NUCLEUS,
					MeasurementScale.PIXELS);
			double value2 = collections[1].getMedian(stat, CellularComponent.NUCLEUS,
					MeasurementScale.PIXELS);

			double magnitude = value2 / value1;
			LOGGER.finest("Found value");
			// add to a list
			magnitudes.add(magnitude);
			fireProgressEvent();
		}

	}

	private ICellCollection[] makeRandomSampledCollection(int firstSize, int secondSize)
			throws MissingComponentException, ProfileException {

		ICellCollection c1 = new VirtualDataset(dataset, "first");
		ICellCollection c2 = new VirtualDataset(dataset, "second");
		LOGGER.finer("Created new collections");

		List<ICell> cells = new ArrayList<>(dataset.getCollection().getCells());
		Collections.shuffle(cells);
		LOGGER.finer("Shuffled cells");

		for (int i = 0; i < firstSize; i++) {
			c1.addCell(cells.get(i));
		}
		LOGGER.finer("Added first set");
		for (int i = firstSize; i < firstSize + secondSize; i++) {
			c2.addCell(cells.get(i));
		}
		LOGGER.finer("Added second set");

		if (stat.equals(Measurement.VARIABILITY)) {
			c1.getProfileCollection().calculateProfiles();
			c2.getProfileCollection().calculateProfiles();
		}

		return new ICellCollection[] { c1, c2 };

	}

	public class RandomSamplingResult extends DefaultAnalysisResult {
		private List<Double> values = new ArrayList<>();

		public RandomSamplingResult(IAnalysisDataset d, List<Double> values) {
			super(d);
			this.values = values;
		}

		public List<Double> getValues() {
			return values;
		}

	}

}
