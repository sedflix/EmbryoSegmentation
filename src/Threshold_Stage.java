import ij.plugin.PlugIn;

public class Threshold_Stage implements PlugIn {
    @Override
    public void run(String s) {
        ThresholdingStage thresholdingStage = new ThresholdingStage();
        thresholdingStage.apply();
    }
}
