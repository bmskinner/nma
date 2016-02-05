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
package gui;

import gui.DatasetEvent.DatasetMethod;
import gui.components.ConsensusNucleusChartPanel;
import gui.tabs.DetailPanel;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import analysis.AnalysisDataset;
import charting.charts.ConsensusNucleusChartFactory;
import charting.options.ChartOptions;
import charting.options.TableOptions;
import components.CellCollection;
import components.generic.BorderTag;
import components.generic.XYPoint;
import components.nuclear.BorderPoint;
import components.nuclei.ConsensusNucleus;

public class ConsensusNucleusPanel extends DetailPanel implements SignalChangeListener {

	private static final long serialVersionUID = 1L;

	private ConsensusNucleusChartPanel consensusChartPanel;
	private JButton runRefoldingButton;
	
	private JPanel offsetsPanel; // store controls for rotating and translating
	private JPanel mainPanel;	
	
	public ConsensusNucleusPanel(Logger programLogger) {
		super(programLogger);
		this.setLayout(new BorderLayout());
		mainPanel = new JPanel();
		mainPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth  = 2;
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
				List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
				list.add(activeDataset());
				fireDatasetEvent(DatasetMethod.REFOLD_CONSENSUS, list);
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
				if(activeDataset().getCollection().hasConsensusNucleus()){
					activeDataset().getCollection().getConsensusNucleus().offset(0, 1);;
					
					update(activeDatasetToList());
				}
			}
		});
		panel.add(moveUp, constraints);
		
		JButton moveDown = new JButton("-y");
		moveDown.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				if(activeDataset().getCollection().hasConsensusNucleus()){
					activeDataset().getCollection().getConsensusNucleus().offset(0, -1);;
					
					update(activeDatasetToList());
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
				if(activeDataset().getCollection().hasConsensusNucleus()){
					activeDataset().getCollection().getConsensusNucleus().offset(-1, 0);;
					
					update(activeDatasetToList());
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
				if(activeDataset().getCollection().hasConsensusNucleus()){
					activeDataset().getCollection().getConsensusNucleus().offset(1, 0);;
					
					update(activeDatasetToList());
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
				if(activeDataset().getCollection().hasConsensusNucleus()){
					double x = 0;
					double y = 0;
					XYPoint point = new XYPoint(x, y);
					
					activeDataset().getCollection().getConsensusNucleus().moveCentreOfMass(point);;
					
					update(activeDatasetToList());
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
				if(activeDataset().getCollection().hasConsensusNucleus()){
					activeDataset().getCollection().getConsensusNucleus().rotate(-89);
					
					update(activeDatasetToList());
				}
			}
		});

		panel.add(rotateFwd, constraints);

		JButton rotateBck = new JButton("+r");
		rotateBck.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				if(activeDataset().getCollection().hasConsensusNucleus()){
					activeDataset().getCollection().getConsensusNucleus().rotate(-91);
					
					update(activeDatasetToList());
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
				if(activeDataset().getCollection().hasConsensusNucleus()){
					BorderPoint orientationPoint = activeDataset().getCollection().getConsensusNucleus().getBorderTag(BorderTag.ORIENTATION_POINT);
					activeDataset().getCollection().getConsensusNucleus().rotatePointToBottom(orientationPoint);
					
					update(activeDatasetToList());
				}
			}
		});
		constraints.gridx = 1;
		constraints.gridy = 0;
		panel.add(rotateRst, constraints);
		
		JButton refoldBtn = new JButton("Re-Refold");
		refoldBtn.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				
				fireDatasetEvent(DatasetMethod.REFOLD_CONSENSUS, activeDatasetToList());
			}
		});
		
		constraints.gridwidth = 3;
		constraints.gridheight = 1;
		constraints.gridx = 0;
		constraints.gridy = 2;
		panel.add(refoldBtn, constraints);
		
		return panel;
	}
	
	@Override
	protected void updateSingle() throws Exception {
		updateSingleDataset();
		programLogger.log(Level.FINEST, "Updated consensus panel");
	}
	

	@Override
	protected void updateMultiple() throws Exception {
		updateMultipleDatasets();
		programLogger.log(Level.FINEST, "Updated consensus panel");
	}
	
	@Override
	protected void updateNull() throws Exception {		
		updateBlankChart();
	}
	
	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) throws Exception {
		return null;
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options) throws Exception{
		return null;
	}
		
	private void updateSingleDataset() throws Exception {
		runRefoldingButton.setVisible(false);

		JFreeChart consensusChart = ConsensusNucleusChartFactory.makeEmptyNucleusOutlineChart();

		CellCollection collection = activeDataset().getCollection();
		if(collection.hasConsensusNucleus()){
			consensusChart = ConsensusNucleusChartFactory.makeSegmentedConsensusChart(activeDataset());

			// hide the refold button
			runRefoldingButton.setVisible(false);
			offsetsPanel.setVisible(true);
			consensusChartPanel.setChart(consensusChart);
			consensusChartPanel.restoreAutoBounds();
		} else {
			
			if(collection.isRefolding()){
				runRefoldingButton.setVisible(false);
			} else {
				runRefoldingButton.setVisible(true);
			}
			
			runRefoldingButton.setVisible(true);
			offsetsPanel.setVisible(false);
			consensusChartPanel.setChart(consensusChart);
		}
	}
	
	private void updateMultipleDatasets() throws Exception{
		boolean oneHasConsensus = false;
		for(AnalysisDataset d : getDatasets()){
			if (d.getCollection().hasConsensusNucleus()){
				oneHasConsensus= true;
			}
		}

		if(oneHasConsensus){
			JFreeChart chart = ConsensusNucleusChartFactory.makeMultipleConsensusChart(getDatasets());
			consensusChartPanel.setChart(chart);
			consensusChartPanel.restoreAutoBounds();
		} else {
			JFreeChart chart = ConsensusNucleusChartFactory.makeEmptyNucleusOutlineChart();
			consensusChartPanel.setChart(chart);
		}


		runRefoldingButton.setVisible(false);
		offsetsPanel.setVisible(false);
	}
	
	private void updateBlankChart(){
		JFreeChart consensusChart = ConsensusNucleusChartFactory.makeEmptyNucleusOutlineChart();
		consensusChartPanel.setChart(consensusChart);
		runRefoldingButton.setVisible(false);
		offsetsPanel.setVisible(false);
	}
	
	
	private void rotateConsensusNucleus(){
		if(activeDataset()!=null){

			if(activeDataset().getCollection().hasConsensusNucleus()){
				
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
					activeDataset().getCollection().getConsensusNucleus().rotate(angle-90);
					
					this.update(activeDatasetToList());
				}
			}
		} else {
			programLogger.log(Level.WARNING, "Cannot rotate: must have one dataset selected");
		}
	}
	
	private void resetConsensusNucleusRotationToOrientationPoint(){
		if(activeDataset()!=null){

			if(activeDataset().getCollection().hasConsensusNucleus()){

				BorderPoint orientationPoint = activeDataset()
						.getCollection()
						.getConsensusNucleus()
						.getBorderTag(BorderTag.ORIENTATION_POINT);
				
				activeDataset().getCollection()
				.getConsensusNucleus()
				.rotatePointToBottom(orientationPoint);
				
				this.update(activeDatasetToList());
			}
		} else {
			programLogger.log(Level.WARNING, "Cannot rotate: must have one dataset selected");
		}
	}
	
	
	private void alignConsensusAlongVerticalPoints(){
		if(activeDataset()!=null){

			if(activeDataset().getCollection().hasConsensusNucleus()){
				
				ConsensusNucleus nucleus = activeDataset().getCollection().getConsensusNucleus();
				
				if(nucleus.hasBorderTag(BorderTag.TOP_VERTICAL) && nucleus.hasBorderTag(BorderTag.BOTTOM_VERTICAL)){
					
					nucleus.alignPointsOnVertical(nucleus.getBorderTag(BorderTag.TOP_VERTICAL), nucleus.getBorderTag(BorderTag.BOTTOM_VERTICAL));
					
					this.update(activeDatasetToList());
					
				} else {
					programLogger.log(Level.WARNING, "Top and bottom vertical points are not available");
				}
				
			} else {
				programLogger.log(Level.WARNING, "No consensus nucleus available");
			}
		} else {
			programLogger.log(Level.WARNING, "Cannot align: must have one dataset selected");
		}
	}
	
	private void offsetConsensusNucleus(){
		if(activeDataset()!=null){

			if(activeDataset().getCollection().hasConsensusNucleus()){

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
					
					activeDataset().getCollection().getConsensusNucleus().offset(x, y);;
					this.update(activeDatasetToList());
				}

			}
		} else {
			programLogger.log(Level.WARNING, "Cannot offset: must have one dataset selected");
		}
	}
	
	private void resetConsensusNucleusOffset(){
		if(activeDataset()!=null){

			if(activeDataset().getCollection().hasConsensusNucleus()){

				double x = 0;
				double y = 0;
				XYPoint point = new XYPoint(x, y);
				
				activeDataset().getCollection().getConsensusNucleus().moveCentreOfMass(point);;
				this.update(activeDatasetToList());
			}
		} else {
			programLogger.log(Level.WARNING, "Cannot offset: must have one dataset selected");
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
				rotateConsensusNucleus();
			}
			
			if(event.type().equals("RotateReset")){
				resetConsensusNucleusRotationToOrientationPoint();
			}
			
			if(event.type().equals("AlignVertical")){
				alignConsensusAlongVerticalPoints();
			}
			
			if(event.type().equals("OffsetAction")){
				offsetConsensusNucleus();
			}
			
			if(event.type().equals("OffsetReset")){
				resetConsensusNucleusOffset();
			}

		}
		
	}
    
    

}
