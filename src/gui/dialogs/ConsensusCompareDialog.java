package gui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
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
import analysis.nucleus.NucleusMeshBuilder;
import analysis.nucleus.NucleusMeshBuilder.NucleusMesh;
import charting.charts.ConsensusNucleusChartFactory;
import charting.charts.OutlineChartFactory;
import components.nuclei.Nucleus;
import gui.LoadingIconDialog;
import gui.components.ExportableChartPanel;
import gui.components.FixedAspectRatioChartPanel;

/**
 * A dialog window allowing comparisons between the consensus nuclei of multiple collections.
 * @author ben
 *
 */
@SuppressWarnings("serial")
public class ConsensusCompareDialog extends LoadingIconDialog implements ActionListener, ChangeListener {
	
	private List<AnalysisDataset> datasets;
	private FixedAspectRatioChartPanel chartPanelOne;
	private FixedAspectRatioChartPanel chartPanelTwo;
	
	private ExportableChartPanel histoOne;
	private ExportableChartPanel histoTwo;
	
	private FixedAspectRatioChartPanel pointOne;
	private FixedAspectRatioChartPanel pointTwo;

	private JSpinner maxRatioSpinner;
	private JSpinner meshSizeSpinner;
	
	private JComboBox<AnalysisDataset> boxOne;
	private JComboBox<AnalysisDataset> boxTwo;
	
	public ConsensusCompareDialog(List<AnalysisDataset> datasets){
		super();
		this.datasets = datasets;
		
		this.setTitle("Consensus nucleus comparator");
		this.setLayout(new BorderLayout());
		
		JPanel header = createHeader();
		this.add(header, BorderLayout.NORTH);
		
		JFreeChart chart = ConsensusNucleusChartFactory.makeEmptyNucleusOutlineChart();
		
		/*
		 * Create a central panel for all charts
		 */
		JPanel centrePanel = new JPanel();
		BoxLayout yLayout = new BoxLayout(centrePanel, BoxLayout.Y_AXIS);
		centrePanel.setLayout(yLayout);
		
		/*
		 * Create a horizontal panel for meshes
		 */
		JPanel    meshPanel  = new JPanel();
		BoxLayout meshLayout = new BoxLayout(meshPanel, BoxLayout.X_AXIS);
		meshPanel.setLayout(meshLayout);
		
		/*
		 * Create a horizontal panel for histograms
		 */
		JPanel    histoPanel  = new JPanel();
		BoxLayout histoLayout = new BoxLayout(histoPanel, BoxLayout.X_AXIS);
		histoPanel.setLayout(histoLayout);
		
		/*
		 * Add empty charts
		 */
		chartPanelOne = new FixedAspectRatioChartPanel(chart);
		chartPanelTwo = new FixedAspectRatioChartPanel(chart);
		meshPanel.add(chartPanelOne);
		meshPanel.add(chartPanelTwo);
		centrePanel.addComponentListener( new ComponentListener(){
			
			@Override
			public void componentResized(ComponentEvent arg0) {
				chartPanelOne.restoreAutoBounds();
				chartPanelTwo.restoreAutoBounds();
				
			}

			@Override
			public void componentHidden(ComponentEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void componentMoved(ComponentEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void componentShown(ComponentEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
		
		
		histoOne = new ExportableChartPanel(chart);
		histoTwo = new ExportableChartPanel(chart);
		
		pointOne = new FixedAspectRatioChartPanel(chart);
		pointTwo = new FixedAspectRatioChartPanel(chart);
		
		histoPanel.add(histoOne);
		histoPanel.add(pointOne);
		
		histoPanel.add(histoTwo);
		histoPanel.add(pointTwo);
		
		histoPanel.addComponentListener( new ComponentListener(){
			
			@Override
			public void componentResized(ComponentEvent arg0) {
				pointOne.restoreAutoBounds();
				pointTwo.restoreAutoBounds();
				
			}

			@Override
			public void componentHidden(ComponentEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void componentMoved(ComponentEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void componentShown(ComponentEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
		
		/*
		 * Add the chart panels to the centre panel
		 */
		centrePanel.add(meshPanel);
		centrePanel.add(histoPanel);
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

		double maxRatio = 0.5;

		
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
		JSpinner.NumberEditor meshNumberEditor = new JSpinner.NumberEditor(meshSizeSpinner,"0");
		meshSizeSpinner.setEditor(meshNumberEditor);
		
		panel.add(new JLabel("Log2 ratio max"));
		panel.add(maxRatioSpinner);
		panel.add(new JLabel("Mesh distance"));
		panel.add(meshSizeSpinner);
		
		panel.add(getLoadingLabel());
		
		return panel;
	}
	
	private void runComparison(){
		
		setLoading(true);
		AnalysisDataset one = (AnalysisDataset) boxOne.getSelectedItem();
		AnalysisDataset two = (AnalysisDataset) boxTwo.getSelectedItem();
		
		double logRatio = (double) maxRatioSpinner.getValue();
		int    meshSize = (int)    meshSizeSpinner.getValue();
		
		
		JFreeChart chartOne;
		JFreeChart chartTwo;
		JFreeChart histoChartOne;
		JFreeChart histoChartTwo;
		JFreeChart pointChartOne;
		JFreeChart pointChartTwo;
		
		NucleusMeshBuilder builder = new NucleusMeshBuilder();
		
		if(one.getCollection().hasConsensusNucleus() && two.getCollection().hasConsensusNucleus()){

			try {
				
				Nucleus n1 = one.getCollection().getConsensusNucleus();
				Nucleus n2 = two.getCollection().getConsensusNucleus();
				
				NucleusMesh comparison1 = builder.createComparisonMesh(n1, n2, meshSize);
				NucleusMesh comparison2 = builder.createComparisonMesh(n2, n1, meshSize);
				
				/*
				 * Create log2 histograms for the entire pairwise mesh
				 */
				
				histoChartOne = OutlineChartFactory.createMeshHistogram(comparison1);
				histoChartTwo = OutlineChartFactory.createMeshHistogram(comparison2);
				
				/*
				 * Create point cloud for edges inside the mesh
				 */
				comparison1.removeExternalEdges();
				comparison2.removeExternalEdges();
				
				pointChartOne = OutlineChartFactory.createMeshMidpointChart(comparison1, logRatio);
				pointChartTwo = OutlineChartFactory.createMeshMidpointChart(comparison2, logRatio);
				
				
				/*
				 * Remove overlapping edges for a clean chart
				 */
				comparison1.pruneOverlaps();
				comparison2.pruneOverlaps();
	
				chartOne = OutlineChartFactory.createMeshChart(comparison1, logRatio);
				chartTwo = OutlineChartFactory.createMeshChart(comparison2, logRatio);
	
				
			} catch (Exception e){
				
				chartOne = ConsensusNucleusChartFactory.makeErrorNucleusOutlineChart();
				chartTwo = ConsensusNucleusChartFactory.makeErrorNucleusOutlineChart();
				
				histoChartOne = ConsensusNucleusChartFactory.makeErrorNucleusOutlineChart();
				histoChartTwo = ConsensusNucleusChartFactory.makeErrorNucleusOutlineChart();
				
				pointChartOne = ConsensusNucleusChartFactory.makeErrorNucleusOutlineChart();
				pointChartTwo = ConsensusNucleusChartFactory.makeErrorNucleusOutlineChart();
				logError("Error creating mesh chart", e);
			}
			
			
			
		} else {
			
			chartOne = ConsensusNucleusChartFactory.makeEmptyNucleusOutlineChart();
			chartTwo = ConsensusNucleusChartFactory.makeEmptyNucleusOutlineChart();
			
			histoChartOne = ConsensusNucleusChartFactory.makeEmptyNucleusOutlineChart();
			histoChartTwo = ConsensusNucleusChartFactory.makeEmptyNucleusOutlineChart();
			
			pointChartOne = ConsensusNucleusChartFactory.makeEmptyNucleusOutlineChart();
			pointChartTwo = ConsensusNucleusChartFactory.makeEmptyNucleusOutlineChart();
			
		}
		
		chartPanelOne.setChart(chartOne);
		chartPanelTwo.setChart(chartTwo);
		
		chartPanelOne.restoreAutoBounds();
		chartPanelTwo.restoreAutoBounds();
		
		histoOne.setChart(histoChartOne);
		histoTwo.setChart(histoChartTwo);
		
		pointOne.setChart(pointChartOne);
		pointTwo.setChart(pointChartTwo);
		
		pointOne.restoreAutoBounds();
		pointTwo.restoreAutoBounds();
		
		
		
		setLoading(false);
	}
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		Thread thr = new Thread(    ()-> {	runComparison(); }   );
		thr.start();

	}

	@Override
	public void stateChanged(ChangeEvent arg0) {
		Thread thr = new Thread(    ()-> {	runComparison(); }   );
		thr.start();
		
	}

}
