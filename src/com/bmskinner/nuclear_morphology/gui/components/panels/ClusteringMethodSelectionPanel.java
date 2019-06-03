package com.bmskinner.nuclear_morphology.gui.components.panels;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions.ClusteringMethod;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions.HierarchicalClusterMethod;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Options panel for clustering setup
 * @author ben
 * @since 1.16.0
 *
 */
public class ClusteringMethodSelectionPanel  extends OptionsPanel implements ActionListener {
	private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

	private static final String CLUSTER_METHOD_LBL  = "Distance method";
	private static final String CLUSTER_NUMBER_LBL = "Number of clusters";
	private static final String EM_ITERATIONS_LBL  = "Iterations";
	private static final String EM_CLUSTERING_LBL  = "Expectation maximisation";
	private static final String HC_CLUSTERING_LBL  = "Hierarchical";

	private JPanel   cardPanel;
	private JSpinner clusterNumberSpinner;
	private JSpinner iterationsSpinner;

	private JComboBox<HierarchicalClusterMethod> clusterMethodBox;

	private JRadioButton hcBtn;
	private JRadioButton emBtn;

	public ClusteringMethodSelectionPanel(IAnalysisDataset dataset, HashOptions options) {
		super(dataset, options);
	}

	@Override
	protected void setDefaults() {
		options.setString(IClusteringOptions.CLUSTER_METHOD_KEY, ClusteringMethod.HIERARCHICAL.name());
		options.setInt(IClusteringOptions.MANUAL_CLUSTER_NUMBER_KEY, IClusteringOptions.DEFAULT_MANUAL_CLUSTER_NUMBER);
		options.setString(IClusteringOptions.HIERARCHICAL_METHOD_KEY, IClusteringOptions.DEFAULT_HIERARCHICAL_METHOD.name());
		options.setInt(IClusteringOptions.EM_ITERATIONS_KEY, IClusteringOptions.DEFAULT_EM_ITERATIONS);
	}

	@Override
	protected JPanel createUI() {
		JPanel panel = new JPanel();
		BoxLayout layout = new BoxLayout(panel, BoxLayout.X_AXIS);
		panel.setLayout(layout);
		panel.add(createClusterMethodPanel());
		panel.setBorder(BorderFactory.createTitledBorder("Clustering method"));
		return panel;
	}


	private JPanel createClusterMethodPanel() {

		JPanel methodPanel = new JPanel();
		methodPanel.setLayout(new BoxLayout(methodPanel, BoxLayout.Y_AXIS));

		hcBtn = new JRadioButton(HC_CLUSTERING_LBL);
		hcBtn.setSelected(true);
		hcBtn.setAlignmentX(0);

		emBtn = new JRadioButton(EM_CLUSTERING_LBL);
		emBtn.setAlignmentX(0);

		// Group the radio buttons.
		ButtonGroup clusterTypeGroup = new ButtonGroup();
		clusterTypeGroup.add(hcBtn);
		clusterTypeGroup.add(emBtn);

		cardPanel = new JPanel(new CardLayout());
		cardPanel.add(createHierarchicalPanel(), HC_CLUSTERING_LBL);
		cardPanel.add(createEMPanel(), EM_CLUSTERING_LBL);
		CardLayout cl = (CardLayout) (cardPanel.getLayout());
		cl.show(cardPanel, HC_CLUSTERING_LBL);
		cardPanel.setAlignmentX(0);

		hcBtn.addActionListener(this);
		emBtn.addActionListener(this);

		methodPanel.add(hcBtn);
		methodPanel.add(emBtn);
		methodPanel.add(cardPanel);
		return methodPanel;
	}

	private JPanel createHierarchicalPanel() {
		JPanel panel = new JPanel();
		GridBagLayout layout = new GridBagLayout();
		panel.setLayout(layout);

		List<JLabel> labels = new ArrayList<>();
		List<Component> fields = new ArrayList<>();

		clusterMethodBox = new JComboBox<>(HierarchicalClusterMethod.values());
		clusterMethodBox.setSelectedItem(IClusteringOptions.DEFAULT_HIERARCHICAL_METHOD);
		clusterMethodBox.addActionListener(this);

		labels.add(new JLabel(CLUSTER_METHOD_LBL));
		fields.add(clusterMethodBox);

		SpinnerModel model = new SpinnerNumberModel(IClusteringOptions.DEFAULT_MANUAL_CLUSTER_NUMBER, // initial
				// value
				1, // min
				100, // max
				1); // step

		clusterNumberSpinner = new JSpinner(model);
		clusterNumberSpinner.setEnabled(true);

		labels.add(new JLabel(CLUSTER_NUMBER_LBL));
		fields.add(clusterNumberSpinner);

		clusterNumberSpinner.addChangeListener(e -> {
			JSpinner j = (JSpinner) e.getSource();
			try {
				j.commitEdit();
				options.setInt(IClusteringOptions.MANUAL_CLUSTER_NUMBER_KEY, (Integer) clusterNumberSpinner.getValue());

			} catch (Exception e1) {
				LOGGER.warning("Error reading value in cluster number field");
				LOGGER.log(Loggable.STACK, e1.getMessage(), e1);
			}
		});

		addLabelTextRows(labels, fields, layout, panel);
		return panel;
	}

	private JPanel createEMPanel() {

		JPanel panel = new JPanel();

		GridBagLayout layout = new GridBagLayout();
		panel.setLayout(layout);

		List<JLabel> labels = new ArrayList<>();
		List<Component> fields = new ArrayList<>();

		SpinnerModel model = new SpinnerNumberModel(IClusteringOptions.DEFAULT_EM_ITERATIONS, // initial
				// value
				1, // min
				1000, // max
				1); // step

		iterationsSpinner = new JSpinner(model);
		iterationsSpinner.addChangeListener(e -> {
			try {
				iterationsSpinner.commitEdit();
				options.setInt(IClusteringOptions.EM_ITERATIONS_KEY, (Integer) iterationsSpinner.getValue());
			} catch (ParseException e1) {
				LOGGER.log(Loggable.STACK,"Error reading value in iterations field", e1);
			}
		});

		labels.add(new JLabel(EM_ITERATIONS_LBL));
		fields.add(iterationsSpinner);

		this.addLabelTextRows(labels, fields, layout, panel);
		return panel;
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {

		// Set card panel based on selected radio button
		if (hcBtn.isSelected()) {

			CardLayout cl = (CardLayout) (cardPanel.getLayout());
			cl.show(cardPanel, HC_CLUSTERING_LBL);

			clusterNumberSpinner.setEnabled(true);

			options.setString(IClusteringOptions.CLUSTER_METHOD_KEY, ClusteringMethod.HIERARCHICAL.name());
			options.setInt(IClusteringOptions.MANUAL_CLUSTER_NUMBER_KEY, (Integer) clusterNumberSpinner.getValue());
			options.setString(IClusteringOptions.HIERARCHICAL_METHOD_KEY, ((HierarchicalClusterMethod) clusterMethodBox.getSelectedItem()).name());
		}

		if (emBtn.isSelected()) {
			CardLayout cl = (CardLayout) (cardPanel.getLayout());
			cl.show(cardPanel, EM_CLUSTERING_LBL);
			options.setString(IClusteringOptions.CLUSTER_METHOD_KEY, ClusteringMethod.EM.name());
			options.setInt(IClusteringOptions.EM_ITERATIONS_KEY, (Integer) iterationsSpinner.getValue());
		}
	}
}
