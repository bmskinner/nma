package no.gui;

import java.io.File;

import ij.IJ;
import ij.gui.GenericDialog;
import ij.io.OpenDialog;

/**
 * This is a placeholder class to put at the end of the analysis
 * and allow a mapping file to be applied rather than reanalyse
 * the whole population
 */
public class PopulationSplitWindow {

	private GenericDialog gd;

	public PopulationSplitWindow(){
		gd = new GenericDialog("Finish analysis?");
		gd.enableYesNoCancel("Add mapping", "End analysis");
		gd.hideCancelButton();

		gd.showDialog();
	}
	
	public boolean getResult(){
		
		if (gd.wasOKed()){
			IJ.log("    Adding a mapping file");
			return true;
		}
		else{
			IJ.log("    Ending analysis");
			return false;
		}
		
	}
	
	public File addMappingFile(){

		OpenDialog fileDialog = new OpenDialog("Select a mapping file...");
		String fileName = fileDialog.getPath();

		return new File(fileName);
	}

	

}
