package com.bmskinner.nuclear_morphology.io;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.Taggable;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;

/**
 * Export the outlines of cellular components
 * @author Ben Skinner
 * @since 1.18.0
 *
 */
public class DatasetOutlinesExporter extends StatsExporter {
	
	private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	
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
     * @throws UnavailableBorderTagException
     * @throws UnavailableProfileTypeException
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
    	for(IBorderPoint p : c.getBorderList()) {
    		outLine.append(p.getX()+PIPE+p.getY()+COMMA);
    	}
    	// Remove final separator and add newline
        if (outLine.length() > 0)
            outLine.setLength(outLine.length() - 1);

        outLine.append(NEWLINE);
    }

}
