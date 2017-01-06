package com.bmskinner.nuclear_morphology.components.nuclei;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import com.bmskinner.nuclear_morphology.components.generic.IPoint;

import ij.gui.Roi;

/**
 * The default implementation of the the LobedNucleus interface.
 * @author ben
 * @since 1.13.4
 *
 */public class DefaultLobedNucleus 
	extends DefaultNucleus 
	implements LobedNucleus {

	private static final long serialVersionUID = 1L;
	
	
	Set<IPoint> lobeCoMs = new HashSet<IPoint>();

	public DefaultLobedNucleus(Roi roi, IPoint centreOfMass, File f, int channel, int[] position, int number) {
		super(roi, centreOfMass, f, channel, position, number);
		
	}

	@Override
	public Set<IPoint> getLobeCoMs() {
		return lobeCoMs;
	}

	@Override
	public int getLobeCount() {
		return lobeCoMs.size();
	}

	@Override
	public void addLobe(IPoint com) {
		lobeCoMs.add(com);
	}
	
	

}
