package components.active;

import logging.Loggable;

public interface ComponentFactory<CellularComponent> extends Loggable {
	
	CellularComponent buildInstance();

}
