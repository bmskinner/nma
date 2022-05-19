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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import org.jdom2.Document;

import com.bmskinner.nma.analysis.AbstractAnalysisMethod;
import com.bmskinner.nma.analysis.AnalysisMethodException;
import com.bmskinner.nma.analysis.DefaultAnalysisResult;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.components.datasets.DatasetCreator;
import com.bmskinner.nma.components.datasets.DatasetRepairer;
import com.bmskinner.nma.components.datasets.DatasetValidator;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.io.Io.Importer;
import com.bmskinner.nma.logging.Loggable;

/**
 * Method to read a dataset from file
 * 
 * @author ben
 * @since 1.13.4
 *
 */
public class DatasetImportMethod extends AbstractAnalysisMethod implements Importer {

	private static final Logger LOGGER = Logger.getLogger(DatasetImportMethod.class.getName());

	private static final byte[] NMD_V1_SIGNATURE = new byte[] { -84, -19, 0, 5 };

	private final Document doc;
	private IAnalysisDataset dataset = null;
	private boolean wasConverted = false;
	public static final int WAS_CONVERTED_BOOL = 0;

	/**
	 * Store a map of signal image locations if necessary
	 */
	private Optional<Map<UUID, File>> signalFileMap = Optional.empty();

	/**
	 * Construct with a file to be read
	 * 
	 * @param f the saved dataset file
	 */
	public DatasetImportMethod(final Document doc) {
		super();

		this.doc = doc;
	}

	/**
	 * Call with an existing map of signal ids to directories of images. Designed
	 * for unit testing.
	 * 
	 * @param f
	 * @param signalFiles a map of signal group to folder of signals
	 */
	public DatasetImportMethod(final Document doc, final Map<UUID, File> signalFiles) {
		this(doc);
		signalFileMap = Optional.of(signalFiles);
	}

	@Override
	public IAnalysisResult call() throws Exception {

		run();

		if (dataset == null)
			throw new UnloadableDatasetException(
					String.format("Could not load document"));

		DefaultAnalysisResult r = new DefaultAnalysisResult(dataset);
		r.setBoolean(WAS_CONVERTED_BOOL, wasConverted);
		return r;
	}

	private void run() throws UnloadableDatasetException {

		try {
			fireIndeterminateState(); // TODO: hook the indeterminate state to the end of file
										// reading,
			// rather than after the document is built - takes a long time with large
			// datasets
			dataset = DatasetCreator.createRoot(doc.getRootElement());

			validateDataset();

		} catch (Exception e) {
			LOGGER.log(Loggable.STACK, "Error in dataset import", e);
			throw new UnloadableDatasetException("Not valid XML dataset for this version", e);
		}
	}

	/**
	 * Check the dataset has valid segments and profiles
	 * 
	 * @throws Exception
	 */
	private void validateDataset() throws Exception {
		// Check the validity of the loaded dataset
		// Repair if possible, or error if not
		DatasetRepairer dr = new DatasetRepairer();
		dr.repair(dataset);

		DatasetValidator dv = new DatasetValidator();
		if (!dv.validate(dataset)) {
			for (String s : dv.getSummary()) {
				LOGGER.log(Loggable.STACK, s);
			}
			for (String s : dv.getErrors()) {
				LOGGER.log(Loggable.STACK, s);
			}

			LOGGER.warning("The dataset is not properly segmented");
			LOGGER.warning("Curated datasets and groups have been saved");
			LOGGER.warning(
					"Redetect cells and import the ." + Importer.LOC_FILE_EXTENSION + " file");

			new CellFileExporter(dataset).call();
			throw new AnalysisMethodException("Unable to validate or repair dataset");
		}
	}

	public class UnloadableDatasetException extends Exception {
		private static final long serialVersionUID = 1L;

		public UnloadableDatasetException() {
			super();
		}

		public UnloadableDatasetException(String message) {
			super(message);
		}

		public UnloadableDatasetException(String message, Throwable cause) {
			super(message, cause);
		}

		public UnloadableDatasetException(Throwable cause) {
			super(cause);
		}
	}

}
