/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;


/**
 * EasyFS.
 * <p>
 * This is the interface that unifies each vendors' filesystem
 * without java nio filesystem. you can extract code from your
 * implementation of com.github.fge.filesystem.driver.FileSystemDriver easily.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-11-23 nsano initial version <br>
 */
public interface EasyFS<T> {

    /** */
    boolean isFolder(T entry);

    /** */
    T getRootEntry() throws IOException;

    /** */
    List<T> getDirectoryEntries(T dirEntry) throws IOException;

    /** */
    T renameEntry(T sourceEntry, String name) throws IOException;

    /** */
    void walk(T dirEntry, Consumer<T> task) throws Exception;
}
