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
package gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jfree.chart.JFreeChart;
import analysis.AnalysisDataset;
import analysis.RandomSampler;
import charting.charts.HistogramChartFactory;
import gui.LoadingIconDialog;
import gui.components.ExportableChartPanel;
import stats.NucleusStatistic;

@SuppressWarnings("serial")
public class RandomSamplingDialog extends LoadingIconDialog implements ActionListener, ChangeListener, PropertyChangeListener  {
	private AnalysisDataset dataset;
	private ExportableChartPanel chartPanel;
	
	private JSpinner set1SizeSpinner;
	private JSpinner set2SizeSpinner;
	private JSpinner iterattionsSpinner;
	private JComboBox<NucleusStatistic> statsBox = new JComboBox<NucleusStatistic>(NucleusStatistic.values());
	private JButton  runButton;
	private JCheckBox showDensity;
	private RandomSampler sampler;
	
	private JSpinner magnitudeTestSpinner; // Enter an observed magnitude, to get observed count
	private JLabel observedPctLabel = new JLabel("Lower in 0.000% of samples");
	
	private JProgressBar progressBar = new JProgressBar(0, 100);
	
	private List<Double> resultList = new ArrayList<Double>();
	
	public RandomSamplingDialog(final AnalysisDataset dataset){
		super();
		this.dataset = dataset;
		createUI();
		this.setModal(false);
		this.pack();
		this.setVisible(true);
	}
	
	private void createUI(){
		this.setTitle("Random sampling: "+dataset.getName());
		this.setLayout(new BorderLayout());
		
		int cellCount = dataset.getCollection().getNucleusCount();
		int halfCellCount = cellCount >>1;
		
		SpinnerNumberModel first = new SpinnerNumberModel(halfCellCount,
				1,
				cellCount,
				1);
		set1SizeSpinner = new JSpinner(first);
		set1SizeSpinner.addChangeListener(this);
		set1SizeSpinner.setToolTipText("Size of population 1");

		SpinnerNumberModel second = new SpinnerNumberModel(halfCellCount,
				1,
				cellCount,
				1);
		set2SizeSpinner = new JSpinner(second);
		set2SizeSpinner.addChangeListener(this);
		set2SizeSpinner.setToolTipText("Size of population 2");
		
		int iterations = 1000;
		SpinnerNumberModel iterationsModel = new SpinnerNumberModel(iterations,
				1,
				100000,
				1);
		iterattionsSpinner = new JSpinner(iterationsModel);
		iterattionsSpinner.setToolTipText("Number of iterations to run");
		
		runButton = new JButton("Run");
		runButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {

				Thread thr = new Thread(){
					public void run(){
						runSampling();
					}
				};
				thr.start();
				
			
			}
		});	
		
		
		JPanel topPanel = new JPanel(new FlowLayout());
		topPanel.add(set1SizeSpinner);
		topPanel.add(set2SizeSpinner);
		topPanel.add(iterattionsSpinner);
		topPanel.add(statsBox);
		topPanel.add(runButton);
		
		
		topPanel.add(this.getLoadingLabel());
		
		progressBar.setValue(0);
		topPanel.add(this.progressBar);
		
		JPanel headerPanel = new JPanel();
		headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
		
		JPanel labelPanel = new JPanel(new FlowLayout());
		labelPanel.add(new JLabel("Create two non-overlapping populations randomly sampled from the dataset n times, and find the magnitude difference in nuclear parameters", JLabel.LEFT));
		
		headerPanel.add(labelPanel);
		headerPanel.add(topPanel);
		
		this.add(headerPanel, BorderLayout.NORTH);
		
		this.add(createFooter(), BorderLayout.SOUTH);
		
		
		try {
			chartPanel = new ExportableChartPanel(HistogramChartFactory.getInstance().createRandomSampleHistogram(resultList));
			this.add(chartPanel, BorderLayout.CENTER);
		} catch (Exception e) {
			log(Level.SEVERE, "Error making chart", e);
		}
		
		
	}
	
	public void setEnabled(boolean b){
		runButton.setEnabled(b);
		showDensity.setEnabled(b);
		magnitudeTestSpinner.setEnabled(b);

		set1SizeSpinner.setEnabled(b);
		set2SizeSpinner.setEnabled(b);
		iterattionsSpinner.setEnabled(b);
		statsBox.setEnabled(b);
		runButton.setEnabled(b);
	}
	
	private JPanel createFooter(){
		JPanel panel = new JPanel(new FlowLayout());
		
		showDensity = new JCheckBox("Density");
		showDensity.addActionListener(this);
		
		SpinnerNumberModel magnitudeSpinnerModel = new SpinnerNumberModel(1d,
				0d,
				1d,
				0.0001d);
		magnitudeTestSpinner = new JSpinner(magnitudeSpinnerModel);
		JSpinner.NumberEditor numberEditor = new JSpinner.NumberEditor(magnitudeTestSpinner,"0.0000");
		magnitudeTestSpinner.setEditor(numberEditor);
		magnitudeTestSpinner.setPreferredSize(new Dimension(70, 20));
		magnitudeTestSpinner.addChangeListener(this);
		
		panel.add(showDensity);
		panel.add(magnitudeTestSpinner);
		panel.add(observedPctLabel);
		return panel;
	}
	
	private void runSampling(){
		
		
		
		int iterations = (Integer) iterattionsSpinner.getValue();
		int firstCount = (Integer) set1SizeSpinner.getValue();
		int secondCount = (Integer) set2SizeSpinner.getValue();
		NucleusStatistic stat = (NucleusStatistic) statsBox.getSelectedItem();
		
		try {
			setStatusLoading();
			setEnabled(false);

			sampler = new RandomSampler(dataset, stat, iterations, firstCount, secondCount);
			sampler.addPropertyChangeListener(this);
			sampler.execute();

		} catch (Exception e) {
			log(Level.SEVERE, "Error running sampling", e);
			setEnabled(true);
		}
	}
	
	public void finished(){
		try {
			resultList = sampler.getResults();
			Collections.sort(resultList);
			double observedPct = calculateObservedPercent();
			DecimalFormat df = new DecimalFormat("#0.000"); 
			observedPctLabel.setText("Lower in "+  df.format(observedPct)  +"% of samples");
			sampler = null;
			progressBar.setValue(0);
			setEnabled(true);

			JFreeChart chart = null;
			if(showDensity.isSelected()){
				chart = HistogramChartFactory.getInstance().createRandomSampleDensity(resultList);
			} else {
				chart = HistogramChartFactory.getInstance().createRandomSampleHistogram(resultList);
			}
			chartPanel.setChart(chart);
			setStatusLoaded();
		} catch (Exception e) {
			log(Level.SEVERE, "Error running sampling", e);
		}
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		JFreeChart chart = null;
		try {
			if(showDensity.isSelected()){

				chart = HistogramChartFactory.getInstance().createRandomSampleDensity(resultList);

			} else {
				chart = HistogramChartFactory.getInstance().createRandomSampleHistogram(resultList);
			}
		}catch (Exception e) {
			log(Level.SEVERE, "Error running sampling", e);
		}
		chartPanel.setChart(chart);

	}

	@Override
	public void stateChanged(ChangeEvent e) {

		try {
			int cellCount = dataset.getCollection().getNucleusCount();

			if(e.getSource()==set1SizeSpinner){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();

				int firstCount  = (Integer) set1SizeSpinner.getValue();
				int secondCount = (Integer) set2SizeSpinner.getValue(); 
				if(secondCount > cellCount - firstCount){
					set2SizeSpinner.setValue( cellCount - firstCount );
				}

			}
			
			if(e.getSource()==set2SizeSpinner){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();

				int firstCount  = (Integer) set1SizeSpinner.getValue();
				int secondCount = (Integer) set2SizeSpinner.getValue(); 
				if(firstCount > cellCount - secondCount){
					set1SizeSpinner.setValue( cellCount - secondCount );
				}

			}
			
			
			if(e.getSource()==magnitudeTestSpinner){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();

				double observedPct = calculateObservedPercent();
				DecimalFormat df = new DecimalFormat("#0.000"); 
				observedPctLabel.setText("Lower in "+  df.format(observedPct)  +"% of samples");

			}
			
		} catch(Exception e1){
			log(Level.SEVERE, "Error in spinners", e1);
		}
		
	}
	
	private double calculateObservedPercent(){
		double magnitudeTest  = (Double) magnitudeTestSpinner.getValue();
		
		if(resultList.size()==0){
			return 0;
		}
		
		int count = 0;
		for(double d : resultList){
			if(d <= magnitudeTest){
				count++;
			} else {
				break;
			}
		}
		
		double observedPct =  ( (double) count / (double) resultList.size()) * 100 ;
		return observedPct;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {

		int value = (Integer) evt.getNewValue(); // should be percent
		log(Level.FINEST,"Property change: "+value);
		
		if(value >=0 && value <=100){
			
			if(this.progressBar.isIndeterminate()){
				this.progressBar.setIndeterminate(false);
			}
			this.progressBar.setValue(value);
		}

		if(evt.getPropertyName().equals("Finished")){
			log(Level.FINEST,"Worker signaled finished");
			finished();
		}
		
	}
}
