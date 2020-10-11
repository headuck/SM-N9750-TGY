package p001fi.iki.elonen.util;

import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import p001fi.iki.elonen.NanoHTTPD;

/* renamed from: fi.iki.elonen.util.ServerRunner */
public class ServerRunner {
    private static final Logger LOG = Logger.getLogger(ServerRunner.class.getName());

    public static void executeInstance(NanoHTTPD server) {
        try {
            server.start(5000, false);
        } catch (IOException ioe) {
            PrintStream printStream = System.err;
            printStream.println("Couldn't start server:\n" + ioe);
            System.exit(-1);
        }
        System.out.println("Server started, Hit Enter to stop.\n");
        try {
            System.in.read();
        } catch (Throwable th) {
        }
        server.stop();
        System.out.println("Server stopped.\n");
    }

    public static <T extends NanoHTTPD> void run(Class<T> serverClass) {
        try {
            executeInstance((NanoHTTPD) serverClass.newInstance());
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Cound nor create server", e);
        }
    }
}
