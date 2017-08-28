import ij.plugin.PlugIn;

public class LeveSet_Stage implements PlugIn {

    @Override
    public void run(String s) {
        LevelSetStage levelSetStage = new LevelSetStage();
        levelSetStage.apply();
    }
}
