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
package com.bmskinner.nma.analysis.image;

import java.awt.Rectangle;

import org.eclipse.jdt.annotation.NonNull;

import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

/*
	Performs the Kuwahara Filter, a noise-reduction filter that preserves edges.

	a  a  ab   b  b
	a  a  ab   b  b
	ac ac abcd bd bd
	c  c  cd   d  d
	c  c  cd   d  d
    
	In the case of a 5x5 sampling window, the mean brightness and the  
	variance of each of the four 3x3 regions (a, b, c, d), are calculated
	and the value of the center pixel (abcd) is set to the mean value 
	of the region that with the smallest variance.
 
	Description based on the one at:
	http://www.incx.nec.co.jp/imap-vision/library/wouter/kuwahara.html
*/
public class KuwaharaFilter {

	private static boolean isFilterRGB = false;

	public static ImageProcessor filter(@NonNull ImageProcessor ip, int size) {

		if (ip.getBitDepth() == 24) {
			if (isFilterRGB)
				return filterRGB(ip);
			return filterIntensity(ip);
		}

		Rectangle roi = ip.getRoi();
		int width = roi.width;
		int height = roi.height;
		int size2 = (size + 1) / 2;
		int offset = (size - 1) / 2;
		int width2 = ip.getWidth() + offset;
		int height2 = ip.getHeight() + offset;
		float[][] mean = new float[width2][height2];
		float[][] variance = new float[width2][height2];
		int x1start = roi.x;
		int y1start = roi.y;
		double sum, sum2;
		int n, v = 0, xbase, ybase;

		// Calculate variances
		for (int y1 = y1start - offset; y1 < y1start + height; y1++) {
			for (int x1 = x1start - offset; x1 < x1start + width; x1++) {
				sum = 0;
				sum2 = 0;
				n = 0;
				for (int x2 = x1; x2 < x1 + size2; x2++) {
					for (int y2 = y1; y2 < y1 + size2; y2++) {
						v = ip.getPixel(x2, y2);
						sum += v;
						sum2 += v * v;
						n++;
					}
				}
				mean[x1 + offset][y1 + offset] = (float) (sum / n);
				variance[x1 + offset][y1 + offset] = (float) (sum2 - sum * sum / n);
			}
		}

		ImageProcessor result = ip.duplicate();
		// Choose new values based on variances
		int xbase2 = 0, ybase2 = 0;
		float var, min;
		for (int y1 = y1start; y1 < y1start + height; y1++) {
			for (int x1 = x1start; x1 < x1start + width; x1++) {
				min = Float.MAX_VALUE;
				xbase = x1;
				ybase = y1;
				var = variance[xbase][ybase];
				if (var < min) {
					min = var;
					xbase2 = xbase;
					ybase2 = ybase;
				}
				xbase = x1 + offset;
				var = variance[xbase][ybase];
				if (var < min) {
					min = var;
					xbase2 = xbase;
					ybase2 = ybase;
				}
				ybase = y1 + offset;
				var = variance[xbase][ybase];
				if (var < min) {
					min = var;
					xbase2 = xbase;
					ybase2 = ybase;
				}
				xbase = x1;
				var = variance[xbase][ybase];
				if (var < min) {
					min = var;
					xbase2 = xbase;
					ybase2 = ybase;
				}
				result.putPixel(x1, y1, (int) (mean[xbase2][ybase2] + 0.5));
			}
		}
		return result;
	}

	private static ImageProcessor filterRGB(ImageProcessor ip) {
		ColorProcessor cp = (ColorProcessor) ip.duplicate();
		int width = cp.getWidth();
		int height = cp.getHeight();
		int size = width * height;
		byte[] R = new byte[size];
		byte[] G = new byte[size];
		byte[] B = new byte[size];
		cp.getRGB(R, G, B);
		ImageProcessor red = new ByteProcessor(width, height, R, null);
		filter(red, size);

		ImageProcessor green = new ByteProcessor(width, height, G, null);
		filter(green, size);

		ImageProcessor blue = new ByteProcessor(width, height, B, null);
		filter(blue, size);
		cp.setRGB((byte[]) red.getPixels(), (byte[]) green.getPixels(), (byte[]) blue.getPixels());
		return cp;
	}

	private static ImageProcessor filterIntensity(ImageProcessor ip) {
		ColorProcessor cp = (ColorProcessor) ip.duplicate();
		int width = cp.getWidth();
		int height = cp.getHeight();
		int size = width * height;
		byte[] H = new byte[size];
		byte[] S = new byte[size];
		byte[] B = new byte[size];
		cp.getHSB(H, S, B);
		ImageProcessor ip2 = new ByteProcessor(width, height, B, null);
		filter(ip2, size);
		cp.setHSB(H, S, (byte[]) ip2.getPixels());
		return cp;
	}
}
