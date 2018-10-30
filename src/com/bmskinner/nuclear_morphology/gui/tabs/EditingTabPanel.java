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
package com.bmskinner.nuclear_morphology.gui.tabs;

import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.generic.Tag;

/**
 * Methods for editing datasets via a tab panel
 * 
 * @author bms41
 * @since 1.13.3
 *
 */
public interface EditingTabPanel extends TabPanel {

    void checkCellLock();

    /**
     * Update the border tag in the median profile to the given index, and
     * update individual nuclei to match.
     * 
     * @param tag
     * @param newTagIndex
     */
    void setBorderTagAction(@NonNull Tag tag, int newTagIndex);

    /**
     * Update the start index of the given segment to the given index in the
     * median profile, and update individual nuclei to match
     * 
     * @param id
     * @param index
     * @throws Exception
     */
    void updateSegmentStartIndexAction(@NonNull UUID id, int index) throws Exception;
}
