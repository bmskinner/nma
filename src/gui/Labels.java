package gui;

/**
 * Hold tool tip labels for the various options panels
 * @author bms41
 *
 */
public class Labels {

	/*
	 * signal detection
	 */
	public static final String FORWARD_THRESHOLDING_RADIO_LABEL = "Take all objects with pixels over the threshold, meeting the size requirements. If there is a lot of bright background, it can mistake this for signal.";

	public static final String REVERSE_THRESHOLDING_RADIO_LABEL = "Starts with the brightest pixels (intensity 255), and tries to detect objects meeting size and shape criteria. If it fails, it looks at pixels with intensity 254 or above. "
			+ "This recurses until either a signal is found, or the signal threshold is reached.";

	public static final String ADAPTIVE_THRESHOLDING_RADIO_LABEL = "<html>The intensity histogram within the nuclear bounding box is trimmed to the minimum <br>"
			+ "signal threshold defined in the options, <br>"
			+ "then scanned for the position with maximum dropoff <br>"
			+ "Formally, in the delta profile, this is the local minimum <br>"
			+ "(a) below zero <br>"
			+ "(b) with an absolute value greater than 10% of the total intensity range of the trimmed profile <br>"
			+ "(c) with the highest index). <br>"
			+ "Since this position lies in the middle of the dropoff, <br>"
			+ "a (currently) fixed offset is added to the index to remove remaining background. <br>"
			+ "This index is used as the new threshold for the detector. <br>"
			+ "If a suitable position is not found, we fall back to the <br>"
			+ "minimum signal threshold defined in the options.</html>";
	
	public static final String MINIMUM_SIGNAL_AREA = "The smallest number of pixels a signal can contain";
	public static final String MAXIMUM_SIGNAL_FRACTION = "The largest size of a signal, as a fraction of the nuclear area (0-1)";


	/*
	 * Clustering and tree building
	 */
	public static final String HIERARCHICAL_CLUSTER_METHOD = "The hierarchical clustering algorithm to run";
	public static final String USE_MODALITY_REGIONS = "Should profile angles with the lowest dip-test p-values "
			+ "be used in the clustering";
	public static final String NUMBER_MODALITY_REGIONS = "The number of dip-test p-values to "
			+ "be used in the clustering";
	public static final String USE_SIMILARITY_MATRIX = "<html>If selected, use the difference between each nucleus profile<br>"
			+ "and every other nucleus for clustering.<br>Otherwise, use area, circularity and aspect ratio</html>";

}
