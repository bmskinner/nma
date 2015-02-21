/*
  -----------------------
  Rodent XY POINT CLASS
  -----------------------
  This class contains the X and Y coordinates of a point as doubles,
  plus any angles calculated for that point. 
  Also contains methods for determining distance and overlap with other points
*/

package no.nuclei.sperm;

import java.util.*;

public class RodentXYPoint
	extends nucleusAnalysis.XYPoint 
{

	private int index; // keep the original index position in case we need to change
  private int numberOfConsecutiveBlocks; // holds the number of interiorAngleDeltaSmootheds > 1 degree after this point
  private int blockNumber; // identifies the group of consecutive blocks this point is part of
  private int blockSize; // the total number of points within the block
  private int positionWithinBlock; // stores the place within the block starting at 0

  private boolean isMidpoint; // is this point the midpoint of a block

  public RodentXYPoint( double x, double y){
  	super(x, y);
  }

  public int getIndex(){
    return this.index;
  }

  public void setIndex(int i){
  	this.index = i;
  }

  public int getConsecutiveBlocks(){
    return this.numberOfConsecutiveBlocks;
  }

  public void setConsecutiveBlocks(int i){
    this.numberOfConsecutiveBlocks = i;
  }

  public int getBlockNumber(){
    return this.blockNumber;
  }

  public void setBlockNumber(int i){
    this.blockNumber = i;
  }

  public int getBlockSize(){
    return this.blockSize;
  }

  public void setBlockSize(int i){
    this.blockSize = i;
  }

  public int getPositionWithinBlock(){
    return this.positionWithinBlock;
  }

  public void setPositionWithinBlock(int i){
    this.positionWithinBlock = i;
  }

  public boolean isBlock(){
      if(this.blockNumber>0){
        return true;
      } else {
        return false;
      }
    }

  public void setMidpoint(boolean b){
    this.isMidpoint = b;
  }

  public boolean isMidpoint(){
   
    int midpoint  = (int)Math.floor(this.getBlockSize()/2);
    if(this.getPositionWithinBlock() == midpoint){
      this.setMidpoint(true);
    } else {
      this.setMidpoint(false);
    }
    return this.isMidpoint;
  }
}