package cell;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import no.nuclei.Nucleus;
import components.Flagellum;
import components.Mitochondrion;

/**
 * The cell is the highest level of analysis here. Cells we can analyse
 * have a nucleus, mitochondria, and maybe a flagellum.
 * @author bms41
 *
 */
public class Cell implements Serializable {

	private static final long serialVersionUID = 1L;

	protected UUID uuid;
	
	protected Nucleus nucleus;
	protected List<Mitochondrion> mitochondria; // unknown staining patterns so far
	protected Flagellum tail;	
	
	public UUID getCellId() {
		return uuid;
	}

	public Nucleus getNucleus() {
		return nucleus;
	}

	public void setNucleus(Nucleus nucleus) {
		this.nucleus = nucleus;
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
	
	public Flagellum getTail() {
		return tail;
	}

	public void setTail(Flagellum tail) {
		this.tail = tail;
	}
}
