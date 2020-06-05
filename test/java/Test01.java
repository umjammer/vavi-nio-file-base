/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

import vavi.nio.file.Base;
import vavi.nio.file.Util;

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

    @Test
    void test01() throws Exception {
        Path path = Paths.get("src/main/java/vavi/nio/file/Util.java");
        assertEquals("Util.java", Util.toFilenameString(path));
//
//        path = Paths.get("src/test/resources/ハ゜ンダ.txt");
//System.err.println(StringUtil.getDump(path.toString().getBytes(Charset.forName("utf-8"))));
//        assertEquals("パンダ.txt", Util.toFilenameString(path));
//System.err.println(StringUtil.getDump(Util.toFilenameString(path).getBytes(Charset.forName("utf-8"))));
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
        InputStream is = new FileInputStream(src.toFile());
        OutputStream os = new FileOutputStream(dst.toFile());
        Util.transfer(is, os);
        assertEquals(Files.size(src), Files.size(dst));
    }
}

/* */
