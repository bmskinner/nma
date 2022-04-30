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
package com.bmskinner.nma.gui.events;

import java.util.EventObject;

/**
 * This listener receives ChartSetEvents. These events are fired by
 * ExportableChartPanels when setChart() is invoked, and allows the UI to update
 * chart panels as soon as the new chart is in place
 * 
 * @author bms41
 *
 */
public interface ChartSetEventListener {
    void chartSetEventReceived(ChartSetEvent e);
    
    public class ChartSetEvent extends EventObject {

        public ChartSetEvent(Object source) {
            super(source);

        }

    }
}
