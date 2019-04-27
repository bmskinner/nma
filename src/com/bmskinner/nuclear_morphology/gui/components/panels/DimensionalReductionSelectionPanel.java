package com.bmskinner.nuclear_morphology.gui.components.panels;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import com.bmskinner.nuclear_morphology.analysis.classification.TsneMethod;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions;
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
	
	private static final String BORDER_LABEL = "Dimensional reduction";

	private static final Logger LOGGER = Logger.getLogger(Loggable.ROOT_LOGGER);

	public DimensionalReductionSelectionPanel(IAnalysisDataset dataset, HashOptions options) {
		super(dataset, options);
	}

	@Override
	protected void setDefaults() {
		options.set(OptionsFactory.makeDefaultTsneOptions());
		options.setBoolean(IClusteringOptions.USE_TSNE_KEY,  IClusteringOptions.DEFAULT_USE_TSNE);
		options.setBoolean(IClusteringOptions.USE_PCA_KEY,  false);
	}

	@Override
	protected JPanel createUI() {
		JPanel panel = new JPanel();
		BoxLayout layout = new BoxLayout(panel, BoxLayout.X_AXIS);
		panel.setLayout(layout);

		panel.add(createTsnePanel());
		
		panel.setBorder(BorderFactory.createTitledBorder(BORDER_LABEL));
		return panel;
	}
	
	private JPanel createTsnePanel() {
		JPanel panel = new JPanel();
		GridBagLayout layout = new GridBagLayout();
		panel.setLayout(layout);

		List<JLabel> labels = new ArrayList<>();
		List<Component> fields = new ArrayList<>();
		
		JCheckBox tSNEBox = new JCheckBox();
		tSNEBox.setForeground(Color.DARK_GRAY);
		
		tSNEBox.setSelected(options.getBoolean(IClusteringOptions.USE_TSNE_KEY));
		JLabel label = new JLabel(Labels.Clusters.TSNE);

		JSpinner iterationsSpinner = makeMaxIterationsSpinner();
		iterationsSpinner.setEnabled(options.getBoolean(IClusteringOptions.USE_TSNE_KEY));
		JSpinner perplexitySpinner = makePerplexitySpinner();
		perplexitySpinner.setEnabled(options.getBoolean(IClusteringOptions.USE_TSNE_KEY));
		
		tSNEBox.addChangeListener(e->{
			options.setBoolean(IClusteringOptions.USE_TSNE_KEY, tSNEBox.isSelected());
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
		pcaBox.setSelected(options.getBoolean(IClusteringOptions.USE_PCA_KEY));
		JLabel pcaLbl = new JLabel(Labels.Clusters.PCA);
		pcaBox.addChangeListener(e->{
			options.setBoolean(IClusteringOptions.USE_PCA_KEY, pcaBox.isSelected());
		});
		
		labels.add(pcaLbl);
		fields.add(pcaBox);

		addLabelTextRows(labels, fields, layout, panel);
		return panel;
	}

	/**
	 * Create the iterations spinner with default perplexity based
	 * on the number of nuclei in the dataset
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
		double maxPerplexity = Math.floor(nNuclei/MAX_PERPLEXITY_FRACTION)-1;
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
}
