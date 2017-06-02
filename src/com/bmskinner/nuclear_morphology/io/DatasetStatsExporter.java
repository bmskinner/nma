/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.io;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.bmskinner.nuclear_morphology.analysis.AbstractAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.analysis.profiles.Taggable;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.IJ;

/**
 * Export all the stats from a dataset to a text file for downstream analysis
 * 
 * @author ben
 * @since 1.13.4
 *
 */
public class DatasetStatsExporter extends AbstractAnalysisMethod implements Exporter, Loggable, IAnalysisMethod {

    private static final String EXPORT_MESSAGE          = "Exporting stats...";
    private File                exportFile;
    private static final String DEFAULT_MULTI_FILE_NAME = "Multiple_stats_export" + Exporter.TAB_FILE_EXTENSION;

    private boolean includeProfiles = true;

    private List<IAnalysisDataset> list = new ArrayList<>();

    /**
     * Create specifying the folder stats will be exported into
     * 
     * @param folder
     */
    public DatasetStatsExporter(File file, List<IAnalysisDataset> list) {
        super(null);
        this.list = list;
        if (file.isDirectory()) {
            file = new File(file, DEFAULT_MULTI_FILE_NAME);
        }
        exportFile = file;

        if (exportFile.exists()) {
            exportFile.delete();
        }

    }

    /**
     * Create specifying the folder stats will be exported into
     * 
     * @param folder
     */
    public DatasetStatsExporter(File file, IAnalysisDataset dataset) {
        super(null);
        list.add(dataset);

        if (file.isDirectory()) {
            file = new File(file, DEFAULT_MULTI_FILE_NAME);
        }
        exportFile = file;

        if (exportFile.exists()) {
            exportFile.delete();
        }

    }

    @Override
    public IAnalysisResult call() {

        export(list);
        return new DefaultAnalysisResult(list);
    }

    /**
     * Export stats from the dataset to a file
     * 
     * @param d
     */
    public void export(IAnalysisDataset d) {

        log(EXPORT_MESSAGE);

        StringBuilder outLine = new StringBuilder();
        writeHeader(outLine);
        try {
            export(d, outLine, exportFile);
        } catch (UnavailableBorderTagException | UnavailableProfileTypeException | ProfileException e) {
            error("Error exporting dataset", e);
        }
        IJ.append(outLine.toString(), exportFile.getAbsolutePath());
        log("Exported stats to " + exportFile.getAbsolutePath());
    }

    /**
     * Export stats from all datasets in the list to the same file
     * 
     * @param list
     */
    public void export(List<IAnalysisDataset> list) {

        log(EXPORT_MESSAGE);
        StringBuilder outLine = new StringBuilder();

        writeHeader(outLine);

        try {
            for (IAnalysisDataset d : list) {
                export(d, outLine, exportFile);
                fireProgressEvent();
            }
        } catch (UnavailableBorderTagException | UnavailableProfileTypeException | ProfileException e) {
            error("Error exporting dataset", e);
        }
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

        outLine.append("Dataset\tCellID\tComponent\tImage\tCentre_of_mass\t");

        for (PlottableStatistic s : PlottableStatistic.getNucleusStats()) {

            String label = s.label(MeasurementScale.PIXELS).replaceAll(" ", "_").replaceAll("\\(", "_")
                    .replaceAll("\\)", "").replaceAll("__", "_");
            outLine.append(label + "\t");

            if (!s.isDimensionless() && !s.isAngle()) { // only give micron
                                                        // measurements when
                                                        // length or area

                label = s.label(MeasurementScale.MICRONS).replaceAll(" ", "_").replaceAll("\\(", "_")
                        .replaceAll("\\)", "").replaceAll("__", "_");

                outLine.append(label + "\t");
            }

        }

        if (includeProfiles) {
            for (ProfileType type : ProfileType.exportValues()) {

                String label = type.toString().replaceAll(" ", "_");
                for (int i = 0; i < 100; i++) {

                    outLine.append(label + "_" + i + "\t");
                }

            }
        }
        outLine.append(NEWLINE);
    }

    /**
     * Test if the given component is present in the dataset
     * 
     * @param d
     * @param component
     * @return
     */
    private boolean hasComponent(IAnalysisDataset d, String component) {

        if (CellularComponent.CYTOPLASM.equals(component)) {
            return d.getCollection().getCells().stream().allMatch(c -> c.hasCytoplasm());
        }

        if (CellularComponent.NUCLEUS.equals(component)) {
            return d.getCollection().getCells().stream().allMatch(c -> c.hasNucleus());
        }

        return false;

    }

    /**
     * Write the dataset level info that will always be present *
     */
    private void writeDatasetHeader() {

    }

    public void export(IAnalysisDataset d, StringBuilder outLine, File exportFile)
            throws UnavailableBorderTagException, UnavailableProfileTypeException, ProfileException {
        // log("Exporting stats...");

        for (ICell cell : d.getCollection().getCells()) {

            // if(cell.hasCytoplasm()){
            //
            // ICytoplasm c = cell.getCytoplasm();
            // outLine.append(d.getName()+"\t")
            // .append(cell.getId()+"\t")
            // .append("Cytoplasm\t")
            // .append(c.getSourceFileName()+"\t");
            //
            // appendNucleusStats(outLine, d, cell, c);
            // outLine.append(NEWLINE);
            // }

            if (cell.hasNucleus()) {

                for (Nucleus n : cell.getNuclei()) {

                    outLine.append(d.getName() + "\t").append(cell.getId() + "\t")
                            .append("Nucleus_" + n.getNameAndNumber() + "\t").append(n.getSourceFileName() + "\t")
                            .append(n.getOriginalCentreOfMass().toString() + "\t");
                    appendNucleusStats(outLine, d, cell, n);

                    if (includeProfiles) {
                        appendProfiles(outLine, n);
                    }

                    outLine.append(NEWLINE);
                }

            }

        }
    }

    private void appendNucleusStats(StringBuilder outLine, IAnalysisDataset d, ICell cell, CellularComponent c) {

        for (PlottableStatistic s : PlottableStatistic.getNucleusStats()) {
            double varP = 0;
            double varM = 0;

            if (s.equals(PlottableStatistic.VARIABILITY)) {

                try {
                    varP = d.getCollection().getNormalisedDifferenceToMedian(Tag.REFERENCE_POINT, (Taggable) c);
                    varM = varP;
                } catch (UnavailableBorderTagException e) {
                    stack("Tag not present in component", e);
                    varP = -1;
                    varM = -1;
                }
            } else {
                varP = c.getStatistic(s, MeasurementScale.PIXELS);
                varM = c.getStatistic(s, MeasurementScale.MICRONS);
            }

            outLine.append(varP + "\t");
            if (!s.isDimensionless() && !s.isAngle()) {
                outLine.append(varM + "\t");
            }
        }
    }

    private void appendProfiles(StringBuilder outLine, Taggable c)
            throws UnavailableBorderTagException, UnavailableProfileTypeException, ProfileException {
        for (ProfileType type : ProfileType.exportValues()) {

            IProfile p = c.getProfile(type, Tag.REFERENCE_POINT);

            for (int i = 0; i < 100; i++) {
                double idx = ((double) i) / 100d;

                double value = p.get(idx);
                outLine.append(value + "\t");
            }

        }
    }

    // private File makeFile(String fileName){
    //// if(fileName==null){
    //// throw new IllegalArgumentException("Filename is null");
    //// }
    //// File f = new File(exportFolder, fileName+TAB_FILE_EXTENSION);
    // if(exportFile.exists()){
    // exportFile.delete();
    // }
    //// return f;
    // }

}
