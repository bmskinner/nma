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
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.analysis.profiles.SegmentationHandler;
import com.bmskinner.nuclear_morphology.charting.charts.MorphologyChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.ProfileChartFactory;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileCollection;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileSegment;
import com.bmskinner.nuclear_morphology.components.profiles.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.profiles.UnsegmentedProfileException;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nuclear_morphology.gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;
import com.bmskinner.nuclear_morphology.gui.components.panels.SegmentationDualChartPanel;
import com.bmskinner.nuclear_morphology.gui.dialogs.AngleWindowSizeExplorer;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.gui.events.SegmentEvent;
import com.bmskinner.nuclear_morphology.gui.events.SegmentEvent.SegmentUpdateType;
import com.bmskinner.nuclear_morphology.gui.events.SegmentEventListener;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.Stats;

@SuppressWarnings("serial")
public class SegmentsEditingPanel extends AbstractEditingPanel implements ActionListener, SegmentEventListener {
	
	private static final Logger LOGGER = Logger.getLogger(SegmentsEditingPanel.class.getName());

    private static final String PANEL_TITLE_LBL = "Segmentation";
    
    private SegmentationDualChartPanel dualPanel;
    
    private JLabel buttonStateLbl = new JLabel(" ", JLabel.CENTER);

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

        this.add(createHeader(), BorderLayout.NORTH);

        setButtonsEnabled(false);

        dualPanel.getMainPanel().getChart().getXYPlot().getDomainAxis().setVisible(false);
        dualPanel.getMainPanel().getChart().getXYPlot().getRangeAxis().setVisible(false);

    }
    
    @Override
    public void setAnalysing(boolean b) {
        super.setAnalysing(b);
        dualPanel.setAnalysing(b);
    }

    private JPanel createHeader() {

    	JPanel headerPanel = new JPanel();
    	headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
    	JPanel txtPanel = new JPanel(new FlowLayout());
    	txtPanel.add(buttonStateLbl);
    	headerPanel.add(txtPanel);
    	
    	
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
        	try {
				boolean ok = getInputSupplier().requestApproval("This action will resegment the dataset. Manual segments will be lost. Continue?", "Continue?");
				if(ok) {
					for(IAnalysisDataset d : getDatasets()) {
						d.getCollection().getProfileManager().setLockOnAllNucleusSegments(false);
					}
					getDatasetEventHandler().fireDatasetEvent(DatasetEvent.SEGMENTATION_ACTION, getDatasets());
				}
			} catch (RequestCancelledException e1) {}
        	 
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
        windowSizeButton.addActionListener(e->new AngleWindowSizeExplorer(activeDataset()));
        panel.add(windowSizeButton);

        updatewindowButton = new JButton(STR_SET_WINDOW_SIZE);
        updatewindowButton.addActionListener(this);
        panel.add(updatewindowButton);

        
        headerPanel.add(panel);
        return headerPanel;

    }

    @Override
    protected synchronized void updateSingle() {

        ISegmentedProfile profile = null;

        ChartOptions options = new ChartOptionsBuilder().setDatasets(getDatasets())
        		.setNormalised(true)
                .setAlignment(ProfileAlignment.LEFT)
                .setShowIQR(false).setTag(Landmark.REFERENCE_POINT)
                .setShowMarkers(false)
                .setProfileType(ProfileType.ANGLE)
                .setSwatch(GlobalOptions.getInstance().getSwatch())
                .setShowProfiles(false)
                .setShowPoints(true)
                .setShowXAxis(false).setShowYAxis(false)
                .setTarget(dualPanel.getMainPanel())
                .build();

        // Set the button configuration
        configureButtons(options);

        JFreeChart chart = getChart(options);

        /*
         * Create the chart for the range panel
         */

        ChartOptions rangeOptions = new ChartOptionsBuilder().setDatasets(getDatasets()).setNormalised(true)
                .setAlignment(ProfileAlignment.LEFT).setShowIQR(false).setTag(Landmark.REFERENCE_POINT).setShowMarkers(false)
                .setProfileType(ProfileType.ANGLE).setSwatch(GlobalOptions.getInstance().getSwatch())
                .setShowProfiles(false)
                .setShowPoints(false).setShowXAxis(false).setShowYAxis(false).setTarget(dualPanel.getRangePanel())
                .build();

        JFreeChart rangeChart = getChart(rangeOptions);

        try {
            profile = activeDataset().getCollection().getProfileCollection().getSegmentedProfile(ProfileType.ANGLE,
                    Landmark.REFERENCE_POINT, Stats.MEDIAN);
        } catch (UnavailableBorderTagException | ProfileException | UnavailableProfileTypeException
                | UnsegmentedProfileException e) {
            LOGGER.log(Loggable.STACK, "Error getting profile", e);

        }

        dualPanel.setCharts(chart, profile, true, rangeChart);
    }

    @Override
    protected synchronized void updateMultiple() {
        JFreeChart mainChart = ProfileChartFactory.createMultipleDatasetEmptyChart();
        JFreeChart rangeChart = ProfileChartFactory.createMultipleDatasetEmptyChart();
        dualPanel.setCharts(mainChart, rangeChart);
        setButtonsEnabled(false);
        buttonStateLbl.setText("Cannot update segments across multiple datasets");
    }

    @Override
    protected synchronized void updateNull() {
        JFreeChart mainChart = ProfileChartFactory.createEmptyChart(null);
        JFreeChart rangeChart = ProfileChartFactory.createEmptyChart(null);
        dualPanel.setCharts(mainChart, rangeChart);
        setButtonsEnabled(false);
        buttonStateLbl.setText("No dataset selected");
    }

    @Override
    public synchronized void setChartsAndTablesLoading() {
        super.setChartsAndTablesLoading();
        dualPanel.setCharts(MorphologyChartFactory.createLoadingChart(), MorphologyChartFactory.createLoadingChart());
    }

    @Override
    protected synchronized JFreeChart createPanelChartType(@NonNull ChartOptions options) {
    	return new ProfileChartFactory(options).createProfileChart();
    }

    /**
     * Enable or disable buttons depending on datasets selected
     * 
     * @param options
     * @throws Exception
     */
    private synchronized void configureButtons(ChartOptions options) {
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
    	
    	if(!options.firstDataset().isRoot()) // only allow resegmentation of root datasets
    		segmentButton.setEnabled(false);
    	
    	ISegmentedProfile medianProfile;
    	try {
    		medianProfile = collection.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE,
    				Landmark.REFERENCE_POINT, Stats.MEDIAN);
    	} catch (UnavailableBorderTagException | ProfileException | UnavailableProfileTypeException
    			| UnsegmentedProfileException e) {
    		LOGGER.log(Loggable.STACK, "Error getting profile", e);
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
    	if (!options.firstDataset().isRoot()) {
    		mergeButton.setEnabled(false);
    		unmergeButton.setEnabled(false);
    		splitButton.setEnabled(false);
    		updatewindowButton.setEnabled(false);
    		buttonStateLbl.setText("Cannot alter child dataset segments - try the root dataset");
    	} else {
    		buttonStateLbl.setText(" ");
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

        Optional<IAnalysisOptions> op = activeDataset().getAnalysisOptions();
        if(op.isPresent())
        	op.get().setAngleWindowProportion(windowSize);

    }

    @Override
    public void actionPerformed(ActionEvent e) {

        try {
            ICellCollection collection = activeDataset().getCollection();
            ISegmentedProfile medianProfile = collection.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE,
                    Landmark.REFERENCE_POINT, Stats.MEDIAN);

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
            LOGGER.warning("Error in action");
            LOGGER.log(Loggable.STACK, "Error in action", e1);
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

    	List<SegMergeItem> names = new ArrayList<>();

    	// Put the names of the mergable segments into a list
    	
    	List<IProfileSegment> segList = medianProfile.getOrderedSegments();
    	for (int i=0; i<segList.size()-1; i++) { // Do not allow merges across the RP
    		IProfileSegment seg = segList.get(i); 
    		SegMergeItem item = new SegMergeItem(seg, seg.nextSegment());
    		names.add(item);
    	}

    	String[] nameArray = names.stream().map(e->e.toString()).toArray(String[]::new);

    	try {
    		int mergeOption = getInputSupplier().
    				requestOption(nameArray, "Choose segments to merge", "Merge");
    		SegMergeItem item = names.get(mergeOption);
    		this.setAnalysing(true);
    		LOGGER.fine("User reqested merge of "+item.getOne().getName()+" and "+item.getTwo().getName());
    		SegmentationHandler sh = new SegmentationHandler(activeDataset());
    		sh.mergeSegments(item.getOne().getID(), item.getTwo().getID());

    		refreshEditingPanelCharts();

    		this.setAnalysing(false);

    	} catch(RequestCancelledException e) {
    		LOGGER.fine("User cancelled segment merge request");
    	}
    }

    private class SegMergeItem {
        private IProfileSegment one, two;

        public SegMergeItem(IProfileSegment one, IProfileSegment two) {
            this.one = one;
            this.two = two;
        }

        @Override
		public String toString() {
            return one.getName() + " - " + two.getName();
        }

        public IProfileSegment getOne() {
            return one;
        }

        public IProfileSegment getTwo() {
            return two;
        }
    }

    private void splitAction(ISegmentedProfile medianProfile) {

    	IProfileSegment[] nameArray = medianProfile.getSegments().toArray(new IProfileSegment[0]);

    	String[] options = Arrays.stream(nameArray).map(IProfileSegment::getName).toArray(String[]::new);

    	try {
    		int option = getInputSupplier().requestOptionAllVisible(options, "Choose segment to split", STR_SPLIT_SEGMENT);

    		setAnalysing(true);
    		SegmentationHandler sh = new SegmentationHandler(activeDataset());
    		sh.splitSegment(nameArray[option].getID());
    		refreshEditingPanelCharts();
    		setAnalysing(false);
    	} catch (RequestCancelledException e) {
    		LOGGER.fine("User cancelled segment split request");
    	}
    }

    /**
     * Unmerge segments in a median profile
     * 
     * @param medianProfile
     * @throws Exception
     */
    private void unmergeAction(ISegmentedProfile medianProfile) throws Exception {

        List<IProfileSegment> names = new ArrayList<>();

        // Put the names of the mergable segments into a list
        for (IProfileSegment seg : medianProfile.getSegments()) {
            if (seg.hasMergeSources()) {
                names.add(seg);
            }
        }
        IProfileSegment[] nameArray = names.toArray(new IProfileSegment[0]);
        String[] options = Arrays.stream(nameArray).map(s->s.getName()).toArray(String[]::new);
        
        try {
    		int option = getInputSupplier().requestOption(options, "Choose merged segment to unmerge", "Unmerge segment");

    		setAnalysing(true);
    		SegmentationHandler sh = new SegmentationHandler(activeDataset());
    		sh.unmergeSegments(nameArray[option].getID());
    		refreshEditingPanelCharts();
    		setAnalysing(false);
    	} catch (RequestCancelledException e) {
    		LOGGER.fine("User cancelled segment unmerge request");
    	}
    }

    @Override
    public void segmentEventReceived(SegmentEvent event) {
        if (event.type.equals(SegmentUpdateType.MOVE_START_INDEX)) {
            LOGGER.finer( "Heard update segment request");
            try {
                setAnalysing(true);
                updateSegmentStartIndexAction(event.id, event.index);

            } catch (Exception e) {
                LOGGER.log(Loggable.STACK, "Error updating segment", e);
            } finally {
                setAnalysing(false);
            }

        }

    }

}
