package gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.MediaTracker;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class CarltonDialog extends JDialog {
	
	private static final String IMAGE_NAME = "Carlton.gif";
	private static final String IMAGE_URL = "res/"+IMAGE_NAME;
	private static final String BACKUP_IMAGE_URL = IMAGE_NAME;
	
    public CarltonDialog(Logger programLogger) {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setUndecorated(false);
        setLocationRelativeTo(null);
        setModal(false);
        
        setContentPane(new JPanel(new BorderLayout()));

        try {
        	
        	ClassLoader cl = this.getClass().getClassLoader();
        	
        	
			URL url = cl.getResource(IMAGE_URL);
			ImageIcon icon = loadURL(url, programLogger);

			if(icon==null){
				url = cl.getResource(BACKUP_IMAGE_URL);
				icon = loadURL(url, programLogger);
			}

			if( icon != null ){ // only trigger if everything was loaded sucessfully

				programLogger.log(Level.FINE, "Image loaded from "+url.toString());
				
				try {
					
					programLogger.log(Level.INFO, "Oh my...");
					Thread.sleep(1500);
					programLogger.log(Level.INFO, "You have been busy.");
					Thread.sleep(1500);
					programLogger.log(Level.INFO, "So many nuclei.");
					Thread.sleep(1500);
					programLogger.log(Level.INFO, "You have earned a dance.");
					Thread.sleep(1500);
					
					
				} catch (InterruptedException e) {
					// If an error occurs in threading, cancel
					programLogger.log(Level.FINE, "Threading error in loading ");
					return;
				}
				
				getContentPane().add(new JLabel(icon), BorderLayout.CENTER);
				this.pack();

				this.setVisible(true);
			} else {
				programLogger.log(Level.FINE, "Could not load icon; cancelling");
				return;
			}

			
			
		} catch (Exception e) {
			programLogger.log(Level.FINE, "Error in loading ");
			return;
		}
    }
    
    private ImageIcon loadURL(URL url, Logger programLogger){
    	ImageIcon icon = null;
    	if(url!=null){
    		programLogger.log(Level.FINE, "URL found: "+url.toString());
    		icon = new ImageIcon(url);
    		
//    		String status = "";
//    		switch(icon.getImageLoadStatus()){
//    		
//    			case 1: status = "Loading";
//    				break;
//    			case 2: status = "Aborted";
//    				break;
//    			case 4: status = "Errored";
//					break;
//    			case 8: status = "Complete";
//					break;
//    		
//    		}
//    		
//    		programLogger.log(Level.WARNING, "Load status: "+status);
    		
//    		if(icon.getImageLoadStatus()== MediaTracker.ERRORED){
//    			programLogger.log(Level.WARNING, "Could not load icon from "+url.getPath());
//    			programLogger.log(Level.WARNING, "Icon status "+icon.getImageLoadStatus());
//
//    			
//    			
//    			programLogger.log(Level.WARNING, "Trying to load icon directly...");
//    			try{
//    				Image img = ImageIO.read(url);
//    				icon = new ImageIcon(img);
//    			} catch(IOException e){
//    				programLogger.log(Level.SEVERE, "Error in loading image ", e);
//    				icon = null;
//    			}
//    		}

    	}
    	return icon;
    }
}
