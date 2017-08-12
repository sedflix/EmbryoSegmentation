import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import levelsets.algorithm.ActiveContours;
import levelsets.algorithm.LevelSetImplementation;
import levelsets.ij.ImageContainer;
import levelsets.ij.ImageProgressContainer;
import levelsets.ij.StateContainer;

public class LevelSetUtility {


    public static ImagePlus getWholeCellMask(ImagePlus imagePlus, boolean isDataFine) {
        //TODO: create a case for isDataFine thing
        return getWholeCellMask(imagePlus, 1, 1, 3, 3);
    }

    public static ImagePlus getWholeCellMask(ImagePlus imagePlus, int x_offset, int y_offset, int new_width, int new_height) {
        Roi roi = new Roi(x_offset, y_offset, imagePlus.getWidth() - new_width, imagePlus.getHeight() - new_height);
        return getWholeCellMask(imagePlus, roi);
    }

    public static ImagePlus getWholeCellMask(ImagePlus imagePlus, Roi roi) {
        return getSegImage(imagePlus, roi, 0.0030, 1.0, 1.0, 1, true, 50, 100);
    }


    public static ImagePlus getSegImage(ImagePlus originalImage, double convergence, double grey_tol, int max_iteration, int step_iteration) {

        Roi roi = originalImage.getRoi();
        // parameters
        double advection = 1.0;
        double curvature = 1.0;
        boolean expandToInside = false;

        return getSegImage(originalImage, roi, convergence, advection, curvature, grey_tol, expandToInside, max_iteration, step_iteration);
    }


    public static ImagePlus getSegImage(ImagePlus originalImage, Roi roi, double convergence, double advection, double curvature, double grey_tol, boolean expandToInside, int max_iteration, int step_iteration) {

        return getSegImage(originalImage, roi, convergence, advection, curvature, grey_tol, expandToInside, max_iteration, step_iteration, false);
    }

    public static ImagePlus getSegImage(ImagePlus originalImage, Roi roi, double convergence, double advection, double curvature, double grey_tol, boolean expandToInside, int max_iteration, int step_iteration, boolean getProgressReport) {
        originalImage.setRoi(roi);

        //creating ImageContainer
        ImageContainer ic = new ImageContainer(originalImage);

        //Creating ImageProgressContainer
        ImageProgressContainer progressImage = null;
        if (getProgressReport) {
            progressImage = new ImageProgressContainer();
            progressImage.duplicateImages(ic);
            progressImage.createImagePlus("Segmentation progress of " + originalImage.getTitle());
            progressImage.showProgressStep();
        }

        // Create a initial state map out of the roi
        StateContainer sc_roi = new StateContainer();
        sc_roi.setROI(roi, ic.getWidth(), ic.getHeight(), ic.getImageCount(), originalImage.getCurrentSlice());

        //For which side to evolve. False implies that it will expand to Outside.
        sc_roi.setExpansionToInside(expandToInside);
        LevelSetImplementation ls = new ActiveContours(ic, progressImage, sc_roi, convergence, advection, curvature, grey_tol);

        for (int iter = 0; iter < max_iteration; iter++) {
            if (!ls.step(step_iteration)) {
                break;
            }
        }
        StateContainer sc_final = ls.getStateContainer();

        // Convert sc_final into binary image ImageContainer and display
        if (sc_final == null) {
            IJ.log("Error. Sc_final is null. Yah! I know this message is not helpful.");
        }
        ImageStack stack = new ImageStack(originalImage.getWidth(), originalImage.getHeight());
        for (ImageProcessor bp : sc_final.getIPMask()) {
            stack.addSlice(null, bp);
        }
        ImagePlus seg = originalImage.createImagePlus();
        seg.setStack("Segmentation of " + originalImage.getTitle(), stack);
        seg.setSlice(originalImage.getCurrentSlice());

        return seg;
    }
}
