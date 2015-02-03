package au.com.forloop.jq.esb.emiratesssim;

import org.apache.camel.main.Main;


public class RouteLauncher {

    /**
     * A main() so you can run these routing rules in our IDE
     */
    public static void main(String... args) throws Exception {
        Main main = new Main();
        main.enableHangupSupport();
        main.enableTrace();
        main.addRouteBuilder(new MulticastRouteBuilder());
        main.run(args);
    }

}
