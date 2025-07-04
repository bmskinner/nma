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
package com.bmskinner.nma.gui.tabs.populations;

import java.awt.Component;
import java.util.Collection;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.IClusterGroup;
import com.bmskinner.nma.components.workspaces.IWorkspace;
import com.bmskinner.nma.gui.AbstractPopupMenu;
import com.bmskinner.nma.gui.ContextEnabled;
import com.bmskinner.nma.gui.Labels;
import com.bmskinner.nma.gui.MenuFactory;
import com.bmskinner.nma.gui.MenuFactory.ContextualMenu;
import com.bmskinner.nma.gui.MenuFactory.ContextualMenuItem;
import com.bmskinner.nma.gui.events.UserActionEvent;

@SuppressWarnings("serial")
public class PopulationListPopupMenu extends AbstractPopupMenu {

	private ContextualMenuItem moveUpMenuItem;
	private ContextualMenuItem moveDownMenuItem;

//	private ContextualMenuItem deleteMenuItem;

	private ContextualMenu workspaceSubMenu;
	private ContextualMenu biosampleSubMenu;

	private ContextualMenuItem saveMenuItem;

	private ContextualMenuItem replaceFolderMenuItem;

	public PopulationListPopupMenu() {
		super();
	}

	protected void addMoveMenuItems() {
		this.add(moveUpMenuItem);
		this.add(moveDownMenuItem);
	}

	@Override
	public void createButtons() {

		MenuFactory fact = new MenuFactory();
		saveMenuItem = fact.makeItem(Labels.Populations.SAVE_AS_LBL,
				UserActionEvent.SAVE_SELECTED_DATASET,
				ContextEnabled.ACTIVE_ON_ROOT_DATASET | ContextEnabled.ACTIVE_ON_SINGLE_OBJECT);

		moveUpMenuItem = fact.makeItem(Labels.Populations.MOVE_UP_LBL,
				UserActionEvent.MOVE_DATASET_UP_ACTION,
				ContextEnabled.ACTIVE_ON_ROOT_DATASET | ContextEnabled.ACTIVE_ON_CHILD_DATASET
						| ContextEnabled.ACTIVE_ON_SINGLE_OBJECT);
		moveDownMenuItem = fact.makeItem(Labels.Populations.MOVE_DOWN_LBL,
				UserActionEvent.MOVE_DATASET_DOWN_ACTION,
				ContextEnabled.ACTIVE_ON_ROOT_DATASET | ContextEnabled.ACTIVE_ON_CHILD_DATASET
						| ContextEnabled.ACTIVE_ON_SINGLE_OBJECT);

		workspaceSubMenu = fact.makeMenu(Labels.Populations.ADD_TO_WORKSPACE_LBL,
				ContextEnabled.ACTIVE_ON_ROOT_DATASET | ContextEnabled.ACTIVE_ON_CHILD_DATASET
						| ContextEnabled.ACTIVE_ON_SINGLE_OBJECT
						| ContextEnabled.ACTIVE_ON_MULTI_OBJECTS);

//		createWorkspaceMenu(null);
//		biosampleSubMenu = fact.makeMenu(Labels.Populations.ADD_TO_BIOSAMPLE_LBL,
//				ContextEnabled.ACTIVE_ON_ROOT_DATASET | ContextEnabled.ACTIVE_ON_SINGLE_OBJECT);
//		createBiosampleMenu(null);

//		mergeMenuItem = fact.makeItem(Labels.Populations.MERGE_LBL, UserActionEvent.MERGE_DATASETS_ACTION,
//				ContextEnabled.ACTIVE_ON_ROOT_DATASET | ContextEnabled.ACTIVE_ON_CHILD_DATASET
//						| ContextEnabled.ACTIVE_ON_MULTI_OBJECTS);

//		curateMenuItem = fact.makeItem(Labels.Populations.CURATE_LBL, UserActionEvent.CURATE_DATASET,
//				ContextEnabled.ACTIVE_ON_ROOT_DATASET | ContextEnabled.ACTIVE_ON_CHILD_DATASET
//						| ContextEnabled.ACTIVE_ON_SINGLE_OBJECT);

//		deleteMenuItem = fact.makeItem(Labels.Populations.DELETE_LBL, UserActionEvent.DELETE_DATASET,
//				ContextEnabled.ACTIVE_ON_ROOT_DATASET | ContextEnabled.ACTIVE_ON_CHILD_DATASET
//						| ContextEnabled.ACTIVE_ON_SINGLE_OBJECT | ContextEnabled.ACTIVE_ON_MULTI_OBJECTS);

//		booleanMenuItem = fact.makeItem(Labels.Populations.ARITHMETIC_LBL, UserActionEvent.DATASET_ARITHMETIC,
//				ContextEnabled.ACTIVE_ON_ROOT_DATASET | ContextEnabled.ACTIVE_ON_CHILD_DATASET
//						| ContextEnabled.ACTIVE_ON_MULTI_OBJECTS);

//		extractMenuItem = fact.makeItem(Labels.Populations.EXTRACT_CELLS_LBL, UserActionEvent.EXTRACT_SUBSET,
//				ContextEnabled.ACTIVE_ON_ROOT_DATASET | ContextEnabled.ACTIVE_ON_CHILD_DATASET
//						| ContextEnabled.ACTIVE_ON_SINGLE_OBJECT);
//
//		changeScaleItem = fact.makeItem(Labels.Populations.CHANGE_SCALE_LBL, UserActionEvent.CHANGE_SCALE,
//				ContextEnabled.ACTIVE_ON_ROOT_DATASET | ContextEnabled.ACTIVE_ON_CHILD_DATASET
//						| ContextEnabled.ACTIVE_ON_MULTI_OBJECTS | ContextEnabled.ACTIVE_ON_SINGLE_OBJECT);

//		relocateMenuItem = fact.makeItem(Labels.Populations.ADD_CHILD_CELLS_LBL, UserActionEvent.RELOCATE_CELLS,
//				ContextEnabled.ACTIVE_ON_ROOT_DATASET | ContextEnabled.ACTIVE_ON_CHILD_DATASET
//						| ContextEnabled.ACTIVE_ON_SINGLE_OBJECT);

//		replaceFolderMenuItem = fact.makeItem(Labels.Populations.CHANGE_FOLDER_LBL,
//				UserActionEvent.CHANGE_NUCLEUS_IMAGE_FOLDER,
//				ContextEnabled.ACTIVE_ON_ROOT_DATASET | ContextEnabled.ACTIVE_ON_SINGLE_OBJECT);

	}

	@Override
	protected void addButtons() {

		addMoveMenuItems();

//		addSeparator();

//		add(workspaceSubMenu);
//    	add(biosampleSubMenu);

//		addSeparator();

//		add(mergeMenuItem);
//		add(deleteMenuItem);
//		add(booleanMenuItem);
//		add(curateMenuItem);

//		addSeparator();

//		add(saveMenuItem);

//		addSeparator();

//		add(replaceFolderMenuItem);

	}

//	private void createWorkspaceMenu(@Nullable IAnalysisDataset d) {
//		if (d == null)
//			return;
//		MenuFactory fact = new MenuFactory();
//		List<IWorkspace> workspaces = DatasetListManager.getInstance().getWorkspaces();
//		for (IWorkspace w : workspaces) {
//			String name = w.has(d) ? Labels.Populations.REMOVE_FROM_LBL_PREFIX
//					: Labels.Populations.ADD_TO_LBL_PREFIX;
//			String action = w.has(d) ? UserActionEvent.REMOVE_FROM_WORKSPACE_PREFIX
//					: UserActionEvent.ADD_TO_WORKSPACE;
//			workspaceSubMenu.add(fact.makeItem(name + w.getName(), action + w.getName(),
//					ContextEnabled.ACTIVE_ON_ROOT_DATASET | ContextEnabled.ACTIVE_ON_CHILD_DATASET
//							| ContextEnabled.ACTIVE_ON_SINGLE_OBJECT
//							| ContextEnabled.ACTIVE_ON_MULTI_OBJECTS));
//		}
//	}

//	private void createBiosampleMenu(@Nullable IAnalysisDataset d) {
//		if (d == null || !DatasetListManager.getInstance().isInWorkspace(d)) {
//			biosampleSubMenu.setEnabled(false);
//			return;
//		}
//
//		List<IWorkspace> workspaces = DatasetListManager.getInstance().getWorkspaces(d);
//
//		MenuFactory fact = new MenuFactory();
//
//		biosampleSubMenu.add(fact.makeItem(Labels.Populations.ADD_TO_NEW_LBL, UserActionEvent.NEW_BIOSAMPLE_PREFIX,
//				ContextEnabled.ACTIVE_ON_ROOT_DATASET | ContextEnabled.ACTIVE_ON_SINGLE_OBJECT));
//		biosampleSubMenu.addSeparator();
//
//		for (IWorkspace w : workspaces) {
//			for (BioSample bs : w.getBioSamples()) {
//				String name = bs.hasDataset(d.getSavePath()) ? Labels.Populations.REMOVE_FROM_LBL_PREFIX
//						: Labels.Populations.ADD_TO_LBL_PREFIX;
//				String action = bs.hasDataset(d.getSavePath()) ? UserActionEvent.REMOVE_FROM_BIOSAMPLE_PREFIX
//						: UserActionEvent.ADD_TO_BIOSAMPLE_PREFIX;
//				biosampleSubMenu.add(fact.makeItem(name + bs.getName(), action + bs.getName(),
//						ContextEnabled.ACTIVE_ON_ROOT_DATASET | ContextEnabled.ACTIVE_ON_SINGLE_OBJECT));
//			}
//		}
//	}

	/**
	 * Tell the menu items to update their state based on the selected items
	 * 
	 * @param nItems the number of selected items
	 */
	@Override
	public void updateSelectionContext(Collection<Object> objects) {

		if (objects.size() == 1) {
			Object o = objects.stream().findFirst().get();
			if (o instanceof IAnalysisDataset)
				updateSelectionContext((IAnalysisDataset) o);
			if (o instanceof IClusterGroup)
				updateSelectionContext((IClusterGroup) o);
			if (o instanceof IWorkspace)
				updateSelectionContext((IWorkspace) o);
		}

		for (Component c : this.getComponents()) {
			if (c instanceof ContextEnabled)
				((ContextEnabled) c).updateSelectionContext(objects);
		}

	}

	protected void updateSelectionContext(IAnalysisDataset d) {
		workspaceSubMenu.removeAll();
//		createWorkspaceMenu(d);

		biosampleSubMenu.removeAll();
//		createBiosampleMenu(d);

//		setAddNuclearSignalEnabled(d.isRoot());
//		setFishRemappingEnabled(d.isRoot());

//		setDeleteString(d.isRoot() ? Labels.Populations.CLOSE_LBL : Labels.Populations.DELETE_LBL);
	}

	protected void updateSelectionContext(IClusterGroup group) {
		moveUpMenuItem.setEnabled(true);
		moveDownMenuItem.setEnabled(true);
//		setDeleteString(Labels.Populations.DELETE_LBL);
	}

	protected void updateSelectionContext(IWorkspace workspace) {
		moveUpMenuItem.setEnabled(true);
		moveDownMenuItem.setEnabled(true);
//		setDeleteString(Labels.Populations.DELETE_LBL);
	}

//	protected void setDeleteString(String s) {
//		deleteMenuItem.setText(s);
//	}

//	protected void setAddNuclearSignalEnabled(boolean b) {
//		addNuclearSignalMenuItem.setEnabled(b);
//	}
//
//	protected void setFishRemappingEnabled(boolean b) {
//		this.fishRemappinglMenuItem.setEnabled(b);
//	}
}
