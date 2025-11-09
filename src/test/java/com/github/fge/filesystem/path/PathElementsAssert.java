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

import java.util.Arrays;
import java.util.Objects;
import javax.annotation.ParametersAreNonnullByDefault;

import org.assertj.core.api.AbstractObjectAssert;


@ParametersAreNonnullByDefault
// cannot be final, see CustomSoftAssertions
public class PathElementsAssert
    extends AbstractObjectAssert<PathElementsAssert, PathElements>
{
    public PathElementsAssert(PathElements actual)
    {
        super(actual, PathElementsAssert.class);
    }

    public static PathElementsAssert assertElements(
        PathElements actual)
    {
        return new PathElementsAssert(actual);
    }

    /*
     * root checks
     */

    public final PathElementsAssert hasNullRoot()
    {
        if (actual.root != null)
            failWithMessage("root component is not null\n (is: <%s>)",
                actual.root);
        return this;
    }

    public final PathElementsAssert hasRoot(String expected)
    {
        if (!Objects.equals(actual.root, expected))
            failWithMessage(
                    """
                            root component is not what is expected
                            expected: <%s>
                            actual: <%s>
                            """,
                expected, actual.root
            );
        return this;
    }

    public final PathElementsAssert hasSameRootAs(PathElements other)
    {
        if (!Objects.equals(actual.root, other.root))
            failWithMessage(
                    """
                            root component is not the same as other
                            expected: <%s>
                            actual  : <%s>
                            """,
                other.root, actual.root
            );
        return this;
    }

    /*
     * names check
     */

    public final PathElementsAssert hasNoNames()
    {
        if (actual.names.length != 0)
            failWithMessage("names array (%s) is not empty",
                Arrays.toString(actual.names));
        return this;
    }

    public final PathElementsAssert hasNames(String... expected)
    {
        if (!Arrays.equals(actual.names, expected))
            failWithMessage("""
                            names array is not what is expected
                            expected: <%s>
                            actual  : <%s>
                            """,
                Arrays.toString(expected), Arrays.toString(actual.names));
        return this;
    }

    public final PathElementsAssert hasSameNamesAs(PathElements other)
    {
        if (!Arrays.equals(actual.names, other.names))
            failWithMessage(
                    """
                            names differ from provided elements instance
                            expected: <%s>
                            actual  : <%s>
                            """,
                Arrays.toString(other.names), Arrays.toString(actual.names));
        return this;
    }

    /*
     * PathElements check
     */

    public final PathElementsAssert hasSameContentsAs(PathElements other)
    {
        return hasSameRootAs(other).hasSameNamesAs(other);
    }
}
