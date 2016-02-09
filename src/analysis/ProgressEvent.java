package analysis;

import java.util.EventObject;

@SuppressWarnings("serial")
public class ProgressEvent extends EventObject {

		/**
		 * Create an event from a source
		 */
		public ProgressEvent( Object source ) {
			super( source );
		}
}
