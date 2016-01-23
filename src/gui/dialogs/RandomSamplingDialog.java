package gui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import analysis.AnalysisDataset;
import analysis.RandomSampler;
import charting.charts.HistogramChartFactory;
import gui.LoadingIconDialog;
import ij.IJ;
import stats.NucleusStatistic;

@SuppressWarnings("serial")
public class RandomSamplingDialog extends LoadingIconDialog {
	private AnalysisDataset dataset;
	private ChartPanel chartPanel;
	
	private JSpinner set1SizeSpinner;
	private JSpinner set2SizeSpinner;
	private JSpinner iterattionsSpinner;
	private JComboBox<NucleusStatistic> statsBox = new JComboBox<NucleusStatistic>(NucleusStatistic.values());
	private JButton  runButton;
	
	public RandomSamplingDialog(final AnalysisDataset dataset, final Logger logger){
		super(logger);
		this.dataset= dataset;
		createUI();
		this.setModal(false);
		this.pack();
		this.setVisible(true);
	}
	
	private void createUI(){
		this.setTitle("Random sampling");
		this.setLayout(new BorderLayout());
		
		int cellCount = dataset.getCollection().getNucleusCount();
		
		SpinnerNumberModel first = new SpinnerNumberModel(cellCount,
				1,
				cellCount,
				1);
		set1SizeSpinner = new JSpinner(first);


		SpinnerNumberModel second = new SpinnerNumberModel(cellCount,
				1,
				cellCount,
				1);
		set2SizeSpinner = new JSpinner(second);
		
		int iterations = 1000;
		SpinnerNumberModel iterationsModel = new SpinnerNumberModel(iterations,
				1,
				10000,
				1);
		iterattionsSpinner = new JSpinner(iterationsModel);
		
		runButton = new JButton("Run");
		runButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {

				run();
			
			}
		});	
		
		
		JPanel topPanel = new JPanel(new FlowLayout());
		topPanel.add(set1SizeSpinner);
		topPanel.add(set2SizeSpinner);
		topPanel.add(iterattionsSpinner);
		topPanel.add(statsBox);
		topPanel.add(runButton);
		topPanel.add(this.getLoadingLabel());
		
		this.add(topPanel, BorderLayout.NORTH);
		
		
		List<Double> list = new ArrayList<Double>();
		list.add(1d);
		list.add(1d);
		list.add(1d);
		list.add(1d);
		
		try {
			chartPanel = new ChartPanel(HistogramChartFactory.createRandomSampleHistogram(list));
			this.add(chartPanel, BorderLayout.CENTER);
		} catch (Exception e) {
			programLogger.log(Level.SEVERE, "Error making chart", e);
		}
		
		
	}
	
	private void run(){
		RandomSampler r = new RandomSampler(dataset);
		
		int iterations = (Integer) iterattionsSpinner.getValue();
		int firstCount = (Integer) set1SizeSpinner.getValue();
		int secondCount = (Integer) set2SizeSpinner.getValue();
		NucleusStatistic stat = (NucleusStatistic) statsBox.getSelectedItem();
		
		try {
			setStatusLoading();
			List<Double> result = r.run(stat, iterations, firstCount, secondCount);
			JFreeChart chart = HistogramChartFactory.createRandomSampleHistogram(result);
			chartPanel.setChart(chart);
			setStatusLoaded();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
