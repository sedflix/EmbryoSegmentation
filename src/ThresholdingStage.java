import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.io.DirectoryChooser;
import ij.io.FileSaver;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * <code>ThresholdingStage</code> is the stage for converting the Probability map we get from Weka Segmentation Stage to a binary image, where black represents cell boundry and white represents other area.
 * <p>
 * TODO: Add the following methods: GraphCut, Local Threshold, Adaptive threshold
 */
public class ThresholdingStage {
    private static final String title = "Thresholding Stage";
    private String inputDir;
    private String outputDir;
    private double lowerThreshold;
    private double upperThreshold;

    /**
     * Constructor.
     * Pass the address of directory containing the input images and the directory in which you want to store the output images
     * and the specify the lower and upper threshold: 0.0 < lowerThreshold < upperThreshold < 1.0
     *
     * We recommend you to explore the probability map and specify a proper threshold limit.
     *
     * @param inputDir absolute path of the directory of which all the images would be read
     * @param outputDir absolute path of the directory for storing the output images
     * @param lowerThreshold specifies the lower threshold for thresholding 0.0 < lowerThreshold < upperThreshold
     * @param upperThreshold specifies the upper threshold for thresholding lowerThreshold < lowerThreshold < 1.0
     */
    public ThresholdingStage(String inputDir, String outputDir, double lowerThreshold, double upperThreshold) {
        this.inputDir = inputDir;
        this.outputDir = outputDir;
        this.lowerThreshold = lowerThreshold;
        this.upperThreshold = upperThreshold;
    }

    /**
     * Constructor.
     * Creates a pop up to get the lowerThreshold and upperThreshold
     * Pass the address of directory containing the input images and the directory in which you want to store the output images
     * @param inputDir absolute path of the directory of which all the images would be read
     * @param outputDir absolute path of the directory for storing the output images
     */
    public ThresholdingStage(String inputDir, String outputDir) {
        GenericDialog gd = new GenericDialog(title);
        gd.addNumericField("Select Lower Threshold", 0.0, 3);
        gd.addNumericField("Select Lower Threshold", 0.5, 3);


    }

    /**
     * Creates a pop up to get inputDir, outputDir and threshold limits
     * @param frame the parent of frame into which this can inserted
     */
    public ThresholdingStage(Frame frame) {
        GenericDialog gd = new GenericDialog(title, frame);
        makeInputForm(gd);
        this.lowerThreshold = (int) gd.getNextNumber();
        this.upperThreshold = (int) gd.getNextNumber();
        gd.pack();
        gd.showDialog();
        if (gd.wasCanceled()) {
            IJ.error("PlugIn canceled!");
        }

    }

    /**
     * Creates a pop up to get inputDir, outputDir and threshold limits
     */
    public ThresholdingStage() {
        GenericDialog gd = new GenericDialog(title);
        makeInputForm(gd);
        this.lowerThreshold = (int) gd.getNextNumber();
        this.upperThreshold = (int) gd.getNextNumber();
    }

    /**
     * @param imagePlus
     * @param lowerThreshold
     * @param upperThreshold
     * @return
     */
    public static ImagePlus applyThreshold(ImagePlus imagePlus, double lowerThreshold, double upperThreshold) {
        IJ.setThreshold(imagePlus, lowerThreshold, upperThreshold, "Black & White");
        IJ.run(imagePlus, "Convert to Mask", "");
        return imagePlus;
    }

    /**
     * Returns an image with less noise
     *
     * @param imagePlus Raw threshold image(binary)
     * @return A image(binary) with less noise
     */
    private static ImagePlus morph(ImagePlus imagePlus) {
        //Morphological Operations
        // TODO: Improve
        // Try Watershed Segmentation for preventing under-segmentation
        IJ.run(imagePlus, "Close-", "");
        IJ.run(imagePlus, "Close-", "");
        IJ.run(imagePlus, "Fill Holes", "");
        IJ.run(imagePlus, "Close-", "");
        IJ.run(imagePlus, "Close-", "");
        IJ.run(imagePlus, "Dilate", "");
        IJ.run(imagePlus, "Erode", "");
        IJ.run(imagePlus, "Erode", "");

        return imagePlus;
    }

    /*
    Helps in making the input form
     */
    private void makeInputForm(GenericDialog genericDialog) {
        genericDialog.addPanel(getChooser("Input Folder", 1));
        genericDialog.addPanel(getChooser("Output Folder", 2));
        genericDialog.addNumericField("Select Lower Threshold", 0.0, 3);
        genericDialog.addNumericField("Select Lower Threshold", 50.0, 3);
        genericDialog.pack();
        genericDialog.showDialog();
        if (genericDialog.wasCanceled()) {
            IJ.error("PlugIn canceled!");
        }
    }

    /*
    Helps in making different kind of fields for input
     */
    private Panel getChooser(String label, int factor) {
        Panel panel = new Panel(new GridLayout(2, 0));
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
                        inputDir = directoryChooser.getDirectory();
                        break;
                    case 2:
                        DirectoryChooser directoryChooser2 = new DirectoryChooser(label);
                        jTextField.setText(directoryChooser2.getDirectory());
                        outputDir = directoryChooser2.getDirectory();
                        break;
                    default:
                        System.out.println("What the hell happened here?");
                }

            }
        });
        panel.add(jButton);
        panel.add(jTextField);
        return panel;
    }

    /**
     * loops over all images in <code>inputDir</code> and calls the function <code>applyThreshold(ImagePlus)</code>
     */
    public void apply() {
        File inputImagesFolder = new File(inputDir);
        for (File inputImage : inputImagesFolder.listFiles()) {
            if (inputImage.isFile()) {
                applyThreshold(inputImage);
            }
        }
    }

    /**
     * Apply threshold(using previously specified threshold values) and save the result in the specified outputDir
     * @param imageFile File object that specifies that input image file
     */
    public void applyThreshold(File imageFile) {
        ImagePlus imagePlus = new ImagePlus(imageFile.getAbsolutePath());
        //TODO: Handle this Slice Thing
        IJ.run(imagePlus, "Delete Slice", "");
        if (imagePlus != null) {

            IJ.setThreshold(imagePlus, lowerThreshold, upperThreshold, "Black & White");
            IJ.run(imagePlus, "Convert to Mask", "");
            imagePlus = morph(imagePlus);
            //make outputFileName
            String outputFileName = imageFile.getName();
            IJ.log(outputFileName);
            new FileSaver(imagePlus).saveAsJpeg(outputDir + File.separator + outputFileName);

            // force garbage collection (important for large images)
            imagePlus = null;
            System.gc();
        } else {
            IJ.error("Not able to read image");
        }
    }

    public static void main(String[] args) {
        ThresholdingStage obj = new ThresholdingStage("/home/sid/Study/GSOC/GSoc/src/data/Data Annotation/YetAnotherProbMap", "/home/sid/Study/GSOC/GSoc/src/data/Data Annotation/YetAnotherThreshold", 0.0, 0.6);
        obj.apply();
    }
}
