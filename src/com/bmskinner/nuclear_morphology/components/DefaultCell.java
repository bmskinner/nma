/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nuclear_morphology.components;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;

/**
 * The cell is the highest level of analysis here. Cells we can analyse have a
 * nucleus, mitochondria, cytoplasm, and maybe a flagellum and acrosome.
 * 
 * @author bms41
 * @since 1.13.3
 */
public class DefaultCell implements ICell {

    private static final long serialVersionUID = 1L;

    protected UUID uuid;

    @Deprecated
    protected volatile Nucleus              nucleus   = null;// use the list
    
    protected volatile List<IMitochondrion> mitochondria;
    protected volatile List<Flagellum>      tails;
    protected volatile List<IAcrosome>      acrosomes;
    protected volatile ICytoplasm           cytoplasm = null;
    protected volatile List<Nucleus>        nuclei;

    /** The statistical values stored for this object */
    private volatile Map<PlottableStatistic, Double> statistics;

    /**
     * Create a new cell with a random ID
     */
    protected DefaultCell() {
        this(java.util.UUID.randomUUID());
    }

    /**
     * Create a new cell with the given ID.
     * 
     * @param id the id for the new cell
     */
    public DefaultCell(@NonNull UUID id) {
        this.uuid = id;
        nuclei = new ArrayList<>(0);
        mitochondria = new ArrayList<>(0);
        tails = new ArrayList<>(0);
        acrosomes = new ArrayList<>(0);
        statistics = new HashMap<>();
    }

    /**
     * Create a new cell based on the given nucleus. The nucleus is NOT copied.
     * 
     * @param n the template nucleus for the cell
     */
    public DefaultCell(Nucleus n) {
        this();
        nuclei.add(n);
    }

    /**
     * Create a new cell based on the given nucleus. The nucleus is NOT copied.
     * 
     * @param n
     *            the template nucleus for the cell
     */
    public DefaultCell(ICytoplasm c) {
        this();
        cytoplasm = c;
    }

    /**
     * Duplicate a cell. The ID is kept consistent
     * 
     * @param c the cell to duplicate
     */
    public DefaultCell(ICell c) {

        this.uuid = c.getId();

        nuclei = new ArrayList<>(0);
        for (Nucleus m : c.getNuclei()) {
            nuclei.add(m.duplicate());
        }

        mitochondria = new ArrayList<>(0);
        for (IMitochondrion m : c.getMitochondria()) {
            mitochondria.add(m.duplicate());
        }

        tails = new ArrayList<>(0);
        for (Flagellum f : c.getFlagella()) {
            tails.add(f.duplicate());
        }

        acrosomes = new ArrayList<>(0);
        for (IAcrosome a : c.getAcrosomes()) {
            acrosomes.add(a.duplicate());
        }

        if (c.hasCytoplasm())
            this.cytoplasm = c.getCytoplasm().duplicate();

        statistics = new HashMap<>();
        for(PlottableStatistic stat : c.getStatistics())
        	statistics.put(stat, c.getStatistic(stat));
    }
    
    @Override
    public ICell duplicate() {
    	return new DefaultCell(this);
    }

    @Override
    public @NonNull UUID getId() {
        return uuid;
    }

    @Override
    public Nucleus getNucleus() {
        return nuclei.get(0);
    }

    @Override
	public List<Nucleus> getNuclei() {
        return nuclei;
    }

    /*
     * 
     * METHODS IMPLEMENTING THE STATISTICAL INTERFACE
     * 
     */

    @Override
    public synchronized boolean hasStatistic(PlottableStatistic stat) {
        return statistics.containsKey(stat) && Statistical.STAT_NOT_CALCULATED != statistics.get(stat);
    }

    @Override
    public synchronized double getStatistic(PlottableStatistic stat) {
        return this.getStatistic(stat, MeasurementScale.PIXELS);
    }

    @Override
    public synchronized double getStatistic(PlottableStatistic stat, MeasurementScale scale) {

        // Get the scale of one of the components of the cell
        double sc = this.hasNucleus() ? this.getNucleus().getScale()
                : this.hasCytoplasm() ? this.getCytoplasm().getScale() : 1d;

        if (hasStatistic(stat)) {
            double result = statistics.get(stat);
            return stat.convert(result, sc, scale);
        }

        double result = calculateStatistic(stat);
        statistics.put(stat, result);
        return result;


    }

    protected double calculateStatistic(PlottableStatistic stat) {

        if (stat == null)
            throw new IllegalArgumentException("Stat cannot be null");

        // Do not add getters for values added at creation time
        // or you'll get infinite loops when things break

        if (PlottableStatistic.CELL_NUCLEUS_COUNT.equals(stat))
            return nuclei.size();

        if (PlottableStatistic.LOBE_COUNT.equals(stat))
            return getLobeCount();

        if (PlottableStatistic.CELL_NUCLEAR_AREA.equals(stat))
            return getNuclearArea();

        if (PlottableStatistic.CELL_NUCLEAR_RATIO.equals(stat))
            return getNuclearRatio();
        return STAT_NOT_CALCULATED;
    }

    @Override
    public void setStatistic(PlottableStatistic stat, double d) {
        if (PlottableStatistic.CELL_NUCLEUS_COUNT.equals(stat))
            statistics.put(stat, d);

        if (PlottableStatistic.LOBE_COUNT.equals(stat))
            statistics.put(stat, d);

        if (PlottableStatistic.CELL_NUCLEAR_AREA.equals(stat))
            statistics.put(stat, d);

        if (PlottableStatistic.CELL_NUCLEAR_RATIO.equals(stat)) 
            statistics.put(stat, d);
    }

    @Override
    public PlottableStatistic[] getStatistics() {
        return PlottableStatistic.getCellStats().toArray(new PlottableStatistic[0]);
    }

    private int getLobeCount() {
        return (int) getNuclei().stream()
        		.mapToDouble(n -> n.getStatistic(PlottableStatistic.LOBE_COUNT))
        		.sum();
    }

    private int getNuclearArea() {
        int i = 0;
        for (Nucleus n : nuclei) {
            i += n.getStatistic(PlottableStatistic.AREA);
        }
        return i;
    }

    private double getNuclearRatio() {
        if (hasCytoplasm()) {
            double cy = cytoplasm.getStatistic(PlottableStatistic.AREA);
            double n = getStatistic(PlottableStatistic.CELL_NUCLEAR_AREA);
            return n / cy;
        }
        return STAT_NOT_CALCULATED;
    }

    /*
     * 
     * METHODS IMPLEMENTING THE ICELL INTERFACE
     * 
     */

    @Override
    public void setNucleus(Nucleus nucleus) {
        if (nuclei.isEmpty()) {
            nuclei.add(nucleus);
        } else {
            nuclei.set(0, nucleus);
        }
    }

    @Override
    public void addNucleus(Nucleus nucleus) {
        nuclei.add(nucleus);
    }

    @Override
    public List<IMitochondrion> getMitochondria() {
        return mitochondria;
    }

    @Override
    public void setMitochondria(List<IMitochondrion> mitochondria) {
        this.mitochondria = mitochondria;
    }

    @Override
    public void addMitochondrion(IMitochondrion mitochondrion) {
        this.mitochondria.add(mitochondrion);
    }

    @Override
    public List<Flagellum> getFlagella() {
        return this.tails;
    }

    @Override
    public void addFlagellum(Flagellum tail) {
        this.tails.add(tail);
    }

    @Override
    public List<IAcrosome> getAcrosomes() {
        return this.acrosomes;
    }

    @Override
    public void addAcrosome(IAcrosome acrosome) {
        this.acrosomes.add(acrosome);
    }

    @Override
    public boolean hasAcrosome() {
        return !this.acrosomes.isEmpty();
    }

    @Override
    public boolean hasNucleus() {
        return !nuclei.isEmpty();
    }

    @Override
    public boolean hasFlagellum() {
        return !this.tails.isEmpty();
    }

    @Override
    public boolean hasMitochondria() {
        return !this.mitochondria.isEmpty();
    }

    @Override
    public ICytoplasm getCytoplasm() {
        return this.cytoplasm;
    }

    @Override
    public boolean hasCytoplasm() {
        return cytoplasm != null;
    }

    @Override
    public void setCytoplasm(ICytoplasm cytoplasm) {
        this.cytoplasm = cytoplasm;
    }
    
    
    @Override
    public List<Taggable> getTaggables() {
        List<Taggable> result = new ArrayList<Taggable>(0);
        
        result.addAll(getTaggables(acrosomes));
        result.addAll(getTaggables(mitochondria));
        result.addAll(getTaggables(nuclei));
        result.addAll(getTaggables(tails));
        
        if(hasCytoplasm()){
            if(cytoplasm instanceof Taggable){
                result.add((Taggable) cytoplasm);
            }
        }
        
        return result;

    }
    
    private List<Taggable> getTaggables(List<? extends CellularComponent> l){
        return l.stream().filter(e-> e instanceof Taggable)
        .map( e->(Taggable)e)
        .collect(Collectors.toList());
    }
    
    @Override
    public void setScale(double scale) {
        nuclei.stream().forEach(n->n.setScale(scale));
        tails.stream().forEach(n->n.setScale(scale));
        acrosomes.stream().forEach(n->n.setScale(scale));
        mitochondria.stream().forEach(n->n.setScale(scale));
        if(cytoplasm!=null)
        	cytoplasm.setScale(scale);
    }
    
    @Override
    public boolean hasNuclearSignals(){
        return getNuclei().stream().anyMatch(n->n.getSignalCollection().hasSignal());
    }
    
    @Override
    public boolean hasNuclearSignals(UUID signalGroupId){
        return getNuclei().stream().anyMatch(n->n.getSignalCollection().hasSignal(signalGroupId));
    }
        
    @Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DefaultCell other = (DefaultCell) obj;
		if (acrosomes == null) {
			if (other.acrosomes != null)
				return false;
		} else if (!acrosomes.equals(other.acrosomes))
			return false;
		if (cytoplasm == null) {
			if (other.cytoplasm != null)
				return false;
		} else if (!cytoplasm.equals(other.cytoplasm))
			return false;
		if (mitochondria == null) {
			if (other.mitochondria != null)
				return false;
		} else if (!mitochondria.equals(other.mitochondria))
			return false;
		if (nuclei == null) {
			if (other.nuclei != null)
				return false;
		} else if (!nuclei.equals(other.nuclei))
			return false;
		if (statistics == null) {
			if (other.statistics != null)
				return false;
		} else if (!statistics.equals(other.statistics))
			return false;
		if (tails == null) {
			if (other.tails != null)
				return false;
		} else if (!tails.equals(other.tails))
			return false;
		if (uuid == null) {
			if (other.uuid != null)
				return false;
		} else if (!uuid.equals(other.uuid))
			return false;
		return true;
	}

    /*
     * (non-Javadoc)
     * 
     * @see components.ICell#compareTo(components.Cell)
     */
    @Override
    public int compareTo(ICell o) {

        if (!this.hasNucleus())
            return -1;

        // If different number of nuclei
        if(this.getNuclei().size()!=o.getNuclei().size())
            return this.getNuclei().size() - o.getNuclei().size();
        
        int val = 0;
        List<Nucleus> other = o.getNuclei();
        
        for(int i=0; i<other.size(); i++){
            val += this.getNuclei().get(i).compareTo(other.get(i));
        }

        return val;
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }
    
    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((acrosomes == null) ? 0 : acrosomes.hashCode());
		result = prime * result + ((cytoplasm == null) ? 0 : cytoplasm.hashCode());
		result = prime * result + ((mitochondria == null) ? 0 : mitochondria.hashCode());
		result = prime * result + ((nuclei == null) ? 0 : nuclei.hashCode());
		result = prime * result + ((statistics == null) ? 0 : statistics.hashCode());
		result = prime * result + ((tails == null) ? 0 : tails.hashCode());
		result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
		return result;
	}
    
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        // Replacce old single nucleus fields
        if (nuclei == null) {
            nuclei = new ArrayList<>(0);
            nuclei.add(nucleus);
        }

        // Add stats if missing
        if (statistics == null)
            statistics = new HashMap<>();
    }

}
