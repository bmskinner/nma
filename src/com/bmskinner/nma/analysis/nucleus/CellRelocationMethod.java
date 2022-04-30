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
package com.bmskinner.nma.analysis.nucleus;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import com.bmskinner.nma.analysis.DefaultAnalysisResult;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nma.components.MissingLandmarkException;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.VirtualDataset;
import com.bmskinner.nma.components.generic.FloatPoint;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.components.profiles.MissingProfileException;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.logging.Loggable;

/**
 * Find cells from a .cell file and assign them to child datasets.
 * 
 * @author ben
 * @since 1.13.4
 *
 */
public class CellRelocationMethod extends SingleDatasetAnalysisMethod {

	private static final Logger LOGGER = Logger.getLogger(CellRelocationMethod.class.getName());

	private static final String TAB = "\\t";
	private static final String UUID_KEY = "UUID";
	private static final String NAME_KEY = "Name";
	private static final String CHILD_OF_KEY = "ChildOf";

	private File inputFile = null;

	/**
	 * Construct with a dataset and a file containing cell locations
	 * 
	 * @param dataset
	 * @param file
	 */
	public CellRelocationMethod(final IAnalysisDataset dataset, final File file) {
		super(dataset);
		this.inputFile = file;
	}

	@Override
	public IAnalysisResult call() throws CellRelocationException {
		run();
		IAnalysisResult r = new DefaultAnalysisResult(dataset);
		return r;
	}

	private void run() {

		try {
			findCells();
		} catch (Exception e) {
			LOGGER.warning("Error selecting cells");
			LOGGER.log(Loggable.STACK, "Error selecting cells", e);
		}
	}

	private void findCells() {
		Set<UUID> newDatasets;
		try {
			newDatasets = parsePathList();
		} catch (CellRelocationException | ProfileException | MissingLandmarkException
				| MissingProfileException e) {
			LOGGER.log(Loggable.STACK, "Error relocating cells", e);
			return;
		}

		if (newDatasets.size() > 0) {

			try {

				for (UUID id : newDatasets) {

					if (!id.equals(dataset.getId())) {
						dataset.getCollection().getProfileManager()
								.copySegmentsAndLandmarksTo(
										dataset.getChildDataset(id).getCollection());

					}
				}
			} catch (ProfileException | MissingProfileException | MissingLandmarkException e) {
				LOGGER.warning("Unable to profile new collections");
				LOGGER.log(Loggable.STACK, "Unable to profile new collections", e);
				return;
			}
		}

	}

	private Set<UUID> parsePathList() throws CellRelocationException, ProfileException,
			MissingLandmarkException, MissingProfileException {
		Scanner scanner;
		try {
			scanner = new Scanner(inputFile);
		} catch (FileNotFoundException e) {
			throw new CellRelocationException("Input file does not exist", e);
		}

		UUID activeID = null;
		String activeName = null;

		Map<UUID, IAnalysisDataset> map = new HashMap<>();

		int cellCount = 0;
		while (scanner.hasNextLine()) {

			/*
			 * File format: UUID 57320dbb-bcde-49e3-ba31-5b76329356fe Name Testing ChildOf
			 * 57320dbb-bcde-49e3-ba31-5b76329356fe J:\Protocols\Scripts and
			 * macros\Testing\s75.tiff 602.0585522824504-386.38060239306236
			 */

			String line = scanner.nextLine();
			if (line.startsWith(UUID_KEY)) {
				/* New dataset found */
				activeID = UUID.fromString(line.split(TAB)[1]);

				if (dataset.getId().equals(activeID) || dataset.hasDirectChild(activeID)) {
					// the dataset already exists with this id - we must fail
					scanner.close();
					LOGGER.warning("Dataset in cell file already exists");
					LOGGER.warning("Cancelling relocation");
					throw new CellRelocationException("Dataset already exists");
				}

				continue;
			}

			if (line.startsWith(NAME_KEY)) {
				/* Name of new dataset */

				activeName = line.split(TAB)[1];

				if (activeID == null)
					continue;
				IAnalysisDataset d = new VirtualDataset(dataset, activeName, activeID);

				Optional<IAnalysisOptions> op = dataset.getAnalysisOptions();
				if (op.isPresent())
					d.setAnalysisOptions(op.get());

				map.put(activeID, d);
				continue;
			}

			if (line.startsWith(CHILD_OF_KEY)) {
				/* Parent dataset */
				UUID parentID = UUID.fromString(line.split(TAB)[1]);

				if (parentID.equals(activeID)) {
					dataset.addChildDataset(map.get(activeID));
				} else {
					map.get(parentID).addChildDataset(map.get(activeID));
				}
				continue;
			}

			/* No header line, so must be a cell for the current dataset */
			Optional<ICell> cell = getCellFromLine(line);
			if (cell.isPresent()) {
				map.get(activeID).getCollection().addCell(cell.get());
				cellCount++;
			} else {
				LOGGER.fine("Cell not found: " + line);
			}
		}

		LOGGER.info(map.get(activeID).getCollection().size() + " cells out of " + cellCount
				+ " relocated");

		if (cellCount == 0 && activeID != null) {
			LOGGER.warning("No cells in dataset " + map.get(activeID).getName());
			dataset.deleteChild(activeID);
			map.remove(activeID);
		}

		// Make the profile collections for the new datasets
		for (IAnalysisDataset d : map.values()) {
			d.getCollection().getProfileCollection().calculateProfiles();
		}

		scanner.close();
		return map.keySet();
	}

	/**
	 * Given a line from the cell file, return a matching cell from the current
	 * dataset
	 * 
	 * @param line
	 * @return
	 * @throws CellRelocationException
	 */
	private Optional<ICell> getCellFromLine(String line) throws CellRelocationException {
		LOGGER.fine("Processing line: " + line);

		// Line format is FilePath\tPosition as x-y
		// Build a file name based on the current image folder and the stored filename
		// Note - we don't need the file to exist for the assignment to work
		Optional<IAnalysisOptions> analysisOptions = dataset.getAnalysisOptions();
		if (analysisOptions.isPresent()) {
			Optional<HashOptions> nucleusOptions = analysisOptions.get()
					.getDetectionOptions(CellularComponent.NUCLEUS);

			if (nucleusOptions.isPresent()) {
				File currentImageDirectory = new File(
						nucleusOptions.get().getString(HashOptions.DETECTION_FOLDER));

				File savedFile = getFile(line);
				// Get the image name and substitute the parent dataset path.
				savedFile = new File(currentImageDirectory, savedFile.getName());
				LOGGER.fine("New path: " + savedFile.getAbsolutePath());

				if (!savedFile.exists()) {
					LOGGER.warning("File does not exist: " + savedFile.getAbsolutePath());
					return Optional.empty();
				}

				// get position
				IPoint com = getPosition(line);

				return copyCellFromRoot(savedFile, com);
			} else {
				throw new CellRelocationException(
						"No nuclear detection options - cannot check directory path");
			}
		} else {
			throw new CellRelocationException("No analysis options - cannot check directory path");
		}
	}

	/**
	 * Make a new cell based on the cell in the root dataset with the given location
	 * in an image file
	 * 
	 * @param f
	 * @param com
	 * @return
	 */
	private Optional<ICell> copyCellFromRoot(File f, IPoint com) {
		// find the nucleus
		Set<ICell> cells = this.dataset.getCollection().getCells(f);
		for (ICell c : cells) {
			for (Nucleus n : c.getNuclei()) {
				if (n.containsOriginalPoint(com)) {
					return Optional.of(c);
				}
			}
		}
		return Optional.empty();
	}

	private File getFile(String line) {
		String[] array = line.split(TAB);
		return new File(array[0]);
	}

	private IPoint getPosition(String line) {
		String[] array = line.split(TAB);
		String position = array[1];

		String[] posArray = position.split("-");

		double x = Double.parseDouble(posArray[0]);
		double y = Double.parseDouble(posArray[1]);
		return new FloatPoint(x, y);
	}

	public class CellRelocationException extends Exception {
		private static final long serialVersionUID = 1L;

		public CellRelocationException() {
			super();
		}

		public CellRelocationException(String message) {
			super(message);
		}

		public CellRelocationException(String message, Throwable cause) {
			super(message, cause);
		}

		public CellRelocationException(Throwable cause) {
			super(cause);
		}
	}

}
