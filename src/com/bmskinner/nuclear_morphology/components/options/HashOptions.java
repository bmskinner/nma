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
import java.util.Map;

import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.components.signals.IShellResult.ShrinkType;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.io.XmlSerializable;

/**
 * The interface for options classes. Store options as key value pairs
 * @author bms41
 *
 */
/**
 * @author ben
 *
 */
public interface HashOptions extends Serializable, XmlSerializable {
	
	String CANNY_SUBOPTIONS_KEY = "Canny";
	String BACKGROUND_KEY = "Background";
	
    String DETECTION_FOLDER                = "Folder";
    String THRESHOLD             = "Threshold";
    String CHANNEL               = "Channel";
    String IS_RGB                = "Is RGB";
    String MIN_CIRC              = "Min circ";
    String MAX_CIRC              = "Max circ";
    String MIN_SIZE_PIXELS       = "Min size";
    String MAX_SIZE_PIXELS       = "Max size";
    String SCALE                 = "Scale";
    String IS_NORMALISE_CONTRAST = "Normalise contrast";
    String IS_USE_CANNY          = "Use Canny";
    String DYNAMIC               = "Dynamic";
    String EROSION               = "Erosion";
    String IS_USE_WATERSHED      = "Use watershed";
    String TOP_HAT_RADIUS        = "Top hat radius";

    double  DEFAULT_SCALE        = GlobalOptions.getInstance().getImageScale();
    double  DEFAULT_MIN_CIRC     = 0;
    double  DEFAULT_MAX_CIRC     = 1;
    boolean DEFAULT_IS_RGB       = false;
    boolean DEFAULT_IS_NORMALISE = false;

    boolean DEFAULT_IS_USE_WATERSHED = true;
    int     DEFAULT_DYNAMIC          = 1;
    int     DEFAULT_EROSION          = 1;
    
    int      DEFAULT_MIN_NUCLEUS_SIZE   = 2000;
    int      DEFAULT_MAX_NUCLEUS_SIZE   = 10000;
    double   DEFAULT_MIN_NUCLEUS_CIRC   = 0.2;
    double   DEFAULT_MAX_NUCLEUS_CIRC   = 0.8;
    int      DEFAULT_NUCLEUS_THRESHOLD  = 36;

    int     DEFAULT_CHANNEL            = 2;
    boolean DEFAULT_NORMALISE_CONTRAST = false;
    
    /** Constants relating to signals **/
    
    String SIGNAL_MAX_FRACTION       = "Max fraction";
    String SIGNAL_DETECTION_MODE_KEY = "DETECTION_MODE";
    String SIGNAL_GROUP_NAME  = "Name";

    int                 DEFAULT_SIGNAL_THRESHOLD    = 70;
    int                 DEFAULT_SIGNAL_MIN_SIZE     = 5;
    double              DEFAULT_SIGNAL_MAX_FRACTION = 0.1;
    SignalDetectionMode DEFAULT_SIGNAL_DETECTION_METHOD = SignalDetectionMode.FORWARD;
    int                 DEFAULT_SIGNAL_CHANNEL      = 0;
    
    /** Constants relating to Canny edge detection **/
    String CANNY_LOW_THRESHOLD_FLT     = "Canny low threshold";
    String CANNY_HIGH_THRESHOLD_FLT    = "Canny high threshold";
    String CANNY_KERNEL_RADIUS_FLT     = "Canny kernel radius";
    String CANNY_KERNEL_WIDTH_INT      = "Canny kernel width";
    String CANNY_CLOSING_RADIUS_INT    = "Closing radius";
    
    String CANNY_IS_AUTO_THRESHOLD       = "Use auto threshold";
    String CANNY_IS_ADD_BORDER           = "Add border";

    float   DEFAULT_CANNY_LOW_THRESHOLD        = 0.5f;
    float   DEFAULT_CANNY_HIGH_THRESHOLD       = 1.5f;
    float   DEFAULT_CANNY_TAIL_LOW_THRESHOLD   = 0.1f;
    float   DEFAULT_CANNY_TAIL_HIGH_THRESHOLD  = 0.5f;
    float   DEFAULT_CANNY_KERNEL_RADIUS        = 3;
    int     DEFAULT_CANNY_KERNEL_WIDTH         = 16;
    int     DEFAULT_CANNY_CLOSING_RADIUS      = 5;
    int     DEFAULT_TAIL_CLOSING_OBJECT_RADIUS = 3;
    
    boolean DEFAULT_IS_USE_CANNY                  = true;
    boolean DEFAULT_IS_CANNY_AUTO_THRESHOLD       = false;
    boolean DEFAULT_IS_CANNY_ADD_BORDER           = false;
    
    
    /** Constants relating to preprocessing options **/
    String IS_USE_GAUSSIAN         = "Use Gaussian blur";
    String IS_USE_KUWAHARA         = "Use Kuwahara filter";
    String IS_USE_ROLLING_BALL     = "Use Rolling ball";
    String IS_USE_FLATTENING       = "Use flattening";
    
    
    /** Should minimum values be raised to a given threshold */
    String IS_USE_RAISING          = "Use raising";
    String IS_USE_COLOUR_THRESHOLD = "Use colour threshold";

    String GAUSSIAN_RADIUS          = "Gaussian radius";
    String KUWAHARA_RADIUS_INT      = "Kuwahara radius";
    String ROLLING_BALL_RADIUS      = "Rolling ball radius";
    String FLATTENING_THRESHOLD_INT = "Flattening threshold";
    String RAISING_THRESHOLD_INT    = "Raising threshold";

    String MIN_HUE = "Min hue";
    String MAX_HUE = "Max hue";
    String MIN_SAT = "Min saturation";
    String MAX_SAT = "Max saturation";
    String MIN_BRI = "Min brightness";
    String MAX_BRI = "Max brightness";

    int     DEFAULT_KUWAHARA_RADIUS = 3;
    boolean DEFAULT_USE_GAUSSIAN           = false;
    boolean DEFAULT_USE_KUWAHARA           = true;
    boolean DEFAULT_USE_ROLLING_BALL       = false;
    boolean DEFAULT_IS_USE_FLATTENNING  = true;
    int     DEFAULT_FLATTEN_THRESHOLD      = 100;
    boolean DEFAULT_IS_USE_RAISING    = false;
    int     DEFAULT_RAISE_THRESHOLD        = 100;
    boolean DEFAULT_IS_USE_COLOUR_THRESHOLD   = false;

    int DEFAULT_MIN_HUE = 0;
    int DEFAULT_MAX_HUE = 255;
    int DEFAULT_MIN_SAT = 0;
    int DEFAULT_MAX_SAT = 255;
    int DEFAULT_MIN_BRI = 0;
    int DEFAULT_MAX_BRI = 255;
    
    /** Constants relating to shell analysis options **/
    String SHELL_COUNT_INT = "SHELL_COUNT";
	String SHELL_EROSION_METHOD_KEY = "EROSION_METHOD";
	
	int DEFAULT_SHELL_COUNT = 5;
	ShrinkType DEFAULT_EROSION_METHOD = ShrinkType.AREA;
	
	/** Constants relating to clustering options **/
	String CLUSTER_SUB_OPTIONS_KEY           = "Clustering options";
	String CLUSTER_USE_SIMILARITY_MATRIX_KEY = "USE_SIMILARITY_MATRIX";
	String CLUSTER_MANUAL_CLUSTER_NUMBER_KEY = "MANUAL_CLUSTER_NUMBER";
	String CLUSTER_METHOD_KEY        = "CLUSTER_METHOD";
	String CLUSTER_HIERARCHICAL_METHOD_KEY   = "HIERARCHICAL_METHOD";
	String CLUSTER_EM_ITERATIONS_KEY         = "EM_ITERATIONS";
	String CLUSTER_MODALITY_REGIONS_KEY      = "MODALITY_REGIONS";
	String CLUSTER_USE_MODALITY_KEY          = "USE_MODALITY";
	String CLUSTER_INCLUDE_PROFILE_KEY       = "INCLUDE_PROFILE";
	String CLUSTER_INCLUDE_MESH_KEY          = "INCLUDE_MESH";
	String CLUSTER_PROFILE_TYPE_KEY          = "PROFILE_TYPE";
	String CLUSTER_USE_TSNE_KEY              = "Use t-SNE";
	String CLUSTER_USE_PCA_KEY               = "Use PCA";
	String CLUSTER_NUM_PCS_KEY               = "Number of PCs";

    int                       DEFAULT_MANUAL_CLUSTER_NUMBER = 2;
    ClusteringMethod          DEFAULT_CLUSTER_METHOD        = ClusteringMethod.HIERARCHICAL;
    HierarchicalClusterMethod DEFAULT_HIERARCHICAL_METHOD   = HierarchicalClusterMethod.WARD;
    ProfileType               DEFAULT_PROFILE_TYPE          = ProfileType.ANGLE;
    int                       DEFAULT_EM_ITERATIONS         = 100;
    int                       DEFAULT_MODALITY_REGIONS      = 2;
    boolean                   DEFAULT_USE_MODALITY          = true;
    boolean                   DEFAULT_USE_SIMILARITY_MATRIX = false;
    boolean                   DEFAULT_INCLUDE_PROFILE       = true;
    boolean                   DEFAULT_INCLUDE_MESH          = false;
    boolean                   DEFAULT_USE_TSNE              = false;
    boolean                   DEFAULT_USE_PCA               = false;
	
	
	/**
	 * Create a copy of this options object
	 * @return
	 */
	HashOptions duplicate();
	
	/**
	 * Test if suboptions are present with the given key
	 * @param s
	 * @return
	 */
	boolean hasSubOptions(String s);
	
	/**
	 * Get suboptions with the given key, if present
	 * @param s
	 * @return
	 */
	HashOptions getSubOptions(String s);
	
	/**
	 * Set suboptions for a given key
	 * @param s
	 * @param o
	 */
	void setSubOptions(String s, HashOptions o);
	
	/**
	 * Test if the given boolean key is present
	 * @param s
	 * @return
	 */
	boolean hasBoolean(String s);
	
	/**
	 * Test if the given float key is present
	 * @param s
	 * @return
	 */
	boolean hasFloat(String s);
	
	/**
	 * Test if the given double key is present
	 * @param s
	 * @return
	 */
	boolean hasDouble(String s);
	
	/**
	 * Test if the given integer key is present
	 * @param s
	 * @return
	 */
	boolean hasInt(String s);
	
	/**
	 * Test if the given string key is present
	 * @param s
	 * @return
	 */
	boolean hasString(String s);
	
    /**
     * Get the double value with the given key.
     * 
     * @param s
     * @return
     */
    double getDouble(String s);

    /**
     * Get the int value with the given key.
     * 
     * @param s
     * @return
     */
    int getInt(String s);

    /**
     * Get the boolean value with the given key.
     * 
     * @param s
     * @return
     */
    boolean getBoolean(String s);

    /**
     * Set the double value with the given key.
     * 
     * @param s
     * @param d
     */
    void setDouble(String s, double d);

    /**
     * ] Set the int value with the given key.
     * 
     * @param s
     * @param i
     */
    void setInt(String s, int i);

    /**
     * Set the boolean value with the given key.
     * 
     * @param s
     * @param b
     */
    void setBoolean(String s, boolean b);

    /**
     * Get the float value with the given key.
     * 
     * @param s
     * @return
     */
    float getFloat(String s);

    /**
     * Set the float value with the given key.
     * 
     * @param s
     * @param f
     */
    void setFloat(String s, float f);
    
    /**
     * Get the string value with the given key
     * @param s
     * @return
     */
    String getString(String s);
    
    /**
     * Set the string value with the given key
     * @param k
     * @param v
     */
    void setString(String k, String v);
    
    /**
     * Get the file with the given key. 
     * @param k
     * @param v
     */
    File getFile(String s);
    
    /**
     * Get the file value with the given key
     * @param k
     * @param v
     */
    void setFile(String s, File f);
    
    /**
     * Get the keys to all the boolean values in this options.
     * 
     * @return
     */
    List<String> getBooleanKeys();
    
    /**
     * Get the keys to all the integer values in this options.
     * 
     * @return
     */
    List<String> getIntegerKeys();
    
    /**
     * Get the keys to all the double values in this options.
     * 
     * @return
     */
    List<String> getDoubleKeys();
    
    /**
     * Get the keys to all the float values in this options.
     * 
     * @return
     */
    List<String> getFloatKeys();
    
    /**
     * Get the keys to all the string values in this options.
     * 
     * @return
     */
    List<String> getStringKeys();
    
    /**
     * Get the keys to all the sub options in this options.
     * 
     * @return
     */
    List<String> getSubOptionKeys();
    
    /**
     * Get the keys to all the values in this options.
     * 
     * @return
     */
    List<String> getKeys();
    
    /**
     * Get the complete set of keys and value objects within the options.
     * @return
     */
    Map<String, Object> getEntries();

     /**
     * Get the object stored with the given key
     * @param key
     * @return
     */
     Object getValue(String key);
     
     /**
      * Set to the values in the given options. Shared keys will be updated,
      * keys not present will be added. Keys not shared will be unaffected.
     * @param o
     */
    void set(HashOptions o);
    
    /**
     * Remove the given key if present.
     * If the key is used for e.g. String and
     * int, both will be removed
     * @param s
     */
    void remove(String s);
    
    /**
     * Remove all keys in the given options
     * @param o
     */
    void remove(HashOptions o);
    
}
