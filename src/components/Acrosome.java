package components;

import java.io.File;
import java.util.List;

import components.generic.XYPoint;

public interface Acrosome {


	public List<XYPoint> getSkeleton();

	/**
	 * Fetch the skeleton offset to zero
	 * @return
	 */
	public List<XYPoint> getOffsetSkeleton();

	public List<XYPoint> getBorder();

	// positions are offset by the bounding rectangle for easier plotting
	public List<XYPoint> getOffsetBorder();

	public double getLength();

	public File getSourceFile();

	public int getSourceChannel();

	public double[] getPosition();
}
