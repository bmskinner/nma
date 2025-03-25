package com.bmskinner.nma;

import java.io.File;
import org.eclipse.jdt.annotation.NonNull;
import com.bmskinner.nma.components.Version;
import com.bmskinner.nma.io.Io;

/**
 * Define the folder and file name constants for testing images and datasets
 * 
 * @author Ben Skinner
 * @since 1.14.0
 */
public class TestResources {

	/** Base folder for test images */
	@NonNull public static final File IMAGE_FOLDER_BASE = new File("test/samples/images/");

	/**
	 * Base folder for general output files - use DATASET_FOLDER for actual storage
	 */
	@NonNull private static final File DATASET_FOLDER_BASE = new File("test/samples/datasets/");

	@NonNull private static final String UNIT_TEST_FOLDERNAME = unitTestFolderName(Version.currentVersion());

	/** Folder for saving general output files */
	@NonNull public static final File DATASET_FOLDER = new File(DATASET_FOLDER_BASE,
			UNIT_TEST_FOLDERNAME);

	@NonNull public static final String MOUSE = "Mouse";
	@NonNull public static final String PIG = "Pig";
	@NonNull public static final String ROUND = "Round";

	@NonNull public static final String MULTIPLE1 = "Multiple_source_1";
	@NonNull public static final String MULTIPLE2 = "Multiple_source_2";

	@NonNull public static final String WITH_CLUSTERS = "_with_clusters";
	@NonNull public static final String WITH_SIGNALS = "_with_signals";

	@NonNull public static final File MOUSE_INPUT_FOLDER = new File(IMAGE_FOLDER_BASE.getAbsolutePath(),
			MOUSE);

	@NonNull public static final File PIG_INPUT_FOLDER = new File(IMAGE_FOLDER_BASE.getAbsolutePath(), PIG);
	@NonNull public static final File ROUND_INPUT_FOLDER = new File(IMAGE_FOLDER_BASE.getAbsolutePath(),
			ROUND);

	@NonNull public static final File MOUSE_OUTPUT_FOLDER = new File(MOUSE_INPUT_FOLDER,
			UNIT_TEST_FOLDERNAME);
	@NonNull public static final File PIG_OUTPUT_FOLDER = new File(PIG_INPUT_FOLDER, UNIT_TEST_FOLDERNAME);
	@NonNull public static final File ROUND_OUTPUT_FOLDER = new File(ROUND_INPUT_FOLDER,
			UNIT_TEST_FOLDERNAME);

	@NonNull public static final File MULTIPLE_BASE_FOLDER = new File(IMAGE_FOLDER_BASE, "Multiple");
	@NonNull public static final File MULTIPLE_SOURCE_1_FOLDER = new File(MULTIPLE_BASE_FOLDER, MULTIPLE1);
	@NonNull public static final File MULTIPLE_SOURCE_2_FOLDER = new File(MULTIPLE_BASE_FOLDER, MULTIPLE2);
	@NonNull public static final File MULTIPLE_BASE_OUTPUT_FOLDER = new File(MULTIPLE_BASE_FOLDER,
			UNIT_TEST_FOLDERNAME);

	@NonNull public static final File MOUSE_SIGNALS_INPUT_FOLDER = new File(IMAGE_FOLDER_BASE,
			MOUSE + WITH_SIGNALS);
	@NonNull public static final File MOUSE_SIGNALS_OUTPUT_FOLDER = new File(MOUSE_SIGNALS_INPUT_FOLDER,
			UNIT_TEST_FOLDERNAME);

	@NonNull public static final File PIG_SIGNALS_INPUT_FOLDER = new File(IMAGE_FOLDER_BASE,
			PIG + WITH_SIGNALS);
	@NonNull public static final File PIG_SIGNALS_OUTPUT_FOLDER = new File(PIG_SIGNALS_INPUT_FOLDER,
			UNIT_TEST_FOLDERNAME);

	@NonNull public static final File ROUND_SIGNALS_INPUT_FOLDER = new File(IMAGE_FOLDER_BASE,
			ROUND + WITH_SIGNALS);
	@NonNull public static final File ROUND_SIGNALS_OUTPUT_FOLDER = new File(ROUND_SIGNALS_INPUT_FOLDER,
			UNIT_TEST_FOLDERNAME);

	@NonNull public static final File MOUSE_CLUSTERS_INPUT_FOLDER = new File(IMAGE_FOLDER_BASE,
			MOUSE + WITH_CLUSTERS);
	@NonNull public static final File MOUSE_CLUSTERS_OUTPUT_FOLDER = new File(MOUSE_CLUSTERS_INPUT_FOLDER,
			UNIT_TEST_FOLDERNAME);

	@NonNull public static final File PIG_CLUSTERS_INPUT_FOLDER = new File(IMAGE_FOLDER_BASE,
			PIG + WITH_CLUSTERS);
	@NonNull public static final File PIG_CLUSTERS_OUTPUT_FOLDER = new File(PIG_CLUSTERS_INPUT_FOLDER,
			UNIT_TEST_FOLDERNAME);

	@NonNull public static final File ROUND_CLUSTERS_INPUT_FOLDER = new File(IMAGE_FOLDER_BASE,
			ROUND + WITH_CLUSTERS);
	@NonNull public static final File ROUND_CLUSTERS_OUTPUT_FOLDER = new File(ROUND_CLUSTERS_INPUT_FOLDER,
			UNIT_TEST_FOLDERNAME);

	@NonNull public static final File MOUSE_TEST_DATASET = new File(MOUSE_OUTPUT_FOLDER,
			MOUSE + Io.NMD_FILE_EXTENSION);
	@NonNull public static final File PIG_TEST_DATASET = new File(PIG_OUTPUT_FOLDER,
			PIG + Io.NMD_FILE_EXTENSION);
	@NonNull public static final File ROUND_TEST_DATASET = new File(ROUND_OUTPUT_FOLDER,
			ROUND + Io.NMD_FILE_EXTENSION);

	@NonNull public static final File MULTIPLE1_TEST_DATASET = new File(MULTIPLE_BASE_OUTPUT_FOLDER,
			MULTIPLE1 + Io.NMD_FILE_EXTENSION);
	@NonNull public static final File MULTIPLE2_TEST_DATASET = new File(MULTIPLE_BASE_OUTPUT_FOLDER,
			MULTIPLE2 + Io.NMD_FILE_EXTENSION);

	@NonNull public static final File MOUSE_CLUSTERS_DATASET = new File(MOUSE_CLUSTERS_OUTPUT_FOLDER,
			MOUSE + WITH_CLUSTERS + Io.NMD_FILE_EXTENSION);
	@NonNull public static final File PIG_CLUSTERS_DATASET = new File(PIG_CLUSTERS_OUTPUT_FOLDER,
			PIG + WITH_CLUSTERS + Io.NMD_FILE_EXTENSION);
	@NonNull public static final File ROUND_CLUSTERS_DATASET = new File(ROUND_CLUSTERS_OUTPUT_FOLDER,
			ROUND + WITH_CLUSTERS + Io.NMD_FILE_EXTENSION);

	@NonNull public static final File MOUSE_SIGNALS_DATASET = new File(MOUSE_SIGNALS_OUTPUT_FOLDER,
			MOUSE + WITH_SIGNALS + Io.NMD_FILE_EXTENSION);
	@NonNull public static final File PIG_SIGNALS_DATASET = new File(PIG_SIGNALS_OUTPUT_FOLDER,
			PIG + WITH_SIGNALS + Io.NMD_FILE_EXTENSION);
	@NonNull public static final File ROUND_SIGNALS_DATASET = new File(ROUND_SIGNALS_OUTPUT_FOLDER,
			ROUND + WITH_SIGNALS + Io.NMD_FILE_EXTENSION);

	@NonNull public static final File MULTIPLE_SOURCE_1_DATASET = new File(MULTIPLE_BASE_OUTPUT_FOLDER,
			MULTIPLE1 + Io.NMD_FILE_EXTENSION);
	@NonNull public static final File MULTIPLE_SOURCE_2_DATASET = new File(MULTIPLE_BASE_OUTPUT_FOLDER,
			MULTIPLE2 + Io.NMD_FILE_EXTENSION);

	@NonNull public static final File GLCM_FOLDER = new File(IMAGE_FOLDER_BASE, "GLCM");

	@NonNull public static final File GLCM_SAMPLE_IMAGE = new File(GLCM_FOLDER,
			"s61.tiff-3_b43854f1-95c5-4810-bbac-a467ef5dd0ed.tiff");

	@NonNull public static final File WARPING_FOLDER = new File(IMAGE_FOLDER_BASE, "Warping");

	@NonNull public static final File WARPING_NORMALISATION_IMAGE = new File(WARPING_FOLDER,
			"Gradient.tiff");

	 
	@NonNull public static final File TEXT_OUTLINES_FOLDER = new File(IMAGE_FOLDER_BASE, "Text");

	@NonNull public static final String WORKSPACE_NAME = "Example.wrk";
	
	
	/**
	 * Get the path to the dataset output folder for the current software version
	 * @return
	 */
	@NonNull public static final File datasetOutputFolder() {
		return new File( DATASET_FOLDER_BASE, unitTestFolderName(Version.currentVersion()));
	}
	
	/**
	 * Get the path to the image output folder for the given dataset name
	 * @param datasetName
	 * @return
	 */
	@NonNull public static final File imageOutputFolder(String datasetName) {
		return new File( new File(IMAGE_FOLDER_BASE, datasetName), unitTestFolderName(Version.currentVersion()));
	}

	/**
	 * Return the dataset unit test folder name for the given version
	 * 
	 * @param version
	 * @return
	 */
	@NonNull public static final String unitTestFolderName(@NonNull Version version) {
		return "UnitTest_" + version;
	}

	/**
	 * Return the unit test output folder for the given image set and version
	 * 
	 * @param imageset the image set e.g. Mouse
	 * @param version  the software version
	 * @return
	 */
	@NonNull public static final File outputFolder(@NonNull String imageset, @NonNull Version version) {
		return new File(imageset, unitTestFolderName(version));
	}

}
