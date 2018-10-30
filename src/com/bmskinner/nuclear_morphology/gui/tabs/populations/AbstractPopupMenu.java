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
package com.bmskinner.nuclear_morphology.gui.tabs.populations;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.IClusterGroup;
import com.bmskinner.nuclear_morphology.components.workspaces.IWorkspace;
import com.bmskinner.nuclear_morphology.gui.ContextEnabled;
import com.bmskinner.nuclear_morphology.gui.events.EventListener;
import com.bmskinner.nuclear_morphology.gui.events.SignalChangeEvent;

/**
 * The base class for popup menus
 * @author bms41
 * @since 1.14.0
 *
 */
public abstract class AbstractPopupMenu extends JPopupMenu {

    public static final String SOURCE_COMPONENT = "PopupMenu";
    private List<EventListener> listeners = new ArrayList<>();
    
    /**
     * Simplify creation of menu items
     * @author bms41
     * @since 1.14.0
     */
    protected class MenuFactory {
    	
    	/**
         * The class for submenus in the popup menu
         * @author bms41
         * @since 1.14.0
         *
         */
        public class PopupMenu extends JMenu implements ContextEnabled {
        	int context;
        	
        	/**
        	 * Create a new popup item
        	 * @param title the label for the menu
        	 * @param context the activity context under which the menu is enabled 
        	 */
        	public PopupMenu(String title, int context) {
        		super(title);
        		this.context = context;
        	}
        	
        	 /**
        	 * Update the item state based on the selected items
        	 * @param objects
        	 */
        	public boolean matchesSelectionContext(Collection<Object> objects) {

        		boolean enabled = true;
        		
        		if(objects.size()==0)
        			enabled=false;
        		if(objects.size()==1)
        			enabled &= ((context&ACTIVE_ON_SINGLE_OBJECT)==ACTIVE_ON_SINGLE_OBJECT);
        		if(objects.size()>1)
        			enabled &= ((context&ACTIVE_ON_MULTI_OBJECTS)==ACTIVE_ON_MULTI_OBJECTS);
        		
    		
        		for(Object o : objects) {
        			if(o instanceof IAnalysisDataset) {
        				IAnalysisDataset d = (IAnalysisDataset)o;
        				if(d.isRoot())
        					enabled &= ((context&ACTIVE_ON_ROOT_DATASET)==ACTIVE_ON_ROOT_DATASET);
        				else
        					enabled &= ((context&ACTIVE_ON_CHILD_DATASET)==ACTIVE_ON_CHILD_DATASET);
        			}
        			
        			// Allow cluster groups and workspaces to be ignored in multi selections
        			if(o instanceof IClusterGroup)
        				enabled |= ((context&ACTIVE_ON_CLUSTER_GROUP)==ACTIVE_ON_CLUSTER_GROUP);
        			if(o instanceof IWorkspace)
        				enabled |= ((context&ACTIVE_ON_WORKSPACE)==ACTIVE_ON_WORKSPACE);
        		}
        		return enabled;
        	} 

        	/**
        	 * Update the item state based on the selected items
        	 * @param objects
        	 */
        	@Override
			public void updateSelectionContext(Collection<Object> objects) {        		
        		setEnabled(matchesSelectionContext(objects));
        	}     	
        }
        
        
        /**
         * The class for items in the popup menu
         * @author bms41
         * @since 1.14.0
         *
         */
        public class PopupMenuItem extends JMenuItem implements ContextEnabled {
        	int context;
        	
        	
        	/**
        	 * Create a new popup item
        	 * @param title the label for the menu item
        	 * @param event the action to call
        	 * @param context the activity context under which the item is enabled 
        	 */
        	public PopupMenuItem(String title, String event, int context) {
        		super(title);
        		this.context = context;
        		addActionListener(e -> fireSignalChangeEvent(event));
        	}
        	
        	 /**
        	 * Update the item state based on the selected items
        	 * @param objects
        	 */
        	public boolean matchesSelectionContext(Collection<Object> objects) {

        		boolean enabled = true;
        		
        		if(objects.size()==0)
        			enabled=false;
        		if(objects.size()==1)
        			enabled &= ((context&ACTIVE_ON_SINGLE_OBJECT)==ACTIVE_ON_SINGLE_OBJECT);
        		if(objects.size()>1)
        			enabled &= ((context&ACTIVE_ON_MULTI_OBJECTS)==ACTIVE_ON_MULTI_OBJECTS);
        		
    		
        		for(Object o : objects) {
        			if(o instanceof IAnalysisDataset) {
        				IAnalysisDataset d = (IAnalysisDataset)o;
        				if(d.isRoot())
        					enabled &= ((context&ACTIVE_ON_ROOT_DATASET)==ACTIVE_ON_ROOT_DATASET);
        				else
        					enabled &= ((context&ACTIVE_ON_CHILD_DATASET)==ACTIVE_ON_CHILD_DATASET);
        			}
        			
        			// Allow cluster groups and workspaces to be ignored in multi selections
        			if(o instanceof IClusterGroup)
        				enabled |= ((context&ACTIVE_ON_CLUSTER_GROUP)==ACTIVE_ON_CLUSTER_GROUP);
        			if(o instanceof IWorkspace)
        				enabled |= ((context&ACTIVE_ON_WORKSPACE)==ACTIVE_ON_WORKSPACE);
        		}
        		
        		return enabled;
        	} 

        	/**
        	 * Update the item state based on the selected items
        	 * @param objects
        	 */
        	@Override
			public void updateSelectionContext(Collection<Object> objects) {        		
        		setEnabled(matchesSelectionContext(objects));
        	}      	

        }
        
       
        

    	public PopupMenuItem makeItem(String label, String event, int context) {
    		return new PopupMenuItem(label, event, context);
    	}
    	
    	public PopupMenu makeMenu(String label, int context) {
    		return new PopupMenu(label, context);
    	}
    }
    
    
    public AbstractPopupMenu() {
        super("Popup");
        createButtons();
        addButtons();
    }
    
    protected abstract void createButtons();
    
    protected abstract void addButtons();
    
    public abstract void updateSelectionContext(Collection<Object> objects);
        
    
    @Override
	public void setEnabled(boolean b) {
        for (Component c : this.getComponents()) {
            c.setEnabled(b);
        }
    }

    public synchronized void addSignalChangeListener(EventListener l) {
        listeners.add(l);
    }

    public synchronized void removeSignalChangeListener(EventListener l) {
        listeners.remove(l);
    }

    protected synchronized void fireSignalChangeEvent(String message) {
        SignalChangeEvent event = new SignalChangeEvent(this, message, SOURCE_COMPONENT);
        Iterator<EventListener> iterator = listeners.iterator();
        while (iterator.hasNext()) {
            iterator.next().eventReceived(event);
        }
    }

}
