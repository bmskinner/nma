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
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import com.bmskinner.nuclear_morphology.components.generic.BorderTag;
import com.bmskinner.nuclear_morphology.components.generic.BorderTagObject;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;
import com.bmskinner.nuclear_morphology.gui.dialogs.SettingsDialog;

@SuppressWarnings("serial")
public class RulesetSaveDialog extends SettingsDialog {

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

    private JPanel createHeader() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        panel.add(new JLabel("Select and name custom rulesets to retain"));

        return panel;
    }

    private void createUI() {
        this.setLayout(new BorderLayout());
        main = new JPanel();

        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setBorder(new EmptyBorder(5, 5, 5, 5));

        for (String s : customCollections.keySet()) {

            CollectionPanel p = new CollectionPanel(s, customCollections.get(s));
            main.add(p);
        }

        JPanel header = createHeader();
        JPanel footer = createFooter();

        this.add(header, BorderLayout.NORTH);
        this.add(main, BorderLayout.CENTER);
        this.add(footer, BorderLayout.SOUTH);

    }

    public RuleSetCollection getSelected() {
        RuleSetCollection result = new RuleSetCollection();
        for (Component c : main.getComponents()) {

            if (c instanceof CollectionPanel) {
                RuleSetCollection r = ((CollectionPanel) c).getCollection();

                BorderTagObject o = new BorderTagObject(((CollectionPanel) c).getText(), BorderTag.CUSTOM);

                result.setRuleSets(o, r.getRuleSets(Tag.CUSTOM_POINT));
                // log("Added custom point: "+o.toString());
            }

        }
        return result;
    }

    public class CollectionPanel extends JPanel {
        private RuleSetCollection collection;
        final JCheckBox           box;
        final JTextField          text;

        public CollectionPanel(String s, RuleSetCollection collection) {
            this.setLayout(new FlowLayout());
            this.collection = collection;
            box = new JCheckBox("", true);
            text = new JTextField(s);

            box.addActionListener(e -> {
                text.setEnabled(box.isSelected());
            });

            this.add(box);
            this.add(text);
        }

        public String getText() {
            return text.getText();
        }

        public RuleSetCollection getCollection() {
            return collection;
        }

    }

}
