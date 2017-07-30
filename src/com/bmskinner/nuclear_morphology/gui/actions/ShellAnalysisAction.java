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

import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.signals.ShellAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.signals.ShellDetector;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.gui.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.MainWindow;
import com.bmskinner.nuclear_morphology.main.ThreadManager;

/**
 * Prepare and run a shell analysis on the provided dataset.
 * @author ben
 *
 */
public class ShellAnalysisAction extends SingleDatasetResultAction {
	
	private static final String CIRC_ERROR_MESSAGE = "Min nucleus circularity is too low to make shells";
	private static final String AREA_ERROR_MESSAGE = "Min nucleus area is too small to break into shells";
	
    /**
     * Construct with a dataset and main event window
     * @param dataset
     * @param mw
     */
    public ShellAnalysisAction(IAnalysisDataset dataset, MainWindow mw) {
        super(dataset, "Shell analysis", mw);

    }

    @Override
    public void run() {
        SpinnerNumberModel sModel = new SpinnerNumberModel(ShellDetector.DEFAULT_SHELL_COUNT, 2, 10, 1);
        JSpinner spinner = new JSpinner(sModel);

        int option = JOptionPane.showOptionDialog(null, spinner, "Select number of shells",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
        if (option == JOptionPane.CANCEL_OPTION) {
            // user hit cancel
            this.cancel();
            return;

        } else if (option == JOptionPane.OK_OPTION) {

            int shellCount = (Integer) spinner.getModel().getValue();
            
            if(! datasetParametersOk(shellCount)){
            	this.cancel();
            	return;
            }

            IAnalysisMethod m = new ShellAnalysisMethod(dataset, shellCount);
            worker = new DefaultAnalysisWorker(m);

            // worker = new ShellAnalysisWorker(dataset,shellCount);

            worker.addPropertyChangeListener(this);
            ThreadManager.getInstance().submit(worker);
        }
    }
    
    private boolean datasetParametersOk(int shells){
    		
			double area = dataset.getCollection()
					.getMin(PlottableStatistic.AREA, 
							CellularComponent.NUCLEUS, 
							MeasurementScale.PIXELS);
			double minArea = ShellAnalysisMethod.MINIMUM_AREA_PER_SHELL * shells;
			if(area < minArea){
				JOptionPane.showMessageDialog(null, AREA_ERROR_MESSAGE);
				return false;
			}
			
			
			double circ = dataset.getCollection()
					.getMin(PlottableStatistic.CIRCULARITY, 
							CellularComponent.NUCLEUS, 
							MeasurementScale.PIXELS);

			if(circ < ShellAnalysisMethod.MINIMUM_CIRCULARITY){
				JOptionPane.showMessageDialog(null, CIRC_ERROR_MESSAGE);
				return false;
			}
    	
    	return true;
    	
    	
    }

    @Override
    public void finished() {
        getDatasetEventHandler().fireDatasetEvent(DatasetEvent.REFRESH_CACHE, dataset);
        super.finished();
    }
}
