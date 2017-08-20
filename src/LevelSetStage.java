import ij.IJ;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.ImageCalculator;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.ImageStatistics;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Level Set Stage.
 * This stage is for changing/modifying the contours/rois found in the previous stage so that now they represent the actual cell membrane.
 * The parameters use for modifying rois(can be called as curve evolution) are present in LevelSetParameters
 * This stage requires the following to do its work
 * - Original Image
 * - Binary image that marks the interior region of each cell in the embryo
 * - A binary image that represent boundry of the embryo
 *
 * @See LevelSetUtility, LevelSetParameters
 */
public class LevelSetStage {
    private String orginalImageDir;
    private String thresholdImageDir;
    private String cellMaskDir;
    private String outputImageDir;

    /**
     * @param originalImage
     * @param thresholdImage
     * @param cellMask
     * @return
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

    private static ImagePlus roisToImage(RoiManager rm, int width, int height) {
        ImagePlus finalImage = NewImage.createImage("Image with all ROIs in it", width, height, 1, 8, NewImage.FILL_BLACK);

        rm.moveRoisToOverlay(finalImage);
        finalImage.flatten();

        return finalImage;
    }

    private static ParticleAnalyzer getParticleAnalyzer() {
        //Parameters for ParticleAnalyzer TODO: Research about them
        int opts = ParticleAnalyzer.ADD_TO_MANAGER;
        //Have used only STD_DEV because I've used only Std Dev
        int meas = Measurements.STD_DEV;
        double minSize = Math.PI * Math.pow((10.0 / 2), 2.0);
        double maxSize = Math.PI * Math.pow((300.0 / 2), 2.0);
        return new ParticleAnalyzer(opts, meas, new ResultsTable(), minSize, maxSize);
    }

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

    public static void main(String[] args) {

        ImagePlus add1 = IJ.openImage("/home/sid/Study/GSOC/GSoc/src/data/Data Annotation/Orignal/c369.jpg");
        ImagePlus add2 = IJ.openImage("/home/sid/Study/GSOC/GSoc/src/data/Data Annotation/YetAnotherThreshold/c369.tif");
        ImagePlus add3 = IJ.openImage("/home/sid/Study/GSOC/GSoc/src/data/Data Annotation/YetAnotherCellMask/c369.jpg");
        IJ.run(add2, "Make Binary", "");
        IJ.run(add3, "Make Binary", "");
        add1.show();
        add2.show();
        add3.show();


        ImagePlus imagePlus = roisToImage(removeOverlappingRois(add1, getEvolvedROIs(add1, add2, add3)), add1.getWidth(), add1.getHeight());
        new FileSaver(imagePlus).saveAsJpeg("/home/sid/Study/GSOC/GSoc/src/data/Data Annotation/" + File.separator + "wow.jpg");


    }
}
