package io.ruck.filewatcher;

import com.sun.nio.file.SensitivityWatchEventModifier;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ruckc
 */
public class Watcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(Watcher.class);
    private static final Map<WatchKey, List<PathListener>> MAPPINGS = new ConcurrentHashMap<>();
    private static final WatchService WATCH_SERVICE;
    private static final Thread EXECUTOR;

    static {
        try {
            WATCH_SERVICE = FileSystems.getDefault().newWatchService();
            EXECUTOR = new Thread(new WatcherRunnable(), "File Watcher Thread");
            EXECUTOR.setDaemon(true);
        } catch (IOException ex) {
            throw new RuntimeException();
        }
    }

    private static class WatcherRunnable implements Runnable {

        @Override
        @SuppressWarnings("unchecked")
        public void run() {
            LOGGER.debug("Starting File Watcher Thread");
            while (true) {
                try {
                    LOGGER.debug("waiting for WatchKey");
                    WatchKey key = WATCH_SERVICE.take();
                    LOGGER.debug("received WatchKey "+key);
                    key.pollEvents().stream().forEach((WatchEvent<?> e) -> {
                        LOGGER.debug("received WatchEvent "+e);
                        WatchEvent<Path> ep = (WatchEvent<Path>) e;
                        List<PathListener> list = MAPPINGS.get(key);
                        list.stream().forEach((PathListener l) -> {
                            LOGGER.debug("delivering "+e+" to "+l);
                            l.handle(ep);
                        });
                    });
                    key.reset();
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex.getMessage(), ex);
                }
            }
        }
    }

    private static class PathListener {

        private final Path base;
        private final WatchListener<Path> listener;

        private PathListener(Path base, WatchListener<Path> listener) {
            this.base = base;
            this.listener = listener;
        }

        private void handle(WatchEvent<Path> e) {
            Path path = (Path) e.context();
            Kind<Path> kind = (Kind<Path>) e.kind();
            listener.handleEvent(base.resolve(path), kind);
        }
    }

    public static void register(Path base, WatchListener<Path> listener, Kind<Path>... events) {
        try {
            if(!EXECUTOR.isAlive()) {
                EXECUTOR.start();
            }
            WatchKey key = base.register(WATCH_SERVICE, events, SensitivityWatchEventModifier.HIGH);
            PathListener pl = new PathListener(base, listener);
            MAPPINGS.compute(key, (WatchKey t, List<PathListener> u) -> {
                if(u != null) {
                    u.add(pl);
                    return u;
                } else {
                    List<PathListener> list = new ArrayList<>(1);
                    list.add(pl);
                    return list;
                }
            });
            LOGGER.trace("Mappings: "+MAPPINGS);
            LOGGER.debug("registered "+key+" for "+base+" with "+listener);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static void unregister(Path base, WatchListener<Path> listener) {
        Iterator<Map.Entry<WatchKey, List<PathListener>>> iterator = MAPPINGS.entrySet().iterator();
        while(iterator.hasNext()) {
            Map.Entry<WatchKey, List<PathListener>> t = iterator.next();
            t.getValue().removeIf((pl) -> {
                return pl.base.equals(base) && pl.listener.equals(listener);
            });
            if(t.getValue().isEmpty()) {
                t.getKey().cancel();
                iterator.remove();
            }
        }
    }
}
