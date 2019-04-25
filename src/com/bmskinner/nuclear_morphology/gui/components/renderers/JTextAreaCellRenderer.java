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
	public static final Color CONSISTENT_CELL_COLOUR = new Color(178, 255, 102);

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
        Color foreground = Color.BLACK;
        if (value != null && !value.toString().equals("")) {
            if(value.toString().equals("false") || value.toString().equals(Labels.NA)) {
                foreground = Color.GRAY;
            }
        }
        
        Color background = Color.WHITE;
        if(isRowConsistentAcrossColumns(table, row))
        	background = CONSISTENT_CELL_COLOUR;
        
        setBackground(background);
        setForeground(foreground);
        return this;
    }
    
    /**
     * Test if the values across the given row are consistent between columns
     * 
     * @param table
     * @param row
     * @return
     */
    protected boolean isRowConsistentAcrossColumns(JTable table, int row) {

        if (table.getColumnCount() <= 2) // don't colour single datasets
        	return false;

        Object test = table.getModel().getValueAt(row, 1);
        for (int col = 1; col < table.getColumnCount(); col++) {
        	Object value = table.getModel().getValueAt(row, col);

        	// Ignore empty cells
        	if(value==null || value.toString().equals(""))
        		return false;

        	if (!test.equals(value))
        		return false;
        }
        return true;
    }
}
