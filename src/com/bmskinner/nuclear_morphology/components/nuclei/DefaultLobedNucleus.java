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
package com.bmskinner.nuclear_morphology.components.nuclei;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.Lobe;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;

import ij.gui.Roi;

/**
 * The default implementation of the the LobedNucleus interface.
 * 
 * @author ben
 * @since 1.13.4
 *
 */
public class DefaultLobedNucleus extends DefaultNucleus implements LobedNucleus {

    private static final long serialVersionUID = 1L;

    Set<Lobe> lobes = new HashSet<Lobe>();

    /**
     * Construct with an ROI, a source image and channel, and the original
     * position in the source image. It sets the immutable original centre of
     * mass, and the mutable current centre of mass. It also assigns a random ID
     * to the component.
     * 
     * @param roi the roi of the object
     * @param centerOfMass the original centre of mass of the component
     * @param source the image file the component was found in
     * @param channel the RGB channel the component was found in
     * @param position the bounding position of the component in the original image
     * @param id the id of the component. Only use when deserialising!
     */
    public DefaultLobedNucleus(@NonNull Roi roi, @NonNull IPoint centreOfMass, File source, int channel, int[] position, int number, @NonNull UUID id) {
        super(roi, centreOfMass, source, channel, position, number, id);
    }
    
    public DefaultLobedNucleus(Roi roi, IPoint centreOfMass, File f, int channel, int[] position, int number) {
        super(roi, centreOfMass, f, channel, position, number);

    }

    @Override
    public Set<Lobe> getLobes() {
        return lobes;
    }

    @Override
    public Set<IPoint> getLobeCoMs() {
        return lobes.stream().map(l -> l.getCentreOfMass()).collect(Collectors.toSet());
    }

    @Override
    public void addLobe(Lobe l) {
        if (l == null)
            throw new IllegalArgumentException("Lobe cannot be null");
        lobes.add(l);
    }

    @Override
    public int getLobeCount() {
        return lobes.size();
    }

    @Override
    protected double calculateStatistic(PlottableStatistic stat) {
        double result = super.calculateStatistic(stat);

        if (PlottableStatistic.LOBE_COUNT.equals(stat)) {
            return lobes.size();
        }

        return result;

    }

    @Override
    public void removeAllLobes() {
        lobes.clear();
    }

}
