///*******************************************************************************
// * Copyright (C) 2018 Ben Skinner
// * 
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// * 
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// * 
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// ******************************************************************************/
//package com.bmskinner.nuclear_morphology.gui.components.panels;
//
//import java.awt.Color;
//import java.awt.event.MouseEvent;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//
//import javax.swing.JMenuItem;
//import javax.swing.JPopupMenu;
//
//import org.eclipse.jdt.annotation.Nullable;
//import org.jfree.chart.ChartMouseEvent;
//import org.jfree.chart.ChartMouseListener;
//import org.jfree.chart.entity.XYItemEntity;
//
//import com.bmskinner.nuclear_morphology.components.cells.ICell;
//import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
//import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
//import com.bmskinner.nuclear_morphology.gui.events.LandmarkUpdateEvent;
//import com.bmskinner.nuclear_morphology.gui.events.revamp.UserActionController;
//
//public class BorderTagDualChartPanel extends DualChartPanel {
//
//	private int activeProfileIndex = 0;
//
//	private JPopupMenu popupMenu = new JPopupMenu("Popup");
//
//	public BorderTagDualChartPanel() {
//		super(false);
//
//		chartPanel.addChartMouseListener(new ChartMouseListener() {
//
//			@Override
//			public void chartMouseClicked(ChartMouseEvent e) {
//
//				if (e.getEntity() instanceof XYItemEntity) {
//					XYItemEntity ent = (XYItemEntity) e.getEntity();
//					int series = ent.getSeriesIndex();
//					int item = ent.getItem();
//					double x = ent.getDataset().getXValue(series, item);
//
//					activeProfileIndex = (int) x;
//					MouseEvent ev = e.getTrigger();
//					popupMenu.show(ev.getComponent(), ev.getX(), ev.getY());
//				}
//
//			}
//
//			@Override
//			public void chartMouseMoved(ChartMouseEvent e) {
//			}
//
//		});
//
//		chartPanel.setRangeZoomable(false);
//		chartPanel.setDomainZoomable(false);
//	}
//
//	public synchronized void createLandmarkPopup(@Nullable IAnalysisDataset dataset, @Nullable ICell c) {
//		if (dataset == null)
//			return;
//		List<Landmark> list = dataset.getCollection().getProfileCollection().getLandmarks();
//		popupMenu = new JPopupMenu("Popup");
//
//		Collections.sort(list);
//
//		for (Landmark tag : list) {
//
//			JMenuItem item = new JMenuItem(tag.toString());
//
//			item.addActionListener(e -> {
//				UserActionController.getInstance().landmarkUpdateEventReceived(
//						new LandmarkUpdateEvent(item, dataset, c, tag, activeProfileIndex));
//			});
//			popupMenu.add(item);
//		}
//
//		// Find border tags with rulesets that have not been assigned in the
//		// median
//		List<Landmark> unassignedTags = new ArrayList<>();
//		for (Landmark tag : Landmark.defaultValues()) {
//			if (!list.contains(tag)) {
//				unassignedTags.add(tag);
//
//			}
//		}
//
//		if (!unassignedTags.isEmpty()) {
//			Collections.sort(unassignedTags);
//
//			popupMenu.addSeparator();
//
//			for (Landmark tag : unassignedTags) {
//				JMenuItem item = new JMenuItem(tag.toString());
//				item.setForeground(Color.DARK_GRAY);
//
//				item.addActionListener(e -> {
//					UserActionController.getInstance().landmarkUpdateEventReceived(
//							new LandmarkUpdateEvent(item, dataset, c, tag, activeProfileIndex));
//				});
//				popupMenu.add(item);
//			}
//
//		}
//	}
//}
