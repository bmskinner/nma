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


package com.bmskinner.nuclear_morphology.gui.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.gui.MainWindow;

/**
 * Extends the VoidResultAction to include a dataset or list of datasets to be
 * processed in the action
 * 
 * @since 1.13.6
 *
 */
public abstract class SingleDatasetResultAction extends VoidResultAction {

    
    // Flags to pass to ProgressableActions to determine the analyses
    // to carry out in subsequently
    public static final int NO_FLAG          = 0;
    public static final int ADD_POPULATION   = 1;
    public static final int STATS_EXPORT     = 2;
    public static final int NUCLEUS_ANNOTATE = 4;
    public static final int CURVE_REFOLD     = 8;
    public static final int EXPORT_COMPOSITE = 16;
    public static final int SAVE_DATASET     = 32;
    public static final int ASSIGN_SEGMENTS  = 64;
 
    // the dataset being worked on
    protected IAnalysisDataset dataset     = null; 
    
    // list of datasets that need processing next
    private final List<IAnalysisDataset> processList = new ArrayList<>(0);

    /**
     * Construct with a dataset to analyse, a message to display, and the window
     * to send messages to
     * 
     * @param dataset
     *            the analysis dataset
     * @param barMessage
     *            the progress bar message
     * @param mw
     *            the main window for analysis
     */
    public SingleDatasetResultAction(@NonNull IAnalysisDataset dataset, @NonNull String barMessage, @NonNull MainWindow mw) {
        super(barMessage, mw);
        if (dataset == null) {
            warn("Unable to create action");
            throw new IllegalArgumentException("Must have dataset for progressable action");
        }
        this.dataset = dataset;
    }

    /**
     * Construct using a list of datasets to be processed. The first is
     * analysed, and the rest stored.
     * 
     * @param list
     * @param barMessage
     * @param mw
     */
    public SingleDatasetResultAction(@NonNull List<IAnalysisDataset> list, @NonNull String barMessage, @NonNull MainWindow mw) {
        this(list.get(0), barMessage, mw);
        processList.addAll(list);
        processList.remove(0); // remove the first entry
    }

    /**
     * Construct using a list of datasets to be processed. The first is
     * analysed, and the rest stored.
     * 
     * @param list
     * @param barMessage
     * @param mw
     * @param flag
     */
    public SingleDatasetResultAction(@NonNull List<IAnalysisDataset> list, @NonNull String barMessage, @NonNull MainWindow mw, int flag) {
        this(list, barMessage, mw);
        this.downFlag = flag;
    }

    /**
     * Constructor including a flag for downstream analyses to be carried out
     * 
     * @param dataset
     * @param barMessage
     * @param mw
     * @param flag
     */
    public SingleDatasetResultAction(@NonNull IAnalysisDataset dataset, @NonNull String barMessage, @NonNull MainWindow mw, int flag) {
        this(dataset, barMessage, mw);
        this.downFlag = flag;
    }

    protected synchronized List<IAnalysisDataset> getRemainingDatasetsToProcess() {
        return this.processList;
    }

    protected synchronized boolean hasRemainingDatasetsToProcess() {
        return processList.size() > 0;
    }
}
