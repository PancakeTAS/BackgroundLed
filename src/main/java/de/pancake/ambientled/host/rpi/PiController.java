package de.pancake.ambientled.host.rpi;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

/**
 * Led controller for Arduino based led strips
 * @author Pancake
 */
public class PiController {

    /** IP of the Raspberry Pi */
    private final String ip;
    /** Port of the Raspberry Pi */
    private final int port;
    /** Socket */
    private Socket socket;
    /** Output stream */
    private OutputStream stream;

    /**
     * Initialize led strip controller
     * @param ip IP of the Raspberry Pi
     * @param port Port of the Raspberry Pi
     * @throws Exception If the connection couldn't be established
     */
    public PiController(String ip, int port) throws Exception {
        this.ip = ip;
        this.port = port;
        this.socket = new Socket(this.ip, this.port);
        this.stream = this.socket.getOutputStream();
    }

    /**
     * Clear the led strip
     * @throws IOException If the data couldn't be written
     */
    public void clear() throws IOException {
        for (int i = 0; i < 144; i++)
            this.stream.write(new byte[] { (byte) 0, (byte) 0, (byte) 0 });

        this.stream.flush();
    }

    /**
     * Write color data to the led strip
     * @param colors Colors with a length of 144
     * @throws IOException If the data couldn't be written
     */
    public void write(Color[] colors) throws IOException {
        for (var c : colors)
            this.stream.write(new byte[]{(byte) (c.getRed() & 0xFF), (byte) (c.getGreen() & 0xFF), (byte) (c.getBlue() & 0xFF)}, 0, 3);

        this.stream.flush();
    }

    /**
     * Close the connection to the raspberry pi
     * @throws IOException If the connection couldn't be closed
     * @return null
     */
    public PiController close() throws IOException {
        this.clear();
        this.stream.close();
        this.socket.close();
        return null;
    }

}