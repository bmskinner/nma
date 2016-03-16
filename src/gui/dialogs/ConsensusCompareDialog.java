package gui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.logging.Level;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.jfree.chart.JFreeChart;

import analysis.AnalysisDataset;
import charting.charts.ConsensusNucleusChartFactory;
import charting.charts.OutlineChartFactory;
import components.nuclei.Nucleus;
import gui.LoadingIconDialog;
import gui.components.FixedAspectRatioChartPanel;

@SuppressWarnings("serial")
public class ConsensusCompareDialog extends LoadingIconDialog implements ActionListener {
	
	private List<AnalysisDataset> datasets;
	private FixedAspectRatioChartPanel chartPanelOne;
	private FixedAspectRatioChartPanel chartPanelTwo;
	
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
		
		return panel;
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {

		AnalysisDataset one = (AnalysisDataset) boxOne.getSelectedItem();
		AnalysisDataset two = (AnalysisDataset) boxTwo.getSelectedItem();
		
		JFreeChart chartOne;
		JFreeChart chartTwo;
		
		if(one.getCollection().hasConsensusNucleus() && two.getCollection().hasConsensusNucleus()){
			
			
			
			try {
				
				Nucleus n1 = one.getCollection().getConsensusNucleus();
				Nucleus n2 = two.getCollection().getConsensusNucleus();
	
				chartOne = OutlineChartFactory.createMeshChart(n1, n2);
				chartTwo = OutlineChartFactory.createMeshChart(n2, n1);
	
				
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

}
