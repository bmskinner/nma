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
import stats.SignalStatistic;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import utility.Utils;
import components.AbstractCellularComponent;
import components.Cell;
import components.CellularComponent;
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
			
			fieldNames.add("Source image");
			rowData.add(n.getPathAndNumber());
			
			fieldNames.add("Source channel");
			rowData.add(n.getChannel());
			
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
					int index = AbstractCellularComponent.wrapIndex(n.getBorderIndex(tag)- n.getBorderIndex(BorderTag.REFERENCE_POINT), n.getBorderLength());
					rowData.add(index);
				}
			} 

			// add info for signals
			for(UUID signalGroup : n.getSignalCollection().getSignalGroupIDs()){
				
				fieldNames.add("");
				rowData.add("");
				
				fieldNames.add("Signal group");
				rowData.add(n.getSignalCollection().getSignalGroupNumber(signalGroup));
				
				fieldNames.add("Signal name");
				rowData.add(n.getSignalCollection().getSignalGroupName(signalGroup));
				
				fieldNames.add("Source image");
				rowData.add(n.getSignalCollection().getSourceFile(signalGroup));
				
				fieldNames.add("Source channel");
				rowData.add(n.getSignalCollection().getSourceChannel(signalGroup));
				
				fieldNames.add("Number of signals");
				rowData.add(n.getSignalCollection().numberOfSignals(signalGroup));
				
				for(NuclearSignal s : n.getSignalCollection().getSignals(signalGroup)){
					
					for(SignalStatistic stat : SignalStatistic.values()){
						
						fieldNames.add(stat.label(MeasurementScale.PIXELS)  );

						double pixel = s.getStatistic(stat, MeasurementScale.PIXELS);
						
						if(stat.isDimensionless()){
							rowData.add(df.format(pixel) );
						} else {
							double micron = s.getStatistic(stat, MeasurementScale.MICRONS);
							rowData.add(df.format(pixel) +" ("+ df.format(micron)+ " "+ stat.units(MeasurementScale.MICRONS)+")");
						}
					}
					
					fieldNames.add("Signal CoM");
					rowData.add(s.getCentreOfMass().toString());
					
				}			
				
			}
			
			model.addColumn("", fieldNames.toArray(new Object[0])); 
			model.addColumn("Info", rowData.toArray(new Object[0]));

			}
		return model;	
	}

}
