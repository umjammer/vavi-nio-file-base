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

import java.io.IOException;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.fge.filesystem.exceptions.NoSuchAttributeException;
import com.github.fge.filesystem.exceptions.ReadOnlyAttributeException;


/**
 * Provider for the {@code "acl"} file attribute view
 *
 * <p>By default, the list of provided ACLs is empty.</p>
 *
 * @see AclFileAttributeView
 */
@SuppressWarnings("DesignForExtension")
@ParametersAreNonnullByDefault
public abstract class AclFileAttributesProvider extends FileAttributesProvider implements AclFileAttributeView {

    protected AclFileAttributesProvider() throws IOException {
        super("acl");
    }

    @Override
    public List<AclEntry> getAcl() throws IOException {
        return Collections.emptyList();
    }

    //
    // read
    //

    //
    // write
    //

    @Override
    public void setOwner(UserPrincipal owner) throws IOException {
        throw new ReadOnlyAttributeException();
    }

    @Override
    public void setAcl(List<AclEntry> acl) throws IOException {
        throw new ReadOnlyAttributeException();
    }

    /*
     * by name
     */
    @Override
    @SuppressWarnings("unchecked")
    public final void setAttributeByName(String name, Object value) throws IOException {
        Objects.requireNonNull(value);
        switch (Objects.requireNonNull(name)) {
            case "owner" -> setOwner((UserPrincipal) value); // owner
            case "acl" -> setAcl((List<AclEntry>) value); // acl
            default -> throw new NoSuchAttributeException(name);
        }
    }

    @Nullable
    @Override
    public final Object getAttributeByName(String name) throws IOException {
        return switch (Objects.requireNonNull(name)) {
            case "owner" -> getOwner(); // owner
            case "acl" -> getAcl(); // acl
            default -> throw new NoSuchAttributeException(name);
        };
    }

    @Nonnull
    @Override
    public final Map<String, Object> getAllAttributes() throws IOException {
        Map<String, Object> map = new HashMap<>();

        map.put("owner", getOwner());
        map.put("acl", getAcl());

        return Collections.unmodifiableMap(map);
    }
}
