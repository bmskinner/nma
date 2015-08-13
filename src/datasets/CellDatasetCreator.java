package datasets;

import ij.IJ;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import cell.Cell;
import no.components.NuclearSignal;
import no.nuclei.Nucleus;

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
