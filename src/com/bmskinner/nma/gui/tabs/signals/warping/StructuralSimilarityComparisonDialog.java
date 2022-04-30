package com.bmskinner.nma.gui.tabs.signals.warping;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.ChartPanel;

import com.bmskinner.nma.analysis.image.MultiScaleStructuralSimilarityIndex;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.components.ExportableTable;
import com.bmskinner.nma.gui.dialogs.LoadingIconDialog;
import com.bmskinner.nma.visualisation.charts.ViolinChartFactory;
import com.bmskinner.nma.visualisation.charts.panels.ExportableChartPanel;
import com.bmskinner.nma.visualisation.tables.AbstractTableCreator;
import com.bmskinner.nma.visualisation.tables.SSIMTableModel;

/**
 * Calculate and display structural similarities between images
 * 
 * @author ben
 * @since 1.15.0
 *
 */
public class StructuralSimilarityComparisonDialog extends LoadingIconDialog {

	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = Logger.getLogger(StructuralSimilarityComparisonDialog.class.getName());

	private static final String DIALOG_TITLE = "MS-SSIM* scores";

	private final ExportableTable comparisonTable;

	private final ChartPanel chartPanel;

	private final JProgressBar progressBar = new JProgressBar(0, 100);

	/** The MS-SSIM calculator */
	private MultiScaleStructuralSimilarityIndex msi = new MultiScaleStructuralSimilarityIndex();

	public StructuralSimilarityComparisonDialog(@NonNull final List<IAnalysisDataset> datasets) {
		super();

		chartPanel = new ExportableChartPanel(ViolinChartFactory.createEmptyChart());
		comparisonTable = new ExportableTable(AbstractTableCreator.createLoadingTable());

		JPanel centrePanel = createCentrePanel();

		setLayout(new BorderLayout());
//		add(headerPanel, BorderLayout.NORTH);
		add(centrePanel, BorderLayout.CENTER);

		setTitle(DIALOG_TITLE);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setModal(false);

		try {
			ThreadManager.getInstance().execute(() -> {
				TableModel compModel = new SSIMTableModel(datasets);
				comparisonTable.setModel(compModel);
				comparisonTable.setRowSorter(new TableRowSorter(compModel));
			});

		} catch (Exception e) {
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
//		JPanel perCellPanel = createPerCellPanel();

		JScrollPane scrollPane = new JScrollPane(comparisonTable);
		scrollPane.setColumnHeaderView(comparisonTable.getTableHeader());

		tablePanel.add(scrollPane, BorderLayout.CENTER);
		tablePanel.add(createHeaderPanel(), BorderLayout.NORTH);

		centrePanel.add(tablePanel, BorderLayout.CENTER);
//		centrePanel.add(perCellPanel, BorderLayout.EAST);

		return centrePanel;
	}

//	private JPanel createPerCellPanel() {
//		JPanel panel = new JPanel(new BorderLayout());
//		panel.add(createPerCellHeaderPanel(), BorderLayout.NORTH);
//		panel.add(chartPanel, BorderLayout.CENTER);
//		return panel;
//
//	}

//	private JPanel createPerCellHeaderPanel() {
//		JPanel panel = new JPanel();
//		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
//
//		JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
//		JLabel compareLabel = new JLabel(
//				"If the dataset has >1 signal group, generate MS-SSIM* scores per-cell " + "between each signal group");
//		JButton runPerCellBtn = new JButton("Run");
//		runPerCellBtn.addActionListener(e -> makePerCellCharts());
//
//		headerPanel.add(runPerCellBtn);
//		headerPanel.add(compareLabel);
//
//		panel.add(headerPanel);
//
//		JPanel progressPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
//		progressPanel.add(getLoadingLabel());
//		progressPanel.add(progressBar);
//		progressBar.setVisible(false);
//
//		panel.add(progressPanel);
//		return panel;
//	}

//	private void makePerCellCharts() {
//		LOGGER.fine("Creating per cell charts");
//
//		chartPanel.setChart(ViolinChartFactory.createLoadingChart());
//		progressBar.setVisible(true);
//		PerCellMSSSIMCalculationMethod calc = new PerCellMSSSIMCalculationMethod(model);
//		calc.addPropertyChangeListener(evt -> {
//			if (evt.getNewValue() instanceof Integer) {
//				int percent = (Integer) evt.getNewValue(); // should be percent
//				if (percent >= 0 && percent <= 100) {
//					if (progressBar.isIndeterminate()) {
//						progressBar.setIndeterminate(false);
//					}
//					progressBar.setValue(percent);
//				}
//			}
//
//			if (IAnalysisWorker.FINISHED_MSG.equals(evt.getPropertyName())) {
//				progressBar.setValue(0);
//				progressBar.setVisible(false);
//				setLoadingLabelText("");
//
//				Map<ViolinKey, List<MSSIMScore>> scores;
//				try {
//					scores = calc.get();
//					chartPanel.setChart(makeCharts(scores));
//				} catch (InterruptedException | ExecutionException e) {
//					LOGGER.log(Loggable.STACK, "Error making per cell MS-SSIM* scores", e);
//					chartPanel.setChart(ViolinChartFactory.createErrorChart());
//				}
//
//			}
//		});
//		setLoadingLabelText("Generating pairwise comparison plots...");
//		LOGGER.fine("Executing pairwise method");
//		ThreadManager.getInstance().execute(calc);
//
//	}

//	private JFreeChart makeCharts(Map<ViolinKey, List<MSSIMScore>> scores) {
//
//		if (scores.isEmpty())
//			return AbstractChartFactory.createEmptyChart();
//
//		ViolinCategoryDataset ds = new ViolinCategoryDataset();
//		for (ViolinKey key : scores.keySet()) {
//			List<Double> msssims = scores.get(key).stream().map(s -> s.msSsimIndex).collect(Collectors.toList());
//			ds.add(msssims, key.colKey, key.rowKey);
//		}
//
//		ViolinRenderer renderer = new ViolinRenderer();
//		CategoryAxis categoryAxis = new CategoryAxis("Sample");
//		NumberAxis valueAxis = new NumberAxis("Score");
//		valueAxis.setAutoRangeIncludesZero(true);
//		CategoryPlot plot = new CategoryPlot(ds, categoryAxis, valueAxis, renderer);
//		valueAxis.setRange(0, 1);
//		return new JFreeChart("", JFreeChart.DEFAULT_TITLE_FONT, plot, true);
//	}

}
