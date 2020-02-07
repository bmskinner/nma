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
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;

/**
 * Class to export only profiles from nuclei
 * @author Ben Skinner
 * @since 1.17.2
 *
 */
public class DatasetProfileExporter extends StatsExporter {
	
	private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	
	 /**
     * Create specifying the folder profiles will be exported into
     * 
     * @param folder
     */
    public DatasetProfileExporter(@NonNull File file, @NonNull List<IAnalysisDataset> list) {
        super(file, list);
    }

    /**
     * Create specifying the folder profiles will be exported into
     * 
     * @param folder
     */
    public DatasetProfileExporter(@NonNull File file, @NonNull IAnalysisDataset dataset) {
        super(file, dataset);
    }
    

	@Override
	protected void appendHeader(@NonNull StringBuilder outLine) {
		// Do nothing, we don't have consistent column headers
		outLine.append("Dataset\tCellID\tComponent\tFolder\tImage\tProfile");
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

                    appendProfiles(outLine, n);
                    
                    // Remove final separator 
                    if (outLine.length() > 0)
                        outLine.setLength(outLine.length() - 1);

                    outLine.append(NEWLINE);
                }
            }
        }
    }
    
    private void appendProfiles(StringBuilder outLine, Taggable c){
    	
    	try {
    		IProfile p = c.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);

    		for(int i : p) {
    			double value = p.get(i);
    			outLine.append(value +COMMA);
    		}
    		
    	} catch (UnavailableBorderTagException | UnavailableProfileTypeException | ProfileException e) {
    		LOGGER.severe("Unable to get profile for component "+c.getID());
    	}
    }
}
