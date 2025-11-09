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

package com.github.fge.filesystem.path;

import com.github.fge.filesystem.fs.GenericFileSystem;
import com.github.fge.filesystem.watch.AbstractWatchService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

/**
 * Generic {@link Path} implementation
 *
 * <p><strong>IMPORTANT:</strong> unlike the definition of {@link Path}, an
 * empty path has no name elements. In theory, an empty path is a path with a
 * single, empty name element, but this is an outright bug, since no filesystem
 * suports empty name elements. At the moment is it NOT planned to "fix" this.
 * </p>
 *
 * <p>Most of the heavy lifting of path manipulation (resolution, parent etc)
 * is delegated to the {@link PathElementsFactory} provided as an argument to
 * the constructor, which is why this class can be made {@code final}.</p>
 *
 * <p>You won't want to create instances of this class directly; use {@link
 * FileSystem#getPath(String, String...)} instead.</p>
 *
 * @see PathElementsFactory
 * @see PathElements
 */
// TODO: empty path problem?
// TODO: introduce the notion of a "current context"
@ParametersAreNonnullByDefault
public final class GenericPath
    implements Path
{
    private final GenericFileSystem fs;

    private final PathElementsFactory factory;
    // visible for testing
    final PathElements elements;
    private final String asString;

    /**
     * Constructor
     *
     * @param fs the file system this path is issued from
     * @param factory the path elements factory
     * @param elements the path elements
     */
    public GenericPath(GenericFileSystem fs,
                       PathElementsFactory factory, PathElements elements)
    {
        this.fs = Objects.requireNonNull(fs);
        this.factory = Objects.requireNonNull(factory);
        this.elements = Objects.requireNonNull(elements);
        asString = factory.toString(elements);
    }

    @Override
    public FileSystem getFileSystem()
    {
        return fs;
    }

    @Override
    public boolean isAbsolute()
    {
        return factory.isAbsolute(elements);
    }

    @Override
    public Path getRoot()
    {
        PathElements newElements = elements.rootPathElement();
        return newElements == null ? null
            : new GenericPath(fs, factory, newElements);
    }

    @Override
    public Path getFileName()
    {
        PathElements names = elements.lastName();
        return names == null ? null : new GenericPath(fs, factory, names);
    }

    @Override
    public Path getParent()
    {
        PathElements newNames = elements.parent();
        return newNames == null ? null : new GenericPath(fs, factory, newNames);
    }

    @Override
    public int getNameCount()
    {
        return elements.names.length;
    }

    @Override
    public Path getName(int index)
    {
        String name;

        //noinspection ProhibitedExceptionCaught
        try {
            name = elements.names[index];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("illegal index " + index, e);
        }

        return new GenericPath(fs, factory, PathElements.singleton(name));
    }

    @Override
    public Path subpath(int beginIndex, int endIndex)
    {
        String[] names;

        //noinspection ProhibitedExceptionCaught
        try {
            names = Arrays.copyOfRange(elements.names, beginIndex, endIndex);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("invalid begin and/or end index",
                e);
        }

        // The result never has a root
        PathElements newNames = new PathElements(null, names);
        return new GenericPath(fs, factory, newNames);
    }

    @Override
    public boolean startsWith(Path other)
    {
        if (!fs.equals(other.getFileSystem()))
            return false;

        PathElements otherNames = ((GenericPath) other).elements;
        if (!Objects.equals(elements.root, otherNames.root))
            return false;
        int len = otherNames.names.length;
        if (len > elements.names.length)
            return false;
        for (int i = 0; i < len; i++)
            if (!elements.names[i].equals(otherNames.names[i]))
                return false;
        return true;
    }

    @Override
    public boolean startsWith(String other)
    {
        Path otherPath
            = new GenericPath(fs, factory, factory.toPathElements(other));
        return startsWith(otherPath);
    }

    @Override
    public boolean endsWith(Path other)
    {
        if (!fs.equals(other.getFileSystem()))
            return false;

        PathElements otherElements = ((GenericPath) other).elements;

        //noinspection VariableNotUsedInsideIf
        if (otherElements.root != null)
            return false;

        String[] names = elements.names;
        int length = names.length;
        String[] otherNames = otherElements.names;
        int otherLength = otherNames.length;

        if (length < otherLength)
            return false;

        for (int i = 0; i < otherLength; i++)
            if (!names[length - i].equals(otherNames[otherLength - i]))
                return false;

        return true;
    }

    @Override
    public boolean endsWith(String other)
    {
        GenericPath otherPath
            = new GenericPath(fs, factory, factory.toPathElements(other));
        return endsWith(otherPath);
    }

    @Override
    public Path normalize()
    {
        PathElements normalized = factory.normalize(elements);
        return elements.equals(normalized) ? this
            : new GenericPath(fs, factory, normalized);
    }

    @SuppressWarnings("ObjectEquality")
    @Override
    public Path resolve(Path other)
    {
        checkProvider(other);
        GenericPath otherPath = (GenericPath) other;

        PathElements newNames
            = factory.resolve(elements, otherPath.elements);

        /*
         * See PathElementsFactory's .resolve()
         */
        if (newNames == elements)
            return this;
        if (newNames == otherPath.elements)
            return other;

        return new GenericPath(fs, factory, newNames);
    }

    @Override
    public Path resolve(String other)
    {
        PathElements otherElements = factory.toPathElements(other);
        return resolve(new GenericPath(fs, factory, otherElements));
    }

    @Override
    public Path resolveSibling(Path other)
    {
        checkProvider(other);
        GenericPath otherPath = (GenericPath) other;

        PathElements newNames
            = factory.resolveSibling(elements, otherPath.elements);

        /*
         * See PathElementsFactory's .resolve()
         */
        //noinspection ObjectEquality
        if (newNames == otherPath.elements)
            return other;

        return new GenericPath(fs, factory, newNames);
    }

    @Override
    public Path resolveSibling(String other)
    {
        PathElements otherElements = factory.toPathElements(other);
        return resolveSibling(new GenericPath(fs, factory, otherElements));
    }

    @Override
    public Path relativize(Path other)
    {
        checkProvider(other);

        GenericPath otherPath = (GenericPath) other;
        PathElements otherElements = otherPath.elements;

        PathElements relativized
            = factory.relativize(elements, otherElements);

        return new GenericPath(fs, factory, relativized);
    }

    @Override
    public URI toUri()
    {
        // URI is normalized, so this works...
        URI base = fs.getUri();
        String scheme = base.getScheme();
        String authority = base.getAuthority();
        String path = base.getPath();
        String query = base.getQuery();
        String fragment = base.getFragment();
        String uriPath = factory.toUriPath(path, elements);
        try {
            return new URI(scheme, authority, uriPath, query, fragment)
                .normalize();
        } catch (URISyntaxException e) {
            throw new RuntimeException("How did I get there??", e);
        }
    }

    @Override
    public Path toAbsolutePath()
    {
        if (isAbsolute())
            return this;
        PathElements root = factory.getRootPathElements();
        return new GenericPath(fs, factory, factory.resolve(root, elements));
    }

    @SuppressWarnings("OverloadedVarargsMethod")
    @Override
    public Path toRealPath(LinkOption... options)
        throws IOException
    {
        // TODO: symlinks
        return toAbsolutePath();
    }

    @Override
    public File toFile()
    {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("OverloadedVarargsMethod")
    @Override
    public WatchKey register(WatchService watcher,
                             WatchEvent.Kind<?>[] events,
                             WatchEvent.Modifier... modifiers)
        throws IOException
    {
        if (!(watcher instanceof AbstractWatchService service)) {
            throw new IllegalArgumentException(
                "watcher (" + watcher + ") is not associated with this file system");
          }

        return service.register(this, Arrays.asList(events), modifiers);
    }

    @SuppressWarnings("OverloadedVarargsMethod")
    @Override
    public WatchKey register(WatchService watcher,
                             WatchEvent.Kind<?>... events)
        throws IOException
    {
        return register(watcher, events, new WatchEvent.Modifier[0]);
    }

    @SuppressWarnings("AnonymousInnerClassWithTooManyMethods")
    @Override
    public Iterator<Path> iterator()
    {
        Iterator<PathElements> iterator = elements.iterator();

        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Path next() {
                return new GenericPath(fs, factory, iterator.next());
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public int compareTo(Path other)
    {
        try {
            checkProvider(other);
        } catch (ProviderMismatchException ignored) {
            // Meh. Required by the contract.
            throw new ClassCastException();
        }
        return asString.compareTo(other.toString());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(fs, factory, elements);
    }

    @Override
    public boolean equals(@Nullable Object obj)
    {
        if (obj == null)
            return false;
        if (this == obj)
            return true;
        if (getClass() != obj.getClass())
            return false;
        GenericPath other = (GenericPath) obj;
        return fs.equals(other.fs)
            && factory.equals(other.factory)
            && elements.equals(other.elements);
    }

    @Override
    @Nonnull
    public String toString()
    {
        return asString;
    }

    private void checkProvider(Path other)
    {
        if (!fs.provider().equals(other.getFileSystem().provider()))
            throw new ProviderMismatchException();
    }
}
