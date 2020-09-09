/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import vavix.util.Checksum;

import static com.rainerhahnekamp.sneakythrow.Sneaky.sneaked;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Base.
 * <p>
 * if you want to write sub class test of this base test,
 * you should include this dependency in your pom.xml
 * <pre>
 * &lt;dependency&gt;
 *   &lt;groupId&gt;com.rainerhahnekamp&lt;/groupId&gt;
 *   &lt;artifactId&gt;sneakythrow&lt;/artifactId&gt;
 *   &lt;version&gt;1.2.0&lt;/version&gt;
 *   &lt;scope&gt;test&lt;/scope&gt;
 * &lt;/dependency&gt;
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/xx umjammer initial version <br>
 */
public interface Base {

    /**
     * prepare src/test/resources/Hello.java
     */
    static void testAll(FileSystem fileSystem) throws Exception {

        Path src = Paths.get("src/test/resources" , "Hello.java");
        Path dir = fileSystem.getPath("/").resolve("VAVIFUSE_FS_TEST");

System.out.println("$ [list]: " + dir.getParent());
Files.list(dir.getParent()).forEach(System.out::println);
        long count = Files.list(dir.getParent()).count();

        if (Files.exists(dir)) {
            removeTree(dir);
            count--;
        }

System.out.println("$ [createDirectory]: " + dir);
        dir = Files.createDirectory(dir);
        count++;
Thread.sleep(300); 
System.out.println("$ [list]: " + dir.getParent());
Files.list(dir.getParent()).forEach(System.out::println);
        assertEquals(count, Files.list(dir.getParent()).count());

        Path src2 = dir.resolve(src.getFileName().toString());
System.out.println("$ [copy (upload)]: " + src + " " + src2);
        src2 = Files.copy(src, src2);
Thread.sleep(300);
System.out.println("$ [list]: " + dir);
Files.list(dir).forEach(System.out::println);
        assertEquals(1, Files.list(dir).count());
        assertEquals(Files.size(src), Files.size(src2));

        Path src3 = dir.resolve(src2.getFileName().toString() + "_コピー");
System.out.println("$ [copy (internal)]: " + src2 + " " + src3);
        src3 = Files.copy(src2, src3); // SPEC target should be file
Thread.sleep(300);
System.out.println("$ [list]: " + dir);
Files.list(dir).forEach(System.out::println);
        assertEquals(2, Files.list(dir).count());

        Path src3_2 = dir.resolve(src2.getFileName().toString() + "_コピー2");
System.out.println("$ [copy (internal)]: " + src2 + " " + src3_2);
        src3_2 = Files.copy(src2, src3_2);
Thread.sleep(300);
System.out.println("$ [list]: " + dir);
Files.list(dir).forEach(System.out::println);
        assertEquals(3, Files.list(dir).count());

        Path src4 = dir.resolve(src2.getFileName().toString() + "_R");
System.out.println("$ [rename (internal)]:" + src3 + " " + src4);
        src4 = Files.move(src3, src4);
Thread.sleep(300);
System.out.println("$ [list]: " + dir);
Files.list(dir).forEach(System.out::println);
        assertEquals(3, Files.list(dir).count());

System.out.println("$ [createDirectory]: " + dir.resolve("SUB_DIR"));
        Path dir2 = Files.createDirectory(dir.resolve("SUB_DIR"));
Thread.sleep(300);
System.out.println("$ [list]: " + dir);
Files.list(dir).forEach(System.out::println);
        assertEquals(4, Files.list(dir).count());

        Path src5 = dir2.resolve(src4.getFileName());
System.out.println("$ [move (internal)]:" + src4 + " " + src5);
        src5 = Files.move(src4, src5); // SPEC: move returns target
Thread.sleep(1000);
System.out.println("$ [list]: " + dir2);
Files.list(dir2).forEach(System.out::println);
        assertEquals(1, Files.list(dir2).count());

        Path src6 = dir2.resolve("World.java");
System.out.println("$ [move (internal)]:" + src3_2 + " " + src6);
        src6 = Files.move(src3_2, src6);
Thread.sleep(1000);
System.out.println("$ [list]: " + dir2);
Files.list(dir2).forEach(System.out::println);
        assertEquals(2, Files.list(dir2).count());

        Path tmp = Paths.get("tmp");
        if (!Files.exists(tmp)) {
System.out.println("$ [*mkdir]: " + tmp);
            Files.createDirectory(tmp);
        }

        Path dst2 = Paths.get("tmp", "Hello.java");
        if (Files.exists(dst2)) {
System.out.println("$ [*rm]: " + dst2);
            Files.delete(dst2);
        }
System.out.println("$ [copy (download)]: " + src6 + " " + dst2);
        dst2 = Files.copy(src6, dst2);
Thread.sleep(300);
System.out.println("$ [list]: " + dst2.getParent());
Files.list(dst2.getParent()).forEach(System.out::println);
        assertTrue(Files.exists(dst2));
        assertEquals(Files.size(dst2), Files.size(src6));

System.out.println("$ [delete file]: " + src2);
        Files.delete(src2);
Thread.sleep(300);
System.out.println("$ [list]: " + dir);
Files.list(dir).forEach(System.out::println);
        assertEquals(1, Files.list(dir).count());

System.out.println("$ [delete file]:" + src5);
        Files.delete(src5);
Thread.sleep(300);
System.out.println("$ [list]: " + dir2);
Files.list(dir2).forEach(System.out::println);
        assertEquals(1, Files.list(dir2).count());

System.out.println("$ [delete file]:" + src6);
        Files.delete(src6);
Thread.sleep(300);
System.out.println("$ [list]: " + dir2);
Files.list(dir2).forEach(System.out::println);
        assertEquals(0, Files.list(dir2).count());

System.out.println("$ [delete directory]: " + dir2);
        Files.delete(dir2);
Thread.sleep(300);
System.out.println("$ [list]: " + dir);
Files.list(dir).forEach(System.out::println);
        assertEquals(0, Files.list(dir).count());

System.out.println("$ [delete directory]: " + dir);
        Files.delete(dir);
Thread.sleep(600);
System.out.println("$ [list]: " + dir.getParent());
Files.list(dir.getParent()).forEach(System.out::println);
        assertEquals(count - 1, Files.list(dir.getParent()).count());

System.out.println("$ [*rm]: " + dst2);
        Files.delete(dst2);
        assertFalse(Files.exists(dst2));
    }

    /** */
    static void testLargeFile(FileSystem fs, Class<? extends OpenOption> optionClass) throws Exception {
        Path tmpDir = Paths.get("tmp");
        if (!Files.exists(tmpDir)) {
            Files.createDirectories(tmpDir);
        }
        Path source = Files.createTempFile(tmpDir, "vavifuse-1-", ".tmp");
        byte[] bytes = new byte[5 * 1024 * 1024 + 12345];
        Random random = new Random(System.currentTimeMillis());
        random.nextBytes(bytes);
        Files.write(source, bytes);

        Path dir = fs.getPath("/").resolve("VAVIFUSE_FS_TEST_L");
        Path target = dir.resolve(source.getFileName().toString());
        if (Files.exists(dir)) {
            Files.list(dir).forEach(p -> {
                try {
System.out.println("rm " + p);
                    Files.delete(p);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
System.out.println("rmdir " + dir);
            Files.delete(dir);
        }

System.out.println("mkdir " + dir);
        Files.createDirectory(dir);

        OpenOption option = optionClass != null ? optionClass.getDeclaredConstructor(Path.class).newInstance(source) : null;

//System.out.println("cp " + source + " " + target);
//      Files.copy(source, target, option); // our option isn't accepted by jdk

System.out.println("cp(2) " + source + " " + target);
        InputStream in = Files.newInputStream(source, StandardOpenOption.READ);
        OutputStream out;
        out = option != null ? Files.newOutputStream(target, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW, option)
                             : Files.newOutputStream(target, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
        Util.transfer(in, out);
        in.close();
        out.close();
        assertTrue(Files.exists(target));
        assertEquals(Files.size(source), Files.size(target));
        assertEquals(Checksum.getChecksum(source), Checksum.getChecksum(target));

        Path source2 = source.getParent().resolve(source.getFileName().toString().replace("vavifuse-1-", "vavifuse-2-"));

System.out.println("cp(3) " + target + " " + source2);
        SeekableByteChannel inChannel = Files.newByteChannel(target, StandardOpenOption.READ);
        SeekableByteChannel outChannel = Files.newByteChannel(source2, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
        Util.transfer(inChannel, outChannel);
        inChannel.close();
        outChannel.close();
        assertTrue(Files.exists(target));
        assertEquals(Files.size(source2), Files.size(target));
        assertEquals(Checksum.getChecksum(source2), Checksum.getChecksum(target));

System.out.println("rm " + source2);
        Files.delete(source2);
System.out.println("rm " + target);
        Files.delete(target);

System.out.println("cp(3) " + source + " " + target);
        inChannel = Files.newByteChannel(source, StandardOpenOption.READ);
        outChannel = option != null ? Files.newByteChannel(target, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW, option)
                                    : Files.newByteChannel(target, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
        Util.transfer(inChannel, outChannel);
        inChannel.close();
        outChannel.close();
        assertTrue(Files.exists(target));
        assertEquals(Files.size(source), Files.size(target));
        assertEquals(Checksum.getChecksum(source), Checksum.getChecksum(target));

        removeTree(dir);

        if (Files.exists(source)) {
System.out.println("rm " + source);
            Files.delete(source);
        }
    }

    /**
     * prepare src/test/resources/Hello.java
     */
    static void testMoveFolder(FileSystem fileSystem) throws Exception {

        Path src = Paths.get("src/test/resources" , "Hello.java");
        Path dir = fileSystem.getPath("/").resolve("VAVIFUSE_FS_TEST_F");

        if (Files.exists(dir)) {
            removeTree(dir);
        }

        Path dir1 = dir.resolve("work1");
System.out.println("$ [createDirectory]: " + dir1);
        dir1 = Files.createDirectories(dir1);

        Path target = dir1.resolve(src.getFileName().toString());
System.out.println("$ [copy (upload)]: " + src + " " + target);
        target = Files.copy(src, target);

        Path target2 = target.resolveSibling(target.getFileName().toString() + "_コピー");
System.out.println("$ [copy (internal)]: " + target + " " + target2);
        target2 = Files.copy(target, target2);

System.out.println("$ [list]: " + dir1);
Files.list(dir1).forEach(System.out::println);
        assertEquals(2, Files.list(dir1).count());

        Path dir2 = dir.resolve("work2");
System.out.println("$ [createDirectory]: " + dir2);
        dir2 = Files.createDirectories(dir2);

        Path dir3 = dir2.resolve(dir1.getFileName());
System.out.println("$ [move (folder, internal)]:" + dir1 + " " + dir3);
        dir3 = Files.move(dir1, dir3); // SPEC: move returns target

System.out.println("$ [list]: " + dir);
Files.list(dir).forEach(System.out::println);
        assertEquals(1, Files.list(dir).count());

System.out.println("$ [list]: " + dir2);
Files.list(dir2).forEach(System.out::println);
        assertEquals(1, Files.list(dir2).count());

System.out.println("$ [list]: " + dir3);
Files.list(dir3).forEach(System.out::println);
        assertEquals(2, Files.list(dir3).count());

        removeTree(dir);
    }

    /** */
    static void removeTree(Path dir) throws Exception {
        List<Path> files = new ArrayList<>();
        List<Path> dirs = new ArrayList<>();
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                files.add(file);
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                dirs.add(dir);
                return FileVisitResult.CONTINUE;
            }
        });
        files.forEach(sneaked(p -> {
System.out.println("$ [*delete file]: " + p);
            Files.delete(p);
Thread.sleep(300);
        }));
        dirs.stream().filter(p -> !p.equals(dir)).forEach(sneaked(p -> {
System.out.println("$ [*delete dir]: " + p);
            Files.delete(p);
Thread.sleep(300);
        }));
Thread.sleep(1000);
System.out.println("$ [delete directory]: " + dir);
        Files.delete(dir);
Thread.sleep(300);
    }

    /**
     * prepare src/test/resources/Hello.java
     */
    static void testDescription(FileSystem fileSystem) throws Exception {

        Path src = Paths.get("src/test/resources" , "Hello.java");
        Path dir = fileSystem.getPath("/").resolve("VAVIFUSE_FS_TEST_D");

        try {
            if (Files.exists(dir)) {
                removeTree(dir);
            }
System.out.println("$ [createDirectory]: " + dir);
            dir = Files.createDirectories(dir);

            Path target = dir.resolve(src.getFileName().toString());
System.out.println("$ [copy (upload)]: " + src + " " + target);
            target = Files.copy(src, target);

            Object o = Files.getAttribute(target, "user:description");
System.out.println("description: " + new String((byte[]) o));

            String description = "説明 ★";
            Files.setAttribute(target, "user:description", description.getBytes());
System.out.println("description write: ");

            o = Files.getAttribute(target, "user:description");
System.out.println("description: " + new String((byte[]) o));
            assertEquals(description, new String((byte[]) o));
        } finally {
            removeTree(dir);
        }
    }
}

/* */
