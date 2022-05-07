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
package com.bmskinner.nma.gui.tabs.cells_detail;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.RectangleEdge;

import com.bmskinner.nma.components.MissingLandmarkException;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.generic.FloatPoint;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.profiles.IProfileSegment;
import com.bmskinner.nma.components.profiles.Landmark;
import com.bmskinner.nma.components.profiles.MissingProfileException;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.gui.RotationMode;
import com.bmskinner.nma.gui.components.ColourSelecter;
import com.bmskinner.nma.gui.components.panels.GenericCheckboxPanel;
import com.bmskinner.nma.gui.events.CellUpdatedEventListener;
import com.bmskinner.nma.gui.events.SegmentStartIndexUpdateEvent;
import com.bmskinner.nma.gui.events.SwatchUpdatedListener;
import com.bmskinner.nma.gui.events.UserActionController;
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.visualisation.ChartComponents;
import com.bmskinner.nma.visualisation.charts.AbstractChartFactory;
import com.bmskinner.nma.visualisation.charts.OutlineChartFactory;
import com.bmskinner.nma.visualisation.charts.overlays.EllipticalOverlay;
import com.bmskinner.nma.visualisation.charts.overlays.EllipticalOverlayObject;
import com.bmskinner.nma.visualisation.charts.overlays.ShapeOverlay;
import com.bmskinner.nma.visualisation.charts.overlays.ShapeOverlayObject;
import com.bmskinner.nma.visualisation.charts.panels.ExportableChartPanel;
import com.bmskinner.nma.visualisation.options.ChartOptions;
import com.bmskinner.nma.visualisation.options.ChartOptionsBuilder;

/**
 * Display panel for cell outlines, including segments and landmarks
 * 
 * @author ben
 *
 */
@SuppressWarnings("serial")
public class CellOutlinePanel extends AbstractCellDetailPanel
		implements ActionListener, CellUpdatedEventListener, SwatchUpdatedListener,
		ChartMouseListener {

	private static final Logger LOGGER = Logger.getLogger(CellOutlinePanel.class.getName());

	private static final String PANEL_TITLE_LBL = "Outline";

	private ExportableChartPanel chartPanel;

	private GenericCheckboxPanel rotatePanel = new GenericCheckboxPanel("Orient");
	private GenericCheckboxPanel warpMeshPanel = new GenericCheckboxPanel(
			"Warp image to consensus shape");

	private ShapeOverlay lmOverlay = new ShapeOverlay();

	private EllipticalOverlay bOverlay = new EllipticalOverlay(new EllipticalOverlayObject(
			Double.NaN, 2, Double.NaN, 2,
			ChartComponents.MARKER_STROKE, Color.decode("#0066CC"), Color.decode("#0066CC")));

	public CellOutlinePanel(CellViewModel model) {
		super(model, PANEL_TITLE_LBL);
		// make the chart for each nucleus
		this.setLayout(new BorderLayout());

		JPanel header = makeHeader();
		add(header, BorderLayout.NORTH);

		chartPanel = new ExportableChartPanel(AbstractChartFactory.createEmptyChart());
		chartPanel.setFixedAspectRatio(true);
		chartPanel.addOverlay(lmOverlay);
		chartPanel.addOverlay(bOverlay);
		chartPanel.addChartMouseListener(this);
		add(chartPanel, BorderLayout.CENTER);

		uiController.addCellUpdatedEventListener(this);

	}

	private JPanel makeHeader() {
		JPanel panel = new JPanel(new FlowLayout());

		JLabel headerLabel = new JLabel(
				"<html><body style='width: 90%'>"
						+ "Click a border point to update segments or landmarks.</html>");
		panel.add(headerLabel);

		rotatePanel.setEnabled(false);
		rotatePanel.addActionListener(this);

		warpMeshPanel.addActionListener(this);
		warpMeshPanel.setEnabled(false);

		panel.add(rotatePanel);
		panel.add(warpMeshPanel);

		return panel;
	}

	@Override
	public void cellUpdatedEventReceived(CellUpdatedEvent event) {
		update();
		setAnalysing(false);
	}

	private synchronized void updateSettingsPanels() {

		if (this.isMultipleDatasets() || !this.hasDatasets()) {
			rotatePanel.setEnabled(false);
			warpMeshPanel.setEnabled(false);
			return;
		}

		if (!this.getCellModel().hasCell()) {
			rotatePanel.setEnabled(false);
			warpMeshPanel.setEnabled(false);
		} else {
			// Only allow one mesh activity to be active
			rotatePanel.setEnabled(!warpMeshPanel.isSelected());
			warpMeshPanel.setEnabled(!rotatePanel.isSelected());

			if (!activeDataset().getCollection().hasConsensus()) {
				warpMeshPanel.setEnabled(false);
			}
		}
	}

	@Override
	public synchronized void update() {

		if (this.isMultipleDatasets() || !this.hasDatasets()) {
			chartPanel.setChart(AbstractChartFactory.createEmptyChart());
			return;
		}

		final ICell cell = getCellModel().getCell();
		final CellularComponent component = getCellModel().getComponent();

		RotationMode rm = rotatePanel.isSelected() ? RotationMode.VERTICAL : RotationMode.ACTUAL;

		ChartOptions options = new ChartOptionsBuilder()
				.setCell(cell)
				.setDatasets(activeDataset())
				.addCellularComponent(component).setRotationMode(rm)
				.setShowWarp(warpMeshPanel.isSelected())
				.setShowXAxis(false)
				.setShowYAxis(false)
				.build();

		chartPanel.setChart(new OutlineChartFactory(options).makeCellOutlineChart());

		updateSettingsPanels();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		update();
	}

	@Override
	protected void updateSingle() {
		update();
	}

	@Override
	protected void updateMultiple() {
		updateNull();
	}

	@Override
	protected void updateNull() {
		chartPanel.setChart(AbstractChartFactory.createEmptyChart());
		updateSettingsPanels();
	}

	@Override
	public void refreshCache() {
		clearCache();
		this.update();
	}

	@Override
	public void swatchUpdated() {
		update(getDatasets());
	}

	@Override
	public void chartMouseClicked(ChartMouseEvent event) {

		// Get the mouse location on the chart
		Rectangle2D dataArea = this.chartPanel.getScreenDataArea();
		JFreeChart chart = event.getChart();
		XYPlot plot = (XYPlot) chart.getPlot();
		ValueAxis xAxis = plot.getDomainAxis();
		ValueAxis yAxis = plot.getRangeAxis();
		double x = xAxis.java2DToValue(event.getTrigger().getX(), dataArea, RectangleEdge.BOTTOM);
		double y = yAxis.java2DToValue(event.getTrigger().getY(), dataArea, RectangleEdge.LEFT);

		double range = Math.min(xAxis.getRange().getLength(), yAxis.getRange().getLength());
		try {
			Nucleus n = rotatePanel.isSelected()
					? getCellModel().getCell().getPrimaryNucleus().getOrientedNucleus()
					: getCellModel().getCell().getPrimaryNucleus();

			// Get the closest border point, and set the overlay if close enough

			IPoint clicked = new FloatPoint(x, y);

			Optional<IPoint> bp = n.getBorderList().stream()
					.filter(p -> p.getLengthTo(clicked) < range / 50)
					.min((p1, p2) -> p1.getLengthTo(clicked) < p2.getLengthTo(clicked) ? -1 : 1);

			if (bp.isPresent()) {

				// Make the popup to change segments or landmarks
				JPopupMenu popup = createPopup(bp.get());
				popup.show(chartPanel, event.getTrigger().getX(), event.getTrigger().getY());
			}

		} catch (Exception e) {
			LOGGER.fine("Unable to create popup menu: " + e.getMessage());
		}
	}

	private synchronized JPopupMenu createPopup(IPoint point) {
		JPopupMenu popupMenu = new JPopupMenu("Popup");

		addSegmentsToPopup(popupMenu, point);

		popupMenu.addSeparator();

		addLandmarksToPopup(popupMenu, point);

		return popupMenu;
	}

	/**
	 * Add segment update options to the popup menu, coloured by segment
	 * 
	 * @param popupMenu
	 * @param point
	 */
	private void addSegmentsToPopup(JPopupMenu popupMenu, IPoint point) {
		try {
			Nucleus n = rotatePanel.isSelected()
					? getCellModel().getCell().getPrimaryNucleus().getOrientedNucleus()
					: getCellModel().getCell().getPrimaryNucleus();

			// Indexes in the nucleus.
			int rawIndex = n.getBorderIndex(point);
			int rpIndex = n.getBorderIndex(OrientationMark.REFERENCE);

			// Get the index of the clicked point in the RP-indexed profile
			int index = n.wrapIndex(rawIndex - rpIndex);

			// Get the relevant segments
			IProfileSegment seg = n.getProfile(ProfileType.ANGLE).getSegmentContaining(index);
//			LOGGER.fine("Index " + index + " Clicked in segment " + seg);
			IProfileSegment prev = seg.prevSegment();
			IProfileSegment next = seg.nextSegment();

			JMenuItem prevItem = new JMenuItem("Extend " + prev.getName() + " to here");
			prevItem.setBorder(
					BorderFactory.createLineBorder(ColourSelecter.getColor(prev.getPosition()), 3));
			prevItem.setBorderPainted(true);

			prevItem.addActionListener(e -> {
				setAnalysing(true);
				UserActionController.getInstance().segmentStartIndexUpdateEventReceived(
						new SegmentStartIndexUpdateEvent(this,
								activeDataset(),
								getCellModel().getCell(),
								seg.getID(),
								index - 1)); // Subtract 1 to adjust for drawing overlaps
			});
			popupMenu.add(prevItem);

			popupMenu.add(Box.createVerticalStrut(2)); // stop borders touching

			JMenuItem nextItem = new JMenuItem("Extend " + next.getName() + " to here");
			nextItem.setBorder(
					BorderFactory.createLineBorder(ColourSelecter.getColor(next.getPosition()), 2));
			nextItem.setBorderPainted(true);

			nextItem.addActionListener(e -> {
				setAnalysing(true);
				UserActionController.getInstance().segmentStartIndexUpdateEventReceived(
						new SegmentStartIndexUpdateEvent(this,
								activeDataset(),
								getCellModel().getCell(),
								next.getID(),
								index - 1));

			});
			popupMenu.add(nextItem);
		} catch (MissingProfileException | MissingLandmarkException | ProfileException
				| ComponentCreationException e) {
			LOGGER.log(Loggable.STACK, "Cannot create segment popup", e);
		}
	}

	/**
	 * Add tags to the popup menu
	 * 
	 * @param popupMenu
	 */
	private void addLandmarksToPopup(JPopupMenu popupMenu, IPoint point) {
		try {
			Nucleus n = rotatePanel.isSelected()
					? getCellModel().getCell().getPrimaryNucleus().getOrientedNucleus()
					: getCellModel().getCell().getPrimaryNucleus();

			// Indexes in the consensus
			int rawIndex = n.getBorderIndex(point);

			List<Landmark> tags = activeDataset().getCollection().getProfileCollection()
					.getLandmarks();

			Collections.sort(tags);

			for (Landmark lm : tags) {

				// Colour the menu item by tag colour
				JMenuItem item = new JMenuItem("Move " + lm.toString().toLowerCase() + " here");
//				item.setBorder(BorderFactory.createLineBorder(ColourSelecter.getColour(tag), 2));
				item.setBackground(Color.DARK_GRAY);
				item.setBorderPainted(true);
				item.setForeground(Color.WHITE);
				item.setOpaque(true);

				item.addActionListener(a -> {
					try {
						getCellModel().getCell().getPrimaryNucleus().setLandmark(lm, rawIndex);
						update();
					} catch (IndexOutOfBoundsException | MissingProfileException
							| MissingLandmarkException | ProfileException e) {
						LOGGER.log(Loggable.STACK, "Cannot update landmark", e);
					}
				});
				popupMenu.add(item);
				popupMenu.add(Box.createVerticalStrut(2)); // stop borders touching
			}
		} catch (MissingLandmarkException | ComponentCreationException e) {
			LOGGER.log(Loggable.STACK, "Cannot create landmark popup", e);
		}
	}

	@Override
	public void chartMouseMoved(ChartMouseEvent event) {

		if (this.isMultipleDatasets() || !this.hasDatasets())
			return;

		setBorderPointHighlight(event);
		setLandmarkHighlight(event);

	}

	private void setBorderPointHighlight(ChartMouseEvent event) {

		// Get the mouse location on the chart
		Rectangle2D dataArea = this.chartPanel.getScreenDataArea();
		JFreeChart chart = event.getChart();
		XYPlot plot = (XYPlot) chart.getPlot();
		ValueAxis xAxis = plot.getDomainAxis();
		ValueAxis yAxis = plot.getRangeAxis();
		double x = xAxis.java2DToValue(event.getTrigger().getX(), dataArea, RectangleEdge.BOTTOM);
		double y = yAxis.java2DToValue(event.getTrigger().getY(), dataArea, RectangleEdge.LEFT);

		double range = Math.min(xAxis.getRange().getLength(), yAxis.getRange().getLength());
		double size = range / 100;
		try {

			// Get the closest border point, and set the overlay if close enough
			Nucleus n = rotatePanel.isSelected()
					? getCellModel().getCell().getPrimaryNucleus().getOrientedNucleus()
					: getCellModel().getCell().getPrimaryNucleus();

			IPoint clicked = new FloatPoint(x, y);

			Optional<IPoint> bp = n.getBorderList().stream()
					.filter(p -> p.getLengthTo(clicked) < range / 50)
					.min((p1, p2) -> p1.getLengthTo(clicked) < p2.getLengthTo(clicked) ? -1 : 1);

			if (bp.isPresent()) {
				bOverlay.getEllipse().setXValue(bp.get().getX());
				bOverlay.getEllipse().setYValue(bp.get().getY());

			} else {
				bOverlay.getEllipse().setXValue(Double.NaN);
				bOverlay.getEllipse().setYValue(Double.NaN);
				bOverlay.getEllipse().setXRadius(size);
				bOverlay.getEllipse().setYRadius(size);
			}

		} catch (Exception e) {
			LOGGER.fine("Unable to draw border highlights: " + e.getMessage());
		}
	}

	private void setLandmarkHighlight(ChartMouseEvent event) {

		// Get the mouse location on the chart
		Rectangle2D dataArea = this.chartPanel.getScreenDataArea();
		JFreeChart chart = event.getChart();
		XYPlot plot = (XYPlot) chart.getPlot();
		ValueAxis xAxis = plot.getDomainAxis();
		ValueAxis yAxis = plot.getRangeAxis();
		double x = xAxis.java2DToValue(event.getTrigger().getX(), dataArea, RectangleEdge.BOTTOM);
		double y = yAxis.java2DToValue(event.getTrigger().getY(), dataArea, RectangleEdge.LEFT);

		double range = Math.min(xAxis.getRange().getLength(), yAxis.getRange().getLength());
		int textSize = (int) (range / 30);
		double distanceLimit = range / 20;
		try {

			// Get the closest landmarks, and set the overlay if close enough
			Nucleus n = rotatePanel.isSelected()
					? getCellModel().getCell().getPrimaryNucleus().getOrientedNucleus()
					: getCellModel().getCell().getPrimaryNucleus();

			IPoint clicked = new FloatPoint(x, y);
			lmOverlay.clearShapes();
			for (OrientationMark lm : n.getOrientationMarks()) {
				IPoint lmPoint = n.getBorderPoint(lm);
				Landmark l = n.getLandmark(lm);

				if (clicked.getLengthTo(lmPoint) < distanceLimit) {
					changeLandmarkOverlay(l.toString(), lmPoint.getX(), lmPoint.getY(), textSize);
				}
			}
		} catch (Exception e) {
			LOGGER.fine("Unable to draw landmark highlights: " + e.getMessage());
		}
	}

	private void changeLandmarkOverlay(String text, double x, double y, int size) {

		lmOverlay.clearShapes();
		lmOverlay.addShape(new ShapeOverlayObject(ShapeOverlayObject.createDiamond(1, x, y),
				ChartComponents.MARKER_STROKE, Color.DARK_GRAY, Color.DARK_GRAY));

		Graphics2D g = (Graphics2D) chartPanel.getGraphics();
		Font font = new Font(Font.SANS_SERIF, Font.PLAIN, size);
		FontRenderContext frc = g.getFontRenderContext();
		TextLayout layout = new TextLayout(text, font, frc);

		AffineTransform txt = new AffineTransform();

		txt.concatenate(
				AffineTransform.getTranslateInstance(x + layout.getBounds().getWidth() / 1.5, y));
		txt.concatenate(AffineTransform.getScaleInstance(1, -1));
		txt.concatenate(AffineTransform.getTranslateInstance(-layout.getBounds().getCenterX(),
				-layout.getBounds().getCenterY()));

		lmOverlay.addShape(
				new ShapeOverlayObject(layout.getOutline(txt), new BasicStroke(0), Color.BLACK,
						Color.BLUE));

	}
}
