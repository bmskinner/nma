package analysis;

import java.util.ArrayList;
import java.util.List;

import components.Cell;
import components.CellCollection;
import components.nuclei.Nucleus;

/**
 * This class is designed to simplify operations on CellCollections
 * involving signals
 * @author bms41
 *
 */
public class SignalManager {
	
	  /** 
	   * Return the nuclei with or without signals in the given group.
	   * @param signalGroup the group number 
	   * @param withSignal
	   * @return a list of cells
	   */
	  public static List<Cell> getCellsWithNuclearSignals(CellCollection collection, int signalGroup, boolean hasSignal){
		  List<Cell> result = new ArrayList<Cell>(0);

		  for(Cell c : collection.getCells()){
			  Nucleus n = c.getNucleus();

			  if(hasSignal){
				  if(n.hasSignal(signalGroup)){
					  result.add(c);
				  }
			  } else {
				  if(!n.hasSignal(Math.abs(signalGroup))){
					  result.add(c);
				  }
			  }
		  }
		  return result;
	  }
	  
	  public static int getNumberOfCellsWithNuclearSignals(CellCollection collection, int signalGroup){
		  return getCellsWithNuclearSignals(collection, signalGroup, true).size();
	  }
	  
	  /**
	   * Find the signal groups present within the nuclei of the collection
	   * @return the list of groups. Order is not guaranteed
	   */
	  public static List<Integer> getSignalGroups(CellCollection collection){
		  List<Integer> result = new ArrayList<Integer>(0);
		  for(Nucleus n : collection.getNuclei()){
			  for( int group : n.getSignalCollection().getSignalGroups()){
				  if(!result.contains(group)){
					  result.add(group);
				  }
			  }
		  } // end nucleus iterations
		  return result;
	  }

}
