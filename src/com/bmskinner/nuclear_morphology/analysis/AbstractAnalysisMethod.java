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


package com.bmskinner.nuclear_morphology.analysis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;

/**
 * Stores the basic methods for an IAnalysisMethod
 * 
 * @author ben
 *
 */
public abstract class AbstractAnalysisMethod implements IAnalysisMethod, ProgressListener {

    protected IAnalysisDataset dataset;
    private List<Object>       listeners = new ArrayList<Object>();
    protected IAnalysisResult  result    = null;

    public AbstractAnalysisMethod(IAnalysisDataset dataset) {
        this.dataset = dataset;
    }

    @Override
    public void addProgressListener(ProgressListener l) {
        listeners.add(l);
    }

    @Override
    public void removeProgressListener(ProgressListener l) {
        listeners.remove(l);
    }

    protected void fireProgressEvent() {
        ProgressEvent e = new ProgressEvent(this);
        fireProgressEvent(e);
    }

    protected void fireProgressEvent(ProgressEvent e) {
        Iterator<Object> iterator = listeners.iterator();
        while (iterator.hasNext()) {
            ((ProgressListener) iterator.next()).progressEventReceived(e);
        }
    }

    @Override
    public void progressEventReceived(ProgressEvent event) {
        // log("Heard progress event in method");
        fireProgressEvent(); // pass upwards

    }

}
