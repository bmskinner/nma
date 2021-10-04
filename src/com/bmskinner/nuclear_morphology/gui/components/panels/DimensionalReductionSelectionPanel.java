package com.bmskinner.nuclear_morphology.gui.components.panels;

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

import com.bmskinner.nuclear_morphology.analysis.classification.PrincipalComponentAnalysis;
import com.bmskinner.nuclear_morphology.analysis.classification.TsneMethod;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.logging.Loggable;

public class DimensionalReductionSelectionPanel extends OptionsPanel {

	private static final double MIN_PERPLEXITY = 5;
	private static final double INTIIAL_PERPLEXITY_FRACTION = 20d;
	private static final double MAX_PERPLEXITY_FRACTION = 3d;
	private static final double STEP_PERPLEXITY = 1;
	
	private static final int MIN_ITERATIONS = 500;
	private static final int MAX_ITERATIONS = 50000;
	private static final int STEP_ITERATIONS = 25;
	
	private static final double DEFAULT_PCA_VARIANCE = 0.95;
	private static final double MIN_PCA_VARIANCE = 0.05;
	private static final double MAX_PCA_VARIANCE = 1;
	private static final double STEP_PCA_VARIANCE = 0.01;
	
	private static final String BORDER_LABEL = "Dimensional reduction";

	private static final Logger LOGGER = Logger.getLogger(DimensionalReductionSelectionPanel.class.getName());

	public DimensionalReductionSelectionPanel(IAnalysisDataset dataset, HashOptions options) {
		super(dataset, options);
	}

	@Override
	protected void setDefaults() {
		options.set(OptionsFactory.makeDefaultTsneOptions());
		options.setBoolean(HashOptions.CLUSTER_USE_TSNE_KEY,  HashOptions.DEFAULT_USE_TSNE);
		options.setBoolean(HashOptions.CLUSTER_USE_PCA_KEY,  HashOptions.DEFAULT_USE_PCA);
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
		
		JCheckBox noneBox = new JCheckBox();
		noneBox.setForeground(Color.DARK_GRAY);
		noneBox.setSelected( !(options.getBoolean(HashOptions.CLUSTER_USE_TSNE_KEY) || options.getBoolean(HashOptions.CLUSTER_USE_PCA_KEY)));
		JLabel noneLabel = new JLabel("None");
		labels.add(noneLabel);
		fields.add(noneBox);
		
		JCheckBox tSNEBox = new JCheckBox();
		tSNEBox.setForeground(Color.DARK_GRAY);
		
		tSNEBox.setSelected(options.getBoolean(HashOptions.CLUSTER_USE_TSNE_KEY));
		JLabel label = new JLabel(Labels.Clusters.TSNE);

		JSpinner iterationsSpinner = makeMaxIterationsSpinner();
		iterationsSpinner.setEnabled(options.getBoolean(HashOptions.CLUSTER_USE_TSNE_KEY));
		JSpinner perplexitySpinner = makePerplexitySpinner();
		perplexitySpinner.setEnabled(options.getBoolean(HashOptions.CLUSTER_USE_TSNE_KEY));
		
		
		// Add checkbox listeners last so we can reference the spinners
		
		tSNEBox.addChangeListener(e->{
			options.setBoolean(HashOptions.CLUSTER_USE_TSNE_KEY, tSNEBox.isSelected());
			iterationsSpinner.setEnabled(tSNEBox.isSelected());
			perplexitySpinner.setEnabled(tSNEBox.isSelected());
		});
		
		noneBox.addActionListener(e->{
			options.setBoolean(HashOptions.CLUSTER_USE_PCA_KEY, false);
			options.setBoolean(HashOptions.CLUSTER_USE_TSNE_KEY, false);
			iterationsSpinner.setEnabled(tSNEBox.isSelected());
			perplexitySpinner.setEnabled(tSNEBox.isSelected());
		});


		labels.add(label);
		fields.add(tSNEBox);
		
		labels.add(new JLabel(TsneMethod.PERPLEXITY_KEY));
		fields.add(perplexitySpinner);

		labels.add(new JLabel(TsneMethod.MAX_ITERATIONS_KEY));
		fields.add(iterationsSpinner);
		
		
		JCheckBox pcaBox = new JCheckBox();
		pcaBox.setSelected(options.getBoolean(HashOptions.CLUSTER_USE_PCA_KEY));
		JLabel pcaLbl = new JLabel(Labels.Clusters.PCA);
		
		JSpinner pcaSpinner = makePcaVarianceSpinner();
		pcaSpinner.setEnabled(options.getBoolean(HashOptions.CLUSTER_USE_PCA_KEY));
		
		pcaBox.addChangeListener(e->{
			options.setBoolean(HashOptions.CLUSTER_USE_PCA_KEY, pcaBox.isSelected());
			pcaSpinner.setEnabled(pcaBox.isSelected());
		});
		

		buttonGroup.add(noneBox);
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
	 * Create the iterations spinner
	 * @return
	 */
	private JSpinner makeMaxIterationsSpinner() {
		int initialIterations = options.getInt(TsneMethod.MAX_ITERATIONS_KEY);
		SpinnerModel iterationsModel = new SpinnerNumberModel(initialIterations, MIN_ITERATIONS, MAX_ITERATIONS, STEP_ITERATIONS);

		JSpinner iterationsSpinner = new JSpinner(iterationsModel);
		iterationsSpinner.setEnabled(true);
		iterationsSpinner.addChangeListener(l->{
			try {
				iterationsSpinner.commitEdit();
				options.setInt(TsneMethod.MAX_ITERATIONS_KEY, (Integer) iterationsSpinner.getValue());
			} catch (ParseException e) {
				LOGGER.log(Loggable.STACK, "Parse error in spinner", e);
			}

		});
		return iterationsSpinner;
	}

	/**
	 * Create the perplexity spinner with default perplexity based
	 * on the number of nuclei in the dataset
	 * @return
	 */
	private JSpinner makePerplexitySpinner() {
		int nNuclei = dataset.getCollection().getNucleusCount();
		double initialPerplexity = Math.max(MIN_PERPLEXITY, Math.floor(nNuclei/INTIIAL_PERPLEXITY_FRACTION));
		
		// Don't allow the max perplexity to be less than the inital when sample sizes are low
		double maxPerplexity = Math.max(Math.floor(nNuclei/MAX_PERPLEXITY_FRACTION)-1, initialPerplexity);
		options.setDouble(TsneMethod.PERPLEXITY_KEY, initialPerplexity);

		SpinnerModel perplexityModel = new SpinnerNumberModel(initialPerplexity, MIN_PERPLEXITY, maxPerplexity, STEP_PERPLEXITY);

		JSpinner perplexitySpinner = new JSpinner(perplexityModel);
		perplexitySpinner.setEnabled(true);

		perplexitySpinner.addChangeListener(l->{
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
	 * @return
	 */
	private JSpinner makePcaVarianceSpinner() {
		options.setDouble(PrincipalComponentAnalysis.PROPORTION_VARIANCE_KEY, DEFAULT_PCA_VARIANCE);

		SpinnerModel model = new SpinnerNumberModel(DEFAULT_PCA_VARIANCE, MIN_PCA_VARIANCE, MAX_PCA_VARIANCE, STEP_PCA_VARIANCE);
		JSpinner spinner = new JSpinner(model);
		spinner.setEnabled(true);

		spinner.addChangeListener(l->{
			try {
				spinner.commitEdit();
				options.setDouble(PrincipalComponentAnalysis.PROPORTION_VARIANCE_KEY, (Double) spinner.getValue());
			} catch (ParseException e) {
				LOGGER.log(Loggable.STACK, "Parse error in spinner", e);
			}

		});  
		return spinner;
	}
}
