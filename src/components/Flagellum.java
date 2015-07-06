package components;

import java.io.File;
import java.util.UUID;

/**
 * There can be many types of flagellum; the type of interest mainly
 * is the sperm tail.
 * @author bms41
 *
 */
public class Flagellum {
	
	private static final long serialVersionUID = 1L;
	
	protected UUID uuid;
	
	protected File sourceFile;    // the image from which the tail came
	protected int sourceChannel; // the channel in the source image
	
	public Flagellum(File source, int channel){
		this.uuid = java.util.UUID.randomUUID();
		this.sourceFile = source;
		this.sourceChannel = channel;
	}

}
