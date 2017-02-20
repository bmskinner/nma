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

package com.bmskinner.nuclear_morphology.analysis.detection;

/**
 * A default implementation of Mask.
 * @author bms41
 * @since 1.13.3
 *
 */
public class BooleanMask implements Mask {
	
	protected boolean[][] array;
	
	protected int w, h;

	/**
	 * Create a mask of the given size set to false
	 * @param width
	 * @param height
	 */
	public BooleanMask(int width, int height){
		w = width;
		h = height;
		array = new boolean[h][w];
		for(int y=0; y<h; y++){
			for(int x=0; x<w; x++){
				array[y][x] = false;
			}
		}
	}
	
	/**
	 * Create a mask from the given array
	 * @param template
	 */
	public BooleanMask(boolean[][] template){
		this(template.length, template[0].length);
		for(int y=0; y<h; y++){
			for(int x=0; x<w; x++){
				array[y][x] = template[y][x];
			}
		}
	}
	
	@Override
	public int getWidth() {
		return w;
	}

	@Override
	public int getHeight() {
		return h;
	}
	
	@Override
	public boolean get(int x, int y) {
		return array[y][x];
	}
	
	/**
	 * Calculate the logical AND of the two input arrays.
	 * @param array1
	 * @param array2
	 * @return
	 */
	@Override
	public Mask and(Mask template){
		
		int height = template.getHeight();
		int width  = template.getWidth();

		boolean[][] result = new boolean[height][width];

		for(int y=0; y<height; y++){
			for(int x=0; x<width; x++){
				result[y][x] = array[y][x]  &&  template.get(x, y);
			}
		}
		return new BooleanMask( result);
	}

	@Override
	public Mask offset(int xOffset, int yOffset) {
		int height = array.length;
		int width  = array[0].length;
		boolean[][] result = new boolean[height][width];
		zeroArray(result); 

		for(int y=0; y<height; y++){
			
			if(y-yOffset<0 || y-yOffset >= height){
				continue;
			}
			
			for(int x=0; x<width; x++){

				if(x-xOffset<0 || x-xOffset >= width){
					continue;
				}
				result[y][x] = array[y-yOffset][x-xOffset];
			}
		}
		return new BooleanMask( result);
	}
	
	private void zeroArray(boolean[][] array){
		int height = array.length;
		int width  = array[0].length;
		for(int y=0; y<height; y++){
			for(int x=0; x<width; x++){

				array[y][x] = false;
			}
		}

	}
	
	private void trueArray(boolean[][] array){
		int height = array.length;
		int width  = array[0].length;
		for(int y=0; y<height; y++){
			for(int x=0; x<width; x++){

				array[y][x] = true;
			}
		}

	}

	@Override
	public boolean[][] toArray() {
		return array;
	}

	@Override
	public Mask setFalse() {
		zeroArray(array);
		return this;
	}

	@Override
	public Mask setTrue() {
		trueArray(array);
		return this;
	}

	@Override
	public void set(int x, int y, boolean b) {
		// TODO - add bounds check
		array[y][x] = b;
	}


}
