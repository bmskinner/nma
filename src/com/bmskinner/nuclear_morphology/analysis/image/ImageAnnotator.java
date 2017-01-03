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
package com.bmskinner.nuclear_morphology.analysis.image;

import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.text.DecimalFormat;
import java.util.List;
import java.util.UUID;

import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalCollection;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.stats.NucleusStatistic;
import com.bmskinner.nuclear_morphology.components.stats.SignalStatistic;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;
import com.bmskinner.nuclear_morphology.io.ImageImporter;

public class ImageAnnotator  extends AbstractImageFilterer {

	public ImageAnnotator(ImageProcessor ip) {
		super(ip);
	}
		
	
	public ImageAnnotator annotateNucleus(Nucleus n){
		
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
	
	private ImageAnnotator annotatePoint(IPoint p, Color c){
		ip.setColor(c);
		ip.setLineWidth(3);
		ip.drawDot( p.getXAsInt(), p.getYAsInt());
		return this;
	}
	
	private ImageAnnotator annotateLine(IPoint p1, IPoint p2, Color c){
		ip.setColor(c);
		ip.setLineWidth(1);
		ip.drawLine(p1.getXAsInt(), p1.getYAsInt(), p2.getXAsInt(), p2.getYAsInt());
		return this;
	}
	
	private ImageAnnotator annotatePolygon(PolygonRoi p, Paint c){
		ip.setColor((Color) c);
		ip.setLineWidth(2);
		ip.draw(p);
		return this;
	}
	

	public ImageAnnotator annotateOP(Nucleus n){		
		try {
			return annotatePoint(n.getBorderPoint(Tag.ORIENTATION_POINT), Color.CYAN);
		} catch (UnavailableBorderTagException e) {
			fine("Cannot get OP index");
			return this;
		}
	}

	public ImageAnnotator annotateRP(Nucleus n){
		try {
			return annotatePoint(n.getBorderPoint(Tag.REFERENCE_POINT), Color.YELLOW);
		} catch (UnavailableBorderTagException e) {
			fine("Cannot get RP index");
			return this;
		}
	}
	
	public ImageAnnotator annotateCoM(CellularComponent n){
		return annotatePoint(n.getCentreOfMass(), Color.MAGENTA);
	}
	
	/**
	 * Draw the outline of the component in the given colour
	 * @param n the component
	 * @param c the colour
	 * @return
	 */
	public ImageAnnotator annotateBorder(CellularComponent n, Color c){
		FloatPolygon p = n.createOriginalPolygon();
		PolygonRoi roi = new PolygonRoi(p, PolygonRoi.POLYGON);
		
		return annotatePolygon(roi, c);
	}
	
	
	
	/**
	 * Annotate the image with the given text. The text background is white and the
	 * text colour is black
	 * @param x the string x
	 * @param y the string y
	 * @param s the text
	 * @return
	 */
	public ImageAnnotator annotateString(int x, int y, String s){
		return this.annotateString(x, y, s, Color.BLACK);
	}
	
	/**
	 * Annotate the image with the given text. The text background is white.
	 * @param x the string x
	 * @param y the string y
	 * @param s the text
	 * @param text the text colour
	 * @return
	 */
	public ImageAnnotator annotateString(int x, int y, String s, Color text){
		return this.annotateString(x, y, s, text, Color.WHITE);
	}
	
	/**
	 * Annotate the image with the given text.
	 * @param x the string x
	 * @param y the string y
	 * @param s the text
	 * @param text the text colour
	 * @param back the backgound colour
	 * @return
	 */
	public ImageAnnotator annotateString(int x, int y, String s, Color text, Color back){
		
		ip.setFont(new Font("SansSerif", Font.PLAIN, 20)); //TODO - choose text size based on image size
		ip.setColor(text);
		ip.drawString(s, 
				x,
				y, 
				back);
		return this;
	}
	
	/**
	 * Draw the size and shape values over the CoM of the component
	 * @param n the component to draw
	 * @param c the color of the text
	 * @return
	 */
	public ImageAnnotator annotateStats(CellularComponent n, Color text, Color back){
		
		DecimalFormat df = new DecimalFormat("#.##");
		
		String areaLbl;
		String perimLbl;
		
		double circ;
		double area;
		
		if(n instanceof INuclearSignal){

			area = n.getStatistic(SignalStatistic.AREA);
			double perim2 = Math.pow(n.getStatistic(SignalStatistic.PERIMETER), 2);
			circ = (4 * Math.PI) * (area / perim2);

		} else {
			area =  n.getStatistic(NucleusStatistic.AREA);
			circ =  n.getStatistic(NucleusStatistic.CIRCULARITY);
		}
		
		areaLbl  = "Area: " + df.format( area);
		perimLbl = "Circ: " + df.format( circ);
		String label = areaLbl + "\n" + perimLbl;
		
		return this.annotateString(n.getOriginalCentreOfMass().getXAsInt(),
				n.getOriginalCentreOfMass().getYAsInt(), 
				label, text, back);
	}
	
	/**
	 * Draw the size and shape values over the CoM of the component
	 * @param n the component to draw
	 * @param c the color of the text
	 * @return
	 */
	public ImageAnnotator annotateSignalStats(CellularComponent parent, CellularComponent signal, Color text, Color back){
		
		DecimalFormat df = new DecimalFormat("#.##");
		
		String areaLbl;
		String perimLbl;
		
		double circ = 0;
		double area = 0;
		double fraction;
		
		if(signal instanceof INuclearSignal){

			area = signal.getStatistic(SignalStatistic.AREA);
			double perim2 = Math.pow(signal.getStatistic(SignalStatistic.PERIMETER), 2);
			circ = (4 * Math.PI) * (area / perim2);

		} 
		
		fraction  = area / parent.getStatistic(NucleusStatistic.AREA);
		
		areaLbl  = "Area: " + df.format( area);
		perimLbl = "Circ: " + df.format( circ);
		String fractLabel = "Fract: " + df.format( fraction);

		String label = areaLbl + "\n" + perimLbl + "\n" + fractLabel;
		
		return this.annotateString(signal.getOriginalCentreOfMass().getXAsInt(),
				signal.getOriginalCentreOfMass().getYAsInt(), 
				label, text, back);

	}
	


	public ImageAnnotator annotateMinFeret(Nucleus n){
		int minIndex;
		try {
			minIndex = n.getProfile(ProfileType.DIAMETER).getIndexOfMin();
			IBorderPoint narrow1 = n.getBorderPoint(minIndex);
			IBorderPoint narrow2 = n.findOppositeBorder(narrow1);
			return annotateLine(narrow1, narrow2, Color.MAGENTA);
		} catch (UnavailableProfileTypeException e) {
			fine("Unable to get diameter profile", e);
			return this;
		}
		
		
	}
	
	public ImageAnnotator annotateSegments(Nucleus n){

		try{

			if(n.getProfile(ProfileType.ANGLE).getSegments().size()>0){ // only draw if there are segments
				for(int i=0;i<n.getProfile(ProfileType.ANGLE).getSegments().size();i++){

					IBorderSegment seg = n.getProfile(ProfileType.ANGLE).getSegment("Seg_"+i);

					float[] xpoints = new float[seg.length()+1];
					float[] ypoints = new float[seg.length()+1];
					for(int j=0; j<=seg.length();j++){
						int k = n.wrapIndex(seg.getStartIndex()+j);
						IBorderPoint p = n.getOriginalBorderPoint(k); // get the border points in the segment
						xpoints[j] = (float) p.getX();
						ypoints[j] = (float) p.getY();
					}

					PolygonRoi segRoi = new PolygonRoi(xpoints, ypoints, Roi.POLYLINE);


					Paint color = ColourSelecter.getColor(i);

					annotatePolygon(segRoi, color);
				}
			}
		} catch(Exception e){
			error("Error annotating segments", e);
		}
		return this;
	}
	
	
	public ImageAnnotator annotateSignals(Nucleus n){

		ISignalCollection signalCollection = n.getSignalCollection();
		for( UUID id : signalCollection.getSignalGroupIDs()){
			int i = signalCollection.getSignalGroupNumber(id);
			List<INuclearSignal> signals = signalCollection.getSignals(id);
			Color colour = i==ImageImporter.FIRST_SIGNAL_CHANNEL 
						 ? Color.RED 
						 : i==ImageImporter.FIRST_SIGNAL_CHANNEL+1 
						 	? Color.GREEN 
						 	: Color.WHITE;
			


			if(!signals.isEmpty()){

				for(INuclearSignal s : signals){
					
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
