/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


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
