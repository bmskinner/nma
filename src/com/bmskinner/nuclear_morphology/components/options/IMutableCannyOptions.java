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

package com.bmskinner.nuclear_morphology.components.options;

/**
 * The setters for ICannyOptions
 * @author bms41
 * @since 1.13.3
 *
 */
public interface IMutableCannyOptions extends ICannyOptions {
	
	IMutableCannyOptions duplicate();
	
	/**
	 * @param useCanny
	 */
	void setUseCanny(boolean useCanny);
	
	void setFlattenImage(boolean flattenImage);
	
	void setFlattenThreshold(int flattenThreshold);
	
	void setUseKuwahara(boolean b);
	
	void setKuwaharaKernel(int radius);
	
	void setClosingObjectRadius(int closingObjectRadius);
	
	void setCannyAutoThreshold(boolean cannyAutoThreshold);
	
	void setLowThreshold(float lowThreshold);
	
	void setHighThreshold(float highThreshold);

	void setKernelRadius(float kernelRadius);
	
	void setKernelWidth(int kernelWidth);
	
	void setAddBorder(boolean b);

}
