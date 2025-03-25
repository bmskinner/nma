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
package com.bmskinner.nma.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Count the number of bytes read in an input stream, and 
 * inform count listeners. Used for tracking opening of datasets
 * @author Ben Skinner
 * @since 1.13.8
 *
 */
public class CountedInputStream extends FilterInputStream {

	private long totalBytes = 0;
	private List<CountListener> listeners = new ArrayList<>();
	
	/**
	 * Construct with an input stream
	 * @param in
	 */
	public CountedInputStream(InputStream in) {
		super(in);
	}
	
	@Override
	public int read() throws IOException {
		totalBytes++; 
		fireCountEvent();
        return in.read();
    }
	
	@Override
	public int read(byte b[], int off, int len) throws IOException {
		totalBytes+=len;
		fireCountEvent();
        return in.read(b, off, len);
    }
		
	public void addCountListener(CountListener l){
		listeners.add(l);
	}
	
	public void removeCountListener(CountListener l){
		listeners.remove(l);
	}
	
	protected void fireCountEvent(){
		for(CountListener l : listeners){
			l.countChanged(totalBytes);
		}
	}
}
