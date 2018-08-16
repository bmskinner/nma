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
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.TestComponentFactory;
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
	private JPanel makeChartPanel(JFreeChart chart, ChartOptions options) throws InterruptedException {
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(new ChartPanel(chart), BorderLayout.CENTER);
		panel.add(new JTextArea(options.toString()), BorderLayout.WEST);
		return panel;
	}
	
	/**
	 * Show the charts in the given panels, and wait until the window has been closed
	 * @param panels
	 * @throws InterruptedException
	 */
	private void showCharts(List<JPanel> panels) throws InterruptedException {
		JFrame f = new JFrame();
		
		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		
		for(JPanel panel : panels) {
			content.add(panel);
		}
		
		ScrollPane sp = new ScrollPane();
		sp.add(content);
		sp.setPreferredSize(new Dimension(1000, 600));
		
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
	private void generateChartsforOptions(IAnalysisDataset d) throws InterruptedException {
		List<IAnalysisDataset> list = new ArrayList<>();
		list.add(d);
		generateChartsforOptions(list);
	}
	
	/**
	 * Create charts with a default range of options variables for the given datasets
	 * @param datasets
	 * @throws InterruptedException
	 */
	private void generateChartsforOptions(List<IAnalysisDataset> datasets) throws InterruptedException {
		List<JPanel> panels = new ArrayList<>();
		for(Tag tag : BorderTagObject.values()) {
			ChartOptions options = new ChartOptionsBuilder().setDatasets(datasets)
					.setTag(tag)
					.setShowMarkers(true)
					.setNormalised(true)
					.build();
			panels.add(makeChartPanel(new ProfileChartFactory(options).createProfileChart(), options));
		}
		
		for(ProfileAlignment p : ProfileAlignment.values()) {
			ChartOptions options = new ChartOptionsBuilder().setDatasets(datasets)
					.setAlignment(p)
					.setNormalised(false)
					.build();
			panels.add(makeChartPanel(new ProfileChartFactory(options).createProfileChart(), options));
		}
		
		for(ColourSwatch p : ColourSwatch.values()) {
			ChartOptions options = new ChartOptionsBuilder().setDatasets(datasets)
					.setSwatch(p)
					.build();
			panels.add(makeChartPanel(new ProfileChartFactory(options).createProfileChart(), options));
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

		panels.add(makeChartPanel(new ProfileChartFactory(trueOptions).createProfileChart(), trueOptions));
		
		ChartOptions falseOptions = new ChartOptionsBuilder().setDatasets(datasets)
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

		panels.add(makeChartPanel(new ProfileChartFactory(falseOptions).createProfileChart(), falseOptions));
		showCharts(panels);
	}
	
	@Test
	public void testSingleNucleusProfile() throws ComponentCreationException, InterruptedException {
		IAnalysisDataset d = TestDatasetFactory.squareDataset(1);
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

		panels.add(makeChartPanel(new ProfileChartFactory(trueOptions).createProfileChart(), trueOptions));
		
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

		panels.add(makeChartPanel(new ProfileChartFactory(falseOptions).createProfileChart(), falseOptions));
		showCharts(panels);
	}
	
	@Test
	public void testSingleNucleusDatasetProfile() throws Exception {
		IAnalysisDataset d = TestDatasetFactory.squareDataset(1);
		DatasetProfilingMethod m = new DatasetProfilingMethod(d);
		m.call();
		generateChartsforOptions(d);
	}
	
	@Test
	public void testMultipleNucleusDatasetProfile() throws Exception {
		IAnalysisDataset d = TestDatasetFactory.variableRectangularDataset(100, 2);
		DatasetProfilingMethod m = new DatasetProfilingMethod(d);
		m.call();
		generateChartsforOptions(d);
	}
	
	@Test
	public void testMultipleDatasetsProfile() throws Exception {
		IAnalysisDataset d1 = TestDatasetFactory.variableRectangularDataset(100, 4);
		DatasetProfilingMethod m = new DatasetProfilingMethod(d1);
		m.call();
		
		IAnalysisDataset d2 = TestDatasetFactory.variableRectangularDataset(100, 10);
		m = new DatasetProfilingMethod(d2);
		m.call();
		
		List<IAnalysisDataset> list = new ArrayList<>();
		list.add(d1);
		list.add(d2);
		
		generateChartsforOptions(list);
	}

}
