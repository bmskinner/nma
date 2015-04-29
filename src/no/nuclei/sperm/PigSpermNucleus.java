  /*
  -----------------------
  PIG SPERM NUCLEUS CLASS
  -----------------------
*/  
package no.nuclei.sperm;

import no.nuclei.*;
import no.components.*;

public class PigSpermNucleus 
    extends SpermNucleus 
  {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
    * The point of the nucleus orthogonal to the narrowest
    * diameter, through the centre of mass.
    * Used in tail identification functions only.
    */
    private NucleusBorderPoint orthPoint1;

  /**
  * Constructor using a Nucleus; passes up
  * to the SpermNucleus constructor
  *
  * @param n the Nucleus to construct from
  */
    public PigSpermNucleus(RoundNucleus n){
      super(n);
    }

    /**
    * Empty constructor. Can be used for class
    * identification (as in AnalysisCreator) 
    */
    public PigSpermNucleus(){

    }

  /**
  * {@inheritDoc}
  * <p>
  * This method overrides the Nucleus method, and uses a single measure
  * to find the pig sperm tail. This is using the maximum angle in the 
  * Profile.
  */
    @Override
    public void findPointsAroundBorder(){

      NucleusBorderPoint tailPoint1 = this.findTailByMinima();
      int tailPointIndex2 = this.findTailByMaxima();
      NucleusBorderPoint tailPoint2 = this.getBorderPoint(tailPointIndex2);
      
      NucleusBorderPoint tailPoint3 = this.findTailByNarrowestPoint();

      this.addTailEstimatePosition(tailPoint1);
      this.addTailEstimatePosition(tailPoint2);
      this.addTailEstimatePosition(tailPoint3);

      // int consensusTailIndex = this.getPositionBetween(tailPoint2, tailPoint3);
      // NucleusBorderPoint consensusTail = this.getBorderPoint(consensusTailIndex);
      // consensusTailIndex = this.getPositionBetween(consensusTail, tailPoint1);
      // consensusTail = this.getBorderPoint(consensusTailIndex);

      int consensusTailIndex = this.getIndex(tailPoint2);
      NucleusBorderPoint consensusTail = this.getBorderPoint(consensusTailIndex);

      addBorderTag("tail", consensusTailIndex);

      int headIndex = getIndex(this.findOppositeBorder(consensusTail));
      addBorderTag("head", headIndex);
    }

    /*
      -----------------------
      Methods for detecting the head and tail
      -----------------------
    */

    /**
    * This method attempts to find the pig sperm tail point
    * using the lowest angles in the Profile. The two lowest
    * minima should be either side of the tail. Not currently
    * used.
    * @see Profile
    */
    public NucleusBorderPoint findTailByMinima(){

      // the two lowest minima are at the tail-end corners. 
      // between them lies the tail. Find the two lowest minima,
      // and get the point between them

      Profile minima = this.getAngleProfile().getLocalMinima(5);

      // sort minima by interior angle
      int lowestMinima = 0;
      int secondLowestMinima = 0;

      for(int i=0; i<minima.size();i++){
        if(minima.get(i)==1){
          if (this.getAngle(i)<this.getAngle(lowestMinima)){
            secondLowestMinima = lowestMinima;
            lowestMinima = i;
          }
        }
      }
      for(int i=0; i<minima.size();i++){
        if(minima.get(i)==1){
          if (this.getAngle(i)<this.getAngle(secondLowestMinima) && 
              this.getAngle(i)>this.getAngle(lowestMinima)){
            secondLowestMinima = i;
          }
        }
      }

      NucleusBorderPoint a = this.getBorderPoint(lowestMinima);
      NucleusBorderPoint b = this.getBorderPoint(secondLowestMinima);

      NucleusBorderPoint tailPoint = this.getBorderPoint(this.getPositionBetween(a, b));
      return tailPoint;
    }

    /**
    * This method attempts to find the pig sperm tail point
    * using the highest angle in the Profile. The highest point 
    * should be at the tail, since the nucleus has an inward 'dent'
    * @see Profile
    */
    public int findTailByMaxima(){
      // the tail is the ?only local maximum with an interior angle above the 180 line

     int tailPoint = this.getAngleProfile().getIndexOfMax();

     //  Profile maxima = this.getAngleProfile().getLocalMaxima(5);
     //  int tailPoint = (int)maxima.get(0);

     //  double maxAngle = 170;

     // for(int i=0; i<maxima.size();i++){
     //    if(maxima.get(i)==1){
     //      if (this.getAngle(i)>maxAngle){
     //        tailPoint = i;
     //      }
     //    }
     //  }
      return tailPoint;
    }


    /**
    * This method attempts to find the pig sperm tail point
    * using the narrowest point across the centre of mass.
    * The narrowest diameter through the CoM is orthogonal to 
    * the tail. Of the two orthogonal border points, the point with
    * the highest angle is the tail.
    * @see Profile
    */
    public NucleusBorderPoint findTailByNarrowestPoint(){

      NucleusBorderPoint narrowPoint = this.getNarrowestDiameterPoint();
      this.orthPoint1  = this.findOrthogonalBorderPoint(narrowPoint);
      NucleusBorderPoint orthPoint2  = this.findOppositeBorder(orthPoint1);

      // NucleusBorderPoint[] array = { orthPoint1, orthPoint2 };

      // the tail should be a maximum, hence have a high angle
      NucleusBorderPoint tailPoint  = getAngle(this.getIndex(orthPoint1)) >
                                      getAngle(this.getIndex(orthPoint2))
                                    ? orthPoint1
                                    : orthPoint2;
      return tailPoint;
    }

}