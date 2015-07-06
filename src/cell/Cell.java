package cell;

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
public interface Cell {

	public UUID getUuid();

	public Nucleus getNucleus();

	public void setNucleus(Nucleus nucleus);

	public Flagellum getTail();

	public void setTail(Flagellum tail);

	public List<Mitochondrion> getMitochondria();

	public void setMitochondria(List<Mitochondrion> mitochondria);
	
	public void addMitochondrion(Mitochondrion mitochondrion);
}
