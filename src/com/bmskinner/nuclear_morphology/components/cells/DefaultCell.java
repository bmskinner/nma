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
package com.bmskinner.nuclear_morphology.components.cells;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.Statistical;
import com.bmskinner.nuclear_morphology.components.Taggable;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.measure.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;

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
    protected ICytoplasm           cytoplasm = null;
    protected List<Nucleus>        nuclei;

    /** The statistical values stored for this object */
    private Map<Measurement, Double> statistics;

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

        if (c.hasCytoplasm())
            this.cytoplasm = c.getCytoplasm().duplicate();

        statistics = new HashMap<>();
        for(Measurement stat : c.getStatistics())
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
    public Nucleus getPrimaryNucleus() {
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
    public synchronized boolean hasStatistic(Measurement stat) {
        return statistics.containsKey(stat) && Statistical.STAT_NOT_CALCULATED != statistics.get(stat);
    }

    @Override
    public synchronized double getStatistic(Measurement stat) {
        return this.getStatistic(stat, MeasurementScale.PIXELS);
    }

    @Override
    public synchronized double getStatistic(Measurement stat, MeasurementScale scale) {

        // Get the scale of one of the components of the cell
        double sc = chooseScale();

        if (hasStatistic(stat)) {
            double result = statistics.get(stat);
            return stat.convert(result, sc, scale);
        }

        double result = calculateStatistic(stat);
        statistics.put(stat, result);
        return result;
    }
    
    private double chooseScale() {
    	
    	if(hasNucleus()) {
    		return getPrimaryNucleus().getScale();
    	}
    	if(hasCytoplasm()) {
    		return getCytoplasm().getScale();
    	}
    	
    	return 1d;
    }

    protected double calculateStatistic(Measurement stat) {

        if (stat == null)
            throw new IllegalArgumentException("Stat cannot be null");

        // Do not add getters for values added at creation time
        // or you'll get infinite loops when things break

        if (Measurement.CELL_NUCLEUS_COUNT.equals(stat))
            return nuclei.size();

        if (Measurement.CELL_NUCLEAR_AREA.equals(stat))
            return getNuclearArea();

        if (Measurement.CELL_NUCLEAR_RATIO.equals(stat))
            return getNuclearRatio();
        return STAT_NOT_CALCULATED;
    }

    @Override
    public void setStatistic(Measurement stat, double d) {
        if (Measurement.CELL_NUCLEUS_COUNT.equals(stat))
            statistics.put(stat, d);

        if (Measurement.CELL_NUCLEAR_AREA.equals(stat))
            statistics.put(stat, d);

        if (Measurement.CELL_NUCLEAR_RATIO.equals(stat)) 
            statistics.put(stat, d);
    }
    
    @Override
    public void clearStatistic(Measurement stat) {
    	statistics.remove(stat);
    }

    @Override
    public Measurement[] getStatistics() {
        return Measurement.getCellStats().toArray(new Measurement[0]);
    }

    private int getNuclearArea() {
        int i = 0;
        for (Nucleus n : nuclei) {
            i += n.getStatistic(Measurement.AREA);
        }
        return i;
    }

    private double getNuclearRatio() {
        if (hasCytoplasm()) {
            double cy = cytoplasm.getStatistic(Measurement.AREA);
            double n = getStatistic(Measurement.CELL_NUCLEAR_AREA);
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
    public boolean hasNucleus() {
        return !nuclei.isEmpty();
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
        List<Taggable> result = new ArrayList<>(0);
        result.addAll(getTaggables(nuclei));
        
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
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DefaultCell other = (DefaultCell) obj;
		return Objects.equals(cytoplasm, other.cytoplasm) && Objects.equals(nuclei, other.nuclei)
				&& Objects.equals(statistics, other.statistics) && Objects.equals(uuid, other.uuid);
	}

	@Override
	public int hashCode() {
		return Objects.hash(cytoplasm, nuclei, statistics, uuid);
	}
    
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }

}
