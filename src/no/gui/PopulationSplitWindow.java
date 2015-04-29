package no.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import no.collections.NucleusCollection;
import no.collections.RoundNucleusCollection;
import ij.gui.GenericDialog;
import ij.io.OpenDialog;

/**
 * This is a placeholder class to put at the end of the analysis
 * and allow a mapping file to be applied rather than reanalyse
 * the whole population
 */
public class PopulationSplitWindow {

	private GenericDialog gd;
	private List<NucleusCollection> collections = new ArrayList<NucleusCollection>(0);
//	private MainWindow mw;

	public PopulationSplitWindow(List<NucleusCollection> collections){
		this.collections = collections;
//		this.mw = mw;
//		gd = new GenericDialog("Finish analysis?");
//		gd.enableYesNoCancel("Add mapping", "End analysis");
//		gd.hideCancelButton();
//
//		gd.showDialog();
	}
	
//	public boolean getResult(){
//		
//		if (gd.wasOKed()){
//			mw.log("Adding a mapping file");
//			return true;
//		}
//		else{
//			mw.log("Ending analysis");
//			return false;
//		}
//		
//	}
	
	public NucleusCollection getCollection(){
		gd = new GenericDialog("Select nuclear population");
		
		List<String> items = new ArrayList<String>(0);
		for(NucleusCollection collection : this.collections){
			items.add(collection.getType());
		}
		
		String[] list = items.toArray(new String[0]);
	    gd.addChoice("Population", list, list[0]);
	    gd.showDialog();

	    
	    String collectionType = gd.getNextChoice();
	    for(NucleusCollection collection : this.collections){
			if(collection.getType().equals(collectionType)){
				return collection;
			}
		}
	    return new RoundNucleusCollection();
	}
	
	public File addMappingFile(){

		OpenDialog fileDialog = new OpenDialog("Select a mapping file...");
		String fileName = fileDialog.getPath();

		return new File(fileName);
	}

	

}
