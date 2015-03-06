package no.components;

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
  private double angleFromReferencePoint;
  private double distanceFromCentreOfMass; // the absolute measured distance from the signal CoM to the nuclear CoM
  private double fractionalDistanceFromCoM; // the distance to the centre of mass as a fraction of the distance from the CoM to the closest border

  private XYPoint centreOfMass;
  private int closestNuclearBorderPoint;

  private Roi roi;

  public NuclearSignal(Roi roi, double area, double feret, double perimeter, XYPoint centreOfMass){
    this.roi = roi;
    this.area = area;
    this.perimeter = perimeter;
    this.feret = feret;
    this.centreOfMass = centreOfMass;
  }

  public NuclearSignal(NuclearSignal n){
    this.roi = n.getRoi();
    this.area = n.getArea();
    this.perimeter = n.getPerimeter();
    this.feret = n.getFeret();
    this.centreOfMass = new XYPoint(n.getCentreOfMass());
  }

  /*
    -----------------------
    Getters for basic values within nucleus
    -----------------------
  */
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
    return this.angleFromReferencePoint;
  }

  public double getDistanceFromCoM(){
    return this.distanceFromCentreOfMass;
  }

  public double getFractionalDistanceFromCoM(){
    return this.fractionalDistanceFromCoM;
  }

  public XYPoint getCentreOfMass(){
    return new XYPoint(this.centreOfMass);
  }

  public int getClosestBorderPoint(){
    return this.closestNuclearBorderPoint;
  }

  /*
    Assuming the signal were a perfect circle of area equal
    to the measured area, get the radius for that circle
  */
  public double getRadius(){
    // r = sqrt(a/pi)
    return Math.sqrt(this.area/Math.PI);
  }

  /*
    -----------------------
    Setters for externally calculated values
    -----------------------
  */
  public void setAngle(double d){
    this.angleFromReferencePoint = d;
  }

  public void setDistanceFromCoM(double d){
    this.distanceFromCentreOfMass = d;
  }

  public void setFractionalDistanceFromCoM(double d){
    this.fractionalDistanceFromCoM = d;
  }

  public void setClosestBorderPoint(int p){
    this.closestNuclearBorderPoint = p;
  }
}