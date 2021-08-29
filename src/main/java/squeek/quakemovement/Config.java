package squeek.quakemovement;

@SimpleConfig.Info(namespace = "squake")
public class Config {

    @SimpleConfig.Category(name = "general")
    static public boolean ENABLED = true;

    @SimpleConfig.Category(name = "trimp")
    static public boolean TRIMPING_ENABLED = true;
    static public double TRIMP_MULTIPLIER = 1.4D;

    @SimpleConfig.Category(name = "caps")
    static public boolean ENFORCE_CAP = false;
    static public double HARDCAP = 2.0D;
    static public double SOFTCAP = 1.4D;
    static public double SOFTCAP_DECAY = 0.65D;

    @SimpleConfig.Category(name = "acceleration")
    static public double ACCELERATE = 10.0D;
    static public double AIR_ACCELERATE = 14.0D;
    static public double MAX_AIR_ACCEL_PER_TICK = 0.045D;
}