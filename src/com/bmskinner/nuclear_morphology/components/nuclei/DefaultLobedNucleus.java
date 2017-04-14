package com.bmskinner.nuclear_morphology.components.nuclei;

import ij.gui.Roi;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.Lobe;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;

/**
 * The default implementation of the the LobedNucleus interface.
 * @author ben
 * @since 1.13.4
 *
 */public class DefaultLobedNucleus 
	extends DefaultNucleus 
	implements LobedNucleus {

	private static final long serialVersionUID = 1L;
		
	Set<Lobe> lobes = new HashSet<Lobe>();

	public DefaultLobedNucleus(Roi roi, IPoint centreOfMass, File f, int channel, int[] position, int number) {
		super(roi, centreOfMass, f, channel, position, number);
		
	}
	
	@Override
	public Set<Lobe> getLobes() {
		return lobes;
	}

	@Override
	public Set<IPoint> getLobeCoMs() {
		return lobes.stream().map( l -> l.getCentreOfMass()).collect(Collectors.toSet());
	}


	@Override
	public void addLobe(Lobe l) {
		if(l==null){
			throw new IllegalArgumentException("Lobe cannot be null");
		}
		lobes.add(l);
	}

	@Override
	public int getLobeCount() {
		return lobes.size();
	}
	
	@Override
	protected double calculateStatistic(PlottableStatistic stat){
		double result = super.calculateStatistic(stat);
		
		if(PlottableStatistic.LOBE_COUNT.equals(stat)){
			return lobes.size();
		}
				
		return result;

	}

	@Override
	public void removeAllLobes() {
		lobes.clear();
	}


}
