package gui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.logging.Level;

import javax.swing.BoxLayout;
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
	
//	private JSpinner minRatioSpinner;
	private JSpinner maxRatioSpinner;
	private JSpinner meshSizeSpinner;
	
	private JComboBox<AnalysisDataset> boxOne;
	private JComboBox<AnalysisDataset> boxTwo;
	
	public ConsensusCompareDialog(List<AnalysisDataset> datasets){
		this.datasets = datasets;
		
		this.setTitle("Consensus nucleus comparator");
		this.setLayout(new BorderLayout());
		
		JPanel header = createHeader();
		this.add(header, BorderLayout.NORTH);
		
		JFreeChart chart = ConsensusNucleusChartFactory.makeEmptyNucleusOutlineChart();
		
		
		JPanel centrePanel = new JPanel();
		BoxLayout layout = new BoxLayout(centrePanel, BoxLayout.X_AXIS);
		centrePanel.setLayout(layout);
		
		chartPanelOne = new FixedAspectRatioChartPanel(chart);
		chartPanelTwo = new FixedAspectRatioChartPanel(chart);
		centrePanel.add(chartPanelOne);
		centrePanel.add(chartPanelTwo);
		
		this.add(centrePanel,BorderLayout.CENTER);
		
		
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
		
//		double minRatio = 0.9;
		double maxRatio = 0.5;
		
		

//		SpinnerNumberModel minRatioModel = new SpinnerNumberModel(minRatio,
//				0,
//				1,
//				0.01);
//		minRatioSpinner = new JSpinner(minRatioModel);
//		minRatioSpinner.addChangeListener(this);
//		minRatioSpinner.setToolTipText("Lower gradient limit");
//		JSpinner.NumberEditor numberEditor = new JSpinner.NumberEditor(minRatioSpinner,"0.00");
//		minRatioSpinner.setEditor(numberEditor);
		
		SpinnerNumberModel maxRatioModel = new SpinnerNumberModel(maxRatio,
				0,
				10,
				0.01);
		maxRatioSpinner = new JSpinner(maxRatioModel);
		maxRatioSpinner.addChangeListener(this);
		maxRatioSpinner.setToolTipText("Gradient colour log2 ratio limit");
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
		
//		panel.add(new JLabel("Blue max"));
//		panel.add(minRatioSpinner);
		panel.add(new JLabel("Log2 ratio max"));
		panel.add(maxRatioSpinner);
		panel.add(new JLabel("Mesh distance"));
		panel.add(meshSizeSpinner);
		
		return panel;
	}
	
	private void runComparison(){
		AnalysisDataset one = (AnalysisDataset) boxOne.getSelectedItem();
		AnalysisDataset two = (AnalysisDataset) boxTwo.getSelectedItem();
		
		double logRatio = (double) maxRatioSpinner.getValue();
		int    meshSize = (int)    meshSizeSpinner.getValue();
		
		
		JFreeChart chartOne;
		JFreeChart chartTwo;
		
		if(one.getCollection().hasConsensusNucleus() && two.getCollection().hasConsensusNucleus()){

			try {
				
				Nucleus n1 = one.getCollection().getConsensusNucleus();
				Nucleus n2 = two.getCollection().getConsensusNucleus();
	
				chartOne = OutlineChartFactory.createMeshChart(n1, n2, logRatio, meshSize);
				chartTwo = OutlineChartFactory.createMeshChart(n2, n1, logRatio, meshSize);
	
				
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
