package com.bmskinner.nma.doc;

import static org.junit.Assert.assertTrue;

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
//package com.bmskinner.nma.documentation;

import java.awt.AWTException;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.Test;

import com.bmskinner.nma.TestResources;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.core.DatasetListManager;
import com.bmskinner.nma.core.NuclearMorphologyAnalysis;
import com.bmskinner.nma.gui.events.FileImportEventListener.FileImportEvent;
import com.bmskinner.nma.gui.events.UserActionController;
import com.bmskinner.nma.gui.tabs.DetailPanel;

/**
 * Walk through the UI, taking screenshots of each window for documentation
 * 
 * @author ben
 * @since 1.14.0
 *
 */
public class Screenshotter {

	private static final Logger LOGGER = Logger.getLogger(Screenshotter.class.getName());

	/** Sleep time after switching tabs */
	private static final int SLEEP_TIME_MILLIS = 150;

	/** Sleep time after loading a dataset */
	private static final int LOAD_TIME_MILLIS = 2000;

	private static final String SCREENSHOT_FOLDER = "res/screens/" + System.getProperty("os.name");

	private final Robot robot;

	private final NuclearMorphologyAnalysis nma;
	private final UserActionController uac;
	private final DatasetListManager dlm;

	public Screenshotter() throws AWTException, InterruptedException {
		robot = new Robot();
		nma = NuclearMorphologyAnalysis.getInstance();
		nma.runWithGUI();
		Thread.sleep(LOAD_TIME_MILLIS);

		uac = UserActionController.getInstance();
		dlm = DatasetListManager.getInstance();
	}

	/**
	 * Launch the screenshotter as a test class
	 * 
	 * @throws InterruptedException
	 * @throws AWTException
	 * @throws IOException
	 */
	@Test
	public void testScreenShotsCreated() throws InterruptedException, AWTException, IOException {
		assertTrue(setup());
	}

	private static boolean setup() throws InterruptedException, IOException, AWTException {
		return new Screenshotter().run();
	}

	public boolean allFilesExist() {
		if (!TestResources.MOUSE_SIGNALS_DATASET.exists()) {
			LOGGER.warning(TestResources.MOUSE_SIGNALS_DATASET.getName() + " missing");
			return false;
		}

		if (!TestResources.MOUSE_TEST_DATASET.exists()) {
			LOGGER.warning(TestResources.MOUSE_TEST_DATASET.getName() + " missing");
			return false;
		}

		if (!TestResources.MOUSE_CLUSTERS_DATASET.exists()) {
			LOGGER.warning(TestResources.MOUSE_CLUSTERS_DATASET.getName() + " missing");
			return false;
		}
		return true;
	}

	private boolean run() throws InterruptedException, IOException {

		// clear previous runs
		File rootFolder = new File(SCREENSHOT_FOLDER);
		FileUtils.deleteQuietly(rootFolder);
		rootFolder.mkdirs();

		if (allFilesExist()) {

			LOGGER.fine("Outputting to " + rootFolder.getAbsolutePath());

			takeSingleDatasetScreenshots(TestResources.MOUSE_SIGNALS_DATASET, rootFolder);
			takeSingleDatasetScreenshots(TestResources.MOUSE_TEST_DATASET, rootFolder);

			takeMultiDatasetScreenshots(TestResources.MOUSE_TEST_DATASET,
					TestResources.MOUSE_CLUSTERS_DATASET, rootFolder);
		} else {
			return false;
		}

		nma.mw.setVisible(false);
		return true;
	}

	/**
	 * Load and take screenshots of the given dataset
	 * 
	 * @param file       the nmd file to load
	 * @param rootFolder the root folder for the screenshots.
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void takeSingleDatasetScreenshots(@NonNull File file, @NonNull File rootFolder)
			throws InterruptedException, IOException {

		LOGGER.fine("Taking images from " + file.getName());

		Thread.sleep(LOAD_TIME_MILLIS);

		uac.fileImportRequested(new FileImportEvent(this,
				file, IAnalysisDataset.XML_ANALYSIS_DATASET, null));

		// Wait for the dataset to load
		Thread.sleep(LOAD_TIME_MILLIS);

		selectSingleDataset();

		takeScreenshots(rootFolder, file.getName());

		dlm.clear();
	}

	public void takeMultiDatasetScreenshots(@NonNull File file1, @NonNull File file2,
			@NonNull File rootFolder) throws InterruptedException, IOException {
		Thread.sleep(LOAD_TIME_MILLIS);

		uac.fileImportRequested(new FileImportEvent(this,
				file1, IAnalysisDataset.XML_ANALYSIS_DATASET, null));

		// Wait for the dataset to load
		Thread.sleep(LOAD_TIME_MILLIS);

		uac.fileImportRequested(new FileImportEvent(this,
				file2, IAnalysisDataset.XML_ANALYSIS_DATASET, null));

		// Wait for the dataset to load
		Thread.sleep(LOAD_TIME_MILLIS);

		selectMultipleDatasets();

		takeScreenshots(rootFolder, "Multi");

		dlm.clear();
	}

	/**
	 * Select a single cell in the cells list panel
	 * 
	 * @param dp
	 */
	private void selectSingleDataset() {
		Point listPos = nma.mw.getLocationOnScreen();
		robot.mouseMove(listPos.x + 400, listPos.y + 90);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		try {
			Thread.sleep(20);
		} catch (InterruptedException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
	}

	/**
	 * Select a single cell in the cells list panel
	 * 
	 * @param dp
	 */
	private void selectMultipleDatasets() {
		Point listPos = nma.mw.getLocationOnScreen();
		robot.mouseMove(listPos.x + 400, listPos.y + 90);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		try {
			Thread.sleep(20);
		} catch (InterruptedException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
		robot.mouseMove(listPos.x + 400, listPos.y + 160);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
	}

	/**
	 * Take screenshots of the program, stepping though each tab
	 * 
	 * @param rootFolder the folder in which to save the images
	 * @param prefix     the prefix to apply to file names
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private void takeScreenshots(File rootFolder, String prefix)
			throws IOException, InterruptedException {
		File outputFolder = new File(rootFolder, prefix);
		outputFolder.mkdirs();

		TabPanelSwitcher s = new TabPanelSwitcher(nma.mw);

		DetailPanelScreenshotter dps = new DetailPanelScreenshotter(nma.mw, robot);
		while (s.hasNext()) {
			DetailPanel d = s.nextTab();
			Thread.sleep(SLEEP_TIME_MILLIS);
			dps.takeScreenShots(d, outputFolder, "");
		}
	}
}
