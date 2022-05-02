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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Document;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import com.bmskinner.nma.analysis.DefaultAnalysisResult;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.logging.Loggable;

/**
 * Export the dataset to an nmd file
 * 
 * @author bms41
 *
 */
public class DatasetExportMethod extends SingleDatasetAnalysisMethod {

	private static final Logger LOGGER = Logger.getLogger(DatasetExportMethod.class.getName());

	private File saveFile = null;

	/**
	 * Construct with a dataset to export and the file location
	 * 
	 * @param dataset  the dataset to be exported
	 * @param saveFile the file to export to
	 */
	public DatasetExportMethod(@NonNull IAnalysisDataset dataset, @NonNull File saveFile) {
		super(dataset);
		this.saveFile = saveFile;
	}

	@Override
	public IAnalysisResult call() throws Exception {
		run();
		return new DefaultAnalysisResult(dataset);
	}

	protected void run() throws Exception {
		boolean isOk = false;
		backupExistingSaveFile();
		isOk = saveAnalysisDatasetToXML(dataset, saveFile);

		if (!isOk)
			LOGGER.warning("Save was unsucessful");
	}

	/**
	 * Save the given dataset in XML format
	 * 
	 * @param dataset  the dataset to save
	 * @param saveFile the file to save to
	 * @return
	 * @throws IOException
	 */
	public boolean saveAnalysisDatasetToXML(IAnalysisDataset dataset, File saveFile)
			throws IOException {
		boolean ok = true;
		LOGGER.fine("Saving XML dataset to " + saveFile.getAbsolutePath());

		File parentFolder = saveFile.getParentFile();
		if (!parentFolder.exists())
			parentFolder.mkdirs();

		if (saveFile.isDirectory())
			throw new IllegalArgumentException(
					String.format("File %s is a directory", saveFile.getName()));
		if (saveFile.getParentFile() == null)
			throw new IllegalArgumentException(
					String.format("Parent directory %s is null", saveFile.getAbsolutePath()));
		if (!saveFile.getParentFile().canWrite())
			throw new IllegalArgumentException(String.format("Parent directory %s is not writable",
					saveFile.getParentFile().getName()));
		Document doc = new Document(dataset.toXmlElement());

//		try (
		OutputStream os = new FileOutputStream(saveFile);
		CountedOutputStream cos = new CountedOutputStream(os);
		cos.addCountListener((l) -> fireProgressEvent(l));
		XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());
		xmlOutput.output(doc, cos);
//		} catch (IOException e) {
//			LOGGER.log(Loggable.STACK, String.format("Unable to write to file %s: %s",
//					saveFile.getAbsolutePath(), e.getMessage()), e);
//			ok = false;
//		}
		return ok;
	}

	private void backupExistingSaveFile() {
		File saveFile = dataset.getSavePath();
		if (!saveFile.exists())
			return;

		File backupFile = new File(saveFile.getParent(),
				saveFile.getName().replaceAll(Io.NMD_FILE_EXTENSION,
						Io.BACKUP_FILE_EXTENSION));
		try {
			copyFile(saveFile, backupFile);
		} catch (IOException e) {
			LOGGER.log(Loggable.STACK, e.getMessage(), e);
		}
	}

	/**
	 * Directly copy the source file to the destination file
	 * 
	 * @param sourceFile
	 * @param destFile
	 * @throws IOException
	 */
	public static void copyFile(File sourceFile, File destFile) throws IOException {
		if (!destFile.exists())
			destFile.createNewFile();

		try (FileChannel source = new FileInputStream(sourceFile).getChannel();
				FileChannel destination = new FileOutputStream(destFile).getChannel();) {
			destination.transferFrom(source, 0, source.size());
		}
	}

}
