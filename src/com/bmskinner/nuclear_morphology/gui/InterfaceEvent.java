/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package com.bmskinner.nuclear_morphology.gui;

import java.util.EventObject;

/**
 * Instruct a UI change or action, without requiring any particular datasets
 * selected
 * 
 * @author ben
 *
 */
public class InterfaceEvent extends EventObject {

    private static final long serialVersionUID = 1L;
    private String            sourceName;
    private InterfaceMethod   method;

    /**
     * Create an event from a source, with the given message
     * 
     * @param source
     *            the source of the datasets
     * @param message
     *            the instruction on what to do with the datasets
     * @param sourceName
     *            the name of the object or component generating the datasets
     * @param list
     *            the datasets to carry
     */
    public InterfaceEvent(Object source, InterfaceMethod method, String sourceName) {
        super(source);
        this.method = method;
        this.sourceName = sourceName;
    }

    public InterfaceEvent(Object source, InterfaceEvent event) {
        super(source);
        this.method = event.method();
        this.sourceName = event.sourceName();
    }

    /**
     * The name of the component that fired the event
     * 
     * @return
     */
    public String sourceName() {
        return this.sourceName;
    }

    /**
     * The message to carry
     * 
     * @return
     */
    public InterfaceMethod method() {
        return method;
    }

    public enum InterfaceMethod {

        // UPDATE_POPULATIONS ("Update populations"),
        UPDATE_PANELS("Update panels"), REFRESH_POPULATIONS("Refresh population panel datasets"), SAVE_ROOT(
                "Save root datasets"), CLEAR_CHART_CACHE("Clear the chart caches"), RECACHE_CHARTS(
                        "Recache charts"), LIST_SELECTED_DATASETS("List selected datasets"), CLEAR_LOG_WINDOW(
                                "Clear log window"), UPDATE_IN_PROGRESS("Updating"), UPDATE_COMPLETE(
                                        "Update finished"), DUMP_LOG_INFO("Dump logs for nuclei"), INFO(
                                                "Show information about the selected dataset"), KILL_ALL_TASKS(
                                                        "Attempt to halt all tasks in the executor service");

        private final String name;

        InterfaceMethod(String name) {
            this.name = name;
        }

        public String toString() {
            return this.name;
        }

    }
}
