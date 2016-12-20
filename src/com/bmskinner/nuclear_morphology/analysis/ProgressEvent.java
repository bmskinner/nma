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
package com.bmskinner.nuclear_morphology.analysis;

import java.util.EventObject;

/**
 * ProgressEvents are used to signal a JProgressBar should be incremented.
 * They are used in RecursiveActions and ForkJoinTasks, where a direct signal
 * to an encompassing ProgressableAction is not possible.
 * @author bms41
 * @since 1.12.0
 *
 */
@SuppressWarnings("serial")
public class ProgressEvent extends EventObject {
	
		public static final int NO_MESSAGE = 0;
		public static final int SET_TOTAL_PROGRESS = 1;
		public static final int SET_INDETERMINATE  = 2;
		
		private int message = NO_MESSAGE;
		private int value;

		/**
		 * Create an event from a source
		 */
		public ProgressEvent( Object source ) {
			super( source );
		}
		
		/**
		 * Create from a source, including a message type and value.
		 * This allows - for example -  the total progress bar length to
		 * be altered based on a computation in an IAnalysisMethod 
		 * @param source the source firing the event
		 * @param m the messsage type
		 * @param v the value in the message
		 */
		public ProgressEvent( Object source, int m, int v ) {
			super( source );
			message = m;
			value = v;
		}
		
		public int getMessage(){
			return message;
		}
		
		public int getValue(){
			return value;
		}
		
		public boolean hasMessage(){
			return message !=NO_MESSAGE;
		}
}
