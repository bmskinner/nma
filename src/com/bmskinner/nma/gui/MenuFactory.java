package com.bmskinner.nma.gui;

import java.awt.Component;
import java.util.Collection;
import java.util.logging.Logger;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nma.gui.events.UserActionEvent;
import com.bmskinner.nma.gui.events.revamp.UserActionController;

/**
 * Simplify creation of menu items
 * 
 * @author bms41
 * @since 1.14.0
 */
public class MenuFactory {

	private static final Logger LOGGER = Logger.getLogger(MenuFactory.class.getName());

	/**
	 * The class for submenus in the popup menu
	 * 
	 * @author bms41
	 * @since 1.14.0
	 *
	 */
	public class ContextualMenu extends JMenu implements ContextEnabled {
		private final int context;

		/**
		 * Create a new popup item
		 * 
		 * @param title   the label for the menu
		 * @param context the activity context under which the menu is enabled
		 */
		public ContextualMenu(String title, int context) {
			super(title);
			this.context = context;
		}

		/**
		 * Update the item state based on the selected items
		 * 
		 * @param objects
		 */
		@Override
		public void updateSelectionContext(Collection<?> objects) {
			for (Component c : this.getMenuComponents()) {
				if (c instanceof ContextEnabled con)
					con.updateSelectionContext(objects);
			}
			setEnabled(ContextEnabled.matchesSelectionContext(objects, context));
		}

		@Override
		public void setEnabled(boolean b) {
			for (Component c : this.getComponents()) {
				c.setEnabled(b);
			}
			super.setEnabled(b);
		}
	}

	/**
	 * The class for items in the popup menu
	 * 
	 * @author bms41
	 * @since 1.14.0
	 *
	 */
	public class ContextualMenuItem extends JMenuItem implements ContextEnabled {
		private final int context;

		/**
		 * Create a new popup item
		 * 
		 * @param title   the label for the menu item
		 * @param event   the action to call
		 * @param context the selection states under which the item is enabled
		 */
		public ContextualMenuItem(@NonNull String title, @NonNull String event, int context, @Nullable String tooltip) {
			super(title);
			this.context = context;
			this.setToolTipText(tooltip);

			addActionListener(
					e -> UserActionController.getInstance().userActionEventReceived(new UserActionEvent(this, event)));
		}

		/**
		 * Update the item state based on the selected items
		 * 
		 * @param objects
		 */
		@Override
		public void updateSelectionContext(Collection<?> objects) {
			setEnabled(ContextEnabled.matchesSelectionContext(objects, context));
		}

	}

	public ContextualMenuItem makeItem(String label, String event, int context) {
		return new ContextualMenuItem(label, event, context, null);
	}

	public ContextualMenuItem makeItem(String label, String event, int context, String tooltip) {
		return new ContextualMenuItem(label, event, context, tooltip);
	}

	public ContextualMenu makeMenu(String label, int context) {
		return new ContextualMenu(label, context);
	}
}