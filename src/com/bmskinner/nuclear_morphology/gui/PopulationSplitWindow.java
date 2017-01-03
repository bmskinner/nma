/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package com.bmskinner.nuclear_morphology.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.bmskinner.nuclear_morphology.components.CellCollection;

import ij.gui.GenericDialog;
import ij.io.OpenDialog;

/**
 * This is a placeholder class to put at the end of the analysis
 * and allow a mapping file to be applied rather than reanalyse
 * the whole population
 */
public class PopulationSplitWindow {

	private GenericDialog gd;
	private List<CellCollection> collections = new ArrayList<CellCollection>(0);
//	private MainWindow mw;

	public PopulationSplitWindow(List<CellCollection> collections){
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
	
	public CellCollection getCollection(){
		gd = new GenericDialog("Select nuclear population");
		
		List<String> items = new ArrayList<String>(0);
		for(CellCollection collection : this.collections){
			items.add(collection.getName());
		}
		
		String[] list = items.toArray(new String[0]);
	    gd.addChoice("Population", list, list[0]);
	    gd.showDialog();

	    
	    String collectionType = gd.getNextChoice();
	    for(CellCollection collection : this.collections){
			if(collection.getName().equals(collectionType)){
				return collection;
			}
		}
	    return null;
	}
	
	public File addMappingFile(){

		OpenDialog fileDialog = new OpenDialog("Select a mapping file...");
		String fileName = fileDialog.getPath();

		return new File(fileName);
	}

	

}
