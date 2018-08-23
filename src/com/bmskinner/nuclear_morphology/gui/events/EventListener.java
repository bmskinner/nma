package com.bmskinner.nuclear_morphology.gui.events;

/**
 * Global interface for the event listeners
 * @author bms41
 * @since 1.14.0
 *
 * @param <E> the type of event to listen for
 */
public interface EventListener {
	void eventReceived(DatasetEvent event);
	void eventReceived(DatasetUpdateEvent event);
	void eventReceived(SignalChangeEvent event);
	void eventReceived(InterfaceEvent event);
	void eventReceived(ChartOptionsRenderedEvent event);
}
