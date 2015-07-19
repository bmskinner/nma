package no.gui;

import ij.IJ;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

import no.analysis.AnalysisDataset;
import no.collections.CellCollection;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;

import datasets.NucleusDatasetCreator;

public class ConsensusNucleusPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private ChartPanel consensusChartPanel;
	private JButton runRefoldingButton;
	
	private List<Object> listeners = new ArrayList<Object>();
	
	public ConsensusNucleusPanel() {

		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		JFreeChart consensusChart = ChartFactory.createXYLineChart(null,
				null, null, null);
		XYPlot consensusPlot = consensusChart.getXYPlot();
		consensusPlot.setBackgroundPaint(Color.WHITE);
		consensusPlot.getDomainAxis().setVisible(false);
		consensusPlot.getRangeAxis().setVisible(false);
		
		consensusChartPanel = new ChartPanel(consensusChart);

		runRefoldingButton = new JButton("Refold");

		runRefoldingButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				fireRefoldEvent();
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
		
		this.add(consensusChartPanel);
	}
	
	/**
	 * Update the consensus nucleus panel with data from the given datasets. Produces a blank
	 * chart if no refolded nuclei are present
	 * @param list the datasets
	 */	
	public void update(List<AnalysisDataset> list){

		CellCollection collection = list.get(0).getCollection();
		try {
			if(list.size()==1){
				if(!collection.hasConsensusNucleus()){
					// add button to run analysis
					JFreeChart consensusChart = ChartFactory.createXYLineChart(null,
							null, null, null);
					XYPlot consensusPlot = consensusChart.getXYPlot();
					consensusPlot.setBackgroundPaint(Color.WHITE);
					consensusPlot.getDomainAxis().setVisible(false);
					consensusPlot.getRangeAxis().setVisible(false);
					consensusChartPanel.setChart(consensusChart);
					
					runRefoldingButton.setVisible(true);


				} else {
					runRefoldingButton.setVisible(false);
					JFreeChart chart = makeConsensusChart(collection);
					consensusChartPanel.setChart(chart);
				} 
			}else {
				// multiple nuclei
				XYDataset ds = NucleusDatasetCreator.createMultiNucleusOutline(list);
				JFreeChart chart = 
						ChartFactory.createXYLineChart(null,
								null, null, ds, PlotOrientation.VERTICAL, true, true,
								false);
				XYPlot plot = chart.getXYPlot();
				plot.getDomainAxis().setVisible(false);
				plot.getRangeAxis().setVisible(false);
				plot.setBackgroundPaint(Color.WHITE);
				plot.addRangeMarker(new ValueMarker(0, Color.LIGHT_GRAY, new BasicStroke(1.0f)));
				plot.addDomainMarker(new ValueMarker(0, Color.LIGHT_GRAY, new BasicStroke(1.0f)));
				
				int seriesCount = plot.getSeriesCount();
				
				for (int i = 0; i < seriesCount; i++) {
					plot.getRenderer().setSeriesVisibleInLegend(i, Boolean.FALSE);
					String name = (String) ds.getSeriesKey(i);
					plot.getRenderer().setSeriesStroke(i, new BasicStroke(2));
					
					int index = getIndexFromLabel(name);
					AnalysisDataset d = list.get(index);
					
					Color color = d.getDatasetColour() == null 
							? ColourSelecter.getSegmentColor(i)
							: d.getDatasetColour();

					// get the group id from the name, and make colour
					plot.getRenderer().setSeriesPaint(i, color);
					if(name.startsWith("Q")){
						// make the IQR distinct from the median
						plot.getRenderer().setSeriesPaint(i, color.darker());
					}
					
				}
				consensusChartPanel.setChart(chart);
				for(Component c : consensusChartPanel.getComponents() ){
					if(c.getClass()==JButton.class){
						c.setVisible(false);
					}
				}
			}
		} catch (Exception e) {
			IJ.log("Error drawing consensus nucleus: "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Create a consenusus chart for the given nucleus collection
	 * @param collection the NucleusCollection to draw the consensus from
	 * @return the consensus chart
	 */
	private JFreeChart makeConsensusChart(CellCollection collection){
		XYDataset ds = NucleusDatasetCreator.createNucleusOutline(collection);
		JFreeChart chart = 
				ChartFactory.createXYLineChart(null,
						null, null, null, PlotOrientation.VERTICAL, true, true,
						false);
		

		double maxX = Math.max( Math.abs(collection.getConsensusNucleus().getMinX()) , Math.abs(collection.getConsensusNucleus().getMaxX() ));
		double maxY = Math.max( Math.abs(collection.getConsensusNucleus().getMinY()) , Math.abs(collection.getConsensusNucleus().getMaxY() ));

		// ensure that the scales for each axis are the same
		double max = Math.max(maxX, maxY);

		// ensure there is room for expansion of the target nucleus due to IQR
		max *=  1.25;		

		XYPlot plot = chart.getXYPlot();
		plot.setDataset(0, ds);
		plot.getDomainAxis().setRange(-max,max);
		plot.getRangeAxis().setRange(-max,max);

		plot.getDomainAxis().setVisible(false);
		plot.getRangeAxis().setVisible(false);

		plot.setBackgroundPaint(Color.WHITE);
		plot.addRangeMarker(new ValueMarker(0, Color.LIGHT_GRAY, new BasicStroke(1.0f)));
		plot.addDomainMarker(new ValueMarker(0, Color.LIGHT_GRAY, new BasicStroke(1.0f)));

		int seriesCount = plot.getSeriesCount();

		for (int i = 0; i < seriesCount; i++) {
			plot.getRenderer().setSeriesVisibleInLegend(i, Boolean.FALSE);
			String name = (String) ds.getSeriesKey(i);
			
			if(name.startsWith("Seg_")){
				int colourIndex = getIndexFromLabel(name);
				plot.getRenderer().setSeriesStroke(i, new BasicStroke(3));
				plot.getRenderer().setSeriesPaint(i, ColourSelecter.getSegmentColor(colourIndex));
			} 
			if(name.startsWith("Q")){
				plot.getRenderer().setSeriesStroke(i, new BasicStroke(2));
				plot.getRenderer().setSeriesPaint(i, Color.DARK_GRAY);
			} 

		}	
		return chart;
	}
	
	/**
	 * Get a series or dataset index for colour selection when drawing charts. The index
	 * is set in the DatasetCreator as part of the label. The format is Name_index_other
	 * @param label the label to extract the index from 
	 * @return the index found
	 */
	private int getIndexFromLabel(String label){
		String[] names = label.split("_");
		return Integer.parseInt(names[1]);
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
        innerPanel.setSize(size, size);
//        innerPanel.setPreferredSize(new Dimension(size, size));
//        innerPanel.setMaximumSize(	new Dimension(size, size));
        container.revalidate();
    }
	
	public synchronized void addSignalChangeListener( SignalChangeListener l ) {
        listeners.add( l );
    }
    
    public synchronized void removeSignalChangeListener( SignalChangeListener l ) {
        listeners.remove( l );
    }
     
    private synchronized void fireRefoldEvent() {
    	
        SignalChangeEvent event = new SignalChangeEvent( this, "RefoldNucleusFired" );
        Iterator iterator = listeners.iterator();
        while( iterator.hasNext() ) {
            ( (SignalChangeListener) iterator.next() ).signalChangeReceived( event );
//            IJ.log("Fired refold");
        }
    }
    
    

}
