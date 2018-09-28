/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.components.nuclei;

import java.awt.Rectangle;
import java.io.IOException;
import java.io.Serializable;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileCreator;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;

import ij.process.FloatPolygon;

/**
 * This holds methods for manipulatiing a refolded consensus nucleus
 *
 */
@Deprecated
public class ConsensusNucleus extends RoundNucleus implements Serializable {

    private static final long serialVersionUID = 1L;

    private NucleusType type;

    public ConsensusNucleus(Nucleus n, NucleusType type) {

        super(n);
        this.type = type;
    }

    public NucleusType getType() {
        return this.type;
    }

    @Override
    public int[] getPosition() {
        Rectangle bounds = getVerticallyRotatedNucleus().toPolygon().getBounds();
        int newWidth = (int) bounds.getWidth();
        int newHeight = (int) bounds.getHeight();
        int newX = (int) bounds.getX();
        int newY = (int) bounds.getY();

        int[] newPosition = { newX, newY, newWidth, newHeight };
        return newPosition;
    }

    @Override
    public void calculateProfiles() throws ProfileException {

        /*
         * The CurveRefolder currently only uses the angle profile so ignore the
         * others to speed refolding
         */
        ProfileCreator creator = new ProfileCreator(this);

        ISegmentedProfile profile = creator.createProfile(ProfileType.ANGLE);

        profileMap.put(ProfileType.ANGLE, profile);

    }

    @Override
    public FloatPolygon toOriginalPolygon() {
        return this.toPolygon();
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ConsensusNucleus other = (ConsensusNucleus) obj;
        if (type != other.type)
            return false;
        return true;
    }

}
