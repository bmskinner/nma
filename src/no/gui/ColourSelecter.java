package no.gui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class ColourSelecter {
	public static List<Color> segmentColourList = new ArrayList<Color>(0);
	
	public static List<Color> signalColourList = new ArrayList<Color>(0);
	
	// these are the colours for segments in the order they will loop
	static {

		
		// For making pretty charts
//		colourList.add(Color.ORANGE);
//		colourList.add(Color.YELLOW);
//		colourList.add(Color.BLUE);
//		colourList.add(Color.CYAN);
		
//		colourList.add(Color.BLUE);
//		colourList.add(Color.GREEN);
//		colourList.add(Color.ORANGE);
		
//		colourList.add(Color.BLUE);
//		colourList.add(new Color(0,102,0));
//		colourList.add(Color.GREEN);
//		colourList.add(new Color(102,255,102));
//		colourList.add(Color.ORANGE);
//
//		 Regular
		segmentColourList.add(Color.RED);
		segmentColourList.add(Color.ORANGE);
		segmentColourList.add(Color.GREEN);
		segmentColourList.add(Color.MAGENTA);
		segmentColourList.add(Color.DARK_GRAY);
		segmentColourList.add(Color.CYAN);
		segmentColourList.add(Color.YELLOW);
		segmentColourList.add(Color.PINK);
				
		signalColourList.add(Color.BLUE);
		signalColourList.add(Color.RED);
		signalColourList.add(Color.GREEN);
		signalColourList.add(Color.CYAN);
		signalColourList.add(Color.LIGHT_GRAY);
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
	 * Get a colour for displaying the given channel specifying transparency and alpha options
	 * @param channel the channel to display
	 * @param transparent is the colour transparent
	 * @param defaultAlpha the transparency level
	 * @return a colour
	 */
	public Color getSignalColour(int channel, boolean transparent, int defaultAlpha){
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
	public Color getSignalColour(int channel, boolean transparent){
		return getSignalColour(channel, transparent, 10);
	}
	
	/**
	 * Get a colour for displaying the given channel without transparency
	 * @param channel the channel to display
	 * @return a solid colour
	 */	
	public Color getSignalColour(int channel){
		return getSignalColour(channel, false);
	}
}
