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

public class ThresholdingStage {
    private static final String title = "Thresholding Stage";
    private String inputDir;
    private String outputDir;
    private double lowerThreshold;
    private double upperThreshold;

    public ThresholdingStage(String inputDir, String outputDir, double lowerThreshold, double upperThreshold) {
        this.inputDir = inputDir;
        this.outputDir = outputDir;
        this.lowerThreshold = lowerThreshold;
        this.upperThreshold = upperThreshold;
    }

    public ThresholdingStage(Frame frame) {
        GenericDialog gd = new GenericDialog(title, frame);
        makeInputForm(gd);
        this.lowerThreshold = (int) gd.getNextNumber();
        this.upperThreshold = (int) gd.getNextNumber();
    }


    public ThresholdingStage() {
        GenericDialog gd = new GenericDialog(title);
        makeInputForm(gd);
        this.lowerThreshold = (int) gd.getNextNumber();
        this.upperThreshold = (int) gd.getNextNumber();
    }

    public static void main(String[] args) {
        ThresholdingStage obj = new ThresholdingStage("/home/sid/Study/GSOC/GSoc/src/data/Data Annotation/YetAnotherProbMap", "/home/sid/Study/GSOC/GSoc/src/data/Data Annotation/YetAnotherThreshold", 0.0, 0.6);
        obj.apply();
    }

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

                }

            }
        });
        panel.add(jButton);
        panel.add(jTextField);
        return panel;
    }

    public void apply() {
        File inputImagesFolder = new File(inputDir);
        for (File inputImage : inputImagesFolder.listFiles()) {
            if (inputImage.isFile()) {
                applyThreshold(inputImage);
            }
        }
    }

    public void applyThreshold(File imageFile) {
        ImagePlus imagePlus = new ImagePlus(imageFile.getAbsolutePath());
        IJ.run(imagePlus, "Delete Slice", "");
        if (imagePlus != null) {

            IJ.setThreshold(imagePlus, lowerThreshold, upperThreshold, "Black & White");
            IJ.run(imagePlus, "Convert to Mask", "");

            //make outputFileName
            String outputFileName = imageFile.getName();
            IJ.log(outputFileName);
            new FileSaver(imagePlus).saveAsTiff(outputDir + File.separator + outputFileName);

            // force garbage collection (important for large images)
            imagePlus = null;
            System.gc();
        } else {
            IJ.error("Not able to read image");
        }
    }
}
