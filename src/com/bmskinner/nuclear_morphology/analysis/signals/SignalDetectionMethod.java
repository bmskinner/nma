package com.bmskinner.nuclear_morphology.analysis.signals;

import ij.ImageStack;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.bmskinner.nuclear_morphology.analysis.AbstractAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.components.ChildAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.VirtualCellCollection;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.nuclear.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalCollection;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclear.UnavailableSignalGroupException;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.IMutableNuclearSignalOptions;
import com.bmskinner.nuclear_morphology.components.options.INuclearSignalOptions;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;

public class SignalDetectionMethod extends AbstractAnalysisMethod {

    protected IMutableNuclearSignalOptions options = null;
    protected File                         folder;
    protected int                          channel;
    protected UUID                         signalGroup;
    protected String                       channelName;

    /**
     * For use when running on an existing dataset
     * 
     * @param d
     *            the dataset to add signals to
     * @param folder
     *            the folder of images
     * @param channel
     *            the RGB channel to search
     * @param options
     *            the analysis options
     * @param group
     *            the signal group to add signals to
     * @throws UnavailableSignalGroupException
     */

    public SignalDetectionMethod(IAnalysisDataset d, INuclearSignalOptions options, UUID group)
            throws UnavailableSignalGroupException {
        super(d);

        this.options = (IMutableNuclearSignalOptions) options.duplicate();
        this.folder = options.getFolder();
        this.channel = options.getChannel();
        this.signalGroup = group;
        this.channelName = d.getCollection().getSignalGroup(group).getGroupName();

    }

    @Override
    public IAnalysisResult call() throws Exception {

        run();
        postDetectionFilter();
        IAnalysisResult r = new DefaultAnalysisResult(dataset);

        return r;
    }

    protected void run() throws Exception {

        fine("Beginning signal detection in channel " + channel);

        try {

            int originalMinThreshold = options.getThreshold();

            SignalFinder finder = new SignalFinder(dataset.getAnalysisOptions(), options, dataset.getCollection());
            // SignalDetector finder = new SignalDetector(options, channel);

            for (ICell c : dataset.getCollection().getCells()) {

                // reset the min threshold for each cell
                options.setThreshold(originalMinThreshold);

                Nucleus n = c.getNucleus();
                finer("Looking for signals associated with nucleus " + n.getSourceFileName() + "-"
                        + n.getNucleusNumber());

                // get the image in the folder with the same name as the
                // nucleus source image
                File imageFile = new File(folder + File.separator + n.getSourceFileName());
                finer("Source file: " + imageFile.getAbsolutePath());

                try {

                    // ImageStack stack = new
                    // ImageImporter(imageFile).importToStack();

                    List<INuclearSignal> signals = finder.findInImage(imageFile, n);
                    // List<INuclearSignal> signals =
                    // finder.detectSignal(imageFile, stack, n);

                    finer("Creating signal collection");

                    ISignalCollection signalCollection = n.getSignalCollection();
                    signalCollection.addSignalGroup(signals, signalGroup, imageFile, channel);

                    SignalAnalyser s = new SignalAnalyser();
                    s.calculateSignalDistancesFromCoM(n);
                    s.calculateFractionalSignalDistancesFromCoM(n);

                    fine("Calculating signal angles");

                    // If the nucleus is asymmetric, calculate angles
                    if (!dataset.getCollection().getNucleusType().equals(NucleusType.ROUND)) {

                        finer("Nucleus type is asymmetric: " + n.getClass().getSimpleName());

                        if (n.hasBorderTag(Tag.ORIENTATION_POINT)) {
                            finest("Calculating angle from orientation point");
                            n.calculateSignalAnglesFromPoint(n.getBorderPoint(Tag.ORIENTATION_POINT));
                        } else {
                            finest("No orientation point in nucleus");
                        }

                    } else {
                        finer("Nucleus type is round: " + n.getClass().getSimpleName());
                    }

                } catch (ImageImportException e) {
                    warn("Cannot open " + imageFile.getAbsolutePath());
                    stack("Cannot load image", e);
                }

                fireProgressEvent();
            }

        } catch (Exception e) {
            stack("Error in signal detection", e);
        }
    }

    private void postDetectionFilter() {

        List<ICellCollection> signalPopulations = dividePopulationBySignals(dataset.getCollection(), signalGroup);

        List<IAnalysisDataset> list = new ArrayList<IAnalysisDataset>();

        for (ICellCollection collection : signalPopulations) {
            finer("Processing " + collection.getName());
            processSubPopulation(collection);
            finer("Processed " + collection.getName());
            list.add(dataset.getChildDataset(collection.getID()));
        }

        fine("Finished processing sub-populations");
    }

    /**
     * Create child datasets for signal populations and perform basic analyses
     * 
     * @param collection
     */
    private void processSubPopulation(ICellCollection collection) {

        try {
            finer("Creating new analysis dataset for " + collection.getName());

            IAnalysisDataset subDataset = new ChildAnalysisDataset(dataset, collection);

            dataset.addChildDataset(subDataset);
            dataset.getCollection().getProfileManager().copyCollectionOffsets(collection);

        } catch (Exception e) {
            error("Error processing signal group", e);
        }
    }

    /**
     * Create two child populations for the given dataset: one with signals in
     * the given group, and one without signals
     * 
     * @param r
     *            the collection to split
     * @param signalGroup
     *            the signal group to split on
     * @return a list of new collections
     */
    private List<ICellCollection> dividePopulationBySignals(ICellCollection r, UUID signalGroup) {

        List<ICellCollection> signalPopulations = new ArrayList<ICellCollection>(0);
        log("Dividing population by signals...");

        ISignalGroup group;
        try {
            group = r.getSignalGroup(signalGroup);

            group.setVisible(true);

            Set<ICell> list = r.getSignalManager().getCellsWithNuclearSignals(signalGroup, true);
            if (!list.isEmpty()) {
                log("Signal group " + group.getGroupName() + ": found nuclei with signals");
                ICellCollection listCollection = new VirtualCellCollection(dataset,
                        group.getGroupName() + "_with_signals");

                for (ICell c : list) {

                    finer("  Added cell: " + c.getNucleus().getNameAndNumber());
                    // ICell newCell = new DefaultCell(c);
                    listCollection.addCell(c);
                }
                signalPopulations.add(listCollection);

                // Only add a group of cells without signals if at least one
                // cell does havea signal

                Set<ICell> notList = r.getSignalManager().getCellsWithNuclearSignals(signalGroup, false);
                if (!notList.isEmpty()) {
                    log("Signal group " + r.getSignalGroup(signalGroup).getGroupName()
                            + ": found nuclei without signals");

                    ICellCollection notListCollection = new VirtualCellCollection(dataset,
                            group.getGroupName() + "_without_signals");

                    for (ICell c : notList) {
                        notListCollection.addCell(c);
                    }

                    signalPopulations.add(notListCollection);
                } else {
                    finest("No cells without signals");
                }

            }

        } catch (UnavailableSignalGroupException e) {
            error("Cannot get signal group from collection", e);
            return new ArrayList<ICellCollection>(0);
        }

        fine("Finished dividing populations based on signals");
        return signalPopulations;
    }

}
