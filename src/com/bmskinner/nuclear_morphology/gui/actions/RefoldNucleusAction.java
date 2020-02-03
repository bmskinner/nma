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
package com.bmskinner.nuclear_morphology.gui.actions;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.nucleus.ConsensusAveragingMethod;
import com.bmskinner.nuclear_morphology.analysis.nucleus.ProfileRefoldMethod;
import com.bmskinner.nuclear_morphology.analysis.nucleus.ProfileRefoldMethod.CurveRefoldingMode;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Refold the consensus nucleus for the selected dataset using default
 * parameters
 */
public class RefoldNucleusAction extends SingleDatasetResultAction {
	
	private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private static final @NonNull String PROGRESS_LBL = "Refolding";
    private static final int PROGRESS_BAR_LENGTH = 100;

    /**
     * Refold the given selected dataset
     */
    public RefoldNucleusAction(@NonNull IAnalysisDataset dataset, @NonNull final ProgressBarAcceptor acceptor, @NonNull final EventHandler eh, CountDownLatch doneSignal) {
        super(dataset, PROGRESS_LBL, acceptor, eh);
        this.setLatch(doneSignal);
    }
    
    public RefoldNucleusAction(@NonNull List<IAnalysisDataset> list, @NonNull final ProgressBarAcceptor acceptor, @NonNull final EventHandler eh, CountDownLatch doneSignal) {
        super(list, PROGRESS_LBL, acceptor, eh);
        this.setLatch(doneSignal);
    }

    @Override
    public void run() {
        this.setProgressBarIndeterminate();
        
        try {
            boolean override = GlobalOptions.getInstance().getBoolean(GlobalOptions.REFOLD_OVERRIDE_KEY);

            IAnalysisMethod m;
            int progressLength = PROGRESS_BAR_LENGTH;
            // The averaging method does not work for nuclei that are round, or have extreme variability. 
            // In these cases, or if the program config file has been set to override, use the old profile method.
            if (override){
                m = new ProfileRefoldMethod(dataset, CurveRefoldingMode.FAST);
                progressLength = CurveRefoldingMode.FAST.maxIterations();
            } else {
                NucleusType t = dataset.getCollection().getNucleusType();
                m = chooseMethod(t);
                progressLength = chooseProgressLength(t);
            }

            
            worker = new DefaultAnalysisWorker(m, progressLength);
            worker.addPropertyChangeListener(this);

            this.setProgressMessage(PROGRESS_LBL + ": " + dataset.getName());
            ThreadManager.getInstance().submit(worker);

        } catch (Exception e1) {
            this.cancel();
            LOGGER.warning("Error refolding nucleus");
            LOGGER.log(Loggable.STACK, "Error refolding nucleus", e1);
        }
    }
    
    /**
     * Choose the type of consensus building method based on the nucleus type
     * @param t
     * @return
     * @throws Exception 
     */
    private IAnalysisMethod chooseMethod(NucleusType t) throws Exception {
    	switch(t){
        case ROUND:
        case NEUTROPHIL: {
            return new ProfileRefoldMethod(dataset, CurveRefoldingMode.FAST);
        }
        
        default: {
            return new ConsensusAveragingMethod(dataset);
        }
    }
    }
    
    /**
     * Choose the length of the progress bar based on the nucleus type
     * (reflects the method chosen in chooseMethod()
     * @param t
     * @return
     */
    private int chooseProgressLength(NucleusType t) {
    	switch(t){
        case ROUND:
        case NEUTROPHIL: {
            return CurveRefoldingMode.FAST.maxIterations();
        }
        
        default: {
            return PROGRESS_BAR_LENGTH;
        }
    	}
    }

    @Override
    public void finished() {

    	Runnable r = () -> {

    		// if no list was provided, or no more entries remain,
    		// call the finish
    		if (!hasRemainingDatasetsToProcess()) {
    			countdownLatch();
    			RefoldNucleusAction.super.finished();

    		} else {
    			// otherwise analyse the next item in the list
    			cancel(); // remove progress bar
    			Runnable task = new RefoldNucleusAction(getRemainingDatasetsToProcess(), progressAcceptors.get(0), eh, getLatch().get());
    			task.run();
    		}
    	};
    	new Thread(r).start();
    }

}
