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

import components.generic.MeasurementScale;
import components.generic.XYPoint;
import stats.PlottableStatistic;

/**
 * These methods are provided through the AbstractCellularComponent,
 * from which all other components should be derived
 * @author bms41
 *
 */
/**
 * @author ben
 *
 */
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
	
	public static final String IMAGE_PREFIX = "export.";
	
	
	public double[] getPosition();
	
		
	public boolean equals(CellularComponent c);
	
	/**
	 * Get the image file the component was found in
	 * @return
	 */
	public File getSourceFile();
	
	public void setSourceFile(File sourceFile);
		
	public int getChannel();

	public void setChannel(int channel);
	
	/**
	 * Get the value of the given statistic for this nucleus.
	 * Note that NucleusStatistic.VARIABILILTY returns zero, 
	 * as this must be calculated at the collection level
	 * @param stat the statistic to fetch
	 * @param scale the units to return values in
	 * @return the value or zero if stat.equals(NucleusStatistic.VARIABILILTY)==true
	 * @throws Exception 
	 */
	public double getStatistic(PlottableStatistic stat, MeasurementScale scale) throws Exception;
	
	
	/**
	 * Get the statistic at the default scale (MeasurementScale.PIXELS)
	 * @param stat
	 * @return
	 * @throws Exception
	 */
	public double getStatistic(PlottableStatistic stat) throws Exception;
	
	public void setStatistic(PlottableStatistic stat, double d);
	
	public PlottableStatistic[] getStatistics();
	
	
	public Rectangle getBounds();
	
	/**
	 * Get the folder of the image the component was found in.
	 *  e.g. C:\Folder\ImageFolder\1.tiff
	 * will return ImageFolder
	 * @return
	 */
	public File getSourceFolder();

	/**
	 * Get the name of the image the component was found in
	 * @return
	 */
	public String getSourceFileName();
	
	/**
	 * Set the position of the component in the original
	 * image. See getPosition() for values to use.
	 * @param d
	 * @see getPosition()
	 */
	public void setPosition(double[] position);


	public void setBoundingRectangle(Rectangle boundingRectangle);


	public void setSourceFileName(String name);
	
	public void setSourceFolder(File sourceFolder);
	
	public double getScale();
	
	public void setScale(double scale);
	
	/**
	 * Get the position of the centre of mass of the nucleus
	 * @return
	 */
	public XYPoint getCentreOfMass();


	public void setCentreOfMass(XYPoint centreOfMass);


}
