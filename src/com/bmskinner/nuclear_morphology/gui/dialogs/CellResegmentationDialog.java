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
package com.bmskinner.nuclear_morphology.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.charting.charts.ConsensusNucleusChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.OutlineChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.ProfileChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.CoupledProfileOutlineChartPanel;
import com.bmskinner.nuclear_morphology.charting.charts.panels.CoupledProfileOutlineChartPanel.BorderPointEvent;
import com.bmskinner.nuclear_morphology.charting.charts.panels.CoupledProfileOutlineChartPanel.BorderPointEventListener;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.MissingComponentException;
import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
import com.bmskinner.nuclear_morphology.components.Taggable;
import com.bmskinner.nuclear_morphology.components.cells.DefaultCell;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.profiles.DefaultProfileSegment;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileSegment;
import com.bmskinner.nuclear_morphology.components.profiles.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.MissingProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.RotationMode;
import com.bmskinner.nuclear_morphology.gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;
import com.bmskinner.nuclear_morphology.gui.tabs.cells_detail.AbstractCellEditingDialog;
import com.bmskinner.nuclear_morphology.gui.tabs.cells_detail.CellViewModel;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * This dialog permits complete resegmentation of a cell via a coupled profile
 * and outline chart. Only one instance is created in the CellProfilePanel
 * 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class CellResegmentationDialog extends AbstractCellEditingDialog implements BorderPointEventListener {
	
	private static final Logger LOGGER = Logger.getLogger(CellResegmentationDialog.class.getName());

    private CoupledProfileOutlineChartPanel panel;

    private boolean              isRunning  = false;
    private boolean              isSelectRP = false;
    private List<IProfileSegment> newSegments;
    private Map<Landmark, Integer>    newTags;
    int                          segCount   = 0;
    int                          segStart   = 0;
    int                          segStop    = 0;

    private JButton resegmentBtn;
    private JButton reassignRpBtn;
    private JButton reverseProfileBtn;
    private JComboBox<Taggable> taggableList = new JComboBox<Taggable>();

    private JTable           table;
    private static final int COLUMN_STATE = 1;

    public CellResegmentationDialog(CellViewModel model) {
        super(model);
        panel.getOutlinePanel().restoreAutoBounds(); // fixed aspect must be set
                                                     // after components are
                                                     // packed
    }

    @Override
    public void load(final ICell cell, final IAnalysisDataset dataset) {

        super.load(cell, dataset);
        
        List<Taggable> taggables = cell.getTaggables();
        
        table.setModel(createTableModel(""));
        this.setTitle("Resegmenting " + taggables.get(0).toString());

        ComboBoxModel<Taggable> m = new DefaultComboBoxModel<Taggable>(taggables.toArray(new Taggable[0]));
        taggableList.setModel(m);
        taggableList.setSelectedIndex(0);
        taggableList.revalidate();
        taggableList.repaint();
        updateCharts(cell);
        pack();
        setVisible(true);
    }

    @Override
    protected void createUI() {

        try {
            LOGGER.finer( "Creating resegmentation dialog");
            this.setLayout(new BorderLayout());

            JPanel header = createHeader();
            this.add(header, BorderLayout.NORTH);

            table = new JTable(createTableModel(""));

            JPanel mainPanel = new JPanel(new BorderLayout());

            JFreeChart outlineChart = ConsensusNucleusChartFactory.createEmptyChart();

            ExportableChartPanel profile = new ExportableChartPanel(
                    ProfileChartFactory.createEmptyChart(ProfileType.ANGLE));
            ExportableChartPanel outline = new ExportableChartPanel(outlineChart);
            outline.setFixedAspectRatio(true);

            panel = new CoupledProfileOutlineChartPanel(profile, outline, null);
            panel.addBorderPointEventListener(this);

            mainPanel.add(profile, BorderLayout.SOUTH);
            mainPanel.add(outline, BorderLayout.CENTER);
            mainPanel.add(table, BorderLayout.WEST);
            this.add(mainPanel, BorderLayout.CENTER);
        } catch (Exception e) {
            LOGGER.log(Loggable.STACK, "Error making UI", e);
        }
    }

    private TableModel createTableModel(String message) {

        DefaultTableModel model = new DefaultTableModel();

        int segTotal = dataset == null ? 1 : dataset.getCollection().getProfileManager().getSegmentCount();

        List<Object> colOne = new ArrayList<Object>();

        for (int i = 0; i < segTotal; i++) {
            colOne.add("Segment " + i);
        }
        model.addColumn("Detail", colOne.toArray());

        List<Object> colTwo = new ArrayList<Object>();
        for (int i = 0; i < segTotal; i++) {
            colTwo.add(message);
        }
        model.addColumn("Set", colTwo.toArray());

        return model;

    }

    private JPanel createHeader() {
        JPanel panel = new JPanel(new FlowLayout());
        
        taggableList = new JComboBox<Taggable>(new Taggable[0]);
        taggableList.addActionListener(e -> {
            Taggable obj = (Taggable) taggableList.getSelectedItem();
            drawCurrentSegments(obj);
        });
        panel.add(taggableList);
        

        reassignRpBtn = new JButton("Reassign RP");
        reassignRpBtn.addActionListener(e -> {

            this.isSelectRP = true;
            newTags = new HashMap<Landmark, Integer>();
            table.setModel(createTableModel(""));
            LOGGER.info("Select RP");
            setEnabled(false);
        });
        panel.add(reassignRpBtn);

        resegmentBtn = new JButton("Resegment");
        resegmentBtn.addActionListener(e -> {
            Taggable obj = (Taggable) taggableList.getSelectedItem();
            this.isRunning = true;
            newSegments = new ArrayList<IProfileSegment>();
            table.setModel(createTableModel("Not set"));
            table.getColumnModel().getColumn(COLUMN_STATE).setCellRenderer(new SegmentStateRenderer());
            segCount = 0;
            try {
				segStart = obj.getBorderIndex(Landmark.REFERENCE_POINT);
			} catch (MissingLandmarkException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
            LOGGER.info("Select endpoint for segment 0");
            drawCurrentSegments(obj); // clear the segment chart
            setEnabled(false);
        });
        panel.add(resegmentBtn);

        reverseProfileBtn = new JButton("Flip profile");
        reverseProfileBtn.addActionListener(e -> {
            Taggable obj = (Taggable) taggableList.getSelectedItem();
            setCellChanged(true);
            try {
				obj.reverse();
			} catch (MissingComponentException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (ProfileException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
            updateCharts(workingCell);
        });
        panel.add(reverseProfileBtn);

        JButton undoBtn = new JButton("Undo");
        undoBtn.addActionListener(e -> {
            workingCell = new DefaultCell(cell);
            table.setModel(createTableModel(""));
            updateCharts(workingCell);

        });
        panel.add(undoBtn);

        return panel;
    }

    @Override
    public void setEnabled(boolean b) {
        resegmentBtn.setEnabled(b);
        reassignRpBtn.setEnabled(b);
        reverseProfileBtn.setEnabled(b);
        taggableList.setEnabled(b);
    }

    /**
     * Draw the segments currently assigned to the nucleus
     */
    private void drawCurrentSegments(Taggable n) {

        LOGGER.finer( "Assigned all segments");
        try {

            List<IProfileSegment> tempList;
            if (segCount > 0) {
                tempList = IProfileSegment.copyWithoutLinking(newSegments);
            } else {
                tempList = new ArrayList<>(); // for clearing the
                                                            // profile on start
                                                            // of resegmentation
            }
            // Get the segment ID to make the new segment
            UUID id = n.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT).getSegments().get(segCount)
                    .getID();

            // Make a final segment after the last clicked position
            IProfileSegment last = new DefaultProfileSegment(segStart, n.getBorderIndex(Landmark.REFERENCE_POINT),
                    n.getBorderLength(), id);
            tempList.add(last);
            IProfileSegment.linkSegments(tempList);

            ISegmentedProfile newProfile = n.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT);
            newProfile.setSegments(tempList);
            LOGGER.finer( "Segments added: ");
            LOGGER.finer( IProfileSegment.toString(tempList));
            LOGGER.finer( "New profile:");
            LOGGER.finer( newProfile.toString());

            LOGGER.finer( "RP index: " + n.getBorderIndex(Landmark.REFERENCE_POINT));

            n.setSegments(newProfile.getSegments());

        } catch (ProfileException e) {
            LOGGER.log(Loggable.STACK, "Cannot link segments", e);
        } catch (Exception e) {
            LOGGER.log(Loggable.STACK, "Error setting profile", e);
        }

        // Need to update OP to new segment boundary position

        // Draw the new cell
        updateCharts(workingCell);
    }

    private void resegmentationComplete() {

    	isRunning = false;
    	setEnabled(true);
    	table.getModel().setValueAt("OK", segCount, COLUMN_STATE);

    	Nucleus n = workingCell.getPrimaryNucleus();
    	UUID id;
    	try {
    		id = n.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT).getSegments().get(segCount).getID();


    		IProfileSegment last = new DefaultProfileSegment(segStart, n.getBorderIndex(Landmark.REFERENCE_POINT),
    				n.getBorderLength(), id);
    		newSegments.add(last);
    		LOGGER.finer( "Added " + last.toString());

    	 LOGGER.info("Resegmenting complete");

    	} catch (ProfileException | MissingLandmarkException | MissingProfileException e) {
    		LOGGER.warning("Cannot get segmented profile");
    	 LOGGER.log(Loggable.STACK, "Error getting profile", e);
    		return;
    	}
    }

    private void moveRPComplete() {

        try {
            isSelectRP = false;
            setEnabled(true);

            int newRpIndex = newTags.get(Landmark.REFERENCE_POINT);

            LOGGER.fine("Selected RP index: " + newRpIndex);

            // Make a new cell with the updated RP
            workingCell.getPrimaryNucleus().setLandmark(Landmark.REFERENCE_POINT, newRpIndex);

            LOGGER.fine("Updated RP");

            // Draw the new cell
            updateCharts(workingCell);

            LOGGER.fine("Finished updating RP chart");

        } catch (Exception e) {
            LOGGER.log(Loggable.STACK, "Error moving RP", e);
        }
    }

    @Override
    protected void updateCharts(ICell cell) {

        Runnable r = () -> {

            LOGGER.finer( "Making profile chart options");
            ChartOptions profileOptions = new ChartOptionsBuilder().setDatasets(dataset).setCell(cell)
                    .setNormalised(false).setAlignment(ProfileAlignment.LEFT).setTag(Landmark.REFERENCE_POINT)
                    .setShowMarkers(false).setProfileType(ProfileType.ANGLE)
                    .setSwatch(GlobalOptions.getInstance().getSwatch()).setShowPoints(true).build();

            LOGGER.finer( "Making outline chart options");
            ChartOptions outlineOptions = new ChartOptionsBuilder().setDatasets(dataset).setCell(cell)
                    .setRotationMode(RotationMode.ACTUAL).setShowSignals(false).setShowBorderTags(false)
                    .setShowAnnotations(true).setInvertYAxis(true) // only
                                                                   // invert for
                                                                   // actual
                    .setShowPoints(false).addCellularComponent(cell.getPrimaryNucleus()).build();

            LOGGER.finer( "Making charts");
            JFreeChart profileChart = new ProfileChartFactory(profileOptions).createProfileChart();
            JFreeChart outlineChart = new OutlineChartFactory(outlineOptions).makeCellOutlineChart();
            LOGGER.finer( "Updating charts");
            panel.setObject(cell.getPrimaryNucleus());
            panel.getProfilePanel().setChart(profileChart);
            panel.getOutlinePanel().setChart(outlineChart);
        };

        ThreadManager.getInstance().submit(r);
    }

    @Override
    public void borderPointEventReceived(BorderPointEvent event) {
        IPoint p = event.getPoint();
        Taggable n = (Taggable) taggableList.getSelectedItem();

        if (isSelectRP) {
            setCellChanged(true);
            newTags.put(Landmark.REFERENCE_POINT, n.getBorderIndex(p));
            moveRPComplete();
        }

        if (isRunning) {

            try {

                setCellChanged(true);
                UUID id = n.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT).getSegments().get(segCount).getID();

                segStop = n.getBorderIndex(p);
                IProfileSegment seg = new DefaultProfileSegment(segStart, segStop, n.getBorderLength(), id);
                newSegments.add(seg);
                LOGGER.finer( "Added " + seg.toString());
                segStart = segStop;

                table.getModel().setValueAt("OK", segCount, COLUMN_STATE);
                segCount++;
                LOGGER.info("Select endpoint for segment " + segCount);

                drawCurrentSegments(n);

                // Check against the original cell segment count
                if (segCount == n.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT).getSegmentCount()
                        - 1) {
                    resegmentationComplete();
                }

            } catch (ProfileException | MissingLandmarkException | MissingProfileException e) {
                LOGGER.warning("Unable to update segments");
                LOGGER.log(Loggable.STACK, "Error getting profiles", e);
            }

        }

    }

    public class SegmentStateRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {

            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            setBackground(Color.WHITE);
            if (value.toString().equals("OK")) {
                setBackground(Color.GREEN);
            }
            return this;
        }
    }

}
