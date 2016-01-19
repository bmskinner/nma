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
	
	private static final String IMAGE_NAME = "Carlton_orig.gif";
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

				programLogger.log(Level.WARNING, "Image loaded from "+url.toString());
				
				try {
					
					programLogger.log(Level.INFO, "Oh boy...");
					Thread.sleep(1000);
					programLogger.log(Level.INFO, "You have been busy.");
					Thread.sleep(1000);
					programLogger.log(Level.INFO, "There are a lot of nuclei here.");
					Thread.sleep(1000);
					programLogger.log(Level.INFO, "You have earned a treat.");
					Thread.sleep(1000);
					
					
				} catch (InterruptedException e) {
					// If an error occurs in threading, cancel
					programLogger.log(Level.WARNING, "Threading error in loading ");
					return;
				}
				
				getContentPane().add(new JLabel(icon), BorderLayout.CENTER);
				this.pack();

				this.setVisible(true);
			} else {
				programLogger.log(Level.WARNING, "Could not load icon; cancelling");
				return;
			}

			
			
		} catch (Exception e) {
			programLogger.log(Level.SEVERE, "Error in loading ", e);
			return;
		}
    }
    
    private ImageIcon loadURL(URL url, Logger programLogger){
    	ImageIcon icon = null;
    	if(url!=null){
    		programLogger.log(Level.WARNING, "URL found: "+url.toString());
    		icon = new ImageIcon(url);
    		
    		String status = "";
    		switch(icon.getImageLoadStatus()){
    		
    			case 1: status = "Loading";
    				break;
    			case 2: status = "Aborted";
    				break;
    			case 4: status = "Errored";
					break;
    			case 8: status = "Complete";
					break;
    		
    		}
    		
    		programLogger.log(Level.WARNING, "Load status: "+status);
    		
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
