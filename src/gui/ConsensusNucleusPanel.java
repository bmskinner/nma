/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
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
package gui;

import gui.components.ConsensusNucleusChartPanel;
import gui.tabs.DetailPanel;
import ij.IJ;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;

import charting.charts.ConsensusNucleusChartFactory;
import charting.charts.MorphologyChartFactory;
import charting.datasets.NucleusDatasetCreator;
import components.CellCollection;
import components.generic.XYPoint;
import components.nuclear.NucleusBorderPoint;
import analysis.AnalysisDataset;

public class ConsensusNucleusPanel extends DetailPanel implements SignalChangeListener {

	private static final long serialVersionUID = 1L;

	private ConsensusNucleusChartPanel consensusChartPanel;
	private JButton runRefoldingButton;
	
	private JPanel offsetsPanel; // store controls for rotating and translating
	private JPanel mainPanel;	
	
	private AnalysisDataset activeDataset;

	
	public ConsensusNucleusPanel() {

		this.setLayout(new BorderLayout());
		mainPanel = new JPanel();
		mainPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = 2;
		c.gridheight = 1;
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;      //reset to default
		c.weightx = 1.0;  
		c.weighty = 1.0; 
		
		JFreeChart consensusChart = ConsensusNucleusChartFactory.makeEmptyNucleusOutlineChart();
		
		consensusChartPanel = new ConsensusNucleusChartPanel(consensusChart);
		consensusChartPanel.addSignalChangeListener(this);
		
		runRefoldingButton = new JButton("Refold");

		runRefoldingButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				fireSignalChangeEvent("RefoldConsensus_"+activeDataset.getUUID().toString());
				runRefoldingButton.setVisible(false);
			}
		});
		runRefoldingButton.setVisible(false);
		
		consensusChartPanel.add(runRefoldingButton);
		
		
		this.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
//				resizePreview(consensusChartPanel, mainPanel);
				consensusChartPanel.restoreAutoBounds();
			}
		});
		
		mainPanel.add(consensusChartPanel, c);
		this.add(mainPanel, BorderLayout.CENTER);
		
		
		c.gridwidth = 1;
		c.gridheight = 1;
		c.gridx = 1;
		c.weightx = 0.3;  
		
		offsetsPanel = createOffsetsPanel();
		this.add(offsetsPanel, BorderLayout.EAST);
		offsetsPanel.setVisible(false);
	}
	
	private JPanel createOffsetsPanel(){
		JPanel panel = new JPanel(new BorderLayout());
		
		JPanel rotatePanel = createRotationPanel();
		panel.add(rotatePanel, BorderLayout.NORTH);
		
		JPanel offsetPanel = createTranslatePanel();
		panel.add(offsetPanel, BorderLayout.SOUTH);
		
		return panel;
	}
	
	private JPanel createTranslatePanel(){
		JPanel panel = new JPanel(new GridBagLayout());
		
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.BOTH;
		constraints.gridwidth = 1;
		constraints.gridheight = 1;
		constraints.gridx = 1;
		constraints.gridy = 0;
		constraints.anchor = GridBagConstraints.CENTER;
		
		JButton moveUp = new JButton("+y");
		moveUp.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				if(activeDataset.getCollection().hasConsensusNucleus()){
					activeDataset.getCollection().getConsensusNucleus().offset(0, 1);;
					List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
					list.add(activeDataset);
					update(list);
				}
			}
		});
		panel.add(moveUp, constraints);
		
		JButton moveDown = new JButton("-y");
		moveDown.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				if(activeDataset.getCollection().hasConsensusNucleus()){
					activeDataset.getCollection().getConsensusNucleus().offset(0, -1);;
					List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
					list.add(activeDataset);
					update(list);
				}
			}
		});
		constraints.gridx = 1;
		constraints.gridy = 2;
		panel.add(moveDown, constraints);
		
		JButton moveLeft = new JButton("-x");
		moveLeft.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				if(activeDataset.getCollection().hasConsensusNucleus()){
					activeDataset.getCollection().getConsensusNucleus().offset(-1, 0);;
					List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
					list.add(activeDataset);
					update(list);
				}
			}
		});
		constraints.gridx = 0;
		constraints.gridy = 1;
		panel.add(moveLeft, constraints);
		
		JButton moveright = new JButton("+x");
		moveright.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				if(activeDataset.getCollection().hasConsensusNucleus()){
					activeDataset.getCollection().getConsensusNucleus().offset(1, 0);;
					List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
					list.add(activeDataset);
					update(list);
				}
			}
		});
		constraints.gridx = 2;
		constraints.gridy = 1;
		panel.add(moveright, constraints);
		
		JButton moveRst = new JButton("!");
		moveRst.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				if(activeDataset.getCollection().hasConsensusNucleus()){
					double x = 0;
					double y = 0;
					XYPoint point = new XYPoint(x, y);
					
					activeDataset.getCollection().getConsensusNucleus().moveCentreOfMass(point);;
					List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
					list.add(activeDataset);
					update(list);
				}
			}
		});
		constraints.gridx = 1;
		constraints.gridy = 1;
		panel.add(moveRst, constraints);
		
		return panel;
	}
	
	private JPanel createRotationPanel(){
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.BOTH;
		constraints.gridwidth = 1;
		constraints.gridheight = 1;
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.anchor = GridBagConstraints.CENTER;
		
		JButton rotateFwd = new JButton("-r");
		rotateFwd.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				if(activeDataset.getCollection().hasConsensusNucleus()){
					activeDataset.getCollection().getConsensusNucleus().rotate(-89);
					List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
					list.add(activeDataset);
					update(list);
				}
			}
		});

		panel.add(rotateFwd, constraints);

		JButton rotateBck = new JButton("+r");
		rotateBck.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				if(activeDataset.getCollection().hasConsensusNucleus()){
					activeDataset.getCollection().getConsensusNucleus().rotate(-91);
					List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
					list.add(activeDataset);
					update(list);
				}
			}
		});
		constraints.gridx = 2;
		constraints.gridy = 0;
		panel.add(rotateBck, constraints);

		JButton rotateRst = new JButton("!");
		rotateRst.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				if(activeDataset.getCollection().hasConsensusNucleus()){
					NucleusBorderPoint orientationPoint = activeDataset.getCollection().getConsensusNucleus().getBorderTag(activeDataset.getCollection().getOrientationPoint());
					activeDataset.getCollection().getConsensusNucleus().rotatePointToBottom(orientationPoint);
					List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
					list.add(activeDataset);
					update(list);
				}
			}
		});
		constraints.gridx = 1;
		constraints.gridy = 0;
		panel.add(rotateRst, constraints);

		return panel;
	}
	
	/**
	 * Update the consensus nucleus panel with data from the given datasets. Produces a blank
	 * chart if no refolded nuclei are present
	 * @param list the datasets
	 */	
	public void update(List<AnalysisDataset> list){
		activeDataset = null;
		try {
			if(!list.isEmpty()){
				
				CellCollection collection = list.get(0).getCollection();

				if(list.size()==1){
					activeDataset = list.get(0);
					runRefoldingButton.setVisible(false);
					
					JFreeChart consensusChart = ConsensusNucleusChartFactory.makeEmptyNucleusOutlineChart();
					
					if(collection.hasConsensusNucleus()){
						consensusChart = ConsensusNucleusChartFactory.makeSegmentedConsensusChart(activeDataset);
						
						// hide the refold button
						runRefoldingButton.setVisible(false);
						offsetsPanel.setVisible(true);
						consensusChartPanel.setChart(consensusChart);
						consensusChartPanel.restoreAutoBounds();
					} else {
						runRefoldingButton.setVisible(true);
						offsetsPanel.setVisible(false);
						consensusChartPanel.setChart(consensusChart);
					}

				}else {
					
					// multiple datasets
					
					boolean oneHasConsensus = false;
					for(AnalysisDataset d : list){
						if (d.getCollection().hasConsensusNucleus()){
							oneHasConsensus= true;
						}
					}
					
					if(oneHasConsensus){
						JFreeChart chart = ConsensusNucleusChartFactory.makeMultipleConsensusChart(list);
						consensusChartPanel.setChart(chart);
						consensusChartPanel.restoreAutoBounds();
					} else {
						JFreeChart chart = ConsensusNucleusChartFactory.makeEmptyNucleusOutlineChart();
						consensusChartPanel.setChart(chart);
					}
					
					
					runRefoldingButton.setVisible(false);
					offsetsPanel.setVisible(false);
				}

			} else { // no datasets in the list
				
				JFreeChart consensusChart = ConsensusNucleusChartFactory.makeEmptyNucleusOutlineChart();
				consensusChartPanel.setChart(consensusChart);
				runRefoldingButton.setVisible(false);
				offsetsPanel.setVisible(false);
				
			}
		} catch (Exception e) {
			error("Error drawing consensus nucleus", e);
		}
	}
				
	@Override
	public void signalChangeReceived(SignalChangeEvent event) {
		
		// pass on log messages back to the main window
		if(event.sourceName().equals(ConsensusNucleusChartPanel.SOURCE_COMPONENT)){
			if(event.type().startsWith("Log_")){
				fireSignalChangeEvent(event.type());
			}
			
			if(event.type().equals("RotateConsensus")){
				if(activeDataset!=null){

					if(activeDataset.getCollection().hasConsensusNucleus()){
						
						SpinnerNumberModel sModel = new SpinnerNumberModel(0, -360, 360, 1.0);
						JSpinner spinner = new JSpinner(sModel);
						
						int option = JOptionPane.showOptionDialog(null, 
								spinner, 
								"Choose the amount to rotate", 
								JOptionPane.OK_CANCEL_OPTION, 
								JOptionPane.QUESTION_MESSAGE, null, null, null);
						if (option == JOptionPane.CANCEL_OPTION) {
						    // user hit cancel
						} else if (option == JOptionPane.OK_OPTION)	{
							
							// offset by 90 because reasons?
							double angle = (Double) spinner.getModel().getValue();
							activeDataset.getCollection().getConsensusNucleus().rotate(angle-90);
							List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
							list.add(activeDataset);
							this.update(list);
						}
					}
				} else {
					log("Cannot rotate: must have one dataset selected");
				}
			}
			
			if(event.type().equals("RotateReset")){
				if(activeDataset!=null){

					if(activeDataset.getCollection().hasConsensusNucleus()){

						NucleusBorderPoint orientationPoint = activeDataset.getCollection().getConsensusNucleus().getBorderTag(activeDataset.getCollection().getOrientationPoint());
						activeDataset.getCollection().getConsensusNucleus().rotatePointToBottom(orientationPoint);
						List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
						list.add(activeDataset);
						this.update(list);
					}
				} else {
					log("Cannot rotate: must have one dataset selected");
				}
			}
			
			if(event.type().equals("OffsetAction")){
				if(activeDataset!=null){

					if(activeDataset.getCollection().hasConsensusNucleus()){

						// get the x and y offset
						SpinnerNumberModel xModel = new SpinnerNumberModel(0, -100, 100, 0.1);
						SpinnerNumberModel yModel = new SpinnerNumberModel(0, -100, 100, 0.1);
				        
						JSpinner xSpinner = new JSpinner(xModel);
						JSpinner ySpinner = new JSpinner(yModel);
						
						JSpinner[] spinners = { xSpinner, ySpinner };
						
						int option = JOptionPane.showOptionDialog(null, 
								spinners, 
								"Choose the amount to offset x and y", 
								JOptionPane.OK_CANCEL_OPTION, 
								JOptionPane.QUESTION_MESSAGE, null, null, null);
						if (option == JOptionPane.CANCEL_OPTION) {
						    // user hit cancel
						} else if (option == JOptionPane.OK_OPTION)	{
							
							// offset by 90 because reasons?
							double x = (Double) xSpinner.getModel().getValue();
							double y = (Double) ySpinner.getModel().getValue();
							
							activeDataset.getCollection().getConsensusNucleus().offset(x, y);;
							List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
							list.add(activeDataset);
							this.update(list);
						}

					}
				} else {
					log("Cannot offset: must have one dataset selected");
				}
			}
			
			if(event.type().equals("OffsetReset")){
				if(activeDataset!=null){

					if(activeDataset.getCollection().hasConsensusNucleus()){

						double x = 0;
						double y = 0;
						XYPoint point = new XYPoint(x, y);
						
						activeDataset.getCollection().getConsensusNucleus().moveCentreOfMass(point);;
						List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
						list.add(activeDataset);
						this.update(list);
					}
				} else {
					log("Cannot offset: must have one dataset selected");
				}
			}

		}
		
	}
    
    

}
