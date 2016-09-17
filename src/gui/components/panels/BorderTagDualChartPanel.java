/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package gui.components.panels;

import gui.components.BorderTagEvent;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.entity.XYItemEntity;

import analysis.AnalysisDataset;
import components.Cell;
import components.generic.BorderTagObject;
import components.generic.ProfileType;

@SuppressWarnings("serial")
public class BorderTagDualChartPanel extends DualChartPanel{
	
	private int activeProfileIndex = 0;
	
	private JPopupMenu popupMenu = new JPopupMenu("Popup");
	
	public BorderTagDualChartPanel(){
		super();
		
		
		chartPanel.addChartMouseListener(new ChartMouseListener() {

		    public void chartMouseClicked(ChartMouseEvent e) {
		    	
		    	if(e.getEntity() instanceof XYItemEntity){
		    		XYItemEntity ent = (XYItemEntity) e.getEntity();
		    		int series = ent.getSeriesIndex();
		    		int item   = ent.getItem();
		    		double x   = ent.getDataset().getXValue(series, item);

		    		activeProfileIndex = (int) x;
		    		MouseEvent ev = e.getTrigger();
		    		popupMenu.show(ev.getComponent(), ev.getX(), ev.getY());
		    	}

		    }

		    public void chartMouseMoved(ChartMouseEvent e) {
		    }
		    	
		});
	}
	
	public void createBorderTagPopup(Cell cell){
		Set<BorderTagObject> set = cell.getNucleus().getBorderTags().keySet();
		List<BorderTagObject> list = new ArrayList<BorderTagObject>(set);
		makePopup(list);
		
	}
		
	public void createBorderTagPopup(AnalysisDataset dataset){

		List<BorderTagObject> list = dataset.getCollection().getProfileCollection(ProfileType.ANGLE).getBorderTags();
		makePopup(list);

	}
	
	private void makePopup(List<BorderTagObject> list){
		popupMenu = new JPopupMenu("Popup");
		
		Collections.sort(list);
		
		for(BorderTagObject tag : list){
			
			/*
			 * The IP is determined solely by the OP
			 */
			if( tag.equals(BorderTagObject.INTERSECTION_POINT)){
				continue;
			}
			
			JMenuItem item = new JMenuItem(tag.toString());
			
			item.addActionListener( e ->{
					fireBorderTagEvent(new BorderTagEvent(item, tag, activeProfileIndex));				
			});
			popupMenu.add(item);			
		}
		
		// Find border tags with rulesets that have not been assigned in the median
		List<BorderTagObject> unassignedTags = new ArrayList<BorderTagObject>();
		for(BorderTagObject tag : BorderTagObject.values()){
			if( tag.equals(BorderTagObject.INTERSECTION_POINT)){
				continue;
			}
			
			if( ! list.contains(tag)){
				unassignedTags.add(tag);
				
			}
		}
		
		if( ! unassignedTags.isEmpty()){
			Collections.sort(unassignedTags);
			
			popupMenu.addSeparator();

			for(BorderTagObject tag : unassignedTags){
				JMenuItem item = new JMenuItem(tag.toString());
				item.setForeground(Color.DARK_GRAY);
				
				item.addActionListener( e ->{
						fireBorderTagEvent(new BorderTagEvent(item, tag, activeProfileIndex));				
				});
				popupMenu.add(item);	
			}
			
			
		}
	}
	
}
