/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
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

package com.bmskinner.nuclear_morphology.analysis.signals;

import java.util.List;

import com.bmskinner.nuclear_morphology.components.generic.Equation;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.nuclear.BorderPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalCollection;
import com.bmskinner.nuclear_morphology.components.nuclear.NuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.stats.NucleusStatistic;
import com.bmskinner.nuclear_morphology.components.stats.SignalStatistic;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * This carries out the calculations of signal positions and angles
 * within a nucleus
 * @author bms41
 *
 */
public class SignalAnalyser implements Loggable {
	
	public SignalAnalyser(){};
	
	 /*
		For each signal within the nucleus, calculate the distance to the nCoM
		and update the signal
	*/
	public void calculateSignalDistancesFromCoM(Nucleus n){

		for(List<INuclearSignal> signals : n.getSignalCollection().getSignals()){

			if(!signals.isEmpty()){
				for(INuclearSignal s : signals){
					double distance = n.getCentreOfMass().getLengthTo(s.getCentreOfMass());
					s.setStatistic(SignalStatistic.DISTANCE_FROM_COM, distance);
				}
			}
		}
	}

	/*
	Calculate the distance from the nuclear centre of
	mass as a fraction of the distance from the nuclear CoM, through the 
	signal CoM, to the nuclear border
	 */
	public void calculateFractionalSignalDistancesFromCoM(Nucleus n){

		ISignalCollection signalCollection = n.getSignalCollection();
		this.calculateClosestBorderToSignals(n);

		for(List<INuclearSignal> signals : signalCollection.getSignals()){

			if(!signals.isEmpty()){

				for(INuclearSignal signal : signals){

					// get the line equation
					Equation eq = new Equation(signal.getCentreOfMass(), n.getCentreOfMass());

					// using the equation, get the y postion on the line for each X point around the roi
					double minDeltaY = 100;
					int minDeltaYIndex = 0;
					double minDistanceToSignal = 1000;

					for(int j = 0; j<n.getBorderLength();j++){
						double x = n.getBorderPoint(j).getX();
						double y = n.getBorderPoint(j).getY();
						double yOnLine = eq.getY(x);
						double distanceToSignal = n.getBorderPoint(j).getLengthTo(signal.getCentreOfMass()); // fetch

						double deltaY = Math.abs(y - yOnLine);
						// find the point closest to the line; this could find either intersection
						// hence check it is as close as possible to the signal CoM also
						if(deltaY < minDeltaY && distanceToSignal < minDistanceToSignal){
							minDeltaY = deltaY;
							minDeltaYIndex = j;
							minDistanceToSignal = distanceToSignal;
						}
					}
					IBorderPoint borderPoint = n.getBorderPoint(minDeltaYIndex);
					double nucleusCoMToBorder = borderPoint.getLengthTo(n.getCentreOfMass());
					double signalCoMToNucleusCoM = n.getCentreOfMass().getLengthTo(signal.getCentreOfMass());
					double fractionalDistance = signalCoMToNucleusCoM / nucleusCoMToBorder;
					signal.setStatistic(SignalStatistic.FRACT_DISTANCE_FROM_COM, fractionalDistance);
				}
			}
		}
	}

	/*
	Go through the signals in the nucleus, and find the point on
	the nuclear ROI that is closest to the signal centre of mass.
	 */
	private void calculateClosestBorderToSignals(Nucleus n) {
		ISignalCollection signalCollection = n.getSignalCollection();
		for(List<INuclearSignal> signals : signalCollection.getSignals()){

			if(!signals.isEmpty()){

				for(INuclearSignal s : signals){

					int minIndex = 0;
					double minDistance = n.getStatistic(NucleusStatistic.MAX_FERET, MeasurementScale.PIXELS);

					for(int j = 0; j<n.getBorderLength();j++){
						IPoint p = n.getBorderPoint(j);
						double distance = p.getLengthTo(s.getCentreOfMass());

						// find the point closest to the CoM
						if(distance < minDistance){
							minIndex = j;
							minDistance = distance;
						}
					}
					s.setClosestBorderPoint(minIndex);

				}
			}
		}
	}



}
