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
package com.bmskinner.nuclear_morphology.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.logging.Level;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultXYDataset;

import com.bmskinner.nuclear_morphology.charting.ChartComponents;
import com.bmskinner.nuclear_morphology.charting.charts.MorphologyChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.components.DefaultCell;
import com.bmskinner.nuclear_morphology.components.DefaultCellCollection;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.IProfileCollection;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.options.MissingOptionException;
import com.bmskinner.nuclear_morphology.gui.LoadingIconDialog;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;
import com.bmskinner.nuclear_morphology.stats.Quartile;

@SuppressWarnings("serial")
public class AngleWindowSizeExplorer  extends LoadingIconDialog implements ChangeListener {
	
	private IAnalysisDataset dataset;
	private ExportableChartPanel chartPanel;
	
	private JSpinner windowSizeMinSpinner;
	private JSpinner windowSizeMaxSpinner;
	private JSpinner stepSizeSpinner;
	
	private JButton  runButton;
	
	public AngleWindowSizeExplorer(final IAnalysisDataset dataset){
		super();
		this.dataset = dataset;
		finest("Creating angle window explorer UI");
		try{
			createUI();
		} catch (Exception e){
			error("Error creating angle window explorer UI", e);
		}
		this.setModal(false);
		this.pack();
		finest("Displaying angle window explorer");
		this.setVisible(true);
	}
	
	private void createUI(){
		this.setTitle("Angle window size explorer: "+dataset.getName());
		this.setLayout(new BorderLayout());
		
		this.add(createSettingsPanel(), BorderLayout.NORTH);
		
		chartPanel = new ExportableChartPanel(MorphologyChartFactory.createEmptyChart());
		this.add(chartPanel, BorderLayout.CENTER);

		
	}
	
	private JPanel createSettingsPanel(){
		JPanel panel = new JPanel(new FlowLayout());
		
		double windowSizeMin = 0.001;
		double windowSizeMax = 0.50d;
		
		double windowSizeActual = 0.05d; // default if analysis options are not present - e.g. a merge
		if(dataset.hasAnalysisOptions()){
			try {
				windowSizeActual = dataset.getAnalysisOptions().getProfileWindowProportion();
			} catch (MissingOptionException e) {
				warn("Error getting analyis options");
				stack(e.getMessage(), e);
			}
		}
		
		Dimension dim = new Dimension(80, 20);
		
		SpinnerNumberModel minSpinnerModel = new SpinnerNumberModel(windowSizeActual-0.02d,
				windowSizeMin,
				windowSizeMax,
				0.001d);
		windowSizeMinSpinner = new JSpinner(minSpinnerModel);
		windowSizeMinSpinner.setPreferredSize(dim);
//		JSpinner.NumberEditor numberEditorMin = new JSpinner.NumberEditor(windowSizeMinSpinner,"0.01");
//		windowSizeMinSpinner.setEditor(numberEditorMin);
		windowSizeMinSpinner.addChangeListener(this);
		windowSizeMinSpinner.setToolTipText("Minimum window size");
		
		SpinnerNumberModel maxSpinnerModel = new SpinnerNumberModel(windowSizeActual+0.02d,
				windowSizeMin,
				windowSizeMax,
				0.001d);
		windowSizeMaxSpinner = new JSpinner(maxSpinnerModel);
		windowSizeMaxSpinner.setPreferredSize(dim);
//		JSpinner.NumberEditor numberEditorMax = new JSpinner.NumberEditor(	windowSizeMaxSpinner,"0.01");
//		windowSizeMaxSpinner.setEditor(numberEditorMax);
		windowSizeMaxSpinner.addChangeListener(this);
		windowSizeMaxSpinner.setToolTipText("Maximum window size");
		
		SpinnerNumberModel stepSpinnerModel = new SpinnerNumberModel(0.01d,
				0.001d,
				0.50d,
				0.001d);
		stepSizeSpinner = new JSpinner(stepSpinnerModel);
		stepSizeSpinner.setPreferredSize(dim);
//		JSpinner.NumberEditor numberEditorStep = new JSpinner.NumberEditor(	stepSizeSpinner,"0.01");
//		stepSizeSpinner.setEditor(numberEditorStep);
		stepSizeSpinner.addChangeListener(this);
		stepSizeSpinner.setToolTipText("Step size");
		
		panel.add(new JLabel("Min:"));
		panel.add(windowSizeMinSpinner);
		panel.add(new JLabel("Max:"));
		panel.add(windowSizeMaxSpinner);
		panel.add(new JLabel("Step:"));
		panel.add(stepSizeSpinner);
		
		runButton = new JButton("Run");
		runButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {

				Thread thr = new Thread(){
					public void run(){
						
						try {
							runAnalysis();
						} catch (Exception e) {
							log(Level.SEVERE, "Error testing", e);
						}
					}
				};
				thr.start();
				
			
			}
		});	
		panel.add(runButton);
		
		return panel;
	}
	
	/**
	 * Toggle wait cursor on element
	 * @param b
	 */
	private void setAnalysing(boolean b){
		if(b){
			this.setEnabled(false);
			for(Component c : this.getComponents()){
				c.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)); //new Cursor(Cursor.WAIT_CURSOR));
			}
			
			this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			
		} else {
			this.setEnabled(true);
			for(Component c : this.getComponents()){
				c.setCursor(Cursor.getDefaultCursor());
			}
			this.setCursor(Cursor.getDefaultCursor());
		}
	}
	
	public void setEnabled(boolean b){
		runButton.setEnabled(b);
		windowSizeMinSpinner.setEnabled(b);
		windowSizeMaxSpinner.setEnabled(b);
		stepSizeSpinner.setEnabled(b);
	}
	
	private void runAnalysis() throws Exception {
		
		windowSizeMinSpinner.commitEdit();
		windowSizeMaxSpinner.commitEdit();
		stepSizeSpinner.commitEdit();
		
		double windowSizeMin  = (double) windowSizeMinSpinner.getValue();
		double windowSizeMax  = (double) windowSizeMaxSpinner.getValue(); 
		double stepSize       = (double) stepSizeSpinner.getValue(); 
		
		setAnalysing(true);
		
		// Clear the old chart
		chartPanel.setChart(MorphologyChartFactory.createEmptyChart());
		
		log("Testing "+windowSizeMin+" - "+windowSizeMax);
		
		for(double i=windowSizeMin; i<=windowSizeMax; i+=stepSize){
			
			// make a duplicate collection
			ICellCollection duplicateCollection = new DefaultCellCollection(dataset.getCollection(), "test");
			
			// put each cell into the new collection
			for(ICell c : dataset.getCollection().getCells()){
				
				ICell newCell = new DefaultCell(c);
				newCell.getNucleus().setWindowProportion(ProfileType.ANGLE, i); // triggers recalc of profile
				
				duplicateCollection.addCell(newCell);
			}

			// recalc the aggregate
			IProfileCollection pc = duplicateCollection.getProfileCollection();
			
			pc.createProfileAggregate(duplicateCollection, dataset.getCollection().getProfileCollection().length());
						
			for(Tag tag : dataset.getCollection().getProfileCollection().getBorderTags()){
				pc.addIndex(tag, dataset.getCollection().getProfileCollection().getIndex(tag));
			}
			
			
			// get the profile median
			IProfile median = pc.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Quartile.MEDIAN);

			// add to the chart
			updateChart(median, i);
			
			duplicateCollection = null;
		}
		setAnalysing(false);
		log("Profiling complete");
	}
	
	private void updateChart(IProfile profile, double windowSize){
		
		XYPlot plot = chartPanel.getChart().getXYPlot();
		int datasetCount = plot.getDatasetCount();
		
		DefaultXYDataset ds = new DefaultXYDataset();
		
        IProfile xpoints = profile.getPositions(100);
        double[][] data = { xpoints.asArray(), profile.asArray() };
        
        DecimalFormat df = new DecimalFormat("#0.000"); 
        
        ds.addSeries(df.format(windowSize), data);
        		
		for(int series=0;series<ds.getSeriesCount(); series++){
			XYLineAndShapeRenderer rend = new XYLineAndShapeRenderer();
			rend.setSeriesOutlinePaint(series, ColourSelecter.getColor(datasetCount));
			rend.setSeriesShapesVisible(series, false);
			rend.setSeriesLinesVisible(series, true);
			rend.setSeriesStroke(series, ChartComponents.MARKER_STROKE);
			rend.setSeriesPaint(series, chooseGradientColour(datasetCount));

			plot.setRenderer(datasetCount, rend);
		}
		
		plot.setDataset(datasetCount, ds);
		
	}
	
	private Color chooseGradientColour(int index){
		double windowSizeMin  = (double) windowSizeMinSpinner.getValue();
		double windowSizeMax  = (double) windowSizeMaxSpinner.getValue(); 
		double stepSize       = (double) stepSizeSpinner.getValue(); 
		int totalSteps = (int) Math.ceil(    ((windowSizeMax) - windowSizeMin) / stepSize);
		
		double proportion = (double) index / (double) totalSteps;
		
		int r = (int) ( 255d * proportion);
		int g = 20;
		int b = (int) (255d -  (255d * proportion));
		
		/*
		 * Validate ranges
		 */	
		r = r > 255 ? 255 : r < 0 ? 0 : r;
		g = g > 255 ? 255 : g < 0 ? 0 : g;
		b = b > 255 ? 255 : b < 0 ? 0 : b;
		
		
		Color result = new Color(r, g, b);
		return result;
		
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		
		try {
			
			double windowSizeMin  = (double) windowSizeMinSpinner.getValue();
			double windowSizeMax  = (double) windowSizeMaxSpinner.getValue(); 
			
			if(e.getSource()==windowSizeMinSpinner){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();

				if( windowSizeMin > windowSizeMax ){
					windowSizeMinSpinner.setValue( windowSizeMax );
				}

			}
			
			if(e.getSource()==windowSizeMaxSpinner){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();

				if( windowSizeMax < windowSizeMin ){
					windowSizeMaxSpinner.setValue( windowSizeMin );
				}

			}
			
			if(e.getSource()==stepSizeSpinner){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();

			}
			
		} catch (ParseException e1) {
			error("Error in spinners", e1);
		}
		
	}
}
