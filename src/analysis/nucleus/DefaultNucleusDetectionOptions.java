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

package analysis.nucleus;

import java.io.File;

import components.CellularComponent;
import components.nuclei.Nucleus;
import analysis.AbstractDetectionOptions;
import analysis.DefaultCannyOptions;
import analysis.IDetectionOptions;
import analysis.IMutableDetectionOptions;

/**
 * The default detection options for a nucleus
 * @author bms41
 * @since 1.13.3
 *
 */
public class DefaultNucleusDetectionOptions extends AbstractDetectionOptions {

	private static final long serialVersionUID = 1L;
	
	public DefaultNucleusDetectionOptions(File folder){
		super(folder);
		
		this.setCannyOptions( new DefaultCannyOptions());
	}
	
	public DefaultNucleusDetectionOptions(IDetectionOptions template){
		super(template);
		
	}
	
	@Override
	public DefaultNucleusDetectionOptions setSize(double min, double max){
		super.setSize(min, max);
		return this;
	}
	
	@Override
	public DefaultNucleusDetectionOptions setCircularity(double min, double max){
		super.setCircularity(min, max);
		return this;
	}
	

	@Override
	public boolean isValid(CellularComponent c) {
		if(c instanceof Nucleus){
			return super.isValid(c);
		} else {
			return false;
		}
	}

	@Override
	public IMutableDetectionOptions duplicate() {
		return new DefaultNucleusDetectionOptions(this);
	}
	
	

}
