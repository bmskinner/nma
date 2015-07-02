package components;

import java.io.Serializable;
import java.util.UUID;

public class Mitochondrion implements Serializable {

	private static final long serialVersionUID = 1L;
	private UUID uuid;
	
	public Mitochondrion(){
		this.uuid = java.util.UUID.randomUUID();
	}
}
