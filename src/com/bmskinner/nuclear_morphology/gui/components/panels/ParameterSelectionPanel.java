package com.bmskinner.nuclear_morphology.gui.components.panels;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.options.DefaultOptions;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.components.stats.StatisticDimension;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.DipTester;

/**
 * Panel to set parameter options for clustering
 * @author bms41
 * @since 1.16.0
 *
 */
public class ParameterSelectionPanel extends JPanel {
	
	private static final Logger LOGGER = Logger.getLogger(Loggable.ROOT_LOGGER);
	private final IAnalysisDataset dataset;
	private final HashOptions options;
	
	public ParameterSelectionPanel(IAnalysisDataset dataset, HashOptions options) {
		super();
		this.dataset = dataset;
		this.options = options;
		add(createUI());
	}
	
	private JPanel createUI() {
		JPanel panel = new JPanel();
		BoxLayout layout = new BoxLayout(panel, BoxLayout.X_AXIS);
		panel.setLayout(layout);
		
		panel.add(createStatsPanel());
		panel.add(createShapePanel());
		panel.add(createSsegmentsPanel());
		
		return panel;
	}
	
	
	private JPanel createStatsPanel() {
		JPanel panel = new JPanel();
		GridBagLayout layout = new GridBagLayout();
		panel.setLayout(layout);

		List<JLabel> labels = new ArrayList<>();
		List<Component> fields = new ArrayList<>();
		
		for (PlottableStatistic stat : PlottableStatistic.getNucleusStats((dataset.getCollection().getNucleusType()))) {
			if(stat.getDimension().equals(StatisticDimension.DIMENSIONLESS))
				continue;
			JCheckBox box = new JCheckBox();
			box.addChangeListener(e ->  options.setBoolean(stat.toString(), box.isSelected()));
			box.setForeground(Color.DARK_GRAY);

			JLabel label = new JLabel(stat.toString());
			labels.add(label);
			fields.add(box);
		}
		addLabelTextRows(labels, fields, layout, panel);
		
		panel.setBorder(BorderFactory.createTitledBorder("Size"));
		return panel;
	}
	
	private JPanel createShapePanel() {
		JPanel panel = new JPanel();
		GridBagLayout layout = new GridBagLayout();
		panel.setLayout(layout);

		List<JLabel> labels = new ArrayList<>();
		List<Component> fields = new ArrayList<>();
		
		for(ProfileType t : ProfileType.displayValues()) {
			JCheckBox box = new JCheckBox();
			box.addChangeListener(e ->  options.setBoolean(t.toString(), box.isSelected()));
			box.setForeground(Color.DARK_GRAY);

			JLabel label = new JLabel(t.toString());
			labels.add(label);
			fields.add(box);
		}
		
		for (PlottableStatistic stat : PlottableStatistic.getNucleusStats((dataset.getCollection().getNucleusType()))) {
			if(!stat.getDimension().equals(StatisticDimension.DIMENSIONLESS))
				continue;
			JCheckBox box = new JCheckBox();
			box.addChangeListener(e ->  options.setBoolean(stat.toString(), box.isSelected()));
			box.setForeground(Color.DARK_GRAY);

			JLabel label = new JLabel(stat.toString());
			labels.add(label);
			fields.add(box);
		}
		
		addLabelTextRows(labels, fields, layout, panel);
		panel.setBorder(BorderFactory.createTitledBorder("Shape"));
		return panel;
	}
	
	private JPanel createSsegmentsPanel() {
		JPanel panel = new JPanel();
		GridBagLayout layout = new GridBagLayout();
		panel.setLayout(layout);

		List<JLabel> labels = new ArrayList<>();
		List<Component> fields = new ArrayList<>();
		
		try {
			for (IBorderSegment s : dataset.getCollection().getProfileCollection().getSegments(Tag.REFERENCE_POINT)) {
				JCheckBox box = new JCheckBox();
				box.setForeground(Color.DARK_GRAY);
				box.addChangeListener(e->options.setBoolean(s.getID().toString(), box.isSelected()));
				JLabel label = new JLabel("Length of " + s.getName());
				labels.add(label);
				fields.add(box);
			}
		} catch(ProfileException | UnavailableBorderTagException e) {
			LOGGER.log(Loggable.STACK, "Unable to get segments", e);
		}
		addLabelTextRows(labels, fields, layout, panel);
		panel.setBorder(BorderFactory.createTitledBorder("Segments"));
		return panel;
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
