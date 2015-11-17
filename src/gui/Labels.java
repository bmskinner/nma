package gui;

public class Labels {

	public static final String FORWARD_THRESHOLDING_RADIO_LABEL = "Take all objects with pixels over the threshold, meeting the size requirements. If there is a lot of bright background, it can mistake this for signal.";

	public static final String REVERSE_THRESHOLDING_RADIO_LABEL = "Starts with the brightest pixels (intensity 255), and tries to detect objects meeting size and shape criteria. If it fails, it looks at pixels with intensity 254 or above. "
			+ "This recurses until either a signal is found, or the signal threshold is reached.";

	public static final String ADAPTIVE_THRESHOLDING_RADIO_LABEL = "The intensity histogram within the nuclear bounding box is trimmed to the minimum signal threshold defined in the options, "
			+ "then scanned for the position with maximum dropoff "
			+ "(formally, in the delta profile, the local minimum "
			+ "(a) below zero "
			+ "(b) with an absolute value greater than 10% of the total intensity range of the trimmed profile "
			+ "(c) with the highest index). "
			+ "Since this position lies in the middle of the dropoff, "
			+ "a (currently) fixed offset is added to the index to remove remaining background. "
			+ "This index is used as the new threshold for the detector. "
			+ "If a suitable position is not found, we fall back to the minimum signal threshold defined in the options.";
	
	public static final String MINIMUM_SIGNAL_AREA = "The smallest number of pixels a signal can contain";
	public static final String MAXIMUM_SIGNAL_FRACTION = "The largest size of a signal, as a fraction of the nuclear area (0-1)";
}
