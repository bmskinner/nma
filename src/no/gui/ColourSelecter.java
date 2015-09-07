package no.gui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class ColourSelecter {
	public static List<Color> segmentColourList = new ArrayList<Color>(0);
	
	public static List<Color> signalColourList = new ArrayList<Color>(0);
	
	public static List<Color> optimisedSwatchList = new ArrayList<Color>(0);
	
	// these are the colours for segments in the order they will loop
	static {

//		 Regular
		segmentColourList.add(Color.RED);
		segmentColourList.add(Color.ORANGE);
		segmentColourList.add(Color.GREEN);
		segmentColourList.add(Color.MAGENTA);
		segmentColourList.add(Color.DARK_GRAY);
		segmentColourList.add(Color.CYAN);
		segmentColourList.add(Color.YELLOW);
		segmentColourList.add(Color.PINK);
		segmentColourList.add( new Color(0,153,0)); // 
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
		
	}
	
	public static int getSegmentListSize(){
		return ColourSelecter.segmentColourList.size();
	}
	
	public static int getSignalListSize(){
		return ColourSelecter.signalColourList.size();
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
