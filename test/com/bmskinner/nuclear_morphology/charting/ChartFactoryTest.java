package com.bmskinner.nuclear_morphology.charting;

import java.awt.BorderLayout;
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

import com.bmskinner.nuclear_morphology.charting.charts.ProfileChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.logging.ConsoleHandler;
import com.bmskinner.nuclear_morphology.logging.LogPanelFormatter;
import com.bmskinner.nuclear_morphology.logging.Loggable;

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
		Logger logger = Logger.getLogger(Loggable.PROGRAM_LOGGER);
		logger.setLevel(Level.FINEST);
		logger.addHandler(new ConsoleHandler(new LogPanelFormatter()));
	}
	
	protected void showSingleChart(JFreeChart chart, ChartOptions options, String variable, boolean fixedAspect) throws InterruptedException {
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
	protected JPanel makeChartPanel(JFreeChart chart, ChartOptions options, String variable, boolean fixedAspect) throws InterruptedException {
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
	protected void showCharts(List<JPanel> panels, String title) throws InterruptedException {
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
