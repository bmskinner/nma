package com.bmskinner.nuclear_morphology;

import com.bmskinner.nuclear_morphology.components.generic.Version;
import com.bmskinner.nuclear_morphology.io.Io;

/**
 * Define the folder and file name constants for testing images and datasets
 * @author ben
 * @since 1.14.0
 */
public class TestResources {
	
	public static final String IMAGE_FOLDER   = "test/samples/images/";
	public static final String DATASET_FOLDER = "test/samples/datasets/";
	
	public static final String SLASH = "/";
	
	public static final String MOUSE = "Mouse";
	public static final String PIG   = "Pig";
	public static final String ROUND = "Round";
	
	public static final String WITH_CLUSTERS = "_with_clusters";
	public static final String WITH_SIGNALS  = "_with_signals";
	
    public static final String TESTING_MOUSE_FOLDER = IMAGE_FOLDER + MOUSE + SLASH;
    public static final String TESTING_PIG_FOLDER   = IMAGE_FOLDER + PIG   + SLASH;
    public static final String TESTING_ROUND_FOLDER = IMAGE_FOLDER + ROUND + SLASH;

    public static final String TESTING_MULTIPLE_BASE_FOLDER = IMAGE_FOLDER + "Multiple/";
    public static final String TESTING_MULTIPLE_SOURCE_1_FOLDER = TESTING_MULTIPLE_BASE_FOLDER + "Multiple_source_1/";
    public static final String TESTING_MULTIPLE_SOURCE_2_FOLDER = TESTING_MULTIPLE_BASE_FOLDER + "Multiple_source_2/";
    
    public static final String TESTING_MOUSE_SIGNALS_FOLDER = IMAGE_FOLDER + MOUSE + WITH_SIGNALS + SLASH;
    public static final String TESTING_PIG_SIGNALS_FOLDER   = IMAGE_FOLDER + PIG   + WITH_SIGNALS + SLASH;
    public static final String TESTING_ROUND_SIGNALS_FOLDER = IMAGE_FOLDER + ROUND + WITH_SIGNALS + SLASH;
    
    public static final String TESTING_MOUSE_CLUSTERS_FOLDER = IMAGE_FOLDER + MOUSE + WITH_CLUSTERS + SLASH;
    public static final String TESTING_PIG_CLUSTERS_FOLDER   = IMAGE_FOLDER + PIG   + WITH_CLUSTERS + SLASH;
    public static final String TESTING_ROUND_CLUSTERS_FOLDER = IMAGE_FOLDER + ROUND + WITH_CLUSTERS + SLASH;

    public static final String UNIT_TEST_FOLDERNAME = "UnitTest_"+Version.currentVersion();
    public static final String UNIT_TEST_FOLDER     = UNIT_TEST_FOLDERNAME + SLASH;
    public static final String UNIT_TEST_FILENAME   = UNIT_TEST_FOLDERNAME+Io.SAVE_FILE_EXTENSION;
    
    public static final String MOUSE_TEST_DATASET     = TESTING_MOUSE_FOLDER + UNIT_TEST_FOLDER + MOUSE + Io.SAVE_FILE_EXTENSION;
    public static final String PIG_TEST_DATASET       = TESTING_PIG_FOLDER   + UNIT_TEST_FOLDER + PIG   + Io.SAVE_FILE_EXTENSION;
    public static final String ROUND_TEST_DATASET     = TESTING_ROUND_FOLDER + UNIT_TEST_FOLDER + ROUND + Io.SAVE_FILE_EXTENSION;
    
    public static final String MOUSE_CLUSTERS_DATASET = TESTING_MOUSE_CLUSTERS_FOLDER + UNIT_TEST_FOLDER + MOUSE + WITH_CLUSTERS +Io.SAVE_FILE_EXTENSION;
    public static final String PIG_CLUSTERS_DATASET   = TESTING_PIG_CLUSTERS_FOLDER   + UNIT_TEST_FOLDER + PIG   + WITH_CLUSTERS +Io.SAVE_FILE_EXTENSION;
    public static final String ROUND_CLUSTERS_DATASET = TESTING_ROUND_CLUSTERS_FOLDER + UNIT_TEST_FOLDER + ROUND + WITH_CLUSTERS +Io.SAVE_FILE_EXTENSION;
    
    public static final String MOUSE_SIGNALS_DATASET  = TESTING_MOUSE_SIGNALS_FOLDER  + UNIT_TEST_FOLDER + MOUSE + WITH_SIGNALS + Io.SAVE_FILE_EXTENSION;
    public static final String PIG_SIGNALS_DATASET    = TESTING_PIG_SIGNALS_FOLDER    + UNIT_TEST_FOLDER + PIG   + WITH_SIGNALS + Io.SAVE_FILE_EXTENSION;
    public static final String ROUND_SIGNALS_DATASET  = TESTING_ROUND_SIGNALS_FOLDER  + UNIT_TEST_FOLDER + ROUND + WITH_SIGNALS + Io.SAVE_FILE_EXTENSION;
    
    public static final String MULTIPLE_SOURCE_1_DATASET  = TESTING_MULTIPLE_BASE_FOLDER+UNIT_TEST_FOLDER+"Multiple_source_1"+Io.SAVE_FILE_EXTENSION;
    public static final String MULTIPLE_SOURCE_2_DATASET  = TESTING_MULTIPLE_BASE_FOLDER+UNIT_TEST_FOLDER+"Multiple_source_2"+Io.SAVE_FILE_EXTENSION;
    

}
