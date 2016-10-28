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
import analysis.IAnalysisDataset;
import analysis.mesh.NucleusMesh;
import charting.charts.ConsensusNucleusChartFactory;
import charting.charts.OutlineChartFactory;
import charting.charts.panels.ExportableChartPanel;
import charting.options.DefaultChartOptions;
import charting.options.ChartOptionsBuilder;
import components.nuclei.Nucleus;
import gui.LoadingIconDialog;
import gui.components.panels.DatasetSelectionPanel;

/**
 * A dialog window allowing comparisons between the consensus nuclei of multiple collections.
 * @author ben
 *
 */
@SuppressWarnings("serial")
public class ConsensusCompareDialog extends LoadingIconDialog implements ActionListener, ChangeListener, ItemListener {
	
	private List<IAnalysisDataset> datasets;
	private ExportableChartPanel chartPanelOne;
	private ExportableChartPanel chartPanelTwo;
	
	private ExportableChartPanel histoOne;
	private ExportableChartPanel histoTwo;

	private JSpinner maxRatioSpinner;
	private JSpinner meshSizeSpinner;
	
	private JCheckBox showAnnotationsBox;
	private JCheckBox showEdgesBox;
	private JCheckBox showFacesBox;
		
	private DatasetSelectionPanel boxOne;
	private DatasetSelectionPanel boxTwo;
	
	public ConsensusCompareDialog(List<IAnalysisDataset> datasets){
		super();
		finest("Creating consensus comparison dialog");
		this.datasets = datasets;
		
		this.setTitle("Consensus nucleus comparator");
		this.setLayout(new BorderLayout());
		
		JPanel header = createHeader();
		finest("Created consensus compare header");
		this.add(header, BorderLayout.NORTH);
		
		JFreeChart chart = ConsensusNucleusChartFactory.makeEmptyChart();
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
		chartPanelOne = new ExportableChartPanel(chart);
		chartPanelTwo = new ExportableChartPanel(chart);
		chartPanelOne.setFixedAspectRatio(true);
		chartPanelTwo.setFixedAspectRatio(true);
		
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
		
		JPanel headerPanel = new JPanel();
		headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
		
		JPanel upperPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JPanel lowerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		
		
		boxOne = new DatasetSelectionPanel(datasets);
		boxOne.setSelectedDataset(datasets.get(0));
		
		boxTwo = new DatasetSelectionPanel(datasets);
		boxTwo.setSelectedDataset(datasets.get(1));
		
		boxOne.addActionListener(this);
		boxTwo.addActionListener(this);
		
		upperPanel.add(new JLabel("Dataset one"));
		upperPanel.add(boxOne);
		lowerPanel.add(new JLabel("Dataset two"));
		lowerPanel.add(boxTwo);
		
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
		
		lowerPanel.add(new JLabel("Log2 ratio max"));
		lowerPanel.add(maxRatioSpinner);
		upperPanel.add(new JLabel("Mesh distance"));
		upperPanel.add(meshSizeSpinner);
		finest("Added log2 and mesh size boxes");
		
		showAnnotationsBox = new JCheckBox("Show labels", false);
		showAnnotationsBox.addItemListener(this);
		lowerPanel.add(showAnnotationsBox);
		finest("Added annotations checkbox");
		
		showEdgesBox = new JCheckBox("Show edges", true);
		showEdgesBox.addItemListener(this);
		lowerPanel.add(showEdgesBox);
		finest("Added edges checkbox");
		
		showFacesBox = new JCheckBox("Show faces", false);
		showFacesBox.addItemListener(this);
		lowerPanel.add(showFacesBox);
		finest("Added faces checkbox");
		
		headerPanel.add(upperPanel);
		headerPanel.add(lowerPanel);
				
		return headerPanel;
	}
	
	private void runComparison(){
		
		setLoading(true);
		IAnalysisDataset one = boxOne.getSelectedDataset();
		IAnalysisDataset two = boxTwo.getSelectedDataset();
		
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
				
				
				DefaultChartOptions options = new ChartOptionsBuilder()
					.setShowAnnotations(showAnnotations)
					.setShowMeshFaces(showFaces)
					.setShowMeshEdges(showEdges)
					.build();
	
				OutlineChartFactory ocf = new OutlineChartFactory(options);
				chartOne = ocf.createMeshChart(comparison1, logRatio );
				chartTwo = ocf.createMeshChart(comparison2, logRatio);
	
				
			} catch (Exception e){
				
				chartOne = ConsensusNucleusChartFactory.makeErrorChart();
				chartTwo = ConsensusNucleusChartFactory.makeErrorChart();
				
				histoChartOne = ConsensusNucleusChartFactory.makeErrorChart();
				histoChartTwo = ConsensusNucleusChartFactory.makeErrorChart();

				log(Level.FINE, "Error creating mesh chart", e);
			}
			
			
			
		} else {
			
			chartOne = ConsensusNucleusChartFactory.makeEmptyChart();
			chartTwo = ConsensusNucleusChartFactory.makeEmptyChart();
			
			histoChartOne = ConsensusNucleusChartFactory.makeEmptyChart();
			histoChartTwo = ConsensusNucleusChartFactory.makeEmptyChart();
			
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
