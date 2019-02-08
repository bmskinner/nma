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

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * This interface defines the detection options available for detecting an
 * object in an image.
 * 
 * @author bms41
 * @since 1.13.3
 *
 */
public interface IDetectionOptions extends Serializable, Loggable, HashOptions {

    static final String NEWLINE = System.getProperty("line.separator");

    static final String THRESHOLD             = "Threshold";
    static final String CHANNEL               = "Channel";
    static final String IS_RGB                = "Is RGB";
    static final String MIN_CIRC              = "Min circ";
    static final String MAX_CIRC              = "Max circ";
    static final String MIN_SIZE              = "Min size";
    static final String MAX_SIZE              = "Max size";
    static final String SCALE                 = "Scale";
    static final String IS_NORMALISE_CONTRAST = "Normalise contrast";
    static final String IS_USE_HOUGH          = "Use Hough";
    static final String IS_USE_CANNY          = "Use Canny";
    static final String DYNAMIC               = "Dynamic";
    static final String EROSION               = "Erosion";
    static final String IS_USE_WATERSHED      = "Use watershed";
    static final String TOP_HAT_RADIUS        = "Top hat radius";

    static final double  DEFAULT_SCALE        = GlobalOptions.getInstance().getImageScale();
    static final double  DEFAULT_MIN_CIRC     = 0;
    static final double  DEFAULT_MAX_CIRC     = 1;
    static final boolean DEFAULT_IS_RGB       = false;
    static final boolean DEFAULT_IS_NORMALISE = false;

    static final boolean DEFAULT_IS_USE_WATERSHED = true;
    static final int     DEFAULT_DYNAMIC          = 1;
    static final int     DEFAULT_EROSION          = 1;

    /**
     * All sub option classes implement this interface. They must be cast
     * appropriately later.
     * 
     * @author ben
     * @since 1.13.4
     *
     */
    public interface IDetectionSubOptions extends HashOptions, Serializable, Loggable {
        static final String CANNY_OPTIONS      = "Canny";
        static final String HOUGH_OPTIONS      = "Hough";
        static final String BACKGROUND_OPTIONS = "Background";
        static final String SHELL_OPTIONS      = "Shell";
        
        IDetectionSubOptions duplicate();

        /**
         * Interface for image preprocessing, such as colour thresholding and
         * background removal
         * 
         * @author bms41
         *
         */
        public interface IPreprocessingOptions extends IDetectionSubOptions {

            boolean isUseColourThreshold();

            int getMinHue();

            int getMaxHue();

            int getMinSaturation();

            int getMaxSaturation();

            int getMinBrightness();

            int getMaxBrightness();

            void setUseColourThreshold(boolean b);

            void setHueThreshold(int min, int max);

            void setSaturationThreshold(int min, int max);

            void setBrightnessThreshold(int min, int max);

        }

    }

    /**
     * Create a copy of these options
     * 
     * @return
     */
    IDetectionOptions duplicate();

    /**
     * Get the folder of images to be analysed
     * 
     * @return the folder
     */
    File getFolder();

    /**
     * Get the threshold for detecting object when not using edge detection
     * 
     * @return the pixel intensity threshold above which a pixel may be part of
     *         a nucleus
     */
    int getThreshold();

    /**
     * Get the minimum size of the object in pixels
     * 
     * @return the minimum size
     */
    double getMinSize();

    /**
     * Get the maximum of the object in pixels
     * 
     * @return the maximum size
     */
    double getMaxSize();

    /**
     * Get the minimum circularity of the object
     * 
     * @return the minimum circularity
     */
    double getMinCirc();

    /**
     * Get the maximum circularity of the object
     * 
     * @return the maximum circularity
     */
    double getMaxCirc();

    /**
     * Get the scale of the objects detected - the number of pixels per micon
     * 
     * @return the scale
     */
    double getScale();

    /**
     * Get the channel the objects are being detected in
     * 
     * @return
     */
    int getChannel();

    /**
     * Test if the image is to be RGB colour or greyscale. Use the channel
     * option to select a greyscale channel if this is false
     * 
     * @return
     */
    boolean isRGB();

    /**
     * Test if constrasts should be normalised when detecting this object
     * 
     * @return
     */
    boolean isNormaliseContrast();

    /**
     * Test if canny edge detection options have been set for this component
     * 
     * @return true if Canny options have been set, false otherwise
     */
    boolean hasCannyOptions();

    /**
     * Should a Hough circle detection be run?
     * 
     * @return
     */
    boolean isUseHoughTransform();

    /**
     * Test if the given sub options type is present
     * 
     * @param key
     * @return
     */
    boolean hasSubOptions(String key);

    /**
     * Get the sub options specified by the given key
     * 
     * @param s
     * @return
     */
    IDetectionSubOptions getSubOptions(String s) throws MissingOptionException;
    
    
    /**
     * Get all the sub option types defined in this object
     * @return
     */
    Set<String> getSubOptionKeys();

    /**
     * Get the Canny edge detection options for this object.
     * 
     * @return the Canny options, or null if none have been set
     */
    ICannyOptions getCannyOptions() throws MissingOptionException;

    /**
     * Test if the given component meets the criteria within these options. Note
     * that if Canny options are present, they take precedence over thresholding
     * options.
     * 
     * @param c
     *            the component to test
     * @return true if the component would be detected with these options, false
     *         otherwise
     */
    boolean isValid(@NonNull CellularComponent c);

    List<String> getKeys();
    
    /**
     * Set the RGB channel to detect the object in
     * 
     * @param channel
     */
    void setChannel(int channel);

    /**
     * Set the scale of the object in pixels per micron
     * 
     * @param scale
     */
    void setScale(double scale);

    /**
     * Set the detection threshold
     * 
     * @param nucleusThreshold
     */
    void setThreshold(int nucleusThreshold);

    /*
     * Set the minimum size of the object
     * 
     * @param minNucleusSize
     */
    void setMinSize(double minNucleusSize);

    /*
     * Set the maximum size of the object
     * 
     * @param minNucleusSize
     */
    void setMaxSize(double maxNucleusSize);

    /*
     * Set the minimum circularity of the object
     * 
     * @param minNucleusSize
     */
    void setMinCirc(double minNucleusCirc);

    /*
     * Set the maximum circularity of the object
     * 
     * @param minNucleusSize
     */
    void setMaxCirc(double maxNucleusCirc);

    /**
     * Set the folder of images where the objects are to be detected
     * 
     * @param folder
     */
    void setFolder(File folder);

    /**
     * Set Canny edge detection options to override thresholds
     * 
     * @param canny
     */
    void setCannyOptions(@NonNull ICannyOptions canny);

    void setNormaliseContrast(boolean b);

    /**
     * Set this options to match the parameters in the given option
     * 
     * @param options
     */
    void set(@NonNull IDetectionOptions options);

    /**
     * Set the hough options
     * 
     * @param hough
     */
    void setHoughOptions(@NonNull IHoughDetectionOptions hough);

    /**
     * Set arbitrary sub options
     * 
     * @param s
     *            the options key
     * @param sub
     */
    void setSubOptions(String s, @NonNull IDetectionSubOptions sub);

    /**
     * Set if the image is RGB or greyscale
     * 
     * @param b
     */
    void setRGB(boolean b);

}
