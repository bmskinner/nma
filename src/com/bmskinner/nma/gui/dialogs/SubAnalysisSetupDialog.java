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
import java.awt.Frame;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.MissingOptionException;

/**
 * A base class for the sub analyses setup options. It contains a reference to
 * the main analysis window to make the dialog modal It can return an
 * IAnalysisMethod preconfigured for running
 * 
 * @author bms41
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public abstract class SubAnalysisSetupDialog extends SettingsDialog {

	protected final @NonNull List<IAnalysisDataset> datasets = new ArrayList<>();

	/**
	 * Construct with a program window to listen for actions, and a dataset to
	 * operate on
	 * 
	 * @param parent
	 * @param dataset
	 */
	public SubAnalysisSetupDialog(final Frame parent, final @NonNull IAnalysisDataset dataset,
			final String title) {
		super(parent, true);
		this.datasets.add(dataset);
		this.setTitle(title);
		this.setModal(true);

		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPanel);
	}

	/**
	 * Construct with a program window to listen for actions, and a dataset to
	 * operate on
	 * 
	 * @param parent
	 * @param dataset
	 */
	public SubAnalysisSetupDialog(final Frame parent,
			final @NonNull List<IAnalysisDataset> datasets, final String title) {
		super(parent, true);
		this.datasets.addAll(datasets);
		this.setTitle(title);
		this.setModal(true);

		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPanel);
	}

	/**
	 * Construct with a main program window to listen for actions, and a dataset to
	 * operate on
	 * 
	 * @param mw
	 * @param dataset
	 */
	public SubAnalysisSetupDialog(final @NonNull IAnalysisDataset dataset, final String title) {
		super(true);
		this.datasets.add(dataset);
		this.setTitle(title);

		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPanel);
	}

	/**
	 * Construct with a main program window to listen for actions, and a dataset to
	 * operate on
	 * 
	 * @param mw
	 * @param dataset
	 */
	public SubAnalysisSetupDialog(final @NonNull List<IAnalysisDataset> datasets,
			final String title) {
		super(true);
		this.datasets.addAll(datasets);
		this.setTitle(title);

		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPanel);
	}

	protected void packAndDisplay() {
		this.pack();
		this.setLocationRelativeTo(null);
		this.setVisible(true);
	}

	protected IAnalysisDataset getFirstDataset() {
		return datasets.get(0);
	}

	/**
	 * Get the method for the analysis to be run
	 * 
	 * @return
	 */
	public abstract IAnalysisMethod getMethod();

	/**
	 * Get the options for the analysis to be run. Can be null
	 * 
	 * @return
	 */
	public abstract HashOptions getOptions();

	/**
	 * Make the UI for the dialog
	 * 
	 * @throws MissingOptionException if an options does not contain expected values
	 */
	protected abstract void createUI() throws MissingOptionException;

	/**
	 * Set the default options for the dialog
	 */
	protected abstract void setDefaults();

}
