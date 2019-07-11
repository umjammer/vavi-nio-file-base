/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/**
 * Test01.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/07/11 umjammer initial version <br>
 */
class Test01 {

    @Test
    void test() {
        Path p = Paths.get("/aaa/bbb/ccc/ddd");
        for (int i = 0; i < p.getNameCount(); i++) {
            System.err.println("name        : " + p.getName(i));
            System.err.println("sub path    : " + p.subpath(0, i + 1));
            System.err.println("name parent : " + p.getName(i).getParent());
            System.err.println("sub  parent : " + p.subpath(0, i + 1).getParent());
        }

        assertNull(p.getName(0).getParent());
        assertNull(p.getName(1).getParent());
        assertNull(p.getName(3).getParent());

        assertNull(p.subpath(0, 1).getParent());
        assertEquals("aaa", p.subpath(0, 2).getParent().toString());
    }
}

/* */
