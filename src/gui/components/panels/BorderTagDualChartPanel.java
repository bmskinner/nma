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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.entity.XYItemEntity;

import components.generic.BorderTagObject;

@SuppressWarnings("serial")
public class BorderTagDualChartPanel extends DualChartPanel{
	
	private int activeProfileIndex = 0;
	
	private JPopupMenu popupMenu = new JPopupMenu("Popup");
	
	public BorderTagDualChartPanel(){
		super();
		
		createBorderTagPopup();
		
		chartPanel.addChartMouseListener(new ChartMouseListener() {

		    public void chartMouseClicked(ChartMouseEvent e) {
		    	XYItemEntity ent = (XYItemEntity) e.getEntity();
		    	int series = ent.getSeriesIndex();
		    	int item   = ent.getItem();
		    	double x   = ent.getDataset().getXValue(series, item);
		    	
		    	activeProfileIndex = (int) x;
		    	MouseEvent ev = e.getTrigger();
		    	popupMenu.show(ev.getComponent(), ev.getX(), ev.getY());

		    }

		    public void chartMouseMoved(ChartMouseEvent e) {
		    }
		    	
		});
	}
		
	private void createBorderTagPopup(){

		for(BorderTagObject tag : BorderTagObject.values()){
			JMenuItem item = new JMenuItem(tag.toString());
			
			item.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {

					fireBorderTagEvent(new BorderTagEvent(item, tag, activeProfileIndex));

					
				}
			});
			popupMenu.add(item);

			/*
			 * The IP is determined solely by the OP
			 */
			if( tag.equals(BorderTagObject.INTERSECTION_POINT)){
				item.setEnabled(false);
			}
		}

	}
	
}
