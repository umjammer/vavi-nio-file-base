/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.text.Normalizer.Form;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import vavi.nio.file.Base;
import vavi.nio.file.Util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


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

    @Test
    @EnabledOnOs(OS.MAC)
    void test01() throws Exception {
        Path path = Paths.get("src/main/java/vavi/nio/file/Util.java");
        assertEquals("Util.java", Util.toFilenameString(path));

        String test1 = "src/test/resources/パンダ.txt";
        String test2 = Normalizer.normalize(test1, Form.NFD);
        assertNotEquals(test1, test2);
        String test3 = Normalizer.normalize(test2, Form.NFC);
        assertEquals(test1, test3);

        Path path1 = Paths.get(test1);
        assertTrue(Files.exists(path1));

        Path path2 = Paths.get(test2); // converted by file system
        assertTrue(Files.exists(path2));
    }

    @Test
    void test02() throws Exception {
        Base.testAll(Jimfs.newFileSystem(Configuration.unix()));
    }

    @Test
    void test04() throws Exception {
        Base.testLargeFile(Jimfs.newFileSystem(Configuration.unix()), null);
    }

    @Test
    void test05() throws Exception {
        Base.testMoveFolder(Jimfs.newFileSystem(Configuration.unix()));
    }

    @Test
    void test03() throws Exception {
        Path tmp = Paths.get("tmp");
        if (!Files.exists(tmp)) {
            Files.createDirectory(tmp);
        }
        Path src = Paths.get("src/test/resources/Hello.java");
        Path dst = Paths.get("tmp/Hello.java");
        if (Files.exists(dst)) {
            Files.delete(dst);
        }
        InputStream is = Files.newInputStream(src);
        OutputStream os = Files.newOutputStream(dst);
        Util.transfer(is, os);
        assertEquals(Files.size(src), Files.size(dst));
        Files.delete(dst);
        assertFalse(Files.exists(dst));
    }

    @Test
    void test06() throws Exception {
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
        Path dir = fs.getPath("test06");
        Files.createDirectory(dir);
        Path src = Paths.get(getClass().getResource("Hello.java").toURI());
        Files.copy(src, dir.resolve(src.getFileName().toString()));
        Base.removeTree(dir, false);
        assertTrue(Files.exists(dir));
        Files.copy(src, dir.resolve(src.getFileName().toString()));
        Base.removeTree(dir, true);
        assertFalse(Files.exists(dir));
    }
}

/* */
