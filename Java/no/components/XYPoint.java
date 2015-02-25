/*
  -----------------------
  XY POINT CLASS
  -----------------------
  This class contains the X and Y coordinates of a point as doubles,
  plus any angles calculated for that point. 
  Also contains methods for determining distance and overlap with other points
*/

package no.components;

import java.util.*;

public class XYPoint {
  private double x;
  private double y;
  
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