package gui.tabs.segments;

import gui.components.ExportableChartPanel;
import gui.tabs.BoxplotsTabPanel;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.logging.Level;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import stats.SegmentStatistic;
import charting.charts.BoxplotChartFactory;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import components.CellCollection;
import components.generic.BorderTag;
import components.generic.MeasurementScale;
import components.generic.ProfileType;
import components.nuclear.NucleusBorderSegment;

@SuppressWarnings("serial")
public class SegmentBoxplotsPanel extends BoxplotsTabPanel implements ActionListener {

	private Dimension preferredSize = new Dimension(200, 300);
			
	public SegmentBoxplotsPanel(){
		super();

		JFreeChart boxplot = BoxplotChartFactory.createEmptyBoxplot();
		

		ExportableChartPanel chartPanel = new ExportableChartPanel(boxplot);
		chartPanel.setPreferredSize(preferredSize);
		chartPanels.put("null", chartPanel);

		mainPanel.add(chartPanel);

		
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		update(getDatasets());
		
	}


	@Override
	protected void updateSingle() throws Exception {
		updateMultiple();
		
	}


	@Override
	protected void updateMultiple() throws Exception {
		mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
		
		MeasurementScale scale = measurementUnitSettingsPanel.getSelected();
		measurementUnitSettingsPanel.setEnabled(true);
		
		log(Level.FINEST, "Dataset list is not empty");

		// Check that all the datasets have the same number of segments
		if(checkSegmentCountsMatch(getDatasets())){ // make a boxplot for each segment
			
			CellCollection collection = activeDataset().getCollection();
			List<NucleusBorderSegment> segments = collection.getProfileCollection(ProfileType.REGULAR)
					.getSegmentedProfile(BorderTag.ORIENTATION_POINT)
					.getOrderedSegments();
			

			// Get each segment as a boxplot
			for(NucleusBorderSegment seg : segments){
				
				ChartOptions options = new ChartOptionsBuilder()
					.setDatasets(getDatasets())
					.setStatistic(SegmentStatistic.LENGTH)
					.setScale(scale)
					.setSegPosition(seg.getPosition())
					.build();
				
				JFreeChart chart = getChart(options);
				
				ExportableChartPanel chartPanel = new ExportableChartPanel(chart);
				chartPanel.setPreferredSize(preferredSize);
				chartPanels.put(seg.getName(), chartPanel);
				mainPanel.add(chartPanel);							
			}

			
		} else { // different number of segments, blank chart
			measurementUnitSettingsPanel.setEnabled(false);
			mainPanel.setLayout(new FlowLayout());
			mainPanel.add(new JLabel("Segment number is not consistent across datasets", JLabel.CENTER));
		}
		mainPanel.revalidate();
		mainPanel.repaint();
		scrollPane.setViewportView(mainPanel);
	}


	@Override
	protected void updateNull() throws Exception {
		mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
		
		MeasurementScale scale = measurementUnitSettingsPanel.getSelected();
		// No datasets, show blank chart
		measurementUnitSettingsPanel.setEnabled(false);

		ChartPanel chartPanel = new ChartPanel(BoxplotChartFactory.createEmptyBoxplot());
		mainPanel.add(chartPanel);
		mainPanel.revalidate();
		mainPanel.repaint();
		scrollPane.setViewportView(mainPanel);
	}
}