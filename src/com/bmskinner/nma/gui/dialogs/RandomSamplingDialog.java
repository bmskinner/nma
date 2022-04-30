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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

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

import com.bmskinner.nma.analysis.DefaultAnalysisWorker;
import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.analysis.IAnalysisWorker;
import com.bmskinner.nma.analysis.RandomSamplingMethod;
import com.bmskinner.nma.analysis.RandomSamplingMethod.RandomSamplingResult;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.visualisation.charts.HistogramChartFactory;
import com.bmskinner.nma.visualisation.charts.panels.ExportableChartPanel;
import com.bmskinner.nma.visualisation.datasets.ChartDatasetCreationException;
import com.bmskinner.nma.visualisation.options.ChartOptions;
import com.bmskinner.nma.visualisation.options.DefaultChartOptions;

@SuppressWarnings("serial")
public class RandomSamplingDialog extends LoadingIconDialog
        implements ChangeListener, PropertyChangeListener {
	
	private static final Logger LOGGER = Logger.getLogger(RandomSamplingDialog.class.getName());

    private IAnalysisDataset     dataset;
    private ExportableChartPanel chartPanel;

    private JSpinner                      set1SizeSpinner;
    private JSpinner                      set2SizeSpinner;
    private JSpinner                      iterattionsSpinner;
    private JComboBox<Measurement> statsBox;
    private JButton                       runButton;
    private JCheckBox                     showDensity;
    private IAnalysisWorker               sampler;

    // Enter a magnitude to get observed proportion of datasets
    private JSpinner magnitudeTestSpinner;
    private JLabel   observedPctLabel = new JLabel("Lower in 0.000% of samples");

    private JProgressBar progressBar = new JProgressBar(0, 100);

    private List<Double> resultList = new ArrayList<Double>();

    public RandomSamplingDialog(final IAnalysisDataset dataset) {
        super();
        this.dataset = dataset;
        createUI();
        this.setModal(false);
        this.pack();
        this.setVisible(true);
    }

    private void createUI() {
        this.setTitle("Random sampling: " + dataset.getName());
        this.setLayout(new BorderLayout());

        this.add(createHeader(), BorderLayout.NORTH);
        this.add(createFooter(), BorderLayout.SOUTH);

        try {
            chartPanel = new ExportableChartPanel(HistogramChartFactory.createRandomSampleHistogram(resultList));
            this.add(chartPanel, BorderLayout.CENTER);
        } catch (Exception e) {
            LOGGER.log(Loggable.STACK, "Error making chart", e);
        }

    }

    @Override
    public void setEnabled(boolean b) {
        runButton.setEnabled(b);
        showDensity.setEnabled(b);
        magnitudeTestSpinner.setEnabled(b);

        set1SizeSpinner.setEnabled(b);
        set2SizeSpinner.setEnabled(b);
        iterattionsSpinner.setEnabled(b);
        statsBox.setEnabled(b);
        runButton.setEnabled(b);
    }
    
    private JPanel createButtonPanel(){
    	statsBox = new JComboBox<Measurement>(Measurement
                .getNucleusStats().toArray(new Measurement[0]));

        int cellCount = dataset.getCollection().size();
        int halfCellCount = cellCount >> 1;

        SpinnerNumberModel first = new SpinnerNumberModel(halfCellCount, 1, cellCount, 1);
        set1SizeSpinner = new JSpinner(first);
        set1SizeSpinner.addChangeListener(this);
        set1SizeSpinner.setToolTipText("Size of population 1");

        SpinnerNumberModel second = new SpinnerNumberModel(halfCellCount, 1, cellCount, 1);
        set2SizeSpinner = new JSpinner(second);
        set2SizeSpinner.addChangeListener(this);
        set2SizeSpinner.setToolTipText("Size of population 2");

        int iterations = 1000;
        SpinnerNumberModel iterationsModel = new SpinnerNumberModel(iterations, 1, 100000, 1);
        iterattionsSpinner = new JSpinner(iterationsModel);
        iterattionsSpinner.setToolTipText("Number of iterations to run");

        runButton = new JButton("Run");
        runButton.addActionListener(e->{ runSampling(); });


        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.add(set1SizeSpinner);
        topPanel.add(set2SizeSpinner);
        topPanel.add(iterattionsSpinner);
        topPanel.add(statsBox);
        topPanel.add(runButton);

        topPanel.add(this.getLoadingLabel());

        progressBar.setValue(0);
        topPanel.add(this.progressBar);
        progressBar.setVisible(false);
        return topPanel;
    }
    
    private JPanel createHeader() {

    	JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JPanel labelPanel = new JPanel(new FlowLayout());
        labelPanel.add(new JLabel(
                "Create two non-overlapping populations randomly sampled "
                + "from the dataset n times, and find the magnitude difference in nuclear parameters",
                JLabel.LEFT));

        headerPanel.add(labelPanel);
        
        JPanel btnPanel = createButtonPanel();
        headerPanel.add(btnPanel);
        return headerPanel;
    }

    private JPanel createFooter() {


    	showDensity = new JCheckBox("Density");
    	showDensity.addActionListener(e->{
    		JFreeChart chart = null;
    		try {
    			if (showDensity.isSelected()) {
    				ChartOptions options = new DefaultChartOptions(dataset);
    				chart = new HistogramChartFactory(options).createRandomSampleDensity(resultList);

    			} else {
    				chart = HistogramChartFactory.createRandomSampleHistogram(resultList);
    			}
    		} catch (ChartDatasetCreationException e1) {
    			chart = HistogramChartFactory.createErrorChart();
    			LOGGER.log(Loggable.STACK, e1.getMessage(), e1);
    		}

    		chartPanel.setChart(chart);
    	});

        SpinnerNumberModel magnitudeSpinnerModel = new SpinnerNumberModel(1d, 0d, 1d, 0.0001d);
        magnitudeTestSpinner = new JSpinner(magnitudeSpinnerModel);
        JSpinner.NumberEditor numberEditor = new JSpinner.NumberEditor(magnitudeTestSpinner, "0.0000");
        magnitudeTestSpinner.setEditor(numberEditor);
        magnitudeTestSpinner.setPreferredSize(new Dimension(70, 20));
        magnitudeTestSpinner.addChangeListener(this);
        
        JPanel panel = new JPanel(new FlowLayout());
        panel.add(showDensity);
        panel.add(magnitudeTestSpinner);
        panel.add(observedPctLabel);
        return panel;
    }

    private void runSampling() {

        int iterations = (int) iterattionsSpinner.getValue();
        int firstCount = (int) set1SizeSpinner.getValue();
        int secondCount = (int) set2SizeSpinner.getValue();
        Measurement stat = (Measurement) statsBox.getSelectedItem();

        try {
            setStatusLoading();
            progressBar.setValue(0);
            progressBar.setVisible(true);
            setEnabled(false);

            IAnalysisMethod randomMethod = new RandomSamplingMethod(dataset, stat, iterations, firstCount, secondCount);
            sampler = new DefaultAnalysisWorker(randomMethod, iterations);
            sampler.addPropertyChangeListener(this);
            ThreadManager.getInstance().submit(sampler);

        } catch (Exception e) {
            LOGGER.log(Loggable.STACK, "Error running sampling", e);
            setEnabled(true);
        }
    }

    public void finished() {
        try {
        	
        	progressBar.setValue(0);
        	progressBar.setVisible(false);
            setEnabled(true);
        	
        	RandomSamplingResult r = (RandomSamplingResult) sampler.get();
            resultList = r.getValues();
            Collections.sort(resultList);
            double observedPct = calculateObservedPercent();
            DecimalFormat df = new DecimalFormat("#0.000");
            observedPctLabel.setText("Lower in " + df.format(observedPct) + "% of samples");
            sampler = null;
            

            JFreeChart chart = null;
            if (showDensity.isSelected()) {
                ChartOptions options = new DefaultChartOptions(dataset);
                chart = new HistogramChartFactory(options).createRandomSampleDensity(resultList);
            } else {
                chart = HistogramChartFactory.createRandomSampleHistogram(resultList);
            }
            chartPanel.setChart(chart);
            setStatusLoaded();
        } catch (Exception e) {
            LOGGER.warning("Error running sampling");
            LOGGER.log(Loggable.STACK, "Error running sampling", e);
        }
    }

    @Override
    public void stateChanged(ChangeEvent e) {

        try {
            int cellCount = dataset.getCollection().size();

            if (e.getSource() == set1SizeSpinner) {
                JSpinner j = (JSpinner) e.getSource();
                j.commitEdit();

                int firstCount = (Integer) set1SizeSpinner.getValue();
                int secondCount = (Integer) set2SizeSpinner.getValue();
                if (secondCount > cellCount - firstCount) {
                    set2SizeSpinner.setValue(cellCount - firstCount);
                }

            }

            if (e.getSource() == set2SizeSpinner) {
                JSpinner j = (JSpinner) e.getSource();
                j.commitEdit();

                int firstCount = (Integer) set1SizeSpinner.getValue();
                int secondCount = (Integer) set2SizeSpinner.getValue();
                if (firstCount > cellCount - secondCount) {
                    set1SizeSpinner.setValue(cellCount - secondCount);
                }

            }

            if (e.getSource() == magnitudeTestSpinner) {
                JSpinner j = (JSpinner) e.getSource();
                j.commitEdit();

                double observedPct = calculateObservedPercent();
                DecimalFormat df = new DecimalFormat("#0.000");
                observedPctLabel.setText("Lower in " + df.format(observedPct) + "% of samples");

            }

        } catch (Exception e1) {
            LOGGER.log(Loggable.STACK, "Error in spinners", e1);
        }

    }

    private double calculateObservedPercent() {
        double magnitudeTest = (Double) magnitudeTestSpinner.getValue();

        if (resultList.size() == 0) {
            return 0;
        }

        int count = 0;
        for (double d : resultList) {
            if (d <= magnitudeTest) {
                count++;
            } else {
                break;
            }
        }

        double observedPct = ((double) count / (double) resultList.size()) * 100;
        return observedPct;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {

        if (evt.getNewValue() instanceof Integer) {
            int value = (Integer) evt.getNewValue(); // should be percent


            if (value >= 0 && value <= 100) {

                if (this.progressBar.isIndeterminate()) {
                    this.progressBar.setIndeterminate(false);
                }
                this.progressBar.setValue(value);
            }
        }

        if (evt.getPropertyName().equals(IAnalysisWorker.FINISHED_MSG)) {
            finished();
        }

    }
}
