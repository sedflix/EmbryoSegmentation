import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.NewImage;
import ij.gui.Roi;
import ij.io.DirectoryChooser;
import ij.io.FileSaver;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.ImageCalculator;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.ImageStatistics;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Level Set Stage.
 * This stage is for changing/modifying the contours/rois found in the previous stage so that now they represent the actual cell membrane.
 * The parameters use for modifying rois(can be called as curve evolution) are present in LevelSetParameters
 * This stage requires the following to do its work:
 * <ul>
 * <li>Original Image</li>
 * <li>Binary image that marks the interior region of each cell in the embryo</li>
 * <li>A binary image that represent boundry of the embryo</li>
 * </ul>
 *
 * @see  LevelSetUtility
 * @see LevelSetParameters
 */
public class LevelSetStage {

    /**
     * contains the original image directory address: Microscopy image
     */
    private String orginalImageDir;

    /**
     * contains threshold(binary) image folder address
     * black represents  interior region of a cell, while white represent any other region plus cell boundry
     */
    private String thresholdImageDir;

    /**
     * /contains binary cellMask image directory address
     * black pixels represents cells that belong to embryo and vice-versa
     */
    private String cellMaskDir;


    private String outputImageDir;

    private final String title = "Level Set Algo Stage";


    /**
     * @param orginalImageDir
     * @param thresholdImageDir
     * @param cellMaskDir
     * @param outputImageDir
     */
    public LevelSetStage(String orginalImageDir, String thresholdImageDir, String cellMaskDir, String outputImageDir) {
        this.orginalImageDir = orginalImageDir;
        this.thresholdImageDir = thresholdImageDir;
        this.cellMaskDir = cellMaskDir;
        this.outputImageDir = outputImageDir;
    }

    public LevelSetStage() {
        GenericDialog gd = new GenericDialog(title);
        makeInputForm(gd);

    }
    /**
     * @param originalImage
     * @param thresholdImage
     * @param cellMask
     * @return RoiManager that contains evolvedROI
     */
    public static RoiManager getEvolvedROIs(ImagePlus originalImage, ImagePlus thresholdImage, ImagePlus cellMask) {


        thresholdImage = refineMask(thresholdImage, cellMask);

        //Declare ROI Manager for Particle Analyzer
        RoiManager rm = new RoiManager();
        ParticleAnalyzer.setRoiManager(rm);

        //ParticleAnalyzer obtained. We apply Particle Analyzer on thresholded Image
        ParticleAnalyzer pa = getParticleAnalyzer();
        pa.analyze(thresholdImage);

        int numberOfRoi = rm.getCount();
        Roi finalRoi[] = new Roi[numberOfRoi];// will store the finally evolved ROI

        LevelSetParameters levelSetParameters = new LevelSetParameters();

        for (int i = 0; i < numberOfRoi; i++) {
            levelSetParameters.setParametersForCurveEvolution(originalImage, rm.getRoi(i));
            ImagePlus result = LevelSetUtility.getSegImage(originalImage, levelSetParameters);

            //to evolved ROI
            IJ.run(result, "Create Selection", "");
            finalRoi[i] = result.getRoi();
        }


        // deletes the previous ROIs.
        originalImage.setOverlay(null);
        thresholdImage.setOverlay(null);
        cellMask.setOverlay(null);
        rm.deselect();
        rm.reset();
        rm.close();

        // Updates ROI manager with properly evolved ROIs
        rm = new RoiManager();
        for (int j = 0; j < numberOfRoi; j++) {
            rm.addRoi(finalRoi[j]);
        }

        return rm;
    }

    /**
     * Plots all rois in a ROI Manager on an image
     * @param rm ROI Manager that contains all the rois that needs to be mentioned in the image
     * @param width width of the resulting image
     * @param height height of the resulting image
     * @return An ImagePlus image with all the rois drwn
     */
    private static ImagePlus roisToImage(RoiManager rm, int width, int height) {
        ImagePlus finalImage = NewImage.createImage("Image with all ROIs in it", width, height, 1, 8, NewImage.FILL_BLACK);

        rm.moveRoisToOverlay(finalImage);
        finalImage.flatten();

        return finalImage;
    }

    /**
     * just another helper function
     * @return a ParticleAnalyser object that can be used in <code>getEvolvedROIs()</code>
     */
    private static ParticleAnalyzer getParticleAnalyzer() {
        //Parameters for ParticleAnalyzer TODO: Research about them
        int opts = ParticleAnalyzer.ADD_TO_MANAGER;
        //Have used only STD_DEV because I've used only Std Dev
        int meas = Measurements.STD_DEV;
        double minSize = Math.PI * Math.pow((10.0 / 2), 2.0);
        double maxSize = Math.PI * Math.pow((300.0 / 2), 2.0);
        return new ParticleAnalyzer(opts, meas, new ResultsTable(), minSize, maxSize);
    }

    /**
     * Combines the cell mask and threshold image to correct a some errors in the classified image
     * @param imagePlus threshold binary image of the classified image [black -> interior region of a cell]
     * @param restrictions cellMask [black -> inside embryo]
     * @return
     */
    private static ImagePlus refineMask(ImagePlus imagePlus, ImagePlus restrictions) {
        IJ.run(restrictions, "Dilate", "");
        IJ.run(restrictions, "Dilate", "");
        IJ.run(restrictions, "Dilate", "");
        IJ.run(restrictions, "Dilate", "");
        ImageCalculator imageCalculator = new ImageCalculator();
        return imageCalculator.run("and create", imagePlus, restrictions);
    }

    /**
     * Code for analysing and detecting overlapping(intersecting) cell(whose overlap ratio(or intersection ratio) is more than x (0.3).
     * Those overlapping ROIs index is added to toBeRemoved Set and then removed.
     *
     * This results in following improvements:
     *  - Effect of over segmentation is rectified here
     *  - small noise in initial stages are rectifies. small noise evolves into big blocks.
     *
     * @param rm it is used only for getting image dimensions
     * @return ROI manager with minimum overlapping Rois
     */
    private static RoiManager removeOverlappingRois(ImagePlus originalImage, RoiManager rm) {

        //will stores the ROI that are not valid
        Set<Integer> tobeRemoved = new HashSet<Integer>();
        int number_of_roi = rm.getCount();

        //loop to go through all possible pair of rois
        for (int i = 0; i < number_of_roi; i++) {
            for (int j = 0; j < i; j++) {

                //temp image for passing to upcoming functions
                ImagePlus roi1ImagePlus = NewImage.createImage("temp image", originalImage.getWidth(), originalImage.getHeight(), 1, 8, NewImage.FILL_WHITE);

                //selects a pair
                rm.setSelectedIndexes(new int[]{i, j});

                //adds the two roi so that we can find the overlapping region
                rm.runCommand(roi1ImagePlus, "AND");

                if (roi1ImagePlus.getRoi() != null) {
                    //if overlapping region exists

                    //ADD operation to get the overlapping portion of roi[i] and roi[j]
                    rm.runCommand(roi1ImagePlus, "ADD");

                    //Bad Code to get what I want.
                    //Get area of the intersectiong region
                    ImageStatistics overlappingRegion = rm.getRoi(rm.getCount() - 1).getStatistics();
                    tobeRemoved.add(rm.getCount() - 1);

                    //finds the ratio between the area of overlapping portion  of that roi and area of that roi
                    ImageStatistics region1 = rm.getRoi(i).getStatistics();
                    ImageStatistics region2 = rm.getRoi(j).getStatistics();
                    double area1 = overlappingRegion.area / region1.area;
                    double area2 = overlappingRegion.area / region2.area;

                    IJ.log("area1: " + area1 + ";  area2: " + area2 + " ; name: " + rm.getRoi(rm.getCount() - 1).getName());

                    //Removes ROI which have 30% or more of area as an overlapping area
                    if (area1 > 0.3) {
                        tobeRemoved.add(i);
                    } else if (area2 > 0.3) {
                        tobeRemoved.add(j);
                    }
                }
            }
        }

        //Removing old and overlapping ROIS
        Object arr[] = tobeRemoved.toArray();
        int arrInt[] = new int[arr.length];
        for (int i = 0; i < arr.length; i++) {
            arrInt[i] = (int) (Integer) arr[i];
        }
        rm.setSelectedIndexes(arrInt);
        rm.runCommand("Delete");
        //Overlapping ROI have been removed

        return rm;
    }


    /**
     *
     * @param originalImage
     * @param thresholdImage
     * @param cellMask
     * @return
     */
    public static ImagePlus apply(ImagePlus originalImage, ImagePlus thresholdImage, ImagePlus cellMask) {
        IJ.run(thresholdImage, "Make Binary", "");
        IJ.run(cellMask, "Make Binary", "");
        ImagePlus imagePlus = roisToImage(removeOverlappingRois(originalImage, getEvolvedROIs(originalImage, thresholdImage, cellMask)), originalImage.getWidth(), originalImage.getHeight());
        RoiManager roiManager = RoiManager.getRoiManager();
        roiManager.close();
        return imagePlus;
    }

    public static void main(String[] args) {

//        ImagePlus add1 = IJ.openImage("/home/sid/Study/GSOC/GSoc/src/data/Data Annotation/Orignal/c369.jpg");
//        ImagePlus add2 = IJ.openImage("/home/sid/Study/GSOC/GSoc/src/data/Data Annotation/YetAnotherThreshold/c369.tif");
//        ImagePlus add3 = IJ.openImage("/home/sid/Study/GSOC/GSoc/src/data/Data Annotation/YetAnotherCellMask/c369.jpg");

//        LevelSetStage levelSetStage = new LevelSetStage(
//                "/home/sid/Study/GSOC/GSoc/src/data/Data Annotation/Orignal/",
//                "/home/sid/Study/GSOC/GSoc/src/data/Data Annotation/YetAnotherThreshold/",
//                "/home/sid/Study/GSOC/GSoc/src/data/Data Annotation/YetAnotherCellMask/",
//                "/home/sid/Study/GSOC/GSoc/src/data/Data Annotation/YetAnotherFinal/");

        LevelSetStage levelSetStage = new LevelSetStage();

        levelSetStage.apply();

//        ImagePlus imagePlus = apply(add1,add2,add3);
//        new FileSaver(imagePlus).saveAsJpeg("/home/sid/Study/GSOC/GSoc/src/data/Data Annotation/" + File.separator + "wow.jpg");


    }

    /**
     *
     * Assumptions:
     * <ul>
     *     <li>All the three folder have the same number of images</li>
     *     <li>They have the same name and sorting them would result in corresponding images in same index</li>
     * </ul>
     */
    public void apply() {

        String[] originalImageList = new File(this.orginalImageDir).list();
        String[] cellMaskList = new File(this.cellMaskDir).list();
        String[] thresholdList = new File(this.thresholdImageDir).list();

        Arrays.sort(originalImageList);
        Arrays.sort(cellMaskList);
        Arrays.sort(thresholdList);

        for (int i = 0; i < originalImageList.length; i++) {

            System.out.println(originalImageList[i]);
            System.out.println(cellMaskList[i]);
            System.out.println(thresholdList[i]);


            ImagePlus orgIm = IJ.openImage(orginalImageDir + originalImageList[i]);
            ImagePlus cellMask = IJ.openImage(cellMaskDir + cellMaskList[i]);
            ImagePlus thresholdIm = IJ.openImage(thresholdImageDir + thresholdList[i]);


            ImagePlus finalResult = LevelSetStage.apply(orgIm, cellMask, thresholdIm);
            new FileSaver(finalResult).saveAsJpeg(outputImageDir + File.separator + originalImageList[i]);
            System.gc();

        }
    }


    /*
Helps to make GUI form
 */
    private void makeInputForm(GenericDialog genericDialog) {
        genericDialog.addPanel(getChooser("Original Image Folder", 1));
        genericDialog.addPanel(getChooser("Threshold Image Folder", 2));
        genericDialog.addPanel(getChooser("Cell Mask Folder", 3));
        genericDialog.addPanel(getChooser("Output Folder", 4));
        genericDialog.pack();
        genericDialog.showDialog();
        if (genericDialog.wasCanceled()) {
            IJ.error("PlugIn canceled!");
        }
    }

    /*
    Helps in making input box
     */
    private Panel getChooser(String label, int factor) {
        Panel panel = new Panel(new GridLayout(2, 1));
        JButton jButton = new JButton(label);
        JTextField jTextField = new JTextField();
        jTextField.setEditable(false);
        jButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                switch (factor) {
                    case 1:
                        DirectoryChooser directoryChooser = new DirectoryChooser(label);
                        jTextField.setText(directoryChooser.getDirectory());
                        orginalImageDir = directoryChooser.getDirectory();
                        break;
                    case 2:
                        DirectoryChooser directoryChooser2 = new DirectoryChooser(label);
                        jTextField.setText(directoryChooser2.getDirectory());
                        thresholdImageDir = directoryChooser2.getDirectory();
                        break;
                    case 3:
                        DirectoryChooser directoryChooser3 = new DirectoryChooser(label);
                        jTextField.setText(directoryChooser3.getDirectory());
                        cellMaskDir = directoryChooser3.getDirectory();
                        break;
                    case 4:
                        DirectoryChooser directoryChooser4 = new DirectoryChooser(label);
                        jTextField.setText(directoryChooser4.getDirectory());
                        outputImageDir = directoryChooser4.getDirectory();
                        break;

                }

            }
        });
        panel.add(jButton);
        panel.add(jTextField);
        return panel;
    }
}
