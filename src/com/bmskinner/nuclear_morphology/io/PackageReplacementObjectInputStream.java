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


package com.bmskinner.nuclear_morphology.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Specifies a map of new package names for classes being deserialised, so that
 * the package structure can be updated without old datasets breaking. From
 * http://stackoverflow.com/questions/10936625/using-readclassdescriptor-and-maybe-resolveclass-to-permit-serialization-ver/14608062#14608062
 * 
 * @author ben
 * @since 1.13.3
 *
 */
@SuppressWarnings("deprecation")
public class PackageReplacementObjectInputStream extends ObjectInputStream implements Loggable {

    /**
     * Migration table. Holds old to new classes representation.
     */
    protected static final Map<String, Class<?>> MIGRATION_MAP = new HashMap<String, Class<?>>();

    static {

        /*
         * Changes from 1.13.2 to 1.13.3
         */

        MIGRATION_MAP.put("analysis.AnalysisDataset",
                com.bmskinner.nuclear_morphology.components.AnalysisDataset.class);
        MIGRATION_MAP.put("analysis.AnalysisOptions",
                com.bmskinner.nuclear_morphology.components.options.AnalysisOptions.class);
        MIGRATION_MAP.put("analysis.AnalysisOptions$CannyOptions",
                com.bmskinner.nuclear_morphology.components.options.AnalysisOptions.CannyOptions.class);
        MIGRATION_MAP.put("analysis.DefaultAnalysisOptions",
                com.bmskinner.nuclear_morphology.components.options.DefaultAnalysisOptions.class);
        MIGRATION_MAP.put("analysis.nucleus.DefaultNucleusDetectionOptions",
                com.bmskinner.nuclear_morphology.components.options.DefaultNucleusDetectionOptions.class);

        MIGRATION_MAP.put("analysis.profiles.RuleSetCollection",
                com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection.class);
        MIGRATION_MAP.put("analysis.profiles.RuleSet", com.bmskinner.nuclear_morphology.components.rules.RuleSet.class);
        MIGRATION_MAP.put("analysis.profiles.Rule", com.bmskinner.nuclear_morphology.components.rules.Rule.class);
        MIGRATION_MAP.put("analysis.profiles.Rule$RuleType",
                com.bmskinner.nuclear_morphology.components.rules.Rule.RuleType.class);

        MIGRATION_MAP.put("analysis.signals.NuclearSignalOptions",
                com.bmskinner.nuclear_morphology.components.options.NuclearSignalOptions.class);
        MIGRATION_MAP.put("analysis.signals.DefaultNuclearSignalOptions",
                com.bmskinner.nuclear_morphology.components.options.DefaultNuclearSignalOptions.class);
        MIGRATION_MAP.put("analysis.signals.INuclearSignalOptions$SignalDetectionMode",
                com.bmskinner.nuclear_morphology.components.options.INuclearSignalOptions.SignalDetectionMode.class);

        MIGRATION_MAP.put("components.AbstractCellularComponent",
                com.bmskinner.nuclear_morphology.components.AbstractCellularComponent.class);
        MIGRATION_MAP.put("components.CellCollection",
                com.bmskinner.nuclear_morphology.components.CellCollection.class);
        MIGRATION_MAP.put("components.CellCollection$StatsCache",
                com.bmskinner.nuclear_morphology.components.CellCollection.StatsCache.class);
        MIGRATION_MAP.put("components.Cell", com.bmskinner.nuclear_morphology.components.Cell.class);
        MIGRATION_MAP.put("components.CellularComponent",
                com.bmskinner.nuclear_morphology.components.CellularComponent.class);
        MIGRATION_MAP.put("components.ClusterGroup", com.bmskinner.nuclear_morphology.components.ClusterGroup.class);
        MIGRATION_MAP.put("components.Mitochondrion", com.bmskinner.nuclear_morphology.components.Mitochondrion.class);
        MIGRATION_MAP.put("components.SpermTail", com.bmskinner.nuclear_morphology.components.SpermTail.class);

        MIGRATION_MAP.put("components.generic.BorderTag",
                com.bmskinner.nuclear_morphology.components.generic.BorderTag.class);
        MIGRATION_MAP.put("components.generic.BorderTagObject",
                com.bmskinner.nuclear_morphology.components.generic.BorderTagObject.class);
        MIGRATION_MAP.put("components.generic.MeasurementScale",
                com.bmskinner.nuclear_morphology.components.generic.MeasurementScale.class);
        MIGRATION_MAP.put("components.generic.Profile",
                com.bmskinner.nuclear_morphology.components.generic.Profile.class);
        MIGRATION_MAP.put("components.generic.ProfileAggregate",
                com.bmskinner.nuclear_morphology.components.generic.ProfileAggregate.class);
        MIGRATION_MAP.put("components.generic.ProfileCollection",
                com.bmskinner.nuclear_morphology.components.generic.ProfileCollection.class);
        MIGRATION_MAP.put("components.generic.ProfileCollection$ProfileCache",
                com.bmskinner.nuclear_morphology.components.generic.ProfileCollection.ProfileCache.class);
        MIGRATION_MAP.put("components.generic.ProfileType",
                com.bmskinner.nuclear_morphology.components.generic.ProfileType.class);
        MIGRATION_MAP.put("components.generic.SegmentedProfile",
                com.bmskinner.nuclear_morphology.components.generic.SegmentedProfile.class);
        MIGRATION_MAP.put("components.generic.XYPoint",
                com.bmskinner.nuclear_morphology.components.generic.XYPoint.class);

        MIGRATION_MAP.put("components.nuclear.BorderPoint",
                com.bmskinner.nuclear_morphology.components.nuclear.BorderPoint.class);
        MIGRATION_MAP.put("components.nuclear.NuclearSignal",
                com.bmskinner.nuclear_morphology.components.nuclear.NuclearSignal.class);
        MIGRATION_MAP.put("components.nuclear.NucleusBorderSegment",
                com.bmskinner.nuclear_morphology.components.nuclear.NucleusBorderSegment.class);
        MIGRATION_MAP.put("components.nuclear.NucleusType",
                com.bmskinner.nuclear_morphology.components.nuclear.NucleusType.class);
        MIGRATION_MAP.put("components.nuclear.ShellResult",
                com.bmskinner.nuclear_morphology.components.nuclear.ShellResult.class);
        MIGRATION_MAP.put("components.nuclear.SignalCollection",
                com.bmskinner.nuclear_morphology.components.nuclear.SignalCollection.class);
        MIGRATION_MAP.put("components.nuclear.SignalGroup",
                com.bmskinner.nuclear_morphology.components.nuclear.SignalGroup.class);

        MIGRATION_MAP.put("components.nuclei.AsymmetricNucleus",
                com.bmskinner.nuclear_morphology.components.nuclei.AsymmetricNucleus.class);
        MIGRATION_MAP.put("components.nuclei.ConsensusNucleus",
                com.bmskinner.nuclear_morphology.components.nuclei.ConsensusNucleus.class);
        MIGRATION_MAP.put("components.nuclei.Nucleus",
                com.bmskinner.nuclear_morphology.components.nuclei.Nucleus.class);
        MIGRATION_MAP.put("components.nuclei.RoundNucleus",
                com.bmskinner.nuclear_morphology.components.nuclei.RoundNucleus.class);

        MIGRATION_MAP.put("components.nuclei.sperm.PigSpermNucleus",
                com.bmskinner.nuclear_morphology.components.nuclei.sperm.PigSpermNucleus.class);
        MIGRATION_MAP.put("components.nuclei.sperm.RodentSpermNucleus",
                com.bmskinner.nuclear_morphology.components.nuclei.sperm.RodentSpermNucleus.class);
        MIGRATION_MAP.put("components.nuclei.sperm.SpermNucleus",
                com.bmskinner.nuclear_morphology.components.nuclei.sperm.SpermNucleus.class);

        MIGRATION_MAP.put("stats.NucleusStatistic",
                com.bmskinner.nuclear_morphology.components.stats.NucleusStatistic.class);
        MIGRATION_MAP.put("stats.PlottableStatistic",
                com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic.class);
        MIGRATION_MAP.put("stats.SegmentStatistic",
                com.bmskinner.nuclear_morphology.components.stats.SegmentStatistic.class);
        MIGRATION_MAP.put("stats.SignalStatistic",
                com.bmskinner.nuclear_morphology.components.stats.SignalStatistic.class);

        MIGRATION_MAP.put("utility.Version", com.bmskinner.nuclear_morphology.components.generic.Version.class);

        /*
         * Changes from 1.13.4 to 1.13.5
         */

        MIGRATION_MAP.put("com.bmskinner.nuclear_morphology.analysis.nucleus.DefaultNucleusDetectionOptions",
                com.bmskinner.nuclear_morphology.components.options.DefaultNucleusDetectionOptions.class);      
        
        /*
         * Changes from 1.13.7 to 1.13.8
         */
        
        MIGRATION_MAP.put("com.bmskinner.nuclear_morphology.components.options.IMutableDetectionOptions",
        		com.bmskinner.nuclear_morphology.components.options.IDetectionOptions.class);  
        
        MIGRATION_MAP.put("com.bmskinner.nuclear_morphology.components.options.IMutableLobeDetectionOptions",
        		com.bmskinner.nuclear_morphology.components.options.ILobeDetectionOptions.class);  
        
        MIGRATION_MAP.put("com.bmskinner.nuclear_morphology.components.options.IMutableCannyOptions",
        		com.bmskinner.nuclear_morphology.components.options.ICannyOptions.class); 
        
        MIGRATION_MAP.put("com.bmskinner.nuclear_morphology.components.options.IMutableNuclearSignalOptions",
        		com.bmskinner.nuclear_morphology.components.options.INuclearSignalOptions.class); 
        
        MIGRATION_MAP.put("com.bmskinner.nuclear_morphology.components.options.IMutableAnalysisOptions",
        		com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions.class); 
        
        MIGRATION_MAP.put("com.bmskinner.nuclear_morphology.components.generic.IMutablePoint",
                com.bmskinner.nuclear_morphology.components.generic.IPoint.class); 
        
        MIGRATION_MAP.put("com.bmskinner.nuclear_morphology.components.IMutableCell",
                com.bmskinner.nuclear_morphology.components.ICell.class); 
        
    }

    /**
     * Constructor.
     * 
     * @param stream input stream
     * @throws IOException if io error
     */
    public PackageReplacementObjectInputStream(final InputStream stream) throws IOException {
        super(stream);
    }


    @Override
    protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
        ObjectStreamClass resultClassDescriptor = super.readClassDescriptor();

        // log("Testing descriptor for "+resultClassDescriptor.getName());

        for (final String oldName : MIGRATION_MAP.keySet()) {
            if (resultClassDescriptor.getName().equals(oldName)) {

                finest("Replacing class " + oldName);
                String replacement = MIGRATION_MAP.get(oldName).getName();

                try {
                    Field f = resultClassDescriptor.getClass().getDeclaredField("name");
                    f.setAccessible(true);
                    f.set(resultClassDescriptor, replacement);  
                } catch (Exception e) {

                    error("Error while replacing class name: " + e.getMessage(), e);
                }

            }
        }

        return resultClassDescriptor;
    }
}
