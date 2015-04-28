package no.utility;

import java.io.File;

import ij.IJ;

public class Logger {

	public static final String ERROR = "ERROR";
	public static final String DEBUG = "DEBUG";
	public static final String INFO  = "INFO";
	public static final String STACK  = "STACK";
	
	private File file;
	private String className;
	
	public Logger(File f, String c){
		this.file = f;
		this.className = c;
	}
	
	/**
	 * Log a message with a given priority level
	 * @param s the message
	 * @param priority 
	 */
	public void log(String s, String priority){
		long time = System.currentTimeMillis();
		String result = IJ.append(time+"\t"+priority+"\t"+this.className+"\t"+s+"\r", file.getAbsolutePath());
		if(result!=null){
			IJ.log("Error in logging: "+file.getAbsolutePath());
		}
	}

	/**
	 * Log a message with assumption of info priority
	 * @param s the message
	 */
	public void log(String s){
		log(s, Logger.INFO);
	}
	
	public File getLogfile(){
		return this.file;
	}
}
