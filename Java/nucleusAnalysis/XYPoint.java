/*
  -----------------------
  XY POINT CLASS
  -----------------------
  This class contains the X and Y coordinates of a point as doubles,
  plus any angles calculated for that point. 
  Also contains methods for determining distance and overlap with other points
*/

package nucleusAnalysis;

import java.util.*;

public class XYPoint {
  private double x;
  private double y;
  private double minAngle;
  private double interiorAngle; // depends on whether the min angle is inside or outside the shape
  private double interiorAngleDelta; // this will hold the difference between a previous interiorAngle and a next interiorAngle
  private double interiorAngleDeltaSmoothed; // holds delta from a 5-window average centred on this point

  private boolean localMin; // is this angle a local minimum based on the minAngle
  private boolean localMax; // is this angle a local maximum based on the interior angle

  public XYPoint (double x, double y){
    this.x = x;
    this.y = y;
  }

  public double getX(){
    return this.x;
  }
  public double getY(){
    return this.y;
  }

  public int getXAsInt(){
    return (int)this.x;
  }

  public int getYAsInt(){
    return (int)this.y;
  }

  public void setX(double x){
    this.x = x;
  }

  public void setY(double y){
    this.y = y;
  }

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

  public double getLengthTo(XYPoint a){

    // a2 = b2 + c2
    double dx = Math.abs(this.getX() - a.getX());
    double dy = Math.abs(this.getY() - a.getY());
    double dx2 = dx * dx;
    double dy2 = dy * dy;
    double length = Math.sqrt(dx2+dy2);
    return length;
  }

  public boolean overlaps(XYPoint a){
    if( this.getXAsInt() == a.getXAsInt() && this.getYAsInt() == a.getYAsInt()){
      return true;
    } else {
      return false;
    }
  }

  public String toString(){
    return this.getXAsInt()+","+this.getYAsInt();
  }

}