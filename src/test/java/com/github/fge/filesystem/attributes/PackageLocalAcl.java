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

package com.github.fge.filesystem.attributes;

import com.github.fge.filesystem.attributes.provider.AclFileAttributesProvider;
import com.github.fge.filesystem.attributes.testclasses.ArgType1;

import java.io.IOException;
import java.nio.file.attribute.UserPrincipal;

@SuppressWarnings("PublicConstructorInNonPublicClass")
final class PackageLocalAcl
    extends AclFileAttributesProvider
{
    public PackageLocalAcl(final ArgType1 arg)
        throws IOException
    {
    }

    @Override
    public UserPrincipal getOwner()
        throws IOException
    {
        return null;
    }
}
