package com.bmskinner.nma.gui.tabs;

import java.awt.Cursor;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.core.InterfaceUpdater;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.visualisation.ChartCache;
import com.bmskinner.nma.visualisation.charts.AbstractChartFactory;
import com.bmskinner.nma.visualisation.options.ChartOptions;

@SuppressWarnings("serial")
public abstract class ChartDetailPanel extends DetailPanel {

	private static final Logger LOGGER = Logger.getLogger(ChartDetailPanel.class.getName());

	private static final String DEFAULT_TAB_TITLE = "Default";

	/**
	 * Create with default title.
	 */
	protected ChartDetailPanel() {
		this(DEFAULT_TAB_TITLE);
	}

	/**
	 * Create with a title. The panel description will use the title also.
	 * 
	 * @param title
	 */
	protected ChartDetailPanel(@NonNull final String title) {
		this(title, title);
	}

	/**
	 * Create with a title and description for tooltips.
	 * 
	 * @param title
	 */
	protected ChartDetailPanel(@NonNull final String title, @NonNull final String description) {
		super(title, description);
		cache = new ChartCache();
	}

	/**
	 * This should be overridden to create the appropriate charts for caching
	 * 
	 * @param options the chart options
	 * @return null unless overridden
	 * @throws Exception
	 */
	protected abstract JFreeChart createPanelChartType(@NonNull ChartOptions options);

	/**
	 * Fetch the chart with the given options from the cache, and display it in the
	 * target ChartPanel. If the chart is not in the cache, a SwingWorker will be
	 * created to render the chart and display it once complete. Note that this
	 * requires the options to have been created with a setTarget() value.
	 * 
	 * @param options
	 */
	protected synchronized void setChart(@NonNull ChartOptions options) {
		if (cache.has(options)) {
			if (options.getTarget() != null) {
				options.getTarget().setChart(cache.get(options));
				options.getTarget().setCursor(Cursor.getDefaultCursor());
			}

		} else { // No cached chart
			// Make a background worker to generate the chart and
			// update the target chart panel when done
			options.getTarget().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			options.getTarget().setChart(AbstractChartFactory.createLoadingChart());

			try {
				ThreadManager.getInstance().submit(new ChartFactoryWorker(options));
			} catch (final RejectedExecutionException e) {
				// probably the dataset does not exist any more. Do not spam error messages
				// though.
			}
		}
	}

	/**
	 * Charting can be an intensive process, especially with background images being
	 * imported for outline charts. This worker will keep the chart generation off
	 * the EDT
	 * 
	 * @author Ben Skinner
	 *
	 */
	protected class ChartFactoryWorker extends SwingWorker<JFreeChart, Void>
			implements InterfaceUpdater {

		private final @NonNull ChartOptions options;

		public ChartFactoryWorker(@NonNull final ChartOptions o) {
			options = o;
		}

		@Override
		protected JFreeChart doInBackground() throws Exception {
			if (this.isCancelled())
				return null;

			try {
				if (options.hasTarget()) {
					options.getTarget().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					options.getTarget().setChart(AbstractChartFactory.createLoadingChart());
				}

				final JFreeChart chart = createPanelChartType(options);
				cache.add(options, chart);

				return chart;
			} catch (final Exception e) {
				LOGGER.log(Level.SEVERE, "Error creating chart in %s: %s"
						.formatted(ChartDetailPanel.this.getClass().getName(), e.getMessage()), e);
				LOGGER.log(Level.SEVERE, "Chart model error: %s"
						.formatted(e.getCause().getMessage()), e.getCause());
				return null;
			}

		}

		@Override
		public void done() {

//			try {
//				if (options.hasTarget()) {
					setChart(options);
//					options.getTarget().setChart(cache.get(options));
//					options.getTarget().setCursor(Cursor.getDefaultCursor());
//				}
//			} catch (final InterruptedException e) {
//				LOGGER.log(Level.SEVERE, "Interruption to charting: %s".formatted(e.getMessage()), e);
//				Thread.currentThread().interrupt();
//			} catch (final ExecutionException e) {
//				LOGGER.log(Level.SEVERE, "Excecution error charting: %s".formatted(e.getMessage()), e);
//			}
		}

		@Override
		public List<IAnalysisDataset> datasetsAffected() {
			return options.getDatasets();
		}

	}
}
