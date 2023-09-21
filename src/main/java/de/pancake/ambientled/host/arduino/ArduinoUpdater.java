package de.pancake.ambientled.host.arduino;

import de.pancake.ambientled.host.AmbientLed;
import de.pancake.ambientled.host.arduino.ArduinoLed;
import de.pancake.ambientled.host.util.ColorUtil;
import lombok.Getter;

import java.awt.*;
import java.util.Arrays;

import static de.pancake.ambientled.host.AmbientLed.LOGGER;

/**
 * Led updater class
 * @author Pancake
 */
public class ArduinoUpdater implements Runnable {

    /** Led instance */
    private final AmbientLed led;
    /** Arduino led instance */
    private ArduinoLed arduino;
    /** Colors */
    @Getter private final Color[] colors = new Color[180];
    /** Interpolated colors */
    private final Color[] final_colors = new Color[180];

    /**
     * Initialize led updater
     * @param led Led instance
     */
    public ArduinoUpdater(AmbientLed led) {
        this.led = led;

        Arrays.fill(this.colors, Color.BLACK);
        Arrays.fill(this.final_colors, Color.BLACK);
        this.reopen();
    }

    /**
     * Update colors of arduino
     */
    @Override
    public void run() {
        try {
            if (this.led.isPaused() && this.arduino != null)
                this.arduino = this.arduino.close();

            if (this.led.isPaused())
                return;

            // lerp and update colors
            for (int i = 0; i < colors.length; i++)
                this.arduino.write(i, final_colors[i] = ColorUtil.lerp(colors[i], final_colors[i], .5));

            // flush the led
            this.arduino.flush();
        } catch (Exception e) {
            // something went wrong, try to reopen the connection
            LOGGER.severe(e.getMessage());
            this.reopen();
        }

    }

    /**
     * Reopens the connection to the Arduino
     */
    private void reopen() {
        try {
            Thread.sleep(500);
            LOGGER.info("Reopening connection to Arduino");
            this.arduino = new ArduinoLed("Arduino");
        } catch (Exception e) {
            this.reopen(); // try again
        }
    }

}