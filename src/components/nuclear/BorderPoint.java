/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/


package components.nuclear;

import components.generic.XYPoint;

/**
 *  This class contains border points around the periphery of a nucleus.
 *	Mostly the same as an XYPoint now, after creation of Profiles. It does
 * allow linkage of points, but this is not yet used
 *
 */
/**
 * @author ben
 *
 */
public class BorderPoint
	extends components.generic.XYPoint {

	private static final long serialVersionUID = 1L;
	
	
	private BorderPoint prevPoint = null;
	private BorderPoint nextPoint = null;
	
	/**
	 * Construct from x and y positions 
	 * @param x
	 * @param y
	 */
	public BorderPoint( double x, double y){
		super(x, y);
	}

	/**
	 * Construct from an existing XY point
	 * @param p
	 */
	public BorderPoint( XYPoint p){
		super(p);
	}
	
	/**
	 * Construct from an existing border point
	 * @param p
	 */
	public BorderPoint( BorderPoint p){
		super(p);
	}
	
	/**
	 * Set the next point in the border
	 * @param next
	 */
	public void setNextPoint(BorderPoint next){
		this.nextPoint = next;
	}
	
	
	/**
	 * Set the previous point in the border
	 * @param prev
	 */
	public void setPrevPoint(BorderPoint prev){
		this.prevPoint = prev;
	}
	
	public BorderPoint nextPoint(){
		return this.nextPoint;
	}
	
	/**
	 * Get the point n points ahead
	 * @param points
	 * @return
	 */
	public BorderPoint nextPoint(int points){
		if(points==1)
			return this.nextPoint;
		else {
			return nextPoint.nextPoint(--points);
		}
	}
	
	public BorderPoint prevPoint(){
		return this.prevPoint;
	}
	
	public BorderPoint prevPoint(int points){
		if(points==1)
			return this.prevPoint;
		else {
			return prevPoint.prevPoint(--points);
		}
	}
	
	public boolean hasNextPoint(){
		if(this.nextPoint()!=null){
			return  true;
		} else {
			return  false;
		}
	}
	
	public boolean hasPrevPoint(){
		if(this.prevPoint()!=null){
			return  true;
		} else {
			return  false;
		}
	}
	
//	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
//		finest("\t\tReading BorderPoint");
//		in.defaultReadObject();
//		finest("\t\tRead BorderPoint");
//	}
//
//	private void writeObject(ObjectOutputStream out) throws IOException {
//		finest("\t\tWriting BorderPoint");
//		out.defaultWriteObject();
//		finest("\t\tWrote BorderPoint");
//	}

//	private void writeObject(ObjectOutputStream stream) throws IOException {
//		
//		// Fields from XYPoint
//		stream.writeDouble(x);
//		stream.writeDouble(y);
//		
//		
//		
//		BorderPoint next = nextPoint;
//		while (next != null) {
//			stream.writeBoolean(true);
//			stream.writeDouble(next.x);
//			stream.writeDouble(next.y);
//			next = next.nextPoint();
//		}
//		stream.writeBoolean(false);
//		
////		private BorderPoint prevPoint = null;
////		private BorderPoint nextPoint = null;
////		
////        stream.writeObject(beforeWindowData);
////        stream.writeObject(onAfterWindowData);
////        stream.writeObject(outputData);
////        stream.writeObject(type);
////        stream.writeLong(timestamp);
////        StreamEvent nextEvent = next;
////        while (nextEvent != null) {
////            stream.writeBoolean(true);
////            stream.writeObject(nextEvent.beforeWindowData);
////            stream.writeObject(nextEvent.onAfterWindowData);
////            stream.writeObject(nextEvent.outputData);
////            stream.writeObject(nextEvent.type);
////            stream.writeLong(nextEvent.timestamp);
////            nextEvent = nextEvent.getNext();
////        }
////        stream.writeBoolean(false);
//    }


//    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
//    	
////    	BorderPoint prev;
////    	x = stream.readDouble();
////    	y = stream.readDouble();
////    	prev = this;
////    	boolean isNextAvailable = stream.readBoolean();
////    	
////    	while (isNextAvailable) {
////    		BorderPoint next = new BorderPoint(0, 0);
////    		next.x = stream.readDouble();
////    		next.y = stream.readDouble();
////    		prev.nextPoint = next;
////    		prev = next;
////    		isNextAvailable = stream.readBoolean();
////    	}
//		
////        StreamEvent previousStreamEvent;
////        beforeWindowData = (Object[]) stream.readObject();
////        onAfterWindowData = (Object[]) stream.readObject();
////        outputData = (Object[]) stream.readObject();
////        type = (Type) stream.readObject();
////        timestamp = stream.readLong();
////        previousStreamEvent = this;
////        boolean isNextAvailable = stream.readBoolean();
////        while (isNextAvailable) {
////            StreamEvent nextEvent = new StreamEvent(0, 0, 0);
////            nextEvent.beforeWindowData = (Object[]) stream.readObject();
////            nextEvent.onAfterWindowData = (Object[]) stream.readObject();
////            nextEvent.outputData = (Object[]) stream.readObject();
////            nextEvent.type = (Type) stream.readObject();
////            nextEvent.timestamp = stream.readLong();
////            previousStreamEvent.next = nextEvent;
////            previousStreamEvent = nextEvent;
////            isNextAvailable = stream.readBoolean();
////        }
//    }

}