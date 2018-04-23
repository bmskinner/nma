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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.signals.shells.ShellAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.signals.shells.ShellDetector;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult.ShrinkType;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.gui.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.MainWindow;
import com.bmskinner.nuclear_morphology.gui.dialogs.SubAnalysisSetupDialog;
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
    	
    	ShellAnalysisSetupDialog sd = new ShellAnalysisSetupDialog(mw, dataset);
    	if(sd.isReadyToRun()) {
    		
    		int shellCount = sd.getShellCount();
    		if(! datasetParametersOk(shellCount)){
            	this.cancel();
            	return;
            }
    		    		
    		IAnalysisMethod m = sd.getMethod();
    		worker = new DefaultAnalysisWorker(m);
    		worker.addPropertyChangeListener(this);
    		ThreadManager.getInstance().submit(worker);
    		
    	}
    }
    
    /**
     * Check if the nuclei in the dataset are suitable for shell analysis
     * @param shells
     * @return
     */
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
    
    
    @SuppressWarnings("serial")
    private class ShellAnalysisSetupDialog extends SubAnalysisSetupDialog {

        private static final String DIALOG_TITLE = "Shell analysis options";
        
        private int nShells = ShellDetector.DEFAULT_SHELL_COUNT;
        private ShrinkType type = ShrinkType.AREA;


        protected JPanel headingPanel;
        protected JPanel optionsPanel;
        protected JPanel footerPanel;

        public ShellAnalysisSetupDialog(final MainWindow mw, final IAnalysisDataset dataset) {
            this(mw, dataset, DIALOG_TITLE);
        }
        
        public int getShellCount() {
        	return nShells;
        }

        /**
         * Constructor that does not make panel visible
         * 
         * @param mw
         * @param title
         */
        protected ShellAnalysisSetupDialog(final MainWindow mw, final IAnalysisDataset dataset, final String title) {
            super(mw, dataset, title);
            setDefaults();
            createUI();
            packAndDisplay();
        }

        protected JPanel createHeader() {

            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            return panel;
        }

        @Override
        public IAnalysisMethod getMethod() {
        	return new ShellAnalysisMethod(dataset, nShells, type);
        }

        @Override
        protected void createUI() {

            contentPanel.setLayout(new BorderLayout());
            contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
            setContentPane(contentPanel);


            headingPanel = createHeader();
            contentPanel.add(headingPanel, BorderLayout.NORTH);

            footerPanel = createFooter();
            contentPanel.add(footerPanel, BorderLayout.SOUTH);


            optionsPanel = new JPanel();
            GridBagLayout layout = new GridBagLayout();
            optionsPanel.setLayout(layout);
            
            List<JLabel> labels = new ArrayList<JLabel>();
            List<Component> fields = new ArrayList<Component>();
            
            JComboBox<ShrinkType> typeBox = new JComboBox<>(ShrinkType.values());
            typeBox.setSelectedItem(ShrinkType.AREA);
            typeBox.addActionListener(e -> type = (ShrinkType) typeBox.getSelectedItem());
            
            labels.add(new JLabel("Shrink method"));
            fields.add(typeBox);
            
            
            SpinnerNumberModel sModel = new SpinnerNumberModel(ShellDetector.DEFAULT_SHELL_COUNT, 2, 10, 1);
            JSpinner spinner = new JSpinner(sModel);
            spinner.addChangeListener(e-> nShells = (int) sModel.getValue());
            
            labels.add(new JLabel("Number of shells"));
            fields.add(spinner);
            
            this.addLabelTextRows(labels, fields, layout, optionsPanel);
            

            contentPanel.add(optionsPanel, BorderLayout.CENTER);
        }

		@Override
		protected void setDefaults() {
			// TODO Auto-generated method stub
			
		}
    }
}
