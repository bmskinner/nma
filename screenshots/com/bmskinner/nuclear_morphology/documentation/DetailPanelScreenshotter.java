package com.bmskinner.nuclear_morphology.documentation;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;

import com.bmskinner.nuclear_morphology.gui.main.DockableMainWindow;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;
import com.bmskinner.nuclear_morphology.io.Io;

/**
 * Take screenshots of a detail panel and all the sub-panels
 * @author ben
 * @since 1.14.0
 *
 */
public class DetailPanelScreenshotter {
	
	private Robot robot;
	private DockableMainWindow mw;
	
	public DetailPanelScreenshotter(DockableMainWindow mw, Robot robot) {
		this.robot = robot;		
		this.mw = mw;
	}
	
	/**
	 * @param c
	 * @return
	 */
	private Rectangle makeCroppedBounds(Component c) {
		Point p = c.getLocationOnScreen();
		int topCrop = 0;
		int btmCrop = 0;
		int leftCrop = 0;
		int rightCrop = 0;
		return new Rectangle(p.x+leftCrop, p.y+topCrop, c.getWidth()-(rightCrop+leftCrop), c.getHeight()-(topCrop+btmCrop));
	}
	
	public void takeScreenShots(DetailPanel panel, File folder, String title) throws IOException {
		takeAnnotatedScreenShot(panel, folder, title);
		takeScreenShot(folder, title);
		for(Component c : panel.getComponents()) {
			takeScreenShots(c, folder, title+"_"+panel.getPanelTitle());
		}
	}
	
	private void takeScreenShots(Component panel, File folder, String title) throws IOException {
		if(panel instanceof DetailPanel) {
			DetailPanel dp = (DetailPanel)panel;
			takeAnnotatedScreenShot(dp, folder, title+"_"+dp.getPanelTitle());
			takeScreenShots(dp, folder, title+"_"+dp.getPanelTitle());
			for(Component c : dp.getComponents()) {
				takeScreenShots(c, folder, title+"_"+dp.getPanelTitle());
			}
			return;
		}
		
		if(panel instanceof JTabbedPane) {
			JTabbedPane pane = (JTabbedPane)panel;
			for(int i=0; i<pane.getTabCount(); i++) {
				pane.setSelectedIndex(i);
				Component t = pane.getSelectedComponent();
				takeScreenShots(t, folder, title+"_"+i);
			}
			return;
		}
		
		if(panel instanceof JComponent) {
			JComponent jc = (JComponent)panel;
			for(Component c : jc.getComponents()) {
				takeScreenShots(c, folder, title+"_"+jc.getClass().getSimpleName());
			}
		}
	}
	
	
	private void takeAnnotatedScreenShot(DetailPanel panel, File folder, String title) throws IOException {
		Point topLeft = mw.getLocationOnScreen();
		BufferedImage img = robot.createScreenCapture(makeCroppedBounds(mw));
		Graphics2D g2 =img.createGraphics();
		g2.setStroke(new BasicStroke(3));
		g2.setColor(Color.RED);
		Point componentTopLeft = panel.getLocationOnScreen();
		int x = componentTopLeft.x-topLeft.x;
		int y = componentTopLeft.y-topLeft.y;
		g2.drawRect(x, y, panel.getWidth(), panel.getHeight());
		File outputfile = new File(folder, title+"_annotated"+Io.PNG_FILE_EXTENSION);
		if(outputfile.exists())
			return;
		ImageIO.write(img, "png", outputfile);
	}
	
	private void takeScreenShot(File folder, String title) throws IOException {
		BufferedImage img = robot.createScreenCapture(makeCroppedBounds(mw));
		File outputfile = new File(folder, title+Io.PNG_FILE_EXTENSION);
		if(outputfile.exists())
			return;
		ImageIO.write(img, "png", outputfile);
	}

}
