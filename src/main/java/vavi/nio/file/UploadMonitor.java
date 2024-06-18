/*
 * Copyright (c) 2017 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * UploadMonitor.
 *
 * TODO {@link java.util.concurrent.locks.ReentrantReadWriteLock}???
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2017/03/19 umjammer initial version <br>
 */
public class UploadMonitor<T> {

    /** */
    private Map<Path, T> uploadFlags = new ConcurrentHashMap<>();

    /** */
    public void start(Path path, T entry) {
        uploadFlags.put(path, entry);
    }

    /** */
    public void finish(Path path) {
        uploadFlags.remove(path);
    }

    /** */
    public boolean isUploading(Path path) {
        return uploadFlags.containsKey(path);
    }

    /** */
    public T entry(Path path) {
        return uploadFlags.get(path);
    }
}
