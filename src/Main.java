import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {

        String originalImage = "/home/sid/Study/GSOC/GSoc/Test/Test1/Orignal";
        String wekaModel = "/home/sid/Study/GSOC/GSoc/src/data/classifiers/MultilayerPerceptron.model";

        File thresholdDir = new File(originalImage + "/../threshold");
        thresholdDir.mkdir();
        File boundryDir = new File(originalImage + "/../boundry");
        boundryDir.mkdir();
        File probDir = new File(originalImage + "/../probMap");
        probDir.mkdir();
        File finalOutput = new File(originalImage + "/../output");
        finalOutput.mkdir();

        WekaSegmentaionStage wekaSegmentaionStage = new WekaSegmentaionStage(originalImage, probDir.getCanonicalPath(), wekaModel);
        wekaSegmentaionStage.apply();

        ThresholdingStage thresholdingStage = new ThresholdingStage(probDir.getAbsolutePath(), thresholdDir.getCanonicalPath(), 0, 0.6);
        thresholdingStage.apply();

        EmbryoBoundryDetection embryoBoundryDetection = new EmbryoBoundryDetection(originalImage, boundryDir.getCanonicalPath());
        embryoBoundryDetection.apply();
    }
}
