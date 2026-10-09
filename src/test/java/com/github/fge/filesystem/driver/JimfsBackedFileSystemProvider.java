/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package com.github.fge.filesystem.driver;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.CopyOption;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

import com.github.fge.filesystem.attributes.FileAttributesFactory;
import com.github.fge.filesystem.attributes.provider.BasicFileAttributesProvider;
import com.github.fge.filesystem.filestore.FileStoreBase;
import com.github.fge.filesystem.options.FileSystemOptionsFactory;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;
import com.github.fge.filesystem.provider.FileSystemProviderBase;
import com.github.fge.filesystem.provider.FileSystemRepositoryBase;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;


/**
 * A test file system whose driver is a {@link CachedFileSystemDriver} backed by jimfs.
 * <p>
 * A jimfs {@link Path} wrapped by {@link Entry} is used as the driver's entry object.
 * Primitive operations (copyEntry, moveEntry ...) are naive on purpose,
 * the JSR-203 semantics must be provided by the base classes.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-08 nsano initial version <br>
 */
public final class JimfsBackedFileSystemProvider extends FileSystemProviderBase {

    public static final String SCHEME = "jimfstest";

    public JimfsBackedFileSystemProvider() {
        super(new Repository());
    }

    /** */
    public static final class BasicAttributes extends BasicFileAttributesProvider {

        private final Path entry;

        public BasicAttributes(@Nonnull Entry entry) throws IOException {
            this.entry = entry.path();
        }

        @Override
        public FileTime lastModifiedTime() {
            try {
                return Files.getLastModifiedTime(entry);
            } catch (IOException e) {
                return UNIX_EPOCH;
            }
        }

        @Override
        public boolean isRegularFile() {
            return Files.isRegularFile(entry);
        }

        @Override
        public boolean isDirectory() {
            return Files.isDirectory(entry);
        }

        @Override
        public long size() {
            try {
                return isDirectory() ? 0 : Files.size(entry);
            } catch (IOException e) {
                return 0;
            }
        }
    }

    /** */
    static final class AttributesFactory extends ExtendedFileSystemDriverBase.ExtendedFileAttributesFactory {

        AttributesFactory() {
            setMetadataClass(Entry.class);
            addImplementation("basic", BasicAttributes.class);
        }
    }

    /** as same as downstream drivers */
    static final class OptionsFactory extends FileSystemOptionsFactory {

        OptionsFactory() {
            addLinkOption(LinkOption.NOFOLLOW_LINKS);
        }
    }

    /** */
    static final class FactoryProvider extends FileSystemFactoryProvider {

        FactoryProvider() {
            setAttributesFactory(new AttributesFactory());
            setOptionsFactory(new OptionsFactory());
        }
    }

    /** */
    static final class Store extends FileStoreBase {

        Store(FileAttributesFactory factory) {
            super("jimfs", factory, false);
        }

        @Override
        public long getTotalSpace() {
            return Long.MAX_VALUE;
        }

        @Override
        public long getUsableSpace() {
            return Long.MAX_VALUE;
        }

        @Override
        public long getUnallocatedSpace() {
            return Long.MAX_VALUE;
        }
    }

    /** */
    static final class Repository extends FileSystemRepositoryBase {

        Repository() {
            super(SCHEME, new FactoryProvider());
        }

        @Nonnull
        @Override
        protected FileSystemDriver createDriver(URI uri, Map<String, ?> env) throws IOException {
            Driver driver = new Driver(new Store(factoryProvider.getAttributesFactory()), factoryProvider);
            driver.setEnv(env);
            return driver;
        }
    }

    /** entry object of the driver, wraps a jimfs path */
    public record Entry(Path path) {}

    /** naive driver, primitives don't care about JSR-203 semantics */
    static final class Driver extends CachedFileSystemDriver<Entry> {

        private final FileSystem jfs = Jimfs.newFileSystem(Configuration.unix());

        Driver(Store store, FileSystemFactoryProvider factoryProvider) {
            super(store, factoryProvider);
        }

        private Path toJ(Path path) {
            return jfs.getPath(path.toAbsolutePath().toString());
        }

        @Override
        protected String getFilenameString(Entry entry) {
            return entry.path().getFileName().toString();
        }

        @Override
        protected boolean isFolder(Entry entry) {
            return Files.isDirectory(entry.path());
        }

        @Override
        protected Entry getRootEntry(Path root) {
            return new Entry(jfs.getPath("/"));
        }

        @Override
        protected Entry getEntry(Entry parentEntry, Path path) {
            Path entry = toJ(path);
            return Files.exists(entry) ? new Entry(entry) : null;
        }

        @Override
        protected InputStream downloadEntry(Entry entry, Path path, Set<? extends OpenOption> options) throws IOException {
            return Files.newInputStream(entry.path());
        }

        @Override
        protected OutputStream uploadEntry(Entry parentEntry, Path path, Set<? extends OpenOption> options) throws IOException {
            Path entry = toJ(path);
            return new FilterOutputStream(Files.newOutputStream(entry)) {
                @Override
                public void close() throws IOException {
                    super.close();
                    updateEntry(path, new Entry(entry));
                }
            };
        }

        @Override
        protected List<Entry> getDirectoryEntries(Entry dirEntry, Path dir) throws IOException {
            try (Stream<Path> s = Files.list(dirEntry.path())) {
                return s.map(Entry::new).collect(Collectors.toList());
            }
        }

        @Override
        protected Entry createDirectoryEntry(Entry parentEntry, Path dir) throws IOException {
            return new Entry(Files.createDirectory(toJ(dir)));
        }

        @Override
        protected boolean hasChildren(Entry dirEntry, Path dir) throws IOException {
            try (Stream<Path> s = Files.list(dirEntry.path())) {
                return s.findAny().isPresent();
            }
        }

        @Override
        protected void removeEntry(Entry entry, Path path) throws IOException {
            Files.delete(entry.path());
        }

        @Override
        protected Entry copyEntry(Entry sourceEntry, Entry targetParentEntry, Path source, Path target, Set<CopyOption> options) throws IOException {
            return new Entry(Files.copy(sourceEntry.path(), toJ(target)));
        }

        @Override
        protected Entry moveEntry(Entry sourceEntry, Entry targetParentEntry, Path source, Path target, boolean targetIsParent) throws IOException {
            Path dst = targetIsParent ? targetParentEntry.path().resolve(source.getFileName().toString()) : toJ(target);
            return new Entry(Files.move(sourceEntry.path(), dst));
        }

        @Override
        protected Entry moveFolderEntry(Entry sourceEntry, Entry targetParentEntry, Path source, Path target, boolean targetIsParent) throws IOException {
            return moveEntry(sourceEntry, targetParentEntry, source, target, targetIsParent);
        }

        @Override
        protected Entry renameEntry(Entry sourceEntry, Entry targetParentEntry, Path source, Path target) throws IOException {
            return new Entry(Files.move(sourceEntry.path(), toJ(target)));
        }

        @Override
        public void close() throws IOException {
            jfs.close();
        }
    }
}
