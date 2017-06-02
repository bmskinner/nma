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


package com.bmskinner.nuclear_morphology.components.nuclear;

import java.awt.Color;
import java.io.File;
import java.io.Serializable;

/**
 * Signal groups are used to store common metadata about nuclear signals within
 * a cell collection - for example, the folder of images the signals were
 * detected in, or the colour with which the signals should be drawn in charts.
 * 
 * @author bms41
 * @since 1.13.3
 *
 */
public interface ISignalGroup extends Serializable {

    /**
     * Get the shell result for the group, if present
     * 
     * @return
     */
    IShellResult getShellResult();

    /**
     * Set the group shell result
     * 
     * @param result
     */
    void setShellResult(IShellResult result);

    /**
     * Test if a shell result is available
     * 
     * @return
     */
    boolean hasShellResult();

    /**
     * Get the name of the signal group
     * 
     * @return
     */
    String getGroupName();

    /**
     * Set the name of the signal group
     * 
     * @param groupName
     */
    void setGroupName(String groupName);

    /**
     * Test if the signals in this group are visible in charts
     * 
     * @return
     */
    boolean isVisible();

    /**
     * Set whether the signals in this group are visible in charts
     * 
     * @return
     */
    void setVisible(boolean isVisible);

    /**
     * Test if a custom colour has been set for this group
     * 
     * @return
     */
    boolean hasColour();

    /**
     * Get the colour for this signal group, if set
     * 
     * @return the colour, or null if not present
     */
    Color getGroupColour();

    /**
     * Set the signal colour for this group
     * 
     * @param groupColour
     */
    void setGroupColour(Color groupColour);

    /**
     * Get the RGB channel this signal was detected in
     * 
     * @return
     */
    int getChannel();

    /**
     * Set the RGB channel the signal was detected in
     * 
     * @param channel
     */
    void setChannel(int channel);

    /**
     * Get the folder the signals were found in
     * 
     * @return
     */
    File getFolder();

    /**
     * Set the folder of images the signals were found in
     * 
     * @param folder
     */
    void setFolder(File folder);

    String toString();

}
