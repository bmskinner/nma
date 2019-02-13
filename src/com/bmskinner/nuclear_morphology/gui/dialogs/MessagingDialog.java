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
package com.bmskinner.nuclear_morphology.gui.dialogs;

import java.awt.Dialog;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.gui.DefaultInputSupplier;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEventHandler;
import com.bmskinner.nuclear_morphology.gui.events.EventListener;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEvent;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEventHandler;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * This extension to a JDialog can fire DatasetEvents and InterfaceEvents to
 * registered listeners. Used to communicate dataset updates and chart recache
 * requests from dialogs which modify datasets.
 * 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public abstract class MessagingDialog extends JDialog implements Loggable {
    
    protected final DatasetEventHandler dh = new DatasetEventHandler(this);
    protected final InterfaceEventHandler ih = new InterfaceEventHandler(this);
    protected final InputSupplier inputSupplier = new DefaultInputSupplier();

    public MessagingDialog() {
        super();
    }

    /**
     * Create with the given Dialog as a parent. Use null to make this dialog
     * have a taskbar icon
     * 
     * @param d the parent. Can be null
     */
    public MessagingDialog(Dialog d) {
        super(d);
    }

    public synchronized void addDatasetEventListener(EventListener l) {
        dh.addListener(l);
    }

    public synchronized void removeDatasetEventListener(EventListener l) {
        dh.removeListener(l);
    }

    public synchronized void addInterfaceEventListener(EventListener l) {
    	ih.addListener(l);
    }

    public synchronized void removeInterfaceEventListener(EventListener l) {
    	ih.removeListener(l);
    }

    protected synchronized void fireDatasetEvent(String method, List<IAnalysisDataset> list) {
        DatasetEvent event = new DatasetEvent(this, method, this.getClass().getSimpleName(), list);
        dh.fire(event);
    }

    protected synchronized void fireDatasetEvent(String method, IAnalysisDataset dataset) {

        List<IAnalysisDataset> list = new ArrayList<IAnalysisDataset>();
        list.add(dataset);
        fireDatasetEvent(method, list);
    }

    protected synchronized void fireDatasetEvent(String method, List<IAnalysisDataset> list,
            IAnalysisDataset template) {

        DatasetEvent event = new DatasetEvent(this, method, this.getClass().getSimpleName(), list, template);
        dh.fire(event);
    }

    protected synchronized void fireDatasetEvent(DatasetEvent event) {
    	dh.fire(event);
    }

    protected synchronized void fireInterfaceEvent(InterfaceMethod method) {

        InterfaceEvent event = new InterfaceEvent(this, method, this.getClass().getSimpleName());
        ih.fire(event);
    }

    protected synchronized void fireInterfaceEvent(InterfaceEvent event) {
    	ih.fire(event);
    }

}
