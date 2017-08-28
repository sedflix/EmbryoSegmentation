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

/**
 * WekaSegmentaionStage is kind of the most crucial in this PlugIn.
 * The model you will are using must satisfy the following properties:
 * <ol>
 <li> Should be a binary classifier</li>
 <li> It should properly identify a major interior region of each cell</li>
 <li> Train the model to minimize under-segmentation.</li>
 <li> Over segmentation can be rectified in the further stages of the algorithm</li>
 * </ol>
 */
public class WekaSegmentaionStage {

    private final static String title = "Detecting interiors of cell using Weka Segmentation";
    private String inputImageFolder;
    private String outpurImageFolder;
    private String classifierModelFileAddress;
    private WekaSegmentation wekaSegmentaion = new WekaSegmentation();

    /**
     * @param inputImageFolder
     * @param outpurImageFolder
     * @param classifierModelFileAddress
     */
    public WekaSegmentaionStage(String inputImageFolder, String outpurImageFolder, String classifierModelFileAddress) {
        this.inputImageFolder = inputImageFolder;
        this.outpurImageFolder = outpurImageFolder;
        this.classifierModelFileAddress = classifierModelFileAddress;
        prepare();
    }

    /**
     *
     * @param frame
     */
    public WekaSegmentaionStage(Frame frame) {
        GenericDialog gd = new GenericDialog(title, frame);
        makeInputForm(gd);
        prepare();

    }

    /**
     *
     */
    public WekaSegmentaionStage() {
        GenericDialog gd = new GenericDialog(title);
        makeInputForm(gd);
        prepare();

    }

    /**
     * TRY NOT TO USE THIS
     * This is a really heavy code.
     * @param imagePlus inout image
     * @param wekaClassifier address of the classifier or model used
     * @return classified image
     */
    public static ImagePlus applyClassifier(ImagePlus imagePlus, String wekaClassifier) {

        //Weka Trainable Segmentation tool created
        WekaSegmentation wekaSegmentation = new WekaSegmentation();

        //Loading classifier using the address in "wekaClassifier"
        // ooooh this is going to take a lot of time
        wekaSegmentation.loadClassifier(wekaClassifier); //slooooww

        if (imagePlus == null) {
            //if not able to read imagePlus
            IJ.error("empty image");
            return null;
        }

        // apply classifier and get results (0 indicates number of threads is auto-detected) and true for probability map
        ImagePlus result = wekaSegmentation.applyClassifier(imagePlus, 0, true); //freaking slow

        //for clearing memory
        System.gc(); //slow

        return result;
    }


    public void apply() {
        File inputImagesFolder = new File(inputImageFolder);
        for (File inputImage : inputImagesFolder.listFiles()) {
            if (inputImage.isFile()) {
                applyClassifier(inputImage);
            }
        }
    }

    public static void main(String[] args) {
        WekaSegmentaionStage obj = new WekaSegmentaionStage();
        obj.apply();
    }

    public static ImagePlus applyClassifier(String imagepath, String wekaClassifier) {
        return applyClassifier(new ImagePlus(imagepath), wekaClassifier);
    }

    /**
     * Call this constructor anywhere after classifierModelFileAddress is changes
     */
    private void prepare() {

        //loads the classifier
        wekaSegmentaion.loadClassifier(this.classifierModelFileAddress);

        File inputImagesFolder = new File(inputImageFolder);
        File outputImageFolder = new File(outpurImageFolder);

        //checks if input directory are okay
        if (inputImagesFolder == null && !(inputImagesFolder.isDirectory() && inputImagesFolder.exists())) {
            IJ.error("Invalid input directory");
            return;
        }
        //checks if output directory are okay
        if (outputImageFolder == null && !(outputImageFolder.isDirectory() && outputImageFolder.exists())) {
            IJ.error("Invalid output directory");
            return;
        }

    }

    public String getOutpurImageFolder() {
        return outpurImageFolder;
    }

    public String getInputImageFolder() {
        return inputImageFolder;
    }

    /**
     * Applies the classifier on the given image, represented by imageFile File object
     *
     * @param imageFile File object that points to the input image
     */
    private void applyClassifier(File imageFile) {
        ImagePlus imagePlus = new ImagePlus(imageFile.getAbsolutePath());
        if (imagePlus != null) {
            // apply classifier and get results (0 indicates number of threads is auto-detected) and true for probability map
            ImagePlus result = wekaSegmentaion.applyClassifier(imagePlus, 0, true);

            //make outputFileName
            String outputFileName = imageFile.getName();
            new FileSaver(result).saveAsJpeg(outpurImageFolder + File.separator + outputFileName);

            // force garbage collection (important for large images)
            result = null;
            imagePlus = null;
            System.gc();
        } else {
            IJ.error("Not able to read image");
        }
    }

    /*
    Helps to make GUI form
     */
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

}
