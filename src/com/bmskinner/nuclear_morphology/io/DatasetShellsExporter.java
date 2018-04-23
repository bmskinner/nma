package com.bmskinner.nuclear_morphology.io;

import java.io.File;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.MultipleDatasetAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.IJ;

/**
 * Export shell analysis results to a file
 * @author ben
 * @since 1.13.8
 *
 */
public class DatasetShellsExporter extends MultipleDatasetAnalysisMethod implements Exporter, Loggable {

    private static final String EXPORT_MESSAGE          = "Exporting shells...";
    private static final String DEFAULT_MULTI_FILE_NAME = "Shell_stats_export" + Exporter.TAB_FILE_EXTENSION;
    private File exportFile;

    /**
     * Create specifying the folder stats will be exported into
     * 
     * @param folder
     */
    public DatasetShellsExporter(@NonNull File file, @NonNull List<IAnalysisDataset> list) {
        super(list);
        setupExportFile(file);
    }

    /**
     * Create specifying the folder stats will be exported into
     * 
     * @param folder
     */
    public DatasetShellsExporter(@NonNull File file, @NonNull IAnalysisDataset dataset) {
        super(dataset);
        setupExportFile(file);
    }

    @Override
    public IAnalysisResult call() {
        export(datasets);
        return new DefaultAnalysisResult(datasets);
    }
    
    private void setupExportFile(@NonNull File file) {
    	if (file.isDirectory())
            file = new File(file, DEFAULT_MULTI_FILE_NAME);

        exportFile = file;

        if (exportFile.exists())
            exportFile.delete();
    }
    
    /**
     * Export stats from the dataset to a file
     * 
     * @param d
     */
    public void export(@NonNull IAnalysisDataset d) {
        log(EXPORT_MESSAGE);
;
        StringBuilder outLine = new StringBuilder();
        writeHeader(outLine);
//        try {
//            append(d, outLine);
//        } catch (UnavailableBorderTagException | UnavailableProfileTypeException | ProfileException e) {
//            error("Error exporting dataset", e);
//        }
        IJ.append(outLine.toString(), exportFile.getAbsolutePath());
        log("Exported stats to " + exportFile.getAbsolutePath());
    }

    /**
     * Export stats from all datasets in the list to the same file
     * 
     * @param list
     */
    public void export(@NonNull List<IAnalysisDataset> list) {
        log(EXPORT_MESSAGE);
        
        StringBuilder outLine = new StringBuilder();
        writeHeader(outLine);

//        try {
//            for (IAnalysisDataset d : list) {
//                append(d, outLine);
//                fireProgressEvent();
//            }
//        } catch (UnavailableBorderTagException | UnavailableProfileTypeException | ProfileException e) {
//            error("Error exporting dataset", e);
//        }
        IJ.append(outLine.toString(), exportFile.getAbsolutePath());
        log("Exported stats to " + exportFile.getAbsolutePath());
    }

    /**
     * Write a column header line to the StringBuilder. Only nuclear stats for
     * now
     * 
     * @param outLine
     */
    private void writeHeader(StringBuilder outLine) {

        outLine.append("Dataset\tCellID\tComponent\tFolder\tImage\tCentre_of_mass\t");
        
        

//        for (PlottableStatistic s : PlottableStatistic.getNucleusStats()) {
//
//            String label = s.label(MeasurementScale.PIXELS).replaceAll(" ", "_").replaceAll("\\(", "_")
//                    .replaceAll("\\)", "").replaceAll("__", "_");
//            outLine.append(label + TAB);
//
//            if (!s.isDimensionless() && !s.isAngle()) { // only give micron
//                                                        // measurements when
//                                                        // length or area
//
//                label = s.label(MeasurementScale.MICRONS).replaceAll(" ", "_").replaceAll("\\(", "_")
//                        .replaceAll("\\)", "").replaceAll("__", "_");
//
//                outLine.append(label + TAB);
//            }
//
//        }
        
        // remove the final tab character
        if (outLine.length() > 0)
            outLine.setLength(outLine.length() - 1);
        
        outLine.append(NEWLINE);
    }

}
