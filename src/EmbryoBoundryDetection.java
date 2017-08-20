import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.io.DirectoryChooser;
import ij.io.FileSaver;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class EmbryoBoundryDetection {
    private String inputDir;
    private String outputDir;
    private static final String title = "Boundry Detection";

    public EmbryoBoundryDetection(String inputDir, String outputDir) {
        this.inputDir = inputDir;
        this.outputDir = outputDir;
    }

    public EmbryoBoundryDetection() {
        GenericDialog gd = new GenericDialog(title);
        makeInputForm(gd);
    }

    public static ImagePlus apply(ImagePlus imagePlus) {
        if (imagePlus == null) {
            IJ.error("Unable to read image");
        }
        //TODO: Add detection of not-data-fine case and processing
        return getWholeCellMask(imagePlus, true);
    }

    public void apply() {
        File inputImagesFolder = new File(inputDir);
        for (File inputImage : inputImagesFolder.listFiles()) {
            if (inputImage.isFile()) {
                ImagePlus imagePlus = new ImagePlus(inputImage.getAbsolutePath());
                imagePlus = apply(imagePlus);
                new FileSaver(imagePlus).saveAsJpeg(outputDir + File.separator + inputImage.getName());
            }
        }
    }


    public static ImagePlus getWholeCellMask(ImagePlus imagePlus, boolean isDataFine) {
        //TODO: create a case for isDataFine thing
        return getWholeCellMask(imagePlus, 1, 1, 3, 3);
    }

    public static ImagePlus getWholeCellMask(ImagePlus imagePlus, int x_offset, int y_offset, int new_width_offset, int new_height_offset) {
        Roi roi = new Roi(x_offset, y_offset, imagePlus.getWidth() - new_width_offset, imagePlus.getHeight() - new_height_offset);
        return getWholeCellMask(imagePlus, roi);
    }

    public static ImagePlus getWholeCellMask(ImagePlus imagePlus, Roi roi) {
        return LevelSetUtility.getSegImage(imagePlus, roi, 0.0030, 1.0, 1.0, 1, true, 50, 100);
    }


    public static void main(String[] args) {
//        EmbryoBoundryDetection obj = new EmbryoBoundryDetection("/home/sid/Study/GSOC/GSoc/src/data/Data Annotation/Orignal", "/home/sid/Study/GSOC/GSoc/src/data/Data Annotation/YetAnotherCellMask");
        EmbryoBoundryDetection embryoBoundryDetection = new EmbryoBoundryDetection();
        embryoBoundryDetection.apply();
    }

    private Panel getChooser(String label, int factor) {

        Panel panel = new Panel(new FlowLayout());
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

    private void makeInputForm(GenericDialog genericDialog) {
        genericDialog.addPanel(getChooser("Input Image Folder", 1));
        genericDialog.addPanel(getChooser("Output Folder", 2));
        genericDialog.pack();
        genericDialog.showDialog();
        if (genericDialog.wasCanceled()) {
            IJ.error("PlugIn canceled!");
        }

    }
}
