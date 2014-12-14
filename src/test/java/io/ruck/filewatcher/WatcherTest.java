package io.ruck.filewatcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import javafx.util.Pair;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ruckc
 */
public class WatcherTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(WatcherTest.class);

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testWorkFlow() throws IOException, Exception {
        test(new TestLambda() {
            private final CopyOnWriteArrayList<Pair<Path, WatchEvent.Kind<Path>>> expectations = new CopyOnWriteArrayList<>();

            @Override
            public void run(Path base) throws IOException, InterruptedException {
                createFile(base, "file01.txt");
                createFile(base, "file02.txt");
                createFile(base, "file03.txt");
                createFile(base, "file04.txt");
                createFile(base, "file05.txt");
                createFile(base, "file06.txt");
                createFile(base, "file07.txt");
                createFile(base, "file08.txt");
                createFile(base, "file09.txt");
                createFile(base, "file10.txt");

                waitFor(10);
                expectations.forEach((p) -> {
                    assertTrue(expect(p.getKey(), p.getValue()));
                });
                expectations.clear();
                
                modifyFile(base, "file01.txt");
                modifyFile(base, "file02.txt");
                modifyFile(base, "file03.txt");
                modifyFile(base, "file04.txt");
                modifyFile(base, "file05.txt");
                modifyFile(base, "file06.txt");
                modifyFile(base, "file07.txt");
                modifyFile(base, "file08.txt");
                modifyFile(base, "file09.txt");
                modifyFile(base, "file10.txt");

                waitFor(10);
                expectations.forEach((p) -> {
                    assertTrue(expect(p.getKey(), p.getValue()));
                });
                expectations.clear();

                deleteFile(base, "file01.txt");
                deleteFile(base, "file02.txt");
                deleteFile(base, "file03.txt");
                deleteFile(base, "file04.txt");
                deleteFile(base, "file05.txt");
                deleteFile(base, "file06.txt");
                deleteFile(base, "file07.txt");
                deleteFile(base, "file08.txt");
                deleteFile(base, "file09.txt");
                deleteFile(base, "file10.txt");

                waitFor(10);
                LOGGER.debug("expectations: "+expectations.toString());
                expectations.forEach((p) -> {
                    assertTrue(expect(p.getKey(), p.getValue()));
                });
            }

            public void createFile(Path base, String name) throws IOException {
                Path file = base.resolve(name);
                Files.createFile(file);
                expectations.add(new Pair<>(file, StandardWatchEventKinds.ENTRY_CREATE));
            }
            
            public void modifyFile(Path base, String name) throws IOException {
                Path file = base.resolve(name);
                Files.write(file, new byte[512]);
                expectations.add(new Pair<>(file, StandardWatchEventKinds.ENTRY_MODIFY));
            }

            public void deleteFile(Path base, String name) throws IOException {
                Path file = base.resolve(name);
                Files.delete(file);
                expectations.add(new Pair<>(file, StandardWatchEventKinds.ENTRY_DELETE));
            }
        });
    }

    private void test(TestLambda tl) throws IOException, Exception {
        Path path = folder.newFolder().toPath();
        Files.createDirectories(path);
        Watcher.register(path, tl, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
        tl.run(path);
        Watcher.unregister(path, tl);
    }

    private abstract class TestLambda implements WatchListener {

        private final CopyOnWriteArrayList<Pair<Path, WatchEvent.Kind<Path>>> queue = new CopyOnWriteArrayList<>();

        @Override
        public void handleEvent(Path f, WatchEvent.Kind k) {
            LOGGER.debug("Received event for " + f + " of " + k.name());
            queue.add(new Pair(f, k));
        }

        public boolean expect(Path f, WatchEvent.Kind k) {
            Pair<Path, WatchEvent.Kind<Path>> expect = new Pair<>(f, k);
            boolean result = queue.remove(expect);
            LOGGER.debug("expect->"+result+":"+queue.size()+" for " + f + " of " + k);
            return result;
        }
        
        public void waitFor(int size) throws InterruptedException {
            while(queue.size() < size) {
                Thread.sleep(100);
            }
            LOGGER.debug("events: "+queue.toString());
        }

        public abstract void run(Path base) throws Exception;
    }
}
