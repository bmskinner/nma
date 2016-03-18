package gui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.logging.Level;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jfree.chart.JFreeChart;

import analysis.AnalysisDataset;
import charting.charts.ConsensusNucleusChartFactory;
import charting.charts.OutlineChartFactory;
import components.nuclei.Nucleus;
import gui.LoadingIconDialog;
import gui.components.FixedAspectRatioChartPanel;

@SuppressWarnings("serial")
public class ConsensusCompareDialog extends LoadingIconDialog implements ActionListener, ChangeListener {
	
	private List<AnalysisDataset> datasets;
	private FixedAspectRatioChartPanel chartPanelOne;
	private FixedAspectRatioChartPanel chartPanelTwo;
	
	private JSpinner minRatioSpinner;
	private JSpinner maxRatioSpinner;
	private JSpinner meshSizeSpinner;
	
	private JComboBox<AnalysisDataset> boxOne;
	private JComboBox<AnalysisDataset> boxTwo;
	
	public ConsensusCompareDialog(List<AnalysisDataset> datasets){
		this.datasets = datasets;
		
		this.setTitle("Consensus nucleus comparator");
		this.setLocationRelativeTo(null);
		this.setLayout(new BorderLayout());
		
		JPanel header = createHeader();
		this.add(header, BorderLayout.NORTH);
		
		JFreeChart chart = ConsensusNucleusChartFactory.makeEmptyNucleusOutlineChart();
		chartPanelOne = new FixedAspectRatioChartPanel(chart);
		chartPanelTwo = new FixedAspectRatioChartPanel(chart);
		this.add(chartPanelOne, BorderLayout.WEST);
		this.add(chartPanelTwo, BorderLayout.EAST);
		
		
		
		this.setModal(false);
		this.pack();
		log(Level.FINEST, "Displaying consensus comparator");
		runComparison();
		this.setVisible(true);
	}
	
	private JPanel createHeader(){
		JPanel panel = new JPanel(new FlowLayout());
		boxOne = new JComboBox<AnalysisDataset>();
		boxTwo = new JComboBox<AnalysisDataset>();
		for(AnalysisDataset d : datasets){
			boxOne.addItem(d);
			boxTwo.addItem(d);
		}
		boxOne.setSelectedItem(datasets.get(0));
		boxTwo.setSelectedItem(datasets.get(1));
		
		boxOne.addActionListener(this);
		boxTwo.addActionListener(this);
		
		panel.add(boxOne);
		panel.add(boxTwo);
		
		double minRatio = 0.9;
		double maxRatio = 1.1;
		
		

		SpinnerNumberModel minRatioModel = new SpinnerNumberModel(minRatio,
				0,
				1,
				0.01);
		minRatioSpinner = new JSpinner(minRatioModel);
		minRatioSpinner.addChangeListener(this);
		minRatioSpinner.setToolTipText("Lower gradient limit");
		JSpinner.NumberEditor numberEditor = new JSpinner.NumberEditor(minRatioSpinner,"0.00");
		minRatioSpinner.setEditor(numberEditor);
		
		SpinnerNumberModel maxRatioModel = new SpinnerNumberModel(maxRatio,
				1,
				100,
				0.01);
		maxRatioSpinner = new JSpinner(maxRatioModel);
		maxRatioSpinner.addChangeListener(this);
		maxRatioSpinner.setToolTipText("Upper gradient limit");
		JSpinner.NumberEditor maxNumberEditor = new JSpinner.NumberEditor(maxRatioSpinner,"0.00");
		maxRatioSpinner.setEditor(maxNumberEditor);
		
		SpinnerNumberModel meshSizeModel = new SpinnerNumberModel(10,
				1,
				100,
				1);
		meshSizeSpinner = new JSpinner(meshSizeModel);
		meshSizeSpinner.addChangeListener(this);
		meshSizeSpinner.setToolTipText("Mesh size");
		JSpinner.NumberEditor meshNumberEditor = new JSpinner.NumberEditor(meshSizeSpinner,"0.00");
		meshSizeSpinner.setEditor(meshNumberEditor);
		
		panel.add(new JLabel("Blue max"));
		panel.add(minRatioSpinner);
		panel.add(new JLabel("Red max"));
		panel.add(maxRatioSpinner);
		panel.add(new JLabel("Mesh distance"));
		panel.add(meshSizeSpinner);
		
		return panel;
	}
	
	private void runComparison(){
		AnalysisDataset one = (AnalysisDataset) boxOne.getSelectedItem();
		AnalysisDataset two = (AnalysisDataset) boxTwo.getSelectedItem();
		
		double minRatio = (double) minRatioSpinner.getValue();
		double maxRatio = (double) maxRatioSpinner.getValue();
		int    meshSize = (int)    meshSizeSpinner.getValue();
		
		
		JFreeChart chartOne;
		JFreeChart chartTwo;
		
		if(one.getCollection().hasConsensusNucleus() && two.getCollection().hasConsensusNucleus()){

			try {
				
				Nucleus n1 = one.getCollection().getConsensusNucleus();
				Nucleus n2 = two.getCollection().getConsensusNucleus();
	
				chartOne = OutlineChartFactory.createMeshChart(n1, n2, minRatio, maxRatio, meshSize);
				chartTwo = OutlineChartFactory.createMeshChart(n2, n1, minRatio, maxRatio, meshSize);
	
				
			} catch (Exception e){
				
				chartOne = ConsensusNucleusChartFactory.makeEmptyNucleusOutlineChart();
				chartTwo = ConsensusNucleusChartFactory.makeEmptyNucleusOutlineChart();
			}
			
			
			
		} else {
			
			chartOne = ConsensusNucleusChartFactory.makeEmptyNucleusOutlineChart();
			chartTwo = ConsensusNucleusChartFactory.makeEmptyNucleusOutlineChart();
			
		}
		
		chartPanelOne.setChart(chartOne);
		chartPanelTwo.setChart(chartTwo);
		
		chartPanelOne.restoreAutoBounds();
		chartPanelTwo.restoreAutoBounds();
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		runComparison();

	}

	@Override
	public void stateChanged(ChangeEvent arg0) {
		runComparison();
		
	}

}
