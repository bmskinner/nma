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
package com.bmskinner.nuclear_morphology.gui.dialogs;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.net.URL;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * This is an abstract class containing a JLabel with an animated gif for a
 * loading icon. This is used in the ImageProbers and in the cluster trees
 * 
 * @author ben
 *
 */
@SuppressWarnings("serial")
public abstract class LoadingIconDialog extends MessagingDialog {
	
	private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    
    private static final String RESOURCE_FOLDER  = "icons/";
    private static final String LOADING_GIF_NAME = "ajax-loader.gif";
    private static final String BLANK_GIF_NAME   = "blank.gif";

    private JLabel    loadingLabel = new JLabel("");
    private ImageIcon loadingGif   = null;          // the icon for the loading
                                                    // gif
    private ImageIcon blankGif     = null;          // the icon for the blank
                                                    // gif

    public LoadingIconDialog() {
        super((Dialog) null); // provides a taskbar icon

        boolean ok = loadResources(RESOURCE_FOLDER);
        if (!ok)
            ok = loadResources("");
        if (!ok)
            LOGGER.fine("Resource loading failed (gif)");
        this.loadingLabel.setIcon(blankGif);

    }

    // Center on screen ( absolute true/false (exact center or 25% upper left) )
    public void centerOnScreen() {
        final int width = getWidth();
        final int height = getHeight();
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screenSize.width / 2) - (width / 2);
        int y = (screenSize.height / 2) - (height / 2);

        setLocation(x, y);
    }

    /**
     * Get the JLabel with the loading icon
     * 
     * @return
     */
    protected JLabel getLoadingLabel() {
        return this.loadingLabel;
    }

    /**
     * Get the gif used for loading
     * 
     * @return
     */
    protected ImageIcon getLoadingGif() {
        return this.loadingGif;
    }

    /**
     * Set the text for the label with the loading icon
     * 
     * @param s
     */
    protected void setLoadingLabelText(String s) {
        this.loadingLabel.setText(s);
    }

    /**
     * Fetch the gif loading resources
     * 
     */
    private boolean loadResources(String path) {

        String pathToGif = path + LOADING_GIF_NAME;
        String pathToBlank = path + BLANK_GIF_NAME;

        boolean ok = false;
        try {

            // Get current classloader
            ClassLoader cl = this.getClass().getClassLoader();
            URL urlToGif = cl.getResource(pathToGif);

            if (urlToGif != null) {
                loadingGif = loadURL(urlToGif);
                ok = loadingGif!=null;
                if(!ok)
                    LOGGER.warning( "Unable to load loading gif");
            }

            // Get current classloader
            URL urlToBlank = cl.getResource(pathToBlank);

            if (urlToBlank != null) {
                blankGif = loadURL(urlToBlank);
                ok = blankGif!=null;
                if(!ok)
                    LOGGER.warning( "Unable to load blank gif");
            }

        } catch (Exception e) {
            LOGGER.log(Loggable.STACK, "Cannot load gif resource", e);
        }
        return ok;
    }

    private ImageIcon loadURL(URL url) {
        ImageIcon icon = null;
        if (url != null) {
            LOGGER.finer( "URL found: " + url.toString());
            icon = new ImageIcon(url);

            String status = "";
            switch (icon.getImageLoadStatus()) {

            case 1:
                status = "Loading";
                break;
            case 2:
                status = "Aborted";
                break;
            case 4:
                status = "Errored";
                break;
            case 8:
                status = "Complete";
                break;
            }

            LOGGER.finer("Load status: " + status);
        }
        return icon;
    }

    protected void setLoading(boolean b) {
        if (b) {
            setStatusLoading();
        } else {
            setStatusLoaded();
        }
    }

    /**
     * Set the header label to loading the loading gif
     */
    protected void setStatusLoading() {
        if (loadingGif != null) {
            ImageIcon hicon = (ImageIcon) loadingLabel.getIcon();
            if (hicon != null) {
                hicon.getImage().flush();
            }
            loadingLabel.setIcon(loadingGif);
            loadingLabel.repaint();
        }

        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        for (Component c : this.getComponents()) {
            c.setEnabled(false);
            c.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }
    }

    /**
     * Set the header label to the blank gif
     */
    protected void setStatusLoaded() {
        ImageIcon hicon = (ImageIcon) loadingLabel.getIcon();
        if (hicon != null) {
            hicon.getImage().flush();
        }
        loadingLabel.setIcon(blankGif);
        loadingLabel.repaint();

        this.setCursor(Cursor.getDefaultCursor());
        for (Component c : this.getComponents()) {
            c.setEnabled(true);
            c.setCursor(Cursor.getDefaultCursor());
        }
    }
}
