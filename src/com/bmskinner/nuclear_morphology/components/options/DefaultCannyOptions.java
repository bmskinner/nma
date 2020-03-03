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
package com.bmskinner.nuclear_morphology.components.options;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * A default implementation of the ICannyOptions interface
 * 
 * @author bms41
 * @since 1.13.3
 * 
 */
@Deprecated
public class DefaultCannyOptions implements ICannyOptions {
	
	private static final Logger LOGGER = Logger.getLogger(DefaultCannyOptions.class.getName());

    private static final long serialVersionUID = 1L;

    // values for Canny edge deteection
    private boolean useCanny;
    private boolean cannyAutoThreshold;

    private boolean flattenChromocentres; // should the white threshold be
                                          // lowered to hide internal
                                          // structures?
    private int     flattenThreshold;     // if the white threhold is lower,
                                          // this is the value
    private boolean useKuwahara;          // perform a Kuwahara filtering to
                                          // enhance edge detection?
    private int     kuwaharaKernel;       // the radius of the Kuwahara kernel -
                                          // must be an odd number

    private float             lowThreshold;        // the canny low threshold
    private float             highThreshold;       // the canny high threshold
    private float             kernelRadius;        // the kernel radius
    private int               kernelWidth;         // the kernel width
    private int               closingObjectRadius; // the circle radius for
                                                   // morphological closing
    private transient boolean isAddBorder = false;

    /**
     * Construct with the default options in ICannyOptions
     */
    public DefaultCannyOptions() {

        useCanny = DEFAULT_USE_CANNY;
        cannyAutoThreshold = DEFAULT_AUTO_THRESHOLD;

        flattenChromocentres = DEFAULT_FLATTEN_CHROMOCENTRES;
        flattenThreshold = DEFAULT_FLATTEN_THRESHOLD;
        useKuwahara = DEFAULT_USE_KUWAHARA;
        kuwaharaKernel = DEFAULT_KUWAHARA_KERNEL_RADIUS;

        lowThreshold = DEFAULT_CANNY_LOW_THRESHOLD;
        highThreshold = DEFAULT_CANNY_HIGH_THRESHOLD;
        kernelRadius = DEFAULT_CANNY_KERNEL_RADIUS;
        kernelWidth = DEFAULT_CANNY_KERNEL_WIDTH;
        closingObjectRadius = DEFAULT_CLOSING_OBJECT_RADIUS;
        isAddBorder = DEFAULT_ADD_BORDER;

    }

    /**
     * Construct from a template options
     * 
     * @param template
     */
    public DefaultCannyOptions(ICannyOptions template) {

        useCanny = template.isUseCanny();
        cannyAutoThreshold = template.isAddBorder();
        flattenChromocentres = template.isUseFlattenImage();
        flattenThreshold = template.getFlattenThreshold();

        useKuwahara = template.isUseKuwahara();
        kuwaharaKernel = template.getKuwaharaKernel();

        lowThreshold = template.getLowThreshold();
        highThreshold = template.getHighThreshold();
        kernelRadius = template.getKernelRadius();
        kernelWidth = template.getKernelWidth();
        closingObjectRadius = template.getClosingObjectRadius();
        isAddBorder = template.isAddBorder();
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.ICannyOptions#isUseCanny()
     */
    @Override
    public boolean isUseCanny() {
        return useCanny;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.ICannyOptions#setUseCanny(boolean)
     */
    @Override
    public void setUseCanny(boolean useCanny) {
        this.useCanny = useCanny;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.ICannyOptions#isUseFlattenImage()
     */
    @Override
    public boolean isUseFlattenImage() {
        return flattenChromocentres;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.ICannyOptions#setFlattenImage(boolean)
     */
    @Override
    public void setFlattenImage(boolean flattenImage) {
        this.flattenChromocentres = flattenImage;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.ICannyOptions#getFlattenThreshold()
     */
    @Override
    public int getFlattenThreshold() {
        return flattenThreshold;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.ICannyOptions#setFlattenThreshold(int)
     */
    @Override
    public void setFlattenThreshold(int flattenThreshold) {
        this.flattenThreshold = flattenThreshold;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.ICannyOptions#isUseKuwahara()
     */
    @Override
    public boolean isUseKuwahara() {
        return useKuwahara;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.ICannyOptions#setUseKuwahara(boolean)
     */
    @Override
    public void setUseKuwahara(boolean b) {
        this.useKuwahara = b;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.ICannyOptions#getKuwaharaKernel()
     */
    @Override
    public int getKuwaharaKernel() {
        return kuwaharaKernel;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.ICannyOptions#setKuwaharaKernel(int)
     */
    @Override
    public void setKuwaharaKernel(int radius) {
        kuwaharaKernel = radius;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.ICannyOptions#getClosingObjectRadius()
     */
    @Override
    public int getClosingObjectRadius() {
        return closingObjectRadius;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.ICannyOptions#setClosingObjectRadius(int)
     */
    @Override
    public void setClosingObjectRadius(int closingObjectRadius) {
        this.closingObjectRadius = closingObjectRadius;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.ICannyOptions#isCannyAutoThreshold()
     */
    @Override
    public boolean isCannyAutoThreshold() {
        return cannyAutoThreshold;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.ICannyOptions#setCannyAutoThreshold(boolean)
     */
    @Override
    public void setCannyAutoThreshold(boolean cannyAutoThreshold) {
        this.cannyAutoThreshold = cannyAutoThreshold;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.ICannyOptions#getLowThreshold()
     */
    @Override
    public float getLowThreshold() {
        return lowThreshold;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.ICannyOptions#setLowThreshold(float)
     */
    @Override
    public void setLowThreshold(float lowThreshold) {
        this.lowThreshold = lowThreshold;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.ICannyOptions#getHighThreshold()
     */
    @Override
    public float getHighThreshold() {
        return highThreshold;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.ICannyOptions#setHighThreshold(float)
     */
    @Override
    public void setHighThreshold(float highThreshold) {
        this.highThreshold = highThreshold;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.ICannyOptions#getKernelRadius()
     */
    @Override
    public float getKernelRadius() {
        return kernelRadius;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.ICannyOptions#setKernelRadius(float)
     */
    @Override
    public void setKernelRadius(float kernelRadius) {
        this.kernelRadius = kernelRadius;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.ICannyOptions#getKernelWidth()
     */
    @Override
    public int getKernelWidth() {
        return kernelWidth;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.ICannyOptions#setKernelWidth(int)
     */
    @Override
    public void setKernelWidth(int kernelWidth) {
        this.kernelWidth = kernelWidth;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.ICannyOptions#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (cannyAutoThreshold ? 1231 : 1237);
        result = prime * result + closingObjectRadius;
        result = prime * result + (flattenChromocentres ? 1231 : 1237);
        result = prime * result + flattenThreshold;
        result = prime * result + Float.floatToIntBits(highThreshold);
        result = prime * result + Float.floatToIntBits(kernelRadius);
        result = prime * result + kernelWidth;
        result = prime * result + kuwaharaKernel;
        result = prime * result + Float.floatToIntBits(lowThreshold);
        result = prime * result + (useCanny ? 1231 : 1237);
        result = prime * result + (useKuwahara ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (obj == null)
            return false;

        if (!(obj instanceof ICannyOptions))
            return false;

        ICannyOptions other = (ICannyOptions) obj;

        if (cannyAutoThreshold != other.isCannyAutoThreshold())
            return false;
        if (closingObjectRadius != other.getClosingObjectRadius())
            return false;
        if (flattenChromocentres != other.isUseFlattenImage())
            return false;
        if (flattenThreshold != other.getFlattenThreshold())
            return false;
        if (Float.floatToIntBits(highThreshold) != Float.floatToIntBits(other.getHighThreshold()))
            return false;
        if (Float.floatToIntBits(kernelRadius) != Float.floatToIntBits(other.getKernelRadius()))
            return false;
        if (kernelWidth != other.getKernelWidth())
            return false;
        if (kuwaharaKernel != other.getKuwaharaKernel())
            return false;
        if (Float.floatToIntBits(lowThreshold) != Float.floatToIntBits(other.getLowThreshold()))
            return false;
        if (useCanny != other.isUseCanny())
            return false;
        if (useKuwahara != other.isUseKuwahara())
            return false;
        return true;
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {

        /*
         * The chromocentre flattening parameter and Kuwahara kernel parameter
         * are transient. When these are stored, check if they were filled, and
         * override if needed.
         */
        in.defaultReadObject();
        isAddBorder = false;
    }

    @Override
    public boolean isAddBorder() {
        return isAddBorder;
    }

    @Override
    public void setAddBorder(boolean b) {
        isAddBorder = b;

    }

    @Override
    public ICannyOptions duplicate() {
        return new DefaultCannyOptions(this);
    }

    @Override
    public void set(ICannyOptions template) {

        useCanny = template.isUseCanny();
        cannyAutoThreshold = template.isAddBorder();
        flattenChromocentres = template.isUseFlattenImage();
        flattenThreshold = template.getFlattenThreshold();

        useKuwahara = template.isUseKuwahara();
        kuwaharaKernel = template.getKuwaharaKernel();

        lowThreshold = template.getLowThreshold();
        highThreshold = template.getHighThreshold();
        kernelRadius = template.getKernelRadius();
        kernelWidth = template.getKernelWidth();
        closingObjectRadius = template.getClosingObjectRadius();
        isAddBorder = template.isAddBorder();

    }

    @Override
    public List<String> getKeys() {
    	LOGGER.warning("Unimplemented method in " + this.getClass().getName());
        return null;
    }

    @Override
    public Object getValue(String key) {
    	LOGGER.warning("Unimplemented method in " + this.getClass().getName());
        return null;
    }

	@Override
	public double getDouble(String s) {
		LOGGER.warning("Unimplemented method in " + this.getClass().getName());
		return 0;
	}

	@Override
	public int getInt(String s) {
		LOGGER.warning("Unimplemented method in " + this.getClass().getName());
		return 0;
	}

	@Override
	public boolean getBoolean(String s) {
		LOGGER.warning("Unimplemented method in " + this.getClass().getName());
		return false;
	}

	@Override
	public void setDouble(String s, double d) {
		LOGGER.warning("Unimplemented method in " + this.getClass().getName());
		
	}

	@Override
	public void setInt(String s, int i) {
		LOGGER.warning("Unimplemented method in " + this.getClass().getName());
		
	}

	@Override
	public void setBoolean(String s, boolean b) {
		LOGGER.warning("Unimplemented method in " + this.getClass().getName());
		
	}

	@Override
	public float getFloat(String s) {
		LOGGER.warning("Unimplemented method in " + this.getClass().getName());
		return 0;
	}

	@Override
	public void setFloat(String s, float f) {
		LOGGER.warning("Unimplemented method in " + this.getClass().getName());
	}

	@Override
	public List<String> getBooleanKeys() {
		LOGGER.warning("Unimplemented method in " + this.getClass().getName());
		return null;
	}

	@Override
	public List<String> getIntegerKeys() {
		LOGGER.warning("Unimplemented method in " + this.getClass().getName());
		return null;
	}

	@Override
	public List<String> getDoubleKeys() {
		LOGGER.warning("Unimplemented method in " + this.getClass().getName());
		return null;
	}

	@Override
	public List<String> getFloatKeys() {
		LOGGER.warning("Unimplemented method in " + this.getClass().getName());
		return null;
	}

	@Override
	public Map<String, Object> getEntries() {
		LOGGER.warning("Unimplemented method in " + this.getClass().getName());
		return null;
	}

	@Override
	public String getString(String s) {
		LOGGER.warning("Unimplemented method in " + this.getClass().getName());
		return null;
	}

	@Override
	public void setString(String k, String v) {
		LOGGER.warning("Unimplemented method in " + this.getClass().getName());
	}

	@Override
	public List<String> getStringKeys() {
		LOGGER.warning("Unimplemented method in " + this.getClass().getName());
		return null;
	}

	@Override
	public void set(HashOptions o) {
		// TODO Auto-generated method stub
		
	}

}
