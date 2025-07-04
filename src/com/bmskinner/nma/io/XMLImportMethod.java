package com.bmskinner.nma.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import com.bmskinner.nma.analysis.AbstractAnalysisMethod;
import com.bmskinner.nma.analysis.DefaultAnalysisResult;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.io.Io.Importer;
import com.bmskinner.nma.logging.Loggable;

public class XMLImportMethod extends AbstractAnalysisMethod implements Importer {

	private static final Logger LOGGER = Logger.getLogger(XMLImportMethod.class.getName());

	private final File file;
	private Document doc = null;

	public XMLImportMethod(@NonNull File f) {
		super();
		this.file = f;
	}

	@Override
	public IAnalysisResult call() throws Exception {

		run();
		return new DefaultAnalysisResult((IAnalysisDataset) null);
	}

	public Document getXMLDocument() {
		return doc;
	}

	private void run() {

		// Deserialise whatever is in the file
		try (InputStream is = new FileInputStream(file);
				CountedInputStream cis = new CountedInputStream(is);) {

			cis.addCountListener((l) -> fireProgressEvent(l));
			SAXBuilder saxBuilder = new SAXBuilder();
			doc = saxBuilder.build(cis);
			fireIndeterminateState();
		} catch (IOException | JDOMException e) {
			LOGGER.log(Loggable.STACK,
					"Could not parse file as XML: " + file.getName() + ": " + e.getMessage(), e);
		}
	}

}
