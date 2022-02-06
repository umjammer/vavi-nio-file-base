/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package com.github.fge.filesystem.driver;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.FileStore;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.fge.filesystem.attributes.FileAttributesFactory;
import com.github.fge.filesystem.attributes.provider.BasicFileAttributesProvider;
import com.github.fge.filesystem.attributes.provider.FileAttributesProvider;
import com.github.fge.filesystem.exceptions.IsDirectoryException;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;

import vavi.nio.file.UploadMonitor;
import vavi.nio.file.Util;
import vavi.util.Debug;


/**
 * ExtendedFileSystemDriverBase.
 * <p>
 * *** CAUTION *** <br/>
 * if your file system driver class extends this class,
 * your {@link FileAttributesFactory} should extends {@link ExtendsdFileAttributesFactory}.
 * </p>
 * TODO separate about {@link UploadMonitor}
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/06/10 umjammer initial version <br>
 * @see UnixLikeFileSystemDriverBase
 */
@ParametersAreNonnullByDefault
public abstract class ExtendedFileSystemDriverBase extends UnixLikeFileSystemDriverBase {

    public static final String ENV_IGNORE_APPLE_DOUBLE = "ignoreAppleDouble";

    /** */
    protected boolean ignoreAppleDouble = false;

    /** currently set ignoreAppleDouble only */
    @SuppressWarnings("unchecked")
    protected void setEnv(Map<String, ?> env) {
        ignoreAppleDouble = (Boolean) ((Map<String, Object>) env).getOrDefault(ENV_IGNORE_APPLE_DOUBLE, Boolean.FALSE);
//Debug.println("ignoreAppleDouble: " + ignoreAppleDouble);
    }

    /** */
    private UploadMonitor<DummyFileAttributes> uploadMonitor;

    /** */
    protected ExtendedFileSystemDriverBase(final FileStore fileStore, final FileSystemFactoryProvider factoryProvider) {
        super(fileStore, factoryProvider);
        uploadMonitor = newUploadMonitor(); 
    }

    /** */
    protected UploadMonitor<DummyFileAttributes> newUploadMonitor() {
        return new UploadMonitor<>();
    }

    @Override
    public final SeekableByteChannel newByteChannel(final Path path,
                                              final Set<? extends OpenOption> options,
                                              final FileAttribute<?>... attrs) throws IOException {
        if (options != null && Util.isWriting(options)) {
            uploadMonitor.start(path, new DummyFileAttributesProvider());
            return new Util.SeekableByteChannelForWriting(newOutputStream(path, options)) {
                @Override
                protected long getLeftOver() throws IOException {
                    long leftover = 0;
                    if (options.contains(StandardOpenOption.APPEND)) {
                        BasicFileAttributes entry = readAttributes(path, BasicFileAttributes.class);
                        if (entry != null && entry.size() >= 0) {
                            leftover = entry.size();
                        }
                    }
                    return leftover;
                }

                @Override
                public SeekableByteChannel position(long pos) throws IOException {
// TODO ad-hoc
if (pos < uploadMonitor.entry(path).size()) {
 throw new IOException("{\"@vavi\":" + uploadMonitor.entry(path).size() + "}");
}
                    return super.position(pos);
                }

                @Override
                public int write(ByteBuffer src) throws IOException {
                    int n = super.write(src);
                    uploadMonitor.entry(path).setSize(written);
                    return n;
                }

                @Override
                public void close() throws IOException {
                    try {
                        super.close();
                    } finally {
                        uploadMonitor.finish(path);
                    }
                }
            };
        } else {
            BasicFileAttributes entry = readAttributes(path, BasicFileAttributes.class);
            if (entry.isDirectory()) {
                throw new IsDirectoryException(path.toString());
            }
            return new Util.SeekableByteChannelForReading(newInputStream(path, null)) {
                @Override
                protected long getSize() throws IOException {
                    return entry.size();
                }
            };
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
     * @throws IOException filesystem level error, or a plain I/O error.
     *               and you should throw {@link NoSuchFileException} when the file not found.
     * @see FileSystemProvider#checkAccess(Path, AccessMode...)
     */
    public final void checkAccess(Path path, AccessMode... modes) throws IOException {
        if (uploadMonitor.isUploading(path)) {
Debug.println("uploading... : " + path + ", " + uploadMonitor.entry(path));
            return;
        }

        checkAccessImpl(path, modes);
    }

    protected abstract void checkAccessImpl(Path path, AccessMode... modes) throws IOException;

    @Override
    public final Object getPathMetadata(final Path path) throws IOException {
        if (uploadMonitor.isUploading(path)) {
Debug.println("uploading... : " + path + ", " + uploadMonitor.entry(path));
            return uploadMonitor.entry(path);
        }

        return getPathMetadataImpl(path);
    }

    /**
     * @throws IOException you should throw {@link NoSuchFileException} when the file not found.
     */
    protected abstract Object getPathMetadataImpl(Path path) throws IOException;

    @Override
    protected FileAttributesProvider getProvider(String name, Object metadata) throws IOException {
        if (DummyFileAttributes.class.isInstance(metadata)) {
            return (FileAttributesProvider) metadata;
        }

        return super.getProvider(name, metadata);
    }

    /** */
    public static class ExtendsdFileAttributesFactory extends FileAttributesFactory {
        @SuppressWarnings("unchecked")
        protected <C> C getProviderInstance(Class<C> targetClass, Map<String, Class<?>> map, Object metadata) throws IOException {
            if (DummyFileAttributes.class.isInstance(metadata)) {
                if (PosixFileAttributes.class.equals(targetClass)) {
                    throw new UnsupportedOperationException("request posix for dummy");
                } else {
                    return (C) metadata;
                }
            }

            return super.getProviderInstance(targetClass, map, metadata);
        }
    }
}

/**
 * DummyFileAttributesProvider.
 *
 * TODO ugly
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/06/10 umjammer initial version <br>
 */
interface DummyFileAttributes {

    long size();

    void setSize(long size);
}

/**
 * Provider for the {@code "dummy"} file attribute view
 */
final class DummyFileAttributesProvider extends BasicFileAttributesProvider implements DummyFileAttributes {

    public DummyFileAttributesProvider() throws IOException {
        super();
    }

    private long size;

    @Override
    public long size() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return "{dummy:{size: " + size + "}}";
    }

    @Override
    public boolean isRegularFile() {
        return true;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }
}
