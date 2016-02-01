package gui.tabs.profiles;

import gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jfree.chart.JFreeChart;

import charting.charts.MorphologyChartFactory;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
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
			
			ChartOptions options = makeOptions();
						
			JFreeChart chart = null;

			// Check for a cached chart
			if( ! getChartCache().hasChart(options)){

				// full segment colouring
				chart = MorphologyChartFactory.makeSingleProfileChart( options );

				getChartCache().addChart(options, chart);
				programLogger.log(Level.FINEST, "Added cached profile chart");
			}
			
			chart = getChartCache().getChart(options);
			chartPanel.setChart(chart);
			
		}
		
		@Override
		protected void updateMultiple() throws Exception {
			super.updateMultiple();
			ChartOptions options = makeOptions();
			
			JFreeChart chart = null;
			
			// Check for a cached chart
			if( ! getChartCache().hasChart(options)){

				// full segment colouring
				chart = MorphologyChartFactory.makeMultiProfileChart( options );

				getChartCache().addChart(options, chart);
				programLogger.log(Level.FINEST, "Added cached profile chart");
			}

			chart = getChartCache().getChart(options);
			chartPanel.setChart(chart);
			
		}
		
		@Override
		protected void updateNull() throws Exception {
			super.updateNull();
			JFreeChart chart = MorphologyChartFactory.makeEmptyProfileChart(type);
			chartPanel.setChart(chart);

		}
		
		private ChartOptions makeOptions(){

			boolean normalised         = profileAlignmentOptionsPanel.isNormalised();
			ProfileAlignment alignment = normalised ?  ProfileAlignment.LEFT : profileAlignmentOptionsPanel.getSelected();
			BorderTag tag              = borderTagOptionsPanel.getSelected();
			boolean showMarkers        = profileMarkersOptionsPanel.showMarkers();
			
			ChartOptionsBuilder builder = new ChartOptionsBuilder();
			ChartOptions options = builder.setDatasets(getDatasets())
				.setLogger(programLogger)
				.setNormalised(normalised)
				.setAlignment(alignment)
				.setTag(tag)
				.setShowMarkers(showMarkers)
				.build();
			return options;
		}
}
		