import ij.ImagePlus;
import ij.gui.Roi;

import java.awt.*;

/**
 * Basic function of <code>LevelSetParameters</code> is to provide a better method to manipulate the parameters in LevelSet Algorithms.
 * Details of the parameters can be found in <code>LevelSet</code> PlugIn offered in Fiji distribution of ImageJ. Link is provided here: <a href="https://imagej.net/Level_Sets">https://imagej.net/Level_Sets</a>
 */
public class LevelSetParameters {

    protected Roi roi;
    protected double convergence;
    protected double advection;
    protected double curvature;
    protected double grey_tol;
    protected boolean expandToInside;
    protected int max_iteration;
    protected int step_iteration;
    protected boolean getProgressReport;

    /**
     * One of the major problem with microscopy images is that the illumination is not  uniform.
     * Due to this, we face the problem of Level set overshooting or premature termination of Level Set.
     * This function tries to fix that problem by changing the parameters of Level Sets with respect to that particular.
     * This function uses standard deviation in and around the ROI to change the parameters of Level Sets to prevent the problem of overshooting and premature termination.
     *
     * My approach for the same is as follows:
     * Assumption 1: Cell having bright cell boundry will have greater standard deviation.
     * If I take two ROI(these ROI are like shortest enclosing box for cell) and compare their standard deviation, the cell with higher brightness will higher std dev.
     *
     * Assumption 2: The expanded ROI bounded box will include the cell boundry of the corresponding cell.
     * //TODO: make expansion dynamic as expanding the bounding box by 10 may not result in inclusion of cell boundry.
     *
     * Std Dev is also proportional to the area of the roi. To remove the effect of area I take the difference of outerStdDev and innerStdDev as the parameter. This kind of results in StdDev of area that is supposed ot contain the cell boundry.
     * @param originalImage the original image, ie, not the thresholded or binary image.
     * @param roi           roi that represents interior region of a particular cell in the embryo
     * @author Siddharth Yadav
     */
    public void setParametersForCurveEvolution(ImagePlus originalImage, Roi roi) {
        originalImage.setRoi(roi);

        //innerStdDev stores the Standard Deviation of roi
        double innerStdDev = originalImage.getStatistics().stdDev;

        Rectangle rectangleBounds = roi.getBounds();
        //make roi larger by 10px in both x and y axis
        rectangleBounds.grow(10, 10);

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
        this.advection = 1;
        this.curvature = 1;
        this.expandToInside = false;
        this.getProgressReport = false;


        //higher parameter implies that we can have more strong LevelSet parameter(which ends at strong edges)
        if (parameter < 5) {
            //represents cells with very lightly illuminated cell boundry
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
            //represents cells with highly illuminated cell boundry
            this.convergence = 0.0030;
            this.grey_tol = 3.0;
            this.step_iteration = 50;
            this.max_iteration = 100;
        }
    }


    /*
    getter and setter functions follows below
     */
    public Roi getRoi() {
        return roi;
    }

    public void setRoi(Roi roi) {
        this.roi = roi;
    }

    public double getConvergence() {
        return convergence;
    }

    public void setConvergence(double convergence) {
        this.convergence = convergence;
    }

    public double getAdvection() {
        return advection;
    }

    public void setAdvection(double advection) {
        this.advection = advection;
    }

    public double getCurvature() {
        return curvature;
    }

    public void setCurvature(double curvature) {
        this.curvature = curvature;
    }

    public double getGrey_tol() {
        return grey_tol;
    }

    public void setGrey_tol(double grey_tol) {
        this.grey_tol = grey_tol;
    }

    public boolean isExpandToInside() {
        return expandToInside;
    }

    public void setExpandToInside(boolean expandToInside) {
        this.expandToInside = expandToInside;
    }

    public int getMax_iteration() {
        return max_iteration;
    }

    public void setMax_iteration(int max_iteration) {
        this.max_iteration = max_iteration;
    }

    public int getStep_iteration() {
        return step_iteration;
    }

    public void setStep_iteration(int step_iteration) {
        this.step_iteration = step_iteration;
    }

    public boolean isGetProgressReport() {
        return getProgressReport;
    }

    public void setGetProgressReport(boolean getProgressReport) {
        this.getProgressReport = getProgressReport;
    }
}