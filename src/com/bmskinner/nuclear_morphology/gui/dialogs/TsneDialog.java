package com.bmskinner.nuclear_morphology.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.util.Optional;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.ChartPanel;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.classification.ProfileTsneMethod;
import com.bmskinner.nuclear_morphology.charting.charts.ScatterChartFactory;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.IClusterGroup;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.components.ImageThumbnailGenerator;
import com.bmskinner.nuclear_morphology.gui.components.panels.ClusterGroupSelectionPanel;

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

		chartPanel.addChartMouseListener(new ImageThumbnailGenerator(chartPanel));

		updateTitle();
		updateChart(ColourByType.NONE, null);
		setLayout(new BorderLayout());

		add(createHeader(), BorderLayout.NORTH);

		add(chartPanel, BorderLayout.CENTER);
		
		pack();
		setLocationRelativeTo(null);
		setVisible(true);				
	}
	
	public enum ColourByType {
		NONE, CLUSTER, MERGE_SOURCE;
	}


	private JPanel createHeader() {
		JPanel panel = new JPanel(new FlowLayout());
		runTsneBtn.addActionListener( l->runNewTsne());
		panel.add(runTsneBtn);
		
		// How should cells be coloured?
		
		final ButtonGroup colourGroup = new ButtonGroup();
		JRadioButton byNoneBtn = new JRadioButton("None");
		JRadioButton byClusterBtn = new JRadioButton("Clusters");
		JRadioButton byMergeSourceBtn = new JRadioButton("Merge source");
		colourGroup.add(byNoneBtn);
		colourGroup.add(byClusterBtn);
		colourGroup.add(byMergeSourceBtn);
		
		byNoneBtn.setSelected(true);
		
		ClusterGroupSelectionPanel clustersBox = new ClusterGroupSelectionPanel(dataset.getClusterGroups());
		clustersBox.setEnabled(false);
		
		
		ActionListener colourListener = e ->{
			ColourByType type = byNoneBtn.isSelected() ? ColourByType.NONE : byClusterBtn.isSelected() ? ColourByType.CLUSTER : ColourByType.MERGE_SOURCE;
			clustersBox.setEnabled(byClusterBtn.isSelected());
			updateChart(type, clustersBox.getSelectedItem());
		};

		byNoneBtn.addActionListener(colourListener);
		byClusterBtn.addActionListener(colourListener);
		byMergeSourceBtn.addActionListener(colourListener);
		clustersBox.addActionListener(colourListener);
		
		panel.add(new JLabel("Colour by:"));
		panel.add(byNoneBtn);
		panel.add(byClusterBtn);
		panel.add(clustersBox);
		panel.add(byMergeSourceBtn);		
		return panel;
	}

	private void runNewTsne() {
		SubAnalysisSetupDialog tsneSetup = new TsneSetupDialog(dataset);
		if (tsneSetup.isReadyToRun()) {
			try {
				chartPanel.setChart(ScatterChartFactory.createLoadingChart());
				IAnalysisWorker w = new DefaultAnalysisWorker(tsneSetup.getMethod());
				w.addPropertyChangeListener(e->{
					if (e.getPropertyName().equals(IAnalysisWorker.FINISHED_MSG)) {
						updateTitle();
						updateChart(ColourByType.NONE, null);
					}
				});
				ThreadManager.getInstance().submit(w);	
			} catch (Exception e) {
				error("Error running new t-SNE", e);
			}
		}
		tsneSetup.dispose();
	}

	private void updateChart(ColourByType type, IClusterGroup group) {
		chartPanel.setChart(ScatterChartFactory.createTsneChart(dataset, type, group));
	}
	
	private void updateTitle() {
		Optional<IAnalysisOptions> analysisOptions = dataset.getAnalysisOptions();
		if(!analysisOptions.isPresent()) {
			warn("Unable to create dialog, no analysis options in dataset");
			setTitle("");
		}

		Optional<HashOptions> tSNEOptions = analysisOptions.get().getSecondaryOptions(IAnalysisOptions.TSNE);
		if(!tSNEOptions.isPresent()) {
			warn("Unable to create dialog, no t-SNE options in dataset");
			setTitle("");
		}
		
		setTitle("tSNE for "+dataset.getName()+
				": "+tSNEOptions.get().getString(ProfileTsneMethod.PROFILE_TYPE_KEY)+
				", Perplexity "+
				tSNEOptions.get().getDouble(ProfileTsneMethod.PERPLEXITY_KEY)+
				", Max iterations "+
				tSNEOptions.get().getInt(ProfileTsneMethod.MAX_ITERATIONS_KEY));
	}


}
