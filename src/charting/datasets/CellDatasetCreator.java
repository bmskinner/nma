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
import stats.NucleusStatistic;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import utility.Utils;
import components.Cell;
import components.generic.BorderTag;
import components.generic.MeasurementScale;
import components.generic.BorderTag.BorderTagType;
import components.nuclear.NuclearSignal;
import components.nuclear.NucleusType;
import components.nuclei.Nucleus;

public class CellDatasetCreator {
	
	/**
	 * Create a table of stats for the given cell.
	 * @param cell the cell
	 * @return a table model
	 * @throws Exception 
	 */
	public static TableModel createCellInfoTable(Cell cell) throws Exception{

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
			
			fieldNames.add("Scale (um/pixel)");
			rowData.add(n.getScale());
			
			for(NucleusStatistic stat : NucleusStatistic.values()){
				
				if(!stat.equals(NucleusStatistic.VARIABILITY)){
					
					fieldNames.add(stat.label(MeasurementScale.PIXELS)  );

					double pixel = n.getStatistic(stat, MeasurementScale.PIXELS);
					
					if(stat.isDimensionless()){
						rowData.add(df.format(pixel) );
					} else {
						double micron = n.getStatistic(stat, MeasurementScale.MICRONS);
						rowData.add(df.format(pixel) +" ("+ df.format(micron)+ " "+ stat.units(MeasurementScale.MICRONS)+")");
					}
					
				}
				
			}

			fieldNames.add("Nucleus CoM");
			rowData.add(n.getCentreOfMass().toString());
			
			fieldNames.add("Nucleus position");
			rowData.add(n.getPosition()[0]+"-"+n.getPosition()[1]);
			
			
			
			NucleusType type = NucleusType.getNucleusType(n);
			
			if(type!=null){
				for(BorderTag tag : BorderTag.values(BorderTagType.CORE)){
					fieldNames.add(type.getPoint(tag));
					int index = Utils.wrapIndex(n.getBorderIndex(tag)- n.getBorderIndex(BorderTag.REFERENCE_POINT), n.getLength());
					rowData.add(index);
				}
			} else {
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
					rowData.add(s.getArea()+" ("+ df.format(Utils.micronArea(s.getArea(), n.getScale()))+" microns)");
					
					fieldNames.add("Signal CoM");
					
					int comX = s.getCentreOfMass().getXAsInt()+ (int) n.getPosition()[Nucleus.X_BASE];
					int comY = s.getCentreOfMass().getYAsInt()+ (int) n.getPosition()[Nucleus.Y_BASE];
					rowData.add(comX+", "+comY);
					
					fieldNames.add("Signal angle");
					rowData.add(s.getAngle());
				}			
				
			}
			
			model.addColumn("", fieldNames.toArray(new Object[0])); 
			model.addColumn("Info", rowData.toArray(new Object[0]));

			}
		return model;	
	}

}
