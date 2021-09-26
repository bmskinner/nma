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
package com.bmskinner.nuclear_morphology.gui.dialogs;

import java.awt.BorderLayout;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.classification.NucleusClusteringMethod;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.gui.components.panels.ClusteringMethodSelectionPanel;
import com.bmskinner.nuclear_morphology.gui.components.panels.DimensionalReductionSelectionPanel;
import com.bmskinner.nuclear_morphology.gui.components.panels.ParameterSelectionPanel;

/**
 * Setup for clustering. It inherits the parameters for inclusion from the hierarchical tree
 * setup, and adds panels for clustering method
 * @author ben
 *
 */
@SuppressWarnings("serial")
public class ClusteringSetupDialog extends SubAnalysisSetupDialog  {

    private static final String DIALOG_TITLE = "Clustering options";
	protected final IClusteringOptions options;

    public ClusteringSetupDialog(final @NonNull IAnalysisDataset dataset) {
        super(dataset, DIALOG_TITLE);
        options = OptionsFactory.makeClusteringOptions();
        setDefaults();
        createUI();
        packAndDisplay();
    }
    
	/**
	 * Set the default options
	 */
	@Override
	protected void setDefaults() {
		options.setClusterNumber(IClusteringOptions.DEFAULT_MANUAL_CLUSTER_NUMBER);
		options.setHierarchicalMethod(IClusteringOptions.DEFAULT_HIERARCHICAL_METHOD);
		options.setIterations(IClusteringOptions.DEFAULT_EM_ITERATIONS);
		options.setBoolean(IClusteringOptions.USE_TSNE_KEY,  IClusteringOptions.DEFAULT_USE_TSNE);
	}

    @Override
    public IAnalysisMethod getMethod() {
    	return new NucleusClusteringMethod(getFirstDataset(), options);
    }
    
	@Override
	public HashOptions getOptions() {
		return options;
	}

    @Override
    protected void createUI() {
        getContentPane().add(createHeader(), BorderLayout.NORTH);
    	getContentPane().add(createFooter(), BorderLayout.SOUTH);
        getContentPane().add(createOptionsPanel(), BorderLayout.CENTER);
    }
    
    private JPanel createOptionsPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		panel.add(new ParameterSelectionPanel(getFirstDataset(), options));
		panel.add(new DimensionalReductionSelectionPanel(getFirstDataset(), options));
		panel.add(new ClusteringMethodSelectionPanel(getFirstDataset(), options));
		return panel;
	}
}
