package com.bmskinner.nma.gui.tabs.signals.warping;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.image.MultiScaleStructuralSimilarityIndex;
import com.bmskinner.nma.analysis.image.MultiScaleStructuralSimilarityIndex.MSSIMScore;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.signals.IWarpedSignal;
import com.bmskinner.nma.core.DatasetListManager;
import com.bmskinner.nma.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nma.gui.Labels;
import com.bmskinner.nma.gui.components.ExportableTable;
import com.bmskinner.nma.gui.events.NuclearSignalUpdatedListener;
import com.bmskinner.nma.gui.tabs.TableDetailPanel;
import com.bmskinner.nma.visualisation.options.TableOptions;
import com.bmskinner.nma.visualisation.options.TableOptionsBuilder;
import com.bmskinner.nma.visualisation.tables.AbstractTableCreator;
import com.bmskinner.nma.visualisation.tables.SignalWarpingTableModel;

public class SignalWarpingTablePanel extends TableDetailPanel implements NuclearSignalUpdatedListener {
	private static final Logger LOGGER = Logger.getLogger(SignalWarpingTablePanel.class.getName());

	private ExportableTable table;
	private static final String PSEUDOCOLOUR_LBL = "Pseudocolour signals";
	private static final String THRESHOLD_LBL = "Threshold";
	private static final String EXPORT_LBL = "Export image";

	private static final String PSEUDOCOLOUR_TOOLTIP = "Peudocoloured signals using the signal group colour";
	private static final String THRESHOLD_TOOLTIP = "Threshold the display to remove fainter signal";
	private static final String EXPORT_TOOLTIP = "Export the image with optimised colours";

	/** Show pseudocolours of warped images */
	private JCheckBox isPseudocolourBox;

	/** Adjust thresholds of warped images */
	private JSlider thresholdSlider;

	private JButton exportButton;

	private final JLabel ssimLabel = new JLabel("");
	private final JButton ssimBtn = new JButton("Full MS-SSIM*");

	private List<WarpedSignalSelectionChangeListener> listeners = new ArrayList<>();

	public SignalWarpingTablePanel() {

		setLayout(new BorderLayout());

		table = new ExportableTable(AbstractTableCreator.createBlankTable()) {
			@Override
			public Class<?> getColumnClass(int column) {
				// Render true/false column as checkbox
				if (column >= 3 && column <= 5)
					return Boolean.class;
				else
					return super.getColumnClass(column);
			}
		};

		table.setCellSelectionEnabled(false);
		table.setRowSelectionAllowed(true);
		table.setColumnSelectionAllowed(false);
		table.setAutoCreateRowSorter(true);

		ListSelectionModel rowModel = table.getSelectionModel();
		rowModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		rowModel.addListSelectionListener(e -> {
			if (e.getValueIsAdjusting())
				return;
			ssimLabel.setText("");
			int[] selectedRow = table.getSelectedRows();
			if (table.getModel()instanceof SignalWarpingTableModel model) {
				List<IWarpedSignal> images = new ArrayList<>();
				for (int i : selectedRow) {
					images.add(model.getWarpedSignal(i));
				}
				fireWarpedSignalSelectionChanged(images);

				if (selectedRow.length == 2) {
					IWarpedSignal w0 = model.getWarpedSignal(selectedRow[0]);
					IWarpedSignal w1 = model.getWarpedSignal(selectedRow[1]);

					// Only compare images with the same target
					if (w0.target().getID().equals(w1.target().getID())) {
						MultiScaleStructuralSimilarityIndex msi = new MultiScaleStructuralSimilarityIndex();
						MSSIMScore values = msi.calculateMSSIM(w0.toImage(), w1.toImage());
						ssimLabel.setText("MS-SSIM*: " + values.toString());
					} else {
						ssimLabel.setText("");
					}
				}
			}

			if (selectedRow.length == 1) {
				IWarpedSignal w = getSelectedImages(selectedRow).get(0);
				isPseudocolourBox.setEnabled(true);
				thresholdSlider.setEnabled(true);
				isPseudocolourBox.setSelected(w.isPseudoColour());
				thresholdSlider.setValue(255 - w.displayThreshold());
			} else {
				isPseudocolourBox.setEnabled(false);
				thresholdSlider.setEnabled(false);
			}

			exportButton.setEnabled(selectedRow.length >= 1);

		});

		// Handle mouse events
		table.addMouseListener(new MouseAdapter() {

			private static final int DOUBLE_CLICK = 2;

			@Override
			public void mouseClicked(MouseEvent e) {

				if (table.getModel()instanceof SignalWarpingTableModel model) {
					int row = table.rowAtPoint(e.getPoint());
					int col = table.columnAtPoint(e.getPoint());

					// Change signal group colour
					if (e.getClickCount() == DOUBLE_CLICK && col == 7) {
						try {
							Color oldColor = model.getWarpedSignal(row).colour();
							Color newColor = getInputSupplier().requestColor(Labels.Signals.CHOOSE_SIGNAL_COLOUR,
									oldColor);
							model.getWarpedSignal(row).setColour(newColor);
							updateTableAndFireVisChange(new int[] { row });
						} catch (RequestCancelledException e1) {
							// No action, user cancelled
						}
					}
				}
			}
		});

		JScrollPane scrollPane = new JScrollPane(table);

		add(createHeader(), BorderLayout.NORTH);
		add(scrollPane, BorderLayout.CENTER);
		uiController.addNuclearSignalUpdatedListener(this);
	}

	private void updateTableAndFireVisChange(int[] rows) {
		cache.clear(makeOptions());
		setTable(makeOptions());
		fireWarpedSignalVisualisationChanged(getSelectedImages(rows));
	}

	private JPanel createHeader() {
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT));
		panel.add(createDisplaySettingsPanel());
		panel.add(Box.createHorizontalStrut(10));
		panel.add(Box.createHorizontalGlue());
		panel.add(createMSSSIMPanel());
		panel.add(Box.createHorizontalGlue());
		return panel;
	}

	private JPanel createDisplaySettingsPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT));

		isPseudocolourBox = new JCheckBox(PSEUDOCOLOUR_LBL, true);
		isPseudocolourBox.setToolTipText(PSEUDOCOLOUR_TOOLTIP);
		isPseudocolourBox.addActionListener(e -> {

			List<IWarpedSignal> ws = getSelectedImages(table.getSelectedRows());
			for (IWarpedSignal w : ws) {
				w.setPseudoColour(isPseudocolourBox.isSelected());
			}
			fireWarpedSignalVisualisationChanged(ws);
		});
		isPseudocolourBox.setEnabled(false);
		panel.add(isPseudocolourBox);

		thresholdSlider = new JSlider(0, 255);
		thresholdSlider.setToolTipText(THRESHOLD_TOOLTIP);
		thresholdSlider.setVisible(true);
		thresholdSlider.setValue(0);
		thresholdSlider.addChangeListener(e -> {

			List<IWarpedSignal> ws = getSelectedImages(table.getSelectedRows());
			for (IWarpedSignal w : ws) {
				w.setDisplayThreshold(255 - thresholdSlider.getValue());
			}
			fireWarpedSignalVisualisationChanged(ws);
		});
		thresholdSlider.setEnabled(false);
		panel.add(thresholdSlider);

		return panel;
	}

	private JPanel createMSSSIMPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT));

		exportButton = new JButton(EXPORT_LBL);
		exportButton.setToolTipText(EXPORT_TOOLTIP);
		exportButton.setEnabled(false);
		exportButton.addActionListener(e -> new WarpedSignalExportDialog(getSelectedImages(table.getSelectedRows())));

		panel.add(exportButton);

		ssimBtn.addActionListener(

				e -> new StructuralSimilarityComparisonDialog(
						DatasetListManager.getInstance().getAllDatasets().stream().toList()));

		ssimLabel.setMinimumSize(new Dimension(100, 10));
		ssimBtn.setEnabled(false);
		panel.add(ssimBtn);
		panel.add(ssimLabel);

		return panel;
	}

	private List<IWarpedSignal> getSelectedImages(int[] rows) {
		List<IWarpedSignal> result = new ArrayList<>();
		if (table.getModel()instanceof SignalWarpingTableModel m) {
			for (int i : rows)
				result.add(m.getWarpedSignal(i));
		}
		return result;
	}

	@Override
	protected TableModel createPanelTableType(@NonNull TableOptions options) {
		return new SignalWarpingTableModel(options.getDatasets());
	}

	@Override
	protected void updateSingle() {
		setTable(makeOptions());
		isPseudocolourBox.setEnabled(false);
		thresholdSlider.setEnabled(false);
		ssimLabel.setText("");
		ssimBtn.setEnabled(true);
	}

	private TableOptions makeOptions() {
		return new TableOptionsBuilder().setDatasets(getDatasets()).setTarget(table)
				.setColumnRenderer(7, new SignalWarpingTableCellRenderer()).build();
	}

	@Override
	protected void updateMultiple() {
		isPseudocolourBox.setEnabled(false);
		thresholdSlider.setEnabled(false);
		ssimLabel.setText("");
		ssimBtn.setEnabled(true);
		setTable(makeOptions());
	}

	@Override
	protected void updateNull() {
		isPseudocolourBox.setEnabled(false);
		thresholdSlider.setEnabled(false);
		ssimLabel.setText("");
		ssimBtn.setEnabled(false);
		table.setModel(AbstractTableCreator.createBlankTable());
	}

	@Override
	public void nuclearSignalUpdated(List<IAnalysisDataset> datasets) {
		refreshCache(datasets);
	}

	@Override
	public void nuclearSignalUpdated(IAnalysisDataset dataset) {
		refreshCache(dataset);
	}

	public void addWarpedSignalSelectionChangeListener(WarpedSignalSelectionChangeListener l) {
		listeners.add(l);
	}

	private void fireWarpedSignalSelectionChanged(List<IWarpedSignal> images) {
		for (WarpedSignalSelectionChangeListener l : listeners)
			l.warpedSignalSelectionChanged(images);
	}

	private void fireWarpedSignalVisualisationChanged(List<IWarpedSignal> images) {
		for (WarpedSignalSelectionChangeListener l : listeners)
			l.warpedSignalVisualisationChanged(images);
	}

	/**
	 * Colour the background of the pseudocolour column in the signal warping table
	 * 
	 * @author bms41
	 * @since 1.15.0
	 *
	 */
	public class SignalWarpingTableCellRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			Component l = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			Color colour = (Color) value;
			l.setBackground(colour);
			l.setForeground(colour);
			return l;
		}
	}

}
