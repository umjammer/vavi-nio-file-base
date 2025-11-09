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

package com.github.fge.filesystem.attributes.provider;

import com.github.fge.filesystem.exceptions.NoSuchAttributeException;
import com.github.fge.filesystem.exceptions.ReadOnlyAttributeException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Provider for the {@code "posix"} file attribute view
 *
 * <p>The default values are the same as {@link BasicFileAttributesProvider};
 * in addition, the default permissions are {@code "rwxr-wr-x"} for directories
 * and {@code rw-r--r--} for all other types.</p>
 *
 * @see PosixFileAttributeView
 * @see PosixFileAttributes
 * @see PosixFilePermissions
 */
@SuppressWarnings("DesignForExtension")
@ParametersAreNonnullByDefault
public abstract class PosixFileAttributesProvider
    extends FileAttributesProvider
    implements PosixFileAttributeView, PosixFileAttributes
{
    protected static final FileTime UNIX_EPOCH = FileTime.fromMillis(0L);
    protected static final Set<PosixFilePermission> DEFAULT_OTHER_PERMS
        = PosixFilePermissions.fromString("rw-r--r--");
    protected static final Set<PosixFilePermission> DEFAULT_DIRECTORY_PERMS
        = PosixFilePermissions.fromString("rwxr-xr-x");

    protected PosixFileAttributesProvider()
        throws IOException
    {
        super("posix");
    }

    /*
     * Attributes
     */
    @Override
    public final PosixFileAttributes readAttributes()
        throws IOException
    {
        return this;
    }

    /*
     * Read
     */
    @Override
    public FileTime lastModifiedTime()
    {
        return UNIX_EPOCH;
    }

    @Override
    public FileTime lastAccessTime()
    {
        return UNIX_EPOCH;
    }

    @Override
    public FileTime creationTime()
    {
        return UNIX_EPOCH;
    }

    @Override
    public boolean isSymbolicLink()
    {
        return false;
    }

    @Override
    public boolean isOther()
    {
        return false;
    }

    @Override
    public Object fileKey()
    {
        return null;
    }

    @Override
    public Set<PosixFilePermission> permissions()
    {
        return isRegularFile() ? DEFAULT_OTHER_PERMS : DEFAULT_DIRECTORY_PERMS;
    }

    /*
     * Write
     */
    @Override
    public void setTimes(@Nullable FileTime lastModifiedTime,
        @Nullable FileTime lastAccessTime,
        @Nullable FileTime createTime)
        throws IOException
    {
        throw new ReadOnlyAttributeException();
    }

    @Override
    public void setOwner(UserPrincipal owner)
        throws IOException
    {
        throw new ReadOnlyAttributeException();
    }

    @Override
    public void setGroup(GroupPrincipal group)
        throws IOException
    {
        throw new ReadOnlyAttributeException();
    }

    @Override
    public void setPermissions(Set<PosixFilePermission> perms)
        throws IOException
    {
        throw new ReadOnlyAttributeException();
    }

    /*
     * By name
     */
    @Override
    public void setAttributeByName(String name, Object value)
        throws IOException
    {
        Objects.requireNonNull(value);
        switch (Objects.requireNonNull(name)) {
            /* basic */
            case "lastModifiedTime":
                setTimes((FileTime) value, null, null);
                break;
            case "lastAccessTime":
                setTimes(null, (FileTime) value, null);
                break;
            case "creationTime":
                setTimes(null, null, (FileTime) value);
                break;
            /* owner */
            case "owner":
                setOwner((UserPrincipal) value);
                break;
            /* posix */
            case "group":
                setGroup((GroupPrincipal) value);
                break;
            case "permissions":
                //noinspection unchecked
                setPermissions((Set<PosixFilePermission>) value);
                break;
            case "size":
            case "isRegularFile":
            case "isDirectory":
            case "isSymbolicLink":
            case "isOther":
            case "fileKey":
                throw new ReadOnlyAttributeException(name);
            default:
                throw new NoSuchAttributeException(name);
        }
    }

    @SuppressWarnings("OverlyComplexMethod")
    @Nonnull
    @Override
    public Object getAttributeByName(String name)
        throws IOException
    {
        return switch (Objects.requireNonNull(name)) {
            /* basic */
            case "lastModifiedTime" -> lastModifiedTime();
            case "lastAccessTime" -> lastAccessTime();
            case "creationTime" -> creationTime();
            case "size" -> size();
            case "isRegularFile" -> isRegularFile();
            case "isDirectory" -> isDirectory();
            case "isSymbolicLink" -> isSymbolicLink();
            case "isOther" -> isOther();
            case "fileKey" -> fileKey();
            /* owner */
            case "owner" -> owner();
            /* posix */
            case "group" -> group();
            case "permissions" -> permissions();
            default -> throw new NoSuchAttributeException(name);
        };
    }

    @Nonnull
    @Override
    public final Map<String, Object> getAllAttributes()
        throws IOException
    {
        Map<String, Object> map = new HashMap<>();

        map.put("lastModifiedTime", lastModifiedTime());
        map.put("lastAccessTime", lastAccessTime());
        map.put("creationTime", creationTime());
        map.put("size", size());
        map.put("isRegularFile", isRegularFile());
        map.put("isDirectory", isDirectory());
        map.put("isSymbolicLink", isSymbolicLink());
        map.put("isOther", isOther());
        map.put("fileKey", fileKey());
        map.put("owner", owner());
        map.put("group", group());
        map.put("permissions", permissions());

        return Collections.unmodifiableMap(map);
    }
}
