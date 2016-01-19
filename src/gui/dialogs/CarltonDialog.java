package gui.dialogs;

import java.awt.BorderLayout;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class CarltonDialog extends JDialog {
	
	private static final String IMAGE_NAME = "Carlton.gif";
	private static final String IMAGE_URL = "res/"+IMAGE_NAME;
	private static final String BACKUP_IMAGE_URL = IMAGE_NAME;
	
    public CarltonDialog() {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setUndecorated(false);
        setLocationRelativeTo(null);
        setModal(false);
        
        setContentPane(new JPanel(new BorderLayout()));

        try {
        	
        	ClassLoader cl = this.getClass().getClassLoader();
        	
			URL url = cl.getResource(IMAGE_URL);
			if(url==null){
				url = cl.getResource(BACKUP_IMAGE_URL);
			}

			ImageIcon icon = new ImageIcon(url);
			getContentPane().add(new JLabel(icon), BorderLayout.CENTER);
			
		} catch (Exception e) {
			
//			programLogger.log(Level.SEVERE, "Error loading Carlton", e);
		} finally {

			this.pack();

			this.setVisible(true);
		}
    }
}
