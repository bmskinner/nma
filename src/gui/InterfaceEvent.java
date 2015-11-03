package gui;

import gui.DatasetEvent.DatasetMethod;

import java.util.EventObject;
import java.util.List;

import analysis.AnalysisDataset;

public class InterfaceEvent extends EventObject {
	
	private static final long serialVersionUID = 1L;
	private String sourceName;
	private InterfaceMethod method;
	
	/**
	 * Create an event from a source, with the given message
	 * @param source the source of the datasets
	 * @param message the instruction on what to do with the datasets
	 * @param sourceName the name of the object or component generating the datasets
	 * @param list the datasets to carry
	 */
	public InterfaceEvent( Object source, InterfaceMethod method, String sourceName ) {
		super( source );
		this.method = method;
		this.sourceName = sourceName;
	}
	
	/**
	 * The name of the component that fired the event
	 * @return
	 */
	public String sourceName(){
		return this.sourceName;
	}

	
	/**
	 * The message to carry
	 * @return
	 */
	public InterfaceMethod method() {
		return method;
	}
	
	public enum InterfaceMethod {
		
		UPDATE_POPULATIONS 	("Update populations"),
		UPDATE_PANELS		("Update panels"),
		REFRESH_POPULATIONS ("Refresh population panel datasets"),
		SAVE_ROOT			("Save root datasets");
		
		private final String name;
		
		InterfaceMethod(String name){
			this.name = name;
		}
		
		public String toString(){
			return this.name;
		}
		
		
	}
}
