package components.nuclear;

import components.CellularComponent;

/**
 * The methods available to a nuclear signal, which is a type
 * of cellular component
 * @author ben
 * @since 1.13.3
 *
 */
public interface INuclearSignal 
	extends CellularComponent {

	/**
	 * Get the index of the closest point in the nuclear 
	 * periphery to this signal
	 * @return
	 */
	int getClosestBorderPoint();

	/**
	 * Set the index of the closest point in the nuclear 
	 * periphery to this signal
	 * @return
	 */
	void setClosestBorderPoint(int p);

	INuclearSignal duplicate();
	
	/**
	 * Translate the border and centre of mass of the signal
	 * to lie within the specified component. The signal must originate
	 * from an image with the same dimensions as the component.
	 * @param c the component to place the signal within
	 */
//	void setPositionWithin(CellularComponent c);

}