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
package com.bmskinner.nma.gui.dialogs;

import java.awt.BorderLayout;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.analysis.classification.NucleusClusteringMethod;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.OptionsFactory;
import com.bmskinner.nma.gui.components.panels.ClusteringMethodSelectionPanel;
import com.bmskinner.nma.gui.components.panels.DimensionalReductionSelectionPanel;
import com.bmskinner.nma.gui.components.panels.ParameterSelectionPanel;

/**
 * Setup for clustering. It inherits the parameters for inclusion from the hierarchical tree
 * setup, and adds panels for clustering method
 * @author Ben Skinner
 *
 */
@SuppressWarnings("serial")
public class ClusteringSetupDialog extends SubAnalysisSetupDialog  {

    private static final String DIALOG_TITLE = "Clustering options";
	protected final HashOptions options;

    public ClusteringSetupDialog(final @NonNull IAnalysisDataset dataset) {
        super(dataset, DIALOG_TITLE);
        options = OptionsFactory.makeDefaultClusteringOptions().build();
        createUI();
        packAndDisplay();
    }
    
	/**
	 * Set the default options
	 */
	@Override
	protected void setDefaults() {
		// handled by the options factory
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
