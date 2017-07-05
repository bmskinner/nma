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


package com.bmskinner.nuclear_morphology.gui;

import java.util.EventObject;

public class SignalChangeEvent extends EventObject {

    public static final String SIGNAL_COLOUR_CHANGE = "SignalColourUpdate";
    public static final String GROUP_VISIBLE_PREFIX = "GroupVisble_";
    public static final String POST_FISH_MAPPING    = "PostFISHRemappingAction";

    public static final String LOBE_DETECTION              = "LobeDetection";
    public static final String EXPORT_STATS                = "ExportStatsAction";
    public static final String CHANGE_SCALE                = "ChangeScaleAction";
    public static final String EXPORT_WORKSPACE            = "ExportWorkspace";
    public static final String ADD_NUCLEAR_SIGNAL          = "AddNuclearSignalAction";
    public static final String DATASET_ARITHMETIC          = "DatasetArithmeticAction";
    public static final String CHANGE_NUCLEUS_IMAGE_FOLDER = "ChangeNucleusFolderAction";

    private static final long serialVersionUID = 1L;
    private String            message;
    private String            sourceName;

    /**
     * Create an event from a source, with the given message
     * 
     * @param source
     * @param type
     */
    public SignalChangeEvent(Object source, String message, String sourceName) {
        super(source);
        this.message = message;
        this.sourceName = sourceName;
    }

    public SignalChangeEvent(Object source, SignalChangeEvent event) {
        super(event.getSource());
        this.message = event.type();
        this.sourceName = event.sourceName();
    }

    /**
     * The type of event, or other message to carry
     * 
     * @return
     */
    public String type() {
        return message;
    }

    /**
     * The name of the component that fired the event
     * 
     * @return
     */
    public String sourceName() {
        return this.sourceName;
    }

}
