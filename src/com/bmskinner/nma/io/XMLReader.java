package com.bmskinner.nma.io;

import java.io.File;
import java.io.IOException;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import com.bmskinner.nma.components.Version.UnsupportedVersionException;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.datasets.DatasetCreator;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.DefaultAnalysisOptions;
import com.bmskinner.nma.components.options.DefaultOptions;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.components.rules.RuleSetCollection;
import com.bmskinner.nma.components.workspaces.DefaultWorkspace;
import com.bmskinner.nma.components.workspaces.IWorkspace;

/**
 * Base class for XML readers
 * 
 * @author bms41
 * @since 1.14.0
 */
public abstract class XMLReader {

	private XMLReader() {
	}

	/**
	 * Read the given file as an XML document
	 * 
	 * @param file the file to read
	 * @return the XML representation of the file content
	 * @throws XMLReadingException if the document could not be read or was not XML
	 */
	public static Document readDocument(File file) throws XMLReadingException {
		SAXBuilder saxBuilder = new SAXBuilder();
		try {
			return saxBuilder.build(file);
		} catch (JDOMException | IOException e) {
			throw new XMLReadingException(String.format("Unable to read file %s as XML: %s",
					file.getAbsolutePath(), e.getMessage()), e);
		}
	}

	public static IAnalysisDataset readDataset(File f)
			throws XMLReadingException, ComponentCreationException, UnsupportedVersionException {
		Document d = readDocument(f);
		return DatasetCreator.createRoot(d.getRootElement(), null);
	}

	public static RuleSetCollection readRulesetCollection(File f)
			throws XMLReadingException, ComponentCreationException {
		Document d = readDocument(f);
		return new RuleSetCollection(d.getRootElement());
	}

	public static HashOptions readOptions(File f) throws XMLReadingException {
		Document d = readDocument(f);
		return new DefaultOptions(d.getRootElement());
	}

	public static IAnalysisOptions readAnalysisOptions(File f)
			throws XMLReadingException, ComponentCreationException {
		Document d = readDocument(f);
		return new DefaultAnalysisOptions(d.getRootElement());
	}

	public static IWorkspace readWorkspace(File f) throws XMLReadingException {
		Document d = readDocument(f);
		return new DefaultWorkspace(f, d.getRootElement());
	}

	/**
	 * Parse a string to an array, assuming it has the format: [1, 2, 3, 4, 5]
	 * 
	 * @param arrayText
	 * @return
	 */
	public static long[] parseLongArray(String arrayText) {
		String[] s = arrayText.replace("[", "")
				.replace("]", "")
				.replace(" ", "")
				.split(",");

		long[] l = new long[s.length];
		for (int i = 0; i < s.length; i++) {
			l[i] = Long.parseLong(s[i]);
		}
		return l;
	}

	/**
	 * Parse a string to an array, assuming it has the format: [1, 2, 3, 4, 5]
	 * 
	 * @param arrayText
	 * @return
	 */
	public static int[] parseIntArray(String arrayText) {
		String[] s = arrayText.replace("[", "")
				.replace("]", "")
				.replace(" ", "")
				.split(",");

		int[] l = new int[s.length];
		for (int i = 0; i < s.length; i++) {
			l[i] = Integer.parseInt(s[i]);
		}
		return l;
	}

	/**
	 * Thrown when an xml file cannot be read
	 * 
	 * @author bms41
	 * @since 1.14.0
	 *
	 */
	public static class XMLReadingException extends Exception {
		private static final long serialVersionUID = 1L;

		public XMLReadingException() {
			super();
		}

		public XMLReadingException(String message) {
			super(message);
		}

		public XMLReadingException(String message, Throwable cause) {
			super(message, cause);
		}

		public XMLReadingException(Throwable cause) {
			super(cause);
		}
	}
}
