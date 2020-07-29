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
package com.bmskinner.nuclear_morphology.gui.tabs.editing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.rules.RuleSet;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;
import com.bmskinner.nuclear_morphology.gui.dialogs.SettingsDialog;

/**
 * Allows new or modified rulesets to be saved against
 * a tag
 * @author ben
 *
 */
@SuppressWarnings("serial")
public class RulesetSaveDialog extends SettingsDialog {
	
	private static final Logger LOGGER = Logger.getLogger(RulesetSaveDialog.class.getName());

    private Map<String, RuleSetCollection> customCollections;

    private JPanel main;

    public RulesetSaveDialog(Dialog owner, Map<String, RuleSetCollection> customCollections) {
        super(owner, true);
        this.setTitle("Save rulesets");
        this.customCollections = customCollections;
        createUI();
        this.pack();
        this.setVisible(true);
    }

    @Override
    protected JPanel createHeader() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.add(new JLabel("Select and name custom rulesets to retain"));
        return panel;
    }

    private void createUI() {
        this.setLayout(new BorderLayout());
        main = new JPanel();

        main.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        
        main.setBorder(new EmptyBorder(5, 5, 5, 5));

        for(Entry<String, RuleSetCollection> entry : customCollections.entrySet() ) {
        	main.add(new JLabel(entry.getKey()), c);
        	for(Tag t : entry.getValue().getTags()) {
        		c.gridy++;
        		main.add(new RuleSetPanel(entry.getKey(), t), c);

        	}
        	c.gridy++;
        }

        add(createHeader(), BorderLayout.NORTH);
        add(main, BorderLayout.CENTER);
        add(createFooter(), BorderLayout.SOUTH);
    }

    /**
     * Create a rule set collection containing only the selected 
     * tags from the custom collections
     * @return
     */
    public RuleSetCollection getSelected() {
        RuleSetCollection result = new RuleSetCollection();
        for (Component c : main.getComponents()) {
        	
        	if (c instanceof RuleSetPanel) {
        		RuleSetPanel p  = (RuleSetPanel)c;
        		if(p.isSelected()) {
        			RuleSetCollection r = customCollections.get(p.getCollectionName());
        			List<RuleSet> rules =  r.getRuleSets(p.oldTag());
        			result.setRuleSets(p.getTag(), rules);
        		}
        	}
        }
        return result;
    }
    
    /**
     * A UI panel displaying a single tag name within a collection
     * @author ben
     *
     */
    public class RuleSetPanel extends JPanel {

    	 private final JCheckBox box;
    	 private final JTextField text;
    	 private final String collectionName;
    	 private final Tag oldTag;
         
         public RuleSetPanel(String collectionName, Tag t) {
        	 this.collectionName = collectionName;
        	 this.oldTag = t;
        	 this.setLayout(new FlowLayout());
        	 box = new JCheckBox("", true);
        	 text = new JTextField(t.toString());
        	 box.addActionListener(e->text.setEnabled(box.isSelected()));
        	 
        	 add(box);
        	 add(text);
         }
         
         public boolean isSelected() {
        	 return box.isSelected();
         }
         
         public String getCollectionName() {
        	 return collectionName;
         }
         
         public Tag oldTag() {
        	 return oldTag;
         }
         
         public Tag getTag() {
        	 return Tag.of(text.getText());
         }
    }
}
