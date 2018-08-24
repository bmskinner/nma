package com.bmskinner.nuclear_morphology.charting;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.ScrollPane;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetProfilingMethod;
import com.bmskinner.nuclear_morphology.charting.charts.ProfileChartFactory;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.generic.BorderTagObject;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.TestComponentFactory;
import com.bmskinner.nuclear_morphology.components.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.components.TestDatasetFactory;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter.ColourSwatch;
import com.bmskinner.nuclear_morphology.gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;
import com.bmskinner.nuclear_morphology.logging.ConsoleHandler;
import com.bmskinner.nuclear_morphology.logging.LogPanelFormatter;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Test the profile charting functions
 * @author bms41
 * @since 1.14.0
 *
 */
public class ProfileChartFactoryTest {
	
	@Before
	public void setUp(){
		Logger logger = Logger.getLogger(Loggable.PROGRAM_LOGGER);
		logger.setLevel(Level.FINEST);
		logger.addHandler(new ConsoleHandler(new LogPanelFormatter()));
	}
	
	/**
	 * Create a panel with a chart and the options used to create the chart
	 * @param chart
	 * @param options
	 * @return
	 * @throws InterruptedException
	 */
	private JPanel makeChartPanel(JFreeChart chart, ChartOptions options, String variable) throws InterruptedException {
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(new JLabel(variable), BorderLayout.NORTH);
		panel.add(new ChartPanel(chart), BorderLayout.CENTER);
		panel.add(new JTextArea(options.toString()), BorderLayout.WEST);
		return panel;
	}
	
	/**
	 * Show the charts in the given panels, and wait until the window has been closed
	 * @param panels
	 * @throws InterruptedException
	 */
	private void showCharts(List<JPanel> panels, String title) throws InterruptedException {
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
	
	/**
	 * Create charts with a default range of options variables for the given dataset
	 * @param d
	 * @throws InterruptedException
	 */
	private void generateChartsforOptions(IAnalysisDataset d, String title) throws InterruptedException {
		List<IAnalysisDataset> list = new ArrayList<>();
		list.add(d);
		generateChartsforOptions(list, title);
	}
	
	/**
	 * Create charts with a default range of options variables for the given datasets
	 * @param datasets
	 * @throws InterruptedException
	 */
	private void generateChartsforOptions(List<IAnalysisDataset> datasets, String title) throws InterruptedException {
		List<JPanel> panels = new ArrayList<>();
		for(Tag tag : BorderTagObject.values()) {
			ChartOptions options = new ChartOptionsBuilder().setDatasets(datasets)
					.setTag(tag)
					.setShowMarkers(true)
					.setNormalised(true)
					.build();
			panels.add(makeChartPanel(new ProfileChartFactory(options).createProfileChart(), options, "Tag: "+tag));
		}
		
		for(ProfileAlignment p : ProfileAlignment.values()) {
			ChartOptions options = new ChartOptionsBuilder().setDatasets(datasets)
					.setAlignment(p)
					.setNormalised(false)
					.build();
			panels.add(makeChartPanel(new ProfileChartFactory(options).createProfileChart(), options, "Alignment: "+p));
		}
		
		for(ColourSwatch p : ColourSwatch.values()) {
			ChartOptions options = new ChartOptionsBuilder().setDatasets(datasets)
					.setSwatch(p)
					.build();
			panels.add(makeChartPanel(new ProfileChartFactory(options).createProfileChart(), options, "Swatch: "+p));
		}
		ChartOptions trueOptions = new ChartOptionsBuilder().setDatasets(datasets)
				.setNormalised(true)
				.setAlignment(ProfileAlignment.LEFT)
				.setTag(Tag.REFERENCE_POINT)
				.setShowMarkers(true)
				.setProfileType(ProfileType.ANGLE)
				.setSwatch(ColourSwatch.REGULAR_SWATCH)
				.setShowAnnotations(true)
				.setShowPoints(true)
				.setShowXAxis(true)
				.setShowYAxis(true)
				.build();

		panels.add(makeChartPanel(new ProfileChartFactory(trueOptions).createProfileChart(), trueOptions, "All true"));
		
		ChartOptions falseOptions = new ChartOptionsBuilder().setDatasets(datasets)
				.setNormalised(false)
				.setAlignment(ProfileAlignment.LEFT)
				.setTag(Tag.REFERENCE_POINT)
				.setShowMarkers(false)
				.setProfileType(ProfileType.ANGLE)
				.setSwatch(ColourSwatch.REGULAR_SWATCH)
				.setShowIQR(false)
				.setShowAnnotations(false)
				.setShowPoints(false)
				.setShowXAxis(false)
				.setShowYAxis(false)
				.build();

		panels.add(makeChartPanel(new ProfileChartFactory(falseOptions).createProfileChart(), falseOptions, "All false"));
		showCharts(panels, title);
	}
	
	@Test
	public void testSingleNucleusProfile() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder().cellCount(1).ofType(NucleusType.ROUND)
				.baseHeight(40).baseWidth(40).build();
		ICell c = d.getCollection().getCells().stream().findFirst().get();
		d.setDatasetColour(Color.BLUE);

		List<JPanel> panels = new ArrayList<>();
		
		ChartOptions trueOptions = new ChartOptionsBuilder().setDatasets(d)
				.setCell(c)
				.setNormalised(true)
				.setAlignment(ProfileAlignment.LEFT)
				.setTag(Tag.REFERENCE_POINT)
				.setShowMarkers(true)
				.setProfileType(ProfileType.ANGLE)
				.setSwatch(ColourSwatch.REGULAR_SWATCH)
				.setShowAnnotations(true)
				.setShowPoints(true)
				.setShowXAxis(true)
				.setShowYAxis(true)
				.build();

		panels.add(makeChartPanel(new ProfileChartFactory(trueOptions).createProfileChart(), trueOptions, "All true"));
		
		ChartOptions falseOptions = new ChartOptionsBuilder().setDatasets(d)
				.setCell(c)
				.setNormalised(false)
				.setAlignment(ProfileAlignment.LEFT)
				.setTag(Tag.REFERENCE_POINT)
				.setShowMarkers(false)
				.setProfileType(ProfileType.ANGLE)
				.setSwatch(ColourSwatch.REGULAR_SWATCH)
				.setShowAnnotations(false)
				.setShowPoints(false)
				.setShowXAxis(false)
				.setShowYAxis(false)
				.build();

		panels.add(makeChartPanel(new ProfileChartFactory(falseOptions).createProfileChart(), falseOptions, "All false"));
		showCharts(panels, "Single nucleus, no dataset");
	}
	
	@Test
	public void testSingleNucleusDatasetProfileWithSingleSegment() throws Exception {
		
		IAnalysisDataset d = new TestDatasetBuilder().cellCount(1).ofType(NucleusType.ROUND)
				.profiled().build();
		generateChartsforOptions(d, "Single nucleus dataset, no segments");
	}
	
	@Test
	public void testSingleNucleusDatasetProfileWithMultipleSegments() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder().cellCount(1).ofType(NucleusType.ROUND)
				.segmented().build();
		generateChartsforOptions(d, "Single nucleus, segmented");
	}
	
	@Test
	public void testMultipleNucleusDatasetProfileWithSingleSegment() throws Exception {
		
		IAnalysisDataset d = new TestDatasetBuilder().cellCount(100).withMaxSizeVariation(4).profiled().build();
		generateChartsforOptions(d, "Single dataset, multiple nuclei, single segment");
	}
	
	@Test
	public void testMultipleDatasetsProfileWithSingleSegment() throws Exception {
		IAnalysisDataset d1 = new TestDatasetBuilder().cellCount(100).withMaxSizeVariation(4).profiled().build();
		IAnalysisDataset d2 = new TestDatasetBuilder().cellCount(100).withMaxSizeVariation(10).profiled().build();
		
		List<IAnalysisDataset> list = new ArrayList<>();
		list.add(d1);
		list.add(d2);
		
		generateChartsforOptions(list, "Multiple datasets, multiple nuclei, single segment");
	}

}
