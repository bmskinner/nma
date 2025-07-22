package com.bmskinner.nma.gui.tabs.populations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nma.TestResources;
import com.bmskinner.nma.components.XMLNames;
import com.bmskinner.nma.components.workspaces.IWorkspace;
import com.bmskinner.nma.components.workspaces.WorkspaceFactory;
import com.bmskinner.nma.core.DatasetListManager;
import com.bmskinner.nma.core.GlobalOptions;
import com.bmskinner.nma.core.NuclearMorphologyAnalysis;
import com.bmskinner.nma.gui.events.FileImportEventListener.FileImportEvent;
import com.bmskinner.nma.gui.events.UserActionController;

/**
 * Test UI updating when datasets are opened
 * 
 */
public class PopulationsPanelTest {

	private static final Logger LOGGER = Logger.getLogger(PopulationsPanelTest.class.getName());

	private NuclearMorphologyAnalysis nma;
	private UserActionController uac;
	private DatasetListManager dlm;

	/** Sleep time after loading a dataset */
	private static final int LOAD_TIME_MILLIS = 3000;

	@Before
	public void setUp() throws Exception {
		GlobalOptions.getInstance().set(GlobalOptions.ALLOW_UPDATE_CHECK_KEY, false);
		GlobalOptions.getInstance().set(GlobalOptions.WARN_LOW_JVM_MEMORY_FRACTION, false);
		nma = NuclearMorphologyAnalysis.getInstance();
		nma.runWithGUI();

		Thread.sleep(LOAD_TIME_MILLIS);

		uac = UserActionController.getInstance();
		dlm = DatasetListManager.getInstance();

	}

	@Test
	public void testWorkspaceImported() throws Exception {
		
		// Create a workspace with multiple datasets
		
		final IWorkspace w = WorkspaceFactory.createWorkspace("Multi-dataset workspace");
		
		w.add(TestResources.MOUSE_CLUSTERS_DATASET);
		w.add(TestResources.MOUSE_TEST_DATASET);
		w.add(TestResources.MOUSE_SIGNALS_DATASET);
		w.add(TestResources.PIG_TEST_DATASET);
		
		w.setSaveFile(new File(TestResources.datasetOutputFolder(), "Multi_dataset.wrk"));
		LOGGER.fine("Saving to %s".formatted(w.getSaveFile().getAbsoluteFile()));
		w.save();
		assertTrue(w.getSaveFile().exists());

		uac.fileImportRequested(new FileImportEvent(this,
				w.getSaveFile(), XMLNames.XML_WORKSPACE, null));

		// Wait for the dataset to load
		LOGGER.fine("Waiting for dataset to load");
		Thread.sleep(LOAD_TIME_MILLIS);

		LOGGER.fine("Selecting loaded dataset");
		dlm.setSelectedDatasets(dlm.getRootDatasets());

		assertEquals("Only one workspace should be present", 1, dlm.getWorkspaces().size());

	}

}
