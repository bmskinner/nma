/*
  -----------------------
  BORDER POINT CLASS
  -----------------------
  This class contains the points around the periphery of a nucleus.
  It holds angle measurements from profiling 

*/

package no.components;

public class NucleusBorderPoint
	extends no.components.XYPoint 
{

  private double minAngle;
  private double interiorAngle; // depends on whether the min angle is inside or outside the shape
  private double interiorAngleDelta; // this will hold the difference between a previous interiorAngle and a next interiorAngle
  private double interiorAngleDeltaSmoothed; // holds delta from a 5-window average centred on this point
  private double distanceAcrossCoM;

  private boolean localMin; // is this angle a local minimum based on the minAngle
  private boolean localMax; // is this angle a local maximum based on the interior angle

	private int index; // keep the original index position in case we need to change
  private int numberOfConsecutiveBlocks; // holds the number of interiorAngleDeltaSmootheds > 1 degree after this point
  private int blockNumber; // identifies the group of consecutive blocks this point is part of
  private int blockSize; // the total number of points within the block
//  private int positionWithinBlock; // stores the place within the block starting at 0

//  private boolean isMidpoint; // is this point the midpoint of a block

  public NucleusBorderPoint( double x, double y){
  	super(x, y);
  }

  public NucleusBorderPoint( NucleusBorderPoint p){
    super(p.getX(), p.getY());
    this.setMinAngle(p.getMinAngle());
    this.setInteriorAngle(p.getInteriorAngle());
    this.setInteriorAngleDelta(p.getInteriorAngleDelta());
    this.setInteriorAngleDeltaSmoothed(p.getInteriorAngleDeltaSmoothed());
    this.setDistanceAcrossCoM(p.getDistanceAcrossCoM());
    this.setLocalMin(p.isLocalMin());
    this.setLocalMax(p.isLocalMax());
    this.setIndex(p.getIndex());
    this.setConsecutiveBlocks(p.getConsecutiveBlocks());
    this.setBlockNumber(p.getBlockNumber());
    this.setBlockSize(p.getBlockSize());
//    this.setPositionWithinBlock(p.getPositionWithinBlock());
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

//  public int getPositionWithinBlock(){
//    return this.positionWithinBlock;
//  }
//
//  public void setPositionWithinBlock(int i){
//    this.positionWithinBlock = i;
//  }

  public boolean isBlock(){
      if(this.blockNumber>0){
        return true;
      } else {
        return false;
      }
    }

//  public void setMidpoint(){
//    int midpoint  = (int)Math.floor(this.getBlockSize()/2);
//    if(this.getPositionWithinBlock() == midpoint){
//      this.isMidpoint = true;
//    } else {
//      this.isMidpoint =false;
//    }
//  }

//  public boolean isMidpoint(){
//    int midpoint  = (int)Math.floor(this.getBlockSize()/2);
//    if(this.getPositionWithinBlock() == midpoint && this.getPositionWithinBlock() !=0){
//      return true;
//    } else {
//      return false;
//    }
//  }

  public double getMinAngle(){
    return this.minAngle;
  }

  public void setMinAngle(double d){
    this.minAngle = d;
  }

  public double getInteriorAngle(){
    return this.interiorAngle;
  }

  public void setInteriorAngle(double d){
    this.interiorAngle = d;
  }

   public double getInteriorAngleDelta(){
    return this.interiorAngleDelta;
  }

  public void setInteriorAngleDelta(double d){
    this.interiorAngleDelta = d;
  }

  public double getInteriorAngleDeltaSmoothed(){
    return this.interiorAngleDeltaSmoothed;
  }

  public void setInteriorAngleDeltaSmoothed(double d){
    this.interiorAngleDeltaSmoothed = d;
  }

  public double getDistanceAcrossCoM(){
    return this.distanceAcrossCoM;
  }

  public void setDistanceAcrossCoM(double d){
    this.distanceAcrossCoM = d;
  }

  public void setLocalMin(boolean b){
    this.localMin = b;
  }

  public void setLocalMax(boolean b){
    this.localMax = b;
  }

  public boolean isLocalMin(){
    return this.localMin;
  }

  public boolean isLocalMax(){
    return this.localMax;
  }
}