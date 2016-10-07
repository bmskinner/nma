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
package analysis.image;

import gui.components.ColourSelecter;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.util.List;
import java.util.UUID;

import utility.Constants;
import components.CellularComponent;
import components.generic.BorderTagObject;
import components.generic.ProfileType;
import components.generic.XYPoint;
import components.nuclear.NuclearSignal;
import components.nuclear.BorderPoint;
import components.nuclear.NucleusBorderSegment;
import components.nuclear.SignalCollection;
import components.nuclei.Nucleus;

public class NucleusAnnotator  extends AbstractImageFilterer {

	public NucleusAnnotator(ImageProcessor ip) {
		super(ip);
	}
		
	
	public NucleusAnnotator annotateNucleus(Nucleus n){
		
		try{

			annotateOP( n);
			annotateRP( n);
			annotateCoM( n);
			annotateMinFeret( n);
			annotateSegments( n);
			annotateSignals( n);

		}  catch(Exception e){
			error("Error annotating nucleus", e);

		} 
		return this;
	}
	
	private NucleusAnnotator annotatePoint(XYPoint p, Color c){
		ip.setColor(c);
		ip.setLineWidth(3);
		ip.drawDot( p.getXAsInt(), p.getYAsInt());
		return this;
	}
	
	private NucleusAnnotator annotateLine(XYPoint p1, XYPoint p2, Color c){
		ip.setColor(c);
		ip.setLineWidth(1);
		ip.drawLine(p1.getXAsInt(), p1.getYAsInt(), p2.getXAsInt(), p2.getYAsInt());
		return this;
	}
	
	private NucleusAnnotator annotatePolygon(PolygonRoi p, Color c){
		ip.setColor(c);
		ip.setLineWidth(2);
		ip.draw(p);
		return this;
	}
	

	public NucleusAnnotator annotateOP(Nucleus n){		
		return annotatePoint(n.getBorderPoint(BorderTagObject.ORIENTATION_POINT), Color.CYAN);
	}

	public NucleusAnnotator annotateRP(Nucleus n){
		return annotatePoint(n.getBorderPoint(BorderTagObject.REFERENCE_POINT), Color.YELLOW);
	}
	
	public NucleusAnnotator annotateCoM(CellularComponent n){
		return annotatePoint(n.getCentreOfMass(), Color.MAGENTA);
	}
	
	public NucleusAnnotator annotateBorder(CellularComponent n){
		FloatPolygon p = n.createPolygon();
		PolygonRoi roi = new PolygonRoi(p, PolygonRoi.POLYGON);
		return annotatePolygon(roi, Color.MAGENTA);
	}
	


	public NucleusAnnotator annotateMinFeret(Nucleus n){
		int minIndex = n.getProfile(ProfileType.DIAMETER).getIndexOfMin();
		BorderPoint narrow1 = n.getBorderPoint(minIndex);
		BorderPoint narrow2 = n.findOppositeBorder(narrow1);
		return annotateLine(narrow1, narrow2, Color.MAGENTA);
	}
	
	public NucleusAnnotator annotateSegments(Nucleus n){

		try{

			if(n.getProfile(ProfileType.ANGLE).getSegments().size()>0){ // only draw if there are segments
				for(int i=0;i<n.getProfile(ProfileType.ANGLE).getSegments().size();i++){

					NucleusBorderSegment seg = n.getProfile(ProfileType.ANGLE).getSegment("Seg_"+i);

					float[] xpoints = new float[seg.length()+1];
					float[] ypoints = new float[seg.length()+1];
					for(int j=0; j<=seg.length();j++){
						int k = n.wrapIndex(seg.getStartIndex()+j);
						BorderPoint p = n.getBorderPoint(k); // get the border points in the segment
						xpoints[j] = (float) p.getX();
						ypoints[j] = (float) p.getY();
					}

					PolygonRoi segRoi = new PolygonRoi(xpoints, ypoints, Roi.POLYLINE);


					Color color = ColourSelecter.getColor(i);

					annotatePolygon(segRoi, color);
				}
			}
		} catch(Exception e){
			error("Error annotating segments", e);
		}
		return this;
	}
	
	
	public NucleusAnnotator annotateSignals(Nucleus n){

		SignalCollection signalCollection = n.getSignalCollection();
		for( UUID id : signalCollection.getSignalGroupIDs()){
			int i = signalCollection.getSignalGroupNumber(id);
			List<NuclearSignal> signals = signalCollection.getSignals(id);
			Color colour = i==Constants.FIRST_SIGNAL_CHANNEL 
						 ? Color.RED 
						 : i==Constants.FIRST_SIGNAL_CHANNEL+1 
						 	? Color.GREEN 
						 	: Color.WHITE;
			


			if(!signals.isEmpty()){

				for(NuclearSignal s : signals){
					
					annotatePoint(s.getCentreOfMass(), colour);
					
					FloatPolygon p = s.createPolygon();
					PolygonRoi roi = new PolygonRoi(p, PolygonRoi.POLYGON);
					annotatePolygon(roi, colour);
				}
			}
		}
		return this;
	}
}
