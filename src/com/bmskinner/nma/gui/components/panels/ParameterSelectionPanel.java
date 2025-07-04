package com.bmskinner.nma.gui.components.panels;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.bmskinner.nma.analysis.image.GLCM.GLCMParameter;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.measure.MeasurementDimension;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.profiles.IProfileSegment;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.profiles.MissingLandmarkException;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.core.GlobalOptions;
import com.bmskinner.nma.logging.Loggable;

/**
 * Panel to set parameter options for clustering
 * 
 * @author bms41
 * @since 1.16.0
 *
 */
public class ParameterSelectionPanel extends OptionsPanel {

	private static final Logger LOGGER = Logger.getLogger(ParameterSelectionPanel.class.getName());

	public ParameterSelectionPanel(IAnalysisDataset dataset, HashOptions options) {
		super(dataset, options);
	}

	@Override
	protected JPanel createUI() {
		JPanel panel = new JPanel();
		BoxLayout layout = new BoxLayout(panel, BoxLayout.X_AXIS);
		panel.setLayout(layout);

		JPanel paramPanel = new JPanel(new FlowLayout());
		JPanel shape = createShapePanel();
		shape.setAlignmentY(Component.TOP_ALIGNMENT);

		JPanel stats = createStatsPanel();
		stats.setAlignmentY(Component.TOP_ALIGNMENT);

		JPanel texture = createTexturePanel();
		texture.setAlignmentY(Component.TOP_ALIGNMENT);

		paramPanel.add(shape);
		paramPanel.add(Box.createHorizontalGlue());
		paramPanel.add(stats);
		paramPanel.add(Box.createHorizontalGlue());

		if (GlobalOptions.getInstance().getBoolean(GlobalOptions.IS_GLCM_INTERFACE_KEY))
			paramPanel.add(texture);

		panel.add(paramPanel);
		panel.setBorder(BorderFactory.createTitledBorder("Parameters"));
		return panel;
	}

	@Override
	protected void setDefaults() {
		for (Measurement stat : Measurement.getNucleusStats())
			options.setBoolean(stat.toString(), false);

		for (ProfileType t : ProfileType.displayValues())
			options.setBoolean(t.toString(), false);

		options.setBoolean(ProfileType.ANGLE.toString(), true);
	}

	private JPanel createStatsPanel() {
		JPanel panel = new JPanel();
		GridBagLayout layout = new GridBagLayout();
		panel.setLayout(layout);

		List<JLabel> labels = new ArrayList<>();
		List<Component> fields = new ArrayList<>();

		for (Measurement stat : Measurement.getNucleusStats()) {
			if (stat.getDimension().equals(MeasurementDimension.NONE))
				continue;

			// Handle texture separately
			Measurement[] textureStats = GLCMParameter.toStats();
			if (Arrays.stream(textureStats).anyMatch(s -> s.equals(stat)))
				continue;

			JCheckBox box = new JCheckBox();
			box.addChangeListener(e -> options.setBoolean(stat.toString(), box.isSelected()));
			box.setForeground(Color.DARK_GRAY);
			box.setSelected(options.getBoolean(stat.toString()));
			JLabel label = new JLabel(stat.toString());
			labels.add(label);
			fields.add(box);
		}
		addLabelTextRows(labels, fields, layout, panel);
		return panel;
	}

	private JPanel createShapePanel() {
		JPanel panel = new JPanel();
		GridBagLayout layout = new GridBagLayout();
		panel.setLayout(layout);

		List<JLabel> labels = new ArrayList<>();
		List<Component> fields = new ArrayList<>();

		for (ProfileType t : ProfileType.displayValues()) {
			JCheckBox box = new JCheckBox();
			box.addChangeListener(e -> options.setBoolean(t.toString(), box.isSelected()));
			box.setForeground(Color.DARK_GRAY);
			box.setSelected(options.getBoolean(t.toString()));
			JLabel label = new JLabel(t.toString());
			labels.add(label);
			fields.add(box);
		}

		for (Measurement stat : Measurement.getNucleusStats()) {
			if (!stat.getDimension().equals(MeasurementDimension.NONE))
				continue;

			// Handle texture separately
			Measurement[] textureStats = GLCMParameter.toStats();
			if (Arrays.stream(textureStats).anyMatch(s -> s.equals(stat)))
				continue;

			JCheckBox box = new JCheckBox();
			box.addChangeListener(e -> options.setBoolean(stat.toString(), box.isSelected()));
			box.setForeground(Color.DARK_GRAY);
			box.setSelected(options.getBoolean(stat.toString()));
			JLabel label = new JLabel(stat.toString());
			labels.add(label);
			fields.add(box);
		}

		addLabelTextRows(labels, fields, layout, panel);
		return panel;
	}

	private JPanel createTexturePanel() {
		JPanel panel = new JPanel();
		GridBagLayout layout = new GridBagLayout();
		panel.setLayout(layout);

		List<JLabel> labels = new ArrayList<>();
		List<Component> fields = new ArrayList<>();

		for (Measurement stat : GLCMParameter.toStats()) {
			JCheckBox box = new JCheckBox();
			box.addChangeListener(e -> options.setBoolean(stat.toString(), box.isSelected()));
			box.setForeground(Color.DARK_GRAY);
			box.setSelected(options.getBoolean(stat.toString()));
			JLabel label = new JLabel(stat.toString());
			labels.add(label);
			fields.add(box);
		}

		addLabelTextRows(labels, fields, layout, panel);
		return panel;
	}

	private JPanel createSegmentsPanel() {
		JPanel panel = new JPanel();
		GridBagLayout layout = new GridBagLayout();
		panel.setLayout(layout);

		List<JLabel> labels = new ArrayList<>();
		List<Component> fields = new ArrayList<>();

		try {
			for (IProfileSegment s : dataset.getCollection().getProfileCollection()
					.getSegments(OrientationMark.REFERENCE)) {
				JCheckBox box = new JCheckBox();
				box.setForeground(Color.DARK_GRAY);
				box.addChangeListener(
						e -> options.setBoolean(s.getID().toString(), box.isSelected()));
				box.setSelected(options.getBoolean(s.getID().toString()));
				JLabel label = new JLabel("Length of " + s.getName());
				labels.add(label);
				fields.add(box);
			}
		} catch (MissingLandmarkException | SegmentUpdateException e) {
			LOGGER.log(Loggable.STACK, "Unable to get segments", e);
		}
		addLabelTextRows(labels, fields, layout, panel);
		return panel;
	}

}
