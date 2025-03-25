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
package com.bmskinner.nma.analysis;

/**
 * This interface allows AnalysisWorkers to detect progress through a
 * multithreaded task run in an AbstractProgressAction.
 * 
 * @author Ben Skinner
 * @since 1.12.0
 *
 */
public interface ProgressListener {

    /**
     * The action to take when a progress event is heard by this listener
     * 
     * @param event
     */
    void progressEventReceived(ProgressEvent event);
}
