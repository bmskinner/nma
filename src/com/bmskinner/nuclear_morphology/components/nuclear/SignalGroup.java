/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.components.nuclear;

import java.awt.Color;
import java.io.File;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.ICellCollection;

/**
 * This contains information about nuclear signals within an
 * {@link ICellCollection},
 * 
 * @author bms41
 *
 */
public class SignalGroup implements ISignalGroup {

    private static final long serialVersionUID = 1L;
    private IShellResult      shellResult      = null;
    private String            groupName        = "";
    private boolean           isVisible        = true;
    private Color             groupColour      = null;
    
    @Deprecated
    private int               channel          = 0;
    
    @Deprecated
    private File              folder           = null;
    
    // Space to store warped signals from this signal group against a template consensus
    private IWarpedSignal	 warpedSignals     = null;
    
    /**
     * Default constructor
     */
    public SignalGroup(@NonNull String name) {
        groupName = name;
    }

    /**
     * Construct from an existing group, duplicating the values in the template
     * group.
     * 
     * @param s
     */
    public SignalGroup(@NonNull ISignalGroup s) {
        if (!s.hasShellResult()) {
            shellResult = null;
        } else {
            shellResult = new DefaultShellResult(s.getShellResult().get());
        }
        groupName = s.getGroupName();
        isVisible = s.isVisible();
        groupColour = s.getGroupColour().isPresent() ? s.getGroupColour().get() : null;
//        channel = s.getChannel();
//        folder = new File(s.getFolder().getAbsolutePath());
    }
    
	@Override
	public Optional<IWarpedSignal> getWarpedSignals() {
		return Optional.ofNullable(warpedSignals);
	}

	@Override
	public void setWarpedSignals(@NonNull IWarpedSignal result) {
		warpedSignals = result;
	}

    @Override
    public Optional<IShellResult> getShellResult() {
        return Optional.ofNullable(shellResult);
    }

    @Override
    public void setShellResult(@NonNull IShellResult shellResult) {
        this.shellResult = shellResult;
    }

    @Override
    public boolean hasShellResult() {
        return(shellResult != null);
    }

    @Override
    public String getGroupName() {
        return groupName;
    }

    @Override
    public void setGroupName(@NonNull String groupName) {
        this.groupName = groupName;
    }

    @Override
    public boolean isVisible() {
        return isVisible;
    }

    @Override
    public void setVisible(boolean isVisible) {
        this.isVisible = isVisible;
    }

    @Override
    public boolean hasColour() {
        return groupColour != null;
    }

    @Override
    public Optional<Color> getGroupColour() {
        return Optional.ofNullable(groupColour);
    }

    @Override
    public void setGroupColour(@NonNull Color groupColour) {
        this.groupColour = groupColour;
    }

//    @Override
//    public int getChannel() {
//        return channel;
//    }
//
//    @Override
//    public void setChannel(int channel) {
//        this.channel = channel;
//    }
//
//    @Override
//    public File getFolder() {
//        return folder;
//    }
//
//    @Override
//    public void setFolder(@NonNull File folder) {
//        this.folder = folder;
//    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();

        String colour = this.groupColour == null ? "No colour" : this.groupColour.toString();

        b.append(groupName + " | " + this.channel + " | " + this.isVisible + " | " + colour + " | "
                + this.folder.getAbsolutePath());
        return b.toString();
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
//		result = prime * result + channel;
//		result = prime * result + ((folder == null) ? 0 : folder.hashCode());
		result = prime * result + ((groupColour == null) ? 0 : groupColour.hashCode());
		result = prime * result + ((groupName == null) ? 0 : groupName.hashCode());
		result = prime * result + (isVisible ? 1231 : 1237);
		result = prime * result + ((shellResult == null) ? 0 : shellResult.hashCode());
		result = prime * result + ((warpedSignals == null) ? 0 : warpedSignals.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SignalGroup other = (SignalGroup) obj;
//		if (channel != other.channel)
//			return false;
//		if (folder == null) {
//			if (other.folder != null)
//				return false;
//		} else if (!folder.equals(other.folder))
//			return false;
		if (groupColour == null) {
			if (other.groupColour != null)
				return false;
		} else if (!groupColour.equals(other.groupColour))
			return false;
		if (groupName == null) {
			if (other.groupName != null)
				return false;
		} else if (!groupName.equals(other.groupName))
			return false;
		if (isVisible != other.isVisible)
			return false;
		if (shellResult == null) {
			if (other.shellResult != null)
				return false;
		} else if (!shellResult.equals(other.shellResult))
			return false;
		if (warpedSignals == null) {
			if (other.warpedSignals != null)
				return false;
		} else if (!warpedSignals.equals(other.warpedSignals))
			return false;
		return true;
	}
    
    
}
