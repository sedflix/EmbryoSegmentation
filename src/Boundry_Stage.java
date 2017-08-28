import ij.plugin.PlugIn;

public class Boundry_Stage implements PlugIn {
    @Override
    public void run(String s) {
        EmbryoBoundryDetection embryoBoundryDetectio = new EmbryoBoundryDetection();
        embryoBoundryDetectio.apply();
    }
}
