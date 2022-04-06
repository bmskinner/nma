package com.bmskinner.nuclear_morphology.analysis.detection;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jdom2.Element;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.ComponentTester;
import com.bmskinner.nuclear_morphology.TestResources;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileCreator;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.cells.DefaultCell;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.components.profiles.IProfile;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;
import com.bmskinner.nuclear_morphology.logging.ConsoleFormatter;
import com.bmskinner.nuclear_morphology.logging.ConsoleHandler;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.Prefs;

public class FluorescentNucleusFinderTest {
	
	private static final Logger LOGGER = Logger.getLogger(Loggable.PROJECT_LOGGER);
	
	static {		
		Prefs.setThreads(2); // Attempt to avoid issue 162
		for(Handler h : LOGGER.getHandlers())
			LOGGER.removeHandler(h);
		Handler h = new ConsoleHandler(new ConsoleFormatter());
		LOGGER.setLevel(Level.FINE);
		h.setLevel(Level.FINE);
		LOGGER.addHandler(h);
	}

	/**
	 * Test an image file that had issues during development
	 * @throws ImageImportException
	 * @throws ComponentCreationException
	 */
	@Test
	public void testNucleiFound() throws Exception {
		IAnalysisOptions op = OptionsFactory.makeDefaultRodentAnalysisOptions(TestResources.MOUSE_SIGNALS_INPUT_FOLDER);
    	HashOptions nucleus = op.getDetectionOptions(CellularComponent.NUCLEUS).get();
    	nucleus.setDouble(HashOptions.MIN_CIRC, 0.15);
    	nucleus.setDouble(HashOptions.MAX_CIRC, 0.85);
    	
    	nucleus.setInt(HashOptions.MIN_SIZE_PIXELS, 2000);
    	nucleus.setInt(HashOptions.MAX_SIZE_PIXELS, 10000);

		FluorescentNucleusFinder f = new FluorescentNucleusFinder(op, FinderDisplayType.PIPELINE);

		List<ICell> cells = f.findInImage(new File(TestResources.MOUSE_SIGNALS_INPUT_FOLDER, "P110.tiff"));
				
		for(ICell c : cells) {
			Element e = c.toXmlElement();
			ICell dup = new DefaultCell(e);
			
			IProfile p1 = ProfileCreator.createProfile(c.getPrimaryNucleus(), ProfileType.ANGLE);
    		IProfile p2 = ProfileCreator.createProfile(dup.getPrimaryNucleus(), ProfileType.ANGLE);
    		assertEquals(p1, p2);
			assertEquals("Nucleus "+c.getPrimaryNucleus().getID(), c.getPrimaryNucleus().getProfile(ProfileType.ANGLE), 
					dup.getPrimaryNucleus().getProfile(ProfileType.ANGLE));
			
			ComponentTester.testDuplicatesByField(c.getId().toString(), c, dup);
		}
		
	}

}
