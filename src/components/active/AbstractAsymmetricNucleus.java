package components.active;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import components.generic.IPoint;
import components.nuclear.IBorderPoint;
import components.nuclei.Nucleus;
import ij.gui.Roi;

public abstract class AbstractAsymmetricNucleus extends DefaultNucleus {
	
	private static final long serialVersionUID = 1L;
	
	private transient List<IBorderPoint> tailEstimatePoints = new ArrayList<IBorderPoint>(3); // holds the points considered to be sperm tails before filtering
	protected transient boolean clockwiseRP = false; // is the original orientation of the nucleus with RP clockwise to the CoM, or not

	public AbstractAsymmetricNucleus(Roi roi, File f, int channel, int[] position, int number) {
		super(roi, f, channel, position, number);

	}
	
	/**
	 * Construct with an ROI, a source image and channel, and the original position in the source image
	 * @param roi
	 * @param f
	 * @param channel
	 * @param position
	 * @param centreOfMass
	 */
	public AbstractAsymmetricNucleus(Roi roi, File f, int channel, int[] position, int number, IPoint centreOfMass){
		super(roi, f, channel, position, number, centreOfMass );
	}

	protected AbstractAsymmetricNucleus(Nucleus n) {
		super(n);
	}


	public List<IBorderPoint> getEstimatedTailPoints(){
		return this.tailEstimatePoints;
	}

	protected void addTailEstimatePosition(IBorderPoint p){
		this.tailEstimatePoints.add(p);
	}

	@Override
	public boolean isClockwiseRP(){
		return this.clockwiseRP;
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		tailEstimatePoints = new ArrayList<IBorderPoint>(0);
		clockwiseRP = false;
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();  
	}
}
