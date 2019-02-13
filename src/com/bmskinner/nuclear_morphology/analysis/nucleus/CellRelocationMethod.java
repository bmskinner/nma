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
package com.bmskinner.nuclear_morphology.analysis.nucleus;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.ChildAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.VirtualCellCollection;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;

/**
 * Find cells from a .cell file and assign them to child datasets.
 * 
 * @author ben
 * @since 1.13.4
 *
 */
public class CellRelocationMethod extends SingleDatasetAnalysisMethod {

    private static final String TAB          = "\\t";
    private static final String UUID_KEY     = "UUID";
    private static final String NAME_KEY     = "Name";
    private static final String CHILD_OF_KEY = "ChildOf";

    private File inputFile = null;

    /**
     * Construct with a dataset and a file containing cell locations
     * 
     * @param dataset
     * @param file
     */
    public CellRelocationMethod(final IAnalysisDataset dataset, final File file) {
        super(dataset);
        this.inputFile = file;
    }

    @Override
    public IAnalysisResult call() throws CellRelocationException {
        run();
        IAnalysisResult r = new DefaultAnalysisResult(dataset);
        return r;
    }

    private void run() {

        try {
            findCells();
        } catch (Exception e) {
            warn("Error selecting cells");
            stack("Error selecting cells", e);
        }
    }

    private void findCells() {
        Set<UUID> newDatasets;
        try {
            newDatasets = parsePathList();
        } catch (CellRelocationException | ProfileException e) {
            stack("Error relocating cells", e);
            return;
        }

        if (newDatasets.size() > 0) {

            try {

                for (UUID id : newDatasets) {

                    if (!id.equals(dataset.getId())) {
                        dataset.getCollection().getProfileManager()
                                .copyCollectionOffsets(dataset.getChildDataset(id).getCollection());

                    }
                }
            } catch (ProfileException e) {
                warn("Unable to profile new collections");
                stack("Unable to profile new collections", e);
                return;
            }
        }

    }

    private Set<UUID> parsePathList() throws CellRelocationException, ProfileException {
        Scanner scanner;
        try {
            scanner = new Scanner(inputFile);
        } catch (FileNotFoundException e) {
            throw new CellRelocationException("Input file does not exist", e);
        }

        UUID activeID = null;
        String activeName = null;

        Map<UUID, IAnalysisDataset> map = new HashMap<UUID, IAnalysisDataset>();

        while (scanner.hasNextLine()) {

            /*
             * File format: UUID 57320dbb-bcde-49e3-ba31-5b76329356fe Name
             * Testing ChildOf 57320dbb-bcde-49e3-ba31-5b76329356fe
             * J:\Protocols\Scripts and macros\Testing\s75.tiff
             * 602.0585522824504-386.38060239306236
             */

            String line = scanner.nextLine();
            if (line.startsWith(UUID_KEY)) {

                /*
                 * New dataset found
                 */
                activeID = UUID.fromString(line.split(TAB)[1]);

                if (dataset.getId().equals(activeID) || dataset.hasChild(activeID)) {
                    // the dataset already exists with this id - we must fail
                    scanner.close();
                    warn("Dataset in cell file already exists");
                    warn("Cancelling relocation");
                    throw new CellRelocationException("Dataset already exists");
                }

                continue;
            }

            if (line.startsWith(NAME_KEY)) {
                /*
                 * Name of new dataset
                 */

                activeName = line.split(TAB)[1];

                if(activeID==null)
                	continue;
                ICellCollection c = new VirtualCellCollection(dataset, activeName, activeID);

                IAnalysisDataset d = new ChildAnalysisDataset(dataset, c);


                Optional<IAnalysisOptions> op = dataset.getAnalysisOptions();
                if(op.isPresent())
                	d.setAnalysisOptions(op.get());

                map.put(activeID, d);
                continue;
            }

            if (line.startsWith(CHILD_OF_KEY)) {
                /*
                 * Parent dataset
                 */
                UUID parentID = UUID.fromString(line.split(TAB)[1]);

                if (parentID.equals(activeID)) {
                    dataset.addChildDataset(map.get(activeID));
                } else {
                    map.get(parentID).addChildDataset(map.get(activeID));
                }
                continue;
            }

            /*
             * No header line, must be a cell for the current dataset
             */

            ICell cell = getCellFromLine(line);
            if (cell != null) {
                map.get(activeID).getCollection().addCell(cell);
            }
        }
        fine("All cells found");

        // Make the profile collections for the new datasets

        for (IAnalysisDataset d : map.values()) {
            d.getCollection().createProfileCollection();
        }

        scanner.close();
        return map.keySet();
    }

    private ICell getCellFromLine(String line) {
        fine("Processing line: " + line);

        if (line.length() < 5) {
            // not enough room for a path and number, skip
            return null;
        }

        // Line format is FilePath\tPosition as x-y

        // get file name

        File file = getFile(line);
        if (!file.isFile() || !file.exists()) {

            // Get the image name and substitute the parent dataset path.
            File newFolder = dataset.getCollection().getFolder();
            if (newFolder.exists()) {
                fine("Updating folder to " + newFolder.getAbsolutePath());
                file = new File(newFolder, file.getName());
                fine("Updating path to " + file);
            } else {
                fine("File does not exist or is malformed: " + file.toString());
                return null;
            }

            //
        }

        // get position
        IPoint com;

        try {
            com = getPosition(line);
        } catch (Exception e) {
            warn(line);
            warn(file.getAbsolutePath());
            stack("Cannot get position", e);
            return null;
        }

        return copyCellFromRoot(file, com);

    }

    /**
     * Make a new cell based on the cell in the root dataset with the given
     * location in an image file
     * 
     * @param f
     * @param com
     * @return
     */
    private ICell copyCellFromRoot(File f, IPoint com) {
        // find the nucleus
        Set<ICell> cells = this.dataset.getCollection().getCells(f);

        for (ICell c : cells) {

            for (Nucleus n : c.getNuclei()) {
                if (n.containsOriginalPoint(com)) {

                    return c;
                }
            }

        }
        return null;
    }

    private File getFile(String line) {
        String[] array = line.split(TAB);
        File f = new File(array[0]);
        return f;
    }

    private IPoint getPosition(String line) throws Exception {
        String[] array = line.split(TAB);
        String position = array[1];

        String[] posArray = position.split("-");

        double x = Double.parseDouble(posArray[0]);
        double y = Double.parseDouble(posArray[1]);
        return IPoint.makeNew(x, y);
    }

    public class CellRelocationException extends Exception {
        private static final long serialVersionUID = 1L;

        public CellRelocationException() {
            super();
        }

        public CellRelocationException(String message) {
            super(message);
        }

        public CellRelocationException(String message, Throwable cause) {
            super(message, cause);
        }

        public CellRelocationException(Throwable cause) {
            super(cause);
        }
    }

}
