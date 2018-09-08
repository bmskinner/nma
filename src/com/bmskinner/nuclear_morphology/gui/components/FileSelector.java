/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.gui.components;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.io.Io;
import com.bmskinner.nuclear_morphology.io.Io.Importer;


/**
 * Provides methods for selecting import and export files
 * @author bms41
 * @since 1.13.7
 *
 */
public class FileSelector {

    public static @Nullable File chooseTableExportFile(){
        
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Table export file", "txt");
        
        File dir = GlobalOptions.getInstance().getDefaultDir();
        File file = chooseSaveFile(dir, filter, null);
        
        if(file==null)
            return null;

        if (!file.getAbsolutePath().endsWith(Io.TAB_FILE_EXTENSION)) {
            file = new File(file.getAbsolutePath() + Io.TAB_FILE_EXTENSION);
        }

        return file;

    }
    
    /**
     * Choose the folder to export dataset stats.
     * @param datasets the datasets to be exported
     * @return the file to export to
     */
    public static @Nullable File chooseStatsExportFile(@NonNull List<IAnalysisDataset> datasets, @Nullable String suffix) {

        File dir = null;
        suffix = suffix==null ? "stats" : suffix;
        String defaultName = "";
        if (datasets.size() == 1) {
            dir = datasets.get(0).getSavePath().getParentFile();
            defaultName = datasets.get(0).getName()+"_"+suffix+Io.TAB_FILE_EXTENSION;
        } else {
            dir = IAnalysisDataset.commonPathOfFiles(datasets);
            if (!dir.exists() || !dir.isDirectory()) {
                dir = GlobalOptions.getInstance().getDefaultDir();
                defaultName = "Multiple_"+suffix+"_export"+Io.TAB_FILE_EXTENSION;
            }
        }
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Table export file", "txt");
        
        File file = chooseSaveFile(dir, filter, defaultName);
        if(file==null)
            return null;

        // Add extension if needed
        if (!file.getAbsolutePath().endsWith(Io.TAB_FILE_EXTENSION)) {
            file = new File(file.getAbsolutePath() + Io.TAB_FILE_EXTENSION);
        }

        return file;
    }
    
    /**
     * Choose the folder to export dataset stats.
     * @param datasets the datasets to be exported
     * @return the file to export to
     */
    public static @Nullable File chooseOptionsExportFile(@NonNull IAnalysisDataset dataset) {


    	File dir = dataset.getSavePath().getParentFile();
    	String defaultName = dataset.getName()+Io.XML_FILE_EXTENSION;

        FileNameExtensionFilter filter = new FileNameExtensionFilter("Options file", "xml");
        
        File file = chooseSaveFile(dir, filter, defaultName);
        if(file==null)
            return null;

        // Add extension if needed
        if (!file.getAbsolutePath().endsWith(Io.XML_FILE_EXTENSION))
            file = new File(file.getAbsolutePath() + Io.XML_FILE_EXTENSION);
        return file;
    }
    
    /**
     * Choose the folder to export dataset stats.
     * @param datasets the datasets to be exported
     * @return the file to export to
     */
    public static @Nullable File chooseOptionsImportFile() {
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Options file", "xml");
        return chooseFile(null, filter, "Choose options file");
    }
    
    /**
     * Get the remapping file to be loaded.
     * 
     * @return the file
     */
    public static @Nullable File chooseRemappingFile(@NonNull IAnalysisDataset dataset) {

        FileNameExtensionFilter filter = new FileNameExtensionFilter("Remapping file", Importer.LOC_FILE_EXTENSION);
        File defaultDir = null;
        
        Optional<IAnalysisOptions> op = dataset.getAnalysisOptions();
        if(!op.isPresent())
        	return null;
        
        Optional<IDetectionOptions> im = op.get().getDetectionOptions(IAnalysisOptions.NUCLEUS);
        if(!im.isPresent())
        	return null;

        defaultDir = im.get().getFolder();
        return chooseOpenFile(defaultDir, filter, "Choose remapping file");
    }
    
    /**
     * Choose a file from a default folder with a file extension filter.
     * @param defaultFolder the default folder
     * @param filter the filename extension filter
     * @return the selected file, or null on cancel or error
     */
    public static @Nullable File chooseFile(File defaultFolder, @Nullable FileNameExtensionFilter filter, @Nullable String message){
        return chooseOpenFile(defaultFolder, filter, message);
    }
    
    /**
     * Choose a file from a default folder.
     * @param defaultFolder the default folder
     * @return the selected file, or null on cancel or error
     */
    public static @Nullable File chooseFile(File defaultFolder){
        return chooseOpenFile(defaultFolder, null, null);
    }
        
    /**
     * Choose a file from a default folder with a file extension filter.
     * @param defaultFolder the default folder
     * @param filter the filename extension filter
     * @return the selected file, or null on cancel or error
     */
    private static @Nullable File chooseOpenFile(File defaultFolder, FileNameExtensionFilter filter, @Nullable String message){
        JFileChooser fc= new JFileChooser(defaultFolder);

        if(filter!=null)
            fc.setFileFilter(filter);
        
        if(message!=null)
        	fc.setDialogTitle(message);;

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
     * @param defaultFolder the default folder
     * @param filter the filename extension filter
     * @return the selected file, or null on cancel or error
     */
    public static @Nullable File chooseSaveFile(@Nullable File defaultFolder, @Nullable FileNameExtensionFilter filter, @Nullable String defaultName){
        JFileChooser fc= new JFileChooser(defaultFolder);

        if(filter!=null)
            fc.setFileFilter(filter);

        fc.setDialogTitle("Specify a file to save as");
        
        if(defaultName!=null)
            fc.setSelectedFile(new File(defaultFolder, defaultName));

        int returnVal = fc.showSaveDialog(fc);
        if (returnVal != 0)
            return null; // user cancelled

        return fc.getSelectedFile();
    }
    
    /**
     * Create a file chooser for the user to select a folder 
     * @param defaultFolder the default folder for the file chooser
     * @return the selected folder, or null if cancelled or error
     */
    public static @Nullable File chooseFolder(@Nullable String title, @Nullable File defaultFolder){
        
    	if(defaultFolder!=null && !defaultFolder.exists())
    		defaultFolder=null;
    	
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
     * @param datasets the datasets in the workspace
     * @return a workspace file
     */
    public static @Nullable File chooseWorkspaceExportFile(List<IAnalysisDataset> datasets) {

        String fileName = null;
        File dir = null;
        if (datasets.size() == 1) {
            dir = datasets.get(0).getSavePath().getParentFile();
            fileName = datasets.get(0).getName() + Importer.WRK_FILE_EXTENSION;

        } else {
            fileName = "Workspace" + Importer.WRK_FILE_EXTENSION;
            dir = IAnalysisDataset.commonPathOfFiles(datasets);
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
        if (!file.getAbsolutePath().endsWith(Importer.WRK_FILE_EXTENSION)) {
            file = new File(file.getAbsolutePath() + Importer.WRK_FILE_EXTENSION);
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
        if(!op.isPresent())
        	return null;
        
        Optional<IDetectionOptions> im = op.get().getDetectionOptions(IAnalysisOptions.NUCLEUS);
        if(!im.isPresent())
        	return null;

        File defaultDir = im.get().getFolder();
        return chooseFolder(null, defaultDir);
    }
    
    
    /**
     * Choose the directory containing the post-FISH images
     * 
     * @param dataset the analysis dataset
     * @return the selected folder, or null if user cancelled or invalid choice
     */
    public static File choosePostFISHDirectory(IAnalysisDataset dataset) {
    	Optional<IAnalysisOptions> op = dataset.getAnalysisOptions();
        if(!op.isPresent())
        	return null;
        
        Optional<IDetectionOptions> im = op.get().getDetectionOptions(IAnalysisOptions.NUCLEUS);
        if(!im.isPresent())
        	return null;
        
        File defaultDir =  im.get().getFolder();
        return chooseFolder(null, defaultDir);
    }
    
    /**
     * Get the signal image directory for the given signal group in a dataset
     * @param dataset the dataset
     * @param signalGroupId the signal group
     * @return the new file
     */
    public static File getSignalDirectory(@NonNull final IAnalysisDataset dataset, @NonNull final UUID signalGroupId) {

    	if(!dataset.getCollection().hasSignalGroup(signalGroupId))
    		return null;
    	
        String signalName = dataset.getCollection().getSignalGroup(signalGroupId).get().getGroupName();
		
		JOptionPane.showMessageDialog(null, "Choose the folder with images for signal group " + signalName);

		// We expect the signal images to be in the folder above the nmd file
		File defaultFolder = dataset.getSavePath().getParentFile();
		JFileChooser fc = new JFileChooser(defaultFolder);
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		int returnVal = fc.showOpenDialog(fc);
		if (returnVal != 0) {
		    return null;
		}

		File file = fc.getSelectedFile();
		return file;
    }

    /**
     * Check if the given folder has files (not just directories)
     * 
     * @param folder
     * @return
     */
    private static boolean containsFiles(File folder) {

        File[] files = folder.listFiles();

        // There must be items in the folder
        if (files == null || files.length == 0) {
            return false;
        }

        int countFiles = 0;

        // Some of the items must be files
        for (File f : files) {
            if (f.isFile()) {
                countFiles++;
            }
        }

        return countFiles>0;
    }

}
