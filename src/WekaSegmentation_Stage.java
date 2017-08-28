import ij.plugin.PlugIn;

public class WekaSegmentation_Stage implements PlugIn {
    @Override
    public void run(String s) {
        WekaSegmentaionStage wekaSegmentaionStage = new WekaSegmentaionStage();
        wekaSegmentaionStage.apply();
    }
}
