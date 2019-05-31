package com.bmskinner.nuclear_morphology.charting;

import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.charting.charts.ProfileChartFactory;
import com.bmskinner.nuclear_morphology.charting.datasets.ChartDatasetCreationException;
import com.bmskinner.nuclear_morphology.charting.datasets.ProfileDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.ProfileDatasetCreator.ProfileChartDataset;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Create profile charts for test cases specifically
 * @author ben
 * @since 1.15.0
 *
 */
public class ProfileTestChartFactory extends ProfileChartFactory {
	
	private static final Logger LOGGER = Logger.getLogger(Loggable.ROOT_LOGGER);

	public ProfileTestChartFactory(ChartOptions o) {
		super(o);
	}
	
	/**
	 * Create a profile chart for the given profile. This is used mostly in test classes
	 * @param profile
	 * @return
	 */
	public JFreeChart createProfileChart(@NonNull IProfile profile) {
		ProfileChartDataset ds;
		try {
			ds = new ProfileDatasetCreator(options).createProfileDataset(profile);
		} catch (ChartDatasetCreationException e) {
			LOGGER.log(Loggable.STACK, "Error creating profile chart", e);
			return createErrorChart();
		}

		JFreeChart chart = makeProfileChart(ds, profile.size());
		// Add segment name annotations

		if(profile instanceof ISegmentedProfile) {
			ISegmentedProfile segProfile = (ISegmentedProfile)profile;
			if (options.isShowAnnotations())
				addSegmentTextAnnotations(segProfile, chart.getXYPlot());
		}
		return chart;
	}

}
