import ij.IJ;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.ImageCalculator;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.ImageStatistics;

import java.util.HashSet;
import java.util.Set;

public class LevelSetStage {
    private String orginalImageDir;
    private String thresholdImageDir;
    private String cellMaskDir;
    private String outputImageDir;


    public static RoiManager apply(ImagePlus originalImage, ImagePlus thresholdImage, ImagePlus cellMask) {
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
        rm.deselect();
        rm.close();

        // Updates ROI manager with properly evolved ROIs
        rm = new RoiManager();
        for (int j = 0; j < numberOfRoi; j++) {
            rm.addRoi(finalRoi[j]);
        }

        // @see removeOverlappingRois()
        rm = removeOverlappingRois(originalImage, rm);

        return rm;

//        ImagePlus finalImage = NewImage.createImage("OutputForBenchmarking", originalImage.getWidth(), originalImage.getHeight(), 1, 8, NewImage.FILL_BLACK);
//
//        rm.moveRoisToOverlay(finalImage);
//        finalImage.flatten();
//
//        return finalImage;
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
     * Code for analysing and detecting overlapping cell(whose overlap ratio is more than x (0.3).
     * Those overlapping ROIs index is added to toBeRemoved Set
     *
     * @param rm
     * @return
     */
    private static RoiManager removeOverlappingRois(ImagePlus originalImage, RoiManager rm) {
        //will stores the ROI that are not valid
        Set<Integer> tobeRemoved = new HashSet<Integer>();
        int number_of_roi = rm.getCount();

        for (int i = 0; i < number_of_roi; i++) {
            for (int j = 0; j < i; j++) {

                ImagePlus roi1ImagePlus = NewImage.createImage("temp image", originalImage.getWidth(), originalImage.getHeight(), 1, 8, NewImage.FILL_WHITE);
                rm.setSelectedIndexes(new int[]{i, j});
                rm.runCommand(roi1ImagePlus, "AND");
                if (roi1ImagePlus.getRoi() != null) {
                    rm.runCommand(roi1ImagePlus, "ADD");
                    ImageStatistics overlappingRegion = rm.getRoi(rm.getCount() - 1).getStatistics();
                    tobeRemoved.add(rm.getCount() - 1);
                    ImageStatistics region1 = rm.getRoi(i).getStatistics();
                    ImageStatistics region2 = rm.getRoi(j).getStatistics();
                    double area1 = overlappingRegion.area / region1.area;
                    double area2 = overlappingRegion.area / region2.area;

                    IJ.log("area1: " + area1 + ";  area2: " + area2 + " ; name: " + rm.getRoi(rm.getCount() - 1).getName());

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

        ImagePlus add1 = IJ.openImage("/home/sid/Study/GSOC/GSoc/src/data/Data Annotation/Orignal/348.jpg");
        ImagePlus add2 = IJ.openImage("/home/sid/Study/GSOC/GSoc/src/data/Data Annotation/YetAnotherThreshold/348.tif");
        ImagePlus add3 = IJ.openImage("/home/sid/Study/GSOC/GSoc/src/data/Data Annotation/YetAnotherCellMask/348.jpg");
        IJ.run(add2, "Make Binary", "");
        IJ.run(add3, "Make Binary", "");
        add1.show();
        add2.show();
        add3.show();
//        ImagePlus imagePlus = apply(add1,add2,add3);
//        new FileSaver(imagePlus).saveAsJpeg("/home/sid/Study/GSOC/GSoc/src/data/Data Annotation/" + File.separator + "wow.jpg");


    }
}
