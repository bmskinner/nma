package datasets;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import utility.Constants;
import no.analysis.AnalysisDataset;
import no.collections.CellCollection;
import no.components.NucleusBorderSegment;
import no.nuclei.Nucleus;
import no.nuclei.RoundNucleus;
import no.nuclei.sperm.PigSpermNucleus;
import no.nuclei.sperm.RodentSpermNucleus;

public class NucleusTableDatasetCreator {
	
	/**
	 * Create a table of signal stats for the given list of datasets. This table
	 * covers size, number of signals
	 * @param list the AnalysisDatasets to include
	 * @return a table model
	 */
	public static TableModel createSegmentStatsTable(Nucleus nucleus){

		DefaultTableModel model = new DefaultTableModel();

		List<Object> fieldNames = new ArrayList<Object>(0);
		
		if(nucleus==null){
			model.addColumn("No data loaded");

		} else {
			// check which reference point to use
			String referencePoint = null;
			if(nucleus.getClass()==RodentSpermNucleus.class){
				referencePoint = Constants.Nucleus.RODENT_SPERM.referencePoint();
			}

			if(nucleus.getClass()==PigSpermNucleus.class){
				referencePoint = Constants.Nucleus.PIG_SPERM.referencePoint();
			}

			if(nucleus.getClass()==RoundNucleus.class){
				referencePoint = Constants.Nucleus.ROUND.referencePoint();
			}


			// get the offset segments
			List<NucleusBorderSegment> segments = nucleus.getSegments(referencePoint);

			// create the row names
			fieldNames.add("Colour");
			fieldNames.add("Length");
			fieldNames.add("Start index");
			fieldNames.add("End index");

			model.addColumn("", fieldNames.toArray(new Object[0]));

			for(NucleusBorderSegment segment : segments) {


				List<Object> rowData = new ArrayList<Object>(0);
				
				rowData.add("");
				rowData.add(segment.length(nucleus.getLength()));
				rowData.add(segment.getStartIndex());
				rowData.add(segment.getEndIndex());

				model.addColumn(segment.getSegmentType(), rowData.toArray(new Object[0])); // separate column per segment
			}

			// format the numbers and make into a tablemodel
//			DecimalFormat df = new DecimalFormat("#0.00"); 

		}
		return model;	
	}

}
