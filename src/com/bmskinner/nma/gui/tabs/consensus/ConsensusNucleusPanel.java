/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nma.gui.tabs.consensus;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.ICellCollection;
import com.bmskinner.nma.core.GlobalOptions;
import com.bmskinner.nma.gui.Labels;
import com.bmskinner.nma.gui.events.ConsensusUpdatedListener;
import com.bmskinner.nma.gui.events.ProfilesUpdatedListener;
import com.bmskinner.nma.gui.events.ScaleUpdatedListener;
import com.bmskinner.nma.gui.events.SwatchUpdatedListener;
import com.bmskinner.nma.gui.events.UserActionController;
import com.bmskinner.nma.gui.events.UserActionEvent;
import com.bmskinner.nma.gui.tabs.ChartDetailPanel;
import com.bmskinner.nma.visualisation.charts.AbstractChartFactory;
import com.bmskinner.nma.visualisation.charts.ConsensusNucleusChartFactory;
import com.bmskinner.nma.visualisation.charts.panels.ConsensusNucleusChartPanel;
import com.bmskinner.nma.visualisation.options.ChartOptions;
import com.bmskinner.nma.visualisation.options.ChartOptionsBuilder;

@SuppressWarnings("serial")
public class ConsensusNucleusPanel extends ChartDetailPanel
		implements ChangeListener, ConsensusUpdatedListener,
		ScaleUpdatedListener, SwatchUpdatedListener, ProfilesUpdatedListener {

	private static final Logger LOGGER = Logger.getLogger(ConsensusNucleusPanel.class.getName());

	private static final String MESH_FACES_LBL = "Mesh faces";
	private static final String MESH_EDGES_LBL = "Mesh edges";
	private static final String MESH_VERTICES_LBL = "Mesh vertices";
	private static final String MESH_SIZE_LBL = "Mesh size";
	private static final String SHOW_MESH_LBL = "Show mesh";
	private static final String PANEL_TITLE = "Consensus panel";

	private ConsensusNucleusChartPanel consensusChartPanel;
	private JButton runRefoldingButton;

	/** Controls for rotating and translating the consensus */
	private JPanel offsetsPanel;

	// Debugging tools for the nucleus mesh - not visible in the final panel
	private JCheckBox showMeshBox;
	private JCheckBox showMeshVerticesBox;
	private JCheckBox showMeshEdgesBox;
	private JCheckBox showMeshFacesBox;
	private JSpinner meshSizeSpinner;

	public ConsensusNucleusPanel() {
		super();
		this.setLayout(new BorderLayout());
		JFreeChart consensusChart = ConsensusNucleusChartFactory.createEmptyChart();
		consensusChartPanel = new ConsensusNucleusChartPanel(consensusChart);

		runRefoldingButton = new JButton(Labels.Consensus.REFOLD_BTN_LBL);

		runRefoldingButton.addActionListener(e -> {
			UserActionController.getInstance().userActionEventReceived(
					new UserActionEvent(this, UserActionEvent.REFOLD_CONSENSUS, getDatasets()));
			runRefoldingButton.setVisible(false);
		});

		runRefoldingButton.setVisible(false);
		consensusChartPanel.add(runRefoldingButton);

		add(consensusChartPanel, BorderLayout.CENTER);

		offsetsPanel = createOffsetsPanel();
		add(offsetsPanel, BorderLayout.EAST);
		offsetsPanel.setVisible(false);
		uiController.addConsensusUpdatedListener(this);
		uiController.addSwatchUpdatedListener(this);
		uiController.addScaleUpdatedListener(this);
		uiController.addProfilesUpdatedListener(this);
	}

	@Override
	public String getPanelTitle() {
		return PANEL_TITLE;
	}

	@Override
	public synchronized void setLoading() {
		consensusChartPanel.setChart(AbstractChartFactory.createLoadingChart());
	}

	/**
	 * Force the chart panel to restore bounds
	 */
	public void restoreAutoBounds() {
		consensusChartPanel.restoreAutoBounds();
	}

	private JPanel createOffsetsPanel() {
		JPanel panel = new JPanel(new BorderLayout());

		JPanel rotatePanel = createRotationPanel();
		panel.add(rotatePanel, BorderLayout.NORTH);

		/* Used for debugging only - do not include in releases */
		JPanel meshPanel = createMeshPanel();

		if (GlobalOptions.getInstance().getBoolean(GlobalOptions.IS_DEBUG_INTERFACE_KEY))
			panel.add(meshPanel, BorderLayout.CENTER);

		JPanel offsetPanel = createTranslatePanel();
		panel.add(offsetPanel, BorderLayout.SOUTH);

		return panel;
	}

	private JPanel createMeshPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		showMeshBox = new JCheckBox(SHOW_MESH_LBL);
		showMeshBox.setSelected(false);
		showMeshBox.addChangeListener(this);

		SpinnerNumberModel meshSizeModel = new SpinnerNumberModel(10, 2, 100, 1);
		meshSizeSpinner = new JSpinner(meshSizeModel);
		meshSizeSpinner.addChangeListener(this);
		meshSizeSpinner.setToolTipText(MESH_SIZE_LBL);
		JSpinner.NumberEditor meshNumberEditor = new JSpinner.NumberEditor(meshSizeSpinner, "0");
		meshSizeSpinner.setEditor(meshNumberEditor);

		showMeshVerticesBox = new JCheckBox(MESH_VERTICES_LBL);
		showMeshVerticesBox.setSelected(false);
		showMeshVerticesBox.setEnabled(false);
		showMeshVerticesBox.addChangeListener(this);

		showMeshEdgesBox = new JCheckBox(MESH_EDGES_LBL);
		showMeshEdgesBox.setSelected(true);
		showMeshEdgesBox.setEnabled(false);
		showMeshEdgesBox.addChangeListener(this);

		showMeshFacesBox = new JCheckBox(MESH_FACES_LBL);
		showMeshFacesBox.setSelected(false);
		showMeshFacesBox.setEnabled(false);
		showMeshFacesBox.addChangeListener(this);

		panel.add(showMeshBox);
		panel.add(meshSizeSpinner);
		panel.add(showMeshVerticesBox);
		panel.add(showMeshEdgesBox);
		panel.add(showMeshFacesBox);

		return panel;

	}

	private JPanel createTranslatePanel() {
		JPanel panel = new JPanel(new GridBagLayout());

		GridBagConstraints constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.BOTH;
		constraints.gridwidth = 1;
		constraints.gridheight = 1;
		constraints.gridx = 1;
		constraints.gridy = 0;
		constraints.anchor = GridBagConstraints.CENTER;

		JButton moveUp = new JButton(Labels.Consensus.INCREASE_Y_LBL);
		moveUp.setToolTipText(Labels.Consensus.INCREASE_Y_TOOLTIP);

		moveUp.addActionListener(
				e -> UserActionController.getInstance()
						.consensusTranslationUpdateReceived(activeDataset(), 0, 1));
		panel.add(moveUp, constraints);

		JButton moveDown = new JButton(Labels.Consensus.DECREASE_Y_LBL);
		moveDown.setToolTipText(Labels.Consensus.DECREASE_Y_TOOLTIP);
		moveDown.addActionListener(
				e -> UserActionController.getInstance()
						.consensusTranslationUpdateReceived(activeDataset(), 0, -1));

		constraints.gridx = 1;
		constraints.gridy = 2;
		panel.add(moveDown, constraints);

		JButton moveLeft = new JButton(Labels.Consensus.DECREASE_X_LBL);
		moveLeft.setToolTipText(Labels.Consensus.DECREASE_X_TOOLTIP);
		moveLeft.addActionListener(
				e -> UserActionController.getInstance()
						.consensusTranslationUpdateReceived(activeDataset(), -1, 0));

		constraints.gridx = 0;
		constraints.gridy = 1;
		panel.add(moveLeft, constraints);

		JButton moveRight = new JButton(Labels.Consensus.INCREASE_X_LBL);
		moveRight.setToolTipText(Labels.Consensus.INCREASE_X_TOOLTIP);
		moveRight.addActionListener(
				e -> UserActionController.getInstance()
						.consensusTranslationUpdateReceived(activeDataset(), 1, 0));

		constraints.gridx = 2;
		constraints.gridy = 1;
		panel.add(moveRight, constraints);

		JButton moveRst = new JButton(Labels.Consensus.RESET_LBL);
		moveRst.setToolTipText(Labels.Consensus.RESET_COM_TOOLTIP);
		moveRst.addActionListener(
				e -> UserActionController.getInstance()
						.consensusTranslationResetReceived(activeDataset()));

		constraints.gridx = 1;
		constraints.gridy = 1;
		panel.add(moveRst, constraints);

		return panel;
	}

	private JPanel createRotationPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.BOTH;
		constraints.gridwidth = 1;
		constraints.gridheight = 1;
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.anchor = GridBagConstraints.CENTER;

		JButton rotateFwd = new JButton(Labels.Consensus.DECREASE_ROTATION_LBL);
		rotateFwd.setToolTipText(Labels.Consensus.DECREASE_ROTATION_TOOLTIP);
		rotateFwd.addActionListener(
				e -> UserActionController.getInstance()
						.consensusRotationUpdateReceived(activeDataset(), 1));

		panel.add(rotateFwd, constraints);

		JButton rotateBck = new JButton(Labels.Consensus.INCREASE_ROTATION_LBL);
		rotateBck.setToolTipText(Labels.Consensus.INCREASE_ROTATION_TOOLTIP);

		rotateBck.addActionListener(
				e -> UserActionController.getInstance()
						.consensusRotationUpdateReceived(activeDataset(), -1));

		constraints.gridx = 2;
		constraints.gridy = 0;
		panel.add(rotateBck, constraints);

		JButton rotateRst = new JButton(Labels.Consensus.RESET_LBL);
		rotateRst.setToolTipText(Labels.Consensus.RESET_ROTATION_TOOLTIP);

		rotateRst.addActionListener(e -> {
			activeDataset().getCollection().rotateConsensus(0);
			uiController.fireConsensusNucleusChanged(activeDataset());
		});

		constraints.gridx = 1;
		constraints.gridy = 0;
		panel.add(rotateRst, constraints);

		JButton refoldBtn = new JButton(Labels.Consensus.RE_REFOLD_LBL);
		refoldBtn.addActionListener(e -> UserActionController.getInstance().userActionEventReceived(
				new UserActionEvent(this, UserActionEvent.REFOLD_CONSENSUS,
						List.of(activeDataset()))));

		constraints.gridwidth = 3;
		constraints.gridheight = 1;
		constraints.gridx = 0;
		constraints.gridy = 2;
		panel.add(refoldBtn, constraints);

		return panel;
	}

	@Override
	protected synchronized JFreeChart createPanelChartType(@NonNull ChartOptions options) {
		return new ConsensusNucleusChartFactory(options).makeConsensusChart();
	}

	@Override
	protected synchronized void updateSingle() {
		super.updateSingle();

		showMeshVerticesBox.setEnabled(showMeshBox.isSelected());
		showMeshEdgesBox.setEnabled(showMeshBox.isSelected());
		showMeshFacesBox.setEnabled(showMeshBox.isSelected());

		ChartOptions options = new ChartOptionsBuilder().setDatasets(getDatasets())
				.setScale(GlobalOptions.getInstance().getScale())
				.setSwatch(GlobalOptions.getInstance().getSwatch())
				.setShowMesh(showMeshBox.isSelected())
				.setShowMeshVertices(showMeshVerticesBox.isSelected())
				.setShowMeshEdges(showMeshEdgesBox.isSelected())
				.setShowMeshFaces(showMeshFacesBox.isSelected())
				.setStraightenMesh(false)
				.setShowAnnotations(false)
				.setShowXAxis(false)
				.setShowYAxis(false)
				.setTarget(consensusChartPanel).build();

		setChart(options);

		if (activeDataset() == null) {
			runRefoldingButton.setVisible(false);
			offsetsPanel.setVisible(false);
			return;
		}
		ICellCollection collection = activeDataset().getCollection();
		// hide the refold button when not needed
		runRefoldingButton.setVisible(!collection.hasConsensus());
		offsetsPanel.setVisible(collection.hasConsensus());

		consensusChartPanel.restoreAutoBounds();
	}

	@Override
	protected synchronized void updateMultiple() {

		ChartOptions options = new ChartOptionsBuilder()
				.setDatasets(getDatasets())
				.setScale(GlobalOptions.getInstance().getScale())
				.setSwatch(GlobalOptions.getInstance().getSwatch())
				.setTarget(consensusChartPanel).build();

		setChart(options);

		// Only show the refold button if no selected datasets have a consensus
		runRefoldingButton.setVisible(
				getDatasets().stream().noneMatch(d -> d.getCollection().hasConsensus()));
		offsetsPanel.setVisible(false);
	}

	@Override
	protected synchronized void updateNull() {

		consensusChartPanel.setChart(ConsensusNucleusChartFactory.createEmptyChart());
		runRefoldingButton.setVisible(false);
		offsetsPanel.setVisible(false);
		consensusChartPanel.restoreAutoBounds();
	}

	@Override
	public void stateChanged(ChangeEvent arg0) {
		update(getDatasets());
	}

	@Override
	public void consensusUpdated(List<IAnalysisDataset> datasets) {
		refreshCache(datasets);
	}

	@Override
	public void consensusUpdated(IAnalysisDataset dataset) {
		refreshCache(dataset);
	}

	@Override
	public void scaleUpdated(List<IAnalysisDataset> datasets) {
		refreshCache(datasets);
	}

	@Override
	public void scaleUpdated(IAnalysisDataset dataset) {
		refreshCache(dataset);
	}

	@Override
	public void scaleUpdated() {
		update();
	}

	@Override
	public void globalPaletteUpdated() {
		update();
	}

	@Override
	public void colourUpdated(IAnalysisDataset dataset) {
		refreshCache(dataset);
	}

	@Override
	public void consensusFillStateUpdated() {
		refreshCache();
	}

	@Override
	public void profilesUpdated(List<IAnalysisDataset> datasets) {
		refreshCache(datasets);

	}

	@Override
	public void profilesUpdated(IAnalysisDataset dataset) {
		refreshCache(dataset);
	}

}
