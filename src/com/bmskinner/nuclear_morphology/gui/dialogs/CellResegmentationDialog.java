/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


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
import com.bmskinner.nuclear_morphology.charting.charts.MorphologyChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.OutlineChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.ProfileChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.CoupledProfileOutlineChartPanel;
import com.bmskinner.nuclear_morphology.charting.charts.panels.CoupledProfileOutlineChartPanel.BorderPointEvent;
import com.bmskinner.nuclear_morphology.charting.charts.panels.CoupledProfileOutlineChartPanel.BorderPointEventListener;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.DefaultCell;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.Profileable;
import com.bmskinner.nuclear_morphology.components.Taggable;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.RotationMode;
import com.bmskinner.nuclear_morphology.gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;
import com.bmskinner.nuclear_morphology.gui.tabs.cells_detail.AbstractCellEditingDialog;
import com.bmskinner.nuclear_morphology.gui.tabs.cells_detail.CellViewModel;

/**
 * This dialog permits complete resegmentation of a cell via a coupled profile
 * and outline chart. Only one instance is created in the CellProfilePanel
 * 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class CellResegmentationDialog extends AbstractCellEditingDialog implements BorderPointEventListener {

    private CoupledProfileOutlineChartPanel panel;

    private boolean              isRunning  = false;
    private boolean              isSelectRP = false;
    private List<IBorderSegment> newSegments;
    private Map<Tag, Integer>    newTags;
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
            finer("Creating resegmentation dialog");
            this.setLayout(new BorderLayout());

            JPanel header = createHeader();
            this.add(header, BorderLayout.NORTH);

            table = new JTable(createTableModel(""));

            JPanel mainPanel = new JPanel(new BorderLayout());

            JFreeChart outlineChart = ConsensusNucleusChartFactory.makeEmptyChart();

            ExportableChartPanel profile = new ExportableChartPanel(
                    ProfileChartFactory.makeEmptyChart(ProfileType.ANGLE));
            ExportableChartPanel outline = new ExportableChartPanel(outlineChart);
            outline.setFixedAspectRatio(true);

            panel = new CoupledProfileOutlineChartPanel(profile, outline, null);
            panel.addBorderPointEventListener(this);

            mainPanel.add(profile, BorderLayout.SOUTH);
            mainPanel.add(outline, BorderLayout.CENTER);
            mainPanel.add(table, BorderLayout.WEST);
            this.add(mainPanel, BorderLayout.CENTER);
        } catch (Exception e) {
            error("Error making UI", e);
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
            newTags = new HashMap<Tag, Integer>();
            table.setModel(createTableModel(""));
            log("Select RP");
            setEnabled(false);
        });
        panel.add(reassignRpBtn);

        resegmentBtn = new JButton("Resegment");
        resegmentBtn.addActionListener(e -> {
            Taggable obj = (Taggable) taggableList.getSelectedItem();
            this.isRunning = true;
            newSegments = new ArrayList<IBorderSegment>();
            table.setModel(createTableModel("Not set"));
            table.getColumnModel().getColumn(COLUMN_STATE).setCellRenderer(new SegmentStateRenderer());
            segCount = 0;
            try {
				segStart = obj.getBorderIndex(Tag.REFERENCE_POINT);
			} catch (UnavailableBorderTagException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
            log("Select endpoint for segment 0");
            drawCurrentSegments(obj); // clear the segment chart
            setEnabled(false);
        });
        panel.add(resegmentBtn);

        reverseProfileBtn = new JButton("Flip profile");
        reverseProfileBtn.addActionListener(e -> {
            Taggable obj = (Taggable) taggableList.getSelectedItem();
            setCellChanged(true);
            obj.reverse();
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

        finer("Assigned all segments");
        try {

            List<IBorderSegment> tempList;
            if (segCount > 0) {
                tempList = IBorderSegment.copyWithoutLinking(newSegments);
            } else {
                tempList = new ArrayList<IBorderSegment>(); // for clearing the
                                                            // profile on start
                                                            // of resegmentation
            }
            // Get the segment ID to make the new segment
            UUID id = n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT).getSegments().get(segCount)
                    .getID();

            // Make a final segment after the last clicked position
            IBorderSegment last = IBorderSegment.newSegment(segStart, n.getBorderIndex(Tag.REFERENCE_POINT),
                    n.getBorderLength(), id);
            tempList.add(last);
            IBorderSegment.linkSegments(tempList);

            ISegmentedProfile newProfile = n.getProfile(ProfileType.ANGLE);
            newProfile.setSegments(tempList);
            finer("Segments added: ");
            finer(IBorderSegment.toString(tempList));
            finer("New profile:");
            finer(newProfile.toString());

            finer("RP index: " + n.getBorderIndex(Tag.REFERENCE_POINT));

            n.setProfile(ProfileType.ANGLE, newProfile);

        } catch (ProfileException e) {
            error("Cannot link segments", e);
        } catch (Exception e) {
            error("Error setting profile", e);
        }

        // Need to update OP to new segment boundary position

        // Draw the new cell
        updateCharts(workingCell);
    }

    private void resegmentationComplete() {

    	isRunning = false;
    	setEnabled(true);
    	table.getModel().setValueAt("OK", segCount, COLUMN_STATE);

    	Nucleus n = workingCell.getNucleus();
    	UUID id;
    	try {
    		id = n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT).getSegments().get(segCount).getID();


    		IBorderSegment last = IBorderSegment.newSegment(segStart, n.getBorderIndex(Tag.REFERENCE_POINT),
    				n.getBorderLength(), id);
    		newSegments.add(last);
    		finer("Added " + last.toString());

    		log("Resegmenting complete");

    	} catch (ProfileException | UnavailableBorderTagException | UnavailableProfileTypeException e) {
    		warn("Cannot get segmented profile");
    		fine("Error getting profile", e);
    		return;
    	}
    }

    private void moveRPComplete() {

        try {
            isSelectRP = false;
            setEnabled(true);

            int newRpIndex = newTags.get(Tag.REFERENCE_POINT);

            fine("Selected RP index: " + newRpIndex);

            // Make a new cell with the updated RP
            workingCell.getNucleus().setBorderTag(Tag.REFERENCE_POINT, newRpIndex);

            fine("Updated RP");

            // Draw the new cell
            updateCharts(workingCell);

            fine("Finished updating RP chart");

        } catch (Exception e) {
            error("Error moving RP", e);
        }
    }

    @Override
    protected void updateCharts(ICell cell) {

        Runnable r = () -> {

            finer("Making profile chart options");
            ChartOptions profileOptions = new ChartOptionsBuilder().setDatasets(dataset).setCell(cell)
                    .setNormalised(false).setAlignment(ProfileAlignment.LEFT).setTag(Tag.REFERENCE_POINT)
                    .setShowMarkers(false).setProfileType(ProfileType.ANGLE)
                    .setSwatch(GlobalOptions.getInstance().getSwatch()).setShowPoints(true).build();

            finer("Making outline chart options");
            ChartOptions outlineOptions = new ChartOptionsBuilder().setDatasets(dataset).setCell(cell)
                    .setRotationMode(RotationMode.ACTUAL).setShowSignals(false).setShowBorderTags(false)
                    .setShowAnnotations(true).setInvertYAxis(true) // only
                                                                   // invert for
                                                                   // actual
                    .setShowPoints(false).setCellularComponent(cell.getNucleus()).build();

            finer("Making charts");
            JFreeChart profileChart = new ProfileChartFactory(profileOptions).createProfileChart();
            JFreeChart outlineChart = new OutlineChartFactory(outlineOptions).makeCellOutlineChart();
            finer("Updating charts");
            panel.setObject(cell.getNucleus());
            panel.getProfilePanel().setChart(profileChart);
            panel.getOutlinePanel().setChart(outlineChart);
        };

        ThreadManager.getInstance().submit(r);
    }

    @Override
    public void borderPointEventReceived(BorderPointEvent event) {
        IBorderPoint p = event.getPoint();
        Taggable n = (Taggable) taggableList.getSelectedItem();

        if (isSelectRP) {
            setCellChanged(true);
            newTags.put(Tag.REFERENCE_POINT, n.getBorderIndex(p));
            moveRPComplete();
        }

        if (isRunning) {

            try {

                setCellChanged(true);
                UUID id = n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT).getSegments().get(segCount).getID();

                segStop = n.getBorderIndex(p);
                IBorderSegment seg = IBorderSegment.newSegment(segStart, segStop, n.getBorderLength(), id);
                newSegments.add(seg);
                finer("Added " + seg.toString());
                segStart = segStop;

                table.getModel().setValueAt("OK", segCount, COLUMN_STATE);
                segCount++;
                log("Select endpoint for segment " + segCount);

                drawCurrentSegments(n);

                // Check against the original cell segment count
                if (segCount == n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT).getSegmentCount()
                        - 1) {
                    resegmentationComplete();
                }

            } catch (ProfileException | UnavailableBorderTagException | UnavailableProfileTypeException e) {
                warn("Unable to update segments");
                fine("Error getting profiles", e);
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
