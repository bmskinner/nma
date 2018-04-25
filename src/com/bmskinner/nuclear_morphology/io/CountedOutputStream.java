package com.bmskinner.nuclear_morphology.io;

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
	
	protected CountedOutputStream(OutputStream out) {
		super(out);
	}
	
	@Override
	public void write(int b) throws IOException{
		totalBytes++; 
		fireCountEvent();
        out.write(b);;
	}
	
	@Override
	public void write(byte[] b) throws IOException {
		totalBytes+=b.length; 
		fireCountEvent();
        out.write(b);;
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
