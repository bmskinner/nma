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


package com.bmskinner.nuclear_morphology.components.options;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;

/**
 * The default implementation of the IAnalysisOptions interface
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public class DefaultAnalysisOptions implements IMutableAnalysisOptions {

    private static final long serialVersionUID = 1L;

    private Map<String, IMutableDetectionOptions> detectionOptions = new HashMap<String, IMutableDetectionOptions>(0);

    private double profileWindowProportion;

    private NucleusType type;

    private boolean isRefoldNucleus, isKeepFailed;

    /**
     * The default constructor, which sets default options specificed in
     * IAnalysisOptions
     */
    public DefaultAnalysisOptions() {
        profileWindowProportion = DEFAULT_WINDOW_PROPORTION;
        type = DEFAULT_TYPE;
        isRefoldNucleus = DEFAULT_REFOLD;
        isKeepFailed = DEFAULT_KEEP_FAILED;

    }

    /**
     * Construct from a template options
     * 
     * @param template
     *            the options to use as a template
     */
    public DefaultAnalysisOptions(IAnalysisOptions template) {

        if (template == null) {
            throw new IllegalArgumentException("Template options is null");
        }

        for (String key : template.getDetectionOptionTypes()) {

            IMutableDetectionOptions op;
            try {
                op = template.getDetectionOptions(key);
                this.setDetectionOptions(key, op.duplicate());
            } catch (MissingOptionException e) {
                stack("Missing expected option type", e);
            }

        }

        this.profileWindowProportion = template.getProfileWindowProportion();
        type = template.getNucleusType();
        isRefoldNucleus = template.refoldNucleus();
        isKeepFailed = template.isKeepFailedCollections();

    }

    @Override
    public IMutableDetectionOptions getDetectionOptions(String key) throws MissingOptionException {
        if (detectionOptions.containsKey(key)) {
            return detectionOptions.get(key);
        } else {
            throw new MissingOptionException(key + " not present in options");
        }

    }

    @Override
    public Set<String> getDetectionOptionTypes() {
        return detectionOptions.keySet();
    }

    @Override
    public boolean hasDetectionOptions(String type) {
        return detectionOptions.containsKey(type);
    }

    @Override
    public double getProfileWindowProportion() {
        return profileWindowProportion;
    }

    @Override
    public NucleusType getNucleusType() {
        return type;
    }

    @Override
    public boolean refoldNucleus() {
        return isRefoldNucleus;
    }

    @Override
    public Set<UUID> getNuclearSignalGroups() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasSignalDetectionOptions(UUID signalGroup) {
        String key = signalGroup.toString();
        return hasDetectionOptions(key);
    }

    @Override
    public boolean isKeepFailedCollections() {
        return isKeepFailed;
    }

    @Override
    public void setDetectionOptions(String key, IMutableDetectionOptions options) {
        detectionOptions.put(key, options);

    }

    @Override
    public void setAngleWindowProportion(double proportion) {
        profileWindowProportion = proportion;

    }

    @Override
    public void setNucleusType(NucleusType nucleusType) {
        type = nucleusType;

    }

    @Override
    public void setRefoldNucleus(boolean refoldNucleus) {
        isRefoldNucleus = refoldNucleus;

    }

    @Override
    public void setKeepFailedCollections(boolean keepFailedCollections) {
        isKeepFailed = keepFailedCollections;

    }

    @Override
    public INuclearSignalOptions getNuclearSignalOptions(UUID signalGroup) {

        try {
            return (INuclearSignalOptions) getDetectionOptions(signalGroup.toString());
        } catch (MissingOptionException e) {
            stack(e.getMessage(), e);
        }

        return null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 0;

        result = prime * result + detectionOptions.hashCode();

        long temp = Double.doubleToLongBits(profileWindowProportion);
        result = prime * result + (int) (temp ^ (temp >>> 32));

        result = prime * result + type.hashCode();

        result = prime * result + (isRefoldNucleus ? 1231 : 1237);
        result = prime * result + (isKeepFailed ? 1231 : 1237);

        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null)
            return false;

        if (!(o instanceof IAnalysisOptions))
            return false;

        IAnalysisOptions other = (IAnalysisOptions) o;

        for (String s : detectionOptions.keySet()) {
            IDetectionOptions d = detectionOptions.get(s);
            try {
                if (!d.equals(other.getDetectionOptions(s)))
                    return false;
            } catch (MissingOptionException e) {
                return false;
            }

        }

        if (Double.doubleToLongBits(profileWindowProportion) != Double
                .doubleToLongBits(other.getProfileWindowProportion()))
            return false;

        if (type != other.getNucleusType())
            return false;

        if (isRefoldNucleus != other.refoldNucleus())
            return false;

        if (isKeepFailed != other.isKeepFailedCollections())
            return false;

        return true;
    }

}
