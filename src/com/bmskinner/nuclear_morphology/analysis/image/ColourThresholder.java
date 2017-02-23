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

import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;

/**
 * This is based on the ImageJ package ij.plugin.frame.ColorThresholder.
 * It removes UI code and adds an API
 * @author bms41
 *
 */
public class ColourThresholder {
	private static final int HSB=0, RGB=1, LAB=2, YUV=3;
//	private static final String[] colorSpaces = {"HSB", "RGB", "Lab", "YUV"};
	private boolean flag = false;
	private int colorSpace = HSB;
//	private Thread thread;
//	private static Frame instance;

//	private int previousImageID = -1;
//	private int previousSlice = -1;
	
	private boolean useH = true, useS=true, useB=true;
	
	private int minHue = 0, minSat = 0, minBri = 0;
	private int maxHue = 255, maxSat = 255, maxBri = 255;

//	private boolean done;
	private byte[] hSource, sSource, bSource;
//	private boolean applyingStack;

//	private static final int DEFAULT = 0;
//	private static String[] methodNames = AutoThresholder.getMethods();
//	private static String method = methodNames[DEFAULT];
//	private static AutoThresholder thresholder = new AutoThresholder();
	private static final int RED=0, WHITE=1, BLACK=2, BLACK_AND_WHITE=3;
//	private static final String[] modes = {"Red", "White", "Black", "B&W"};
	private static int mode = BLACK_AND_WHITE;	

	private int numSlices;
	private ImageStack stack;
	private int width, height, numPixels;
	
	/**
	 * Create with default parameters (no thresholding)
	 */
	public ColourThresholder(){	}
	
	private void checkArgs(int min, int max){
		if(min>=max){
			throw new IllegalArgumentException("Min is greater than max");
		}
		
		if(min<0 || min > 255){
			throw new IllegalArgumentException("Min must be in range 0-255");
		}
		
		if(max<0 || max > 255){
			throw new IllegalArgumentException("Max must be in range 0-255");
		}
	}
	
	public void setHue(int min, int max){
		
		checkArgs(min, max);
		
		minHue = min;
		maxHue = max;
	}
	
	public void setSat(int min, int max){
		checkArgs(min, max);
		minSat = min;
		maxSat = max;
	}
	
	public void setBri(int min, int max){
		checkArgs(min, max);
		minBri = min;
		maxBri = max;
	}
	
	
	/**
	 * Threshold the given color image processor using the HSV values set
	 * @param ip
	 * @return
	 */
	public ImageProcessor threshold(ImageProcessor ip) {

		if(! (ip instanceof ColorProcessor)){
			throw new IllegalArgumentException("Must be a colour processor");
		}
		
		ImagePlus imp = new ImagePlus("", ip.duplicate());
		
		setup(imp);
		
		apply(imp);
		
		return imp.getProcessor();
	}
	
	private boolean setup(ImagePlus imp) {

			ImageProcessor ip = imp.getProcessor();
			
			flag = false; //if true, flags a change of colour model
			numSlices = imp.getStackSize();
			stack = imp.getStack();
			width = stack.getWidth();
			height = stack.getHeight();
			numPixels = width*height;

			hSource = new byte[numPixels];
			sSource = new byte[numPixels];
			bSource = new byte[numPixels];
			
			ImageProcessor mask = new ByteProcessor(width, height);
			imp.setProperty("Mask", mask);

			//Get hsb or rgb from image.
			ColorProcessor cp = (ColorProcessor)ip;
//			IJ.showStatus("Converting colour space...");
			if(colorSpace==RGB)
				cp.getRGB(hSource,sSource,bSource);
			else if(colorSpace==HSB)
				cp.getHSB(hSource,sSource,bSource);
			else if(colorSpace==LAB)
				getLab(cp, hSource,sSource,bSource);
			else if(colorSpace==YUV)
				getYUV(cp, hSource,sSource,bSource);

//			IJ.showStatus("");

			//Create a spectrum ColorModel for the Hue histogram plot.
			Color c;
			byte[] reds = new byte[256];
			byte[] greens = new byte[256];
			byte[] blues = new byte[256];
			for (int i=0; i<256; i++) {
				c = Color.getHSBColor(i/255f, 1f, 1f);
				reds[i] = (byte)c.getRed();
				greens[i] = (byte)c.getGreen();
				blues[i] = (byte)c.getBlue();
			}
			ColorModel cm = new IndexColorModel(8, 256, reds, greens, blues);

			//Make an image with just the hue from the RGB image and the spectrum LUT.
			//This is just for a hue histogram for the plot.  Do not show it.
			//ByteProcessor bpHue = new ByteProcessor(width,height,h,cm);
			ByteProcessor bpHue = new ByteProcessor(width,height,hSource,cm);
			ImagePlus impHue = new ImagePlus("Hue",bpHue);


			ByteProcessor bpSat = new ByteProcessor(width,height,sSource,cm);
			ImagePlus impSat = new ImagePlus("Sat",bpSat);


			ByteProcessor bpBri = new ByteProcessor(width,height,bSource,cm);
			ImagePlus impBri = new ImagePlus("Bri",bpBri);

		return ip!=null;
	}

	
	private void apply(ImagePlus imp) {

		ImageProcessor fillMaskIP = (ImageProcessor)imp.getProperty("Mask");
		if (fillMaskIP==null) return;
		byte[] fillMask = (byte[])fillMaskIP.getPixels();
		byte fill = (byte)255;
		byte keep = (byte)0;

		if (useH && useS && useB){ //PPP All pass
			for (int j = 0; j < numPixels; j++){
				int hue = hSource[j]&0xff;
				int sat = sSource[j]&0xff;
				int bri = bSource[j]&0xff;
				if (((hue < minHue)||(hue > maxHue)) || ((sat < minSat)||(sat > maxSat)) || ((bri < minBri)||(bri > maxBri)))
					fillMask[j] = keep;
				else
					fillMask[j] = fill;
			}
		}
		else if(!useH && !useS && !useB){ //SSS All stop
			for (int j = 0; j < numPixels; j++){
				int hue = hSource[j]&0xff;
				int sat = sSource[j]&0xff;
				int bri = bSource[j]&0xff;
				if (((hue >= minHue)&&(hue <= maxHue)) || ((sat >= minSat)&&(sat <= maxSat)) || ((bri >= minBri)&&(bri <= maxBri)))
					fillMask[j] = keep;
				else
					fillMask[j] = fill;
			}
		}
		else if(useH && useS && !useB){ //PPS
			for (int j = 0; j < numPixels; j++){
				int hue = hSource[j]&0xff;
				int sat = sSource[j]&0xff;
				int bri = bSource[j]&0xff;
				if (((hue < minHue)||(hue > maxHue)) || ((sat < minSat)||(sat > maxSat)) || ((bri >= minBri) && (bri <= maxBri)))
					fillMask[j] = keep;
				else
					fillMask[j] = fill;
			}
		}
		else if(!useH && !useS && useB){ //SSP
			for (int j = 0; j < numPixels; j++){
				int hue = hSource[j]&0xff;
				int sat = sSource[j]&0xff;
				int bri = bSource[j]&0xff;
				if (((hue >= minHue) && (hue <= maxHue)) || ((sat >= minSat) && (sat <= maxSat)) || ((bri < minBri) || (bri > maxBri)))
					fillMask[j] = keep;
				else
					fillMask[j] = fill;
			}
		}
		else if (useH && !useS && !useB){ //PSS
			for (int j = 0; j < numPixels; j++){
				int hue = hSource[j]&0xff;
				int sat = sSource[j]&0xff;
				int bri = bSource[j]&0xff;
				if (((hue < minHue) || (hue > maxHue)) || ((sat >= minSat) && (sat <= maxSat)) || ((bri >= minBri) && (bri <= maxBri)))
					fillMask[j] = keep;
				else
					fillMask[j] = fill;
			}
		}
		else if(!useH && useS && useB){ //SPP
			for (int j = 0; j < numPixels; j++){
				int hue = hSource[j]&0xff;
				int sat = sSource[j]&0xff;
				int bri = bSource[j]&0xff;
				if (((hue >= minHue) && (hue <= maxHue))|| ((sat < minSat) || (sat > maxSat)) || ((bri < minBri) || (bri > maxBri)))
					fillMask[j] = keep;
				else
					fillMask[j] = fill;
			}
		}
		else if (!useH && useS && !useB){ //SPS
			for (int j = 0; j < numPixels; j++){
				int hue = hSource[j]&0xff;
				int sat = sSource[j]&0xff;
				int bri = bSource[j]&0xff;
				if (((hue >= minHue)&& (hue <= maxHue)) || ((sat < minSat)||(sat > maxSat)) || ((bri >= minBri) && (bri <= maxBri)))
					fillMask[j] = keep;
				else
					fillMask[j] = fill;
			}
		}
		else if(useH && !useS && useB){ //PSP
			for (int j = 0; j < numPixels; j++){
				int hue = hSource[j]&0xff;
				int sat = sSource[j]&0xff;
				int bri = bSource[j]&0xff;
				if (((hue < minHue) || (hue > maxHue)) || ((sat >= minSat)&&(sat <= maxSat)) || ((bri < minBri) || (bri > maxBri)))
					fillMask[j] = keep;
				else
					fillMask[j] = fill;
			}
		}

		ImageProcessor ip = imp.getProcessor();
		if (ip==null) return;
		if (mode==BLACK_AND_WHITE) {
			int[] pixels = (int[])ip.getPixels();
			int fcolor = Prefs.blackBackground?0xffffffff:0xff000000;
			int bcolor = Prefs.blackBackground?0xff000000:0xffffffff;
			for (int i=0; i<numPixels; i++) {
				if (fillMask[i]!=0)
					pixels[i] = fcolor;
				else
					pixels[i]= bcolor;
			}
		} else {
			ip.setColor(thresholdColor());
			ip.fill(fillMaskIP);
		}
	}
	
	private Color thresholdColor() {
		Color color = null;
		switch (mode) {
			case RED: color=Color.red; break;
			case WHITE: color=Color.white; break;
			case BLACK: color=Color.black; break;
			case BLACK_AND_WHITE: color=Color.black; break;
		}
		return color;
	}
	
	
	public void getLab(ImageProcessor ip, byte[] L, byte[] a, byte[] b) {
		// Returns Lab in 3 byte arrays.
		//http://www.brucelindbloom.com/index.html?WorkingSpaceInfo.html#Specifications
		//http://www.easyrgb.com/math.php?MATH=M7#text7
		int c, x, y, i=0;
		double rf, gf, bf;
		double X, Y, Z, fX, fY, fZ;
		double La, aa, bb;
		double ot=1/3.0, cont = 16/116.0;

		int width=ip.getWidth();
		int height=ip.getHeight();

		for(y=0;y<height; y++) {
			for (x=0; x< width;x++){
				c = ip.getPixel(x,y);

				// RGB to XYZ
				rf = ((c&0xff0000)>>16)/255.0; //R 0..1
				gf = ((c&0x00ff00)>>8)/255.0; //G 0..1
				bf = ( c&0x0000ff)/255.0; //B 0..1

				// gamma = 1.0?
				//white reference D65 PAL/SECAM
				X = 0.430587 * rf + 0.341545 * gf + 0.178336 * bf;
				Y = 0.222021 * rf + 0.706645 * gf + 0.0713342* bf;
				Z = 0.0201837* rf + 0.129551 * gf + 0.939234 * bf;

				// XYZ to Lab
				if ( X > 0.008856 )
					fX =  Math.pow(X, ot);
				else
					fX = ( 7.78707 * X ) + cont;
					//7.7870689655172

				if ( Y > 0.008856 )
					fY = Math.pow(Y, ot);
				else
					fY = ( 7.78707 * Y ) + cont;

				if ( Z > 0.008856 )
					fZ =  Math.pow(Z, ot);
				else
					fZ = ( 7.78707 * Z ) + cont;

				La = ( 116 * fY ) - 16;
				aa = 500 * ( fX - fY );
				bb = 200 * ( fY - fZ );

				// rescale
				La = (int) (La * 2.55);
				aa = (int) (Math.floor((1.0625 * aa + 128) + 0.5));
				bb = (int) (Math.floor((1.0625 * bb + 128) + 0.5));

				//hsb = Color.RGBtoHSB(r, g, b, hsb);
				// a* and b* range from -120 to 120 in the 8 bit space

				//L[i] = (byte)((int)(La*2.55) & 0xff);
				//a[i] = (byte)((int)(Math.floor((1.0625 * aa + 128) + 0.5)) & 0xff);
				//b[i] = (byte)((int)(Math.floor((1.0625 * bb + 128) + 0.5)) & 0xff);

				L[i] = (byte)((int)(La<0?0:(La>255?255:La)) & 0xff);
				a[i] = (byte)((int)(aa<0?0:(aa>255?255:aa)) & 0xff);
				b[i] = (byte)((int)(bb<0?0:(bb>255?255:bb)) & 0xff);
				i++;
			}
		}
	}
	
	public void getYUV(ImageProcessor ip, byte[] Y, byte[] U, byte[] V) {
		// Returns YUV in 3 byte arrays.
		
		//RGB <--> YUV Conversion Formulas from http://www.cse.msu.edu/~cbowen/docs/yuvtorgb.html
		//R = Y + (1.4075 * (V - 128));
		//G = Y - (0.3455 * (U - 128) - (0.7169 * (V - 128));
		//B = Y + (1.7790 * (U - 128);
		//
		//Y = R *  .299 + G *  .587 + B *  .114;
		//U = R * -.169 + G * -.332 + B *  .500 + 128.;
		//V = R *  .500 + G * -.419 + B * -.0813 + 128.;

		int c, x, y, i=0, r, g, b;
		double yf;

		int width=ip.getWidth();
		int height=ip.getHeight();

		for(y=0;y<height; y++) {
			for (x=0; x< width;x++){
				c = ip.getPixel(x,y);

				r = ((c&0xff0000)>>16);//R
				g = ((c&0x00ff00)>>8);//G
				b = ( c&0x0000ff); //B 

				// Kai's plugin
				yf = (0.299 * r  + 0.587 * g + 0.114 * b);
				Y[i] = (byte)((int)Math.floor(yf + 0.5)) ;
				U[i] = (byte)(128+(int)Math.floor((0.493 *(b - yf))+ 0.5)); 
				V[i] = (byte)(128+(int)Math.floor((0.877 *(r - yf))+ 0.5)); 
				
				//Y[i] = (byte) (Math.floor( 0.299 * r + 0.587 * g + 0.114  * b)+.5);
				//U[i] = (byte) (Math.floor(-0.169 * r - 0.332 * g + 0.500  * b + 128.0)+.5);
				//V[i] = (byte) (Math.floor( 0.500 * r - 0.419 * g - 0.0813 * b + 128.0)+.5);
				
				i++;
			}
		}
	}
	
	
}
