package gui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.logging.Level;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jfree.chart.JFreeChart;

import analysis.AnalysisDataset;
import analysis.mesh.NucleusMesh;
import charting.charts.ConsensusNucleusChartFactory;
import charting.charts.OutlineChartFactory;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import components.nuclei.Nucleus;
import gui.LoadingIconDialog;
import gui.components.ExportableChartPanel;
import gui.components.FixedAspectRatioChartPanel;
import gui.components.panels.DatasetSelectionPanel;

/**
 * A dialog window allowing comparisons between the consensus nuclei of multiple collections.
 * @author ben
 *
 */
@SuppressWarnings("serial")
public class ConsensusCompareDialog extends LoadingIconDialog implements ActionListener, ChangeListener, ItemListener {
	
	private List<AnalysisDataset> datasets;
	private FixedAspectRatioChartPanel chartPanelOne;
	private FixedAspectRatioChartPanel chartPanelTwo;
	
	private ExportableChartPanel histoOne;
	private ExportableChartPanel histoTwo;

	private JSpinner maxRatioSpinner;
	private JSpinner meshSizeSpinner;
	
	private JCheckBox showAnnotationsBox;
	private JCheckBox showEdgesBox;
	private JCheckBox showFacesBox;
		
	private DatasetSelectionPanel boxOne;
	private DatasetSelectionPanel boxTwo;
	
	public ConsensusCompareDialog(List<AnalysisDataset> datasets){
		super();
		finest("Creating consensus comparison dialog");
		this.datasets = datasets;
		
		this.setTitle("Consensus nucleus comparator");
		this.setLayout(new BorderLayout());
		
		JPanel header = createHeader();
		finest("Created consensus compare header");
		this.add(header, BorderLayout.NORTH);
		
		JFreeChart chart = ConsensusNucleusChartFactory.makeEmptyNucleusOutlineChart();
		finest("Created empty chart");
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
		centrePanel.addComponentListener( new ComponentAdapter(){
			
			@Override
			public void componentResized(ComponentEvent arg0) {
				chartPanelOne.restoreAutoBounds();
				chartPanelTwo.restoreAutoBounds();
				
			}
		});
		
		
		histoOne = new ExportableChartPanel(chart);
		histoTwo = new ExportableChartPanel(chart);
		
		histoPanel.add(histoOne);	
		histoPanel.add(histoTwo);

		
		/*
		 * Add the chart panels to the centre panel
		 */
		centrePanel.add(meshPanel);
		centrePanel.add(histoPanel);
		this.add(centrePanel,BorderLayout.CENTER);
		finest("Created panels");
		
		this.setModal(false);
		this.pack();
		finest("Displaying consensus comparator");
		runComparison();
		this.setVisible(true);
	}
	
	private JPanel createHeader(){
		JPanel panel = new JPanel(new FlowLayout());
		
		boxOne = new DatasetSelectionPanel(datasets);
		boxOne.setSelectedDataset(datasets.get(0));
		boxTwo = new DatasetSelectionPanel(datasets);
		boxOne.setSelectedDataset(datasets.get(1));
		
		boxOne.addActionListener(this);
		boxTwo.addActionListener(this);
		
		panel.add(boxOne);
		panel.add(boxTwo);
		
		finest("Added dataset boxes");

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
				2,
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
		finest("Added log2 and mesh size boxes");
		
		showAnnotationsBox = new JCheckBox("Show labels", false);
		showAnnotationsBox.addItemListener(this);
		panel.add(showAnnotationsBox);
		finest("Added annotations checkbox");
		
		showEdgesBox = new JCheckBox("Show edges", true);
		showEdgesBox.addItemListener(this);
		panel.add(showEdgesBox);
		finest("Added edges checkbox");
		
		showFacesBox = new JCheckBox("Show faces", false);
		showFacesBox.addItemListener(this);
		panel.add(showFacesBox);
		finest("Added faces checkbox");
		
		
//		panel.add(getLoadingLabel());
		
		return panel;
	}
	
	private void runComparison(){
		
		setLoading(true);
		AnalysisDataset one = boxOne.getSelectedDataset();
		AnalysisDataset two = boxTwo.getSelectedDataset();
		
		double logRatio = (double) maxRatioSpinner.getValue();
		int    meshSize = (int)    meshSizeSpinner.getValue();
		
		boolean showAnnotations = showAnnotationsBox.isSelected();
		boolean showFaces       = showFacesBox.isSelected();
		boolean showEdges       = showEdgesBox.isSelected();
		
		
		JFreeChart chartOne;
		JFreeChart chartTwo;
		JFreeChart histoChartOne;
		JFreeChart histoChartTwo;

		
		if(one.getCollection().hasConsensusNucleus() && two.getCollection().hasConsensusNucleus()){

			try {
				
				Nucleus n1 = one.getCollection().getConsensusNucleus();
				Nucleus n2 = two.getCollection().getConsensusNucleus();
				
				NucleusMesh mesh1 = new NucleusMesh(n1, meshSize);
				NucleusMesh mesh2 = new NucleusMesh(n2, mesh1);
				
				
				NucleusMesh comparison1 = mesh1.compareTo(mesh2);
				NucleusMesh comparison2 = mesh2.compareTo(mesh1);
				
				/*
				 * Create log2 histograms for the entire pairwise mesh
				 */
				
				histoChartOne = OutlineChartFactory.createMeshHistogram(comparison1);
				histoChartTwo = OutlineChartFactory.createMeshHistogram(comparison2);
				
				
				ChartOptions options = new ChartOptionsBuilder()
					.setShowAnnotations(showAnnotations)
					.setShowMeshFaces(showFaces)
					.setShowMeshEdges(showEdges)
					.build();
	
				chartOne = OutlineChartFactory.createMeshChart(comparison1, logRatio, options);
				chartTwo = OutlineChartFactory.createMeshChart(comparison2, logRatio, options);
	
				
			} catch (Exception e){
				
				chartOne = ConsensusNucleusChartFactory.makeErrorNucleusOutlineChart();
				chartTwo = ConsensusNucleusChartFactory.makeErrorNucleusOutlineChart();
				
				histoChartOne = ConsensusNucleusChartFactory.makeErrorNucleusOutlineChart();
				histoChartTwo = ConsensusNucleusChartFactory.makeErrorNucleusOutlineChart();

				logError("Error creating mesh chart", e);
			}
			
			
			
		} else {
			
			chartOne = ConsensusNucleusChartFactory.makeEmptyNucleusOutlineChart();
			chartTwo = ConsensusNucleusChartFactory.makeEmptyNucleusOutlineChart();
			
			histoChartOne = ConsensusNucleusChartFactory.makeEmptyNucleusOutlineChart();
			histoChartTwo = ConsensusNucleusChartFactory.makeEmptyNucleusOutlineChart();
			
		}
		
		chartPanelOne.setChart(chartOne);
		chartPanelTwo.setChart(chartTwo);
		
		chartPanelOne.restoreAutoBounds();
		chartPanelTwo.restoreAutoBounds();
		
		histoOne.setChart(histoChartOne);
		histoTwo.setChart(histoChartTwo);
		
		
		setLoading(false);
	}
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		runComparison();
	}

	@Override
	public void stateChanged(ChangeEvent arg0) {
		runComparison();
		
	}

	@Override
	public void itemStateChanged(ItemEvent arg0) {
		runComparison();
	}

}
