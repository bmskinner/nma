package no.nuclei;

import java.io.Serializable;

import no.components.NucleusBorderPoint;
import no.components.XYPoint;
import utility.Utils;

/**
 * This holds methods for manipulatiing a refolded consensus nucleus
 *
 */
public class ConsensusNucleus extends RoundNucleus implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private Class<?> type;
	
	
	public ConsensusNucleus(Nucleus n, Class<?> type) throws Exception{
		
		super(  (RoundNucleus) n);
		this.type = type;
	}
	
	public Class<?> getType(){
		return this.type;
	}
		
	/**
	 * Test the effect on a given point's position of rotating the nucleus 
	 * @param point the point of interest
	 * @param angle the angle of rotation
	 * @return the new position
	 */
	private XYPoint getPositionAfterRotation(NucleusBorderPoint point, double angle){
		
		// get the angle from the tail to the vertical axis
		double tailAngle = findAngleBetweenXYPoints( point, this.getCentreOfMass(), new XYPoint(0,-10));
		if(point.getX()<0){
			tailAngle = 360-tailAngle; // correct for measuring the smallest angle
		}
		// get a copy of the new bottom point
		XYPoint p = new XYPoint( point.getX(), point.getY() );

		// get the distance from the bottom point to the CoM
		double distance = p.getLengthTo(this.getCentreOfMass());

		// add the suggested rotation amount
		double newAngle = tailAngle + angle;

		// get the new X and Y coordinates of the point after rotation
		double newX = Utils.getXComponentOfAngle(distance, newAngle);
		double newY = Utils.getYComponentOfAngle(distance, newAngle);
		return new XYPoint(newX, newY);
	}
	
	/**
	 * Rotate the nucleus so that the given point is directly 
	 * below the centre of mass
	 * @param bottomPoint
	 */
	public void rotatePointToBottom(NucleusBorderPoint bottomPoint){

		// find the angle to rotate
		double angleToRotate 	= 0;

		// start with a high distance from the central vertical line
		double distanceFromZero = 180;

		// Go around in a circle
		for(int angle=0;angle<360;angle++){

			XYPoint newPoint = getPositionAfterRotation(bottomPoint, angle);

			// if the new x position is closer to the central vertical line
			// AND the y position is below zero
			// this is a better rotation
			if(Math.abs(newPoint.getX()) < distanceFromZero && newPoint.getY() < 0){
				angleToRotate = angle;
				distanceFromZero = Math.abs(newPoint.getX());
			}
		}
		this.rotate(angleToRotate);
	}
	
	/**
	 * Given two points in the nucleus, rotate the nucleus so that they are vertical.
	 * @param topPoint the point to have the higher Y value
	 * @param bottomPoint the point to have the lower Y value
	 */
	public void alignPointsOnVertical(NucleusBorderPoint topPoint, NucleusBorderPoint bottomPoint){
		
		double angleToRotate 	= 0;
		
		// get the angle from vertical of the line between the points
		// This is the line running from top to bottom, then up
		// to the y position of the top directly above the bottom 
		double angleToBeat = findAngleBetweenXYPoints( topPoint, 
				bottomPoint, 
				new XYPoint(bottomPoint.getX(),topPoint.getY()));
		
		for(int angle=0;angle<360;angle++){
			
			XYPoint newTop 		= getPositionAfterRotation(topPoint, angle);
			XYPoint newBottom 	= getPositionAfterRotation(bottomPoint, angle);
			
			double newAngle = findAngleBetweenXYPoints( newTop, 
					newBottom, 
					new XYPoint(newBottom.getX(),newTop.getY()));
			
			// We want to minimise the angle between the points, whereupon
			// they are vertically aligned. Also test that the top is still on the
			// top
			if(newAngle < angleToBeat && newTop.getY() > newBottom.getY()){
				angleToBeat = newAngle;
				angleToRotate = angle;
			}
		}
		this.rotate(angleToRotate);
	}
	
	/**
	 * Rotate the nucleus by the given amount around the centre of mass
	 * @param angle
	 */
	public void rotate(double angle){
		
		for(NucleusBorderPoint p : this.getBorderList()){

			double distance = p.getLengthTo(this.getCentreOfMass());
			double oldAngle = RoundNucleus.findAngleBetweenXYPoints( p, this.getCentreOfMass(), new XYPoint(0,-10));
			if(p.getX()<0){
				oldAngle = 360-oldAngle;
			}

			double newAngle = oldAngle + angle;
			double newX = Utils.getXComponentOfAngle(distance, newAngle);
			double newY = Utils.getYComponentOfAngle(distance, newAngle);

			this.updatePoint(this.getIndex(p), newX, newY);
		}
	}
	

	/**
	 * Translate the XY coordinates of each border point so that
	 * the nuclear centre of mass is at the given point
	 * @param point the new centre of mass
	 */
	public void moveCentreOfMass(XYPoint point){

		XYPoint centreOfMass = this.getCentreOfMass();
		double xOffset = centreOfMass.getX() - point.getX();
		double yOffset = centreOfMass.getY() - point.getY() ;

		this.setCentreOfMass(point);

		for(int i=0; i<this.getLength(); i++){
			XYPoint p = this.getBorderPoint(i);

			double x = p.getX() - xOffset;
			double y = p.getY() - yOffset;

			this.updatePoint(i, x, y );
		}
	}

}
