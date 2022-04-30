package com.bmskinner.nma;

import java.io.File;

import com.bmskinner.nma.components.Version;
import com.bmskinner.nma.io.Io;

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
	
    public static final String UNIT_TEST_FOLDERNAME = "UnitTest_"+Version.currentVersion();
    public static final File   UNIT_TEST_FOLDER = new File(IMAGE_FOLDER,UNIT_TEST_FOLDERNAME+SLASH);
    public static final String UNIT_TEST_FILENAME   = UNIT_TEST_FOLDERNAME+Io.SAVE_FILE_EXTENSION;
	
    public static final File MOUSE_INPUT_FOLDER = new File(IMAGE_FOLDER.getAbsolutePath(), MOUSE);
    
    public static final File PIG_INPUT_FOLDER   = new File(IMAGE_FOLDER.getAbsolutePath(), PIG);
    public static final File ROUND_INPUT_FOLDER = new File(IMAGE_FOLDER.getAbsolutePath(), ROUND);

    public static final File MOUSE_OUTPUT_FOLDER = new File(MOUSE_INPUT_FOLDER, UNIT_TEST_FOLDERNAME);
    public static final File PIG_OUTPUT_FOLDER   = new File(PIG_INPUT_FOLDER,   UNIT_TEST_FOLDERNAME);
    public static final File ROUND_OUTPUT_FOLDER = new File(ROUND_INPUT_FOLDER, UNIT_TEST_FOLDERNAME);
    

    public static final File MULTIPLE_BASE_FOLDER        = new File(IMAGE_FOLDER, "Multiple/");
    public static final File MULTIPLE_SOURCE_1_FOLDER    = new File(MULTIPLE_BASE_FOLDER, MULTIPLE1);
    public static final File MULTIPLE_SOURCE_2_FOLDER    = new File(MULTIPLE_BASE_FOLDER, MULTIPLE2);
    public static final File MULTIPLE_BASE_OUTPUT_FOLDER = new File(MULTIPLE_BASE_FOLDER, UNIT_TEST_FOLDERNAME);
    
    public static final File MOUSE_SIGNALS_INPUT_FOLDER  = new File(IMAGE_FOLDER, MOUSE + WITH_SIGNALS);
    public static final File MOUSE_SIGNALS_OUTPUT_FOLDER = new File(MOUSE_SIGNALS_INPUT_FOLDER, UNIT_TEST_FOLDERNAME);

    public static final File PIG_SIGNALS_INPUT_FOLDER    = new File(IMAGE_FOLDER, PIG   + WITH_SIGNALS);
    public static final File PIG_SIGNALS_OUTPUT_FOLDER   = new File(PIG_SIGNALS_INPUT_FOLDER, UNIT_TEST_FOLDERNAME);
    
    public static final File ROUND_SIGNALS_INPUT_FOLDER  = new File(IMAGE_FOLDER, ROUND + WITH_SIGNALS);
    public static final File ROUND_SIGNALS_OUTPUT_FOLDER = new File(ROUND_SIGNALS_INPUT_FOLDER, UNIT_TEST_FOLDERNAME);
    
    public static final File MOUSE_CLUSTERS_INPUT_FOLDER  = new File(IMAGE_FOLDER, MOUSE + WITH_CLUSTERS);
    public static final File MOUSE_CLUSTERS_OUTPUT_FOLDER = new File(MOUSE_CLUSTERS_INPUT_FOLDER, UNIT_TEST_FOLDERNAME);
    
    public static final File PIG_CLUSTERS_INPUT_FOLDER   = new File(IMAGE_FOLDER, PIG   + WITH_CLUSTERS);
    public static final File PIG_CLUSTERS_OUTPUT_FOLDER  = new File(PIG_CLUSTERS_INPUT_FOLDER, UNIT_TEST_FOLDERNAME);
    
    public static final File ROUND_CLUSTERS_INPUT_FOLDER  = new File(IMAGE_FOLDER, ROUND + WITH_CLUSTERS);
    public static final File ROUND_CLUSTERS_OUTPUT_FOLDER = new File(ROUND_CLUSTERS_INPUT_FOLDER, UNIT_TEST_FOLDERNAME);
    


    
    public static final File DATASET_FOLDER = new File(DATASET_FOLDERNAME, "UnitTest_"+Version.currentVersion());
    
    public static final File MOUSE_TEST_DATASET     = new File(MOUSE_OUTPUT_FOLDER, MOUSE + Io.SAVE_FILE_EXTENSION);
    public static final File PIG_TEST_DATASET       = new File(PIG_OUTPUT_FOLDER,   PIG   + Io.SAVE_FILE_EXTENSION);
    public static final File ROUND_TEST_DATASET     = new File(ROUND_OUTPUT_FOLDER, ROUND + Io.SAVE_FILE_EXTENSION);
    
    public static final File MULTIPLE1_TEST_DATASET =  new File(MULTIPLE_BASE_OUTPUT_FOLDER, MULTIPLE1 + Io.SAVE_FILE_EXTENSION);
    public static final File MULTIPLE2_TEST_DATASET =  new File(MULTIPLE_BASE_OUTPUT_FOLDER, MULTIPLE2 + Io.SAVE_FILE_EXTENSION);
    
    
    public static final File MOUSE_CLUSTERS_DATASET = new File(MOUSE_CLUSTERS_OUTPUT_FOLDER, MOUSE + WITH_CLUSTERS +Io.SAVE_FILE_EXTENSION);
    public static final File PIG_CLUSTERS_DATASET   = new File(PIG_CLUSTERS_OUTPUT_FOLDER,   PIG   + WITH_CLUSTERS +Io.SAVE_FILE_EXTENSION);
    public static final File ROUND_CLUSTERS_DATASET = new File(ROUND_CLUSTERS_OUTPUT_FOLDER, ROUND + WITH_CLUSTERS +Io.SAVE_FILE_EXTENSION);
    
    public static final File MOUSE_SIGNALS_DATASET  = new File(MOUSE_SIGNALS_OUTPUT_FOLDER, MOUSE + WITH_SIGNALS + Io.SAVE_FILE_EXTENSION);
    public static final File PIG_SIGNALS_DATASET    = new File(PIG_SIGNALS_OUTPUT_FOLDER, PIG   + WITH_SIGNALS + Io.SAVE_FILE_EXTENSION);
    public static final File ROUND_SIGNALS_DATASET  = new File(ROUND_SIGNALS_OUTPUT_FOLDER, ROUND + WITH_SIGNALS + Io.SAVE_FILE_EXTENSION);
    
    public static final File MULTIPLE_SOURCE_1_DATASET  = new File(MULTIPLE_BASE_OUTPUT_FOLDER, MULTIPLE1+Io.SAVE_FILE_EXTENSION);
    public static final File MULTIPLE_SOURCE_2_DATASET  = new File(MULTIPLE_BASE_OUTPUT_FOLDER, MULTIPLE2+Io.SAVE_FILE_EXTENSION);
    
    public static final String GLCM_SAMPLE_IMAGE = IMAGE_FOLDER+ SLASH+"GLCM"+SLASH+"s61.tiff-3_b43854f1-95c5-4810-bbac-a467ef5dd0ed.tiff";
    
    public static final File WARPING_FOLDER = new File(IMAGE_FOLDER, "Warping");
    public static final String WARPING_NORMALISATION_IMAGE = WARPING_FOLDER + SLASH + "Gradient.tiff";
    

    
}
