package com.bmskinner.nuclear_morphology.gui.main;

import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * This monitors the length of the task queue once per second,
 * and writes the value to a text label. 
 * @author bms41
 * @since 1.13.8
 *
 */
@SuppressWarnings("serial")
public class TaskListMonitor extends JLabel
implements Runnable, Loggable {
    
    private static final int PREFERRED_WIDTH = 50;
    private static final int PREFERRED_HEIGHT = 20;
    
    private static final long SLEEP_TIME = 1000L;
	
	/**
	 * Create with default parameters.
	 */
	public TaskListMonitor() {
		super("0", SwingConstants.CENTER);
		Thread t = new Thread(this);
		t.setName("Task list tracking thread");
		t.start();
	}

	@Override
	public void run() {
		do  {
			try {
				Thread.sleep(SLEEP_TIME);
			} catch (InterruptedException e) {

			}
			
			int l = ThreadManager.getInstance().uiQueueLength();
			int m = ThreadManager.getInstance().methodQueueLength();
			setText(l+"/"+m);
		} while(true);
	}

	@Override
	public Dimension getPreferredSize(){
		return new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT);
	}

}