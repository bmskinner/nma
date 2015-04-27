package no.export;

import ij.IJ;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import no.collections.INuclearCollection;

public class PopulationExporter {

	public static void savePopulation(INuclearCollection collection){

		File saveFile = new File(collection.getOutputFolder()+File.separator+collection.getType()+".sav");
		if(saveFile.exists()){
			saveFile.delete();
		}

		try{
			//use buffering
			OutputStream file = new FileOutputStream(saveFile);
			OutputStream buffer = new BufferedOutputStream(file);
			ObjectOutputStream output = new ObjectOutputStream(buffer);
			
			IJ.log("    Saving data to file...");

			try{
				
				output.writeObject(collection.getFolder());
				output.writeObject(collection.getOutputFolderName());
				output.writeObject(collection.getType());
				output.writeObject(collection.getNuclei());
				
				IJ.log("    Save complete");

			} catch(IOException e){
				IJ.log("    Unable to save nuclei: "+e.getMessage());
				for(StackTraceElement el : e.getStackTrace()){
					IJ.log(el.toString());
				}
				
			} finally{
				output.close();
				buffer.close();
				file.close();
			}

		} catch(Exception e){
			IJ.log("    Error in saving: "+e.getMessage());
			for(StackTraceElement el : e.getStackTrace()){
				IJ.log(el.toString());
			}
		}
	}

//	private static void writeNucleus(INuclearFunctions n, ObjectOutputStream oos) throws IOException {
//
//		
////		oos.writeObject(n.getRoi().); // not serializable - build from polygon
////		oos.writeObject(n.getPolygon().); // not serializable - build from border list
//		
//		// Basic info
//		oos.writeObject(n.getPosition());
//		oos.writeObject(n.getNucleusNumber());
//		oos.writeObject(n.getNucleusFolder());
//		oos.writeObject(n.getPerimeter());
//		oos.writeObject(n.getPathLength());
//		oos.writeObject(n.getFeret());
//		oos.writeObject(n.getArea());
//		oos.writeObject(n.getCentreOfMass());
//		oos.writeObject(n.getSourceFile());
//		oos.writeObject(n.getOutputFolderName());
//		oos.writeObject(n.getAngleProfileWindowSize());
//
//		// Profiles
//		oos.writeObject(n.getAngleProfile());
//		oos.writeObject(n.getDistanceProfile());
//		oos.writeObject(n.getSingleDistanceProfile());
//		
//		// Segment and border info
//		oos.writeObject(n.getBorderTags());
//		oos.writeObject(n.getBorderList());
//
//		oos.writeObject(n.getSegmentMap());
//		oos.writeObject(n.getSegments());
//		
//		// Signals
//		oos.writeObject(n.getSignalCollection());
//	}
}


//public class SerializableObject implements Serializable {
//
//    private transient UnserializableObject unserializableObject;
//
//    private void writeObject(ObjectOutputStream oos) throws IOException {
//        oos.defaultWriteObject();
//        oos.writeObject(unserializableObject.getSerializableProperty());
//        oos.writeObject(unserializableObject.getAnotherSerializableProperty());
//        // ...
//    }
//
//    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
//        ois.defaultReadObject();
//        unserializableObject = new UnserializableObject();
//        unserializableObject.setSerializableProperty((SomeSerializableObject) ois.readObject());
//        unserializableObject.setAnotherSerializableProperty((AnotherSerializableObject) ois.readObject());
//        // ...
//    }
//
//}
