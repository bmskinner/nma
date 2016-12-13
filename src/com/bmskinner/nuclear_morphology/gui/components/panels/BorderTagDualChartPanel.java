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

package com.bmskinner.nuclear_morphology.gui.components.panels;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.entity.XYItemEntity;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.generic.BorderTagObject;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.gui.components.BorderTagEvent;

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
		
		chartPanel.setRangeZoomable(false);
		chartPanel.setDomainZoomable(false);
	}
	
	public void createBorderTagPopup(ICell cell){
		Set<Tag> set = cell.getNucleus().getBorderTags().keySet();
		List<Tag> list = new ArrayList<Tag>(set);
		makePopup(list);
		
	}
		
	public void createBorderTagPopup(IAnalysisDataset dataset){

		List<Tag> list = dataset.getCollection().getProfileCollection().getBorderTags();
		makePopup(list);

	}
	
	private void makePopup(List<Tag> list){
		popupMenu = new JPopupMenu("Popup");
		
		Collections.sort(list);
		
		for(Tag tag : list){
			
			/*
			 * The IP is determined solely by the OP
			 */
			if( tag.equals(Tag.INTERSECTION_POINT)){
				continue;
			}
			
			JMenuItem item = new JMenuItem(tag.toString());
			
			item.addActionListener( e ->{
					fireBorderTagEvent(new BorderTagEvent(item, tag, activeProfileIndex));				
			});
			popupMenu.add(item);			
		}
		
		// Find border tags with rulesets that have not been assigned in the median
		List<Tag> unassignedTags = new ArrayList<Tag>();
		for(Tag tag : BorderTagObject.values()){
			if( tag.equals(Tag.INTERSECTION_POINT)){
				continue;
			}
			
			if( ! list.contains(tag)){
				unassignedTags.add(tag);
				
			}
		}
		
		if( ! unassignedTags.isEmpty()){
			Collections.sort(unassignedTags);
			
			popupMenu.addSeparator();

			for(Tag tag : unassignedTags){
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
