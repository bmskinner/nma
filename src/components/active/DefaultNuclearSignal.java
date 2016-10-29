package components.active;

import java.awt.Rectangle;
import java.awt.Shape;
import java.io.File;
import java.util.List;
import java.util.UUID;

import components.CellularComponent;
import components.generic.IPoint;
import components.generic.MeasurementScale;
import components.nuclear.IBorderPoint;
import components.nuclear.INuclearSignal;
import components.nuclear.NuclearSignal;
import ij.gui.Roi;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import io.UnloadableImageException;
import stats.PlottableStatistic;

public class DefaultNuclearSignal 
	extends DefaultCellularComponent 
	implements INuclearSignal {
	
	private static final long serialVersionUID = 1L;

	private int closestNuclearBorderPoint;
		
	public DefaultNuclearSignal(Roi roi, File f, int channel, int[] position, IPoint centreOfMass){
		super(roi, f, channel, position, centreOfMass);
		
	}
	
	/**
	 * Create a copy of the given signal
	 * @param n
	 */
	public DefaultNuclearSignal(INuclearSignal n){
		super(n);

		this.closestNuclearBorderPoint = n.getClosestBorderPoint();

	}	

	/* (non-Javadoc)
	 * @see components.nuclear.INuclearSignal#getClosestBorderPoint()
	 */
	@Override
	public int getClosestBorderPoint(){
		return this.closestNuclearBorderPoint;
	}

	/* (non-Javadoc)
	 * @see components.nuclear.INuclearSignal#setClosestBorderPoint(int)
	 */
	@Override
	public void setClosestBorderPoint(int p){
		this.closestNuclearBorderPoint = p;
	}
	
	/* (non-Javadoc)
	 * @see components.nuclear.INuclearSignal#duplicate()
	 */
	@Override
	public INuclearSignal duplicate() {
		return new DefaultNuclearSignal(this);
	}

	@Override
	public void alignVertically() {
		// TODO Auto-generated method stub
		
	}

}
