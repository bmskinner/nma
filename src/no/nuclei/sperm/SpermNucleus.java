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

import ij.gui.Roi;

import java.io.File;

import no.nuclei.*;

public class SpermNucleus
	extends AsymmetricNucleus
{
	
  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

/**
  * Constructor using a Nucleus; passes up
  * to the Nucleus constructor
  *
  * @param n the Nucleus to construct from
  * @return a SpermNucleus
 * @throws Exception 
  */
  public SpermNucleus(RoundNucleus n) throws Exception{
  	super(n);
  }
  
  public SpermNucleus (Roi roi, File file, int number, double[] position) { // construct from an roi
		super(roi, file, number, position);
	}


}