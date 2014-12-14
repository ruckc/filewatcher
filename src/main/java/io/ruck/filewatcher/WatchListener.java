package io.ruck.filewatcher;

import java.nio.file.Path;
import java.nio.file.WatchEvent;

/**
 *
 * @author ruckc
 */
public interface WatchListener<T> {
    void handleEvent(Path f, WatchEvent.Kind<Path> k);
}
