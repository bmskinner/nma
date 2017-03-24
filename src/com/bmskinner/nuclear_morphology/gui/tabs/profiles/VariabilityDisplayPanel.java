/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package com.bmskinner.nuclear_morphology.gui.tabs.profiles;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.util.logging.Level;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.charting.charts.AbstractChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.MorphologyChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.generic.BorderTagObject;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.gui.GlobalOptions;
import com.bmskinner.nuclear_morphology.gui.components.panels.BorderTagOptionsPanel;
import com.bmskinner.nuclear_morphology.gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;
import com.bmskinner.nuclear_morphology.gui.components.panels.ProfileCollectionTypeSettingsPanel;
import com.bmskinner.nuclear_morphology.gui.components.panels.ProfileMarkersOptionsPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;
import com.bmskinner.nuclear_morphology.stats.SignificanceTest;

@SuppressWarnings("serial")
public class VariabilityDisplayPanel extends DetailPanel implements ActionListener, ChangeListener {
	
	private JPanel buttonPanel = new JPanel(new FlowLayout());
	protected ExportableChartPanel chartPanel;
	private JSpinner pvalueSpinner;

	private BorderTagOptionsPanel borderTagOptionsPanel = new BorderTagOptionsPanel();
	private ProfileCollectionTypeSettingsPanel profileCollectionTypeSettingsPanel = new ProfileCollectionTypeSettingsPanel();
	private ProfileMarkersOptionsPanel profileMarkersOptionsPanel = new ProfileMarkersOptionsPanel();

	public VariabilityDisplayPanel(){
		super();
		this.setLayout(new BorderLayout());
		
		ChartOptions options =  new ChartOptionsBuilder()
			.setProfileType(ProfileType.ANGLE)
			.build();
		
		JFreeChart chart = new MorphologyChartFactory(options).makeVariabilityChart();


		chartPanel = new ExportableChartPanel(chart);
		chartPanel.getChartRenderingInfo().setEntityCollection(null);
		this.add(chartPanel, BorderLayout.CENTER);

		buttonPanel.add(borderTagOptionsPanel);
		borderTagOptionsPanel.addActionListener(this);
		borderTagOptionsPanel.setEnabled(false);


		pvalueSpinner = new JSpinner(new SpinnerNumberModel(SignificanceTest.FIVE_PERCENT_SIGNIFICANCE_LEVEL,	0d, 1d, 0.001d));
		pvalueSpinner.setEnabled(false);
		pvalueSpinner.addChangeListener(this);
		JComponent field = ((JSpinner.DefaultEditor) pvalueSpinner.getEditor());
		Dimension prefSize = field.getPreferredSize();
		prefSize = new Dimension(50, prefSize.height);
		field.setPreferredSize(prefSize);

		// add extra fields to the header panel
		buttonPanel.add(new JLabel("Dip test p-value:"));
		buttonPanel.add(pvalueSpinner);


		profileCollectionTypeSettingsPanel.addActionListener(this);
		profileCollectionTypeSettingsPanel.setEnabled(false);
		buttonPanel.add(profileCollectionTypeSettingsPanel);

		buttonPanel.revalidate();

		this.add(buttonPanel, BorderLayout.NORTH);
	}


	public void setEnabled(boolean b){
		borderTagOptionsPanel.setEnabled(b);
		profileCollectionTypeSettingsPanel.setEnabled(b);
		profileMarkersOptionsPanel.setEnabled(b);
		pvalueSpinner.setEnabled(b);
	}

//	public void update(List<AnalysisDataset> list){
//
//		
//	}

	/**
	 * Update the profile panel with data from the given datasets
	 * @param list the datasets
	 * @param normalised flag for raw or normalised lengths
	 * @param rightAlign flag for left or right alignment (no effect if normalised is true)
	 */	
	private void updateProfiles(ChartOptions options){

		try {
			
			setChart(options);
//			JFreeChart chart = getChart(options);			
//			chartPanel.setChart(chart);
			
		} catch (Exception e) {
			log(Level.SEVERE, "Error in plotting variability chart", e);
		}	
	}


	@Override
	public void actionPerformed(ActionEvent e) {

		update(getDatasets());

	}

	@Override
	public void stateChanged(ChangeEvent arg0) {
		if(arg0.getSource()==pvalueSpinner){
			JSpinner j = (JSpinner) arg0.getSource();
			try {
				j.commitEdit();
			} catch (ParseException e) {
				log(Level.SEVERE, "Error setting p-value spinner", e);
			}
		}
		update(getDatasets());

	}
	


	@Override
	protected void updateSingle() {

			this.setEnabled(true);

			BorderTagObject tag = borderTagOptionsPanel.getSelected();
			boolean showMarkers = profileMarkersOptionsPanel.showMarkers();
			ProfileType type = profileCollectionTypeSettingsPanel.getSelected();

			ChartOptions options =  new ChartOptionsBuilder()
				.setDatasets(getDatasets())
				.setNormalised(true)
				.setAlignment(ProfileAlignment.LEFT)
				.setTag(tag)
				.setShowMarkers(showMarkers)
				.setModalityPosition((Double) pvalueSpinner.getValue())
				.setSwatch(GlobalOptions.getInstance().getSwatch())
				.setProfileType(type)
				.setTarget(chartPanel)
				.build();

			updateProfiles(options);
		
	}

	@Override
	protected void updateMultiple() {
		updateSingle();
		// Don't allow marker selection for multiple datasets
		profileMarkersOptionsPanel.setEnabled(false);
		pvalueSpinner.setEnabled(false);
	}

	@Override
	protected void updateNull() {
		updateSingle();
		this.setEnabled(false);
	}
	
	@Override
	public void setChartsAndTablesLoading(){
		super.setChartsAndTablesLoading();
		chartPanel.setChart(AbstractChartFactory.createLoadingChart());			
	}
	
	@Override
	protected JFreeChart createPanelChartType(ChartOptions options){
		return new MorphologyChartFactory(options).makeVariabilityChart();
	}
	
}
