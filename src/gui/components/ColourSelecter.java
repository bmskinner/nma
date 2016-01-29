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
package gui.components;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class ColourSelecter {
	
	public static final int REGULAR_SWATCH 		= 0; // segmentColourList
	public static final int NO_SWATCH 			= 1; // black
	public static final int ACCESSIBLE_SWATCH 	= 2; // colour blind optimisedSwatchList
	
	public static List<Color> segmentColourList = new ArrayList<Color>(0);
	
	public static List<Color> signalColourList = new ArrayList<Color>(0);
	
	public static List<Color> optimisedSwatchList = new ArrayList<Color>(0);
	
	public static List<Color> blackList = new ArrayList<Color>(0);
	
	// these are the colours for segments in the order they will loop
	static {

//		 Regular
		segmentColourList.add(Color.BLUE);
		segmentColourList.add(Color.ORANGE);
		segmentColourList.add(Color.GREEN);
		segmentColourList.add(Color.MAGENTA);
		segmentColourList.add(Color.DARK_GRAY);
		segmentColourList.add(Color.CYAN);
		segmentColourList.add(Color.RED);
		segmentColourList.add(Color.YELLOW);
		segmentColourList.add(Color.PINK);
		segmentColourList.add( new Color(0,153,0)); // lime green
		segmentColourList.add(new Color(135,206,235)); // sky blue
				
		// Colours for signals in nuclei
		signalColourList.add(Color.RED);
		signalColourList.add(Color.GREEN);
		signalColourList.add(Color.CYAN);
		signalColourList.add(Color.MAGENTA);
		signalColourList.add(Color.YELLOW);
		signalColourList.add(Color.LIGHT_GRAY);
		
//		Color blind friendly swatch
		// See http://optional.is/required/2011/06/20/accessible-color-swatches/
		optimisedSwatchList.add(Color.decode("#fff200"));
		optimisedSwatchList.add(Color.decode("#006f45"));
		optimisedSwatchList.add(Color.decode("#f7941e"));
		optimisedSwatchList.add(Color.decode("#008fd5"));
		optimisedSwatchList.add(Color.decode("#abd69c"));
		optimisedSwatchList.add(Color.decode("#741472"));
		
		// no segments shown
		blackList.add(Color.BLACK);

	}
	
	public enum ColourSwatch {
		REGULAR_SWATCH 	  ("Regular", 	segmentColourList), 
		NO_SWATCH		  ("No colours"	, blackList),
		ACCESSIBLE_SWATCH ("Acessible colours", optimisedSwatchList);
		
	    private final String asString;   
	    private final List<Color> colours;
	    
	    ColourSwatch(String value, List<Color> colours) {
	        this.asString = value;
	        this.colours = colours;
		}
	    
	    public String toString(){
	    	return this.asString;
	    }
	    
	    public Color color(int index){
	    	return colours.get(index % colours.size());
	    }
	}
	
	public static int getSegmentListSize(){
		return ColourSelecter.segmentColourList.size();
	}
	
	public static int getSignalListSize(){
		return ColourSelecter.signalColourList.size();
	}
	
	/**
	 * Get an colour from the desired swatch for the given index.
	 * @param swatch the swatch to use
	 * @param i the number of the colour to return
	 * @return a colour
	 */
	public static Color getSwatchColour(int swatch, int index){
		
		Color color = null;
		switch(swatch){
			case REGULAR_SWATCH: color = getSegmentColor(index);
					break;
			case NO_SWATCH: color = Color.BLACK;
					break;
			case ACCESSIBLE_SWATCH: color = getOptimisedColor(index);
					break;
			default: color = getSegmentColor(index);
					break;
		}
		return color;
	}
	
	/**
	 * Get an appropriate segment colour for the given number.
	 * Loops through 8 colours.
	 * @param i the number of the colour to return
	 * @return a colour
	 */
	public static Color getSegmentColor(int i){		
		Color color = ColourSelecter.segmentColourList.get(i % ColourSelecter.segmentColourList.size());
		return color;
	}
	
	/**
	 * Get an appropriate colour for the given number.
	 * Loops through 6 colours that have been chosen to be distinguishable
	 * in three major types of color blindness, Deutranopia, 
	 * Protanopia and Tritanopia.
	 * @param i the number of the colour to return
	 * @return a colour
	 */
	public static Color getOptimisedColor(int i){		
		Color color = ColourSelecter.optimisedSwatchList.get(i % ColourSelecter.optimisedSwatchList.size());
		return color;
	}
	
	/**
	 * Get a colour for displaying the given channel specifying transparency and alpha options
	 * @param channel the channel to display
	 * @param transparent is the colour transparent
	 * @param defaultAlpha the transparency level
	 * @return a colour
	 */
	public static Color getSignalColour(int channel, boolean transparent, int defaultAlpha){
		Color result;
		Color color = ColourSelecter.signalColourList.get(channel % ColourSelecter.signalColourList.size());
		result = transparent ? new Color(color.getRed(),color.getGreen(),color.getBlue(),defaultAlpha) : color;
		return result;
	}
	
	/**
	 * Get a colour for displaying the given channel specifying transparency
	 * @param channel the channel to display
	 * @param transparent is the colour transparent
	 * @return a colour with the default transparency
	 */
	public static Color getSignalColour(int channel, boolean transparent){
		return getSignalColour(channel, transparent, 10);
	}
	
	/**
	 * Get a colour for displaying the given channel without transparency
	 * @param channel the channel to display
	 * @return a solid colour
	 */	
	public static Color getSignalColour(int channel){
		return getSignalColour(channel, false);
	}
	
	/**
	 * Get a transparent version of a colour
	 * @param c the input colour
	 * @param transparent flag
	 * @param defaultAlpha the alpha level
	 * @return the new colour
	 */
	public static Color getTransparentColour(Color c, boolean transparent, int defaultAlpha){
		Color result = transparent ? new Color(c.getRed(),c.getGreen(),c.getBlue(),defaultAlpha) : c;
		return result;
	}
	
	/**
	 * Get a transparent version of a colour using default transparency (10)
	 * @param c the input colour
	 * @param transparent flag
	 * @return  a colour with the default transparency
	 */
	public static Color getTransparentColour(Color c, boolean transparent){
		return getTransparentColour(c, transparent, 10);
	}
}
