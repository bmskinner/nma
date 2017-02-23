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
package com.bmskinner.nuclear_morphology.gui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.logging.Level;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.charting.charts.AbstractChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.ConsensusNucleusChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ConsensusNucleusChartPanel;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.FloatPoint;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;

@SuppressWarnings("serial")
public class ConsensusNucleusPanel extends DetailPanel implements ChangeListener {

	private ConsensusNucleusChartPanel consensusChartPanel;
	private JButton runRefoldingButton;
	
	private JPanel offsetsPanel; // store controls for rotating and translating
	
	// Debugging tools for the nucleus mesh - not visible in the final panel
	private JCheckBox showMeshBox;
	private JCheckBox showMeshEdgesBox;
	private JCheckBox showMeshFacesBox;
	private JCheckBox straightenMeshBox;
	private JSpinner  meshSizeSpinner;
	
	public ConsensusNucleusPanel() {
		super();
		this.setLayout(new BorderLayout());
		
		ChartOptions options = new ChartOptionsBuilder()
			.setScale(GlobalOptions.getInstance().getScale())
			.setSwatch(GlobalOptions.getInstance().getSwatch())
			.setShowXAxis(false)
			.setShowYAxis(false)
			.build();
		
		JFreeChart consensusChart = getChart(options);
		consensusChartPanel = new ConsensusNucleusChartPanel(consensusChart);
		consensusChartPanel.addSignalChangeListener(this);

		runRefoldingButton = new JButton("Refold");
		
		runRefoldingButton.addActionListener( e -> {
			fine("Heard refold button clicked");
			fireDatasetEvent(DatasetEvent.REFOLD_CONSENSUS, getDatasets());
			runRefoldingButton.setVisible(false);
		});

		runRefoldingButton.setVisible(false);
		
		consensusChartPanel.add(runRefoldingButton);

		add(consensusChartPanel, BorderLayout.CENTER);

		offsetsPanel = createOffsetsPanel();
		add(offsetsPanel, BorderLayout.EAST);
		offsetsPanel.setVisible(false);
	}
	
	@Override
	public void setChartsAndTablesLoading(){
		consensusChartPanel.setChart(AbstractChartFactory.createLoadingChart());
	}
	
	/**
	 * Force the chart panel to restore bounds
	 */
	public void restoreAutoBounds(){
		consensusChartPanel.restoreAutoBounds();
	}
	
	private JPanel createOffsetsPanel(){
		JPanel panel = new JPanel(new BorderLayout());
		
		JPanel rotatePanel = createRotationPanel();
		panel.add(rotatePanel, BorderLayout.NORTH);
		
		/*
		 * Used for debugging only - do not include in releases
		 */
		JPanel meshPanel = createMeshPanel();
//		panel.add(meshPanel, BorderLayout.CENTER);
		
		
		JPanel offsetPanel = createTranslatePanel();
		panel.add(offsetPanel, BorderLayout.SOUTH);
		
		return panel;
	}
			
	private JPanel createMeshPanel(){
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		
		showMeshBox = new JCheckBox("Show mesh");
		showMeshBox.setSelected(false);
		showMeshBox.addChangeListener( this);
		
		SpinnerNumberModel meshSizeModel = new SpinnerNumberModel(10,
				2,
				100,
				1);
		meshSizeSpinner = new JSpinner(meshSizeModel);
		meshSizeSpinner.addChangeListener(this);
		meshSizeSpinner.setToolTipText("Mesh size");
		JSpinner.NumberEditor meshNumberEditor = new JSpinner.NumberEditor(meshSizeSpinner,"0");
		meshSizeSpinner.setEditor(meshNumberEditor);

		
		showMeshEdgesBox = new JCheckBox("Mesh edges");
		showMeshEdgesBox.setSelected(true);
		showMeshEdgesBox.setEnabled(false);
		showMeshEdgesBox.addChangeListener( this);
		
		showMeshFacesBox = new JCheckBox("Mesh faces");
		showMeshFacesBox.setSelected(false);
		showMeshFacesBox.setEnabled(false);
		showMeshFacesBox.addChangeListener( this);
		
		straightenMeshBox = new JCheckBox("Straighten mesh");
		straightenMeshBox.setSelected(false);
		straightenMeshBox.setEnabled(false);
		straightenMeshBox.addChangeListener( this);
		

		
		panel.add(showMeshBox);
		panel.add(meshSizeSpinner);
		panel.add(showMeshEdgesBox);
		panel.add(showMeshFacesBox);
		panel.add(straightenMeshBox);
		
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
		moveUp.setToolTipText("Move centre of mass y+1");
		
		moveUp.addActionListener( e -> {
			if(activeDataset().getCollection().hasConsensus()){
				activeDataset().getCollection().getConsensus().offset(0, 1);;
				refreshChartCache(getDatasets());
			}
		});
		panel.add(moveUp, constraints);
		
		JButton moveDown = new JButton("-y");
		moveDown.setToolTipText("Move centre of mass y-1");
		moveDown.addActionListener( e -> {
			if(activeDataset().getCollection().hasConsensus()){
				activeDataset().getCollection().getConsensus().offset(0, -1);;
				refreshChartCache(getDatasets());
			}
		});

		constraints.gridx = 1;
		constraints.gridy = 2;
		panel.add(moveDown, constraints);
		
		JButton moveLeft = new JButton("-x");
		moveLeft.setToolTipText("Move centre of mass x-1");
		moveLeft.addActionListener( e -> {
			if(activeDataset().getCollection().hasConsensus()){
				activeDataset().getCollection().getConsensus().offset(-1, 0);;
				refreshChartCache(getDatasets());
			}
		});

		constraints.gridx = 0;
		constraints.gridy = 1;
		panel.add(moveLeft, constraints);
		
		JButton moveRight = new JButton("+x");
		moveRight.setToolTipText("Move centre of mass x+1");
		moveRight.addActionListener( e -> {
			if(activeDataset().getCollection().hasConsensus()){
				activeDataset().getCollection().getConsensus().offset(1, 0);;
				refreshChartCache(getDatasets());
			}
		});

		constraints.gridx = 2;
		constraints.gridy = 1;
		panel.add(moveRight, constraints);
		
		JButton moveRst = new JButton("!");
		moveRst.setToolTipText("Reset centre of mass to 0,0");
		moveRst.addActionListener( e -> {
			if(activeDataset().getCollection().hasConsensus()){
				double x = 0;
				double y = 0;
				IPoint point = IPoint.makeNew(x, y);
				
				activeDataset().getCollection().getConsensus().moveCentreOfMass(point);;
				refreshChartCache(getDatasets());
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
		rotateFwd.setToolTipText("Rotate anti-clockwise 1 degree");
		rotateFwd.addActionListener( e -> {
			if(activeDataset().getCollection().hasConsensus()){
				activeDataset().getCollection().getConsensus().rotate(-89);
				refreshChartCache(getDatasets());

			}
		});


		panel.add(rotateFwd, constraints);

		JButton rotateBck = new JButton("+r");
		rotateBck.setToolTipText("Rotate clockwise 1 degree");
		
		rotateBck.addActionListener( e -> {
			if(activeDataset().getCollection().hasConsensus()){
				activeDataset().getCollection().getConsensus().rotate(-91);
				refreshChartCache(getDatasets());
			}
		});

		
		constraints.gridx = 2;
		constraints.gridy = 0;
		panel.add(rotateBck, constraints);

		JButton rotateRst = new JButton("!");
		rotateRst.setToolTipText("Reset rotation to orientation point");
		
		rotateRst.addActionListener( e -> {
			if(activeDataset().getCollection().hasConsensus()){
				IBorderPoint orientationPoint;
				try {
					orientationPoint = activeDataset().getCollection()
							.getConsensus()
							.getBorderTag(Tag.ORIENTATION_POINT);
					activeDataset().getCollection().getConsensus().rotatePointToBottom(orientationPoint);
				} catch (UnavailableBorderTagException e1) {
					stack("Cannot get OP index in nucleus profile", e1);
				}
				
				refreshChartCache(getDatasets());
			}
		});

		constraints.gridx = 1;
		constraints.gridy = 0;
		panel.add(rotateRst, constraints);
		
		JButton refoldBtn = new JButton("Re-Refold");
		refoldBtn.addActionListener( e -> {
			fireDatasetEvent(DatasetEvent.REFOLD_CONSENSUS, activeDatasetToList());
		});
		
		constraints.gridwidth = 3;
		constraints.gridheight = 1;
		constraints.gridx = 0;
		constraints.gridy = 2;
		panel.add(refoldBtn, constraints);
		
		return panel;
	}
	
	@Override
	protected void updateSingle() {
		super.updateSingle();
		updateSingleDataset();
		finest("Updated consensus panel");
	}
	

	@Override
	protected void updateMultiple() {
		updateMultipleDatasets();
		finest("Updated consensus panel");
	}
	
	@Override
	protected void updateNull() {		
		updateBlankChart();
	}
	
	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) {
			return new ConsensusNucleusChartFactory(options).makeConsensusChart();
	}

		
	private void updateSingleDataset() {
		runRefoldingButton.setVisible(false);
		
		showMeshEdgesBox.setEnabled(showMeshBox.isSelected());
		showMeshFacesBox.setEnabled(showMeshBox.isSelected());
		straightenMeshBox.setEnabled(showMeshBox.isSelected());

		ChartOptions options = new ChartOptionsBuilder()
			.setDatasets(getDatasets())
			.setScale(GlobalOptions.getInstance().getScale())
			.setSwatch(GlobalOptions.getInstance().getSwatch())
			.setShowMesh(showMeshBox.isSelected())
			.setShowMeshEdges(showMeshEdgesBox.isSelected())
			.setShowMeshFaces(showMeshFacesBox.isSelected())
			.setStraightenMesh(straightenMeshBox.isSelected())
			.setShowAnnotations(false)
			.setShowXAxis(false)
			.setShowYAxis(false)
			.setTarget(consensusChartPanel)
			.build();
		
		setChart(options);
		


		ICellCollection collection = activeDataset().getCollection();
		if(collection.hasConsensus()){

			// hide the refold button
			runRefoldingButton.setVisible(false);
			offsetsPanel.setVisible(true);
			consensusChartPanel.restoreAutoBounds();
						
		} else {

			runRefoldingButton.setVisible( !collection.isRefolding() );
			offsetsPanel.setVisible(false);
		}
	}
	
	private void updateMultipleDatasets() {
		
		ChartOptions options = new ChartOptionsBuilder()
			.setDatasets(getDatasets())
			.setScale(GlobalOptions.getInstance().getScale())
			.setSwatch(GlobalOptions.getInstance().getSwatch())
			.setTarget(consensusChartPanel)
			.build();

		setChart(options);

		runRefoldingButton.setVisible(false);
		offsetsPanel.setVisible(false);
	}
	
	private void updateBlankChart() {
		
		consensusChartPanel.setChart(ConsensusNucleusChartFactory.makeEmptyChart());
		runRefoldingButton.setVisible(false);
		offsetsPanel.setVisible(false);
		consensusChartPanel.restoreAutoBounds();
	}
	
	
	private void rotateConsensusNucleus(){
		if(activeDataset()!=null){

			if(activeDataset().getCollection().hasConsensus()){
				
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
					activeDataset().getCollection().getConsensus().rotate(angle-90);
					
					this.update(activeDatasetToList());
				}
			}
		} else {
			log(Level.WARNING, "Cannot rotate: must have one dataset selected");
		}
	}
	
	private void resetConsensusNucleusRotationToOrientationPoint(){
		if(activeDataset()!=null){

			if(activeDataset().getCollection().hasConsensus()){

				IBorderPoint orientationPoint;
				try {
					orientationPoint = activeDataset()
							.getCollection()
							.getConsensus()
							.getBorderTag(Tag.ORIENTATION_POINT);
					
					activeDataset().getCollection()
					.getConsensus()
					.rotatePointToBottom(orientationPoint);
					
				} catch (UnavailableBorderTagException e) {
					fine("Cannot get OP index in nucleus profile", e);
				}
				
				
				
				this.update(activeDatasetToList());
			}
		} else {
			log(Level.WARNING, "Cannot rotate: must have one dataset selected");
		}
	}
	
	
	private void alignConsensusAlongVerticalPoints(){
		if(activeDataset()!=null){

			if(activeDataset().getCollection().hasConsensus()){
				
				Nucleus nucleus = activeDataset().getCollection().getConsensus();
				
				if(nucleus.hasBorderTag(Tag.TOP_VERTICAL) && nucleus.hasBorderTag(Tag.BOTTOM_VERTICAL)){
					
					try {
						nucleus.alignPointsOnVertical(nucleus.getBorderTag(Tag.TOP_VERTICAL), nucleus.getBorderTag(Tag.BOTTOM_VERTICAL));
					} catch (UnavailableBorderTagException e) {
						fine("Cannot align points on vertical", e);
					}
					
					this.update(activeDatasetToList());
					
				} else {
					log(Level.WARNING, "Top and bottom vertical points are not available");
				}
				
			} else {
				log(Level.WARNING, "No consensus nucleus available");
			}
		} else {
			log(Level.WARNING, "Cannot align: must have one dataset selected");
		}
	}
	
	private void offsetConsensusNucleus(){
		if(activeDataset()!=null){

			if(activeDataset().getCollection().hasConsensus()){

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
					
					activeDataset().getCollection().getConsensus().offset(x, y);;
					this.update(activeDatasetToList());
				}

			}
		} else {
			log(Level.WARNING, "Cannot offset: must have one dataset selected");
		}
	}
	
	private void resetConsensusNucleusOffset(){
		if(activeDataset()!=null){

			if(activeDataset().getCollection().hasConsensus()){

				double x = 0;
				double y = 0;
				IPoint point = IPoint.makeNew(x, y);
				
				activeDataset().getCollection().getConsensus().moveCentreOfMass(point);;
				this.update(activeDatasetToList());
			}
		} else {
			log(Level.WARNING, "Cannot offset: must have one dataset selected");
		}
	}
	
	@Override
	public void signalChangeReceived(SignalChangeEvent event) {
		
		// pass on log messages back to the main window
		if(event.sourceName().equals(ConsensusNucleusChartPanel.SOURCE_COMPONENT)){
			
			this.clearChartCache(getDatasets());
			
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
			this.update(getDatasets());
			
		}
		
	}

	@Override
	public void stateChanged(ChangeEvent arg0) {
				
		this.update(getDatasets());
	}
    
    

}
