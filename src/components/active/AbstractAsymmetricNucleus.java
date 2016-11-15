package components.active;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import components.active.generic.UnprofilableObjectException;
import components.generic.IPoint;
import components.nuclear.IBorderPoint;
import components.nuclei.Nucleus;
import ij.gui.Roi;

/**
 * The class of non-round nuclei from which all other assymetric nuclei derive
 * @author ben
 * @since 1.13.3
 */
public abstract class AbstractAsymmetricNucleus extends DefaultNucleus {
	
	private static final long serialVersionUID = 1L;
	
	private transient List<IBorderPoint> tailEstimatePoints = new ArrayList<IBorderPoint>(3); // holds the points considered to be sperm tails before filtering
	protected transient boolean clockwiseRP = false; // is the original orientation of the nucleus with RP clockwise to the CoM, or not
	
	/**
	 * Construct with an ROI, a source image and channel, and the original position in the source image
	 * @param roi
	 * @param f
	 * @param channel
	 * @param position
	 * @param centreOfMass
	 */
	public AbstractAsymmetricNucleus(Roi roi, IPoint centreOfMass, File f, int channel, int[] position, int number){
		super(roi, centreOfMass, f, channel, position, number );
	}

	protected AbstractAsymmetricNucleus(Nucleus n) throws UnprofilableObjectException {
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
