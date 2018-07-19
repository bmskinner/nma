package com.bmskinner.nuclear_morphology.io;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.nuclear.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;

public class DatasetSignalsExporter extends StatsExporter {

    /**
     * Create specifying the folder stats will be exported into
     * 
     * @param folder
     */
    public DatasetSignalsExporter(@NonNull File file, @NonNull List<IAnalysisDataset> list) {
        super(file, list);
    }

    /**
     * Create specifying the folder stats will be exported into
     * 
     * @param folder
     */
    public DatasetSignalsExporter(@NonNull File file, @NonNull IAnalysisDataset dataset) {
        super(file, dataset);
    }

    /**
     * Append a column header line to the StringBuilder.
     * @param outLine
     */
    @Override
    protected void appendHeader(@NonNull StringBuilder outLine) {

        String[] headers = {
            "Dataset",
            "CellId",
            "ComponentId",
            "SignalGroup",
            "SignalFolder",
            "SignalImage",
            "SignalChannel" 
        };

        outLine.append(Stream.of(headers).collect(Collectors.joining(TAB))+TAB);
        
        for(PlottableStatistic s : PlottableStatistic.getSignalStats()) {
        	 String label = s.label(MeasurementScale.PIXELS)
        			 .replaceAll(" ", "_")
        			 .replaceAll("\\(", "_")
                     .replaceAll("\\)", "")
                     .replaceAll("__", "_");
             outLine.append(label + TAB);

             if (!s.isDimensionless() && !s.isAngle()) { 
                 label = s.label(MeasurementScale.MICRONS).replaceAll(" ", "_")
                		 .replaceAll("\\(", "_")
                         .replaceAll("\\)", "")
                         .replaceAll("__", "_");
                 outLine.append(label + TAB);
             }
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
    @Override
    protected void append(@NonNull IAnalysisDataset d, @NonNull StringBuilder outLine) {
        
        
        for(@NonNull UUID signalGroupId : d.getCollection().getSignalGroupIDs()){
            ISignalGroup signalGroup = d.getCollection().getSignalGroup(signalGroupId).get();
            String groupName   = signalGroup.getGroupName();
            String groupFolder = signalGroup.getFolder().getAbsolutePath();            
            
            for (ICell cell : d.getCollection().getCells()) {
                
                if (!cell.hasNucleus())
                    continue;

                for (Nucleus n : cell.getNuclei()) {

                    if(!n.getSignalCollection().hasSignal(signalGroupId))
                        continue;

                    for(INuclearSignal s : n.getSignalCollection().getSignals(signalGroupId)){

                        outLine.append(d.getName() + TAB)
                        .append(cell.getId() + TAB)
                        .append(n.getID() + TAB)
                        .append(groupName + TAB)
                        .append(groupFolder + TAB)
                        .append(s.getSourceFile().getName() + TAB)
                        .append(s.getChannel() + TAB);
                        
                        appendSignalStats(outLine, s);
                        
                        if (outLine.length() > 0)
                            outLine.setLength(outLine.length() - 1);
                        outLine.append(NEWLINE);
                    }
                }



            }
        }
    }
    
    private void appendSignalStats(@NonNull StringBuilder outLine, @NonNull CellularComponent c) {

        for (PlottableStatistic s : PlottableStatistic.getSignalStats()) {
            double varP = c.getStatistic(s, MeasurementScale.PIXELS);
            double varM = c.getStatistic(s, MeasurementScale.MICRONS);

            outLine.append(varP + TAB);
            if (!s.isDimensionless() && !s.isAngle()) {
                outLine.append(varM + TAB);
            }
        }
    }
}
