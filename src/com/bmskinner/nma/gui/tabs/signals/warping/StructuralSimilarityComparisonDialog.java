package com.bmskinner.nma.gui.tabs.signals.warping;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.Level;

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
import com.bmskinner.nma.gui.components.panels.ExportableChartPanel;
import com.bmskinner.nma.gui.dialogs.MessagingDialog;
import com.bmskinner.nma.visualisation.charts.ViolinChartFactory;
import com.bmskinner.nma.visualisation.tables.AbstractTableCreator;
import com.bmskinner.nma.visualisation.tables.SSIMTableModel;

/**
 * Calculate and display structural similarities between images
 * 
 * @author Ben Skinner
 * @since 1.15.0
 *
 */
public class StructuralSimilarityComparisonDialog extends MessagingDialog {

	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = Logger
			.getLogger(StructuralSimilarityComparisonDialog.class.getName());

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
		setVisible(true);
	}

	private JPanel createHeaderPanel() {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panel.add(new JLabel(
				"Showing full MS-SSIM* values for all possible warped image comparisons"));
		return panel;
	}

	private JPanel createCentrePanel() {

		JPanel tablePanel = new JPanel(new BorderLayout());
		JPanel centrePanel = new JPanel(new BorderLayout());

		JScrollPane scrollPane = new JScrollPane(comparisonTable);
		scrollPane.setColumnHeaderView(comparisonTable.getTableHeader());

		tablePanel.add(scrollPane, BorderLayout.CENTER);
		tablePanel.add(createHeaderPanel(), BorderLayout.NORTH);

		centrePanel.add(tablePanel, BorderLayout.CENTER);

		return centrePanel;
	}
}
