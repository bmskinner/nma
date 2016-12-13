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

package com.bmskinner.nuclear_morphology.charting.datasets;

import java.awt.Color;
import java.awt.Paint;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import com.bmskinner.nuclear_morphology.analysis.signals.ShellRandomDistributionCreator;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ProfileableCellularComponent.IndexOutOfBoundsException;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.nuclear.BorderPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.NuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclear.UnavailableSignalGroupException;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.stats.NucleusStatistic;
import com.bmskinner.nuclear_morphology.components.stats.SignalStatistic;
import com.bmskinner.nuclear_morphology.gui.GlobalOptions;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;

/**
 * Generate the stats tables for a single cell
 * @author bms41
 *
 */
public class CellTableDatasetCreator extends AbstractCellDatasetCreator {
	
	public CellTableDatasetCreator(ICell c){
		super(c);
	}
	
	
	/**
	 * Create a table of stats for the given cell.
	 * @param cell the cell
	 * @return a table model
	 * @throws ChartDatasetCreationException 
	 * @throws Exception 
	 */
	public TableModel createCellInfoTable(TableOptions options){
		
		if( ! options.hasDatasets()){
			return createBlankTable();
		}
		
		if(options.isMultipleDatasets()){
			return createBlankTable();
		}
		
		IAnalysisDataset d = options.firstDataset();
		DefaultTableModel model = new DefaultTableModel();
		
		List<Object> fieldNames = new ArrayList<Object>(0);
		List<Object> rowData 	= new ArrayList<Object>(0);
						
		// find the collection with the most channels
		// this defines  the number of rows
			
		Nucleus n = cell.getNucleus();

		fieldNames.add("Source image file");
		rowData.add(n.getPathAndNumber());

		fieldNames.add("Source image name");
		rowData.add(n.getSourceFileName());

		fieldNames.add("Source channel");
		rowData.add(n.getChannel());
		
		fieldNames.add("Angle window prop.");
		rowData.add(n.getWindowProportion(ProfileType.ANGLE));
		
		fieldNames.add("Angle window size");
		rowData.add(n.getWindowSize(ProfileType.ANGLE));

		fieldNames.add("Scale (pixels/um)");
		rowData.add(n.getScale());

		addNuclearStatisticsToTable(fieldNames, rowData, n);
		
		fieldNames.add("Original bounding width");
		rowData.add(n.getBounds().getWidth());
		
		fieldNames.add("Original bounding height");
		rowData.add(n.getBounds().getHeight());

		fieldNames.add("Nucleus CoM");
		rowData.add(n.getCentreOfMass().toString());
		
		fieldNames.add("Original CoM");
		rowData.add(n.getOriginalCentreOfMass().toString());

		fieldNames.add("Original nucleus position");
		rowData.add("x: "+n.getPosition()[0]+" : y: "+n.getPosition()[1]);

		fieldNames.add("Current nucleus position");
		rowData.add("x: "+n.getMinX()+" : y: "+n.getMinY());

		NucleusType type = NucleusType.getNucleusType(n);

		if(type!=null){
						
			for(Tag tag : n.getBorderTags().keySet()){
				fieldNames.add(tag);
				if(n.hasBorderTag(tag)){

					IBorderPoint p;
					try {
						p = n.getBorderPoint(tag);
					} catch (UnavailableBorderTagException e) {
						fine("Tag not present: "+tag);
						return createBlankTable();
					}
					
					int index = n.getOffsetBorderIndex(Tag.REFERENCE_POINT, n.getBorderIndex(tag));
					
					rowData.add(p.toString()+" at profile index "+index);
				} else {
					rowData.add("N/A");
				}
			}
		} 
		
		
//		for(int i=0; i<n.getBorderLength(); i++){
//			fieldNames.add("Border index "+i);
//			rowData.add("x: "+n.getBorderPoint(i).getX()+" : y: "+n.getBorderPoint(i).getY());
//		}
		
		addNuclearSignalsToTable(fieldNames, rowData, n, d);

		model.addColumn("", fieldNames.toArray(new Object[0])); 
		model.addColumn("Info", rowData.toArray(new Object[0]));

			
		return model;	
	}
	
	/**
	 * Add the nuclear statistic information to a cell table
	 * @param fieldNames
	 * @param rowData
	 * @param n
	 */
	private void addNuclearStatisticsToTable(List<Object> fieldNames,  List<Object> rowData, Nucleus n){
		
//		DecimalFormat df = new DecimalFormat("#0.00"); 
		
		for(NucleusStatistic stat : NucleusStatistic.values()){

			if( ! stat.equals(NucleusStatistic.VARIABILITY)){

				fieldNames.add(stat.label(GlobalOptions.getInstance().getScale()  )  );

				double value = n.getStatistic(stat, GlobalOptions.getInstance().getScale()  );
					rowData.add(DEFAULT_DECIMAL_FORMAT.format(value) );
			}

		}
		
	}
	
	/**
	 * Add the nuclear signal information to a cell table
	 * @param fieldNames
	 * @param rowData
	 * @param n the nucleus
	 * @param d the source dataset for the nucleus
	 */
	private void addNuclearSignalsToTable(List<Object> fieldNames,  List<Object> rowData, Nucleus n, IAnalysisDataset d){
		
		int j=0;

		for(UUID signalGroup : d.getCollection().getSignalGroupIDs()){
			
			if(signalGroup.equals(ShellRandomDistributionCreator.RANDOM_SIGNAL_ID)){
				continue;
			}
			
			try {

				ISignalGroup g = d.getCollection().getSignalGroup(signalGroup);

				fieldNames.add("");
				rowData.add("");
				Color colour = g.hasColour()
						? g.getGroupColour()
						: ColourSelecter.getColor(j);
						
				SignalTableCell tableCell = new SignalTableCell(signalGroup, g.getGroupName(), colour);


				fieldNames.add("Signal group");
				rowData.add(tableCell);		

				fieldNames.add("Source image");
				rowData.add(n.getSignalCollection().getSourceFile(signalGroup));

				fieldNames.add("Source channel");
				rowData.add(g.getChannel());

				fieldNames.add("Number of signals");
				rowData.add(n.getSignalCollection().numberOfSignals(signalGroup));

				for(INuclearSignal s : n.getSignalCollection().getSignals(signalGroup)){
					addSignalStatisticsToTable(fieldNames, rowData, s );
				}	
			} catch (UnavailableSignalGroupException e){
				fine("Signal group "+signalGroup+" is not present in collection", e);
			} finally {
				j++;
			}

		}

	}
	
	/**
	 * Add the nuclear signal statistics to a cell table
	 * @param fieldNames
	 * @param rowData
	 * @param s
	 */
	private void addSignalStatisticsToTable(List<Object> fieldNames,  List<Object> rowData, INuclearSignal s){
		
//		DecimalFormat df = new DecimalFormat("#0.00"); 
		
		for(SignalStatistic stat : SignalStatistic.values()){

			fieldNames.add(    stat.label(   GlobalOptions.getInstance().getScale() )  );

			double value = s.getStatistic(stat, GlobalOptions.getInstance().getScale() );

			rowData.add(DEFAULT_DECIMAL_FORMAT.format(value) );
		}

		fieldNames.add("Signal CoM");
		rowData.add(s.getCentreOfMass().toString());
		
		fieldNames.add("Original CoM");
		rowData.add(s.getOriginalCentreOfMass().toString());
		
		fieldNames.add("First border point");
		rowData.add(s.getBorderPoint(0).toString());
		
	}

}
