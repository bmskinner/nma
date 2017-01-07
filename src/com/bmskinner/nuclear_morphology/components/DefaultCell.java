/*******************************************************************************
 *  	Copyright (C) 2015, 2016 Ben Skinner
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
 *     GNU General Public License for more details. Gluten-free. May contain 
 *     traces of LDL asbestos. Avoid children using heavy machinery while under the
 *     influence of alcohol.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package com.bmskinner.nuclear_morphology.components;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;

/**
 * The cell is the highest level of analysis here. Cells we can analyse
 * have a nucleus, mitochondria, cytoplasm, and maybe a flagellum and acrosome.
 * @author bms41
 * @since 1.13.3
 */
public class DefaultCell 
	implements IMutableCell {

	private static final long serialVersionUID = 1L;

	protected UUID uuid;
	
	protected Nucleus nucleus = null;
	protected List<IMitochondrion> mitochondria; // unknown staining patterns so far
	protected List<Flagellum> tails;	
	protected List<IAcrosome> acrosomes;
	protected ICytoplasm cytoplasm = null;
	
	/**
	 * Create a new cell with a random ID
	 */
	protected DefaultCell(){
		this(java.util.UUID.randomUUID());
	}
	
	/**
	 * Create a new cell with the given ID.
	 * @param id the id for the new cell
	 */
	public DefaultCell(UUID id){
		this.uuid    = id;
		mitochondria = new ArrayList<IMitochondrion>(0);
		tails        = new ArrayList<Flagellum>(0);
		acrosomes    = new ArrayList<IAcrosome>(0);
	}
	
	/**
	 * Create a new cell based on the given nucleus. The 
	 * nucleus is NOT copied.
	 * @param n the template nucleus for the cell
	 */
	public DefaultCell(Nucleus n){
		this();
		nucleus = n;
	}
	
	/**
	 * Create a new cell based on the given nucleus. The 
	 * nucleus is NOT copied.
	 * @param n the template nucleus for the cell
	 */
	public DefaultCell(ICytoplasm c){
		this();
		cytoplasm = c;
	}
	
	/**
	 * Duplicate a cell. The ID is kept consistent
	 * @param c the cell to duplicate
	 */
	public DefaultCell(ICell c){

		this.uuid = c.getId();
//		try {
			nucleus   = c.getNucleus().duplicate();//new DefaultNucleus(c.getNucleus());
//		} catch (UnprofilableObjectException e) {
//			warn("Unable to make cell from template");
//			stack("Cannot profile the nucleus", e);
//		}
		
		mitochondria = new ArrayList<IMitochondrion>(0);
		for(IMitochondrion m : c.getMitochondria()){
			mitochondria.add(m.duplicate());
		}
		
		tails = new ArrayList<Flagellum>(0);
		for(Flagellum f : c.getFlagella()){
			tails.add(f.duplicate());
		}
		
		acrosomes = new ArrayList<IAcrosome>(0);
		for(IAcrosome a : c.getAcrosomes()){
			acrosomes.add(a.duplicate());
		}
		
		if(c.hasCytoplasm()){
			this.cytoplasm = c.getCytoplasm().duplicate();
		}
	}
	
	/* (non-Javadoc)
	 * @see components.ICell#getId()
	 */
	@Override
	public UUID getId() {
		return uuid;
	}

	/* (non-Javadoc)
	 * @see components.ICell#getNucleus()
	 */
	@Override
	public Nucleus getNucleus() {
		return nucleus;
	}
	
	/* (non-Javadoc)
	 * @see components.ICell#setNucleus(components.nuclei.Nucleus)
	 */
	@Override
	public void setNucleus(Nucleus nucleus) {
		this.nucleus = nucleus;
	}

	/* (non-Javadoc)
	 * @see components.ICell#getMitochondria()
	 */
	@Override
	public List<IMitochondrion> getMitochondria() {
		return mitochondria;
	}

	/* (non-Javadoc)
	 * @see components.ICell#setMitochondria(java.util.List)
	 */
	@Override
	public void setMitochondria(List<IMitochondrion> mitochondria) {
		this.mitochondria = mitochondria;
	}
	
	/* (non-Javadoc)
	 * @see components.ICell#addMitochondrion(components.Mitochondrion)
	 */
	@Override
	public void addMitochondrion(IMitochondrion mitochondrion) {
		this.mitochondria.add(mitochondrion);
	}
	
	/* (non-Javadoc)
	 * @see components.ICell#getTails()
	 */
	@Override
	public List<Flagellum> getFlagella(){
		return this.tails;
	}
	

	/* (non-Javadoc)
	 * @see components.ICell#addTail(components.Flagellum)
	 */
	@Override
	public void addFlagellum(Flagellum tail) {
		this.tails.add(tail);
	}
	
	/* (non-Javadoc)
	 * @see components.ICell#getAcrosomes()
	 */
	@Override
	public List<IAcrosome> getAcrosomes(){
		return this.acrosomes;
	}
	
	/* (non-Javadoc)
	 * @see components.ICell#addAcrosome(components.Acrosome)
	 */
	@Override
	public void addAcrosome(IAcrosome acrosome){
		this.acrosomes.add(acrosome);
	}
	
	/* (non-Javadoc)
	 * @see components.ICell#hasNucleus()
	 */
	@Override
	public boolean hasAcrosome(){
		return !this.acrosomes.isEmpty();
	}
	
	/* (non-Javadoc)
	 * @see components.ICell#hasNucleus()
	 */
	@Override
	public boolean hasNucleus(){
		return this.nucleus!=null;
	}
	
	/* (non-Javadoc)
	 * @see components.ICell#hasTail()
	 */
	@Override
	public boolean hasFlagellum(){
		return !this.tails.isEmpty();
	}
	
	/* (non-Javadoc)
	 * @see components.ICell#hasMitochondria()
	 */
	@Override
	public boolean hasMitochondria(){
		return !this.mitochondria.isEmpty();
	}
	
	@Override
	public ICytoplasm getCytoplasm() {
		return this.cytoplasm;
	}

	@Override
	public boolean hasCytoplasm() {
		return cytoplasm!=null;
	}

	@Override
	public void setCytoplasm(ICytoplasm cytoplasm) {
		this.cytoplasm = cytoplasm;
	}
	
	/* (non-Javadoc)
	 * @see components.ICell#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o){
		
		if(this==o){
			return true;
		}
		
		if(o==null){
			return false;
		}
		
		if (getClass() != o.getClass())
			return false;
		
		ICell other = (ICell) o;
		
		if(!other.getId().equals(this.getId())){
			return false;
		}
		
		return true;
	}
	
	
	/* (non-Javadoc)
	 * @see components.ICell#compareTo(components.Cell)
	 */
	@Override
	public int compareTo(ICell o) {
		
		if( ! this.hasNucleus()){
			return -1;
		}
		
		return this.nucleus.compareTo(o.getNucleus());
	}
	
	
	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
//		finest("\tWriting cell");
		out.defaultWriteObject();
//		finest("\tWrote cell");
	}

	/* (non-Javadoc)
	 * @see components.ICell#hashCode()
	 */
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
