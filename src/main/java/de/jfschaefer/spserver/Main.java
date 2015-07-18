package de.jfschaefer.spserver;

import java.io.IOException;

/**
 * Starts the Server on the port given as an argument
 * Created by jfs on 7/13/15.
 */

public class Main {
    static private int evaluateArguments(String[] args) {
        if (args.length != 1) {
            System.err.println("de.jfschaefer.spserver.Main: Wrong number of arguments.");
            System.err.println("Expected port number as argument");
        }
        try {
            return Integer.parseInt(args[0]);
        } catch (NumberFormatException ex) {
            System.err.println("de.jfschaefer.spserver.Main: Invalid port number \"" + args[0] + "\"");
            System.exit(1);
            return 0xdead;
        }
    }

    public static void main(String[] args) {
        int portnumber = evaluateArguments(args);
        try {
            Server server = new Server(portnumber);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}