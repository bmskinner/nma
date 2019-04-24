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

import com.bmskinner.nuclear_morphology.analysis.classification.ProfileTsneMethod;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.logging.Loggable;

public class DimensionalReductionSelectionPanel extends OptionsPanel {

	private static final double MIN_PERPLEXITY = 5;
	private static final double MAX_PERPLEXITY = 10000;
	private static final double STEP_PERPLEXITY = 1;

	private static final Logger LOGGER = Logger.getLogger(Loggable.ROOT_LOGGER);

	public DimensionalReductionSelectionPanel(IAnalysisDataset dataset, HashOptions options) {
		super(dataset, options);
	}

	@Override
	protected void setDefaults() {
		options.set(OptionsFactory.makeDefaultTsneOptions());
		options.setBoolean(IClusteringOptions.USE_TSNE_KEY,  IClusteringOptions.DEFAULT_USE_TSNE);
	}

	@Override
	protected JPanel createUI() {
		JPanel panel = new JPanel();
		BoxLayout layout = new BoxLayout(panel, BoxLayout.X_AXIS);
		panel.setLayout(layout);

		panel.add(createTsnePanel());
		
		panel.setBorder(BorderFactory.createTitledBorder("Dimensional reduction"));
		return panel;
	}
	
	private JPanel createTsnePanel() {
		JPanel panel = new JPanel();
		GridBagLayout layout = new GridBagLayout();
		panel.setLayout(layout);

		List<JLabel> labels = new ArrayList<>();
		List<Component> fields = new ArrayList<>();
		
		JCheckBox box = new JCheckBox();
		box.setForeground(Color.DARK_GRAY);
		
		box.setSelected(options.getBoolean(IClusteringOptions.USE_TSNE_KEY));
		JLabel label = new JLabel("t-SNE");

		JSpinner iterationsSpinner = makeMaxIterationsSpinner();
		iterationsSpinner.setEnabled(options.getBoolean(IClusteringOptions.USE_TSNE_KEY));
		JSpinner perplexitySpinner = makePerplexitySpinner();
		perplexitySpinner.setEnabled(options.getBoolean(IClusteringOptions.USE_TSNE_KEY));
		
		box.addChangeListener(e->{
			options.setBoolean(IClusteringOptions.USE_TSNE_KEY, box.isSelected());
			iterationsSpinner.setEnabled(box.isSelected());
			perplexitySpinner.setEnabled(box.isSelected());
		});

		labels.add(label);
		fields.add(box);

		labels.add(new JLabel(ProfileTsneMethod.MAX_ITERATIONS_KEY));
		fields.add(iterationsSpinner);

		labels.add(new JLabel(ProfileTsneMethod.PERPLEXITY_KEY));
		fields.add(perplexitySpinner);

		addLabelTextRows(labels, fields, layout, panel);
		return panel;
	}

	/**
	 * Create the iterations spinner with default perplexity based
	 * on the number of nuclei in the dataset
	 * @return
	 */
	private JSpinner makeMaxIterationsSpinner() {
		SpinnerModel iterationsModel = new SpinnerNumberModel(options.getInt(ProfileTsneMethod.MAX_ITERATIONS_KEY), // initial                                                                           // value
				500, // min
				5000, // max
				25); // step

		JSpinner iterationsSpinner = new JSpinner(iterationsModel);
		iterationsSpinner.setEnabled(true);
		iterationsSpinner.addChangeListener(l->{
			try {
				iterationsSpinner.commitEdit();
				options.setInt(ProfileTsneMethod.MAX_ITERATIONS_KEY, (Integer) iterationsSpinner.getValue());
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
		double initialPerplexity = Math.max(MIN_PERPLEXITY, nNuclei/20d);
		options.setDouble(ProfileTsneMethod.PERPLEXITY_KEY, initialPerplexity);

		SpinnerModel perplexityModel = new SpinnerNumberModel(initialPerplexity, MIN_PERPLEXITY, MAX_PERPLEXITY, STEP_PERPLEXITY);

		JSpinner perplexitySpinner = new JSpinner(perplexityModel);
		perplexitySpinner.setEnabled(true);

		perplexitySpinner.addChangeListener(l->{
			try {
				perplexitySpinner.commitEdit();
				options.setDouble(ProfileTsneMethod.PERPLEXITY_KEY, (Double) perplexitySpinner.getValue());
			} catch (ParseException e) {
				LOGGER.log(Loggable.STACK, "Parse error in spinner", e);
			}

		});  
		return perplexitySpinner;
	}
}
