package com.bmskinner.nuclear_morphology.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Count the number of bytes read in an input stream, and 
 * inform count listeners. Used for tracking opening of datasets
 * @author ben
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
