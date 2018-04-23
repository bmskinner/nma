package com.bmskinner.nuclear_morphology.io;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.MultipleDatasetAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.analysis.signals.SignalManager;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.nuclear.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult.Aggregation;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult.CountType;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
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
    private void export(@NonNull IAnalysisDataset d) {
        log(EXPORT_MESSAGE);
;
        StringBuilder outLine = new StringBuilder();
        writeHeader(outLine);
        append(d, outLine);
        IJ.append(outLine.toString(), exportFile.getAbsolutePath());
        log("Exported stats to " + exportFile.getAbsolutePath());
    }

    /**
     * Export stats from all datasets in the list to the same file
     * 
     * @param list
     */
    private void export(@NonNull List<IAnalysisDataset> list) {
        log(EXPORT_MESSAGE);
        
        StringBuilder outLine = new StringBuilder();
        writeHeader(outLine);

        for (IAnalysisDataset d : list) {
            append(d, outLine);
            fireProgressEvent();
        }

        IJ.append(outLine.toString(), exportFile.getAbsolutePath());
        log("Exported stats to " + exportFile.getAbsolutePath());
    }

    /**
     * Append a column header line to the StringBuilder.
     * @param outLine
     */
    private void writeHeader(StringBuilder outLine) {

        String[] headers = {
            "Dataset",
            "CellID",
            "Component",
            "Folder",
            "ComponentImage",
            "SignalGroup",
            "SignalFolder",
            "SignalImage",
            "SignalChannel",
            "Aggregation"
        };

        outLine.append(Stream.of(headers).collect(Collectors.joining(TAB))+TAB);
        
        for(int i=0; i<getMaximumNumberOfShells(); i++){
            String label = "Signal_shell_"+i;
            outLine.append(label + TAB); 
        }
        
        for(int i=0; i<getMaximumNumberOfShells(); i++){
            String label = "Counterstain_shell_"+i;
            outLine.append(label + TAB); 
        }
        
        // remove the final tab character
        if (outLine.length() > 0)
            outLine.setLength(outLine.length() - 1);
        
        outLine.append(NEWLINE);
    }
    
    /**
     * Append the given dataset stats into the string builder
     * @param d the dataset to export
     * @param outLine the string builder to append to
     * @throws UnloadableImageException 
     * @throws UnavailableBorderTagException
     * @throws UnavailableProfileTypeException
     * @throws ProfileException
     */
    private void append(@NonNull IAnalysisDataset d, @NonNull StringBuilder outLine) {
        
        
        for(UUID signalGroupId : d.getCollection().getSignalGroupIDs()){
            ISignalGroup signalGroup = d.getCollection().getSignalGroup(signalGroupId).get();
            String groupName   = signalGroup.getGroupName();
            String groupFolder = signalGroup.getFolder().getAbsolutePath();
            int groupChannel   = signalGroup.getChannel();
            
            Optional<IShellResult> oShellResult = signalGroup.getShellResult();
            if(!oShellResult.isPresent())
                continue;
            
            IShellResult shellResult = oShellResult.get();

            for (ICell cell : d.getCollection().getCells()) {
                
                if (!cell.hasNucleus())
                    continue;

                for (Nucleus n : cell.getNuclei()) {

                    if(!n.getSignalCollection().hasSignal(signalGroupId))
                        continue;

                    outLine.append(d.getName() + TAB)
                    .append(cell.getId() + TAB)
                    .append(CellularComponent.NUCLEUS+"_" + n.getNameAndNumber() + TAB)
                    .append(n.getSourceFolder() + TAB)
                    .append(n.getSourceFileName() + TAB)
                    .append(groupName + TAB)
                    .append(groupFolder + TAB)
                    .append(n.getSignalCollection().getSourceFile(signalGroupId).getName() + TAB)
                    .append(n.getSignalCollection().getSourceChannel(signalGroupId)+TAB)
                    .append(Aggregation.BY_NUCLEUS + TAB);

                    long[] signalByNucleus = shellResult.getPixelValues(CountType.SIGNAL, cell, n, null);
                    long[] counterstain    = shellResult.getPixelValues(CountType.COUNTERSTAIN, cell, n, null);

                    for(int i=0; i<shellResult.getNumberOfShells(); i++){
                        outLine.append(signalByNucleus[i]+TAB);
                    }
                    for(int i=0; i<shellResult.getNumberOfShells(); i++){
                        outLine.append(counterstain[i]+TAB);
                    }

                    if (outLine.length() > 0)
                        outLine.setLength(outLine.length() - 1);
                    outLine.append(NEWLINE);

                    for(INuclearSignal s : n.getSignalCollection().getSignals(signalGroupId)){

                        outLine.append(d.getName() + TAB)
                        .append(cell.getId() + TAB)
                        .append(CellularComponent.NUCLEUS+"_" + n.getNameAndNumber() + TAB)
                        .append(n.getSourceFolder() + TAB)
                        .append(n.getSourceFileName() + TAB)
                        .append(groupName + TAB)
                        .append(groupFolder + TAB)
                        .append(s.getSourceFile().getName() + TAB)
                        .append(s.getChannel() + TAB)
                        .append(Aggregation.BY_SIGNAL + TAB);

                        long[] signalBySignal = shellResult.getPixelValues(CountType.SIGNAL, cell, n, s);

                        for(int i=0; i<shellResult.getNumberOfShells(); i++){
                            outLine.append(signalBySignal[i]+TAB);
                        }
                        for(int i=0; i<shellResult.getNumberOfShells(); i++){
                            outLine.append(counterstain[i]+TAB);
                        }

                        if (outLine.length() > 0)
                            outLine.setLength(outLine.length() - 1);
                        outLine.append(NEWLINE);
                    }
                }



            }
        }
    }
    
    private void appendSignalStats(@NonNull StringBuilder outLine, @NonNull IAnalysisDataset d, @NonNull Nucleus n, @NonNull INuclearSignal s){
        
    }
    
    /**
     * Find the maximum number of shells within the datasets
     * @return
     */
    private int getMaximumNumberOfShells(){
        int shells = 0;
        for(IAnalysisDataset d : datasets){
            SignalManager sm = d.getCollection().getSignalManager();
            int sc = sm.getShellCount();
            shells = sc>shells?sc:shells;
        }
        return shells;
    }

}
