package no.gui;

import ij.IJ;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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
	
	private AnalysisDataset activeDataset;

	
	public ConsensusNucleusPanel() {

		this.setLayout(new GridBagLayout());
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
				fireSignalChangeEvent("RefoldNucleusFired");
				runRefoldingButton.setVisible(false);
			}
		});
		runRefoldingButton.setVisible(false);
		
		consensusChartPanel.add(runRefoldingButton);
		consensusChartPanel.setMinimumSize(new Dimension(200, 200));
		
		
		this.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				resizePreview(consensusChartPanel, ConsensusNucleusPanel.this);
			}
		});
		
		this.add(consensusChartPanel, c);
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
					
					runRefoldingButton.setVisible(true);
					JFreeChart consensusChart = ConsensusNucleusChartFactory.makeEmptyNucleusOutlineChart();
					
					if(collection.hasConsensusNucleus()){
						consensusChart = ConsensusNucleusChartFactory.makeSegmentedConsensusChart(activeDataset);
						
						// hide the refold button
						runRefoldingButton.setVisible(false);
					}
					consensusChartPanel.setChart(consensusChart);
					
				}else {
					JFreeChart chart = ConsensusNucleusChartFactory.makeMultipleConsensusChart(list);
					consensusChartPanel.setChart(chart);
					runRefoldingButton.setVisible(false);
				}

			} else { // no datasets in the list
				
				JFreeChart consensusChart = ConsensusNucleusChartFactory.makeEmptyNucleusOutlineChart();
				consensusChartPanel.setChart(consensusChart);
				runRefoldingButton.setVisible(false);
				
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
