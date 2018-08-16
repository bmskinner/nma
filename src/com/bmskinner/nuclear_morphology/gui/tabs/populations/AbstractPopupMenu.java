package com.bmskinner.nuclear_morphology.gui.tabs.populations;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.IClusterGroup;
import com.bmskinner.nuclear_morphology.components.workspaces.IWorkspace;
import com.bmskinner.nuclear_morphology.components.workspaces.IWorkspace.BioSample;
import com.bmskinner.nuclear_morphology.core.DatasetListManager;
import com.bmskinner.nuclear_morphology.gui.ContextEnabled;
import com.bmskinner.nuclear_morphology.gui.ContextEnabled.ActiveCountContext;
import com.bmskinner.nuclear_morphology.gui.EventListener;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.SignalChangeEvent;
import com.bmskinner.nuclear_morphology.gui.tabs.populations.AbstractPopupMenu.MenuFactory.PopupMenu;
import com.bmskinner.nuclear_morphology.gui.tabs.populations.AbstractPopupMenu.MenuFactory.PopupMenuItem;

/**
 * The basic popup menu for the popuations panel
 * @author bms41
 * @since 1.14.0
 *
 */
public abstract class AbstractPopupMenu extends JPopupMenu {

    public static final String SOURCE_COMPONENT = "PopupMenu";
    
    private PopupMenuItem moveUpMenuItem;
    private PopupMenuItem moveDownMenuItem;

    private PopupMenuItem changeScaleItem;
    private PopupMenuItem mergeMenuItem;
    private PopupMenuItem curateMenuItem;
    private PopupMenuItem deleteMenuItem;
    private PopupMenuItem booleanMenuItem;
    private PopupMenuItem extractMenuItem;
    
    private PopupMenu workspaceSubMenu;
    private PopupMenu biosampleSubMenu;

    
    private PopupMenu exportSubMenu;
    private PopupMenuItem exportStatsMenuItem;
    private PopupMenuItem exportSignalsItem;
    private PopupMenuItem exportShellsItem;
    private PopupMenuItem saveCellsMenuItem;
    
    private PopupMenuItem saveMenuItem;
    private PopupMenuItem relocateMenuItem;

    private PopupMenuItem replaceFolderMenuItem;
   
    private PopupMenu addSubMenu;
    private PopupMenuItem addNuclearSignalMenuItem;
    private PopupMenuItem fishRemappinglMenuItem;

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
        	ActiveCountContext context;
        	boolean rootOnly;
        	
        	
        	/**
        	 * Create a new popup item
        	 * @param title the label for the menu
        	 * @param context the activity context under which the menu is enabled 
        	 */
        	public PopupMenu(String title, ActiveCountContext context) {
        		super(title);
        		this.context = context;
        	}
        	        	
        	/**
        	 * Update the item state based on the total number of selected items
        	 * @param nObjects
        	 */
        	public void updateSelectionContext(int nObjects) {
        		if(nObjects<=0)
        			setEnabled(false);
        		if(nObjects==1)
        			setEnabled(context.equals(ActiveCountContext.SINGLE_AND_MULTIPLE_OBJECTS) || context.equals(ActiveCountContext.SINGLE_OBJECT_ONLY));
        		if(nObjects>1)
        			setEnabled(context.equals(ActiveCountContext.SINGLE_AND_MULTIPLE_OBJECTS) || context.equals(ActiveCountContext.MULTIPLE_OBJECTS_ONLY));
        	}

			@Override
			public void updateSelectionContext(ActiveTypeContext type) {
				// TODO Auto-generated method stub
				
			}        	
        }
        
        
        /**
         * The class for items in the popup menu
         * @author bms41
         * @since 1.14.0
         *
         */
        public class PopupMenuItem extends JMenuItem implements ContextEnabled {
        	ActiveCountContext context;
        	
        	
        	/**
        	 * Create a new popup item
        	 * @param title the label for the menu item
        	 * @param event the action to call
        	 * @param context the activity context under which the item is enabled 
        	 */
        	public PopupMenuItem(String title, String event, ActiveCountContext context) {
        		super(title);
        		this.context = context;
        		addActionListener(e -> fireSignalChangeEvent(event));
        	}
        	
        	/**
        	 * Update the item state based on the total number of selected items
        	 * @param nObjects
        	 */
        	@Override
        	public void updateSelectionContext(int nObjects) { 
        		if(nObjects<=0)
        			setEnabled(false);
        		if(nObjects==1)
        			setEnabled(context.equals(ActiveCountContext.SINGLE_AND_MULTIPLE_OBJECTS) || context.equals(ActiveCountContext.SINGLE_OBJECT_ONLY));
        		if(nObjects>1)
        			setEnabled(context.equals(ActiveCountContext.SINGLE_AND_MULTIPLE_OBJECTS) || context.equals(ActiveCountContext.MULTIPLE_OBJECTS_ONLY));
        	}

			@Override
			public void updateSelectionContext(ActiveTypeContext type) {
				// TODO Auto-generated method stub
				
			}
        }
        

    	public PopupMenuItem makeItem(String label, String event, ActiveCountContext context) {
    		return new PopupMenuItem(label, event, context);
    	}

    	/**
    	 * Create a menu item that is active only when a single object is selected
    	 * @param label the label for the menu item
    	 * @param event the event to be triggered when the menu item is selected
    	 * @return
    	 */
    	public PopupMenuItem makeSingleMenuItem(String label, String event) {
    		return makeItem(label, event, ActiveCountContext.SINGLE_OBJECT_ONLY);
    	}
    	
    	/**
    	 * Create a menu item that is active only when multiple objects are selected
    	 * @param label the label for the menu item
    	 * @param event the event to be triggered when the menu item is selected
    	 * @return
    	 */
    	public PopupMenuItem makeMultipleMenuItem(String label, String event) {
    		return makeItem(label, event, ActiveCountContext.MULTIPLE_OBJECTS_ONLY);
    	}
    	
    	/**
    	 * Create a menu item that is active when single or multiple objects are selected
    	 * @param label the label for the menu item
    	 * @param event the event to be triggered when the menu item is selected
    	 * @return
    	 */
    	public PopupMenuItem makeBothMenuItem(String label, String event) {
    		return makeItem(label, event, ActiveCountContext.SINGLE_AND_MULTIPLE_OBJECTS);
    	}
    	
    	public PopupMenu makeMenu(String label, ActiveCountContext context) {
    		return new PopupMenu(label, context);
    	}
    	
    	/**
    	 * Create a menu that is active only when a single object is selected
    	 * @param label the label for the menu item
    	 * @param event the event to be triggered when the menu item is selected
    	 * @return
    	 */
    	public PopupMenu makeSingleMenu(String label) {
    		return new PopupMenu(label, ActiveCountContext.SINGLE_OBJECT_ONLY);
    	}
    	
    	/**
    	 * Create a menu that is active only when multiple objects are selected
    	 * @param label the label for the menu item
    	 * @param event the event to be triggered when the menu item is selected
    	 * @return
    	 */
    	public PopupMenu makeMultipleMenu(String label) {
    		return new PopupMenu(label, ActiveCountContext.SINGLE_OBJECT_ONLY);
    	}
    	
    	/**
    	 * Create a menu that is active when single or multiple objects are selected
    	 * @param label the label for the menu item
    	 * @param event the event to be triggered when the menu item is selected
    	 * @return
    	 */
    	public PopupMenu makeBothMenu(String label) {
    		return new PopupMenu(label, ActiveCountContext.SINGLE_AND_MULTIPLE_OBJECTS);
    	}
    }
    
    
    public AbstractPopupMenu() {

        super("Popup");
        createButtons();
        addButtons();
    }
    
    protected void addMoveMenuItems() {
    	this.add(moveUpMenuItem);
        this.add(moveDownMenuItem);
    }
    
    protected void addButtons() {

    	addMoveMenuItems();

    	addSeparator();
    	
    	add(workspaceSubMenu);
    	add(biosampleSubMenu);
    	
    	addSeparator();

    	add(mergeMenuItem);
    	add(deleteMenuItem);
    	add(booleanMenuItem);
    	add(curateMenuItem);

    	addSeparator();

    	add(saveMenuItem);

    	addSeparator();

    	add(relocateMenuItem);

    	addSeparator();

    	add(replaceFolderMenuItem);
    	add(exportSubMenu);
    	add(changeScaleItem);

    	addSeparator();

    	add(addSubMenu);
    }

    private void createWorkspaceMenu(@Nullable IAnalysisDataset d) {
    	if(d==null)
    		return;
    	MenuFactory fact = new MenuFactory();
//        newWorkspaceMenuItem = fact.makeSingleMenuItem(Labels.Populations.NEW_WORKSPACE, SignalChangeEvent.NEW_WORKSPACE);
        
//        addWorkspaceSubMenu.add(newWorkspaceMenuItem);
//        addWorkspaceSubMenu.addSeparator();
//        JMenuItem wsItem = new JMenuItem("Workspaces");
//    	wsItem.setEnabled(false);
//    	addWorkspaceSubMenu.add(wsItem);
        
    	List<IWorkspace> workspaces = DatasetListManager.getInstance().getWorkspaces();
    	for(IWorkspace w : workspaces) {
    		String name   = w.has(d) ? Labels.Populations.REMOVE_FROM_LBL_PREFIX : Labels.Populations.ADD_TO_LBL_PREFIX;
    		String action = w.has(d) ? SignalChangeEvent.REMOVE_FROM_WORKSPACE_PREFIX : SignalChangeEvent.ADD_TO_WORKSPACE_PREFIX;
    		workspaceSubMenu.add(fact.makeSingleMenuItem(name+w.getName(), action+w.getName()));
    	}    	
    }
    
    private void createBiosampleMenu(@Nullable IAnalysisDataset d) {
    	if(d==null || !DatasetListManager.getInstance().isInWorkspace(d)) {
    		biosampleSubMenu.setEnabled(false);
    		return;
    	}
    	
    	List<IWorkspace> workspaces = DatasetListManager.getInstance().getWorkspaces(d);
    	    	
    	MenuFactory fact = new MenuFactory();

    	biosampleSubMenu.add(fact.makeSingleMenuItem(Labels.Populations.ADD_TO_NEW_LBL, SignalChangeEvent.NEW_BIOSAMPLE_PREFIX));
    	biosampleSubMenu.addSeparator();

    	for(IWorkspace w : workspaces) {
    		for(BioSample bs : w.getBioSamples()) {
    			String name = bs.hasDataset(d.getSavePath()) ? Labels.Populations.REMOVE_FROM_LBL_PREFIX : Labels.Populations.ADD_TO_LBL_PREFIX;
    			String action = bs.hasDataset(d.getSavePath()) ? SignalChangeEvent.REMOVE_FROM_BIOSAMPLE_PREFIX : SignalChangeEvent.ADD_TO_BIOSAMPLE_PREFIX;
    			biosampleSubMenu.add(fact.makeSingleMenuItem(name+bs.getName(), action+bs.getName()));
    		}
    	}  
    }

    public void createButtons() {
    	
    	MenuFactory fact = new MenuFactory();
    	saveMenuItem = fact.makeSingleMenuItem(Labels.Populations.SAVE_AS_LBL, SignalChangeEvent.SAVE_SELECTED_DATASET);
        
    	moveUpMenuItem   = fact.makeSingleMenuItem(Labels.Populations.MOVE_UP_LBL,   SignalChangeEvent.MOVE_DATASET_UP_ACTION);
    	moveDownMenuItem = fact.makeSingleMenuItem(Labels.Populations.MOVE_DOWN_LBL, SignalChangeEvent.MOVE_DATASET_DOWN_ACTION);

        workspaceSubMenu =  fact.makeSingleMenu(Labels.Populations.ADD_TO_WORKSPACE_LBL);
        createWorkspaceMenu(null);
        biosampleSubMenu =  fact.makeSingleMenu(Labels.Populations.ADD_TO_BIOSAMPLE_LBL);
        createBiosampleMenu(null);
        
        mergeMenuItem   = fact.makeMultipleMenuItem(Labels.Populations.MERGE_LBL, SignalChangeEvent.MERGE_DATASETS_ACTION);
        curateMenuItem  = fact.makeSingleMenuItem(Labels.Populations.CURATE_LBL, SignalChangeEvent.CURATE_DATASET);
        
        deleteMenuItem = fact.makeBothMenuItem(Labels.Populations.DELETE_LBL, SignalChangeEvent.DELETE_DATASET);
        
        booleanMenuItem = fact.makeBothMenuItem(Labels.Populations.ARITHMETIC_LBL, SignalChangeEvent.DATASET_ARITHMETIC);

        extractMenuItem  = fact.makeSingleMenuItem(Labels.Populations.EXTRACT_CELLS_LBL, SignalChangeEvent.EXTRACT_SUBSET);
        
        changeScaleItem = fact.makeBothMenuItem(Labels.Populations.CHANGE_SCALE_LBL, SignalChangeEvent.CHANGE_SCALE);
        relocateMenuItem = fact.makeSingleMenuItem(Labels.Populations.RELOCATE_CELLS_LBL, SignalChangeEvent.RELOCATE_CELLS);
        
        exportSubMenu = fact.makeBothMenu(Labels.Populations.EXPORT);

        exportStatsMenuItem = fact.makeBothMenuItem(Labels.Populations.EXPORT_STATS, SignalChangeEvent.EXPORT_STATS);
        exportSignalsItem   = fact.makeBothMenuItem(Labels.Populations.EXPORT_SIGNALS, SignalChangeEvent.EXPORT_SIGNALS);
        exportShellsItem    = fact.makeBothMenuItem(Labels.Populations.EXPORT_SHELLS, SignalChangeEvent.EXPORT_SHELLS);
        saveCellsMenuItem   = fact.makeBothMenuItem(Labels.Populations.EXPORT_CELL_LOCS, SignalChangeEvent.EXPORT_CELL_LOCS);

        exportSubMenu.add(exportStatsMenuItem);
        exportSubMenu.add(exportSignalsItem);
        exportSubMenu.add(exportShellsItem);
        exportSubMenu.add(saveCellsMenuItem);
        
        addNuclearSignalMenuItem = fact.makeSingleMenuItem(Labels.Populations.ADD_NUCLEAR_SIGNAL_LBL, SignalChangeEvent.ADD_NUCLEAR_SIGNAL);
        addNuclearSignalMenuItem.setToolTipText(Labels.Populations.ADD_NUCLEAR_SIGNAL_TIP);
        fishRemappinglMenuItem = fact.makeSingleMenuItem(Labels.Populations.POST_FISH_MAPPING_LBL, SignalChangeEvent.POST_FISH_MAPPING);
        
        addSubMenu = fact.makeSingleMenu(Labels.Populations.ADD);
        addSubMenu.add(addNuclearSignalMenuItem);
        addSubMenu.add(fishRemappinglMenuItem);
        
        replaceFolderMenuItem = fact.makeSingleMenuItem(Labels.Populations.CHANGE_FOLDER_LBL, SignalChangeEvent.CHANGE_NUCLEUS_IMAGE_FOLDER);

    }
    
    public void setEnabled(boolean b) {
        for (Component c : this.getComponents()) {
            c.setEnabled(b);
        }
    }
    
    /**
     * Tell the menu items to update their state based on the number of selected items
     * @param nItems the number of selected items
     */
    public void updateSelectionContext(int nItems) {
    	for (Component c : this.getComponents()) {
    		if(c instanceof ContextEnabled)
    			((ContextEnabled) c).updateSelectionContext(nItems);
        }
    }
    
    /**
     * Tell the menu items to update their state based on the selected item class,
     * if recognised
     * @param o the selected object
     */
    public void updateSelectionContext(Object o) {
    	if(o instanceof IAnalysisDataset)
    		updateSelectionContext((IAnalysisDataset)o);
    	if(o instanceof IClusterGroup)
    		updateSelectionContext((IClusterGroup)o);
    	if(o instanceof IWorkspace)
    		updateSelectionContext((IWorkspace)o);
    }

    protected void updateSelectionContext(IAnalysisDataset d) {
    	updateSelectionContext(1);
    	workspaceSubMenu.removeAll(); 
    	createWorkspaceMenu(d);
    	
    	biosampleSubMenu.removeAll(); 
    	createBiosampleMenu(d);
    	
    	setAddNuclearSignalEnabled(d.isRoot());
    	setFishRemappingEnabled(d.isRoot());
    	
    	setDeleteString( d.isRoot() ? Labels.Populations.CLOSE_LBL : Labels.Populations.DELETE_LBL);
    }

    protected void updateSelectionContext(IClusterGroup group){
    	updateSelectionContext(0);
    	moveUpMenuItem.setEnabled(true);
    	moveDownMenuItem.setEnabled(true);
    	setDeleteString(Labels.Populations.DELETE_LBL);
    }
    
    protected void updateSelectionContext(IWorkspace workspace){
    	updateSelectionContext(0);
    	moveUpMenuItem.setEnabled(true);
    	moveDownMenuItem.setEnabled(true);
    	setDeleteString(Labels.Populations.DELETE_LBL);
    }
    
    protected void setDeleteString(String s) {
        deleteMenuItem.setText(s);
    }

    protected void setAddNuclearSignalEnabled(boolean b) {
        addNuclearSignalMenuItem.setEnabled(b);
    }

    protected void setFishRemappingEnabled(boolean b) {
        this.fishRemappinglMenuItem.setEnabled(b);
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
