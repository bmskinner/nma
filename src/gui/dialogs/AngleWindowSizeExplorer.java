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

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

import gui.LoadingIconDialog;
import gui.components.ColourSelecter;
import gui.components.ExportableChartPanel;
import gui.tabs.DetailPanel;

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

import utility.Constants;
import components.Cell;
import components.CellCollection;
import components.generic.BorderTag;
import components.generic.Profile;
import components.generic.ProfileCollection;
import components.generic.ProfileType;
import charting.ChartComponents;
import charting.charts.MorphologyChartFactory;
import analysis.AnalysisDataset;

@SuppressWarnings("serial")
public class AngleWindowSizeExplorer  extends LoadingIconDialog implements ChangeListener {
	
	private AnalysisDataset dataset;
	private ExportableChartPanel chartPanel;
	
	private JSpinner windowSizeMinSpinner;
	private JSpinner windowSizeMaxSpinner;
	private JSpinner stepSizeSpinner;
	
	private JButton  runButton;
	
	public AngleWindowSizeExplorer(final AnalysisDataset dataset, final Logger logger){
		super(logger);
		this.dataset = dataset;
		createUI();
		this.setModal(false);
		this.pack();
		this.setVisible(true);
	}
	
	private void createUI(){
		this.setTitle("Angle window size explorer: "+dataset.getName());
		this.setLayout(new BorderLayout());
		this.setLocationRelativeTo(null);
		
		this.add(createSettingsPanel(), BorderLayout.NORTH);
		
		chartPanel = new ExportableChartPanel(MorphologyChartFactory.makeEmptyProfileChart(ProfileType.REGULAR));
		this.add(chartPanel, BorderLayout.CENTER);

		
	}
	
	private JPanel createSettingsPanel(){
		JPanel panel = new JPanel(new FlowLayout());
		
		int windowSizeMin = 1;
		int windowSizeMax = (int) dataset.getCollection().getMedianArrayLength();
		int windowSizeActual = dataset.getAnalysisOptions().getAngleProfileWindowSize();
		
		SpinnerNumberModel minSpinnerModel = new SpinnerNumberModel(windowSizeActual-2,
				windowSizeMin,
				windowSizeMax,
				1);
		windowSizeMinSpinner = new JSpinner(minSpinnerModel);
		windowSizeMinSpinner.addChangeListener(this);
		windowSizeMinSpinner.setToolTipText("Minimum window size");
		
		SpinnerNumberModel maxSpinnerModel = new SpinnerNumberModel(windowSizeActual+2,
				windowSizeMin,
				windowSizeMax,
				1);
		windowSizeMaxSpinner = new JSpinner(maxSpinnerModel);
		windowSizeMaxSpinner.addChangeListener(this);
		windowSizeMaxSpinner.setToolTipText("Maximum window size");
		
		SpinnerNumberModel stepSpinnerModel = new SpinnerNumberModel(1,
				1,
				100,
				1);
		stepSizeSpinner = new JSpinner(stepSpinnerModel);
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
							programLogger.log(Level.SEVERE, "Error testing", e);
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
		int windowSizeMin  = (Integer) windowSizeMinSpinner.getValue();
		int windowSizeMax  = (Integer) windowSizeMaxSpinner.getValue(); 
		int stepSize       = (Integer) stepSizeSpinner.getValue(); 
		
		setAnalysing(true);
		
		// Clear the old chart
		chartPanel.setChart(MorphologyChartFactory.makeEmptyProfileChart(ProfileType.REGULAR));
		
		programLogger.log(Level.INFO, "Testing "+windowSizeMin+" - "+windowSizeMax);
		
		for(int i=windowSizeMin; i<=windowSizeMax; i+=stepSize){
			
			// make a duplicate collection
//			programLogger.log(Level.INFO, "\t"+i);
			
			CellCollection duplicateCollection = new CellCollection(dataset.getCollection(), "test");
			
			// put each cell into the new collection
			for(Cell c : dataset.getCollection().getCells()){
				
				Cell newCell = new Cell(c);
				newCell.getNucleus().setAngleProfileWindowSize(i);
				
				// recalc the profiles
				newCell.getNucleus().calculateProfiles();
				
				duplicateCollection.addCell(newCell);
			}
//			programLogger.log(Level.INFO, "\tMade collection");
			// recalc the aggregate
			
			ProfileCollection pc = duplicateCollection.getProfileCollection(ProfileType.REGULAR);
			
			pc.createProfileAggregate(duplicateCollection, ProfileType.REGULAR);
			
//			programLogger.log(Level.INFO, "\tCalculated aggregate");
			
			for(BorderTag tag : dataset.getCollection().getProfileCollection(ProfileType.REGULAR).getOffsetKeys()){
				pc.addOffset(tag, dataset.getCollection().getProfileCollection(ProfileType.REGULAR).getOffset(tag));
			}
			
//			programLogger.log(Level.INFO, "\tAdded offsets");
			
			// get the profile median
			
			Profile median = pc.getProfile(BorderTag.REFERENCE_POINT, Constants.MEDIAN);
			
//			programLogger.log(Level.INFO, "\tMade median");
			// add to the chart
			updateChart(median, i);
//			programLogger.log(Level.INFO, "\tUpdated chart");
			
			duplicateCollection = null;
		}
		setAnalysing(false);
		programLogger.log(Level.INFO, "Profiling complete");
	}
	
	private void updateChart(Profile profile, int windowSize){
		
		XYPlot plot = chartPanel.getChart().getXYPlot();
		int datasetCount = plot.getDatasetCount();
		
		DefaultXYDataset ds = new DefaultXYDataset();
		
        Profile xpoints = profile.getPositions(100);
        double[][] data = { xpoints.asArray(), profile.asArray() };
        ds.addSeries(windowSize, data);
        		
		for(int series=0;series<ds.getSeriesCount(); series++){
			XYLineAndShapeRenderer rend = new XYLineAndShapeRenderer();
			rend.setSeriesOutlinePaint(series, ColourSelecter.getSegmentColor(datasetCount));
			rend.setSeriesShapesVisible(series, false);
			rend.setSeriesLinesVisible(series, true);
			rend.setSeriesStroke(series, ChartComponents.MARKER_STROKE);
			rend.setSeriesPaint(series, chooseGradientColour(datasetCount));
//			rend.setSeriesOutlineStroke(series, new BasicStroke(4f));
			plot.setRenderer(datasetCount, rend);
		}
		
		plot.setDataset(datasetCount, ds);
		
	}
	
	private Color chooseGradientColour(int index){
		int windowSizeMin  = (Integer) windowSizeMinSpinner.getValue();
		int windowSizeMax  = (Integer) windowSizeMaxSpinner.getValue(); 
		int stepSize       = (Integer) stepSizeSpinner.getValue(); 
		int totalSteps = (int) Math.ceil(    ((windowSizeMax+1) - windowSizeMin) / stepSize);
		
		double proportion = (double) index / (double) totalSteps;
		
		int r = (int) ( 255d * proportion);
		int g = 20;
		int b = (int) (255d -  (255d * proportion));
		
		Color result = new Color(r, g, b);
		return result;
		
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		
		try {
			
			int windowSizeMin  = (Integer) windowSizeMinSpinner.getValue();
			int windowSizeMax  = (Integer) windowSizeMaxSpinner.getValue(); 
			
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
			programLogger.log(Level.SEVERE, "Error in spinners", e1);
		}
		
	}
}
