package com.bmskinner.nma.analysis.detection;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collection;

import org.junit.Test;

import com.bmskinner.nma.TestResources;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.components.options.OptionsFactory;

public class TextFileNucleusFinderTest {

	@Test
	public void test() throws Exception {
		IAnalysisOptions op = OptionsFactory
				.makeDefaultRodentAnalysisOptions(TestResources.TEXT_OUTLINES_FOLDER);
		HashOptions nucleus = op.getDetectionOptions(CellularComponent.NUCLEUS).get();
		nucleus.setDouble(HashOptions.MIN_CIRC, 0.15);
		nucleus.setDouble(HashOptions.MAX_CIRC, 1);

		nucleus.setInt(HashOptions.MIN_SIZE_PIXELS, 500);
		nucleus.setInt(HashOptions.MAX_SIZE_PIXELS, 10000);

		TextFileNucleusFinder f = new TextFileNucleusFinder(op);

		Collection<ICell> cells = f
				.findInFile(new File(TestResources.TEXT_OUTLINES_FOLDER, "1012-1-1.jpg.txt"));

		assertTrue("Cells were found", cells.size() > 0);
	}

}
