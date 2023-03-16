package com.bmskinner.nma.gui.components.panels;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import com.bmskinner.nma.analysis.classification.PrincipalComponentAnalysis;
import com.bmskinner.nma.analysis.classification.TsneMethod;
import com.bmskinner.nma.analysis.classification.UMAPMethod;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.OptionsFactory;
import com.bmskinner.nma.gui.Labels;
import com.bmskinner.nma.logging.Loggable;

public class DimensionalReductionSelectionPanel extends OptionsPanel {

	private static final double MIN_PERPLEXITY = 5;
	private static final double INTIIAL_PERPLEXITY_FRACTION = 20d;
	private static final double MAX_PERPLEXITY_FRACTION = 3d;
	private static final double STEP_PERPLEXITY = 1;

	private static final int MIN_ITERATIONS = 500;
	private static final int MAX_ITERATIONS = 50000;
	private static final int STEP_ITERATIONS = 25;

	private static final int MIN_UMAP_NEIGHBOUR = 1;
	private static final int STEP_UMAP_NEIGHBOUR = 1;

	private static final float MIN_UMAP_MINDIST = 0.001f;
	private static final float MAX_UMAP_MINDIST = 0.5f;
	private static final float STEP_UMAP_MINDIST = 0.001f;

	private static final double DEFAULT_PCA_VARIANCE = 0.95;
	private static final double MIN_PCA_VARIANCE = 0.05;
	private static final double MAX_PCA_VARIANCE = 1;
	private static final double STEP_PCA_VARIANCE = 0.01;

	private static final String BORDER_LABEL = "Dimensional reduction";

	private static final Logger LOGGER = Logger
			.getLogger(DimensionalReductionSelectionPanel.class.getName());

	public DimensionalReductionSelectionPanel(IAnalysisDataset dataset, HashOptions options) {
		super(dataset, options);
	}

	@Override
	protected void setDefaults() {
		options.set(OptionsFactory.makeDefaultTsneOptions().build());
		options.set(OptionsFactory.makeDefaultUmapOptions().build());
		options.setBoolean(HashOptions.CLUSTER_USE_TSNE_KEY, HashOptions.DEFAULT_USE_TSNE);
		options.setBoolean(HashOptions.CLUSTER_USE_PCA_KEY, HashOptions.DEFAULT_USE_PCA);
		options.setBoolean(HashOptions.CLUSTER_USE_UMAP_KEY, HashOptions.DEFAULT_USE_UMAP);
	}

	@Override
	protected JPanel createUI() {
		JPanel panel = new JPanel();
		BoxLayout layout = new BoxLayout(panel, BoxLayout.X_AXIS);
		panel.setLayout(layout);

		JPanel optionPanel = createOptionPanel();
		optionPanel.setAlignmentY(Component.TOP_ALIGNMENT);
		panel.add(Box.createRigidArea(new Dimension(5, 0)));
		panel.add(optionPanel);
		panel.add(Box.createVerticalGlue());

		panel.setBorder(BorderFactory.createTitledBorder(BORDER_LABEL));
		return panel;
	}

	private JPanel createOptionPanel() {
		JPanel panel = new JPanel();
		GridBagLayout layout = new GridBagLayout();
		panel.setLayout(layout);

		List<JLabel> labels = new ArrayList<>();
		List<Component> fields = new ArrayList<>();

		ButtonGroup buttonGroup = new ButtonGroup();

		// Add box for no dimensionality reduction
		JCheckBox noneBox = new JCheckBox();
		noneBox.setForeground(Color.DARK_GRAY);
		noneBox.setSelected(!(options.getBoolean(HashOptions.CLUSTER_USE_TSNE_KEY)
				|| options.getBoolean(HashOptions.CLUSTER_USE_PCA_KEY)
				|| options.getBoolean(HashOptions.CLUSTER_USE_UMAP_KEY)));
		JLabel noneLabel = new JLabel("None");
		labels.add(noneLabel);
		fields.add(noneBox);

		// Add options for UMAP
		JCheckBox umapBox = new JCheckBox();
		umapBox.setForeground(Color.DARK_GRAY);

		umapBox.setSelected(options.getBoolean(HashOptions.CLUSTER_USE_UMAP_KEY));
		JLabel umaplabel = new JLabel(Labels.Clusters.UMAP);

		JSpinner neighbourSpinner = makeUmapNeighbourSpinner();
		neighbourSpinner.setEnabled(options.getBoolean(HashOptions.CLUSTER_USE_UMAP_KEY));
		JSpinner minDistSpinner = makeUmapMinDistanceSpinner();
		minDistSpinner.setEnabled(options.getBoolean(HashOptions.CLUSTER_USE_UMAP_KEY));

		// Add box for t-SNE
		JCheckBox tSNEBox = new JCheckBox();
		tSNEBox.setForeground(Color.DARK_GRAY);

		tSNEBox.setSelected(options.getBoolean(HashOptions.CLUSTER_USE_TSNE_KEY));
		JLabel label = new JLabel(Labels.Clusters.TSNE);

		JSpinner iterationsSpinner = makeMaxIterationsSpinner();
		iterationsSpinner.setEnabled(options.getBoolean(HashOptions.CLUSTER_USE_TSNE_KEY));
		JSpinner perplexitySpinner = makePerplexitySpinner();
		perplexitySpinner.setEnabled(options.getBoolean(HashOptions.CLUSTER_USE_TSNE_KEY));

		umapBox.addChangeListener(e -> {
			options.setBoolean(HashOptions.CLUSTER_USE_UMAP_KEY, umapBox.isSelected());
			neighbourSpinner.setEnabled(umapBox.isSelected());
			minDistSpinner.setEnabled(umapBox.isSelected());
		});

		// Add checkbox listeners last so we can reference the spinners
		tSNEBox.addChangeListener(e -> {
			options.setBoolean(HashOptions.CLUSTER_USE_TSNE_KEY, tSNEBox.isSelected());
			iterationsSpinner.setEnabled(tSNEBox.isSelected());
			perplexitySpinner.setEnabled(tSNEBox.isSelected());
		});

		noneBox.addActionListener(e -> {
			options.setBoolean(HashOptions.CLUSTER_USE_PCA_KEY, false);
			options.setBoolean(HashOptions.CLUSTER_USE_TSNE_KEY, false);
			options.setBoolean(HashOptions.CLUSTER_USE_UMAP_KEY, false);
			neighbourSpinner.setEnabled(umapBox.isSelected());
			minDistSpinner.setEnabled(umapBox.isSelected());
			iterationsSpinner.setEnabled(tSNEBox.isSelected());
			perplexitySpinner.setEnabled(tSNEBox.isSelected());
		});

		// Add UMAP elements
		labels.add(umaplabel);
		fields.add(umapBox);

		labels.add(new JLabel(UMAPMethod.N_NEIGHBOUR_KEY));
		fields.add(neighbourSpinner);

		labels.add(new JLabel(UMAPMethod.MIN_DISTANCE_KEY));
		fields.add(minDistSpinner);

		// Add tSNE elements
		labels.add(label);
		fields.add(tSNEBox);

		labels.add(new JLabel(TsneMethod.PERPLEXITY_KEY));
		fields.add(perplexitySpinner);

		labels.add(new JLabel(TsneMethod.MAX_ITERATIONS_KEY));
		fields.add(iterationsSpinner);

		// Add box for PCA
		JCheckBox pcaBox = new JCheckBox();
		pcaBox.setSelected(options.getBoolean(HashOptions.CLUSTER_USE_PCA_KEY));
		JLabel pcaLbl = new JLabel(Labels.Clusters.PCA);

		JSpinner pcaSpinner = makePcaVarianceSpinner();
		pcaSpinner.setEnabled(options.getBoolean(HashOptions.CLUSTER_USE_PCA_KEY));

		pcaBox.addChangeListener(e -> {
			options.setBoolean(HashOptions.CLUSTER_USE_PCA_KEY, pcaBox.isSelected());
			pcaSpinner.setEnabled(pcaBox.isSelected());
		});

		buttonGroup.add(noneBox);
		buttonGroup.add(umapBox);
		buttonGroup.add(tSNEBox);
		buttonGroup.add(pcaBox);

		labels.add(pcaLbl);
		fields.add(pcaBox);

		labels.add(new JLabel(PrincipalComponentAnalysis.PROPORTION_VARIANCE_KEY));
		fields.add(pcaSpinner);

		addLabelTextRows(labels, fields, layout, panel);
		return panel;
	}

	/**
	 * Create the UMAP neighbour spinner
	 * 
	 * @return
	 */
	private JSpinner makeUmapNeighbourSpinner() {
		int initialValue = options.getInt(UMAPMethod.N_NEIGHBOUR_KEY);
		SpinnerModel model = new SpinnerNumberModel(initialValue, MIN_UMAP_NEIGHBOUR,
				dataset.getCollection().size(), STEP_UMAP_NEIGHBOUR);

		JSpinner spinner = new JSpinner(model);
		spinner.setEnabled(true);
		spinner.addChangeListener(l -> {
			try {
				spinner.commitEdit();
				options.setInt(UMAPMethod.N_NEIGHBOUR_KEY,
						(Integer) spinner.getValue());
			} catch (ParseException e) {
				LOGGER.log(Loggable.STACK, "Parse error in spinner", e);
			}

		});
		return spinner;
	}

	/**
	 * Create the UMAP min distance spinner
	 * 
	 * @return
	 */
	private JSpinner makeUmapMinDistanceSpinner() {
		float initialValue = options.getFloat(UMAPMethod.MIN_DISTANCE_KEY);
		SpinnerModel model = new SpinnerNumberModel(initialValue, MIN_UMAP_MINDIST,
				MAX_UMAP_MINDIST, STEP_UMAP_MINDIST);

		JSpinner spinner = new JSpinner(model);
		spinner.setEnabled(true);
		spinner.addChangeListener(l -> {
			try {
				spinner.commitEdit();
				options.setFloat(UMAPMethod.MIN_DISTANCE_KEY,
						((Double) spinner.getValue()).floatValue());
			} catch (ParseException e) {
				LOGGER.log(Loggable.STACK, "Parse error in spinner", e);
			}

		});
		return spinner;
	}

	/**
	 * Create the iterations spinner
	 * 
	 * @return
	 */
	private JSpinner makeMaxIterationsSpinner() {
		int initialIterations = options.getInt(TsneMethod.MAX_ITERATIONS_KEY);
		SpinnerModel iterationsModel = new SpinnerNumberModel(initialIterations, MIN_ITERATIONS,
				MAX_ITERATIONS, STEP_ITERATIONS);

		JSpinner iterationsSpinner = new JSpinner(iterationsModel);
		iterationsSpinner.setEnabled(true);
		iterationsSpinner.addChangeListener(l -> {
			try {
				iterationsSpinner.commitEdit();
				options.setInt(TsneMethod.MAX_ITERATIONS_KEY,
						(Integer) iterationsSpinner.getValue());
			} catch (ParseException e) {
				LOGGER.log(Loggable.STACK, "Parse error in spinner", e);
			}

		});
		return iterationsSpinner;
	}

	/**
	 * Create the perplexity spinner with default perplexity based on the number of
	 * nuclei in the dataset
	 * 
	 * @return
	 */
	private JSpinner makePerplexitySpinner() {
		int nNuclei = dataset.getCollection().getNucleusCount();
		double initialPerplexity = Math.max(MIN_PERPLEXITY,
				Math.floor(nNuclei / INTIIAL_PERPLEXITY_FRACTION));

		// Don't allow the max perplexity to be less than the inital when sample sizes
		// are low
		double maxPerplexity = Math.max(Math.floor(nNuclei / MAX_PERPLEXITY_FRACTION) - 1,
				initialPerplexity);
		options.setDouble(TsneMethod.PERPLEXITY_KEY, initialPerplexity);

		SpinnerModel perplexityModel = new SpinnerNumberModel(initialPerplexity, MIN_PERPLEXITY,
				maxPerplexity, STEP_PERPLEXITY);

		JSpinner perplexitySpinner = new JSpinner(perplexityModel);
		perplexitySpinner.setEnabled(true);

		perplexitySpinner.addChangeListener(l -> {
			try {
				perplexitySpinner.commitEdit();
				options.setDouble(TsneMethod.PERPLEXITY_KEY, (Double) perplexitySpinner.getValue());
			} catch (ParseException e) {
				LOGGER.log(Loggable.STACK, "Parse error in spinner", e);
			}

		});
		return perplexitySpinner;
	}

	/**
	 * Create the PCA variance spinner
	 * 
	 * @return
	 */
	private JSpinner makePcaVarianceSpinner() {
		options.setDouble(PrincipalComponentAnalysis.PROPORTION_VARIANCE_KEY, DEFAULT_PCA_VARIANCE);

		SpinnerModel model = new SpinnerNumberModel(DEFAULT_PCA_VARIANCE, MIN_PCA_VARIANCE,
				MAX_PCA_VARIANCE, STEP_PCA_VARIANCE);
		JSpinner spinner = new JSpinner(model);
		spinner.setEnabled(true);

		spinner.addChangeListener(l -> {
			try {
				spinner.commitEdit();
				options.setDouble(PrincipalComponentAnalysis.PROPORTION_VARIANCE_KEY,
						(Double) spinner.getValue());
			} catch (ParseException e) {
				LOGGER.log(Loggable.STACK, "Parse error in spinner", e);
			}

		});
		return spinner;
	}
}
