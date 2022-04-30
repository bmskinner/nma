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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Count the number of bytes written in an output stream, and 
 * inform count listeners. Used for tracking saving of datasets
 * @author ben
 * @since 1.13.8
 *
 */
public class CountedOutputStream extends FilterOutputStream {
	
	private long totalBytes = 0;
	private List<CountListener> listeners = new ArrayList<>();
	
	public CountedOutputStream(OutputStream out) {
		super(out);
	}
	
	@Override
	public void write(int b) throws IOException{
		totalBytes++; 
		fireCountEvent();
        out.write(b);
	}
	
	@Override
	public void write(byte[] b) throws IOException {
		totalBytes+=b.length; 
		fireCountEvent();
        out.write(b);
	}
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		totalBytes+=len;
		fireCountEvent();
		out.write(b, off, len);
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
