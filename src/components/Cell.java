/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package components;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import logging.Loggable;
import components.nuclei.Nucleus;

/**
 * The cell is the highest level of analysis here. Cells we can analyse
 * have a nucleus, mitochondria, and maybe a flagellum.
 * @author bms41
 *
 */
public class Cell implements Serializable, Loggable {

	private static final long serialVersionUID = 1L;

	protected UUID uuid;
	
	protected Nucleus nucleus;
	protected List<Mitochondrion> mitochondria; // unknown staining patterns so far
	protected List<Flagellum> tails;	
	protected List<Acrosome> acrosomes;
	
	public Cell(){
		this.uuid    = java.util.UUID.randomUUID();
		mitochondria = new ArrayList<Mitochondrion>(0);
		tails        = new ArrayList<Flagellum>(0);
		acrosomes    = new ArrayList<Acrosome>(0);
	}
	
	/**
	 * Duplicate a cell. The ID is kept consistent
	 * @param c the cell to duplicate
	 */
	public Cell(Cell c){

		this.uuid = c.getId();
		nucleus   = c.getNucleus().duplicate();
		
		mitochondria = new ArrayList<Mitochondrion>(0);
		for(Mitochondrion m : c.getMitochondria()){
			mitochondria.add(new Mitochondrion(m));
		}
		
		tails = new ArrayList<Flagellum>(0);
		for(Flagellum f : c.getTails()){
			tails.add(new SpermTail((SpermTail) f));
		}
		
		acrosomes = new ArrayList<Acrosome>(0);
		for(Acrosome a : c.getAcrosomes()){
			acrosomes.add(new Acrosome(a));
		}
	}
	
	public UUID getId() {
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
	
	public List<Acrosome> getAcrosomes(){
		return this.acrosomes;
	}
	
	public void addAcrosome(Acrosome acrosome){
		this.acrosomes.add(acrosome);
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
	
	/**
	 * Compare cells using the UUID only
	 * @param c
	 * @return
	 */
	public boolean equals(Object o){
		
		if(this==o){
			return true;
		}
		
		if(o==null){
			return false;
		}
		
		if (getClass() != o.getClass())
			return false;
		
		Cell other = (Cell) o;
		
		if(!other.getId().equals(this.getId())){
			return false;
		}
		
		return true;
	}
	
	
	
	
	
	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
//		finest("\tWriting cell");
		out.defaultWriteObject();
//		finest("\tWrote cell");
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((acrosomes == null) ? 0 : acrosomes.hashCode());
		result = prime * result
				+ ((mitochondria == null) ? 0 : mitochondria.hashCode());
		result = prime * result + ((nucleus == null) ? 0 : nucleus.hashCode());
		result = prime * result + ((tails == null) ? 0 : tails.hashCode());
		result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
		return result;
	}

//	@Override
//	public boolean equals(Object obj) {
//		if (this == obj)
//			return true;
//		if (obj == null)
//			return false;
//		if (getClass() != obj.getClass())
//			return false;
//		Cell other = (Cell) obj;
//		if (acrosomes == null) {
//			if (other.acrosomes != null)
//				return false;
//		} else if (!acrosomes.equals(other.acrosomes))
//			return false;
//		if (mitochondria == null) {
//			if (other.mitochondria != null)
//				return false;
//		} else if (!mitochondria.equals(other.mitochondria))
//			return false;
//		if (nucleus == null) {
//			if (other.nucleus != null)
//				return false;
//		} else if (!nucleus.equals(other.nucleus))
//			return false;
//		if (tails == null) {
//			if (other.tails != null)
//				return false;
//		} else if (!tails.equals(other.tails))
//			return false;
//		if (uuid == null) {
//			if (other.uuid != null)
//				return false;
//		} else if (!uuid.equals(other.uuid))
//			return false;
//		return true;
//	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
//		finest("Reading cell");
		in.defaultReadObject();
//		finest("Read cell"); 
	}
}
