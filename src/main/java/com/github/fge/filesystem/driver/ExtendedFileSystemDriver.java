/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
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
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.fge.filesystem.exceptions.IsDirectoryException;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;

import vavi.nio.file.Util;
import vavi.util.Debug;


/**
 * ExtendedFileSystemDriver.
 * <p>
 * Wrapping same processing for each different type of file system driver's file object as <code>T</code>
 * You need to implement minimum abstract methods only.
 * </p>
 * @param <T> different type of file system driver's file object
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/06/10 umjammer initial version <br>
 */
@ParametersAreNonnullByDefault
public abstract class ExtendedFileSystemDriver<T> extends ExtendedFileSystemDriverBase {

    /** */
    protected ExtendedFileSystemDriver(final FileStore fileStore, final FileSystemFactoryProvider factoryProvider) {
        super(fileStore, factoryProvider);
    }

    /** utility for entries */
    protected abstract String getFilenameString(T entry);

    /** utility for entries */
    protected abstract boolean isFolder(T entry) throws IOException;

    /** utility for entries */
    protected abstract boolean exists(T entry) throws IOException;

    /**
     * @return not null
     * @throws NoSuchFileException when an entry for the path not found
     */
    @Nonnull
    protected abstract T getEntry(Path path)throws IOException;

    @Override
    public final InputStream newInputStream(final Path path, final Set<? extends OpenOption> options) throws IOException {
        T entry = getEntry(path);

        if (isFolder(entry)) {
            throw new IsDirectoryException("path: " + path);
        }

        return downloadEntry(entry, path, options);
    }

    /**
     * implement driver depends code
     * @param entry source
     * @param path source
     * @see #newInputStream(Path, Set)
     */
    protected abstract InputStream downloadEntry(T entry, Path path, Set<? extends OpenOption> options) throws IOException;

    @Override
    public final OutputStream newOutputStream(final Path path, final Set<? extends OpenOption> options) throws IOException {
        try {
            T entry = getEntry(path);

            if (exists(entry)) {
                if (isFolder(entry)) {
                    throw new IsDirectoryException("path: " + path);
                } else {
                    whenUploadEntryExists(entry, path, options);
                }
            }
Debug.println(Level.FINE, "newOutputStream: cause target not exists");
        } catch (NoSuchFileException e) {
Debug.println(Level.FINE, "newOutputStream: cause target not found, " + e.getMessage());
        }

        T parent = getEntry(path.toAbsolutePath().getParent());
        return uploadEntry(parent, path, options);
    }

    /**
     * Overrides this method if you want to do special action when the target file exists.
     * @throws FileAlreadyExistsException if you don't override this method.
     * @see #newOutputStream(Path, Set), {@link #uploadEntry(Object, Path, Set)}
     */
    protected void whenUploadEntryExists(T destEntry, Path path, Set<? extends OpenOption> options) throws IOException {
        throw new FileAlreadyExistsException("path: " + path);
    }

    /**
     * you must implement `cache.addEntry(path, newEntry)` after async upload is done.
     * @param parentEntry dest parent
     * @param path dest
     * @see #newOutputStream(Path, Set)
     */
    protected abstract OutputStream uploadEntry(T parentEntry, Path path, Set<? extends OpenOption> options) throws IOException;

    @Override
    public final DirectoryStream<Path> newDirectoryStream(Path dir,
                                                    DirectoryStream.Filter<? super Path> filter) throws IOException {
        return Util.newDirectoryStream(getDirectoryEntries(dir, false), filter);
    }

    /**
     * implement driver depends code
     * @see #newDirectoryStream(Path, DirectoryStream.Filter), {@link #getDirectoryEntries(Path, boolean)}}
     */
    protected abstract List<T> getDirectoryEntries(T dirEntry, Path dir) throws IOException;

    /**
     * common process
     * @see #getDirectoryEntries(Path, boolean)
     */
    protected List<Path> getDirectoryEntries(Path dir, boolean dummy) throws IOException {
        T dirEntry = getEntry(dir);

        if (!isFolder(dirEntry)) {
            throw new NotDirectoryException("dir: " + dir);
        }

        return getDirectoryEntries(dirEntry, dir).stream()
                .map(child -> dir.resolve(getFilenameString(child)))
                .collect(Collectors.toList());
    }

    @Override
    public final void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        try {
            T dirEntry = getEntry(dir);
            if (exists(dirEntry)) {
                throw new FileAlreadyExistsException("dir: "+ dir);
            }
Debug.println(Level.FINE, "createDirectory: target cause not exists");
        } catch (NoSuchFileException e) {
Debug.println(Level.FINE, "createDirectory: target cause not found, " + e.getMessage());
        }

        createDirectoryEntry(dir);
    }

    /**
     * implement driver depends code
     * @see #createDirectory(Path, FileAttribute[]), {@link #createDirectoryEntry(Path)}
     */
    protected abstract T createDirectoryEntry(T parentEntry, Path dir) throws IOException;

    /**
     * common process
     * @see #createDirectoryEntry(Object, Path)
     */
    protected void createDirectoryEntry(Path dir) throws IOException {
        T parentEntry = getEntry(dir.toAbsolutePath().getParent());
        createDirectoryEntry(parentEntry, dir);
    }

    @Override
    public final void delete(Path path) throws IOException {
        T entry = getEntry(path);

        if (isFolder(entry)) {
            if (hasChildren(entry, path)) {
                throw new DirectoryNotEmptyException("dir : " + path);
            }
        }

        removeEntry(path);
    }

    /**
     * should implement light-weight-ly.
     * @see #delete(Path)
     */
    protected abstract boolean hasChildren(T dirEntry, Path dir) throws IOException;

    /**
     * implement driver depends code
     * @see #delete(Path), {@link #removeEntry(Path)}
     */
    protected abstract void removeEntry(T entry, Path path) throws IOException;

    /**
     * common process
     * @see #removeEntry(Object, Path)
     */
    protected void removeEntry(Path path) throws IOException {
        T entry = getEntry(path);
        removeEntry(entry, path);
    }

    @Override
    public final void copy(Path source, Path target, Set<CopyOption> options) throws IOException {
        try {
            T targetEntry = getEntry(target);
            if (exists(targetEntry)) {
                if (options != null && options.stream().anyMatch(o -> o.equals(StandardCopyOption.REPLACE_EXISTING))) {
                    removeEntry(target);
                } else {
                    throw new FileAlreadyExistsException("path: " + target);
                }
            }
Debug.println(Level.FINE, "copy: cause target not exists");
        } catch (NoSuchFileException e) {
Debug.println(Level.FINE, "copy: cause target not found, " + e.getMessage());
        }

        copyEntry(source, target, options);
    }

    /**
     * implement driver depends code
     * @return null means that copy is async, after process like cache by your self.
     * @see #copyEntry(Object, Object, Path, Path, Set)
     */
    protected abstract T copyEntry(T sourceEntry, T targetParentEntry, Path source, Path target, Set<CopyOption> options) throws IOException;

    /**
     * common process
     * @see #copyEntry(Object, Object, Path, Path, Set)
     */
    protected void copyEntry(Path source, Path target, Set<CopyOption> options) throws IOException {
        T sourceEntry = getEntry(source);
        T targetParentEntry = getEntry(target.toAbsolutePath().getParent());
        if (!isFolder(sourceEntry)) {
            copyEntry(sourceEntry, targetParentEntry, source, target, options);
        } else {
            // TODO java spec. allows empty folder
            throw new UnsupportedOperationException("source can not be a folder: " + source);
        }
    }

    @Override
    public final void move(Path source, Path target, final Set<CopyOption> options) throws IOException {
        try {
            T targetEntry = getEntry(target);
            if (exists(targetEntry)) {
                if (isFolder(targetEntry)) {
                    if (options != null && options.stream().anyMatch(o -> o.equals(StandardCopyOption.REPLACE_EXISTING))) {
                        // replace the target
                        if (hasChildren(targetEntry, target)) {
                            throw new DirectoryNotEmptyException("dir: " + target);
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
                        throw new FileAlreadyExistsException("path: " + target);
                    }
                }
                return;
            }
            Debug.println(Level.FINE, "move: cause target not exists");
        } catch (NoSuchFileException e) {
Debug.println(Level.FINE, "move: cause target not found, " + e.getMessage());
        }

        if (source.toAbsolutePath().getParent().equals(target.toAbsolutePath().getParent())) {
            // rename
            renameEntry(source, target);
        } else {
            moveEntry(source, target, false);
        }
    }

    /**
     * implement driver depends code
     * @see #move(Path, Path, Set)
     */
    protected abstract T moveEntry(T sourceEntry, T targetParentEntry, Path source, Path target, boolean targetIsParent) throws IOException;
    /**
     * implement driver depends code
     * @see #move(Path, Path, Set)
     */
    protected abstract T moveFolderEntry(T sourceEntry, T targetParentEntry, Path source, Path target, boolean targetIsParent) throws IOException;

    /**
     * common process
     * @param targetIsParent if the target is folder
     * @see #moveEntry(Object, Object, Path, Path, boolean), {@link #moveFolderEntry(Object, Object, Path, Path, boolean)}
     */
    protected void moveEntry(Path source, Path target, boolean targetIsParent) throws IOException {
        T sourceEntry = getEntry(source);
        T targetParentEntry = getEntry(targetIsParent ? target : target.toAbsolutePath().getParent());
        if (!isFolder(sourceEntry)) {
            moveEntry(sourceEntry, targetParentEntry, source, targetIsParent ? source : target, targetIsParent);
        } else {
            moveFolderEntry(sourceEntry, targetParentEntry, source, target, targetIsParent);
        }
    }

    /**
     * implement driver depends code
     * @see #renameEntry(Path, Path)
     */
    protected abstract T renameEntry(T sourceEntry, T targetParentEntry, Path source, Path target) throws IOException;

    /**
     * common process
     * @see #renameEntry(Object, Object, Path, Path)
     */
    protected void renameEntry(Path source, Path target) throws IOException {
        T sourceEntry = getEntry(source);
        T targetParentEntry = getEntry(target.getParent());
        renameEntry(sourceEntry, targetParentEntry, source, target);
    }

    /**
     * to ignore check, override me
     * @see #checkAccessEntry(Object, Path, AccessMode...)
     */
    @Override
    protected final void checkAccessImpl(Path path, AccessMode... modes) throws IOException {
        T entry = getEntry(path);

        if (isFolder(entry)) {
            return;
        }

        checkAccessEntry(entry, path, modes);
    }

    /**
     * to check original access mode, override me
     * @see #checkAccessImpl(Path, AccessMode...)
     */
    protected void checkAccessEntry(T entry, Path path, AccessMode... modes) throws IOException {
        // TODO: assumed; not a file == directory
        for (final AccessMode mode : modes) {
            if (mode == AccessMode.EXECUTE) {
                throw new AccessDeniedException(path.toString());
            }
        }
    }

    /** @see #getPathMetadata(Path) */
    @Override
    protected final Object getPathMetadataImpl(Path path) throws IOException {
        return getPathMetadata(getEntry(path));
    }

    /**
     * if you pass your own object, override me
     * @see #getPathMetadata(Path)
     */
    protected Object getPathMetadata(T entry) throws IOException {
        return entry; 
    }

    /** do nothing, cause most fs need not close, if it's needed override this */
    @Override
    public void close() throws IOException {
    }
}
