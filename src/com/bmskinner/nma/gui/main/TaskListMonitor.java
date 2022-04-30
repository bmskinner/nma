/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nma.gui.main;

import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

import com.bmskinner.nma.core.ThreadManager;

/**
 * This monitors the length of the task queue once per second,
 * and writes the value to a text label. 
 * @author bms41
 * @since 1.13.8
 *
 */
@SuppressWarnings("serial")
public class TaskListMonitor extends JLabel
implements Runnable {
    
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
