/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file;

import java.net.URI;
import java.nio.file.FileSystem;
import java.util.Collections;
import java.util.UUID;

import com.github.fge.filesystem.driver.JimfsBackedFileSystemProvider;
import org.junit.jupiter.api.Test;


/**
 * runs the downstream test harness {@link Base} against a jimfs backed driver.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-08 nsano initial version <br>
 */
class BaseTest {

    static final JimfsBackedFileSystemProvider provider = new JimfsBackedFileSystemProvider();

    static FileSystem newFileSystem() throws Exception {
        URI uri = URI.create(JimfsBackedFileSystemProvider.SCHEME + ":///" + UUID.randomUUID());
        return provider.newFileSystem(uri, Collections.emptyMap());
    }

    @Test
    void testAll() throws Exception {
        try (FileSystem fs = newFileSystem()) {
            Base.testAll(fs);
        }
    }

    @Test
    void testMoveFolder() throws Exception {
        try (FileSystem fs = newFileSystem()) {
            Base.testMoveFolder(fs);
        }
    }
}
