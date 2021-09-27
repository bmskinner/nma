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
package com.bmskinner.nuclear_morphology.components.signals;

import java.awt.Color;
import java.io.File;
import java.util.Optional;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;

/**
 * This contains information about nuclear signals within an
 * {@link ICellCollection},
 * 
 * @author bms41
 *
 */
public class DefaultSignalGroup implements ISignalGroup {

	private static final Logger LOGGER = Logger.getLogger(DefaultSignalGroup.class.getName());
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
    public DefaultSignalGroup(@NonNull String name) {
        groupName = name;
    }

    /**
     * Construct from an existing group, duplicating the values in the template
     * group.
     * 
     * @param s
     */
    public DefaultSignalGroup(@NonNull ISignalGroup s, boolean copyWarped) {

    	shellResult = null;
        groupName = s.getGroupName();
        isVisible = s.isVisible();
        groupColour = s.getGroupColour().isPresent() ? s.getGroupColour().get() : null;
        if(copyWarped)
        	warpedSignals = s.getWarpedSignals().orElse(null);
    }
    
	@Override
	public ISignalGroup duplicate() {
		return new DefaultSignalGroup(this, true);
	}
	
	@Override
	public boolean hasWarpedSignals() {
		return warpedSignals!=null;
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
    public void clearShellResult() {
    	shellResult = null;
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

    @Override
    public String toString() {
        return groupName;
    }
    
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
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
		DefaultSignalGroup other = (DefaultSignalGroup) obj;
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
