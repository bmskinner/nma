package charting.datasets;

import java.awt.Color;
import java.util.UUID;

/**
 * This allows a signal group UUID to be stored with the signal
 * group number and the signal colour for use in table formatting.
 * @author bms41
 *
 */
public class SignalTableCell {
	
	private UUID   id;
	private String name;
	
	public SignalTableCell(UUID id, String name) {
		this.id     = id;
		this.name   = name;
	}
	
	public UUID getID(){
		return id;
	}
		
	public String toString(){
		return name;
	}


}
