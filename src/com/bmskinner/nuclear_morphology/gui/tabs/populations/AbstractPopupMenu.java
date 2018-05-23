package com.bmskinner.nuclear_morphology.gui.tabs.populations;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.IWorkspace;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.SignalChangeEvent;
import com.bmskinner.nuclear_morphology.gui.SignalChangeListener;
import com.bmskinner.nuclear_morphology.main.DatasetListManager;

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
    
    private JMenu addWorkspaceSubMenu;
    private PopupMenuItem newWorkspaceMenuItem;
    
    private PopupMenu exportSubMenu;
    private PopupMenuItem exportStatsMenuItem;
    private PopupMenuItem exportSignalsItem;
    private PopupMenuItem exportShellsItem;
    private PopupMenuItem saveCellsMenuItem;
    
    private PopupMenuItem saveMenuItem;
    private PopupMenuItem relocateMenuItem;

    private JMenuItem replaceFolderMenuItem;
   
    private PopupMenu addSubMenu;
    private PopupMenuItem addNuclearSignalMenuItem;
    private PopupMenuItem fishRemappinglMenuItem;

    private List<Object> listeners = new ArrayList<Object>();
    
    
    /**
     * Interface for components that have their enabled state driven 
     * by the number of selected objects
     * @author bms41
     * @since 1.14.0
     *
     */
    public interface ContextEnabled {
    	void setContext(int nObjects);
    }
    
    /**
     * The class for submenus in the popup menu
     * @author bms41
     * @since 1.14.0
     *
     */
    public class PopupMenu extends JMenu implements ContextEnabled {
    	ActiveContext context;
    	
    	
    	/**
    	 * Create a new popup item
    	 * @param title the label for the menu
    	 * @param context the activity context under which the menu is enabled 
    	 */
    	public PopupMenu(String title, ActiveContext context) {
    		super(title);
    		this.context = context;
    	}
    	
    	/**
    	 * Update the item state based on the total number of selected items
    	 * @param nObjects
    	 */
    	public void setContext(int nObjects) {
    		if(nObjects<=0) {
    			setEnabled(false);
    		}
    		
    		if(nObjects==1) {
    			setEnabled(context.equals(ActiveContext.SINGLE_AND_MULTIPLE_OBJECTS) || context.equals(ActiveContext.SINGLE_OBJECT_ONLY));
    		}
    		
    		if(nObjects>1) {
    			setEnabled(context.equals(ActiveContext.SINGLE_AND_MULTIPLE_OBJECTS) || context.equals(ActiveContext.MULTIPLE_OBJECTS_ONLY));
    		}
    	}
    }
    
    
    /**
     * The class for items in the popup menu
     * @author bms41
     * @since 1.14.0
     *
     */
    public class PopupMenuItem extends JMenuItem implements ContextEnabled {
    	ActiveContext context;
    	
    	
    	/**
    	 * Create a new popup item
    	 * @param title the label for the menu item
    	 * @param event the action to call
    	 * @param context the activity context under which the item is enabled 
    	 */
    	public PopupMenuItem(String title, String event, ActiveContext context) {
    		super(title);
    		this.context = context;
    		addActionListener(e -> fireSignalChangeEvent(event));
    	}
    	
    	/**
    	 * Update the item state based on the total number of selected items
    	 * @param nObjects
    	 */
    	public void setContext(int nObjects) {    		
    		if(nObjects<=0) {
    			setEnabled(false);
    		}
    		if(nObjects==1) {
    			setEnabled(context.equals(ActiveContext.SINGLE_AND_MULTIPLE_OBJECTS) || context.equals(ActiveContext.SINGLE_OBJECT_ONLY));
    		}
    		if(nObjects>1) {
    			setEnabled(context.equals(ActiveContext.SINGLE_AND_MULTIPLE_OBJECTS) || context.equals(ActiveContext.MULTIPLE_OBJECTS_ONLY));
    		}
    	}
    }
    
    /**
     * Track when a menu item should be active.
     * Objects can be datasets, cluster groups, workspaces,
     * or anything else in the populations menu
     * @author bms41
     * @since 1.14.0
     *
     */
    public enum ActiveContext {
    	SINGLE_OBJECT_ONLY,
    	MULTIPLE_OBJECTS_ONLY,
    	SINGLE_AND_MULTIPLE_OBJECTS
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

    	this.addSeparator();
    	this.add(addWorkspaceSubMenu);
    	this.addSeparator();

    	this.add(mergeMenuItem);
    	this.add(deleteMenuItem);
    	this.add(booleanMenuItem);
    	this.add(curateMenuItem);

    	this.addSeparator();

    	this.add(saveMenuItem);

    	this.addSeparator();

    	this.add(relocateMenuItem);

    	this.addSeparator();

    	this.add(replaceFolderMenuItem);
    	this.add(exportSubMenu);
    	this.add(changeScaleItem);

    	this.addSeparator();

    	this.add(addSubMenu);
    }

    public void setDeleteString(String s) {
        deleteMenuItem.setText(s);
    }
    
    private void createWorkspaceMenu(@Nullable IAnalysisDataset d) {

        newWorkspaceMenuItem = new PopupMenuItem(Labels.Populations.NEW_WORKSPACE, SignalChangeEvent.NEW_WORKSPACE, ActiveContext.SINGLE_OBJECT_ONLY);
        
        addWorkspaceSubMenu.add(newWorkspaceMenuItem);
        addWorkspaceSubMenu.addSeparator();
        
        if(d!=null) {
        	List<IWorkspace> workspaces = DatasetListManager.getInstance().getWorkspaces();
        	for(IWorkspace w : workspaces) {
        		if(w.has(d)) {
        			addWorkspaceSubMenu.add(new PopupMenuItem("Remove from "+w.getName(), "RemoveFromWorkspace|"+w.getName(), ActiveContext.SINGLE_OBJECT_ONLY));
        		} else {
        			addWorkspaceSubMenu.add(new PopupMenuItem("Add to "+w.getName(),"AddToWorkspace|"+w.getName(), ActiveContext.SINGLE_OBJECT_ONLY));
        		}
        	}
        }
    }

    public void createButtons() {
    	
    	saveMenuItem = new PopupMenuItem(Labels.Populations.SAVE_AS_LBL, SignalChangeEvent.SAVE_DATASET_ACTION, ActiveContext.SINGLE_OBJECT_ONLY);
        
    	moveUpMenuItem   = new PopupMenuItem("Move up", "MoveDatasetUpAction", ActiveContext.SINGLE_OBJECT_ONLY);
        moveDownMenuItem = new PopupMenuItem("Move down", "MoveDatasetDownAction", ActiveContext.SINGLE_OBJECT_ONLY);
        
        addWorkspaceSubMenu =  new PopupMenu(Labels.Populations.ADD_TO_WORKSPACE_LBL, ActiveContext.SINGLE_OBJECT_ONLY);
        createWorkspaceMenu(null);
        
        mergeMenuItem = new PopupMenuItem("Merge", "MergeCollectionAction", ActiveContext.MULTIPLE_OBJECTS_ONLY);

        curateMenuItem = new PopupMenuItem("Curate", "CurateCollectionAction", ActiveContext.SINGLE_OBJECT_ONLY);
        deleteMenuItem = new PopupMenuItem("Delete", "DeleteCollectionAction", ActiveContext.SINGLE_AND_MULTIPLE_OBJECTS);
        booleanMenuItem = new PopupMenuItem("Boolean", "DatasetArithmeticAction", ActiveContext.SINGLE_AND_MULTIPLE_OBJECTS);

  
        extractMenuItem = new PopupMenuItem("Extract cells", SignalChangeEvent.EXTRACT_SUBSET, ActiveContext.SINGLE_OBJECT_ONLY);

        changeScaleItem = new PopupMenuItem(Labels.Populations.CHANGE_SCALE_LBL, SignalChangeEvent.CHANGE_SCALE, ActiveContext.SINGLE_AND_MULTIPLE_OBJECTS);
        relocateMenuItem = new PopupMenuItem(Labels.Populations.RELOCATE_CELLS_LBL, SignalChangeEvent.RELOCATE_CELLS, ActiveContext.SINGLE_OBJECT_ONLY);
        
        exportSubMenu = new PopupMenu(Labels.Populations.EXPORT, ActiveContext.SINGLE_AND_MULTIPLE_OBJECTS);
        exportStatsMenuItem = new PopupMenuItem(Labels.Populations.EXPORT_STATS, SignalChangeEvent.EXPORT_STATS, ActiveContext.SINGLE_AND_MULTIPLE_OBJECTS);
        exportSignalsItem = new PopupMenuItem(Labels.Populations.EXPORT_SIGNALS, SignalChangeEvent.EXPORT_SIGNALS, ActiveContext.SINGLE_AND_MULTIPLE_OBJECTS);  
        exportShellsItem = new PopupMenuItem(Labels.Populations.EXPORT_SHELLS, SignalChangeEvent.EXPORT_SHELLS, ActiveContext.SINGLE_AND_MULTIPLE_OBJECTS);
        saveCellsMenuItem = new PopupMenuItem(Labels.Populations.EXPORT_CELL_LOCS, SignalChangeEvent.EXPORT_CELL_LOCS, ActiveContext.SINGLE_AND_MULTIPLE_OBJECTS);

        exportSubMenu.add(exportStatsMenuItem);
        exportSubMenu.add(exportSignalsItem);
        exportSubMenu.add(exportShellsItem);
        exportSubMenu.add(saveCellsMenuItem);
        
        addNuclearSignalMenuItem = new PopupMenuItem(Labels.Populations.ADD_NUCLEAR_SIGNAL_LBL, SignalChangeEvent.ADD_NUCLEAR_SIGNAL, ActiveContext.SINGLE_OBJECT_ONLY);
        addNuclearSignalMenuItem.setToolTipText(Labels.Populations.ADD_NUCLEAR_SIGNAL_TIP);
        fishRemappinglMenuItem = new PopupMenuItem(Labels.Populations.POST_FISH_MAPPING_LBL, SignalChangeEvent.POST_FISH_MAPPING, ActiveContext.SINGLE_OBJECT_ONLY);
        
        addSubMenu = new PopupMenu(Labels.Populations.ADD, ActiveContext.SINGLE_OBJECT_ONLY);
        addSubMenu.add(addNuclearSignalMenuItem);
        addSubMenu.add(fishRemappinglMenuItem);
        
        replaceFolderMenuItem = new PopupMenuItem("Change folder", "ChangeNucleusFolderAction", ActiveContext.SINGLE_OBJECT_ONLY);

    }
    
    /**
     * Tell the menu items to update their state based on the number of selected items
     * @param nItems the number of selected items
     */
    public void updateSelectionContext(int nItems) {
    	for (Component c : this.getComponents()) {
    		if(c instanceof ContextEnabled)
    			((ContextEnabled) c).setContext(nItems);
        }
    }

    public void setEnabled(boolean b) {
        for (Component c : this.getComponents()) {
            c.setEnabled(b);
        }
    }
    
    public void updateSingle(IAnalysisDataset d) {
    	addWorkspaceSubMenu.removeAll(); 
    	createWorkspaceMenu(d);
    	
    	setAddNuclearSignalEnabled(d.isRoot());
    	setFishRemappingEnabled(d.isRoot());
    	
    	if (d.isRoot()) {
            setDeleteString("Close");
        } else {
            setDeleteString("Delete");
        }
    }
    
    public void updateClusterGroup(){
    	setEnabled(false);
    	moveUpMenuItem.setEnabled(true);
    	moveDownMenuItem.setEnabled(true);
    }

    protected void setAddNuclearSignalEnabled(boolean b) {
        addNuclearSignalMenuItem.setEnabled(b);
    }

    protected void setFishRemappingEnabled(boolean b) {
        this.fishRemappinglMenuItem.setEnabled(b);
    }

    public synchronized void addSignalChangeListener(SignalChangeListener l) {
        listeners.add(l);
    }

    public synchronized void removeSignalChangeListener(SignalChangeListener l) {
        listeners.remove(l);
    }

    protected synchronized void fireSignalChangeEvent(String message) {
        SignalChangeEvent event = new SignalChangeEvent(this, message, SOURCE_COMPONENT);
        Iterator<Object> iterator = listeners.iterator();
        while (iterator.hasNext()) {
            ((SignalChangeListener) iterator.next()).signalChangeReceived(event);
        }
    }

}
