package gay.pancake.ambientled;

import com.google.gson.Gson;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchService;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;

import static gay.pancake.ambientled.AmbientLed.LOGGER;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

/**
 * Configuration manager for the led strip
 *
 * @author Pancake
 */
public class ConfigurationManager implements Closeable {

    /**
     * Segment of leds on a strip
     *
     * @param offset The offset of the segment
     * @param length The length of the segment
     * @param display The display to use
     * @param x The x offset of the image
     * @param y The y offset of the image
     * @param width The width of the image
     * @param height The height of the image
     * @param steps The step size for averaging
     * @param orientation The orientation of the segment (true = horizontal, false = vertical)
     * @param invert If the segment is inverted
     */
    public record Segment(int offset, int length, int display, int x, int y, int width, int height, int steps, boolean orientation, boolean invert) {}

    /**
     * Strip of leds
     *
     * @param type The type of the strip
     * @param ip The ip of the pi
     * @param port The port of the pi
     * @param com The com port of the arduino
     * @param leds The number of leds
     * @param segments The segments of the strip
     * @param maxBrightness The max brightness of the strip (0 - 255*3)
     * @param reductionR The reduction of red (0 - 1)
     * @param reductionG The reduction of green (0 - 1)
     * @param reductionB The reduction of blue (0 - 1)
     */
    public record Strip(String type, String ip, int port, String com, int leds, List<Segment> segments, int maxBrightness, float reductionR, float reductionG, float reductionB) {}

    /**
     * Configuration for the led strip
     *
     * @param strips The strips
     * @param ups The updates per second
     * @param fps The frames per second
     * @param lerp The lerp value
     */
    public record Configuration(List<Strip> strips, int ups, int fps, float lerp) {}

    /** The gson instance */
    private static final Gson GSON = new Gson();

    /** The watch service for the configuration directory */
    private final WatchService watchService;
    /** The configuration directory */
    private final Path currentDir = Path.of("");
    /** The watch thread */
    private final Thread watchThread;

    /** The reload consumer */
    private final Function<Configuration, Boolean> reload;


    /**
     * Creates a new configuration manager
     *
     * @param reload The reload consumer
     * @throws IOException If an I/O error occurs
     */
    public ConfigurationManager(Function<Configuration, Boolean> reload) throws IOException {
        this.reload = reload;
        this.watchService = FileSystems.getDefault().newWatchService();
        this.currentDir.register(this.watchService, ENTRY_MODIFY);
        this.watchThread = new Thread(this::watch);
        this.watchThread.setDaemon(true);
        this.watchThread.setName("Configuration Watcher");
        this.watchThread.start();
    }

    /**
     * Watches the configuration directory for changes
     */
    private void watch() {
        try {
            this.reloadConfiguration();

            while (true) {
                var key = this.watchService.take();

                for (var e : key.pollEvents())
                    if (e.context().equals(this.currentDir.resolve("config.json")))
                        this.reloadConfiguration();

                key.reset();
            }
        } catch (InterruptedException ignored) {
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to watch configuration directory!", e);
        }
    }

    /**
     * Reloads the configuration
     */
    private void reloadConfiguration() {
        var config = this.currentDir.resolve("config.json");
        if (!Files.exists(config))
            return;

        try {
            var gson = GSON.fromJson(Files.newBufferedReader(config), Configuration.class);
            while (!this.reload.apply(gson))
                Thread.yield();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to read configuration!", e);
        }
    }

    @Override
    public void close() throws IOException {
        this.watchService.close();
        this.watchThread.interrupt();
    }

}