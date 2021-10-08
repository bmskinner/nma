package com.bmskinner.nuclear_morphology;

import java.io.File;

import com.bmskinner.nuclear_morphology.components.Version;
import com.bmskinner.nuclear_morphology.io.Io;

/**
 * Define the folder and file name constants for testing images and datasets
 * @author ben
 * @since 1.14.0
 */
public class TestResources {
	
	public static final File IMAGE_FOLDER   = new File("test/samples/images/");
	public static final File DATASET_FOLDERNAME = new File("test/samples/datasets/");
	
	public static final String SLASH = "/";
	
	public static final String MOUSE = "Mouse";
	public static final String PIG   = "Pig";
	public static final String ROUND = "Round";
	
	public static final String MULTIPLE1 = "Multiple_source_1";
	public static final String MULTIPLE2 = "Multiple_source_2";
	
	public static final String WITH_CLUSTERS = "_with_clusters";
	public static final String WITH_SIGNALS  = "_with_signals";
	
    public static final File TESTING_MOUSE_FOLDER = new File(IMAGE_FOLDER.getAbsolutePath(), MOUSE);
    public static final File TESTING_PIG_FOLDER   = new File(IMAGE_FOLDER.getAbsolutePath(), PIG);
    public static final File TESTING_ROUND_FOLDER = new File(IMAGE_FOLDER.getAbsolutePath(), ROUND);

    public static final File TESTING_MULTIPLE_BASE_FOLDER     = new File(IMAGE_FOLDER, "Multiple/");
    public static final File TESTING_MULTIPLE_SOURCE_1_FOLDER = new File(TESTING_MULTIPLE_BASE_FOLDER, MULTIPLE1);
    public static final File TESTING_MULTIPLE_SOURCE_2_FOLDER = new File(TESTING_MULTIPLE_BASE_FOLDER, MULTIPLE2);
    
    public static final File TESTING_MOUSE_SIGNALS_FOLDER = new File(IMAGE_FOLDER, MOUSE + WITH_SIGNALS);
    public static final File TESTING_PIG_SIGNALS_FOLDER   = new File(IMAGE_FOLDER, PIG   + WITH_SIGNALS);
    public static final File TESTING_ROUND_SIGNALS_FOLDER = new File(IMAGE_FOLDER, ROUND + WITH_SIGNALS);
    
    public static final File TESTING_MOUSE_CLUSTERS_FOLDER = new File(IMAGE_FOLDER, MOUSE + WITH_CLUSTERS);
    public static final File TESTING_PIG_CLUSTERS_FOLDER   = new File(IMAGE_FOLDER, PIG   + WITH_CLUSTERS);
    public static final File TESTING_ROUND_CLUSTERS_FOLDER = new File(IMAGE_FOLDER, ROUND + WITH_CLUSTERS);

    public static final String UNIT_TEST_FOLDERNAME = "UnitTest_"+Version.currentVersion();
    public static final File   UNIT_TEST_FOLDER = new File(IMAGE_FOLDER,UNIT_TEST_FOLDERNAME+SLASH);
    public static final String UNIT_TEST_FILENAME   = UNIT_TEST_FOLDERNAME+Io.SAVE_FILE_EXTENSION;
    
    public static final File DATASET_FOLDER = new File(DATASET_FOLDERNAME, "UnitTest_"+Version.currentVersion());
    
    public static final File MOUSE_TEST_DATASET     = new File(TESTING_MOUSE_FOLDER.getAbsolutePath() + UNIT_TEST_FOLDER, MOUSE + Io.SAVE_FILE_EXTENSION);
    public static final File PIG_TEST_DATASET       = new File(TESTING_PIG_FOLDER.getAbsolutePath()   + UNIT_TEST_FOLDER, PIG   + Io.SAVE_FILE_EXTENSION);
    public static final File ROUND_TEST_DATASET     = new File(TESTING_ROUND_FOLDER.getAbsolutePath() + UNIT_TEST_FOLDER, ROUND + Io.SAVE_FILE_EXTENSION);
    
    public static final File MULTIPLE1_TEST_DATASET =  new File(TESTING_MULTIPLE_BASE_FOLDER.getAbsolutePath() + UNIT_TEST_FOLDER, MULTIPLE1 + Io.SAVE_FILE_EXTENSION);
    public static final File MULTIPLE2_TEST_DATASET =  new File(TESTING_MULTIPLE_BASE_FOLDER.getAbsolutePath() + UNIT_TEST_FOLDER, MULTIPLE2 + Io.SAVE_FILE_EXTENSION);
    
    
    public static final File MOUSE_CLUSTERS_DATASET = new File(TESTING_MOUSE_CLUSTERS_FOLDER.getAbsolutePath() + UNIT_TEST_FOLDER, MOUSE + WITH_CLUSTERS +Io.SAVE_FILE_EXTENSION);
    public static final File PIG_CLUSTERS_DATASET   = new File(TESTING_PIG_CLUSTERS_FOLDER.getAbsolutePath() + UNIT_TEST_FOLDER, PIG   + WITH_CLUSTERS +Io.SAVE_FILE_EXTENSION);
    public static final File ROUND_CLUSTERS_DATASET = new File(TESTING_ROUND_CLUSTERS_FOLDER.getAbsolutePath() + UNIT_TEST_FOLDER, ROUND + WITH_CLUSTERS +Io.SAVE_FILE_EXTENSION);
    
    public static final File MOUSE_SIGNALS_DATASET  = new File(TESTING_MOUSE_SIGNALS_FOLDER.getAbsolutePath()  + UNIT_TEST_FOLDER, MOUSE + WITH_SIGNALS + Io.SAVE_FILE_EXTENSION);
    public static final File PIG_SIGNALS_DATASET    = new File(TESTING_PIG_SIGNALS_FOLDER.getAbsolutePath()    + UNIT_TEST_FOLDER, PIG   + WITH_SIGNALS + Io.SAVE_FILE_EXTENSION);
    public static final File ROUND_SIGNALS_DATASET  = new File(TESTING_ROUND_SIGNALS_FOLDER.getAbsolutePath()  + UNIT_TEST_FOLDER, ROUND + WITH_SIGNALS + Io.SAVE_FILE_EXTENSION);
    
    public static final File MULTIPLE_SOURCE_1_DATASET  = new File(TESTING_MULTIPLE_BASE_FOLDER.getAbsolutePath()  + UNIT_TEST_FOLDER, MULTIPLE1+Io.SAVE_FILE_EXTENSION);
    public static final File MULTIPLE_SOURCE_2_DATASET  = new File(TESTING_MULTIPLE_BASE_FOLDER.getAbsolutePath()  + UNIT_TEST_FOLDER, MULTIPLE2+Io.SAVE_FILE_EXTENSION);
    
    public static final String GLCM_SAMPLE_IMAGE = IMAGE_FOLDER+ SLASH+"GLCM"+SLASH+"s61.tiff-3_b43854f1-95c5-4810-bbac-a467ef5dd0ed.tiff";
    
    public static final String WARPING_FOLDER = IMAGE_FOLDER + SLASH + "Warping";
    public static final String WARPING_NORMALISATION_IMAGE = WARPING_FOLDER + SLASH + "Gradient.tiff";
    

    
}
