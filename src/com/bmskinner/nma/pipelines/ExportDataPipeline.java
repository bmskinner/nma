package com.bmskinner.nma.pipelines;

import java.io.File;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.DefaultOptions;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.io.DatasetImportMethod;
import com.bmskinner.nma.io.DatasetStatsExporter;
import com.bmskinner.nma.io.Io;
import com.bmskinner.nma.io.XMLImportMethod;

public class ExportDataPipeline {

	private static final Logger LOGGER = Logger
			.getLogger(ExportDataPipeline.class.getName());

	private File nmdFile;

	/**
	 * Build a pipeline covering all the options within the given file
	 * 
	 * @param rootFolder the root folder
	 * @param xmlFile    the options for analysis
	 * @throws Exception
	 */
	public ExportDataPipeline(@NonNull final File nmdFile)
			throws Exception {
		this.nmdFile = nmdFile;
		run();
	}

	public void run() throws Exception {
		XMLImportMethod m = new XMLImportMethod(nmdFile);
		m.call();

		LOGGER.info("Read xml file");

		IAnalysisDataset d = new DatasetImportMethod(m.getXMLDocument()).call().getFirstDataset();

		LOGGER.info("Created dataset");

		File statsFile = new File(d.getSavePath().getParentFile(),
				d.getSavePath().getName() + Io.TAB_FILE_EXTENSION);
		LOGGER.info("Exporting data to :" + statsFile.getAbsolutePath());

		HashOptions exportOptions = new DefaultOptions();
		exportOptions.setInt(Io.PROFILE_SAMPLES_KEY, 100);
		new DatasetStatsExporter(statsFile, d, exportOptions)
				.call();
	}

}
