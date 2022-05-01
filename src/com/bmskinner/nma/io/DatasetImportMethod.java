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
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import javax.xml.XMLConstants;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import com.bmskinner.nma.analysis.AbstractAnalysisMethod;
import com.bmskinner.nma.analysis.AnalysisMethodException;
import com.bmskinner.nma.analysis.DefaultAnalysisResult;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.components.Version.UnsupportedVersionException;
import com.bmskinner.nma.components.cells.ComponentCreationException;
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

	private final File file;
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
	public DatasetImportMethod(final File f) {
		super();

		if (!Importer.isSuitableImportFile(f)) {
			LOGGER.warning(INVALID_FILE_ERROR);
			throw new IllegalArgumentException(Importer.whyIsUnsuitableImportFile(f));
		}

		if (!(f.getName().endsWith(SAVE_FILE_EXTENSION)
				|| f.getName().endsWith(BACKUP_FILE_EXTENSION))) {
			LOGGER.warning(
					"File is not nmd or bak format or has been renamed: " + f.getAbsolutePath());
			throw new IllegalArgumentException(
					"File is not nmd or bak format or has been renamed: " + f.getAbsolutePath());
		}

		this.file = f;
	}

	/**
	 * Call with an existing map of signal ids to directories of images. Designed
	 * for unit testing.
	 * 
	 * @param f
	 * @param signalFiles a map of signal group to folder of signals
	 */
	public DatasetImportMethod(final File f, final Map<UUID, File> signalFiles) {
		this(f);
		signalFileMap = Optional.of(signalFiles);
	}

	@Override
	public IAnalysisResult call() throws Exception {

		if (isOldNmdFormat())
			throw new UnsupportedVersionException(null);

		run();

		if (dataset == null)
			throw new UnloadableDatasetException(
					String.format("Could not load file '%s'", file.getAbsolutePath()));

		DefaultAnalysisResult r = new DefaultAnalysisResult(dataset);
		r.setBoolean(WAS_CONVERTED_BOOL, wasConverted);
		return r;
	}

	private void run() throws Exception {

		// Clean up old log lock files. Legacy.
		cleanLockFilesInDir(file.getParentFile());

		// Deserialise whatever is in the file

		try (InputStream is = new FileInputStream(file);
				CountedInputStream cis = new CountedInputStream(is);) {

			cis.addCountListener((l) -> fireProgressEvent(l));
			SAXBuilder saxBuilder = new SAXBuilder();
			saxBuilder.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			saxBuilder.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			Document doc = saxBuilder.build(cis);
//    		LOGGER.fine("Built XML document");
			fireIndeterminateState(); // TODO: hook the indeterminate state to the end of file
										// reading,
			// rather than after the document is built - takes a long time with large
			// datasets
			dataset = DatasetCreator.createRoot(doc.getRootElement());
			validateDataset();
		} catch (UnsupportedVersionException e) {
			throw (e);

		} catch (ComponentCreationException | IOException | JDOMException e) {
			LOGGER.fine("Error reading XML: " + e.getMessage());
			throw new UnloadableDatasetException(
					"Cannot read as XML dataset: " + file.getAbsolutePath(), e);
		}
	}

	public boolean isOldNmdFormat() {

		try (InputStream is = new FileInputStream(file);) {
			byte[] b = is.readNBytes(4);
			return Arrays.equals(b, NMD_V1_SIGNATURE);
		} catch (IOException e) {
			LOGGER.log(Loggable.STACK, "Error reading first bytes of file", e);
			return false;
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

	/**
	 * Older version of the program did not always close log handlers properly, so
	 * lck files may have proliferated. Kill them with fire.
	 * 
	 * @param dir the directory to clean
	 * @throws IOException
	 */
	private void cleanLockFilesInDir(File dir) throws IOException {

		FilenameFilter filter = (folder, name) -> name.toLowerCase()
				.endsWith(Io.LOCK_FILE_EXTENSION);

		File[] files = dir.listFiles(filter);

		if (files == null)
			return;

		for (File lockFile : files)
			Files.delete(lockFile.toPath());
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
