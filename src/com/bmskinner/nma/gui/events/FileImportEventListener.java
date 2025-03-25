package com.bmskinner.nma.gui.events;

import java.io.File;

import org.jdom2.Document;

public interface FileImportEventListener {

	/**
	 * Inform listeners that a file has been imported
	 * 
	 * @param f
	 */
	void fileImported(FileImportEvent f);

	/**
	 * Inform listeners that a file should be imported
	 * 
	 * @param f
	 */
	void fileImportRequested(FileImportEvent f);

	/**
	 * Values relating to file import
	 * 
	 * @author Ben Skinner
	 *
	 */
	record FileImportEvent(Object source, File file, String type, Document document) {
	}

}
