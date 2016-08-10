package OpenCL;

import java.awt.*;
import java.applet.*;

public class GameofLife extends Applet {

    private LifeEnv env;
    private Worker worker;

    // Get the applet started
    public void init() {
        build(this);
        new Worker(this).start();
    }

    public void work() {

        env.runIterations();

    }

    // Make a user interface
    private void build(Container f) {
        setLayout(new BorderLayout());
        env = new LifeEnv();
        env.setBackground(Color.white);
        f.add("Center", env);
    }
}

