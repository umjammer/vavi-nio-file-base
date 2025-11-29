/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * CacheTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-07-20 nsano initial version <br>
 */
class CacheTest {

    static class HackedCache extends Cache<String> {

        @Override
        public String getEntry(Path path) throws IOException {
            return null;
        }

        public Map<Path, String> getEntryCache() {
            return entryCache;
        }

        public Map<Path, List<Path>> getFolderCache() {
            return folderCache;
        }
    }

    @Test
    void test1() {
        HackedCache cache = new HackedCache();
        Path dir = Paths.get("/aaa");
        cache.addEntry(dir.resolve("bbb"), "bbb");
        cache.setAllowDuplicatedName(true);
        cache.addEntry(dir.resolve("bbb"), "bbb");
        assertEquals(2, cache.getFolderCache().get(dir).size());
        cache.setAllowDuplicatedName(false);
        cache.addEntry(dir.resolve("bbb"), "bbb");
        assertEquals(2, cache.getFolderCache().get(dir).size());
        cache.setAllowDuplicatedName(true);
        cache.addEntry(dir.resolve("bbb"), "bbb");
        assertEquals(3, cache.getFolderCache().get(dir).size());
    }
}
