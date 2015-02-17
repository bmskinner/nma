package nucleusAnalysis;

import ij.IJ;
import ij.gui.Roi;
import java.util.*;

/*
  -----------------------
  NUCLEUS SIGNAL CLASS
  -----------------------
  Contains the variables for storing a signal within the nucleus
*/  
public class NuclearSignal {

  private double area;
  private double perimeter;
  private double feret;
  private double angleFromTail;
  private double distanceFromCentreOfMass; // the absolute measured distance from the signal CoM to the nuclear CoM
  private double fractionalDistanceFromCoM; // the distance to the centre of mass as a fraction of the distance from the CoM to the closest border

  private XYPoint centreOfMass;
  private NucleusBorderPoint closestNuclearBorderPoint;

  private Roi roi;

  public NuclearSignal(Roi roi, double area, double feret, double perimeter, XYPoint centreOfMass){
    this.roi = roi;
    this.area = area;
    this.perimeter = perimeter;
    this.feret = feret;
    this.centreOfMass = centreOfMass;
  }

  public Roi getRoi(){
    return this.roi;
  }

  public double getArea(){
    return this.area;
  }

  public double getPerimeter(){
    return this.perimeter;
  }

  public double getFeret(){
    return this.feret;
  }

  public double getAngle(){
    return this.angleFromTail;
  }

  public double getDistance(){
    return this.distanceFromCentreOfMass;
  }

  public double getFractionalDistance(){
    return this.fractionalDistanceFromCoM;
  }

  public XYPoint getCentreOfMass(){
    return this.centreOfMass;
  }

  public NucleusBorderPoint getClosestBorderPoint(){
    return this.closestNuclearBorderPoint;
  }

  public void setArea(double d){
    this.area = d;
  }

  public void setPerimeter(double d){
    this.perimeter = d;
  }

  public void setFeret(double d){
    this.feret = d;
  }

  public void setAngle(double d){
    this.angleFromTail = d;
  }

  public void setDistance(double d){
    this.distanceFromCentreOfMass = d;
  }

  public void setFractionalDistance(double d){
    this.fractionalDistanceFromCoM = d;
  }

  public void setCentreOfMass(XYPoint p){
    this.centreOfMass = p;
  }

  public void setClosestBorderPoint(NucleusBorderPoint p){
    this.closestNuclearBorderPoint = p;
  }

  /*
    Assuming the signal were a perfect circle of area equal
    to the measured area, get the radius for that circle
  */
  public double getRadius(){
    // r = sqrt(a/pi)
    return Math.sqrt(this.area/Math.PI);
  }
}