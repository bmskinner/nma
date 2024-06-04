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
package com.bmskinner.nma.io;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.measure.MeasurementScale;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.MissingOptionException;
import com.bmskinner.nma.components.profiles.MissingLandmarkException;
import com.bmskinner.nma.components.profiles.MissingProfileException;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.components.signals.INuclearSignal;
import com.bmskinner.nma.components.signals.ISignalGroup;

public class DatasetSignalsExporter extends MeasurementsExportMethod {

	/**
	 * Create specifying the file or directory profiles will be exported into
	 * 
	 * @param file    the output file or directory for the export
	 * @param list    the datasets to export
	 * @param options other options for the export
	 */
	public DatasetSignalsExporter(@NonNull File file, @NonNull List<IAnalysisDataset> list,
			@NonNull HashOptions options) {
		super(file, list, options);
	}

	/**
	 * Create specifying the file or directory profiles will be exported into
	 * 
	 * @param file    the output file or directory for the export
	 * @param dataset the dataset to export
	 * @param options other options for the export
	 */
	public DatasetSignalsExporter(@NonNull File file, @NonNull IAnalysisDataset dataset,
			@NonNull HashOptions options) {
		super(file, dataset, options);
	}

	/**
	 * Append a column header line to the StringBuilder.
	 * 
	 * @param outLine
	 */
	@Override
	protected void appendHeader(@NonNull StringBuilder outLine) {

		String[] headers = {
				"Dataset",
				"CellId",
				"ComponentId",
				"SignalGroup",
				"SignalFolder",
				"SignalImage",
				"SignalChannel"
		};

		outLine.append(Stream.of(headers).collect(Collectors.joining(TAB)) + TAB);

		for (Measurement s : Measurement.getSignalStats()) {
			String label = s.label(MeasurementScale.PIXELS)
					.replaceAll(" ", "_")
					.replaceAll("\\(", "_")
					.replaceAll("\\)", "")
					.replaceAll("__", "_");
			outLine.append(label + TAB);

			if (!s.isDimensionless() && !s.isAngle()) {
				label = s.label(MeasurementScale.MICRONS).replaceAll(" ", "_")
						.replaceAll("\\(", "_")
						.replaceAll("\\)", "")
						.replaceAll("__", "_");
				outLine.append(label + TAB);
			}
		}

		// remove the final tab character
		if (outLine.length() > 0)
			outLine.setLength(outLine.length() - 1);

		outLine.append(NEWLINE);
	}

	/**
	 * Append the given dataset stats into the string builder
	 * 
	 * @param d       the dataset to export
	 * @param outLine the string builder to append to
	 * @throws MissingOptionException
	 * @throws UnloadableImageException
	 * @throws MissingLandmarkException
	 * @throws MissingProfileException
	 * @throws ProfileException
	 */
	@Override
	protected void append(@NonNull IAnalysisDataset d, @NonNull PrintWriter pw)
			throws MissingOptionException {

		for (@NonNull
		UUID signalGroupId : d.getCollection().getSignalGroupIDs()) {
			ISignalGroup signalGroup = d.getCollection().getSignalGroup(signalGroupId).get();
			String groupName = signalGroup.getGroupName();
			String groupFolder = d.getAnalysisOptions().get()
					.getNuclearSignalDetectionFolder(signalGroupId)
					.orElseThrow(MissingOptionException::new)
					.getAbsolutePath();

			for (ICell cell : d.getCollection().getCells()) {

				StringBuilder outLine = new StringBuilder();

				if (!cell.hasNucleus())
					continue;

				for (Nucleus n : cell.getNuclei()) {

					if (!n.getSignalCollection().hasSignal(signalGroupId))
						continue;

					for (INuclearSignal s : n.getSignalCollection().getSignals(signalGroupId)) {

						outLine.append(d.getName() + TAB)
								.append(cell.getId() + TAB)
								.append(n.getID() + TAB)
								.append(groupName + TAB)
								.append(groupFolder + TAB)
								.append(s.getSourceFile().getName() + TAB)
								.append(s.getChannel() + TAB);

						appendSignalStats(outLine, s);

						if (outLine.length() > 0)
							outLine.setLength(outLine.length() - 1);
						outLine.append(NEWLINE);
					}
				}

				pw.write(outLine.toString());
			}
		}
	}

	private void appendSignalStats(@NonNull StringBuilder outLine, @NonNull CellularComponent c) {

		for (Measurement s : Measurement.getSignalStats()) {
			double varP = c.getMeasurement(s, MeasurementScale.PIXELS);
			double varM = c.getMeasurement(s, MeasurementScale.MICRONS);

			outLine.append(varP + TAB);
			if (!s.isDimensionless() && !s.isAngle()) {
				outLine.append(varM + TAB);
			}
		}
	}
}
