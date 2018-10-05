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
package com.bmskinner.nuclear_morphology.analysis.classification;

/**
 * This exception gets thrown when errors occur during a clustering
 * or classification
 * 
 * @author ben
 *
 */
public class ClusteringMethodException extends Exception {
    private static final long serialVersionUID = 1L;

    public ClusteringMethodException() {
        super();
    }

    public ClusteringMethodException(String message) {
        super(message);
    }

    public ClusteringMethodException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClusteringMethodException(Throwable cause) {
        super(cause);
    }
}
