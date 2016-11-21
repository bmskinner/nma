package components.nuclear;

import java.awt.Color;
import java.io.File;

import components.active.generic.DefaultShellResult;

/**
 * This contains information about signals for an AnalysisDataset.
 * @author bms41
 *
 */
public class SignalGroup implements ISignalGroup {
    
    private static final long serialVersionUID = 1L;
    private IShellResult shellResult = null;
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
    public SignalGroup(ISignalGroup s){
        if(! s.hasShellResult()){
            shellResult=null;
        } else {
            shellResult = new DefaultShellResult(s.getShellResult());
        }
        groupName   = s.getGroupName();
        isVisible   = s.isVisible();
        groupColour = s.getGroupColour();
        channel     = s.getChannel();
        folder      = s.getFolder();
    }

    /* (non-Javadoc)
	 * @see components.nuclear.ISignalGroup#getShellResult()
	 */
    @Override
	public IShellResult getShellResult() {
        return shellResult;
    }

    /* (non-Javadoc)
	 * @see components.nuclear.ISignalGroup#setShellResult(components.nuclear.ShellResult)
	 */
    @Override
	public void setShellResult(IShellResult shellResult) {
        this.shellResult = shellResult;
    }
    
    /* (non-Javadoc)
	 * @see components.nuclear.ISignalGroup#hasShellResult()
	 */
    @Override
	public boolean hasShellResult(){
        if(shellResult==null){
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
	 * @see components.nuclear.ISignalGroup#getGroupName()
	 */
    @Override
	public String getGroupName() {
        return groupName;
    }

    /* (non-Javadoc)
	 * @see components.nuclear.ISignalGroup#setGroupName(java.lang.String)
	 */
    @Override
	public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    /* (non-Javadoc)
	 * @see components.nuclear.ISignalGroup#isVisible()
	 */
    @Override
	public boolean isVisible() {
        return isVisible;
    }

    /* (non-Javadoc)
	 * @see components.nuclear.ISignalGroup#setVisible(boolean)
	 */
    @Override
	public void setVisible(boolean isVisible) {
        this.isVisible = isVisible;
    }
    
    /* (non-Javadoc)
	 * @see components.nuclear.ISignalGroup#hasColour()
	 */
    @Override
	public boolean hasColour(){
        if(this.groupColour==null){
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
	 * @see components.nuclear.ISignalGroup#getGroupColour()
	 */
    @Override
	public Color getGroupColour() {
        return groupColour;
    }

    /* (non-Javadoc)
	 * @see components.nuclear.ISignalGroup#setGroupColour(java.awt.Color)
	 */
    @Override
	public void setGroupColour(Color groupColour) {
        this.groupColour = groupColour;
    }

    /* (non-Javadoc)
	 * @see components.nuclear.ISignalGroup#getChannel()
	 */
    @Override
	public int getChannel() {
        return channel;
    }

    /* (non-Javadoc)
	 * @see components.nuclear.ISignalGroup#setChannel(int)
	 */
    @Override
	public void setChannel(int channel) {
        this.channel = channel;
    }

    /* (non-Javadoc)
	 * @see components.nuclear.ISignalGroup#getFolder()
	 */
    @Override
	public File getFolder() {
        return folder;
    }

    /* (non-Javadoc)
	 * @see components.nuclear.ISignalGroup#setFolder(java.io.File)
	 */
    @Override
	public void setFolder(File folder) {
        this.folder = folder;
    }
    
    /* (non-Javadoc)
	 * @see components.nuclear.ISignalGroup#toString()
	 */
    @Override
	public String toString(){
    	StringBuilder b = new StringBuilder();
    	
    	String colour = this.groupColour==null ? "No colour" : this.groupColour.toString();
    	
    	b.append(groupName+" | "+this.channel+" | "+this.isVisible+" | "+colour+" | "+this.folder.getAbsolutePath());
    	return b.toString();
    }
}

