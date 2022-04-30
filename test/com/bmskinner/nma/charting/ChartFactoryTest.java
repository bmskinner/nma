package com.bmskinner.nma.charting;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.ScrollPane;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.jfree.chart.JFreeChart;
import org.junit.Before;

import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.profiles.IProfile;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.visualisation.charts.ConsensusNucleusChartFactory;
import com.bmskinner.nma.visualisation.charts.ProfileChartFactory;
import com.bmskinner.nma.visualisation.charts.panels.ExportableChartPanel;
import com.bmskinner.nma.visualisation.options.ChartOptions;
import com.bmskinner.nma.visualisation.options.ChartOptionsBuilder;

/**
 * Base class for testing the charting functions. This provides methods for 
 * displaying charts onscreen for manual examination.
 * @author bms41
 * @since 1.14.0
 *
 */
public abstract class ChartFactoryTest {
	
	@Before
	public void setUp(){
	}
	
	/**
	 * Show a single profile, with any segments if present
	 * @param profile
	 * @throws InterruptedException
	 */
	public static void showProfile(IProfile profile, String title) throws InterruptedException {
		
		ChartOptions options = new ChartOptionsBuilder()
				.setShowAnnotations(true)
				.setShowProfiles(true)
				.build();
		
		showSingleChart(new ProfileTestChartFactory(options).createProfileChart(profile), options, title, false);
	}
	
	/**
	 * Show a panel of profiles, with any segments if present
	 * @param profile
	 * @throws InterruptedException
	 */
	public static void showProfiles(List<IProfile> profiles, List<String> names, String title) throws InterruptedException {
		
		ChartOptions options = new ChartOptionsBuilder()
				.setShowAnnotations(true)
				.setShowProfiles(true)
				.build();
		
		List<JPanel> panels = new ArrayList<>();
		for(int i=0; i<profiles.size(); i++) {
			panels.add(makeChartPanel(new ProfileTestChartFactory(options).createProfileChart(profiles.get(i)), options, names.get(i), false));
		}
		showCharts(panels, title);
	}
	
	/**
	 * Show the median profile for the given dataset
	 * @param d
	 * @param title
	 * @throws InterruptedException
	 */
	public static void showMedianProfile(IAnalysisDataset d, String title) throws InterruptedException {
		ChartOptions options = new ChartOptionsBuilder().setDatasets(d)
				.setShowAnnotations(true)
				.build();
		showSingleChart(new ProfileChartFactory(options).createProfileChart(), options, title, false);
	}
	
	/**
	 * Show the profiles for the given cells from a dataset
	 * @param cells the cells to show
	 * @param d the dataset the cells belong to
	 * @throws InterruptedException
	 */
	public static void showProfiles(Collection<ICell> cells, IAnalysisDataset d) throws InterruptedException {

		List<JPanel> panels = new ArrayList<>();
		
		ChartOptions options = new ChartOptionsBuilder().setDatasets(d)
				.setShowAnnotations(true)
				.setShowProfiles(true)
				.setNormalised(true)
				.build();
		makeChartPanel(new ProfileChartFactory(options).createProfileChart(), options, "Dataset", false);
		panels.add(makeChartPanel(new ProfileChartFactory(options).createProfileChart(), options, "Dataset profile", false));
		
		for(ICell cell : cells) {
			// show the profile corresponding to the chart
			ChartOptions profileOptions = new ChartOptionsBuilder().setDatasets(d)
					.setCell(cell)
					.setLandmark(OrientationMark.REFERENCE)
					.setShowMarkers(true)
					.setShowAnnotations(true)
					.setProfileType(ProfileType.ANGLE)
					.build();
			panels.add(makeChartPanel(new ProfileChartFactory(profileOptions).createProfileChart(), profileOptions, "Cell profile", false));
		}

		showCharts(panels, "Cells in dataset "+d.getName());
	}
	
	public static JPanel makeConsensusChartPanel(IAnalysisDataset d) throws InterruptedException {
//		List<JPanel> panels = new ArrayList<>();
		ChartOptions options = new ChartOptionsBuilder().setDatasets(d).build();
		JFreeChart chart = new ConsensusNucleusChartFactory(options).makeConsensusChart();
		return makeChartPanel(chart, options, "Consensus", true);
//		showCharts(panels, "Consensus");
	}
	
	public static void showConsensus(IAnalysisDataset d) throws InterruptedException {
		List<JPanel> panels = new ArrayList<>();
		ChartOptions options = new ChartOptionsBuilder().setDatasets(d).build();
		JFreeChart chart = new ConsensusNucleusChartFactory(options).makeConsensusChart();
		panels.add(makeChartPanel(chart, options, "Consensus", true));
		showCharts(panels, "Consensus");
	}
	
	protected static void showSingleChart(JFreeChart chart, ChartOptions options, String variable, boolean fixedAspect) throws InterruptedException {
		List<JPanel> panels = new ArrayList<>();
		panels.add(makeChartPanel(chart, options, variable, fixedAspect));
		showCharts(panels, variable);
	}
	
	/**
	 * Create a panel with a chart and the options used to create the chart
	 * @param chart the chart to display
	 * @param options the chart options used to create the chart
	 * @param fixedAspect should the panel be drawn with a fixed aspect ratio?
	 * @return
	 * @throws InterruptedException
	 */
	protected static JPanel makeChartPanel(JFreeChart chart, ChartOptions options, String variable, boolean fixedAspect) throws InterruptedException {
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(new JLabel(variable), BorderLayout.NORTH);
		ExportableChartPanel exp = new ExportableChartPanel(chart);
		exp.setFixedAspectRatio(fixedAspect);
		panel.add(exp, BorderLayout.CENTER);
		panel.add(new JTextArea(options.toString()), BorderLayout.WEST);
		return panel;
	}
	
	/**
	 * Show the charts in the given panels, and wait until the window has been closed
	 * @param panels the panels to show
	 * @param the title of the window
	 * @throws InterruptedException
	 */
	public static void showCharts(List<JPanel> panels, String title) throws InterruptedException {
		JFrame f = new JFrame();
		
		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		
		for(JPanel panel : panels) {
			content.add(panel);
		}
		
		ScrollPane sp = new ScrollPane();
		sp.add(content);
		sp.setPreferredSize(new Dimension(1000, 600));
		f.setTitle(title);
		f.getContentPane().add(sp, BorderLayout.CENTER);
		f.pack();
		f.setVisible(true);
		while(f.isVisible()) {
			Thread.sleep(1000);
		}
	}

}
