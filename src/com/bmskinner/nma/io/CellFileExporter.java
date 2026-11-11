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
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.DefaultAnalysisResult;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.analysis.MultipleDatasetAnalysisMethod;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.core.GlobalOptions;

/**
 * Export the locations of the centre of mass of nuclei in a dataset to a file
 * 
 * @author Ben Skinner
 *
 */
public class CellFileExporter extends MultipleDatasetAnalysisMethod implements Io {

	private static final Logger LOGGER = Logger.getLogger(CellFileExporter.class.getName());

	/**
	 * Create specifying the folder cell files will be exported into
	 * 
	 * @param folder
	 */
	public CellFileExporter(@NonNull List<IAnalysisDataset> list) {
		super(list);
	}

	/**
	 * Create specifying the folder cell files will be exported into
	 * 
	 * @param folder
	 */
	public CellFileExporter(@NonNull IAnalysisDataset dataset) {
		super(dataset);
	}

	@Override
	public IAnalysisResult call() throws Exception {
		for (final IAnalysisDataset d : datasets) {
			exportCellLocations(d);
		}
		return new DefaultAnalysisResult(datasets);
	}

	private boolean exportCellLocations(IAnalysisDataset d) {

		final String fileName = d.getName() + ".cell-locations" + Io.TAB_FILE_EXTENSION;
		File exportFile = new File(d.getSavePath().getParent(), fileName);

		if (!exportFile.getParentFile().isDirectory()) {
			LOGGER.warning("The intended export folder does not exist");

			final File folder = GlobalOptions.getInstance().getDefaultDir();
			exportFile = new File(folder, fileName);
		}

		LOGGER.info("Exporting cells to %s".formatted(exportFile.getAbsolutePath()));

		if (exportFile.exists()) {
			LOGGER.fine("Overwriting existing cell location file %s".formatted(exportFile.getAbsolutePath()));
			exportFile.delete();
		}

		final StringBuilder builder = new StringBuilder();

		/*
		 * Add the cells from the root dataset
		 */
		builder.append(makeDatasetHeaderString(d, d.getId()));
		builder.append(makeDatasetCellsString(d));

		/*
		 * Add cells from all child datasets
		 */
		builder.append(makeChildString(d));

		try {
			export(builder.toString(), exportFile);
		} catch (final FileNotFoundException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
			return false;
		}
		return true;
	}

	/**
	 * Add the cells from all child datasets recursively
	 * 
	 * @param d
	 * @return
	 */
	private static String makeChildString(IAnalysisDataset d) {
		final StringBuilder builder = new StringBuilder();

		for (final IAnalysisDataset child : d.getChildDatasets()) {
			builder.append(makeDatasetHeaderString(child, d.getId()));
			builder.append(makeDatasetCellsString(child));

			if (child.hasChildren()) {
				builder.append(makeChildString(child));
			}
		}

		return builder.toString();
	}

	/**
	 * Add the cell positions and image names
	 * 
	 * @param d
	 * @return
	 */
	private static String makeDatasetCellsString(IAnalysisDataset d) {
		final StringBuilder builder = new StringBuilder();

		for (final ICell c : d.getCollection().getCells()) {

			final IPoint com = c.getPrimaryNucleus().getCentreOfMass();

			final double x = com.getX() + c.getPrimaryNucleus().getXBase();
			final double y = com.getY() + c.getPrimaryNucleus().getYBase();

			try {

				if (c.getPrimaryNucleus().getSourceFile() != null) {
					builder.append(c.getPrimaryNucleus().getSourceFile().getAbsolutePath());
					builder.append(Io.TAB);
					builder.append(x);
					builder.append("-");
					builder.append(y);
					builder.append(NEWLINE);
				}
			} catch (final Exception e) {
				return null;
			}

		}
		return builder.toString();
	}

	/**
	 * Add the dataset id, name, and parent
	 * 
	 * @param child
	 * @param parent
	 * @return
	 */
	private static String makeDatasetHeaderString(IAnalysisDataset child, UUID parent) {
		final StringBuilder builder = new StringBuilder();

		builder.append("UUID\t");
		builder.append(child.getId().toString());
		builder.append(NEWLINE);

		builder.append("Name\t");
		builder.append(child.getName());
		builder.append(NEWLINE);

		builder.append("ChildOf\t");
		builder.append(parent.toString());
		builder.append(NEWLINE);
		return builder.toString();
	}

	private static void export(String s, File f) throws FileNotFoundException {

		PrintWriter out;
		out = new PrintWriter(f);
		out.print(s);
		out.close();

	}

}
