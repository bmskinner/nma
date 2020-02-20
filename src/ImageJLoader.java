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
import com.bmskinner.nuclear_morphology.core.Nuclear_Morphology_Analysis;

import ij.IJ;
import ij.plugin.PlugIn;

/**
 * ImageJ plugins require their runnable class to be in 
 * the default package. This stub invokes the real main 
 * class in its appropriate package.
 * @author Ben Skinner
 *
 */
public class ImageJLoader implements PlugIn {
	
	/* 
     * The first method run when the plugin starts within ImageJ.
	 */
	public void run(String paramString){
				
		try {
		    
			new Nuclear_Morphology_Analysis();

		} catch(Exception e){
			IJ.log(e.toString());
		} 
	}

}
