package com.bmskinner.nuclear_morphology.gui.components.panels;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
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
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.components.stats.StatisticDimension;
import com.bmskinner.nuclear_morphology.logging.Loggable;

public class DimensionalReductionSelectionPanel extends JPanel {
	
	private static final double MIN_PERPLEXITY = 5;
    private static final double MAX_PERPLEXITY = 10000;
    private static final double STEP_PERPLEXITY = 1;
	
	private static final Logger LOGGER = Logger.getLogger(Loggable.ROOT_LOGGER);
	private final IAnalysisDataset dataset;
	private final HashOptions options;
	
	public DimensionalReductionSelectionPanel(IAnalysisDataset dataset, HashOptions options) {
		super();
		this.dataset = dataset;
		this.options = options;
		setDefaults();
		add(createUI());
	}
	

	private void setDefaults() {
		options.set(OptionsFactory.makeDefaultTsneOptions());
	}
	
	private JPanel createUI() {
		JPanel panel = new JPanel();
		BoxLayout layout = new BoxLayout(panel, BoxLayout.X_AXIS);
		panel.setLayout(layout);
		
		panel.add(createTsnePanel());
		
		return panel;
	}
		
	private JPanel createTsnePanel() {

		JPanel panel = new JPanel();
		GridBagLayout layout = new GridBagLayout();
		panel.setLayout(layout);

		List<JLabel> labels = new ArrayList<>();
		List<Component> fields = new ArrayList<>();

		labels.add(new JLabel(ProfileTsneMethod.MAX_ITERATIONS_KEY));
		fields.add(makeMaxIterationsSpinner());

		labels.add(new JLabel(ProfileTsneMethod.PERPLEXITY_KEY));
		fields.add(makePerplexitySpinner());

		addLabelTextRows(labels, fields, layout, panel);
		panel.setBorder(BorderFactory.createTitledBorder("Dimensions"));
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
    
	
    /**
     * Add components to a container via a list
     * 
     * @param labels the list of labels
     * @param fields the list of components
     * @param gridbag the layout
     * @param container the container to add the labels and fields to
     */
    protected void addLabelTextRows(List<JLabel> labels, List<Component> fields, GridBagLayout gridbag,
            Container container) {
        JLabel[] labelArray = labels.toArray(new JLabel[0]);
        Component[] fieldArray = fields.toArray(new Component[0]);
        addLabelTextRows(labelArray, fieldArray, gridbag, container);
    }

    /**
     * Add components to a container via arrays
     * 
     * @param labels the list of labels
     * @param fields the list of components
     * @param gridbag the layout
     * @param container the container to add the labels and fields to
     */
    protected void addLabelTextRows(JLabel[] labels, Component[] fields, GridBagLayout gridbag, Container container) {
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHEAST;
        int numLabels = labels.length;

        for (int i = 0; i < numLabels; i++) {
            c.gridwidth = 1; // next-to-last
            c.fill = GridBagConstraints.NONE; // reset to default
            c.weightx = 0.0; // reset to default
            container.add(labels[i], c);

            Dimension minSize = new Dimension(10, 5);
            Dimension prefSize = new Dimension(10, 5);
            Dimension maxSize = new Dimension(Short.MAX_VALUE, 5);
            c.gridwidth = GridBagConstraints.RELATIVE; // next-to-last
            c.fill = GridBagConstraints.NONE; // reset to default
            c.weightx = 0.0; // reset to default
            container.add(new Box.Filler(minSize, prefSize, maxSize), c);

            c.gridwidth = GridBagConstraints.REMAINDER; // end row
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;
            container.add(fields[i], c);
        }
    }

}
