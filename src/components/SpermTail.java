package components;

import ij.gui.Roi;

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
public class SpermTail extends Flagellum implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	

	public SpermTail(File source, int channel, Roi skeleton, Roi border){
		super(source, channel, skeleton, border);
	}
	
	

}
