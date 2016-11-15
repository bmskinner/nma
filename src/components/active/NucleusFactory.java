package components.active;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import components.CellularComponent;
import components.generic.IPoint;
import components.nuclear.NucleusType;
import components.nuclei.Nucleus;
import ij.gui.Roi;

public class NucleusFactory implements ComponentFactory<CellularComponent> {

	public NucleusFactory(){}
	
	@Override
	public Nucleus buildInstance(){
		return null;
	}
	
	/**
	 * Create a nucleus of the appropriate class for the given nucleus tyoe
	 * @param roi
	 * @param path
	 * @param channel
	 * @param nucleusNumber
	 * @param originalPosition
	 * @param nucleusType
	 * @param centreOfMass
	 * @return
	 * @throws NucleusCreationException 
	 */
	public Nucleus createNucleus(Roi roi, File path, 
			int channel, int nucleusNumber, 
			int[] originalPosition, NucleusType nucleusType, 
			IPoint centreOfMass) throws NucleusCreationException {
		
		if(roi==null || path==null || nucleusType==null || centreOfMass==null){
			throw new IllegalArgumentException("Argument cannot be null in nucleus factory");
		}
		
		Nucleus n = null;
		
		try {

			  // The classes for the constructor
			  Class<?>[] classes = {Roi.class, IPoint.class, File.class, int.class, int[].class, int.class };
			  
			  Constructor<?> nucleusConstructor = nucleusType.getNucleusClass()
						  .getConstructor(classes);

				n = (Nucleus) nucleusConstructor.newInstance(roi,
						  centreOfMass, 
						  path, 
						  channel, 
						  originalPosition,
						  nucleusNumber);

		} catch (InvocationTargetException e) {
			stack("Invokation error creating nucleus", e.getCause());
			throw new NucleusCreationException("Error making nucleus:" +e.getMessage(), e);
		} catch(Error e){
			stack("Error creating nucleus", e);
			throw new NucleusCreationException("Error making nucleus:" +e.getMessage(), e);
		} catch (InstantiationException | IllegalAccessException |
				IllegalArgumentException | NoSuchMethodException | SecurityException e) {
			stack("Error creating nucleus", e);
			throw new NucleusCreationException("Error making nucleus:" +e.getMessage(), e);
		}
			  

		if(n==null){
			throw new NucleusCreationException("Error making nucleus");
		}
		  return n;
	  }
	
//	private static Nucleus createRodentSpermNucleus(Roi roi, IPoint centreOfMass, File path, 
//			int channel, int[] originalPosition, int nucleusNumber	) throws NucleusCreationException{
//	
//		
//		return new DefaultRodentSpermNucleus(roi,
//						  centreOfMass, 
//						  path, 
//						  channel, 
//						  originalPosition,
//						  nucleusNumber);
//	}
	
	/**
	 * Thrown when a profile collection or segmented profile has no assigned
	 * segments
	 * @author bms41
	 *
	 */
	public static class NucleusCreationException extends Exception {
			private static final long serialVersionUID = 1L;
			public NucleusCreationException() { super(); }
			public NucleusCreationException(String message) { super(message); }
			public NucleusCreationException(String message, Throwable cause) { super(message, cause); }
			public NucleusCreationException(Throwable cause) { super(cause); }
		
	}
}
