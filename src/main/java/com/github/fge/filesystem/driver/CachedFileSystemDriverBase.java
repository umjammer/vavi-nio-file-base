/*
 * Copyright (c) 2021 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package com.github.fge.filesystem.driver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.github.fge.filesystem.exceptions.IsDirectoryException;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;

import vavi.nio.file.Cache;
import vavi.nio.file.Util;
import vavi.util.Debug;


/**
 * CachedFileSystemDriver.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2021/10/30 umjammer initial version <br>
 */
public abstract class CachedFileSystemDriverBase<T> extends ExtendedFileSystemDriverBase {

    protected boolean ignoreAppleDouble = false;

    /** */
    protected CachedFileSystemDriverBase(final FileStore fileStore, final FileSystemFactoryProvider factoryProvider) {
        super(fileStore, factoryProvider);
    }

    /** utility for entries */
    protected abstract String getFilenameString(T entry) throws IOException;

    /** utility for entries  */
    protected abstract boolean isFolder(T entry);

    /**
     * for some file systems, root folder has different structure against ordinary folders.
     * you can return new instance, because it will be cached.
     */
    protected abstract T getRootEntry() throws IOException;

    /**
     * @return null when not found 
     */
    protected abstract T getEntry(T dirEntry, Path path)throws IOException;

    /** */
    protected Cache<T> cache = new Cache<T>() {
        /**
         * @see #ignoreAppleDouble
         * @throws NoSuchFileException must be thrown when the path is not found in this cache
         */
        public T getEntry(Path path) throws IOException {
            if (containsFile(path)) {
                return getFile(path);
            } else {
                if (ignoreAppleDouble && path.getFileName() != null && Util.isAppleDouble(path)) {
                    throw new NoSuchFileException("ignore apple double file: " + path);
                }

                if (path.getNameCount() == 0) { // means root
                	T entry = getRootEntry();
                	putFile(path, entry);
                    return entry;
                } else {
                	return cacheEntry(path);
                }
            }
        }

        /**
         * @throws NoSuchFileException 
         */
        private T cacheEntry(Path path) throws IOException {
        	T dir = getEntry(path.toAbsolutePath().getParent());
            T entry = CachedFileSystemDriverBase.this.getEntry(dir, path);
            if (entry != null) {
                addEntry(path, entry);
            } else {
	            // clean cache
	            if (containsFile(path)) {
	                removeEntry(path);
	            }
	            // re-cache
                getDirectoryEntries(path.toAbsolutePath().getParent(), false);

                // re-try
                entry = getFile(path);
                if (entry == null) {
                	throw new NoSuchFileException(path.toString());
                }
            }
            return entry;
        }
    };

    /** */
    protected abstract InputStream downloadEntry(T entry, Path path, Set<? extends OpenOption> options) throws IOException;

    @Override
    public InputStream newInputStream(final Path path, final Set<? extends OpenOption> options) throws IOException {
        final T entry = cache.getEntry(path);

        if (isFolder(entry)) {
            throw new IsDirectoryException("path: " + path);
        }

        return downloadEntry(entry, path, options);
    }

    @Override
    public OutputStream newOutputStream(final Path path, final Set<? extends OpenOption> options) throws IOException {
        final T entry;
        try {
            entry = cache.getEntry(path);

            if (isFolder(entry)) {
                throw new IsDirectoryException("path: " + path);
            } else {
                throw new FileAlreadyExistsException("path: " + path);
            }
        } catch (NoSuchFileException e) {
Debug.println("newOutputStream: " + e.getMessage());
        }

        T parent = cache.getEntry(path.toAbsolutePath().getParent());
        return uploadEntry(parent, path, options);
    }

    /**
     * you must implement `cache.addEntry(path, newEntry)` after async upload is done.
     */
    protected abstract OutputStream uploadEntry(T parentEntry, Path path, Set<? extends OpenOption> options) throws IOException;

    @Override
    public DirectoryStream<Path> newDirectoryStream(final Path dir,
                                                    final DirectoryStream.Filter<? super Path> filter) throws IOException {
        return Util.newDirectoryStream(getDirectoryEntries(dir, true), filter);
    }

    /** */
    protected abstract T createDirectoryEntry(Path dir) throws IOException;

    @Override
    public void createDirectory(final Path dir, final FileAttribute<?>... attrs) throws IOException {
        T newEntry = createDirectoryEntry(dir);
        cache.addEntry(dir, newEntry);
    }

    @Override
    public void delete(final Path path) throws IOException {
        removeEntry(path);
    }

    @Override
    public void copy(final Path source, final Path target, final Set<CopyOption> options) throws IOException {
        if (cache.existsEntry(target)) {
            if (options != null && options.stream().anyMatch(o -> o.equals(StandardCopyOption.REPLACE_EXISTING))) {
                removeEntry(target);
            } else {
                throw new FileAlreadyExistsException(target.toString());
            }
        }

        copyEntry(source, target, options);
    }

    @Override
    public void move(final Path source, final Path target, final Set<CopyOption> options) throws IOException {
        if (cache.existsEntry(target)) {
            if (isFolder(cache.getEntry(target))) {
                if (options != null && options.stream().anyMatch(o -> o.equals(StandardCopyOption.REPLACE_EXISTING))) {
                    // replace the target
                    if (cache.getChildCount(target) > 0) {
                        throw new DirectoryNotEmptyException(target.toString());
                    } else {
                        removeEntry(target);
                        moveEntry(source, target, false);
                    }
                } else {
                    // move into the target
                    // TODO SPEC is FileAlreadyExistsException ?
                    moveEntry(source, target, true);
                }
            } else {
                if (options != null && options.stream().anyMatch(o -> o.equals(StandardCopyOption.REPLACE_EXISTING))) {
                    removeEntry(target);
                    moveEntry(source, target, false);
                } else {
                    throw new FileAlreadyExistsException(target.toString());
                }
            }
        } else {
            if (source.toAbsolutePath().getParent().equals(target.toAbsolutePath().getParent())) {
                // rename
                renameEntry(source, target);
            } else {
                moveEntry(source, target, false);
            }
        }
    }

    /**
     * Check access modes for a path on this filesystem
     * <p>
     * If no modes are provided to check for, this simply checks for the
     * existence of the path.
     * </p>
     *
     * @param path the path to check
     * @param modes the modes to check for, if any
     * @throws IOException filesystem level error, or a plain I/O error
     *                     if you use this with fuse, you should throw {@link NoSuchFileException} when the file not found.
     * @see FileSystemProvider#checkAccess(Path, AccessMode...)
     */
    @Override
    protected void checkAccessImpl(final Path path, final AccessMode... modes) throws IOException {
        final T entry = cache.getEntry(path);

        if (isFolder(entry)) {
            return;
        }

        // TODO: assumed; not a file == directory
        for (final AccessMode mode : modes) {
            if (mode == AccessMode.EXECUTE) {
                throw new AccessDeniedException(path.toString());
            }
        }
    }

    /** do nothing as default */
    @Override
    public void close() throws IOException {
    }

    /** do nothing as default */
    protected Object getMetadata(T entry) throws IOException {
        return entry;
    }

    /**
     * @throws IOException if you use this with fuse, you should throw {@link NoSuchFileException} when the file not found.
     */
    @Override
    protected Object getPathMetadataImpl(final Path path) throws IOException {
        return getMetadata(cache.getEntry(path));
    }

    /** */
    protected abstract List<T> getDirectoryEntries(T dirEntry, Path dir) throws IOException;

    /** */
    protected List<Path> getDirectoryEntries(Path dir, boolean useCache) throws IOException {
        T entry = cache.getEntry(dir);

        if (!isFolder(entry)) {
            throw new NotDirectoryException("dir: " + dir);
        }

        List<Path> list = new ArrayList<>();
        if (useCache && cache.containsFolder(dir)) {
            list = cache.getFolder(dir);
        } else {
        	List<T> children = getDirectoryEntries(entry, dir);
            for (final T child : children) {
                Path childPath = dir.resolve(getFilenameString(child));
                list.add(childPath);

                cache.putFile(childPath, child);
            }

            cache.putFolder(dir, list);
        }

        return list;
    }

    /**
     * should implement light-weight. 
     */
    protected abstract boolean hasChildren(T dirEntry, Path dir) throws IOException;

    protected abstract void removeEntry(T entry, Path path) throws IOException;
    
    /** */
    private void removeEntry(Path path) throws IOException {
        final T entry = cache.getEntry(path);

        if (isFolder(entry)) {
            if (hasChildren(entry, path)) {
                throw new DirectoryNotEmptyException(path.toString());
            }
        }

        removeEntry(entry, path);

        cache.removeEntry(path);
    }

    /**
     * @return null means that copy is async, cache by your self.
     */
    protected abstract T copyEntry(T sourceEntry, T targetParentEntry, Path source, Path target, Set<CopyOption> options) throws IOException;

    /** */
    private void copyEntry(final Path source, final Path target, Set<CopyOption> options) throws IOException {
        final T sourceEntry = cache.getEntry(source);
        T targetParentEntry = cache.getEntry(target.toAbsolutePath().getParent());
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

    protected abstract T moveEntry(T sourceEntry, T targetParentEntry, Path source, Path target, boolean targetIsParent) throws IOException;
    protected abstract T moveFolderEntry(T sourceEntry, T targetParentEntry, Path source, Path target, boolean targetIsParent) throws IOException;

    /**
     * @param targetIsParent if the target is folder
     */
    private void moveEntry(final Path source, final Path target, boolean targetIsParent) throws IOException {
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

    protected abstract T renameEntry(T sourceEntry, T targetParentEntry, Path source, Path target) throws IOException;

    /** */
    private void renameEntry(final Path source, final Path target) throws IOException {
        T sourceEntry = cache.getEntry(source);
        T targetParentEntry = cache.getEntry(target.getParent());
        T newEntry = renameEntry(sourceEntry, targetParentEntry, source, target);
        cache.removeEntry(source);
        cache.addEntry(target, newEntry);
    }
}
