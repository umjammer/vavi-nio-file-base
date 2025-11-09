/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import vavi.io.Seekable;
import vavi.nio.file.Base;
import vavi.nio.file.Util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * TestCase.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/07/11 umjammer initial version <br>
 */
class TestCase {

    static Path tmp = Paths.get("tmp");

    @BeforeAll
    static void setup() throws Exception {
        if (!Files.exists(tmp)) {
            Files.createDirectory(tmp);
        }
    }

    @Test
    @Disabled("just for NFD filename creation")
    void test0() throws Exception {
        Path path = Path.of("src/test/resources/パンダ.txt");
        String normalized = Normalizer.normalize(path.getFileName().toString(), Form.NFD);
        Files.copy(path, Path.of("tmp", normalized));
    }

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

    static final String nfd1 = "がぎぐげござじずぜぞだぢづでどばびぶべぼゞヾぱぴぷぺぽゔ";
    static final String nfd2 = "ガギグゲゴザジズゼゾダヂヅデドバビブベボゞヾパピプペポヴ";
    static final String nfc1 = "がぎぐげござじずぜぞだぢづでどばびぶべぼゞヾぱぴぷぺぽゔ";
    static final String nfc2 = "ガギグゲゴザジズゼゾダヂヅデドバビブベボゞヾパピプペポヴ";

    @Test
    void test11() throws Exception {
        String actual = Util.toNormalizedString(nfd1);
        assertEquals(nfc1, actual);
        actual = Util.toNormalizedString(nfd2);
        assertEquals(nfc2, actual);
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
        fs.close();
    }

    static class SeekableByteArrayInputStream extends InputStream implements Seekable {
        byte[] buf;
        int pos;

        SeekableByteArrayInputStream(byte[] buf) {
            this.buf = buf;
            this.pos = 0;
        }

        @Override
        public int available() {
            return buf.length - pos;
        }

        @Override
        public int read() {
            if (pos >= buf.length) {
                return -1;
            } else {
                return buf[pos++] & 0xff;
            }
        }

        @Override
        public void position(long l) {
            if (l < 0 || l >= buf.length) {
                throw new IndexOutOfBoundsException(String.valueOf(l));
            }
            pos = (int) l;
        }

        @Override
        public long position() {
            return pos;
        }
    }

    static class SeekableByteArrayOutputStream extends OutputStream implements Seekable {
        List<Byte> buf;
        int capacity;
        int pos;

        SeekableByteArrayOutputStream(int capacity) {
            this.capacity = capacity;
            this.buf = new ArrayList<>(capacity);
            this.pos = 0;
        }

        @Override
        public void write(int b) {
            for (int i = buf.size(); i <= pos; i++) {
                buf.add(i, (byte) 0);
            }
            buf.set(pos, (byte) b);
        }

        @Override
        public void position(long l) {
            if (l < 0 || l >= capacity) {
                throw new IndexOutOfBoundsException(String.valueOf(l));
            }
            pos = (int) l;
        }

        @Override
        public long position() {
            return pos;
        }

        /** */
        public byte[] toByteArray() {
            byte[] a = new byte[buf.size()];
            IntStream.range(0, buf.size()).forEach(i -> a[i] = buf.get(i));
            return a;
        }

        /** */
        public String toString() {
            return new String(toByteArray());
        }
    }

    @Test
    void test07() throws Exception {
        byte[] b = new byte[256];
        for (int i = 0; i < 256; i++) {
            b[i] = (byte) i;
        }
        InputStream is = new SeekableByteArrayInputStream(b);
        SeekableByteChannel sbc = new Util.SeekableByteChannelForReading(is) {
            @Override protected long getSize() {
                return b.length;
            }
        };
        sbc.position(100);
        byte[] rb = new byte[1];
        sbc.read(ByteBuffer.wrap(rb));
        assertEquals(100, rb[0]);
    }

    @Test
    void test07_1() throws Exception {
        byte[] b = new byte[256];
        for (int i = 0; i < 256; i++) {
            b[i] = (byte) i;
        }
        Path tmp = TestCase.tmp.resolve("test07_1.dat");
        Files.write(tmp, b, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
        FileInputStream fis = new FileInputStream(tmp.toFile());
        SeekableByteChannel sbc = new Util.SeekableByteChannelForReading(fis) {
            @Override protected long getSize() {
                return b.length;
            }
        };
        sbc.position(100);
        byte[] rb = new byte[1];
        sbc.read(ByteBuffer.wrap(rb));
        assertEquals(100, rb[0]);
        Files.delete(tmp);
    }

    @Test
    void test08() throws Exception {
        int capacity = 256;
        SeekableByteArrayOutputStream sbaos = new SeekableByteArrayOutputStream(capacity);
        SeekableByteChannel sbc = new Util.SeekableByteChannelForWriting(sbaos) {
            @Override
            protected long getLeftOver() throws IOException {
                return capacity - position();
            }
        };

        sbc.position(99);
        byte[] rb = new byte[] { 100 };
        sbc.write(ByteBuffer.wrap(rb));

        assertEquals(100, sbaos.toByteArray().length);
        assertEquals(100, sbaos.toByteArray()[99]);

        sbc.position(49);
        rb = new byte[] { 50 };
        sbc.write(ByteBuffer.wrap(rb));

        assertEquals(100, sbaos.toByteArray().length);
        assertEquals(50, sbaos.toByteArray()[49]);
    }

    @Test
    void test08_1() throws Exception {
        Path tmp = TestCase.tmp.resolve("test08_1.dat");
        FileOutputStream fos = new FileOutputStream(tmp.toFile());
        SeekableByteChannel sbc = new Util.SeekableByteChannelForWriting(fos) {
            @Override
            protected long getLeftOver() throws IOException {
                return 256 - position();
            }
        };

        sbc.position(99);
        byte[] rb = new byte[] { 100 };
        sbc.write(ByteBuffer.wrap(rb));

        sbc.position(49);
        rb = new byte[] { 50 };
        sbc.write(ByteBuffer.wrap(rb));

        byte[] b = Files.readAllBytes(tmp);

        assertEquals(100, b.length);
        assertEquals(100, b[99]);
        assertEquals(50, b[49]);

        Files.delete(tmp);
    }
}
