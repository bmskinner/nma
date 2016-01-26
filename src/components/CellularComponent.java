/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
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
package components;

import java.awt.Rectangle;
import java.io.File;
import java.util.UUID;

public interface CellularComponent {
		
	public UUID getID();
	
	
	/**
	 * Get the position of the object in the 
	 * original image. The indexes in the double are
	 * 0 - X_BASE of the bounding box
	 * 1 - Y_BASE of the bounding box
	 * 2 - WIDTH of the bounding box
	 * 3 - HEIGHT of the bounding box
	 * @return
	 */
	public static final int X_BASE 	= 0;
	public static final int Y_BASE 	= 1;
	public static final int WIDTH 	= 2;
	public static final int HEIGHT 	= 3;
	
	public double[] getPosition();
	
	public double getArea();
	
	public boolean equals(CellularComponent c);
	
	public File getSourceFile();
	
	public void setSourceFile(File sourceFile);
		
	public int getChannel();

	public void setChannel(int channel);
	
	public double getPerimeter();
	
	public Rectangle getBounds();
	
	public File getSourceFolder();

	public String getSourceFileName();

	public void setPosition(double[] position);


	public void setArea(double area);


	public void setPerimeter(double perimeter);


	public void setBoundingRectangle(Rectangle boundingRectangle);


	public void setSourceFileName(String name);
	
	public void setSourceFolder(File sourceFolder);
	


}
