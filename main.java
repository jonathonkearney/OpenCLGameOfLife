package OpenCL;


import javax.swing.*;
import java.awt.*;

public class main{

    public static void main( final String... args ) {


        GameofLife g = new GameofLife();
        JFrame frame = new JFrame();
        frame.getContentPane().add(g);
        Container c = frame.getContentPane();
        Dimension d = new Dimension(400,400);
        c.setPreferredSize(d);
        frame.pack();
        frame.setResizable(false);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        g.init();

    }
}
