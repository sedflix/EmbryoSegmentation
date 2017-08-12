import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.io.DirectoryChooser;
import ij.io.FileSaver;
import ij.io.OpenDialog;
import trainableSegmentation.WekaSegmentation;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class WekaSegmentaionStage {

    private final static String title = "Detecting interiors of cell using Weka Segmentation";
    private String inputImageFolder;
    private String outpurImageFolder;
    private String classifierModelFileAddress;
    private WekaSegmentation wekaSegmentaion = new WekaSegmentation();

    public WekaSegmentaionStage(String inputImageFolder, String outpurImageFolder, String classifierModelFileAddress) {
        this.inputImageFolder = inputImageFolder;
        this.outpurImageFolder = outpurImageFolder;
        this.classifierModelFileAddress = classifierModelFileAddress;
        prepare();
    }

    public WekaSegmentaionStage(Frame frame) {
        GenericDialog gd = new GenericDialog(title, frame);
        makeInputForm(gd);
        prepare();

    }

    public WekaSegmentaionStage() {
        GenericDialog gd = new GenericDialog(title);
        makeInputForm(gd);
        prepare();

    }

    private void prepare() {
        wekaSegmentaion.loadClassifier(this.classifierModelFileAddress);
        File inputImagesFolder = new File(inputImageFolder);
        File outputImageFolder = new File(outpurImageFolder);

        if (inputImagesFolder == null && !(inputImagesFolder.isDirectory() && inputImagesFolder.exists())) {
            IJ.error("Invalid input directory");
            return;
        }
        if (outputImageFolder == null && !(outputImageFolder.isDirectory() && outputImageFolder.exists())) {
            IJ.error("Invalid output directory");
            return;
        }

    }

    public static void main(String[] args) {
        WekaSegmentaionStage obj = new WekaSegmentaionStage();
        obj.apply();
    }

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
                        inputImageFolder = directoryChooser.getDirectory();
                        break;
                    case 2:
                        DirectoryChooser directoryChooser2 = new DirectoryChooser(label);
                        jTextField.setText(directoryChooser2.getDirectory());
                        outpurImageFolder = directoryChooser2.getDirectory();
                        break;
                    case 3:
                        OpenDialog openDialog = new OpenDialog(label);
                        jTextField.setText(openDialog.getPath());
                        classifierModelFileAddress = openDialog.getPath();
                        break;

                }

            }
        });
        panel.add(jButton);
        panel.add(jTextField);
        return panel;
    }

    public void apply() {
        File inputImagesFolder = new File(inputImageFolder);
        for (File inputImage : inputImagesFolder.listFiles()) {
            if (inputImage.isFile()) {
                applyClassifier(inputImage);
            }
        }
    }

    private void makeInputForm(GenericDialog genericDialog) {
        genericDialog.addPanel(getChooser("Input Image Folder", 1));
        genericDialog.addPanel(getChooser("Output Folder", 2));
        genericDialog.addPanel(getChooser("Classier model", 3));
        genericDialog.pack();
        genericDialog.showDialog();
        if (genericDialog.wasCanceled()) {
            IJ.error("PlugIn canceled!");
        }

    }

    private void applyClassifier(File imageFile) {
        ImagePlus imagePlus = new ImagePlus(imageFile.getAbsolutePath());
        if (imagePlus != null) {
            // apply classifier and get results (0 indicates number of threads is auto-detected) and true for probability map
            ImagePlus result = wekaSegmentaion.applyClassifier(imagePlus, 0, true);

            //make outputFileName
            String outputFileName = imageFile.getName() + ".tif";
            new FileSaver(result).saveAsTiff(outpurImageFolder + File.separator + outputFileName);

            // force garbage collection (important for large images)
            result = null;
            imagePlus = null;
            System.gc();
        } else {
            IJ.error("Not able to read image");
        }
    }

    public String getOutpurImageFolder() {
        return outpurImageFolder;
    }

    public String getInputImageFolder() {
        return inputImageFolder;
    }
}
