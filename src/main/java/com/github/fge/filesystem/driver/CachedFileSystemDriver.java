/*
 * Copyright (c) 2021 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package com.github.fge.filesystem.driver;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.FileStore;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.github.fge.filesystem.provider.FileSystemFactoryProvider;

import vavi.nio.file.Cache;
import vavi.nio.file.Util;


/**
 * CachedFileSystemDriver.
 * <p>
 * Retrieved filenames and directories are cached.
 * </p>
 * @param <T> different type of file system driver's file object
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2021/10/30 umjammer initial version <br>
 */
public abstract class CachedFileSystemDriver<T> extends ExtendedFileSystemDriver<T> {

    /** */
    protected CachedFileSystemDriver(FileStore fileStore, FileSystemFactoryProvider factoryProvider) {
        super(fileStore, factoryProvider);
    }

    /** for async method in subclass */
    protected void updateEntry(Path path, T newEntry) {
        cache.addEntry(path, newEntry);
    }

    @Override
    protected boolean exists(T entry) throws IOException {
        return entry != null;
    }

    /**
     * for some file systems, root folder has different structure against ordinary folders.
     * you can return new instance, because it will be cached.
     */
    protected abstract T getRootEntry(Path root) throws IOException;

    @Override
    protected T getEntry(Path path) throws IOException {
        return cache.getEntry(path);
    }

    /**
     * if your api has more effective api. override this method.
     * this method traverses all siblings from root, it costs heavy.
     * @param parentEntry not used in this method, nullable
     * @return null when not found
     * @see "cache#cacheEntry(Path)"
     */
    protected T getEntry(T parentEntry, Path path) throws IOException {
//logger.log(Level.DEBUG, "search: " + path);
        for (int i = 0; i < path.getNameCount(); i++) {
            Path name = path.getName(i);
            Path sub = path.subpath(0, i + 1);
            Path parent = sub.getParent() != null ? sub.getParent() : path.getFileSystem().getRootDirectories().iterator().next();
            List<Path> bros = getDirectoryEntries(parent, false);
            Optional<Path> found = bros.stream().filter(p -> p.getFileName().equals(name)).findFirst();
//System.err.println("name: " + name + ", sub: " + sub + ", parent: " + parent + ", found: " + found.isPresent() + ", list: " + bros);
            if (found.isPresent()) {
                continue;
            } else {
//logger.log(Level.DEBUG, "not found: " + path);
                return null;
            }
        }
//logger.log(Level.DEBUG, "found: " + path + ", " + cache.getFile(path));
        return cache.getFile(path);
    }

    /** cache for filenames */
    protected Cache<T> cache = new Cache<>() {
        /**
         * @throws NoSuchFileException must be thrown when the path is not found in this cache
         * @see ExtendedFileSystemDriverBase#ignoreAppleDouble
         */
        @Override
        public T getEntry(Path path) throws IOException {
            if (containsFile(path)) {
                return getFile(path);
            } else {
                if (ignoreAppleDouble && path.getFileName() != null && Util.isAppleDouble(path)) {
                    throw new NoSuchFileException("ignore apple double file: " + path);
                }

                if (path.getNameCount() == 0) { // means root
                    T entry = getRootEntry(path);
                    putFile(path, entry);
                    return entry;
                } else {
                    return cacheEntry(path);
                }
            }
        }

        /**
         * @throws NoSuchFileException see {@link #getEntry(Object)}
         */
        private T cacheEntry(Path path) throws IOException {
            T parentEntry = getEntry(path.toAbsolutePath().getParent());
            T entry = CachedFileSystemDriver.this.getEntry(parentEntry, path);
            if (entry != null) {
                addEntry(path, entry);
            } else {
                removeEntry(path);
                throw new NoSuchFileException(path.toString());
            }
            return entry;
        }
    };

    /**
     * path -> cache
     */
    @Override
    protected List<Path> getDirectoryEntries(Path dir, boolean useCache) throws IOException {
        T entry = getEntry(dir);

        if (!isFolder(entry)) {
            throw new NotDirectoryException("dir: " + dir);
        }

        List<Path> list = new ArrayList<>();
        if (useCache && cache.containsFolder(dir)) {
//logger.log(Level.DEBUG, "cache list: " + cache.getFolder(dir));
            list = cache.getFolder(dir);
        } else {
            List<T> children = getDirectoryEntries(entry, dir);
            for (T child : children) {
                Path childPath = dir.resolve(getFilenameString(child));
                list.add(childPath);
                cache.putFile(childPath, child);
            }

            cache.putFolder(dir, list);
        }

        return list;
    }

    @Override
    protected void createDirectoryEntry(Path dir) throws IOException {
        T parentEntry = getEntry(dir.toAbsolutePath().getParent());
        T newEntry = createDirectoryEntry(parentEntry, dir);
        cache.addEntry(dir, newEntry);
    }

    @Override
    protected void removeEntry(Path path) throws IOException {
        T entry = getEntry(path);
        removeEntry(entry, path);
        cache.removeEntry(path);
    }

    @Override
    protected void copyEntry(Path source, Path target, Set<CopyOption> options) throws IOException {
        T sourceEntry = getEntry(source);
        T targetParentEntry = getEntry(target.toAbsolutePath().getParent());
        if (!isFolder(sourceEntry)) {
            T newEntry = copyEntry(sourceEntry, targetParentEntry, source, target, options);

            if (newEntry != null) {
                cache.addEntry(target, newEntry);
            }
        } else {
            // TODO java spec. allows empty folder
            throw new UnsupportedOperationException("source can not be a folder");
        }
    }

    @Override
    protected void moveEntry(Path source, Path target, boolean targetIsParent) throws IOException {
        T sourceEntry = cache.getEntry(source);
        T targetParentEntry = cache.getEntry(targetIsParent ? target : target.toAbsolutePath().getParent());
        if (!isFolder(sourceEntry)) {
            T newEntry = moveEntry(sourceEntry, targetParentEntry, source, targetIsParent ? source : target, targetIsParent);
            cache.removeEntry(source);
            if (targetIsParent) {
                cache.addEntry(target.resolve(source.getFileName()), newEntry);
            } else {
                cache.addEntry(target, newEntry);
            }
        } else if (isFolder(sourceEntry)) {
            T newEntry = moveFolderEntry(sourceEntry, targetParentEntry, source, target, targetIsParent);
            cache.moveEntry(source, target, newEntry);
        }
    }

    @Override
    protected void renameEntry(Path source, Path target) throws IOException {
        T sourceEntry = cache.getEntry(source);
        T targetParentEntry = cache.getEntry(target.getParent());
        T newEntry = renameEntry(sourceEntry, targetParentEntry, source, target);
        cache.removeEntry(source);
        cache.addEntry(target, newEntry);
    }
}
