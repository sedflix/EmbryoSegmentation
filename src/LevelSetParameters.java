import ij.ImagePlus;
import ij.gui.Roi;

import java.awt.*;

/**
 * Basic function of this is to provide a better method to manipulate the parameters in LevelSet Algorithms.
 * Details of the parameters can be found in LevelSet PlugIn offered in Fiji distribution of ImageJ. Link is provided here: https://imagej.net/Level_Sets
 */
public class LevelSetParameters {

    Roi roi;
    double convergence;
    double advection;
    double curvature;
    double grey_tol;
    boolean expandToInside;
    int max_iteration;
    int step_iteration;
    boolean getProgressReport;

    /**
     * One of the major problem with microscopy images is that the illumination is not  uniform.
     * Due to this, we face the problem of Level set overshooting or premature termination of Level Set.
     * This function tries to fix that problem by changing the parameters of Level Sets with respect to that particular.
     * This function uses standard deviation in and around the ROI to change the parameters of Level Sets to prevent the problem of overshooting and premature termination.
     *
     * @param originalImage the original image, ie, not the thresholded or binary image.
     * @param roi           roi that represents interior region of a particular cell in the embryo
     */
    public void setParametersForCurveEvolution(ImagePlus originalImage, Roi roi) {
        originalImage.setRoi(roi);

        //innerStdDev stores the Standard Deviation of roi
        double innerStdDev = originalImage.getStatistics().stdDev;

        Rectangle rectangleBounds = roi.getBounds();
        //make roi larger by 10px in both x and y axis
        rectangleBounds.grow(10, 10); //TODO: make this dynamic

        Roi expandedRoi = new Roi(rectangleBounds);
        originalImage.setRoi(expandedRoi);

        //outerStdDev stores the Standard Deviation of the expanded bound of roi, ie, enclosing square of the roi, expanded by 10 px.
        double outerStdDev = expandedRoi.getStatistics().stdDev;

        roi.setName((outerStdDev - innerStdDev) + ":");
//            IJ.log("\n" + (outerStdDev - innerStdDev )+ "  :  " + roi.getName() + "\n");

        //parameter is directly proportional to the illumination of the boundry of the cell that roi corresponds to
        //the brighter the cell boundry the higher the parameter
        int parameter = (int) (outerStdDev - innerStdDev);

        this.roi = roi;
        this.grey_tol = 1;
        this.curvature = 1;
        this.expandToInside = true;
        this.getProgressReport = false;


        if (parameter < 5) {
            this.convergence = 0.0070;
            this.step_iteration = 10;
            this.max_iteration = 50;
        } else if (parameter < 12) {
            this.convergence = 0.0060;
            this.step_iteration = 25;
            this.max_iteration = 50;
        } else if (parameter < 20) {
            this.convergence = 0.0050;
            this.step_iteration = 25;
            this.max_iteration = 100;
        } else {
            this.convergence = 0.0030;
            this.grey_tol = 3.0;
            this.step_iteration = 50;
            this.max_iteration = 100;
        }
    }
}