package com.bmskinner.nuclear_morphology.gui.main;

import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.main.ThreadManager;

public class TaskListMonitor extends JLabel
implements Runnable, Loggable {

	
	public TaskListMonitor() {
		super("0", SwingConstants.CENTER);
		Thread t = new Thread(this);
		t.start();
	}

	@Override
	public void run() {
		do  {
			try {
				Thread.sleep(1000L);
			} catch (InterruptedException e) {

			}
			
			int l = ThreadManager.getInstance().queueLength();
			setText(""+l);
		} while(true);
	}

	@Override
	public Dimension getPreferredSize(){
		return new Dimension(30, 20);
	}

}