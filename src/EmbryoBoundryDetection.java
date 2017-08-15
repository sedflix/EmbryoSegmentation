import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;

import java.io.File;

public class EmbryoBoundryDetection {
    private String inputDir;
    private String outputDir;

    public EmbryoBoundryDetection(String inputDir, String outputDir) {
        this.inputDir = inputDir;
        this.outputDir = outputDir;
    }

    public static ImagePlus apply(ImagePlus imagePlus) {
        if (imagePlus == null) {
            IJ.error("Unable to read image");
        }
        //TODO: Add detection of not-data-fine case and processing
        return LevelSetUtility.getWholeCellMask(imagePlus, true);
    }

    public static void main(String[] args) {
        EmbryoBoundryDetection obj = new EmbryoBoundryDetection("/home/sid/Study/GSOC/GSoc/src/data/Data Annotation/Orignal", "/home/sid/Study/GSOC/GSoc/src/data/Data Annotation/YetAnotherCellMask");
        obj.apply();
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
}
