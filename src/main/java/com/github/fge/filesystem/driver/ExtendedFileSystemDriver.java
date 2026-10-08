/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package com.github.fge.filesystem.driver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.FileSystemException;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.fge.filesystem.exceptions.IsDirectoryException;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;
import vavi.nio.file.Util;

import static java.lang.System.getLogger;


/**
 * ExtendedFileSystemDriver.
 * <p>
 * Wrapping same processing for each different type of file system driver's file object as <code>T</code>
 * You need to implement minimum abstract methods only.
 * </p>
 *
 * @param <T> different type of file system driver's file object
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/06/10 umjammer initial version <br>
 */
@ParametersAreNonnullByDefault
public abstract class ExtendedFileSystemDriver<T> extends ExtendedFileSystemDriverBase {

    private static final Logger logger = getLogger(ExtendedFileSystemDriver.class.getName());

    /** */
    protected ExtendedFileSystemDriver(FileStore fileStore, FileSystemFactoryProvider factoryProvider) {
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
    protected abstract T getEntry(Path path) throws IOException;

    @Override
    public final InputStream newInputStream(Path path, Set<? extends OpenOption> options) throws IOException {
        T entry = getEntry(path);

        if (isFolder(entry)) {
            throw new IsDirectoryException("path: " + path);
        }

        return downloadEntry(entry, path, options);
    }

    /**
     * implement driver depends code
     *
     * @param entry source
     * @param path  source
     * @see #newInputStream(Path, Set)
     */
    protected abstract InputStream downloadEntry(T entry, Path path, Set<? extends OpenOption> options) throws IOException;

    @Override
    public final OutputStream newOutputStream(Path path, Set<? extends OpenOption> options) throws IOException {
        try {
            T entry = getEntry(path);

            if (exists(entry)) {
                if (isFolder(entry)) {
                    throw new IsDirectoryException("path: " + path);
                } else {
                    whenUploadEntryExists(entry, path, options);
                }
            }
            logger.log(Level.DEBUG, "newOutputStream: cause target not exists");
        } catch (NoSuchFileException e) {
            logger.log(Level.DEBUG, "newOutputStream: cause target not found, " + e.getMessage());
        }

        T parent = getEntry(path.toAbsolutePath().getParent());
        return uploadEntry(parent, path, options);
    }

    /**
     * Overrides this method if you want to do special action when the target file exists.
     *
     * @throws FileAlreadyExistsException if you don't override this method.
     * @see #newOutputStream(Path, Set), {@link #uploadEntry(Object, Path, Set)}
     */
    protected void whenUploadEntryExists(T destEntry, Path path, Set<? extends OpenOption> options) throws IOException {
        throw new FileAlreadyExistsException("path: " + path);
    }

    /**
     * you must implement `cache.addEntry(path, newEntry)` after async upload is done.
     *
     * @param parentEntry dest parent
     * @param path        dest
     * @see #newOutputStream(Path, Set)
     */
    protected abstract OutputStream uploadEntry(T parentEntry, Path path, Set<? extends OpenOption> options) throws IOException;

    @Override
    public final DirectoryStream<Path> newDirectoryStream(
            Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        return Util.newDirectoryStream(getDirectoryEntries(dir, false), filter);
    }

    /**
     * implement driver depends code
     *
     * @see #newDirectoryStream(Path, DirectoryStream.Filter), {@link #getDirectoryEntries(Path, boolean)}}
     */
    protected abstract List<T> getDirectoryEntries(T dirEntry, Path dir) throws IOException;

    /**
     * common process
     *
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
                throw new FileAlreadyExistsException("dir: " + dir);
            }
            logger.log(Level.DEBUG, "createDirectory: target cause not exists");
        } catch (NoSuchFileException e) {
            logger.log(Level.DEBUG, "createDirectory: target cause not found, " + e.getMessage());
        }

        createDirectoryEntry(dir);
    }

    /**
     * implement driver depends code
     *
     * @see #createDirectory(Path, FileAttribute[]), {@link #createDirectoryEntry(Path)}
     */
    protected abstract T createDirectoryEntry(T parentEntry, Path dir) throws IOException;

    /**
     * common process
     *
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
     *
     * @see #delete(Path)
     */
    protected abstract boolean hasChildren(T dirEntry, Path dir) throws IOException;

    /**
     * implement driver depends code
     *
     * @see #delete(Path), {@link #removeEntry(Path)}
     */
    protected abstract void removeEntry(T entry, Path path) throws IOException;

    /**
     * common process
     *
     * @see #removeEntry(Object, Path)
     */
    protected void removeEntry(Path path) throws IOException {
        T entry = getEntry(path);
        removeEntry(entry, path);
    }

    /**
     * <ul>
     *  <li>if the source and the target are the same file, do nothing</li>
     *  <li>if the source is a directory, an empty directory is created at the target</li>
     * </ul>
     *
     * @throws NoSuchFileException        the source does not exist
     * @throws FileAlreadyExistsException the target exists and {@link StandardCopyOption#REPLACE_EXISTING} is not set
     * @throws DirectoryNotEmptyException {@link StandardCopyOption#REPLACE_EXISTING} is set but the target is a non-empty directory
     */
    @Override
    public final void copy(Path source, Path target, Set<CopyOption> options) throws IOException {
        T sourceEntry = getEntry(source);
        if (!exists(sourceEntry)) {
            throw new NoSuchFileException(source.toString());
        }

        if (isSameFile(source, target)) {
            return;
        }

        removeTargetIfReplaceable(target, options);

        copyEntry(source, target, options);
    }

    /**
     * checks the target existence before copy or move, and removes the target if replacing is allowed.
     *
     * @throws FileAlreadyExistsException the target exists and {@link StandardCopyOption#REPLACE_EXISTING} is not set
     * @throws DirectoryNotEmptyException {@link StandardCopyOption#REPLACE_EXISTING} is set but the target is a non-empty directory
     */
    private void removeTargetIfReplaceable(Path target, Set<CopyOption> options) throws IOException {
        T targetEntry;
        try {
            targetEntry = getEntry(target);
        } catch (NoSuchFileException e) {
            logger.log(Level.DEBUG, "cause target not found, " + e.getMessage());
            return;
        }
        if (!exists(targetEntry)) {
            logger.log(Level.DEBUG, "cause target not exists");
            return;
        }

        if (options == null || !options.contains(StandardCopyOption.REPLACE_EXISTING)) {
            throw new FileAlreadyExistsException(target.toString());
        }
        if (isFolder(targetEntry) && hasChildren(targetEntry, target)) {
            throw new DirectoryNotEmptyException(target.toString());
        }

        removeEntry(target);
    }

    /**
     * implement driver depends code
     *
     * @return null means that copy is async, after process like cache by your self.
     * @see #copyEntry(Object, Object, Path, Path, Set)
     */
    protected abstract T copyEntry(T sourceEntry, T targetParentEntry, Path source, Path target, Set<CopyOption> options) throws IOException;

    /**
     * common process
     *
     * @see #copyEntry(Object, Object, Path, Path, Set)
     */
    protected void copyEntry(Path source, Path target, Set<CopyOption> options) throws IOException {
        T sourceEntry = getEntry(source);
        T targetParentEntry = getEntry(target.toAbsolutePath().getParent());
        if (!isFolder(sourceEntry)) {
            copyEntry(sourceEntry, targetParentEntry, source, target, options);
        } else {
            // java spec. copies a folder as an empty folder
            createDirectoryEntry(target);
        }
    }

    /**
     * <ul>
     *  <li>if the source and the target are the same file, do nothing</li>
     *  <li>if the target is an existing directory, the source is NOT moved into the directory</li>
     *  <li>if the source and the target have the same parent, {@link #renameEntry(Path, Path)} is used,
     *      otherwise {@link #moveEntry(Path, Path, boolean)}</li>
     * </ul>
     *
     * @throws NoSuchFileException        the source does not exist
     * @throws FileAlreadyExistsException the target exists and {@link StandardCopyOption#REPLACE_EXISTING} is not set
     * @throws DirectoryNotEmptyException {@link StandardCopyOption#REPLACE_EXISTING} is set but the target is a non-empty directory
     * @throws FileSystemException        the source is a directory and the target is in the source
     */
    @Override
    public final void move(Path source, Path target, Set<CopyOption> options) throws IOException {
        T sourceEntry = getEntry(source);
        if (!exists(sourceEntry)) {
            throw new NoSuchFileException(source.toString());
        }

        if (isSameFile(source, target)) {
            return;
        }

        if (isFolder(sourceEntry) && target.toAbsolutePath().startsWith(source.toAbsolutePath())) {
            throw new FileSystemException(source.toString(), target.toString(), "cannot move a directory into itself");
        }

        removeTargetIfReplaceable(target, options);

        if (source.toAbsolutePath().getParent().equals(target.toAbsolutePath().getParent())) {
            // rename
            renameEntry(source, target);
        } else {
            moveEntry(source, target, false);
        }
    }

    /**
     * implement driver depends code
     *
     * @param targetIsParent always false, {@link #move(Path, Path, Set)} doesn't move into an existing directory (JSR-203).
     *                       still exists for compatibility.
     * @see #move(Path, Path, Set)
     */
    protected abstract T moveEntry(T sourceEntry, T targetParentEntry, Path source, Path target, boolean targetIsParent) throws IOException;

    /**
     * implement driver depends code
     *
     * @param targetIsParent always false, see {@link #moveEntry(Object, Object, Path, Path, boolean)}
     * @see #move(Path, Path, Set)
     */
    protected abstract T moveFolderEntry(T sourceEntry, T targetParentEntry, Path source, Path target, boolean targetIsParent) throws IOException;

    /**
     * common process
     *
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
     *
     * @see #renameEntry(Path, Path)
     */
    protected abstract T renameEntry(T sourceEntry, T targetParentEntry, Path source, Path target) throws IOException;

    /**
     * common process
     *
     * @see #renameEntry(Object, Object, Path, Path)
     */
    protected void renameEntry(Path source, Path target) throws IOException {
        T sourceEntry = getEntry(source);
        T targetParentEntry = getEntry(target.toAbsolutePath().getParent());
        renameEntry(sourceEntry, targetParentEntry, source, target);
    }

    /**
     * to ignore check, override me
     *
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
     *
     * @see #checkAccessImpl(Path, AccessMode...)
     */
    protected void checkAccessEntry(T entry, Path path, AccessMode... modes) throws IOException {
        // TODO: assumed; not a file == directory
        for (AccessMode mode : modes) {
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
     *
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
