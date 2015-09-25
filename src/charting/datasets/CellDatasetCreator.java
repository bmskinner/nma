/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
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
package charting.datasets;

import ij.IJ;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import components.Cell;
import components.nuclear.NuclearSignal;
import components.nuclei.Nucleus;
import utility.Utils;

public class CellDatasetCreator {
	
	/**
	 * Create a table of stats for the given cell.
	 * @param cell the cell
	 * @return a table model
	 */
	public static TableModel createCellInfoTable(Cell cell){

		DefaultTableModel model = new DefaultTableModel();
		
		List<Object> fieldNames = new ArrayList<Object>(0);
		List<Object> rowData 	= new ArrayList<Object>(0);
		
		DecimalFormat df = new DecimalFormat("#0.00"); 
				
		// find the collection with the most channels
		// this defines  the number of rows

		if(cell==null){
			model.addColumn("No data loaded");
			
		} else {
			
			Nucleus n = cell.getNucleus();
			
			fieldNames.add("Image");
			rowData.add(n.getPathAndNumber());
			
			fieldNames.add("Area");
			rowData.add(df.format(n.getArea()));
			
			fieldNames.add("Perimeter");
			rowData.add(df.format(n.getPerimeter()));
			
			fieldNames.add("Max feret");
			rowData.add(df.format(n.getFeret()));
			
			fieldNames.add("Min feret");
			rowData.add(df.format(n.getNarrowestDiameter()));
			
			fieldNames.add("Nucleus CoM");
			rowData.add(n.getCentreOfMass().toString());
			
			fieldNames.add("Nucleus position");
			rowData.add(n.getPosition()[0]+"-"+n.getPosition()[1]);
			
			fieldNames.add("Scale (um/pixel)");
			rowData.add(n.getScale());
			

			for(String tag : n.getBorderTags().keySet()){
				fieldNames.add(tag);
				int index = Utils.wrapIndex(n.getBorderIndex(tag)- n.getBorderIndex(n.getReferencePoint()), n.getLength());
				rowData.add(index);
			}

			// add info for signals
			for(int signalGroup : n.getSignalGroups()){
				
				fieldNames.add("");
				rowData.add("");
				
				fieldNames.add("Signal group");
				rowData.add(signalGroup);
				
				fieldNames.add("Signal name");
				rowData.add(n.getSignalCollection().getSignalGroupName(signalGroup));
				
				fieldNames.add("Number of signals");
				rowData.add(n.getSignalCount(signalGroup));
				
				for(NuclearSignal s : n.getSignals(signalGroup)){
					
					fieldNames.add("Signal area");
					rowData.add(s.getArea());
					
					fieldNames.add("Signal CoM");
					
					int comX = s.getCentreOfMass().getXAsInt()+ (int) n.getPosition()[Nucleus.X_BASE];
					int comY = s.getCentreOfMass().getYAsInt()+ (int) n.getPosition()[Nucleus.Y_BASE];
					rowData.add(comX+", "+comY);
				}			
				
			}
			
			model.addColumn("", fieldNames.toArray(new Object[0])); 
			model.addColumn("Info", rowData.toArray(new Object[0]));

			}
		return model;	
	}

}
