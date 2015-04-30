/*
  -----------------------
  ASYMMETRIC NUCLEUS CLASS
  -----------------------
  Contains the variables for storing a non-circular nucleus.
  They have a head and a tail, hence can be oriented
  in one axis.

  A tail is the point determined via profile analysis. The
  head is assigned as the point opposite through the CoM.
*/  
package no.nuclei;


import java.util.ArrayList;
import java.util.List;

import no.components.*;

public class AsymmetricNucleus
  extends RoundNucleus
{

  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
private List<NucleusBorderPoint> tailEstimatePoints = new ArrayList<NucleusBorderPoint>(0); // holds the points considered to be sperm tails before filtering

  public AsymmetricNucleus(RoundNucleus n){
    super(n);
  }

  public AsymmetricNucleus(){

  }

  /*
    -----------------------
    Get nucleus features
    -----------------------
  */

  public List<NucleusBorderPoint> getEstimatedTailPoints(){
    return this.tailEstimatePoints;
  }

  /*
    -----------------------
    Set nucleus features
    -----------------------
  */

  protected void addTailEstimatePosition(NucleusBorderPoint p){
    this.tailEstimatePoints.add(p);
  }

  /*
    Find the angle that the nucleus must be rotated to make the CoM-tail vertical.
    Uses the angle between [sperm tail x,0], sperm tail, and sperm CoM
    Returns an angle
  */
  @Override
  public double findRotationAngle(){
    XYPoint end = new XYPoint(this.getBorderTag("tail").getXAsInt(),this.getBorderTag("tail").getYAsInt()-50);

    double angle = findAngleBetweenXYPoints(end, this.getBorderTag("tail"), this.getCentreOfMass());

    if(this.getCentreOfMass().getX() < this.getBorderTag("tail").getX()){
      return angle;
    } else {
      return 0-angle;
    }
  }
}