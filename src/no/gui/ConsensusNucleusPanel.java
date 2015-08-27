package no.gui;

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

import no.analysis.AnalysisDataset;
import no.collections.CellCollection;
import no.components.NucleusBorderPoint;
import no.components.XYPoint;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;

import datasets.ConsensusNucleusChartFactory;
import datasets.MorphologyChartFactory;
import datasets.NucleusDatasetCreator;

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
		c.gridwidth = 1;
		c.fill = GridBagConstraints.BOTH;      //reset to default
		c.weightx = 0.0;         
		
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
		consensusChartPanel.setMinimumSize(new Dimension(200, 200));
		
		
		this.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				resizePreview(consensusChartPanel, mainPanel);
			}
		});
		
		mainPanel.add(consensusChartPanel, c);
		this.add(mainPanel, BorderLayout.CENTER);
		
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
		
		JButton moveUp = new JButton("+x");
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
		
		JButton moveDown = new JButton("-x");
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
		
		JButton moveLeft = new JButton("-y");
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
		
		JButton moveright = new JButton("-y");
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
		
		JButton rotateFwd = new JButton("-");
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

		JButton rotateBck = new JButton("+");
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
					} else {
						runRefoldingButton.setVisible(true);
						offsetsPanel.setVisible(false);
					}
					consensusChartPanel.setChart(consensusChart);
					
					
				}else {
					
					JFreeChart chart = ConsensusNucleusChartFactory.makeMultipleConsensusChart(list);
					consensusChartPanel.setChart(chart);
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
			IJ.log("Error drawing consensus nucleus: "+e.getMessage());
			for(StackTraceElement e1 : e.getStackTrace()){
				IJ.log(e1.toString());
			}
		}
	}
			
	/**
	 * Alter the size of the given panel to keep the aspect ratio constant.
	 * The minimum of the width or height of the container is used as the
	 * preferred size of the chart
	 * @param innerPanel the chart to constrain
	 * @param container the contining panel
	 */
	private static void resizePreview(ChartPanel innerPanel, JPanel container) {
        int w = container.getWidth();
        int h = container.getHeight();
        int size =  Math.min(w, h);
//        innerPanel.setSize(size, size);
        innerPanel.setPreferredSize(new Dimension(size, size));
//        innerPanel.setMaximumSize(	new Dimension(size, size));
        container.revalidate();
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
