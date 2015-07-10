package cell;

import java.io.Serializable;
import java.util.ArrayList;
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
	protected List<Flagellum> tails;	
	
	public Cell(){
		this.uuid = java.util.UUID.randomUUID();
		mitochondria = new ArrayList<Mitochondrion>(0);
		tails = new ArrayList<Flagellum>(0);
	}
	
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
	
	public List<Flagellum> getTails(){
		return this.tails;
	}
	
	public Flagellum getTail(int i) {
		return tails.get(i);
	}

	public void addTail(Flagellum tail) {
		this.tails.add(tail);
	}
	
	public boolean hasNucleus(){
		if(this.nucleus!=null){
			return true;
		} else {
			return false;
		}
	}
	
	public boolean hasTail(){
		if(this.tails.size()>0){
			return true;
		} else {
			return false;
		}
	}
	
	public boolean hasMitochondria(){
		if(this.mitochondria.isEmpty()){
			return false;
		} else {
			return true;
		}
	}
}
