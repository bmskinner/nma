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
/*
  -----------------------
  ASYMMETRIC NUCLEUS CLASS
  -----------------------
  Contains the variables for storing a non-circular nucleus.
  They have a head and a tail, hence can be oriented
  in one axis.

  A tail is the point determined via profile analysis. The
  head is assigned as the point opposite through the CoM.
*/
package com.bmskinner.nuclear_morphology.components.nuclei;

import ij.gui.Roi;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;

@Deprecated
public class AsymmetricNucleus extends RoundNucleus {

    private static final long            serialVersionUID   = 1L;
    private transient List<IBorderPoint> tailEstimatePoints = new ArrayList<IBorderPoint>(0); // holds
                                                                                              // the
                                                                                              // points
                                                                                              // considered
                                                                                              // to
                                                                                              // be
                                                                                              // sperm
                                                                                              // tails
                                                                                              // before
                                                                                              // filtering
    protected transient boolean          clockwiseRP        = false;                          // is
                                                                                              // the
                                                                                              // original
                                                                                              // orientation
                                                                                              // of
                                                                                              // the
                                                                                              // nucleus
                                                                                              // with
                                                                                              // RP
                                                                                              // clockwise
                                                                                              // to
                                                                                              // the
                                                                                              // CoM,
                                                                                              // or
                                                                                              // not

    public AsymmetricNucleus(Nucleus n) {
        super(n);
    }

    protected AsymmetricNucleus() {
        super();
    }

    // public AsymmetricNucleus (Roi roi, File file, int number, int[] position)
    // { // construct from an roi
    // super(roi, file, number, position);
    // }

    public AsymmetricNucleus(Roi roi, File file, int number, int[] position, IPoint centreOfMass) { // construct
                                                                                                    // from
                                                                                                    // an
                                                                                                    // roi
        super(roi, file, number, position, centreOfMass);
    }

    @Override
    public Nucleus duplicate() {
        AsymmetricNucleus duplicate = new AsymmetricNucleus(this);
        return duplicate;
    }

    /*
     * ----------------------- Get nucleus features -----------------------
     */

    public List<IBorderPoint> getEstimatedTailPoints() {
        return this.tailEstimatePoints;
    }

    /*
     * ----------------------- Set nucleus features -----------------------
     */
    @Override
    public boolean isClockwiseRP() {
        return this.clockwiseRP;
    }

    protected void addTailEstimatePosition(IBorderPoint p) {
        this.tailEstimatePoints.add(p);
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        // finest("\tReading asymmetric nucleus");
        in.defaultReadObject();
        tailEstimatePoints = new ArrayList<IBorderPoint>(0);
        clockwiseRP = false;
        // finest("\tRead asymmetric nucleus");
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        // finest("\tWriting asymmetric nucleus");
        out.defaultWriteObject();
        // finest("\tWrote asymmetric nucleus");
    }
}