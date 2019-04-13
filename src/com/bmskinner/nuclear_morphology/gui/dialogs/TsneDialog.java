package com.bmskinner.nuclear_morphology.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.ChartPanel;

import com.bmskinner.nuclear_morphology.analysis.classification.ProfileTsneMethod;
import com.bmskinner.nuclear_morphology.charting.charts.ScatterChartFactory;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.gui.components.ImageThumbnailGenerator;

/**
 * Display tSNE results. This is a temporary class for testing.
 * @author ben
 * @since 1.16.0
 *
 */

public class TsneDialog extends LoadingIconDialog {

	private final IAnalysisDataset dataset;
	private final ChartPanel chartPanel = new ChartPanel(ScatterChartFactory.createEmptyChart());

	private final JButton runTsneBtn = new JButton("Run new t-SNE");

	public TsneDialog(final @NonNull IAnalysisDataset dataset) {
		this.dataset = dataset;

		Optional<IAnalysisOptions> analysisOptions = dataset.getAnalysisOptions();
		if(!analysisOptions.isPresent()) {
			warn("Unable to create dialog, no analysis options in dataset");
			return;
		}

		Optional<HashOptions> tSNEOptions = analysisOptions.get().getSecondaryOptions(IAnalysisOptions.TSNE);
		if(!tSNEOptions.isPresent()) {
			warn("Unable to create dialog, no t-SNE options in dataset");
			return;
		}

		chartPanel.addChartMouseListener(new ImageThumbnailGenerator(chartPanel));

		updateChart();
		setLayout(new BorderLayout());

		add(createHeader(), BorderLayout.NORTH);

		add(chartPanel, BorderLayout.CENTER);
		setTitle("tSNE for "+dataset.getName()+": Perplexity "+
				tSNEOptions.get().getDouble(ProfileTsneMethod.PERPLEXITY_KEY)+
				" max_iter "+
				tSNEOptions.get().getInt(ProfileTsneMethod.MAX_ITERATIONS_KEY));
		pack();
		setLocationRelativeTo(null);
		setVisible(true);				
	}


	private JPanel createHeader() {
		JPanel panel = new JPanel(new FlowLayout());
		runTsneBtn.addActionListener( l->runNewTsne());
		panel.add(runTsneBtn);

		return panel;
	}

	private void runNewTsne() {
		SubAnalysisSetupDialog tsneSetup = new TsneSetupDialog(dataset);
		if (tsneSetup.isReadyToRun()) {
			try {
				tsneSetup.getMethod().call();
				updateChart();
			} catch (Exception e) {
				error("Error running new t-SNE", e);
			}
		}
		tsneSetup.dispose();
	}

	private void updateChart() {
		chartPanel.setChart(ScatterChartFactory.createTsneChart(dataset));
	}


}
