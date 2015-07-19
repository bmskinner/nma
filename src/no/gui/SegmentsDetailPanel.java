package no.gui;

import ij.IJ;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import no.analysis.AnalysisDataset;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.xy.DefaultXYDataset;

import datasets.NucleusDatasetCreator;

public class SegmentsDetailPanel extends JPanel implements ActionListener {

	private static final long serialVersionUID = 1L;

	private ChartPanel segmentsBoxplotChartPanel; // for displaying the legnth of a given segment
	private ChartPanel segmentsProfileChartPanel; // for displaying the profiles of a given segment
	
	// 
	private JCheckBox    normSegmentCheckBox = new JCheckBox("Normalised");	// to toggle raw or normalised segment profiles in segmentsProfileChartPanel
	private JRadioButton rawSegmentLeftButton  = new JRadioButton("Left"); // left align raw segment profiles in segmentsProfileChartPanel
	private JRadioButton rawSegmentRightButton = new JRadioButton("Right"); // right align raw segment profiles in segmentsProfileChartPanel
	
	private List<AnalysisDataset> list;
	
	private JPanel segmentsBoxplotPanel;// container for boxplots chart and decoration
	private JPanel segmentsProfilePanel;// container for profiles
	private JComboBox<String> segmentSelectionBox; // choose which segments to compare
	
	
	public SegmentsDetailPanel() {
			
		this.setLayout(new GridLayout(0,2,0,0));
		
		segmentsProfilePanel  = createSegmentProfilePanel();
		segmentsBoxplotPanel = createSegmentBoxplotsPanel();
		
		this.add(segmentsProfilePanel);
		this.add(segmentsBoxplotPanel);
		
	}
	
	private JPanel createSegmentProfilePanel(){
		
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		Dimension minimumChartSize = new Dimension(50, 100);
		Dimension preferredChartSize = new Dimension(400, 300);
		JFreeChart profileChart = ChartFactory.createXYLineChart(null,
	            "Position", "Angle", null);
		XYPlot plot = profileChart.getXYPlot();
		plot.getDomainAxis().setRange(0,100);
		plot.getRangeAxis().setRange(0,360);
		plot.setBackgroundPaint(Color.WHITE);
		
		segmentsProfileChartPanel = new ChartPanel(profileChart);
		segmentsProfileChartPanel.setMinimumSize(minimumChartSize);
		segmentsProfileChartPanel.setPreferredSize(preferredChartSize);
		segmentsProfileChartPanel.setMinimumDrawWidth( 0 );
		segmentsProfileChartPanel.setMinimumDrawHeight( 0 );
		panel.add(segmentsProfileChartPanel, BorderLayout.CENTER);
		
		
		// checkbox to select raw or normalised profiles
		normSegmentCheckBox.setSelected(true);
		normSegmentCheckBox.setEnabled(false);
		normSegmentCheckBox.setActionCommand("NormalisedSegmentProfile");
		normSegmentCheckBox.addActionListener(this);
		
		// make buttons to select raw profiles
		rawSegmentLeftButton.setSelected(true);
		rawSegmentLeftButton.setActionCommand("LeftAlignSegmentProfile");
		rawSegmentRightButton.setActionCommand("RightAlignSegmentProfile");
		rawSegmentLeftButton.addActionListener(this);
		rawSegmentRightButton.addActionListener(this);
		rawSegmentLeftButton.setEnabled(false);
		rawSegmentRightButton.setEnabled(false);
		

		//Group the radio buttons.
		final ButtonGroup alignGroup = new ButtonGroup();
		alignGroup.add(rawSegmentLeftButton);
		alignGroup.add(rawSegmentRightButton);
		
		JPanel alignPanel = new JPanel();
		alignPanel.setLayout(new BoxLayout(alignPanel, BoxLayout.X_AXIS));

		alignPanel.add(normSegmentCheckBox);
		alignPanel.add(rawSegmentLeftButton);
		alignPanel.add(rawSegmentRightButton);
		panel.add(alignPanel, BorderLayout.NORTH);
		return panel;
	}
	
	private JPanel createSegmentBoxplotsPanel(){
		JPanel panel = new JPanel(); // main container in tab

		panel.setLayout(new BorderLayout());
		
		JFreeChart boxplot = ChartFactory.createBoxAndWhiskerChart(null, null, null, new DefaultBoxAndWhiskerCategoryDataset(), false);	
		boxplot.getPlot().setBackgroundPaint(Color.WHITE);
		
		segmentsBoxplotChartPanel = new ChartPanel(boxplot);
		panel.add(segmentsBoxplotChartPanel, BorderLayout.CENTER);
		
		segmentSelectionBox = new JComboBox<String>();
		segmentSelectionBox.setActionCommand("SegmentBoxplotChoice");
		segmentSelectionBox.addActionListener(this);
		panel.add(segmentSelectionBox, BorderLayout.NORTH);

		return panel;
	}
	
	public void update(List<AnalysisDataset> list){
		this.list = list;
		
		if(list!=null && !list.isEmpty()){
			normSegmentCheckBox.setEnabled(true);
			// get the list of segments from the first dataset
			ComboBoxModel<String> aModel = new DefaultComboBoxModel<String>(list.get(0).getCollection().getSegmentNames().toArray(new String[0]));
			segmentSelectionBox.setModel(aModel);
			segmentSelectionBox.setSelectedIndex(0);
			updateSegmentsBoxplot(list, (String) segmentSelectionBox.getSelectedItem()); // get segname from panel
			updateSegmentsProfile(list, (String) segmentSelectionBox.getSelectedItem(), true, false); // get segname from panel
			 
		} else {
			// if the list is empty, do not enable controls
			normSegmentCheckBox.setEnabled(false);
			rawSegmentLeftButton.setEnabled(false);
			rawSegmentRightButton.setEnabled(false);
		}
	}
	
	private void updateSegmentsBoxplot(List<AnalysisDataset> list, String segName){
		BoxAndWhiskerCategoryDataset ds = NucleusDatasetCreator.createSegmentLengthDataset(list, segName);
		JFreeChart boxplotChart = ChartFactory.createBoxAndWhiskerChart(null, null, null, ds, false); 
		formatBoxplotChart(boxplotChart, list);
		segmentsBoxplotChartPanel.setChart(boxplotChart);
	}
	
	private void updateSegmentsProfile(List<AnalysisDataset> list, String segName, boolean normalised, boolean rightAlign){
		
		DefaultXYDataset ds = null;
		if(normalised){
			ds = NucleusDatasetCreator.createMultiProfileSegmentDataset(list, segName);
		} else {
			ds = NucleusDatasetCreator.createRawMultiProfileSegmentDataset(list, segName, rightAlign);
		}
		try {
				
				JFreeChart chart = 
						ChartFactory.createXYLineChart(null,
						                "Position", "Angle", ds, PlotOrientation.VERTICAL, true, true,
						                false);

				XYPlot plot = chart.getXYPlot();
				
				if(!normalised){
					int length = 100;
					for(AnalysisDataset d : list){
						if(   (int) d.getCollection().getMedianArrayLength()>length){
							length = (int) d.getCollection().getMedianArrayLength();
						}
					}
					plot.getDomainAxis().setRange(0,length);
				} else {
					plot.getDomainAxis().setRange(0,100);
				}

				plot.getRangeAxis().setRange(0,360);
				plot.setBackgroundPaint(Color.WHITE);
				plot.addRangeMarker(new ValueMarker(180, Color.BLACK, new BasicStroke(2.0f)));
				
				for (int i = 0; i < plot.getSeriesCount(); i++) {
					plot.getRenderer().setSeriesVisibleInLegend(i, Boolean.FALSE);
					String name = (String) ds.getSeriesKey(i);
					if(name.startsWith("Seg_")){
						int colourIndex = getIndexFromLabel(name);
						plot.getRenderer().setSeriesStroke(i, new BasicStroke(4));
						plot.getRenderer().setSeriesPaint(i, ColourSelecter.getSegmentColor(colourIndex));
					} 
					if(name.startsWith("Profile_")){
						plot.getRenderer().setSeriesStroke(i, new BasicStroke(1));
						plot.getRenderer().setSeriesPaint(i, Color.LIGHT_GRAY);
					} 
					
				}	
								
				segmentsProfileChartPanel.setChart(chart);
			
			
		} catch (Exception e) {
			IJ.log("Error in plotting segment profile");
		} 
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub
		if(arg0.getActionCommand().equals("SegmentBoxplotChoice")){
			String segName = (String) segmentSelectionBox.getSelectedItem();
			
			// create the appropriate chart
			updateSegmentsBoxplot(list, segName);
			
			if(  normSegmentCheckBox.isSelected()){
				updateSegmentsProfile(list, (String) segmentSelectionBox.getSelectedItem(), true, false);
			} else {
				
				if(  rawSegmentLeftButton.isSelected()){
					updateSegmentsProfile(list, (String) segmentSelectionBox.getSelectedItem(), false, false);
				} else {
					updateSegmentsProfile(list, (String) segmentSelectionBox.getSelectedItem(), false, true);
				}
			}
			
		}
				
		if(arg0.getActionCommand().equals("LeftAlignSegmentProfile")){
			updateSegmentsProfile(list, (String) segmentSelectionBox.getSelectedItem(), false, false);
		}
		
		if(arg0.getActionCommand().equals("RightAlignSegmentProfile")){
			updateSegmentsProfile(list, (String) segmentSelectionBox.getSelectedItem(), false, true);
		}
		
		if(arg0.getActionCommand().equals("NormalisedSegmentProfile")){

			if(  normSegmentCheckBox.isSelected()){
				rawSegmentLeftButton.setEnabled(false);
				rawSegmentRightButton.setEnabled(false);
				updateSegmentsProfile(list, (String) segmentSelectionBox.getSelectedItem(), true, false);
			} else {
				rawSegmentLeftButton.setEnabled(true);
				rawSegmentRightButton.setEnabled(true);
				
				if(  rawSegmentLeftButton.isSelected()){
					updateSegmentsProfile(list, (String) segmentSelectionBox.getSelectedItem(), false, false);
				} else {
					updateSegmentsProfile(list, (String) segmentSelectionBox.getSelectedItem(), false, true);
				}
			}
			
		}
		
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
	
//	/**
//	 * Apply the default formatting to a boxplot
//	 * @param boxplot
//	 */
//	private void formatBoxplotChart(JFreeChart boxplot){
//		CategoryPlot plot = boxplot.getCategoryPlot();
//		plot.setBackgroundPaint(Color.WHITE);
//		BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
//		plot.setRenderer(renderer);
//		renderer.setUseOutlinePaintForWhiskers(true);   
//		renderer.setBaseOutlinePaint(Color.BLACK);
//		renderer.setBaseFillPaint(Color.LIGHT_GRAY);
//		for(int i=0;i<plot.getDataset().getRowCount();i++){
//			Color color = ColourSelecter.getSegmentColor(i);
//			renderer.setSeriesPaint(i, color);
//		}
//		renderer.setMeanVisible(false);
//	}
	
	/**
	 * Apply the default formatting to a boxplot with list
	 * @param boxplot
	 */
	private void formatBoxplotChart(JFreeChart boxplot, List<AnalysisDataset> list){
		formatBoxplotChart(boxplot);
		CategoryPlot plot = boxplot.getCategoryPlot();
		BoxAndWhiskerRenderer renderer = (BoxAndWhiskerRenderer) plot.getRenderer();
		
		for(int i=0;i<plot.getDataset().getRowCount();i++){
			
			AnalysisDataset d = list.get(i);

			Color color = d.getDatasetColour() == null 
						? ColourSelecter.getSegmentColor(i)
						: d.getDatasetColour();
						
						renderer.setSeriesPaint(i, color);
		}
		renderer.setMeanVisible(false);
	}
	
	/**
	 * Apply basic formatting to the charts, without any series added
	 * @param boxplot
	 */
	private void formatBoxplotChart(JFreeChart boxplot){
		CategoryPlot plot = boxplot.getCategoryPlot();
		plot.setBackgroundPaint(Color.WHITE);
		BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
		plot.setRenderer(renderer);
		renderer.setUseOutlinePaintForWhiskers(true);   
		renderer.setBaseOutlinePaint(Color.BLACK);
		renderer.setBaseFillPaint(Color.LIGHT_GRAY);
	}

}
