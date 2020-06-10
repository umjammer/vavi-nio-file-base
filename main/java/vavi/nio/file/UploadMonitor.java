/*
 * Copyright (c) 2017 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file;

import java.util.HashSet;
import java.util.Set;


/**
 * UploadMonitor.
 *
 * TODO embed into java7-fs-base
 * TODO {@link java.util.concurrent.locks.ReentrantReadWriteLock}???
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2017/03/19 umjammer initial version <br>
 */
public class UploadMonitor<T> {

    /** */
    private Set<T> uploadFlags = new HashSet<>();

    /** */
    public void start(T path) {
        uploadFlags.add(path);
    }

    /** */
    public void finish(T path) {
        uploadFlags.remove(path);
    }

    /** */
    public boolean isUploading(T path) {
        return uploadFlags.contains(path);
    }
}

/* */
