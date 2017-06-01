package com.bmskinner.nuclear_morphology.charting.datasets;

import java.awt.Color;
import java.util.UUID;

/**
 * This allows a signal group UUID to be stored with the signal group number and
 * the signal colour for use in table formatting.
 * 
 * @author bms41
 *
 */
public class SignalTableCell {

    private UUID   id;
    private String name;
    private Color  color = Color.WHITE;

    /**
     * Construct with a signal ID, a signal name and the signal display colour
     * 
     * @param id
     * @param name
     * @param color
     */
    public SignalTableCell(UUID id, String name, Color color) {

        if (id == null || name == null) {
            throw new IllegalArgumentException("ID or name is null");
        }
        this.id = id;
        this.name = name;

        if (color != null) {
            this.color = color;
        }

    }

    public UUID getID() {
        return id;
    }

    public String toString() {
        return name;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

}
