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
package com.bmskinner.nuclear_morphology.gui.tabs.profiles;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.charting.charts.MorphologyChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.ProfileChartFactory;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;

@SuppressWarnings("serial")
public class ProfileDisplayPanel extends AbstractProfileDisplayPanel {

    public ProfileDisplayPanel(@NonNull InputSupplier context, ProfileType type) {
        super(context, type);

        JFreeChart chart = ProfileChartFactory.createEmptyChart(type);
        chartPanel.setChart(chart);

        if (this.type == ProfileType.FRANKEN) {
            this.profileAlignmentOptionsPanel.setEnabled(false);
        }
    }
    
    @Override
    protected void updateSingle() {
        super.updateSingle();
        updateChart();

    }

    @Override
    protected void updateMultiple() {
        super.updateMultiple();
        updateChart();
    }

    @Override
    protected void updateNull() {
        super.updateNull();
        JFreeChart chart = ProfileChartFactory.createEmptyChart(type);
        chartPanel.setChart(chart);

    }

    @Override
    protected JFreeChart createPanelChartType(ChartOptions options) {
        return new ProfileChartFactory(options).createProfileChart();
    }

    private void updateChart() {
        ChartOptions options = makeOptions();
        setChart(options);
    }

    private ChartOptions makeOptions() {

        boolean normalised = profileAlignmentOptionsPanel.isNormalised();
        ProfileAlignment alignment = normalised ? ProfileAlignment.LEFT : profileAlignmentOptionsPanel.getSelected();
        // BorderTagObject tag = borderTagOptionsPanel.getSelected();
        boolean showMarkers = profileMarkersOptionsPanel.showMarkers();
        boolean hideProfiles = profileMarkersOptionsPanel.isShowNuclei();

        // log("Creating options: normalised: "+normalised);

        ChartOptions options = new ChartOptionsBuilder().setDatasets(getDatasets())
        		.setNormalised(normalised)
                .setAlignment(alignment)
                .setTag(Tag.REFERENCE_POINT)
                .setShowMarkers(showMarkers)
                .setShowProfiles(hideProfiles)
                .setSwatch(GlobalOptions.getInstance().getSwatch())
                .setProfileType(type)
                .setTarget(chartPanel)
                .build();
        return options;
    }
}
