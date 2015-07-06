package components;

import java.io.File;
import java.io.Serializable;
import java.util.UUID;

/**
 * The sperm tail is a specialised type of flagellum. It is anchored at
 * the tail end of the sperm nucleus, and contains a midpiece (with mitochondria
 * attached) and a long thin tail. Cytoplasmic droplets may be present. Imaged
 * tails often overlap themselves and other tails. Common stain - anti-tubulin.
 * @author bms41
 *
 */
public class SpermTail implements Flagellum, Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private UUID uuid;
	
	protected File sourceFile;    // the image from which the tail came
	protected int sourceChannel; // the channel in the source image
	
	public SpermTail(File source, int channel){
		this.uuid = java.util.UUID.randomUUID();
		this.sourceFile = source;
		this.sourceChannel = channel;
	}

}
