/*
 * Copyright (c) 2014, Francis Galiegue (fgaliegue@gmail.com)
 *
 * This software is dual-licensed under:
 *
 * - the Lesser General Public License (LGPL) version 3.0 or, at your option, any
 *   later version;
 * - the Apache Software License (ASL) version 2.0.
 *
 * The text of both licenses is available under the src/resources/ directory of
 * this project (under the names LGPL-3.0.txt and ASL-2.0.txt respectively).
 *
 * Direct link to the sources:
 *
 * - LGPL 3.0: https://www.gnu.org/licenses/lgpl-3.0.txt
 * - ASL 2.0: http://www.apache.org/licenses/LICENSE-2.0.txt
 */

package com.github.fge.filesystem.provider;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.ClosedFileSystemException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.fge.filesystem.driver.FileSystemDriver;
import com.github.fge.filesystem.fs.GenericFileSystem;


@ParametersAreNonnullByDefault
public abstract class FileSystemRepositoryBase
    implements FileSystemRepository
{
    private final String scheme;
    private final Map<URI, GenericFileSystem> filesystems = new HashMap<>();

    protected final FileSystemFactoryProvider factoryProvider;

    protected FileSystemRepositoryBase(String scheme,
                                       FileSystemFactoryProvider factoryProvider)
    {
        this.scheme = Objects.requireNonNull(scheme);
        this.factoryProvider = Objects.requireNonNull(factoryProvider);
        factoryProvider.validate();
    }

    @Override
    @Nonnull
    public final String getScheme()
    {
        return scheme;
    }

    @Nonnull
    @Override
    public final FileSystemFactoryProvider getFactoryProvider()
    {
        return factoryProvider;
    }

    @Nonnull
    protected abstract FileSystemDriver createDriver(URI uri,
        Map<String, ?> env)
        throws IOException;

    @Override
    @Nonnull
    public final FileSystem createFileSystem(FileSystemProvider provider,
                                             URI uri, Map<String, ?> env)
        throws IOException
    {
        Objects.requireNonNull(provider);
        Objects.requireNonNull(env);
        checkURI(uri);

        synchronized (filesystems) {
            if (filesystems.containsKey(uri))
                throw new FileSystemAlreadyExistsException();
            FileSystemDriver driver = createDriver(uri, env);
            GenericFileSystem fs
                = new GenericFileSystem(uri, this, driver, provider);
            filesystems.put(uri, fs);
            return fs;
        }
    }

    @Override
    @Nonnull
    public final FileSystem getFileSystem(URI uri)
    {
        checkURI(uri);

        FileSystem fs;

        synchronized (filesystems) {
            fs = filesystems.get(uri);
        }

        if (fs == null)
            throw new FileSystemNotFoundException();

        return fs;
    }

    // Note: fs never created automatically
    @Override
    @Nonnull
    public final Path getPath(URI uri)
    {
        checkURI(uri);

        URI tmp;
        GenericFileSystem fs;
        String path;

        synchronized (filesystems) {
            for (Map.Entry<URI, GenericFileSystem> entry:
                filesystems.entrySet()) {
                tmp = uri.relativize(entry.getKey());
                if (tmp.isAbsolute())
                    continue;
                fs = entry.getValue();
                // TODO: can happen...
                if (!fs.isOpen())
                    continue;
                path = tmp.getPath();
                if (path == null)
                    path = "";
                return entry.getValue().getPath(path);
            }
        }

        throw new FileSystemNotFoundException();
    }

    @Nonnull
    @Override
    public final FileSystemDriver getDriver(Path path)
    {
        FileSystem fs = Objects.requireNonNull(path).getFileSystem();

        synchronized (filesystems) {
            for (GenericFileSystem gfs: filesystems.values()) {
                //noinspection ObjectEquality
                if (gfs != fs)
                    continue;
                if (!gfs.isOpen())
                    throw new ClosedFileSystemException();
                return gfs.getDriver();
            }
        }

        throw new FileSystemNotFoundException();
    }

    // Called ONLY after the driver and fs have been successfully closed
    // uri is guaranteed to exist
    @Override
    public final void unregister(URI uri)
    {
        Objects.requireNonNull(uri);
        synchronized (filesystems) {
            filesystems.remove(uri);
        }
    }

    /** if you want to check at the provider level, override */
    protected void checkURI(@Nullable URI uri)
    {
        Objects.requireNonNull(uri);
        if (!uri.isAbsolute())
            throw new IllegalArgumentException("uri is not absolute");
        if (uri.isOpaque())
            throw new IllegalArgumentException("uri is not hierarchical "
                + "(.isOpaque() returns true)");
        if (!scheme.equals(uri.getScheme()))
            throw new IllegalArgumentException("bad scheme");
    }

    /** */
    protected Map<String, String> getParamsMap(URI uri) {
        try {
            Map<String, String[]> params = splitQuery(uri);
            Map<String, String> result = new HashMap<>();
            for (String key : params.keySet()) {
                String[] values = params.get(key);
                result.put(key, values != null && values.length > 0 ? values[0] : null);
            }
            return result;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** */
    private static Map<String, String[]> splitQuery(URI uri) throws IOException {
        Map<String, String[]> queryPairs = new HashMap<>();
        if (uri.getQuery() != null) {
            String[] pairs = uri.getQuery().split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8) : pair;
                String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8) : null;
                if (!queryPairs.containsKey(key)) {
                    queryPairs.put(key, new String[] { value });
                } else {
                    queryPairs.put(key, Stream.concat(Arrays.stream(queryPairs.get(key)), Arrays.stream(new String[] { value })).toArray(String[]::new));
                }
            }
        }
        return queryPairs;
    }
}
