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


package com.bmskinner.nuclear_morphology.gui.tabs.signals;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.analysis.image.ImageFilterer;
import com.bmskinner.nuclear_morphology.analysis.signals.SignalManager;
import com.bmskinner.nuclear_morphology.analysis.signals.SignalWarper;
import com.bmskinner.nuclear_morphology.charting.charts.ConsensusNucleusChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.OutlineChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclear.UnavailableSignalGroupException;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.gui.components.panels.DatasetSelectionPanel;
import com.bmskinner.nuclear_morphology.gui.components.panels.SignalGroupSelectionPanel;
import com.bmskinner.nuclear_morphology.gui.dialogs.LoadingIconDialog;
import com.bmskinner.nuclear_morphology.gui.tabs.signals.SignalWarpingDialog.ImageCache.Key;
import com.bmskinner.nuclear_morphology.main.ThreadManager;

import ij.process.ImageProcessor;

/**
 * Displays signals warped onto the consensus nucleus of a dataset
 * 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class SignalWarpingDialog extends LoadingIconDialog implements PropertyChangeListener, ActionListener {

    private static final String SOURCE_DATASET_LBL  = "Source dataset";
    private static final String TARGET_DATASET_LBL  = "Target dataset";
    private static final String SIGNAL_GROUP_LBL    = "Signal group";
    private static final String INCLUDE_CELLS_LBL   = "Only include cells with signals";
    private static final String STRAIGHTEN_MESH_LBL = "Straighten meshes";
    private static final String RUN_LBL             = "Run";
    private static final String DIALOG_TITLE        = "Signal warping";
    private static final String COW_LBL             = "Co-warpalise";

    private List<IAnalysisDataset> datasets;
    private ExportableChartPanel   chartPanel;

    private DatasetSelectionPanel datasetBoxOne;
    private DatasetSelectionPanel datasetBoxTwo;

    private SignalGroupSelectionPanel signalBox;

    private JButton   runButton;
    private JCheckBox cellsWithSignalsBox;
    private JCheckBox straightenMeshBox;
    private JButton   cowarpaliseBtn;
    private JSlider   thresholdSlider;

    private SignalWarper warper;

    private JProgressBar progressBar = new JProgressBar(0, 100);

    private JTree tree;

    private ImageCache cache = new ImageCache();

    private boolean ctrlPressed = false;

    private boolean isCtrlPressed() {
        synchronized (SignalWarpingDialog.class) {
            return ctrlPressed;
        }
    }

    /**
     * Construct with a list of datasets available to warp signals to and from
     * 
     * @param datasets
     */
    public SignalWarpingDialog(final List<IAnalysisDataset> datasets) {
        super();
        this.datasets = datasets;
        createUI();
        addCtrlPressListener();
        this.setModal(false);
        this.pack();

        chartPanel.restoreAutoBounds();
        this.setVisible(true);
    }

    private void addCtrlPressListener() {
        // Track when the Ctrl key is down
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {

            @Override
            public boolean dispatchKeyEvent(KeyEvent ke) {
                synchronized (SignalWarpingDialog.class) {
                    switch (ke.getID()) {
                    case KeyEvent.KEY_PRESSED:
                        if (ke.getKeyCode() == KeyEvent.VK_CONTROL) {
                            ctrlPressed = true;
                        }
                        break;

                    case KeyEvent.KEY_RELEASED:
                        if (ke.getKeyCode() == KeyEvent.VK_CONTROL) {
                            ctrlPressed = false;
                        }
                        break;
                    }
                    return false;
                }
            }

        });
    }

    private void createUI() {
        this.setLayout(new BorderLayout());
        this.setTitle(DIALOG_TITLE);

        JPanel header = createHeader();
        this.add(header, BorderLayout.NORTH);

        ChartOptions options = new ChartOptionsBuilder().setDatasets(datasets.get(0)).build();

        JFreeChart chart = new ConsensusNucleusChartFactory(options).makeNucleusOutlineChart();
        chartPanel = new ExportableChartPanel(chart);
        chartPanel.setFixedAspectRatio(true);
        this.add(chartPanel, BorderLayout.CENTER);

        JPanel westPanel = createWestPanel();
        this.add(westPanel, BorderLayout.WEST);

    }

    /**
     * Create the settings header panel
     * 
     * @return
     */
    private JPanel createHeader() {

        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JPanel upperPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel lowerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        datasetBoxOne = new DatasetSelectionPanel(datasets);
        datasetBoxTwo = new DatasetSelectionPanel(datasets);

        datasetBoxOne.setSelectedDataset(datasets.get(0));
        datasetBoxTwo.setSelectedDataset(datasets.get(0));

        datasetBoxOne.addActionListener(e -> {
            if (datasetBoxOne.getSelectedDataset().getCollection().getSignalManager().hasSignals()) {

                signalBox.setDataset(datasetBoxOne.getSelectedDataset());
            }
        });
        datasetBoxTwo.addActionListener(e -> {
            updateBlankChart();
        });

        upperPanel.add(new JLabel(SOURCE_DATASET_LBL));
        upperPanel.add(datasetBoxOne);

        signalBox = new SignalGroupSelectionPanel(datasetBoxOne.getSelectedDataset());

        if (!signalBox.hasSelection()) {
            signalBox.setEnabled(false);
        }

        upperPanel.add(new JLabel(SIGNAL_GROUP_LBL));
        upperPanel.add(signalBox);

        signalBox.addActionListener(this);

        cellsWithSignalsBox = new JCheckBox(INCLUDE_CELLS_LBL, true);
        cellsWithSignalsBox.addActionListener(this);
        upperPanel.add(cellsWithSignalsBox);

        straightenMeshBox = new JCheckBox(STRAIGHTEN_MESH_LBL, false);
        straightenMeshBox.addActionListener(e -> {
            updateBlankChart();
        });

        lowerPanel.add(new JLabel(TARGET_DATASET_LBL));
        lowerPanel.add(datasetBoxTwo);

        runButton = new JButton(RUN_LBL);

        runButton.addActionListener(e -> {

            Runnable task = () -> {
                runWarping();
            };

            ThreadManager.getInstance().submit(task);

        });

        lowerPanel.add(runButton);

        if (!signalBox.hasSelection()) {
            runButton.setEnabled(false);
        }

        cowarpaliseBtn = new JButton(COW_LBL);
        cowarpaliseBtn.addActionListener(e -> {

            if (cache.displayCount() == 2) {
                ImageProcessor w = null;
                Key k0 = cache.getDisplayKeys().get(0);
                Key k1 = cache.getDisplayKeys().get(1);

                if (!k0.target.equals(k1.target)) {
                    updateBlankChart();
                } else {

                    ImageProcessor a = cache.get(k0);
                    ImageProcessor b = cache.get(k1);
                    w = ImageFilterer.cowarpalise(a, b);
                    updateChart(w, k0.target);
                }

            }
        });
//        lowerPanel.add(cowarpaliseBtn); //TODO reenable when ready
        cowarpaliseBtn.setEnabled(false);
        
        
        thresholdSlider = new JSlider(0, 255);
        lowerPanel.add(thresholdSlider);
        thresholdSlider.setVisible(false);
        thresholdSlider.addChangeListener( e-> {
        	if(cache.getDisplayKeys().size()==1){
        		
        		JSlider s = (JSlider) e.getSource();
        		int value = 255 - s.getValue();
        		Key k = cache.getDisplayKeys().get(0);
        		cache.setThreshold(k, value);
        		ImageProcessor display = createDisplayImage();
                updateChart(display, k.target);
        	}
        });

        lowerPanel.add(progressBar);
        progressBar.setVisible(false);

        lowerPanel.add(this.getLoadingLabel());

        headerPanel.add(upperPanel);
        headerPanel.add(lowerPanel);

        return headerPanel;
    }

    /**
     * Create the list of saved warp images
     * 
     * @return
     */
    private JPanel createWestPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        tree = new JTree();

        updateTree();

        tree.addTreeSelectionListener(e -> {

            if (!isCtrlPressed()) {
                cache.clearDisplayImages();
            }

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.getPath().getLastPathComponent();

            Object obj = node.getUserObject();
            if (obj instanceof Key) {

                Key k = (Key) obj;

                if (isCtrlPressed() && cache.hasDisplayImage(k)) {
                    cache.removeDisplayImage(k);
                } else {
                    cache.addDisplayImage(k);
                }
                thresholdSlider.setValue(255-cache.getThreshold(k));
                updateChart(createDisplayImage(), k.target);
            }

            cowarpaliseBtn.setEnabled(cache.displayCount() == 2);
            thresholdSlider.setVisible(cache.displayCount() == 1);
            

        });

        panel.add(tree, BorderLayout.CENTER);
        return panel;
    }

    private void updateTree() {

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Targets");

        for (IAnalysisDataset d : cache.getTargets()) {

            DefaultMutableTreeNode m = new DefaultMutableTreeNode(d.getName());
            root.add(m);
            for (Key k : cache.getKeys(d)) {
                m.add(new DefaultMutableTreeNode(k));
            }

        }

        TreeModel model = new DefaultTreeModel(root);
        tree.setModel(model);

        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }

    }

    /**
     * Run the warper with the currently selected settings
     */
    private void runWarping() {

        finest("Running warping");
        cache.clearDisplayImages();
        thresholdSlider.setVisible(false);
        progressBar.setValue(0);

        IAnalysisDataset sourceDataset = datasetBoxOne.getSelectedDataset();
        IAnalysisDataset targetDataset = datasetBoxTwo.getSelectedDataset();

        boolean cellsWithSignals = cellsWithSignalsBox.isSelected();
        boolean straighten = straightenMeshBox.isSelected();

        Nucleus target = targetDataset.getCollection().getConsensus();

        finest("Signal group: " + signalBox.getSelectedGroup());
        try {
            setStatusLoading();
            setEnabled(false);

            progressBar.setStringPainted(true);

            progressBar.setVisible(true);

            warper = new SignalWarper(sourceDataset, target, signalBox.getSelectedID(), cellsWithSignals, straighten);
            warper.addPropertyChangeListener(this);
            warper.execute();

        } catch (Exception e) {
            error("Error running warping", e);

            ChartOptions options = new ChartOptionsBuilder().setDatasets(targetDataset).build();

            JFreeChart chart = new ConsensusNucleusChartFactory(options).makeNucleusOutlineChart();

            chartPanel.setChart(chart);
            setEnabled(true);
        }
    }

    @Override
    public void setEnabled(boolean b) {
        signalBox.setEnabled(b);
        cellsWithSignalsBox.setEnabled(b);
        straightenMeshBox.setEnabled(b);
        runButton.setEnabled(b);
        datasetBoxOne.setEnabled(b);
        datasetBoxTwo.setEnabled(b);
        // addToImage.setEnabled(b);
        cowarpaliseBtn.setEnabled(b);
    }

    /**
     * Run when the warper is finished. Create the final image for display and
     * set the chart
     */
    public void finished() {
        try {

            ImageProcessor image = warper.get();

            Key k = cache.new Key(datasetBoxTwo.getSelectedDataset(), datasetBoxOne.getSelectedDataset(),
                    signalBox.getSelectedID());

            cache.add(k, image);

            cache.addDisplayImage(k);

            Color c = assignDisplayColour(image);
            cache.setColour(k, c);
            
            cache.setThreshold(k, 255);

            updateTree();
            
            ImageProcessor display = createDisplayImage();

            updateChart(display, k.target);

            setEnabled(true);
            if (cache.displayCount() != 2) {
                cowarpaliseBtn.setEnabled(false);
            }
            setStatusLoaded();

        } catch (Exception e) {
            error("Error getting warp results", e);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {

        if (evt.getNewValue() instanceof Integer) {
            int percent = (Integer) evt.getNewValue(); // should be percent
            if (percent >= 0 && percent <= 100) {
                if (progressBar.isIndeterminate()) {
                    progressBar.setIndeterminate(false);
                }
                progressBar.setValue(percent);
            }
        }

        if (evt.getPropertyName().equals("Finished")) {
            progressBar.setValue(0);
            progressBar.setVisible(false);
            finished();
        }

    }

    private Color assignDisplayColour(final ImageProcessor image) {
        Color colour = Color.WHITE;
        colour = datasetBoxOne.getSelectedDataset().getCollection().getSignalGroup(signalBox.getSelectedID()).get()
		        .getGroupColour().orElse(Color.WHITE);

        return colour;
    }

    /**
     * Create an image for display based on the given greyscale image
     * 
     * @param image
     * @return
     */
    private ImageProcessor createDisplayImage() {

        // Recolour each of the grey images according to the stored colours
        List<ImageProcessor> recoloured = new ArrayList<>();

        for (Key k : cache.getDisplayKeys()) {
            // The image from the warper is greyscale. Change to use the signal
            // colour
        	
        	ImageProcessor raw = cache.get(k);
        	ImageProcessor thresh = raw.duplicate();
        	ImageProcessor recol = ImageFilterer.recolorImage(thresh, cache.getColour(k));
        	recol.setMinAndMax(0, cache.getThreshold(k));
            recoloured.add(recol);
        }

        if (cache.displayCount() == 0) {
            return ImageFilterer.createBlankByteProcessor(100, 100);
        }

        if (cache.displayCount() == 1) {
            return recoloured.get(0);
        }

        // If multiple images are in the list, make an average of their RGB
        // values
        // so territories can be compared
        try {
            ImageProcessor averaged = ImageFilterer.averageRGBImages(recoloured);
            return averaged;

        } catch (Exception e) {
            warn("Error averaging images");
            stack(e);
            return ImageFilterer.createBlankByteProcessor(100, 100);
        }

    }

    /**
     * Update the chart to display the given image over the nucleus outline for
     * dataset two
     * 
     * @param image
     */
    private void updateChart(final ImageProcessor image, IAnalysisDataset target) {

        Runnable task = () -> {
            fine("Updating chart");
            boolean straighten = straightenMeshBox.isSelected();

            ChartOptions options = new ChartOptionsBuilder().setDatasets(target).setShowXAxis(false).setShowYAxis(false)
                    .setShowBounds(false).setStraightenMesh(straighten).build();

            final JFreeChart chart = new OutlineChartFactory(options).makeSignalWarpChart(image);

            chartPanel.setChart(chart);
            chartPanel.restoreAutoBounds();

        };
        ThreadManager.getInstance().submit(task);

    }

    /**
     * Display the nucleus outline for dataset two
     * 
     */
    private void updateBlankChart() {

        fine("Updating blank chart");
        JFreeChart chart = null;
        if (straightenMeshBox.isSelected()) {

            ChartOptions options = new ChartOptionsBuilder().setDatasets(datasetBoxTwo.getSelectedDataset())
                    .setShowMesh(true).setStraightenMesh(true).setShowAnnotations(false).build();

            try {
                chart = new ConsensusNucleusChartFactory(options).makeConsensusChart();
            } catch (Exception ex) {
                error("Error making straight mesh chart", ex);
                chart = ConsensusNucleusChartFactory.makeErrorChart();
            }

        } else {

            ChartOptions options = new ChartOptionsBuilder().setDatasets(datasetBoxTwo.getSelectedDataset()).build();

            chart = new ConsensusNucleusChartFactory(options).makeNucleusOutlineChart();

        }
        chartPanel.setChart(chart);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        IAnalysisDataset sourceDataset = datasetBoxOne.getSelectedDataset();

        SignalManager m = sourceDataset.getCollection().getSignalManager();
        if (!m.hasSignals()) {
            signalBox.setEnabled(false);
            cellsWithSignalsBox.setEnabled(false);
            straightenMeshBox.setEnabled(false);
            runButton.setEnabled(false);
            datasetBoxTwo.setEnabled(false);

        } else {
            signalBox.setEnabled(true);
            cellsWithSignalsBox.setEnabled(true);
            straightenMeshBox.setEnabled(true);
            runButton.setEnabled(true);
            datasetBoxTwo.setEnabled(true);
        }

    }

    /**
     * Store the warped images
     * 
     * @author ben
     * @since 1.13.7
     *
     */
    public class ImageCache {

        final private Map<Key, ImageProcessor> map  = new HashMap<>();   // all generated images
        final private List<Key> displayImages = new ArrayList<>(); // images currently displayed
        final private Map<Key, Color> imageColours  = new HashMap<>();   // colours for warped images
        final private Map<Key, Integer> thresholds  = new HashMap<>();   // thresholds for warped images

        public void addDisplayImage(Key k) {
            displayImages.add(k);
        }

        public void addDisplayImage(IAnalysisDataset target, IAnalysisDataset template, UUID signalGroupId) {
            displayImages.add(new Key(target, template, signalGroupId));
        }

        public void removeDisplayImage(Key k) {
            displayImages.remove(k);
        }

        public boolean hasDisplayImage(Key k) {
            return displayImages.contains(k);
        }

        public int displayCount() {
            return displayImages.size();
        }

        public void clearDisplayImages() {
            displayImages.clear();
        }

        public List<ImageProcessor> getDisplayImages() {
            return map.entrySet().stream().filter(e -> displayImages.contains(e.getKey())).map(e -> e.getValue())
                    .collect(Collectors.toList());
        }

        public List<Key> getDisplayKeys() {
            return displayImages;
        }
        
        public int getThreshold(Key k){
        	return thresholds.get(k);
        }
        
        /**
         * Set the thresholding value for the image. This is the minimum intensity
         * to display. 
         * @param k
         * @param i
         */
        public void setThreshold(Key k, int i) {
        	thresholds.put(k, i);
        }

        public Color getColour(Key k) {
            return imageColours.get(k);
        }

        public void setColour(Key k, Color c) {
            imageColours.put(k, c);
        }

        public void add(Key k, ImageProcessor ip) {
            map.put(k, ip);
        }

        public void add(IAnalysisDataset target, IAnalysisDataset template, UUID signalGroupId, ImageProcessor ip) {
            map.put(new Key(target, template, signalGroupId), ip);
        }

        public ImageProcessor get(Key k) {
            return map.get(k);
        }

        public List<IAnalysisDataset> getTargets() {
            return map.keySet().stream().map(k -> {
                return k.target;
            }).distinct().collect(Collectors.toList());
        }

        public List<Key> getKeys(IAnalysisDataset n) {
            return map.keySet().stream().filter(k -> k.target.getId().equals(n.getId()))
                    .collect(Collectors.toList());
        }

        /**
         * Key to store warped images
         * 
         * @author ben
         *
         */
        public class Key {

            private IAnalysisDataset target;
            private IAnalysisDataset template;
            private UUID             signalGroupId;

            public Key(IAnalysisDataset target, IAnalysisDataset template, UUID signalGroupId) {
                this.target = target;
                this.template = template;
                this.signalGroupId = signalGroupId;
            }

            @Override
            public int hashCode() {
                final int prime = 31;
                int result = 1;
                result = prime * result + getOuterType().hashCode();
                result = prime * result + ((signalGroupId == null) ? 0 : signalGroupId.hashCode());
                result = prime * result + ((target == null) ? 0 : target.hashCode());
                result = prime * result + ((template == null) ? 0 : template.hashCode());
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
                Key other = (Key) obj;
                if (!getOuterType().equals(other.getOuterType()))
                    return false;
                if (signalGroupId == null) {
                    if (other.signalGroupId != null)
                        return false;
                } else if (!signalGroupId.equals(other.signalGroupId))
                    return false;
                if (target == null) {
                    if (other.target != null)
                        return false;
                } else if (!target.equals(other.target))
                    return false;
                if (template == null) {
                    if (other.template != null)
                        return false;
                } else if (!template.equals(other.template))
                    return false;
                return true;
            }

            private SignalWarpingDialog getOuterType() {
                return SignalWarpingDialog.this;
            }

            @Override
            public String toString() {
                return template.getName() + " - "
				        + template.getCollection().getSignalManager().getSignalGroupName(signalGroupId);
            }

        }

    }

}
