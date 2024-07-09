package com.bmskinner.nma.charting;

import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nma.components.profiles.IProfile;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.profiles.ISegmentedProfile;
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.visualisation.charts.ProfileChartFactory;
import com.bmskinner.nma.visualisation.datasets.ChartDatasetCreationException;
import com.bmskinner.nma.visualisation.datasets.ProfileDatasetCreator;
import com.bmskinner.nma.visualisation.datasets.ProfileDatasetCreator.ProfileChartDataset;
import com.bmskinner.nma.visualisation.options.ChartOptions;

/**
 * Create profile charts for test cases specifically
 * 
 * @author ben
 * @since 1.15.0
 *
 */
public class ProfileTestChartFactory extends ProfileChartFactory {

	private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

	public ProfileTestChartFactory(ChartOptions o) {
		super(o);
	}

	/**
	 * Create a profile chart for the given profile. This is used mostly in test
	 * classes
	 * 
	 * @param profile
	 * @return
	 * @throws SegmentUpdateException
	 */
	public JFreeChart createProfileChart(@NonNull IProfile profile) throws SegmentUpdateException {
		ProfileChartDataset ds;
		try {
			ds = new ProfileDatasetCreator(options).createProfileDataset(profile);
		} catch (ChartDatasetCreationException e) {
			LOGGER.log(Loggable.STACK, "Error creating profile chart", e);
			return createErrorChart();
		}

		JFreeChart chart = makeProfileChart(ds, profile.size());
		// Add segment name annotations

		if (profile instanceof ISegmentedProfile) {
			ISegmentedProfile segProfile = (ISegmentedProfile) profile;
			if (options.isShowAnnotations())
				addSegmentTextAnnotations(segProfile, chart.getXYPlot());
		}
		return chart;
	}

}
