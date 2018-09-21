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


package com.bmskinner.nuclear_morphology.gui.tabs.editing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.analysis.profiles.SegmentFitter;
import com.bmskinner.nuclear_morphology.analysis.profiles.SegmentationHandler;
import com.bmskinner.nuclear_morphology.charting.charts.MorphologyChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.ProfileChartFactory;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.ChildAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.IProfileCollection;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.generic.UnsegmentedProfileException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nuclear_morphology.gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;
import com.bmskinner.nuclear_morphology.gui.components.panels.SegmentationDualChartPanel;
import com.bmskinner.nuclear_morphology.gui.dialogs.AngleWindowSizeExplorer;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.events.SegmentEvent;
import com.bmskinner.nuclear_morphology.gui.events.SegmentEventListener;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.stats.Stats;

@SuppressWarnings("serial")
public class SegmentsEditingPanel extends AbstractEditingPanel implements ActionListener, SegmentEventListener {

    private static final String PANEL_TITLE_LBL = "Segmentation";
    
    private SegmentationDualChartPanel dualPanel;

    private JPanel  buttonsPanel;
    private JButton segmentButton;
    private JButton mergeButton;
    private JButton unmergeButton;
    private JButton splitButton;
    private JButton windowSizeButton;
    private JButton updatewindowButton;

    private static final String STR_SEGMENT_PROFILE   = "Segment profile";
    private static final String STR_MERGE_SEGMENT     = "Hide segment boundary";
    private static final String STR_UNMERGE_SEGMENT   = "Unhide segment boundary";
    private static final String STR_SPLIT_SEGMENT     = "Split segment";
    private static final String STR_SET_WINDOW_SIZE   = "Set window size";
    private static final String STR_SHOW_WINDOW_SIZES = "Window sizes";

    public SegmentsEditingPanel(@NonNull InputSupplier context) {
        super(context, PANEL_TITLE_LBL);
        this.setLayout(new BorderLayout());

        dualPanel = new SegmentationDualChartPanel();
        dualPanel.addSegmentEventListener(this);

        JPanel chartPanel = new JPanel();
        chartPanel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.EAST;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.fill = GridBagConstraints.BOTH; // reset to default
        c.weightx = 1.0;
        c.weighty = 0.7;

        chartPanel.add(dualPanel.getMainPanel(), c);
        c.weighty = 0.3;
        c.gridx = 0;
        c.gridy = 1;
        chartPanel.add(dualPanel.getRangePanel(), c);

        this.add(chartPanel, BorderLayout.CENTER);

        buttonsPanel = makeButtonPanel();
        this.add(buttonsPanel, BorderLayout.NORTH);

        setButtonsEnabled(false);

        dualPanel.getMainPanel().getChart().getXYPlot().getDomainAxis().setVisible(false);
        dualPanel.getMainPanel().getChart().getXYPlot().getRangeAxis().setVisible(false);

    }
    
    @Override
    public void setAnalysing(boolean b) {
        super.setAnalysing(b);
        dualPanel.setAnalysing(b);
    }

    private JPanel makeButtonPanel() {

        JPanel panel = new JPanel(new FlowLayout()) {
            @Override
            public void setEnabled(boolean b) {
                super.setEnabled(b);
                for (Component c : this.getComponents()) {
                    c.setEnabled(b);
                }
            }
        };
        
        segmentButton = new JButton(STR_SEGMENT_PROFILE);
        segmentButton.addActionListener(e->{
        	 getDatasetEventHandler().fireDatasetEvent(DatasetEvent.REFPAIR_SEGMENTATION, getDatasets());
        });
        panel.add(segmentButton);

        mergeButton = new JButton(STR_MERGE_SEGMENT);
        mergeButton.addActionListener(this);
        panel.add(mergeButton);

        unmergeButton = new JButton(STR_UNMERGE_SEGMENT);
        unmergeButton.addActionListener(this);
        panel.add(unmergeButton);

        splitButton = new JButton(STR_SPLIT_SEGMENT);
        splitButton.addActionListener(this);
        panel.add(splitButton);

        windowSizeButton = new JButton(STR_SHOW_WINDOW_SIZES);
        windowSizeButton.addActionListener(this);
        panel.add(windowSizeButton);

        updatewindowButton = new JButton(STR_SET_WINDOW_SIZE);
        updatewindowButton.addActionListener(this);
        panel.add(updatewindowButton);

        return panel;

    }

    @Override
    protected synchronized void updateSingle() {

        ISegmentedProfile profile = null;

        ChartOptions options = new ChartOptionsBuilder().setDatasets(getDatasets()).setNormalised(true)
                .setAlignment(ProfileAlignment.LEFT).setShowIQR(false).setTag(Tag.REFERENCE_POINT).setShowMarkers(false)
                .setProfileType(ProfileType.ANGLE).setSwatch(GlobalOptions.getInstance().getSwatch())
                .setShowProfiles(false)
                .setShowPoints(true).setShowXAxis(false).setShowYAxis(false).setTarget(dualPanel.getMainPanel())
                .build();

        // Set the button configuration
        configureButtons(options);

        JFreeChart chart = getChart(options);

        /*
         * Create the chart for the range panel
         */

        ChartOptions rangeOptions = new ChartOptionsBuilder().setDatasets(getDatasets()).setNormalised(true)
                .setAlignment(ProfileAlignment.LEFT).setShowIQR(false).setTag(Tag.REFERENCE_POINT).setShowMarkers(false)
                .setProfileType(ProfileType.ANGLE).setSwatch(GlobalOptions.getInstance().getSwatch())
                .setShowProfiles(false)
                .setShowPoints(false).setShowXAxis(false).setShowYAxis(false).setTarget(dualPanel.getRangePanel())
                .build();

        JFreeChart rangeChart = getChart(rangeOptions);

        try {
            profile = activeDataset().getCollection().getProfileCollection().getSegmentedProfile(ProfileType.ANGLE,
                    Tag.REFERENCE_POINT, Stats.MEDIAN);
        } catch (UnavailableBorderTagException | ProfileException | UnavailableProfileTypeException
                | UnsegmentedProfileException e) {
            stack("Error getting profile", e);

        }

        dualPanel.setCharts(chart, profile, true, rangeChart);
    }

    @Override
    protected synchronized void updateMultiple() {
        updateNull();

    }

    @Override
    protected synchronized void updateNull() {

        ChartOptions options = new ChartOptionsBuilder().setShowXAxis(false).setShowYAxis(false).build();

        JFreeChart mainChart = ProfileChartFactory.makeEmptyChart(null);
        JFreeChart rangeChart = ProfileChartFactory.makeEmptyChart(null);

        dualPanel.setCharts(mainChart, rangeChart);
        setButtonsEnabled(false);
    }

    @Override
    public synchronized void setChartsAndTablesLoading() {
        super.setChartsAndTablesLoading();
        dualPanel.setCharts(MorphologyChartFactory.createLoadingChart(), MorphologyChartFactory.createLoadingChart());
    }

    @Override
    protected JFreeChart createPanelChartType(ChartOptions options) {
    	return new ProfileChartFactory(options).createProfileChart();
    }

    /**
     * Enable or disable buttons depending on datasets selected
     * 
     * @param options
     * @throws Exception
     */
    private void configureButtons(ChartOptions options) {
    	if(options.isMultipleDatasets()) {
    		setButtonsEnabled(false);
    		return;
    	}

    	ICellCollection collection = options.firstDataset().getCollection();
    	setButtonsEnabled(true);
    	if(!collection.getProfileCollection().hasSegments()) {
    		unmergeButton.setEnabled(false);
            mergeButton.setEnabled(false);
            return;
    	}
    	
    	ISegmentedProfile medianProfile;
    	try {
    		medianProfile = collection.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE,
    				Tag.REFERENCE_POINT, Stats.MEDIAN);
    	} catch (UnavailableBorderTagException | ProfileException | UnavailableProfileTypeException
    			| UnsegmentedProfileException e) {
    		stack("Error getting profile", e);
    		setButtonsEnabled(false);
    		return;
    	}

    	// Don't allow merging below 2 segments
    	mergeButton.setEnabled(medianProfile.getSegmentCount()>2);

    	// Check if there are any merged segments
    	boolean hasMerges = medianProfile.getSegments().stream().anyMatch(s->s.hasMergeSources());

    	// If there are no merged segments, don't allow unmerging
    	unmergeButton.setEnabled(hasMerges);


    	// set child dataset options
    	if (options.firstDataset() instanceof ChildAnalysisDataset) {
    		mergeButton.setEnabled(false);
    		unmergeButton.setEnabled(false);
    		splitButton.setEnabled(false);
    		updatewindowButton.setEnabled(false);
    	}
    }

    public void setButtonsEnabled(boolean b) {
    	segmentButton.setEnabled(b);
        unmergeButton.setEnabled(b);
        mergeButton.setEnabled(b);
        splitButton.setEnabled(b);
        windowSizeButton.setEnabled(b);
        updatewindowButton.setEnabled(b);

    }

    private void updateCollectionWindowSize() throws Exception {
    	
    	double windowSizeActual = IAnalysisOptions.DEFAULT_WINDOW_PROPORTION;
        Optional<IAnalysisOptions> op = activeDataset().getAnalysisOptions();
        if(op.isPresent())
        	windowSizeActual = op.get().getProfileWindowProportion();
        
        try {
        	double windowSize =  getInputSupplier().requestDouble( "Select new window size", windowSizeActual, 0.01, 0.1, 0.01);
        	setAnalysing(true);
        	setCollectionWindowSize(windowSize);
        	refreshChartCache();
        	getInterfaceEventHandler().fireInterfaceEvent(InterfaceMethod.RECACHE_CHARTS);
        	setAnalysing(false);
        } catch(RequestCancelledException e) {

        }
    }

    private void setCollectionWindowSize(double windowSize) throws Exception {

        // Update cells

        for (Nucleus n : activeDataset().getCollection().getNuclei()) {
            n.setWindowProportion(ProfileType.ANGLE, windowSize);
        }

        // recalc the aggregate

        IProfileCollection pc = activeDataset().getCollection().getProfileCollection();

        pc.createProfileAggregate(activeDataset().getCollection(), pc.length());

        ISegmentedProfile medianProfile = pc.getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT,
                Stats.MEDIAN);

        // Does nothing, but needed to access segment fitter
        // DatasetSegmenter segmenter = new DatasetSegmenter(activeDataset(),
        // MorphologyAnalysisMode.NEW, programLogger);

        // Make a fitter
//        SegmentFitter fitter = new SegmentFitter(medianProfile);

//        for (Nucleus n : activeDataset().getCollection().getNuclei()) {
//
//            // recombine the segments at the lengths of the median profile
//            // segments
//            ISegmentedProfile frankenProfile = fitter.recombine(n, Tag.REFERENCE_POINT);
//
//            n.setProfile(ProfileType.FRANKEN, frankenProfile.copy());
//
//        }

        pc.createProfileAggregate(activeDataset().getCollection(), pc.length());

//        fitter = null; // clean up

        Optional<IAnalysisOptions> op = activeDataset().getAnalysisOptions();
        if(op.isPresent())
        	op.get().setAngleWindowProportion(windowSize);

    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == windowSizeButton) {
            new AngleWindowSizeExplorer(activeDataset());
        }

        try {
            ICellCollection collection = activeDataset().getCollection();
            ISegmentedProfile medianProfile = collection.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE,
                    Tag.REFERENCE_POINT, Stats.MEDIAN);

            SegmentsEditingPanel.this.setAnalysing(true);

            if (e.getSource().equals(mergeButton)) {
                this.getDatasetEventHandler().fireDatasetEvent(DatasetEvent.CLEAR_CACHE, getDatasets());
                mergeAction(medianProfile);

            }

            if (e.getSource().equals(unmergeButton)) {
                this.getDatasetEventHandler().fireDatasetEvent(DatasetEvent.CLEAR_CACHE, getDatasets());
                unmergeAction(medianProfile);
            }

            if (e.getSource().equals(splitButton)) {
                this.getDatasetEventHandler().fireDatasetEvent(DatasetEvent.CLEAR_CACHE, getDatasets());
                splitAction(medianProfile);

            }

            if (e.getSource() == updatewindowButton) {
                this.getDatasetEventHandler().fireDatasetEvent(DatasetEvent.CLEAR_CACHE, getDatasets());
                updateCollectionWindowSize();
            }

        } catch (Exception e1) {
            warn("Error in action");
            stack("Error in action", e1);
        } finally {
            SegmentsEditingPanel.this.setAnalysing(false);
        }
    }

    /**
     * Choose segments to be merged in the given segmented profile
     * 
     * @param medianProfile
     * @throws Exception
     */
    private void mergeAction(ISegmentedProfile medianProfile) throws Exception {

        List<SegMergeItem> names = new ArrayList<SegMergeItem>();

        // Put the names of the mergable segments into a list
        for (IBorderSegment seg : medianProfile.getOrderedSegments()) {
            SegMergeItem item = new SegMergeItem(seg, seg.nextSegment());
            names.add(item);
        }
        SegMergeItem[] nameArray = names.toArray(new SegMergeItem[0]);

        SegMergeItem mergeOption = (SegMergeItem) JOptionPane.showInputDialog(null, "Choose segments to merge", "Merge",
                JOptionPane.QUESTION_MESSAGE, null, nameArray, nameArray[0]);

        if (mergeOption != null) {

            this.setAnalysing(true);

            SegmentationHandler sh = new SegmentationHandler(activeDataset());
            sh.mergeSegments(mergeOption.getOne().getID(), mergeOption.getTwo().getID());

            refreshEditingPanelCharts();

        } else {
            JOptionPane.showMessageDialog(this, "Cannot merge segments: they would cross a core border tag");
        }
        this.setAnalysing(false);

    }

    private class SegMergeItem {
        private IBorderSegment one, two;

        public SegMergeItem(IBorderSegment one, IBorderSegment two) {
            this.one = one;
            this.two = two;
        }

        @Override
		public String toString() {
            return one.getName() + " - " + two.getName();
        }

        public IBorderSegment getOne() {
            return one;
        }

        public IBorderSegment getTwo() {
            return two;
        }
    }

    private void splitAction(ISegmentedProfile medianProfile) {

    	IBorderSegment[] nameArray = medianProfile.getSegments().toArray(new IBorderSegment[0]);

    	String[] options = Arrays.stream(nameArray).map(s->s.getName()).toArray(String[]::new);

    	try {
    		int option = getInputSupplier().requestOption(options, 0, "Choose segment to split", "Split segment");

    		setAnalysing(true);
    		SegmentationHandler sh = new SegmentationHandler(activeDataset());
    		sh.splitSegment(nameArray[option].getID());
    		refreshEditingPanelCharts();
    		setAnalysing(false);
    	} catch (RequestCancelledException e) {
    		return;
    	}
    }

    /**
     * Unmerge segments in a median profile
     * 
     * @param medianProfile
     * @throws Exception
     */
    private void unmergeAction(ISegmentedProfile medianProfile) throws Exception {

        List<IBorderSegment> names = new ArrayList<>();

        // Put the names of the mergable segments into a list
        for (IBorderSegment seg : medianProfile.getSegments()) {
            if (seg.hasMergeSources()) {
                names.add(seg);
            }
        }
        IBorderSegment[] nameArray = names.toArray(new IBorderSegment[0]);
        String[] options = Arrays.stream(nameArray).map(s->s.getName()).toArray(String[]::new);
        
        try {
    		int option = getInputSupplier().requestOption(options, 0, "Choose merged segment to unmerge", "Unmerge segment");

    		setAnalysing(true);
    		SegmentationHandler sh = new SegmentationHandler(activeDataset());
    		sh.unmergeSegments(nameArray[option].getID());
    		refreshEditingPanelCharts();
    		setAnalysing(false);
    	} catch (RequestCancelledException e) {
    		return;
    	}
    }

    @Override
    public void segmentEventReceived(SegmentEvent event) {
        if (event.getType() == SegmentEvent.MOVE_START_INDEX) {
            finest("Heard update segment request");
            try {

                setAnalysing(true);

                UUID segID = event.getId();

                int index = event.getIndex();

                updateSegmentStartIndexAction(segID, index);

            } catch (Exception e) {
                error("Error updating segment", e);
            } finally {
                setAnalysing(false);
            }

        }

    }

}
