/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package com.github.fge.filesystem.driver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

import com.github.fge.filesystem.provider.FileSystemFactoryProvider;
import vavi.io.SeekableDataInputStream;
import vavi.util.ByteUtil;

import static java.lang.System.getLogger;


/**
 * DoubleCachedFileSystemDriver.
 * <p>
 * Caching downloaded files also for like a network drive.
 * Double means caching "directory structure" and "file data".
 * </p>
 * system property
 * <li>"disableFileCache" ({@link #ENV_DISABLED_FILE_CACHE}) ... true: don't use files cache</li>
 *
 * @param <T> different type of file system driver's file object
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-09-18 nsano initial version <br>
 */
public abstract class DoubleCachedFileSystemDriver<T> extends CachedFileSystemDriver<T> {

    private static final Logger logger = getLogger(DoubleCachedFileSystemDriver.class.getName());

    /** TODO size limit */
    private Path cacheRoot;

    /** env key for ignoring apple double files */
    public static final String ENV_DISABLED_FILE_CACHE = "disableFileCache";

    /** file cache enabled */
    private boolean isFileCacheDisabled;

    /** creates a cached filesystem */
    protected DoubleCachedFileSystemDriver(FileStore fileStore, FileSystemFactoryProvider factoryProvider) {
        super(fileStore, factoryProvider);
    }

    @Override
    protected void setEnv(Map<String, ?> env) throws IOException {
        super.setEnv(env);
        this.isFileCacheDisabled = isEnabled(ENV_DISABLED_FILE_CACHE);
        if (!isFileCacheDisabled) {
            cacheRoot = Files.createTempDirectory("java7-fs-base");
logger.log(Level.DEBUG, "files cache is created: " + cacheRoot);
            Runtime.getRuntime().addShutdownHook(new Thread(this::dispose));
        } else {
logger.log(Level.DEBUG, "files cache is disabled");
        }
    }

    /** clean up cache */
    private void dispose() {
        try {
logger.log(Level.DEBUG, "cleaning downloaded files cache: " + cacheRoot);
            Files.walk(cacheRoot)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
logger.log(Level.DEBUG, "done cleaning cache: " + cacheRoot);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** */
    private void downloadAsCache(Path localCache, Path source, InputStream in) throws IOException {
        OutputStream os = Files.newOutputStream(localCache, StandardOpenOption.CREATE_NEW);
        byte[] buf = new byte[8192];
        while (true) {
            int r = in.read(buf, 0, buf.length);
            if (r < 0) {
                break;
            }
            os.write(buf, 0, r);
        }
        os.close();
        if (Files.size(localCache) != Files.size(source)) {
logger.log(Level.DEBUG, "CACHE failed, delete: " + localCache.getFileName() + ", local: " + Files.size(localCache) + ", source: " + Files.size(source));
            Files.delete(localCache);
        } else {
logger.log(Level.DEBUG, "CACHE created: " + localCache.getFileName() + ", local: " + Files.size(localCache) + ", source: " + Files.size(source));
        }
    }

    /** */
    private static String getUniqueKey(Path path) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(path.toAbsolutePath().toString().getBytes());
            return ByteUtil.toHexString(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }

    @Override
    protected final InputStream downloadEntry(T entry, Path path, Set<? extends OpenOption> options) throws IOException {
        if (isFileCacheDisabled) {
logger.log(Level.DEBUG, "downloading and caching ignored: " + path);
            return downloadEntryImpl(entry, path, options);
        }

        Path localCache = cacheRoot.resolve(getUniqueKey(path));
        if (!Files.exists(localCache)) {
logger.log(Level.DEBUG, "downloading and caching: " + path + ", " + localCache.getFileName());
            downloadAsCache(localCache, path, downloadEntryImpl(entry, path, options));
        } else {
logger.log(Level.DEBUG, "CACHE hit for: " + path + ", " + localCache.getFileName());
        }
        // see vavi.nio.file.Util.SeekableByteChannelForReading
        return new SeekableDataInputStream(Files.newByteChannel(localCache));
    }

    /**
     * implement driver depends code
     * @see #newInputStream(Path, Set), {@link #downloadEntry(Object, Path, Set)}
     */
    protected abstract InputStream downloadEntryImpl(T sourceEntry, Path path, Set<? extends OpenOption> options) throws IOException;
}
