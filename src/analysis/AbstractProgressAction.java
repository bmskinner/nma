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
package analysis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.RecursiveAction;
import logging.Loggable;

@SuppressWarnings("serial")
public abstract class AbstractProgressAction extends RecursiveAction implements ProgressListener, Loggable {

	private final List<Object> listeners = new ArrayList<Object>();
	
	public synchronized void addProgressListener( ProgressListener l ) {
		listeners.add( l );
	}

	public synchronized void removeProgressListener( ProgressListener l ) {
		listeners.remove( l );
	}

	protected synchronized void fireProgressEvent() {

		ProgressEvent event = new ProgressEvent( this);
		Iterator<Object> iterator = listeners.iterator();
		while( iterator.hasNext() ) {
			( (ProgressListener) iterator.next() ).progressEventReceived( event );
		}
	}

	@Override
	public void progressEventReceived(ProgressEvent event) {
		// pass up the chain
		fireProgressEvent();

	}

}
