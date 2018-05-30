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

import java.util.concurrent.CountDownLatch;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.nucleus.ConsensusAveragingMethod;
import com.bmskinner.nuclear_morphology.analysis.nucleus.ProfileRefoldMethod;
import com.bmskinner.nuclear_morphology.analysis.nucleus.ProfileRefoldMethod.CurveRefoldingMode;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.gui.MainWindow;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.main.EventHandler;
import com.bmskinner.nuclear_morphology.main.GlobalOptions;
import com.bmskinner.nuclear_morphology.main.ThreadManager;

/**
 * Refold the consensus nucleus for the selected dataset using default
 * parameters
 */
public class RefoldNucleusAction extends SingleDatasetResultAction {

    private static final String PROGRESS_LBL = "Refolding";
    private static final int PROGRESS_BAR_LENGTH = 100;

    /**
     * Refold the given selected dataset
     */
    public RefoldNucleusAction(@NonNull IAnalysisDataset dataset, @NonNull final ProgressBarAcceptor acceptor, @NonNull final EventHandler eh, CountDownLatch doneSignal) {
        super(dataset, PROGRESS_LBL, acceptor, eh);
        this.setLatch(doneSignal);
    }

    @Override
    public void run() {
        this.setProgressBarIndeterminate();
        
        try {
            boolean override = GlobalOptions.getInstance().getBoolean(GlobalOptions.REFOLD_OVERRIDE_KEY);

            IAnalysisMethod m;

            // The averaging method does not work for nuclei that are round, or have extreme variability. 
            // In these cases, or if the program config file has been set to override, use the old profile method.
            if (override){
                m = new ProfileRefoldMethod(dataset, CurveRefoldingMode.FAST);
            } else {
                
                NucleusType t = dataset.getCollection().getNucleusType();
                switch(t){
                    case ROUND:
                    case NEUTROPHIL: {
                        m = new ProfileRefoldMethod(dataset, CurveRefoldingMode.FAST);
                        break;
                    }
                    
                    default: {
                        m = new ConsensusAveragingMethod(dataset);
                    }
                }
            }

            worker = new DefaultAnalysisWorker(m, PROGRESS_BAR_LENGTH);
            worker.addPropertyChangeListener(this);

            this.setProgressMessage(PROGRESS_LBL + ": " + dataset.getName());
            ThreadManager.getInstance().submit(worker);

        } catch (Exception e1) {
            this.cancel();
            warn("Error refolding nucleus");
            stack("Error refolding nucleus", e1);
        }
    }

    @Override
    public void finished() {

        this.cancel();
        fine("Refolding finished, cleaning up");
        super.finished();
        this.countdownLatch();

    }

}
