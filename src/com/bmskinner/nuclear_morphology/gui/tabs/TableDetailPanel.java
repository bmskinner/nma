package com.bmskinner.nuclear_morphology.gui.tabs;

import java.awt.Cursor;
import java.awt.EventQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.core.InterfaceUpdater;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.CancellableRunnable;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.visualisation.TableCache;
import com.bmskinner.nuclear_morphology.visualisation.options.TableOptions;
import com.bmskinner.nuclear_morphology.visualisation.tables.AbstractTableCreator;

public abstract class TableDetailPanel extends DetailPanel {

	private static final Logger LOGGER = Logger.getLogger(TableDetailPanel.class.getName());

	private static final String DEFAULT_TAB_TITLE = "Default";

	protected TableDetailPanel() {
		this(DEFAULT_TAB_TITLE);
	}

	protected TableDetailPanel(@NonNull final String title) {
		super(title);
		cache = new TableCache();
	}

	/**
	 * This should be overridden to create the appropriate tables for caching
	 * 
	 * @param options the table options
	 * @return null unless overridden
	 * @throws Exception
	 */
	protected abstract TableModel createPanelTableType(@NonNull TableOptions options);

	/**
	 * Fetch the table model with the given options from the cache, and display it
	 * in the target JTable. If the model is not in the cache, a SwingWorker will be
	 * created to render the model and display it once complete. Note that this
	 * requires the options to have been created with a setTarget() value.
	 * 
	 * @param options
	 */
	protected synchronized void setTable(TableOptions options) {
		if (cache.has(options)) {
			TableModel model = cache.get(options);

			if (options.getTarget() != null) {
				options.getTarget().setModel(model);
				setRenderers(options);
//				options.getTarget().updateRowHeights();
				if (options.getTarget() instanceof ExportableTable) {
					EventQueue.invokeLater(() -> ((ExportableTable) options.getTarget()).updateRowHeights());

				}

			}

		} else { // No cached chart

			// Make a background worker to generate the chart and
			// update the target chart panel when done
			TableFactoryWorker worker = new TableFactoryWorker(options);

			ThreadManager.getInstance().submit(worker);
		}
	}

	/**
	 * Set the given table to use a custom table renderer. The renderer will be used
	 * for every column except the first.
	 * 
	 * @param table
	 */
	protected synchronized void setRenderer(@NonNull JTable table, @NonNull TableCellRenderer renderer) {
		int columns = table.getColumnModel().getColumnCount();
		if (columns > 1) {
			for (int i = 1; i < columns; i++) {
				table.getColumnModel().getColumn(i).setCellRenderer(renderer);
			}
		}
	}

	/**
	 * Fetch the desired table, either from the cache, or by creating it
	 * 
	 * @param options
	 * @return
	 * @throws Exception
	 */
	protected synchronized TableModel getTable(TableOptions options) {

		TableModel model;
		if (cache.has(options)) {
			model = cache.get(options);
		} else {
			try {
				model = createPanelTableType(options);
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Error creating table: " + this.getClass().getSimpleName());
				LOGGER.log(Loggable.STACK, this.getClass().getName() + ": Error creating table", e);
				model = AbstractTableCreator.createBlankTable();
			}
			cache.add(options, model);
		}
		return model;
	}

	private static void setRenderers(TableOptions options) {
		JTable table = options.getTarget();

		if (table.getRowCount() == 0)
			return;

		int columns = table.getColumnModel().getColumnCount();

		for (int i : options.getRendererColumns()) {

			TableCellRenderer renderer = options.getRenderer(i);

			if (i == TableOptions.FIRST_COLUMN) {

				table.getColumnModel().getColumn(0).setCellRenderer(renderer);
				continue;
			}

			if (i == TableOptions.ALL_COLUMNS) {
				for (int j = 0; j < columns; j++) {
					table.getColumnModel().getColumn(j).setCellRenderer(renderer);
				}
				continue;
			}

			if (i == TableOptions.ALL_EXCEPT_FIRST_COLUMN) {
				for (int j = 1; j < columns; j++) {
					table.getColumnModel().getColumn(j).setCellRenderer(renderer);
				}
				continue;
			}

			table.getColumnModel().getColumn(i).setCellRenderer(renderer);
		}
	}

	/**
	 * Tables can also be an intensive process, especially with venn comparisons.
	 * This worker will keep the model generation off the EDT
	 * 
	 * @author bms41
	 *
	 */
	protected class TableFactoryWorker extends SwingWorker<TableModel, Void>
			implements CancellableRunnable, InterfaceUpdater {

		private final TableOptions options;

		public TableFactoryWorker(@NonNull final TableOptions o) {
			options = o;
		}

		@Override
		protected synchronized TableModel doInBackground() throws Exception {

			try {
				if (options.hasTarget())
					options.getTarget().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

				TableModel model = createPanelTableType(options);
				cache.add(options, model);

				return model;
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Error creating table model");
				LOGGER.log(Loggable.STACK, "Error creating table model", e);
				return null;
			}

		}

		@Override
		public synchronized void done() {
			options.getTarget().setCursor(Cursor.getDefaultCursor());
			setTable(options);
		}

		@Override
		public void cancel() {
			LOGGER.fine("Cancelling detail panel table update");
			this.cancel(true);
		}

	}

}
