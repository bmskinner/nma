package com.bmskinner.nuclear_morphology.gui.components.renderers;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;

import com.bmskinner.nuclear_morphology.gui.Labels;

/**
 * A renderer that displays cell contents within a JTextArea
 * allowing multi-line cells
 * @author ben
 * @since 1.16.0
 *
 */
public class JTextAreaCellRenderer extends JTextArea implements TableCellRenderer {
	private static final Font DEFAULT_FONT = UIManager.getFont("Label.font");

    private void setColor(boolean isSelected, JTable table) {
        setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
        setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
    }

    @Override
    public Component getTableCellRendererComponent(JTable table,
            Object value, boolean isSelected, boolean hasFocus, int row,
            int column) {
        setText(value == null ? "" : value.toString());
        setColor(isSelected,table);
        setFont(DEFAULT_FONT);
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setLineWrap(true);
        setWrapStyleWord(true);
        Color colour = Color.BLACK;
        if (value != null && !value.toString().equals("")) {
            if(value.toString().equals("false") || value.toString().equals(Labels.NA)) {
                colour = Color.GRAY;
            }
        }

        setForeground(colour);
        return this;
    }
}
