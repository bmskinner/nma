/*
  -----------------------
  SPERM NUCLEUS CLASS
  -----------------------
  Sperm have a head and a tail, hence can be oriented
  in one axis. This is inherited from the AsymmetricNucleus.
  Mostly empty for now, but analyses involving
  segments such as acrosomes may need common methods.
*/  
package no.nuclei.sperm;

import no.nuclei.*;

public class SpermNucleus
	extends AsymmetricNucleus
{
	
  /**
  * Constructor using a Nucleus; passes up
  * to the Nucleus constructor
  *
  * @param n the Nucleus to construct from
  * @return a SpermNucleus
  */
  public SpermNucleus(Nucleus n){
  	super(n);
  }

  /**
  * Empty constructor. Can be used for class
  * identification (as in AnalysisCreator) 
  *
  * @return an empty SpermNucleus
  */
  public SpermNucleus(){
    
  }

  /*
    -----------------------
    Get sperm nucleus features
    -----------------------
  */


  /*
    -----------------------
    Set sperm nucleus features
    -----------------------
  */


  /*
    -----------------------
    Get raw and normalised profile and values
    -----------------------
  */



  /*
    -----------------------
    Annotate features of the nucleus
    -----------------------
  */


  /*
    -----------------------
    Find rotations based on tail point
    -----------------------
  */

  /*
    Find the angle that the nucleus must be rotated to make the CoM-tail vertical.
    Uses the angle between [sperm tail x,0], sperm tail, and sperm CoM
    Returns an angle
  */

  /*
    -----------------------
    Measure signal positions
    -----------------------
  */

}