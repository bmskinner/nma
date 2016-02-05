package gui.tabs.profiles;

import gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import charting.charts.MorphologyChartFactory;
import charting.datasets.NucleusTableDatasetCreator;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import charting.options.TableOptions;
import components.generic.BorderTag;
import components.generic.ProfileType;

@SuppressWarnings("serial")
public class ProfileDisplayPanel extends AbstractProfileDisplayPanel {
	
	private ProfileType type;
		
		public ProfileDisplayPanel(Logger logger, ProfileType type){
			super(logger);
			this.type = type;
			
			JFreeChart chart = MorphologyChartFactory.makeEmptyProfileChart(type);
			chartPanel.setChart(chart);
		}
		
		@Override
		protected void updateSingle() throws Exception {
			super.updateSingle();
			updateChart();
			
		}
		
		@Override
		protected void updateMultiple() throws Exception {
			super.updateMultiple();
			updateChart();
		}
		
		@Override
		protected void updateNull() throws Exception {
			super.updateNull();
			JFreeChart chart = MorphologyChartFactory.makeEmptyProfileChart(type);
			chartPanel.setChart(chart);

		}
		
		@Override
		protected JFreeChart createPanelChartType(ChartOptions options) throws Exception {
			return MorphologyChartFactory.createProfileChart( options );
		}
		
		@Override
		protected TableModel createPanelTableType(TableOptions options) throws Exception{
			return null;
		}
		
		private void updateChart() throws Exception{
			ChartOptions options = makeOptions();
			JFreeChart   chart   = getChart(options);
			chartPanel.setChart(chart);
			
		}
		
		private ChartOptions makeOptions(){

			boolean normalised         = profileAlignmentOptionsPanel.isNormalised();
			ProfileAlignment alignment = normalised ?  ProfileAlignment.LEFT : profileAlignmentOptionsPanel.getSelected();
			BorderTag tag              = borderTagOptionsPanel.getSelected();
			boolean showMarkers        = profileMarkersOptionsPanel.showMarkers();
			
			ChartOptions options = new ChartOptionsBuilder()
				.setDatasets(getDatasets())
				.setLogger(programLogger)
				.setNormalised(normalised)
				.setAlignment(alignment)
				.setTag(tag)
				.setShowMarkers(showMarkers)
				.setProfileType(type)
				.build();
			return options;
		}
}
		