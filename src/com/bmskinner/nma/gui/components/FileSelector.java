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
package com.bmskinner.nma.gui.components;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.core.GlobalOptions;
import com.bmskinner.nma.io.Io;
import com.bmskinner.nma.utility.FileUtils;

/**
 * Provides methods for selecting import and export files
 * 
 * @author Ben Skinner
 * @since 1.13.7
 *
 */
public class FileSelector {

	private FileSelector() {
	}

	public static @Nullable File chooseTableExportFile() {

		FileNameExtensionFilter filter = new FileNameExtensionFilter("Table export file", "txt");

		File dir = GlobalOptions.getInstance().getDefaultDir();
		File file = chooseSaveFile(dir, filter, null);

		if (file == null)
			return null;

		if (!file.getAbsolutePath().endsWith(Io.TAB_FILE_EXTENSION)) {
			file = new File(file.getAbsolutePath() + Io.TAB_FILE_EXTENSION);
		}

		return file;

	}

	/**
	 * Choose the folder to export dataset stats.
	 * 
	 * @param datasets the datasets to be exported
	 * @return the file to export to
	 */
	public static @Nullable File chooseStatsExportFile(@NonNull List<IAnalysisDataset> datasets,
			@Nullable String suffix) {

		File dir = null;
		suffix = suffix == null ? "stats" : suffix;
		String defaultName = "";
		if (datasets.size() == 1) {
			dir = datasets.get(0).getSavePath().getParentFile();
			defaultName = datasets.get(0).getName() + "_" + suffix + Io.TAB_FILE_EXTENSION;
		} else {
			dir = FileUtils.commonPathOfDatasets(datasets);

			if (!dir.exists() || !dir.isDirectory()) {
				dir = GlobalOptions.getInstance().getDefaultDir();
				defaultName = "Multiple_" + suffix + "_export" + Io.TAB_FILE_EXTENSION;
			}
		}

		FileNameExtensionFilter filter = new FileNameExtensionFilter("Table export file", "txt");

		File file = chooseSaveFile(dir, filter, defaultName);
		if (file == null)
			return null;

		// Add extension if needed
		if (!file.getAbsolutePath().endsWith(Io.TAB_FILE_EXTENSION)) {
			file = new File(file.getAbsolutePath() + Io.TAB_FILE_EXTENSION);
		}

		return file;
	}

	/**
	 * Choose the folder to export dataset stats.
	 * 
	 * @param datasets the datasets to be exported
	 * @return the file to export to
	 */
	public static @Nullable File chooseOptionsExportFile(@NonNull IAnalysisDataset dataset) {

		File dir = dataset.getSavePath().getParentFile();
		String defaultName = dataset.getName() + Io.XML_FILE_EXTENSION;

		FileNameExtensionFilter filter = new FileNameExtensionFilter("Options file", "xml");

		File file = chooseSaveFile(dir, filter, defaultName);
		if (file == null)
			return null;

		// Add extension if needed
		if (!file.getAbsolutePath().endsWith(Io.XML_FILE_EXTENSION))
			file = new File(file.getAbsolutePath() + Io.XML_FILE_EXTENSION);
		return file;
	}

	/**
	 * Choose the folder to import detection options.
	 * 
	 * @param datasets the datasets to be exported
	 * @return the file to export to
	 */
	public static @Nullable File chooseOptionsImportFile(@Nullable File defaultFolder) {
		FileNameExtensionFilter filter = new FileNameExtensionFilter("Options file", "xml");
		return chooseFile(defaultFolder, filter, "Choose options file");
	}

	/**
	 * Get the remapping file to be loaded.
	 * 
	 * @return the file
	 */
	public static @Nullable File chooseRemappingFile(@NonNull IAnalysisDataset dataset) {

		FileNameExtensionFilter filter = new FileNameExtensionFilter("Remapping file",
				Io.LOC_FILE_EXTENSION);

		Optional<IAnalysisOptions> op = dataset.getAnalysisOptions();
		if (!op.isPresent())
			return null;

		Optional<File> of = op.get().getNucleusDetectionFolder();
		if (!of.isPresent())
			return null;

		return chooseOpenFile(of.get(), filter, "Choose remapping file");
	}

	/**
	 * Choose a file from a default folder with a file extension filter.
	 * 
	 * @param defaultFolder the default folder
	 * @param filter        the filename extension filter
	 * @param message		the message to display on the file picker
	 * @return the selected file, or null on cancel or error
	 */
	public static @Nullable File chooseFile(File defaultFolder,
			@Nullable FileNameExtensionFilter filter, @Nullable String message) {
		return chooseOpenFile(defaultFolder, filter, message);
	}

	/**
	 * Choose a file from a default folder.
	 * 
	 * @param defaultFolder the default folder
	 * @return the selected file, or null on cancel or error
	 */
	public static @Nullable File chooseFile(File defaultFolder) {
		return chooseOpenFile(defaultFolder, null, null);
	}

	/**
	 * Choose a file from a default folder with a file extension filter.
	 * 
	 * @param defaultFolder the default folder
	 * @param filter        the filename extension filter
	 * @return the selected file, or null on cancel or error
	 */
	private static @Nullable File chooseOpenFile(File defaultFolder, FileNameExtensionFilter filter,
			@Nullable String message) {
		JFileChooser fc = new JFileChooser(defaultFolder);

		if (filter != null)
			fc.setFileFilter(filter);

		if (message != null)
			fc.setDialogTitle(message);

		int returnVal = fc.showOpenDialog(fc);
		if (returnVal != 0)
			return null;

		File file = fc.getSelectedFile();

		if (file.isDirectory())
			return null;
		return file;
	}

	/**
	 * Choose a file from a default folder with a file extension filter.
	 * 
	 * @param defaultFolder the default folder
	 * @param filter        the filename extension filter
	 * @return the selected file, or null on cancel or error
	 */
	public static @Nullable File chooseSaveFile(@Nullable File defaultFolder,
			@Nullable FileNameExtensionFilter filter, @Nullable String defaultName) {
		JFileChooser fc = new JFileChooser(defaultFolder);

		if (filter != null)
			fc.setFileFilter(filter);

		fc.setDialogTitle("Specify a file to save as");

		if (defaultName != null)
			fc.setSelectedFile(new File(defaultFolder, defaultName));

		int returnVal = fc.showSaveDialog(fc);
		if (returnVal != 0)
			return null; // user cancelled

		return fc.getSelectedFile();
	}

	/**
	 * Create a file chooser for the user to select a folder
	 * 
	 * @param defaultFolder the default folder for the file chooser
	 * @return the selected folder, or null if cancelled or error
	 */
	public static @Nullable File chooseFolder(@Nullable String title,
			@Nullable File defaultFolder) {

		if (defaultFolder != null && !defaultFolder.exists())
			defaultFolder = null;

		JFileChooser fc = new JFileChooser(defaultFolder); // if null, will be home
		fc.setDialogTitle(title);
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		int returnVal = fc.showOpenDialog(fc);
		if (returnVal != 0)
			return null; // user cancelled

		File file = fc.getSelectedFile();

		if (!file.isDirectory())
			return null;
		return file;

	}

	/**
	 * Choose the file to save the workspace to
	 * 
	 * @param datasets the datasets in the workspace
	 * @return a workspace file
	 */
	public static @Nullable File chooseWorkspaceExportFile(List<IAnalysisDataset> datasets) {

		String fileName = null;
		File dir = null;
		if (datasets.size() == 1) {
			dir = datasets.get(0).getSavePath().getParentFile();
			fileName = datasets.get(0).getName() + Io.WRK_FILE_EXTENSION;

		} else {
			fileName = "Workspace" + Io.WRK_FILE_EXTENSION;
			dir = FileUtils.commonPathOfDatasets(datasets);
			if (!dir.exists() || !dir.isDirectory()) {
				dir = new File(System.getProperty("user.home"));
			}
		}

		JFileChooser fc = new JFileChooser(dir);
		fc.setSelectedFile(new File(fileName));
		fc.setDialogTitle("Save workspace as");

		int returnVal = fc.showSaveDialog(fc);
		if (returnVal != 0) {
			return null; // user cancelled
		}

		File file = fc.getSelectedFile();

		// Add extension if needed
		if (!file.getAbsolutePath().endsWith(Io.WRK_FILE_EXTENSION)) {
			file = new File(file.getAbsolutePath() + Io.WRK_FILE_EXTENSION);
		}

		return file;
	}

	/**
	 * Choose the directory containing the FISH images
	 * 
	 * @param dataset the analysis dataset
	 * @return the selected folder, or null if user cancelled or invalid choice
	 */
	public static File chooseFISHDirectory(IAnalysisDataset dataset) {

		Optional<IAnalysisOptions> op = dataset.getAnalysisOptions();
		if (!op.isPresent())
			return null;

		Optional<File> of = op.get().getNucleusDetectionFolder();
		if (!of.isPresent())
			return null;

		return chooseFolder(null, of.get());
	}

	/**
	 * Choose the directory containing the post-FISH images
	 * 
	 * @param dataset the analysis dataset
	 * @return the selected folder, or null if user cancelled or invalid choice
	 */
	public static File choosePostFISHDirectory(IAnalysisDataset dataset) {
		Optional<IAnalysisOptions> op = dataset.getAnalysisOptions();
		if (!op.isPresent())
			return null;

		Optional<File> of = op.get().getNucleusDetectionFolder();
		if (!of.isPresent())
			return null;

		return chooseFolder(null, of.get());
	}

	/**
	 * Get the signal image directory for the given signal group in a dataset
	 * 
	 * @param dataset       the dataset
	 * @param signalGroupId the signal group
	 * @return the new file
	 */
	public static File getSignalDirectory(@NonNull final IAnalysisDataset dataset,
			@NonNull final UUID signalGroupId) {

		if (!dataset.getCollection().hasSignalGroup(signalGroupId))
			return null;

		String signalName = dataset.getCollection().getSignalGroup(signalGroupId).get()
				.getGroupName();

		JOptionPane.showMessageDialog(null,
				"Choose the folder with images for signal group " + signalName);

		// We expect the signal images to be in the folder above the nmd file
		File defaultFolder = dataset.getSavePath().getParentFile();
		JFileChooser fc = new JFileChooser(defaultFolder);
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		int returnVal = fc.showOpenDialog(fc);
		if (returnVal != 0)
			return null;

		return fc.getSelectedFile();
	}
}
