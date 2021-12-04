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
package com.bmskinner.nuclear_morphology.gui.tabs.cells_detail;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.Range;
import org.jfree.data.xy.XYDataset;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.charting.charts.ConsensusNucleusChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.OutlineChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.overlays.EllipticalOverlay;
import com.bmskinner.nuclear_morphology.charting.charts.overlays.EllipticalOverlayObject;
import com.bmskinner.nuclear_morphology.charting.charts.panels.CoupledProfileOutlineChartPanel.BorderPointEvent;
import com.bmskinner.nuclear_morphology.charting.charts.panels.CoupledProfileOutlineChartPanel.BorderPointEventListener;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
import com.bmskinner.nuclear_morphology.components.UnavailableBorderPointException;
import com.bmskinner.nuclear_morphology.components.cells.DefaultCell;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.profiles.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.MissingProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.gui.RotationMode;
import com.bmskinner.nuclear_morphology.gui.components.panels.DualChartPanel;
import com.bmskinner.nuclear_morphology.gui.dialogs.CellResegmentationDialog;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.FloatPolygon;

/**
 * This dialog allows the border points in a CellularComponent to be removed or
 * moved. Upon completion, the border FloatPolygon is re-interpolated to provide
 * a new BorderPoint list
 * 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class CellBorderAdjustmentDialog extends AbstractCellEditingDialog implements BorderPointEventListener,
         MouseListener, MouseMotionListener, MouseWheelListener {
	
	private static final Logger LOGGER = Logger.getLogger(CellBorderAdjustmentDialog.class.getName());

    private DualChartPanel dualPanel;

    Map<IPoint, XYShapeAnnotation> selectedPoints = new HashMap<IPoint, XYShapeAnnotation>();

    private boolean canMove = false; // set if a point can be moved or not

    private double initialMovePointY = 0;
    private double finalMovePointY   = 0;

    private double initialMovePointX = 0;
    private double finalMovePointX   = 0;

    /**
     * flag if the cell must be manually resegmented - e.g if profile creation
     * fails due to segments becomming too small
     */
    private boolean mustResegment = false;

    private XYItemEntity xyItemEntity = null;

    private EllipticalOverlayObject ellipse;

    private JToggleButton selectToggle;
    private JToggleButton deleteToggle;
    private JToggleButton addToggle;

    /**
     * The colour for the ellipse when selecting points
     */
    private Color selectFill = new Color(0, 0, 255, 128);

    /**
     * The colour for the ellipse when deleting points
     */
    private Color deleteFill = new Color(255, 0, 0, 128);

    public CellBorderAdjustmentDialog(CellViewModel model) {
        super(model);
    }

    @Override
    public void load(final ICell cell, final IAnalysisDataset dataset) {
        super.load(cell, dataset);

        this.setTitle("Adjusting border in " + cell.getPrimaryNucleus().getNameAndNumber());
        updateCharts(cell);
        selectToggle.setSelected(true);
        ellipse.setFill(selectFill);

        // Clear the default listener and replace,
        // so we can listen for 'mustResegment'
        for (WindowListener l : this.getWindowListeners()) {
            this.removeWindowListener(l);
            ;
        }

        this.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {

                if (cellHasChanged()) {
                    requestSaveOption();
                }

                // If we have to resegment the cell manually, load the dialog
                if (mustResegment) {
                    CellResegmentationDialog c = new CellResegmentationDialog(cellModel);
                    c.load(cellModel.getCell(), dataset);

                }
                setVisible(false);
            }
        });

        setVisible(true);
    }

    @Override
    protected void createUI() {

        try {
            LOGGER.finer( "Creating border adjustment dialog");
            this.setLayout(new BorderLayout());

            JPanel header = createHeader();
            this.add(header, BorderLayout.NORTH);

            JFreeChart empty1 = ConsensusNucleusChartFactory.createEmptyChart();
            JFreeChart empty2 = ConsensusNucleusChartFactory.createEmptyChart();

            dualPanel = new CellBorderDualPanel();

            dualPanel.setCharts(empty1, empty2);

            ExportableChartPanel mainPanel = dualPanel.getMainPanel();
            mainPanel.setFixedAspectRatio(true);
            mainPanel.setPopupMenu(null);

            mainPanel.addBorderPointEventListener(this);
            mainPanel.addMouseMotionListener(this);
            mainPanel.addMouseListener(this);
            mainPanel.addMouseWheelListener(this);

            JPanel chartPanel = new JPanel();
            chartPanel.setLayout(new GridBagLayout());

            GridBagConstraints c = new GridBagConstraints();
            c.anchor = GridBagConstraints.EAST;
            c.gridx = 0;
            c.gridy = 0;
            c.gridwidth = 1;
            c.gridheight = 1;
            c.fill = GridBagConstraints.BOTH; // reset to default
            c.weightx = 0.6;
            c.weighty = 1.0;

            chartPanel.add(mainPanel, c);
            c.weightx = 0.4;
            c.gridx = 1;
            c.gridy = 0;

            ExportableChartPanel rangePanel = dualPanel.getRangePanel();
            rangePanel.setFixedAspectRatio(true);
            chartPanel.add(rangePanel, c);

            this.add(chartPanel, BorderLayout.CENTER);

            Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
            this.setPreferredSize(new Dimension((int) (screenSize.width * 0.9), (int) (screenSize.height * 0.6)));
            this.pack();

            // Create an ellipse overlay for the main panel with circular
            // appearance
            double aspect = mainPanel.getPlotAspectRatio();

            double h = 3;
            // double w = h / aspect;
            double w = 3;

            ellipse = new EllipticalOverlayObject(0, w, 0, h);
            mainPanel.addOverlay(new EllipticalOverlay(ellipse));

        } catch (Exception e) {
            LOGGER.log(Loggable.STACK, "Error making UI", e);
        }
    }

    private JPanel createHeader() {
        JPanel panel = new JPanel(new FlowLayout());

        selectToggle = new JToggleButton("Select");
        selectToggle.addActionListener(e -> {
            ellipse.setVisible(true);
            ellipse.setFill(selectFill);
        });

        deleteToggle = new JToggleButton("Delete");
        deleteToggle.addActionListener(e -> {
            ellipse.setVisible(true);
            ellipse.setFill(deleteFill);
        });

        addToggle = new JToggleButton("Add point");
        addToggle.addActionListener(e -> {
            ellipse.setVisible(false);
        });

        final ButtonGroup toggleGroup = new ButtonGroup();
        toggleGroup.add(selectToggle);
        toggleGroup.add(deleteToggle);
        toggleGroup.add(addToggle);

        //
        panel.add(selectToggle);
        panel.add(deleteToggle);
        panel.add(addToggle);

        selectToggle.setSelected(true);

        Component box = Box.createHorizontalStrut(10);
        panel.add(box);

        JButton undoBtn = new JButton("Undo");
        undoBtn.addActionListener(e -> {
            workingCell = new DefaultCell(cell);
            updateCharts(workingCell);
            setCellChanged(false);
            mustResegment = false;

        });
        panel.add(undoBtn);

        return panel;
    }

    @Override
    protected void updateCharts(ICell cell) {

        LOGGER.finer( "Making outline chart options");
        ChartOptions outlineOptions = new ChartOptionsBuilder()
        		.setDatasets(dataset).setCell(cell)
                .setRotationMode(RotationMode.ACTUAL)
                .setShowAnnotations(false)
                .setInvertYAxis(true)
                .setShowXAxis(false)
                .setShowYAxis(false)
                .setShowPoints(true)
                .addCellularComponent(cell.getPrimaryNucleus())
                .build();

        OutlineChartFactory ocf = new OutlineChartFactory(outlineOptions);

        JFreeChart outlineChart = ocf.makeCellOutlineChart();
        JFreeChart outlineChart2 = ocf.makeCellOutlineChart();

        dualPanel.setCharts(outlineChart, outlineChart2);

    }

    @Override
    public void borderPointEventReceived(BorderPointEvent event) {
        LOGGER.fine("Border point event received");

    }

    private void selectClickedPoint(@NonNull IPoint clickedPoint) {
        for (IPoint point : workingCell.getPrimaryNucleus().getBorderList()) {

            if (point.overlapsPerfectly(clickedPoint)) {

                if (!selectedPoints.containsKey(point)) {

                    double aspect = dualPanel.getMainPanel().getPanelAspectRatio();
                    double radius = 0.45;
                    double h = radius * 2;
                    // double w = h / aspect;
                    double w = h;
                    Ellipse2D r = new Ellipse2D.Double(point.getX() - (w / 2), point.getY() - (h / 2), w, h);
                    XYShapeAnnotation a = new XYShapeAnnotation(r, null, null, Color.BLUE);
                    selectedPoints.put(point, a);
                    dualPanel.getMainPanel().getChart().getXYPlot().addAnnotation(a);
                }
                break;
            }
        }
    }

    private void clearSelectedPoints() {

        for (XYShapeAnnotation a : selectedPoints.values()) {
            dualPanel.getMainPanel().getChart().getXYPlot().removeAnnotation(a);

        }
        selectedPoints = new HashMap<IPoint, XYShapeAnnotation>();
    }

    private void moveSelectedPoint() {
        // Move the selected point in the border list copy
        List<IPoint> borderList = workingCell.getPrimaryNucleus().getBorderList();
        for (int i = 0; i < borderList.size(); i++) {

            IPoint point = borderList.get(i);
            if (selectedPoints.containsKey(point)) {
                // workingCell.getNucleus().updateBorderPoint(i,
                // finalMovePointX, finalMovePointY);
                break;
            }
        }
        setCellChanged(true);
        updateWorkingCell(workingCell.getPrimaryNucleus().getBorderList());
    }

    // Add a point at the screen position in the main chart
    private void addPoint(Point pt) {

        // Convert the screen position to chart coordinates
        XYPlot xy = dualPanel.getMainPanel().getChart().getXYPlot();
        Rectangle2D dataArea = dualPanel.getMainPanel().getChartRenderingInfo().getPlotInfo().getDataArea();
        Point2D p = dualPanel.getMainPanel().translateScreenToJava2D(pt);

        double newY = xy.getRangeAxis().java2DToValue(p.getY(), dataArea, xy.getRangeAxisEdge());

        double newX = xy.getDomainAxis().java2DToValue(p.getX(), dataArea, xy.getDomainAxisEdge());

        LOGGER.log(Loggable.STACK, "Adding point at " + newX + ", " + newY);
        IPoint newPoint = IPoint.makeNew(newX, newY);

        // Get the border point that is closest to the clicked point
        IPoint bp = null;
        try {
            bp = workingCell.getPrimaryNucleus().findClosestBorderPoint(newPoint);
        } catch (UnavailableBorderPointException e) {
            LOGGER.log(Loggable.STACK, "Unable to get border point", e);

        }

        List<IPoint> newList = new ArrayList<>();

        // Insert the new point after the closest existing point to it
        List<IPoint> borderList = workingCell.getPrimaryNucleus().getBorderList();
        Iterator<IPoint> it = borderList.iterator();
        while (it.hasNext()) {
            IPoint point = it.next();
            newList.add(point);

            if (point.equals(bp)) {
                newList.add(newPoint.duplicate());
            }
        }
        setCellChanged(true);
        updateWorkingCell(newList);

    }

    private void deleteSelectedPoints() {
        // Remove the selected points from the border list copy
        List<IPoint> borderList = workingCell.getPrimaryNucleus().getBorderList();
        Iterator<IPoint> it = borderList.iterator();
        while (it.hasNext()) {
            IPoint point = it.next();
            if (selectedPoints.containsKey(point)) {
                it.remove();
            }
        }
        setCellChanged(true);
        updateWorkingCell(borderList);

    }

    private void updateWorkingCell(List<IPoint> borderList) {
        // Make a interpolated FloatPolygon from the new array
        float[] xPoints = new float[borderList.size()];
        float[] yPoints = new float[borderList.size()];
        int nPoints = borderList.size();
        int type = Roi.POLYGON;

        for (int i = 0; i < nPoints; i++) {
            xPoints[i] = (float) borderList.get(i).getX();
            yPoints[i] = (float) borderList.get(i).getY();
        }

        PolygonRoi roi = new PolygonRoi(xPoints, yPoints, nPoints, type);
        FloatPolygon fp = roi.getInterpolatedPolygon(1, false); // one pixel
                                                                // spacing, do
                                                                // not smooth

        // Make new border list and assign to the working cell

        List<IPoint> newList = new ArrayList<>();
        for (int i = 0; i < fp.npoints; i++) {
            newList.add(IPoint.makeNew(fp.xpoints[i], fp.ypoints[i]));
        }

        Range domainRange = dualPanel.getMainPanel().getChart().getXYPlot().getDomainAxis().getRange();
        Range rangeRange = dualPanel.getMainPanel().getChart().getXYPlot().getRangeAxis().getRange();
        updateCharts(workingCell);
        clearSelectedPoints();

        // Recalculate profiles for the cell, and update segments to best fit
        // positions
        updateWorkingCellProfiles();

        dualPanel.getMainPanel().getChart().getXYPlot().getDomainAxis().setRange(domainRange);
        dualPanel.getMainPanel().getChart().getXYPlot().getRangeAxis().setRange(rangeRange);

    }

    private void updateWorkingCellProfiles() {

        // Get the positions of segment boundaries
        ISegmentedProfile templateProfile;
        try {
            templateProfile = workingCell.getPrimaryNucleus().getProfile(ProfileType.ANGLE);
        } catch (MissingProfileException | ProfileException | MissingLandmarkException e1) {
            LOGGER.warning("Angle profile not present");
            return;
        }
        int oldLength = templateProfile.size();

        try {
            workingCell.getPrimaryNucleus().calculateProfiles();

            int newLength = workingCell.getPrimaryNucleus().getProfile(ProfileType.ANGLE).size();

            // Get the border tag positions, and set equivalent positions in the
            // new profile
            Map<Landmark, Integer> tagMap = workingCell.getPrimaryNucleus().getLandmarks();

            Map<Landmark, Integer> newMap = new HashMap<Landmark, Integer>();

            for (Landmark tag : tagMap.keySet()) {
                int oldIndex = tagMap.get(tag);

                double proportion = (double) oldIndex / (double) oldLength;

                int newIndex = (int) (proportion * (double) newLength);
                LOGGER.fine(tag.toString() + " From: " + oldIndex + " : To: " + newIndex);
                // workingCell.getNucleus().setBorderTag(tag, newIndex);
                newMap.put(tag, newIndex);
            }
//            workingCell.getNucleus().replaceBorderTags(newMap);

        } catch (Exception e) {
            LOGGER.warning("Cannot calculate profiles for cell");
            mustResegment = true;
        }

    }

    public void movePoint(MouseEvent me) {
        if (canMove) {
            int itemIndex = xyItemEntity.getItem();
            Point pt = me.getPoint();
            XYPlot xy = dualPanel.getMainPanel().getChart().getXYPlot();
            Rectangle2D dataArea = dualPanel.getMainPanel().getChartRenderingInfo().getPlotInfo().getDataArea();
            Point2D p = dualPanel.getMainPanel().translateScreenToJava2D(pt);
            finalMovePointY = xy.getRangeAxis().java2DToValue(p.getY(), dataArea, xy.getRangeAxisEdge());

            finalMovePointX = xy.getDomainAxis().java2DToValue(p.getX(), dataArea, xy.getDomainAxisEdge());
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        movePoint(e);

    }

    @Override
    public void mouseMoved(MouseEvent e) {

        int x = e.getX();
        int y = e.getY();

        // Update the overlay position on the main chart

        Rectangle2D dataArea = dualPanel.getMainPanel().getScreenDataArea();
        JFreeChart chart = dualPanel.getMainPanel().getChart();
        XYPlot plot = (XYPlot) chart.getPlot();
        ValueAxis xAxis = plot.getDomainAxis();
        ValueAxis yAxis = plot.getRangeAxis();

        double xValue = xAxis.java2DToValue(x, dataArea, RectangleEdge.BOTTOM);

        double yValue = yAxis.java2DToValue(y, dataArea, RectangleEdge.LEFT);

        ellipse.setXValue(xValue);
        ellipse.setYValue(yValue);

    }

    @Override
    public void mouseClicked(MouseEvent arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseEntered(MouseEvent arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseExited(MouseEvent arg0) {
        canMove = false;
        initialMovePointY = 0;
        initialMovePointX = 0;
    }

    @Override
    public void mousePressed(MouseEvent e) {

        // For both delete and select, we want to get the points under the
        // ellipse
        if (selectToggle.isSelected() || deleteToggle.isSelected()) {
            EntityCollection entities = dualPanel.getMainPanel().getChartRenderingInfo().getEntityCollection();

            if ((e.getModifiers() & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK) {
                // Find the entities under the ellipse overlay

                XYDataset ds = dualPanel.getMainPanel().getChart().getXYPlot().getDataset();
                for (Object entity : entities.getEntities()) {

                    if (entity instanceof XYItemEntity) {

                        xyItemEntity = (XYItemEntity) entity;

                        int series = xyItemEntity.getSeriesIndex();
                        int item = xyItemEntity.getItem();

                        double xVal = ds.getXValue(series, item);
                        double yVal = ds.getYValue(series, item);

                        if (ellipse.contains(xVal, yVal)) {
                            IPoint clickedPoint = IPoint.makeNew(xVal, yVal);
                            selectClickedPoint(clickedPoint);
                        }
                    }

                }
            }
        }

        // Detect right-click and clear
        if (selectToggle.isSelected()) {

            if ((e.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {
                clearSelectedPoints();
            }
        }

        if (deleteToggle.isSelected()) {
            if ((e.getModifiers() & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK) {
                deleteSelectedPoints();
            }

        }

        if (addToggle.isSelected()) {
            // log("Adding point");
            addPoint(e.getPoint());
        }

    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // stop dragging on mouse released

        if (canMove && (e.getModifiers() & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK) {
            canMove = false;

            if (finalMovePointX != initialMovePointX && finalMovePointY != initialMovePointY) {

                // Point was moved, get the item to change in the dataset
                LOGGER.log(Loggable.STACK, "Released at " + finalMovePointX + " , " + finalMovePointY);
                moveSelectedPoint();
            }

            initialMovePointY = 0;
            initialMovePointX = 0;
            dualPanel.getMainPanel().setCursor(Cursor.getDefaultCursor());
        }

    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        int rotation = e.getWheelRotation();

        // + = zoom in

        if (rotation > 0) {
            ellipse.setXRadius(ellipse.getXRadius() / 1.5);
            ellipse.setYRadius(ellipse.getYRadius() / 1.5);
        }
        if (rotation < 0) {
            ellipse.setXRadius(ellipse.getXRadius() * 1.5);
            ellipse.setYRadius(ellipse.getYRadius() * 1.5);
        }

    }

}
