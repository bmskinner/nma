package com.bmskinner.nuclear_morphology.gui.components.panels;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;

/**
 * A panel that allows options to be selected for downstream analyses
 * @author bms41
 * @since 1.16.0
 *
 */
public abstract class OptionsPanel extends JPanel {
	
	protected final IAnalysisDataset dataset;
	protected final HashOptions options;
	
	public OptionsPanel(IAnalysisDataset dataset, HashOptions options) {
		super();
		this.dataset = dataset;
		this.options = options;
		setDefaults();
		this.setLayout(new BorderLayout());
		add(createUI(), BorderLayout.CENTER);
	}

	protected abstract void setDefaults();
	
	protected abstract JPanel createUI();
	
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
