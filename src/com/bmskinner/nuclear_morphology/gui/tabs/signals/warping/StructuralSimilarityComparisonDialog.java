package com.bmskinner.nuclear_morphology.gui.tabs.signals.warping;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;

import com.bmskinner.ViolinPlots.ViolinCategoryDataset;
import com.bmskinner.ViolinPlots.ViolinRenderer;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.image.MultiScaleStructuralSimilarityIndex;
import com.bmskinner.nuclear_morphology.analysis.image.MultiScaleStructuralSimilarityIndex.MSSIMScore;
import com.bmskinner.nuclear_morphology.analysis.image.PerCellMSSSIMCalculationMethod;
import com.bmskinner.nuclear_morphology.analysis.image.PerCellMSSSIMCalculationMethod.ViolinKey;
import com.bmskinner.nuclear_morphology.charting.charts.AbstractChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.ViolinChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.charting.datasets.tables.AbstractTableCreator;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;
import com.bmskinner.nuclear_morphology.gui.dialogs.LoadingIconDialog;
import com.bmskinner.nuclear_morphology.gui.tabs.signals.warping.SignalWarpingModel.ImageCache.WarpedImageKey;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.process.ImageProcessor;

/**
 * Calculate and display structural similarities between images
 * @author ben
 * @since 1.15.0
 *
 */
public class StructuralSimilarityComparisonDialog extends LoadingIconDialog {
	
	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = Logger.getLogger(StructuralSimilarityComparisonDialog.class.getName());
	
	private static final String DIALOG_TITLE = "MS-SSIM* scores";
	
	private final SignalWarpingModel model;
	private final ExportableTable comparisonTable;
	
	private final ChartPanel chartPanel;
	
	private final JProgressBar progressBar = new JProgressBar(0, 100);
	
	/** The MS-SSIM calculator */
	private MultiScaleStructuralSimilarityIndex msi = new MultiScaleStructuralSimilarityIndex();
	
	public StructuralSimilarityComparisonDialog(@NonNull final SignalWarpingModel model) {
		super();
		LOGGER.finer("Creating MS-SSIM dialog");
		this.model = model;
		chartPanel = new ExportableChartPanel(ViolinChartFactory.createEmptyChart());
		comparisonTable = new ExportableTable(AbstractTableCreator.createLoadingTable());
		
//		JPanel headerPanel = createHeaderPanel();
		JPanel centrePanel = createCentrePanel();
		
		setLayout(new BorderLayout());
//		add(headerPanel, BorderLayout.NORTH);
		add(centrePanel, BorderLayout.CENTER);
		
		setTitle(DIALOG_TITLE);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setModal(false);
		
		try {
			ThreadManager.getInstance().execute( () ->{
				TableModel compModel = createTableModel();
				comparisonTable.setModel(compModel);
				comparisonTable.setRowSorter(new TableRowSorter(compModel));
			});
						
		} catch(Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
			comparisonTable.setModel(AbstractTableCreator.createBlankTable());
		}
		validate();
		pack();
		setLocationRelativeTo(null);
		centerOnScreen();
		LOGGER.finer("Showing MS-SSIM dialog");
		setVisible(true);
	}
	
	private JPanel createHeaderPanel() {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panel.add(new JLabel("Showing full MS-SSIM* values for all possible warped image comparisons"));
		return panel;
	}
	
	private JPanel createCentrePanel() {
		
		JPanel tablePanel = new JPanel(new BorderLayout());
		JPanel centrePanel = new JPanel(new BorderLayout());
		JPanel perCellPanel = createPerCellPanel();

		JScrollPane scrollPane = new JScrollPane(comparisonTable);
		scrollPane.setColumnHeaderView(comparisonTable.getTableHeader());
		
		tablePanel.add(scrollPane, BorderLayout.CENTER);
		tablePanel.add(createHeaderPanel(), BorderLayout.NORTH);
		
		centrePanel.add(tablePanel, BorderLayout.CENTER);
		centrePanel.add(perCellPanel, BorderLayout.EAST);
		
        return centrePanel;
	}
	
	private JPanel createPerCellPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(createPerCellHeaderPanel(), BorderLayout.NORTH);
		panel.add(chartPanel, BorderLayout.CENTER);
		return panel;
		
	}
	
	private JPanel createPerCellHeaderPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		
		JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JLabel compareLabel = new JLabel("If the dataset has >1 signal group, generate MS-SSIM* scores per-cell "
				+ "between each signal group");
		JButton runPerCellBtn = new JButton("Run");
		runPerCellBtn.addActionListener(e->makePerCellCharts());
		
		headerPanel.add(runPerCellBtn);
		headerPanel.add(compareLabel);
		
		panel.add(headerPanel);
		
		JPanel progressPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		progressPanel.add(getLoadingLabel());
		progressPanel.add(progressBar);
		progressBar.setVisible(false);
		
		panel.add(progressPanel);
		return panel;
	}

	/**
	 * Create the MS-SSIM* table. Generate all pairwise
	 * combinations of MS-SSIM* scores excluding self-self
	 * comparisons and reciprocal comparisons.
	 * @return the MS-SSIM* table model
	 */
	private TableModel createTableModel() {
		DefaultTableModel compModel = new DefaultTableModel();
		Object[] columns = { "Source 1", "Signal 1", 
				"Source 2", "Signal 2", 
				"Target", "Luminance", "Contrast", "Structure", "MS-SSIM*"};
		compModel.setColumnIdentifiers(columns);
		
		for(CellularComponent c : model.getTargets())
			createTargetData(compModel, c);

		return compModel;
	}
	
	/**
	 * Create MS-SSIM* comparisons for all images warped onto the
	 * given target shape
	 * @param compModel the table model to add results to 
	 * @param c the target shape
	 */
	private void createTargetData(DefaultTableModel compModel, CellularComponent c) {
		LOGGER.finer("Creating comparisons for target shape "+c.getID());
		for(WarpedImageKey k1 : model.getKeys(c)) {
			for(WarpedImageKey k2 : model.getKeys(c)) {
				if(k1==k2)
					continue;
				LOGGER.finer("Comparing "+k1+" and "+k2);
				try {
					Object[] rowData = createRowData(k1, k2);
					if(!containsRow(rowData, compModel))
						compModel.addRow(rowData);
				} catch(Exception e) {
					LOGGER.log(Loggable.STACK, String.format("Error calculating MS-SSIM* for pair %s and %s: %s", k1, k2, e.getMessage()), e);
				}
			}
		}
	}
		
	/**
	 * Create an MS-SSIM* row based on the given warped image keys
	 * @param k1 the first key
	 * @param k2 the second key
	 * @return 
	 */
	private Object[] createRowData(WarpedImageKey k1, WarpedImageKey k2) {
		DecimalFormat df = new DecimalFormat("0.000");
		// choose the order of keys 
		List<WarpedImageKey> keys = new ArrayList<>();
		keys.add(k1);
		keys.add(k2);
		keys.sort((c1, c2)-> (c1.getSignalGroupName().compareTo(c2.getSignalGroupName())*10+c1.getTemplate().getName().compareTo(c2.getTemplate().getName())));


		ImageProcessor ip1 = model.getImage(keys.get(0));
		ImageProcessor ip2 = model.getImage(keys.get(1));
		LOGGER.finer( keys.get(0)+" vs "+keys.get(1));
		MSSIMScore score = msi.calculateMSSIM(ip1, ip2);

		return new Object[]  { keys.get(0).getTemplate().getName(), 
				keys.get(0).getSignalGroupName(), 
				keys.get(1).getTemplate().getName(), 
				keys.get(1).getSignalGroupName(), 
				keys.get(1).getTargetName(), 
				df.format(score.luminance), df.format(score.contrast),  df.format(score.structure),  df.format(score.msSsimIndex) };
	}
	
	/**
	 * Test if a table model already contains a row with the given data
	 * @param rowData the data to check
	 * @param tableModel the model to look within
	 * @return true if the row is present in the table model, false otherwise
	 */
	private boolean containsRow(Object[] rowData, TableModel tableModel) {
		for(int row=0; row<tableModel.getRowCount(); row++) {
			boolean isMatching = true;
			for(int col=0; col<tableModel.getColumnCount(); col++)
				isMatching &= tableModel.getValueAt(row, col).equals(rowData[col]);
			if(isMatching)
				return true;
		}
		return false;
	}
	
		
	private void makePerCellCharts() {
		LOGGER.fine("Creating per cell charts");
		
		chartPanel.setChart(ViolinChartFactory.createLoadingChart());
		progressBar.setVisible(true);
		PerCellMSSSIMCalculationMethod calc = new PerCellMSSSIMCalculationMethod(model);
		calc.addPropertyChangeListener( evt ->{
			if (evt.getNewValue() instanceof Integer) {
	            int percent = (Integer) evt.getNewValue(); // should be percent
	            if (percent >= 0 && percent <= 100) {
	                if (progressBar.isIndeterminate()) {
	                    progressBar.setIndeterminate(false);
	                }
	                progressBar.setValue(percent);
	            }
	        }

	        if (IAnalysisWorker.FINISHED_MSG.equals(evt.getPropertyName())) {
	            progressBar.setValue(0);
	            progressBar.setVisible(false);
				setLoadingLabelText("");
				
				Map<ViolinKey, List<MSSIMScore>> scores;
				try {
					scores = calc.get();
					chartPanel.setChart(makeCharts(scores));
				} catch (InterruptedException | ExecutionException e) {
					LOGGER.log(Loggable.STACK, "Error making per cell MS-SSIM* scores", e);
					chartPanel.setChart(ViolinChartFactory.createErrorChart());
				}
				
	        }
		});
		setLoadingLabelText("Generating pairwise comparison plots...");
		LOGGER.fine("Executing pairwise method");
		ThreadManager.getInstance().execute(calc);		

	}
	
	private JFreeChart makeCharts(Map<ViolinKey, List<MSSIMScore>> scores) {
		
		if(scores.isEmpty())
			return AbstractChartFactory.createEmptyChart();
		
		ViolinCategoryDataset ds = new ViolinCategoryDataset();
		for(ViolinKey key : scores.keySet()) {
			List<Double> msssims = scores.get(key).stream().map(s->s.msSsimIndex).collect(Collectors.toList());
			ds.add(msssims, key.colKey, key.rowKey);
		}

		ViolinRenderer renderer = new ViolinRenderer();
		CategoryAxis categoryAxis = new CategoryAxis("Sample");
		NumberAxis valueAxis = new NumberAxis("Score");
		valueAxis.setAutoRangeIncludesZero(true);
		CategoryPlot plot = new CategoryPlot(ds, categoryAxis, valueAxis, renderer);
		valueAxis.setRange(0, 1);
		return new JFreeChart("", JFreeChart.DEFAULT_TITLE_FONT, plot, true);
	}

}
