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
import gui.components.panels.MeasurementUnitSettingsPanel;
import gui.tabs.DetailPanel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import charting.charts.ConsensusNucleusChartFactory;
import charting.charts.ConsensusNucleusChartPanel;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import charting.options.TableOptions;
import components.CellCollection;
import components.generic.BorderTag;
import components.generic.BorderTagObject;
import components.generic.XYPoint;
import components.nuclear.BorderPoint;
import components.nuclei.ConsensusNucleus;

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
			.build();
		
		JFreeChart consensusChart = getChart(options);
		consensusChartPanel = new ConsensusNucleusChartPanel(consensusChart);
		consensusChartPanel.addSignalChangeListener(this);

		runRefoldingButton = new JButton("Refold");

		
		runRefoldingButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				fireDatasetEvent(DatasetMethod.REFOLD_CONSENSUS, getDatasets());
				runRefoldingButton.setVisible(false);
			}
		});
		

		runRefoldingButton.setVisible(false);
		
		consensusChartPanel.add(runRefoldingButton);
		
		
		
		this.add(consensusChartPanel, BorderLayout.CENTER);
//		
//		JPanel headerPanel = createHeaderPanel();
//		this.add(headerPanel, BorderLayout.NORTH);
		

		offsetsPanel = createOffsetsPanel();
		this.add(offsetsPanel, BorderLayout.EAST);
		offsetsPanel.setVisible(false);
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
		
		JPanel meshPanel = createMeshPanel();
		panel.add(meshPanel, BorderLayout.CENTER);
		
		
		JPanel offsetPanel = createTranslatePanel();
		panel.add(offsetPanel, BorderLayout.SOUTH);
		
		return panel;
	}
	
	private JPanel createHeaderPanel(){
		JPanel panel = new JPanel(new FlowLayout());		
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
		moveUp.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				if(activeDataset().getCollection().hasConsensusNucleus()){
					activeDataset().getCollection().getConsensusNucleus().offset(0, 1);;
					refreshChartCache(getDatasets());
//					update(activeDatasetToList());
				}
			}
		});
		panel.add(moveUp, constraints);
		
		JButton moveDown = new JButton("-y");
		moveDown.setToolTipText("Move centre of mass y-1");
		moveDown.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				if(activeDataset().getCollection().hasConsensusNucleus()){
					activeDataset().getCollection().getConsensusNucleus().offset(0, -1);;
					refreshChartCache(getDatasets());
				}
			}
		});
		constraints.gridx = 1;
		constraints.gridy = 2;
		panel.add(moveDown, constraints);
		
		JButton moveLeft = new JButton("-x");
		moveLeft.setToolTipText("Move centre of mass x-1");
		moveLeft.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				if(activeDataset().getCollection().hasConsensusNucleus()){
					activeDataset().getCollection().getConsensusNucleus().offset(-1, 0);;
					refreshChartCache(getDatasets());
				}
			}
		});
		constraints.gridx = 0;
		constraints.gridy = 1;
		panel.add(moveLeft, constraints);
		
		JButton moveright = new JButton("+x");
		moveright.setToolTipText("Move centre of mass x+1");
		moveright.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				if(activeDataset().getCollection().hasConsensusNucleus()){
					activeDataset().getCollection().getConsensusNucleus().offset(1, 0);;
					refreshChartCache(getDatasets());
//					update(activeDatasetToList());
				}
			}
		});
		constraints.gridx = 2;
		constraints.gridy = 1;
		panel.add(moveright, constraints);
		
		JButton moveRst = new JButton("!");
		moveRst.setToolTipText("Reset centre of mass to 0,0");
		moveRst.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				if(activeDataset().getCollection().hasConsensusNucleus()){
					double x = 0;
					double y = 0;
					XYPoint point = new XYPoint(x, y);
					
					activeDataset().getCollection().getConsensusNucleus().moveCentreOfMass(point);;
					refreshChartCache(getDatasets());
//					update(activeDatasetToList());
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
		rotateFwd.setToolTipText("Rotate anti-clockwise 1 degree");
		rotateFwd.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				if(activeDataset().getCollection().hasConsensusNucleus()){
					activeDataset().getCollection().getConsensusNucleus().rotate(-89);
					refreshChartCache(getDatasets());
//					update(activeDatasetToList());
				}
			}
		});

		panel.add(rotateFwd, constraints);

		JButton rotateBck = new JButton("+r");
		rotateBck.setToolTipText("Rotate clockwise 1 degree");
		rotateBck.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				if(activeDataset().getCollection().hasConsensusNucleus()){
					activeDataset().getCollection().getConsensusNucleus().rotate(-91);
					refreshChartCache(getDatasets());
//					update(activeDatasetToList());
				}
			}
		});
		constraints.gridx = 2;
		constraints.gridy = 0;
		panel.add(rotateBck, constraints);

		JButton rotateRst = new JButton("!");
		rotateRst.setToolTipText("Reset rotation to orientation point");
		rotateRst.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				if(activeDataset().getCollection().hasConsensusNucleus()){
					BorderPoint orientationPoint = activeDataset().getCollection().getConsensusNucleus().getBorderTag(BorderTagObject.ORIENTATION_POINT);
					activeDataset().getCollection().getConsensusNucleus().rotatePointToBottom(orientationPoint);
					refreshChartCache(getDatasets());
//					update(activeDatasetToList());
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
	protected void updateSingle() {
		super.updateSingle();
		updateSingleDataset();
		log(Level.FINEST, "Updated consensus panel");
	}
	

	@Override
	protected void updateMultiple() {
		updateMultipleDatasets();
		log(Level.FINEST, "Updated consensus panel");
	}
	
	@Override
	protected void updateNull() {		
		updateBlankChart();
	}
	
	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) throws Exception {
			return ConsensusNucleusChartFactory.getInstance().makeConsensusChart(options);
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options) throws Exception{
		return null;
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
		
//		JFreeChart consensusChart = getChart(options);


		CellCollection collection = activeDataset().getCollection();
		if(collection.hasConsensusNucleus()){

			// hide the refold button
			runRefoldingButton.setVisible(false);
			offsetsPanel.setVisible(true);
//			consensusChartPanel.setChart(consensusChart);
			consensusChartPanel.restoreAutoBounds();
						
		} else {
			
			if(collection.isRefolding()){
				runRefoldingButton.setVisible(false);
			} else {
				runRefoldingButton.setVisible(true);
			}
			
			runRefoldingButton.setVisible(true);
			offsetsPanel.setVisible(false);
//			consensusChartPanel.setChart(consensusChart);
			
			
		}
	}
	
	private void updateMultipleDatasets() {
		
		ChartOptions options = new ChartOptionsBuilder()
			.setDatasets(getDatasets())
			.setScale(GlobalOptions.getInstance().getScale())
			.setSwatch(GlobalOptions.getInstance().getSwatch())
			.setTarget(consensusChartPanel)
			.build();

		JFreeChart chart = getChart(options);

		consensusChartPanel.setChart(chart);
		consensusChartPanel.restoreAutoBounds();


		runRefoldingButton.setVisible(false);
		offsetsPanel.setVisible(false);
	}
	
	private void updateBlankChart() {
		
		ChartOptions options = new ChartOptionsBuilder()
			.setTarget(consensusChartPanel)
			.build();
		JFreeChart consensusChart = getChart(options);

		consensusChartPanel.setChart(consensusChart);
		runRefoldingButton.setVisible(false);
		offsetsPanel.setVisible(false);
		consensusChartPanel.restoreAutoBounds();
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
			log(Level.WARNING, "Cannot rotate: must have one dataset selected");
		}
	}
	
	private void resetConsensusNucleusRotationToOrientationPoint(){
		if(activeDataset()!=null){

			if(activeDataset().getCollection().hasConsensusNucleus()){

				BorderPoint orientationPoint = activeDataset()
						.getCollection()
						.getConsensusNucleus()
						.getBorderTag(BorderTagObject.ORIENTATION_POINT);
				
				activeDataset().getCollection()
				.getConsensusNucleus()
				.rotatePointToBottom(orientationPoint);
				
				this.update(activeDatasetToList());
			}
		} else {
			log(Level.WARNING, "Cannot rotate: must have one dataset selected");
		}
	}
	
	
	private void alignConsensusAlongVerticalPoints(){
		if(activeDataset()!=null){

			if(activeDataset().getCollection().hasConsensusNucleus()){
				
				ConsensusNucleus nucleus = activeDataset().getCollection().getConsensusNucleus();
				
				if(nucleus.hasBorderTag(BorderTagObject.TOP_VERTICAL) && nucleus.hasBorderTag(BorderTagObject.BOTTOM_VERTICAL)){
					
					nucleus.alignPointsOnVertical(nucleus.getBorderTag(BorderTagObject.TOP_VERTICAL), nucleus.getBorderTag(BorderTagObject.BOTTOM_VERTICAL));
					
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
			log(Level.WARNING, "Cannot offset: must have one dataset selected");
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
