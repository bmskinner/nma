package components.nuclear;

import java.awt.Color;
import java.io.File;
import java.io.Serializable;

/**
 * This contains information about signals for an AnalysisDataset.
 * @author bms41
 *
 */
public class SignalGroup implements Serializable {
    
    private static final long serialVersionUID = 1L;
    private ShellResult shellResult = null;
    private String      groupName   = "";
    private boolean     isVisible   = true;
    private Color       groupColour = null;
    private int         channel     = 0;
    private File        folder      = null;
    
    public SignalGroup(){}
    
    /**
     * Duplicate a group
     * @param s
     */
    public SignalGroup(SignalGroup s){
        if(s.shellResult==null){
            shellResult=null;
        } else {
            shellResult = new ShellResult(s.shellResult);
        }
        groupName   = s.groupName;
        isVisible   = s.isVisible;
        groupColour = s.groupColour;
        channel     = s.channel;
        folder      = s.folder;
    }

    public ShellResult getShellResult() {
        return shellResult;
    }

    public void setShellResult(ShellResult shellResult) {
        this.shellResult = shellResult;
    }
    
    public boolean hasShellResult(){
        if(shellResult==null){
            return false;
        }
        return true;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean isVisible) {
        this.isVisible = isVisible;
    }
    
    public boolean hasColour(){
        if(this.groupColour==null){
            return false;
        }
        return true;
    }

    public Color getGroupColour() {
        return groupColour;
    }

    public void setGroupColour(Color groupColour) {
        this.groupColour = groupColour;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public File getFolder() {
        return folder;
    }

    public void setFolder(File folder) {
        this.folder = folder;
    }
    
    public String toString(){
    	StringBuilder b = new StringBuilder();
    	
    	String colour = this.groupColour==null ? "No colour" : this.groupColour.toString();
    	
    	b.append(groupName+" | "+this.channel+" | "+this.isVisible+" | "+colour+" | "+this.folder.getAbsolutePath());
    	return b.toString();
    }
}

