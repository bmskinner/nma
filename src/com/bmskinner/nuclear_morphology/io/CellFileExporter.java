/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nuclear_morphology.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.MultipleDatasetAnalysisMethod;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.io.Io.Exporter;
import com.bmskinner.nuclear_morphology.io.Io.Importer;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Export the locations of the centre of mass of nuclei in a dataset to a file
 * 
 * @author ben
 *
 */
public class CellFileExporter extends MultipleDatasetAnalysisMethod implements Exporter, Loggable {
	
	/**
     * Create specifying the folder cell files will be exported into
     * 
     * @param folder
     */
    public CellFileExporter(@NonNull List<IAnalysisDataset> list) {
        super(list);
    }

    /**
     * Create specifying the folder cell files will be exported into
     * 
     * @param folder
     */
    public CellFileExporter(@NonNull IAnalysisDataset dataset) {
        super(dataset);
    }

    @Override
    public IAnalysisResult call() throws Exception{
    	for(IAnalysisDataset d :  datasets) {
    		exportCellLocations(d);
    	}
        return new DefaultAnalysisResult(datasets);
    }

    private boolean exportCellLocations(IAnalysisDataset d) {

        String fileName = d.getName() + "." + Importer.LOC_FILE_EXTENSION;
        File exportFile = new File(d.getCollection().getOutputFolder(), fileName);

        if (!exportFile.getParentFile().isDirectory()) {
            // the desired output folder does not exist
            warn("The intended export folder does not exist");

            File folder = GlobalOptions.getInstance().getDefaultDir();
            // warn("Defaulting to: "+folder.getAbsolutePath());
            exportFile = new File(folder, fileName);
        }

        log("Exporting cells to " + exportFile.getAbsolutePath());

        if (exportFile.exists()) {
            exportFile.delete();
        }

        StringBuilder builder = new StringBuilder();

        /*
         * Add the cells from the root dataset
         */
        builder.append(makeDatasetHeaderString(d, d.getId()));
        builder.append(makeDatasetCellsString(d));

        /*
         * Add cells from all child datasets
         */
        builder.append(makeChildString(d));

        try {
            export(builder.toString(), exportFile);
        } catch (FileNotFoundException e) {
            stack(e);
            return false;
        }
        return true;
    }

    /**
     * Add the cells from all child datasets recursively
     * 
     * @param d
     * @return
     */
    private static String makeChildString(IAnalysisDataset d) {
        StringBuilder builder = new StringBuilder();

        for (IAnalysisDataset child : d.getChildDatasets()) {
            builder.append(makeDatasetHeaderString(child, d.getId()));
            builder.append(makeDatasetCellsString(child));

            if (child.hasChildren()) {
                builder.append(makeChildString(child));
            }
        }

        return builder.toString();
    }

    /**
     * Add the cell positions and image names
     * 
     * @param d
     * @return
     */
    private static String makeDatasetCellsString(IAnalysisDataset d) {
        StringBuilder builder = new StringBuilder();

        for (ICell c : d.getCollection().getCells()) {

            // IJ.log("Cell "+c.getNucleus().getNameAndNumber());

            int[] originalPosition = c.getNucleus().getPosition();

            IPoint com = c.getNucleus().getCentreOfMass();

            double x = com.getX() + originalPosition[CellularComponent.X_BASE];
            double y = com.getY() + originalPosition[CellularComponent.Y_BASE];

            // IJ.log(" Found position: "+x+"-"+y);

            try {

                if (c.getNucleus().getSourceFile() != null) {
                    builder.append(c.getNucleus().getSourceFile().getAbsolutePath());
                    builder.append("\t");
                    builder.append(x);
                    builder.append("-");
                    builder.append(y);
                    builder.append(NEWLINE);
                }
            } catch (Exception e) {
                return null;
            }

        }
        return builder.toString();
    }

    /**
     * Add the dataset id, name, and parent
     * 
     * @param child
     * @param parent
     * @return
     */
    private static String makeDatasetHeaderString(IAnalysisDataset child, UUID parent) {
        StringBuilder builder = new StringBuilder();

        builder.append("UUID\t");
        builder.append(child.getId().toString());
        builder.append(NEWLINE);

        builder.append("Name\t");
        builder.append(child.getName());
        builder.append(NEWLINE);

        builder.append("ChildOf\t");
        builder.append(parent.toString());
        builder.append(NEWLINE);
        return builder.toString();
    }

    private static void export(String s, File f) throws FileNotFoundException {

        PrintWriter out;
        out = new PrintWriter(f);
        out.print(s);
        out.close();

    }

}
