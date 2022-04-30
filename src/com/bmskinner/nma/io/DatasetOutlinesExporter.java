package com.bmskinner.nma.io;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.MissingLandmarkException;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.profiles.MissingProfileException;
import com.bmskinner.nma.components.profiles.ProfileException;

/**
 * Export the outlines of cellular components
 * @author Ben Skinner
 * @since 1.18.0
 *
 */
public class DatasetOutlinesExporter extends StatsExporter {
	
	private static final Logger LOGGER = Logger.getLogger(DatasetOutlinesExporter.class.getName());
	
	 /**
     * Create specifying the folder profiles will be exported into
     * 
     * @param folder
     */
    public DatasetOutlinesExporter(@NonNull File file, @NonNull List<IAnalysisDataset> list) {
        super(file, list);
    }

    /**
     * Create specifying the folder profiles will be exported into
     * 
     * @param folder
     */
    public DatasetOutlinesExporter(@NonNull File file, @NonNull IAnalysisDataset dataset) {
        super(file, dataset);
    }
    

	@Override
	protected void appendHeader(@NonNull StringBuilder outLine) {
		outLine.append("Dataset\tCellID\tComponent\tFolder\tImage\tCoordinates");
		outLine.append(NEWLINE);
	}

    /**
     * Append the given dataset stats into the string builder
     * @param d the dataset to export
     * @param outLine the string builder to append to
     * @throws MissingLandmarkException
     * @throws MissingProfileException
     * @throws ProfileException
     */
    @Override
	protected void append(@NonNull IAnalysisDataset d, @NonNull StringBuilder outLine) throws Exception {
    	
    	for (ICell cell : d.getCollection().getCells()) {

            if (cell.hasNucleus()) {

                for (Nucleus n : cell.getNuclei()) {

                    outLine.append(d.getName() + TAB)
                        .append(cell.getId() + TAB)
                        .append(CellularComponent.NUCLEUS+"_" + n.getNameAndNumber() + TAB)
                        .append(n.getSourceFolder() + TAB)
                        .append(n.getSourceFileName() + TAB);

                    appendOutlines(outLine, n);
                }
            }

            if(cell.hasCytoplasm()) {
            	outLine.append(d.getName() + TAB)
	            	.append(cell.getId() + TAB)
	            	.append(CellularComponent.CYTOPLASM+"_" + cell.getCytoplasm().getID() + TAB)
	            	.append(cell.getCytoplasm().getSourceFolder() + TAB)
	            	.append(cell.getCytoplasm().getSourceFileName() + TAB);

            	appendOutlines(outLine, cell.getCytoplasm());
            }
    	}
    }
    
    private void appendOutlines(StringBuilder outLine, CellularComponent c){
    	for(IPoint p : c.getBorderList()) {
    		outLine.append(p.getX()+PIPE+p.getY()+COMMA);
    	}
    	// Remove final separator and add newline
        if (outLine.length() > 0)
            outLine.setLength(outLine.length() - 1);

        outLine.append(NEWLINE);
    }

}
