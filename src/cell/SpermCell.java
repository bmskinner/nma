package cell;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import components.Flagellum;
import components.Mitochondrion;
import no.nuclei.Nucleus;

public class SpermCell implements Cell, Serializable {
	
	private static final long serialVersionUID = 1L;

	private UUID uuid;
	
	private Nucleus nucleus;
	private Flagellum tail;
	private List<Mitochondrion> mitochondria; // unknown staining patterns so far
	
	public SpermCell(){
		this.uuid = java.util.UUID.randomUUID();
	}

	public UUID getUuid() {
		return uuid;
	}

	public Nucleus getNucleus() {
		return nucleus;
	}

	public void setNucleus(Nucleus nucleus) {
		this.nucleus = nucleus;
	}

	public Flagellum getTail() {
		return tail;
	}

	public void setTail(Flagellum tail) {
		this.tail = tail;
	}

	public List<Mitochondrion> getMitochondria() {
		return mitochondria;
	}

	public void setMitochondria(List<Mitochondrion> mitochondria) {
		this.mitochondria = mitochondria;
	}
	
	public void addMitochondrion(Mitochondrion mitochondrion) {
		this.mitochondria.add(mitochondrion);
	}
	
	
}
