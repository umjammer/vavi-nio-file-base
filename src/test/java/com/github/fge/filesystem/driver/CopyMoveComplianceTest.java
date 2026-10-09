/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package com.github.fge.filesystem.driver;

import java.io.IOException;
import java.net.URI;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Checks {@link Files#copy} and {@link Files#move} semantics (JSR-203)
 * for drivers built on {@link ExtendedFileSystemDriver} / {@link CachedFileSystemDriver}.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-08 nsano initial version <br>
 */
class CopyMoveComplianceTest {

    static final JimfsBackedFileSystemProvider provider = new JimfsBackedFileSystemProvider();

    FileSystem fs;
    Path root;

    static FileSystem newFileSystem() throws IOException {
        URI uri = URI.create(JimfsBackedFileSystemProvider.SCHEME + ":///" + UUID.randomUUID());
        return provider.newFileSystem(uri, Collections.emptyMap());
    }

    @BeforeEach
    void setup() throws IOException {
        fs = newFileSystem();
        root = fs.getPath("/");
    }

    @AfterEach
    void teardown() throws IOException {
        fs.close();
    }

    Path file(String name, String content) throws IOException {
        Path path = root.resolve(name);
        Files.writeString(path, content);
        return path;
    }

    Path dir(String name) throws IOException {
        return Files.createDirectory(root.resolve(name));
    }

    // ---- copy

    @Test
    void copyFile() throws IOException {
        Path src = file("a", "A");
        Path dst = root.resolve("b");
        assertEquals(dst, Files.copy(src, dst));
        assertEquals("A", Files.readString(dst));
        assertEquals("A", Files.readString(src));
    }

    @Test
    void copyFileToOtherDir() throws IOException {
        Path src = file("a", "A");
        Path d = dir("d");
        Path dst = d.resolve("b");
        Files.copy(src, dst);
        assertEquals("A", Files.readString(dst));
        assertTrue(Files.exists(src));
    }

    @Test
    void copyNoSource() {
        assertThrows(NoSuchFileException.class, () -> Files.copy(root.resolve("none"), root.resolve("b")));
    }

    @Test
    void copyNoTargetParent() throws IOException {
        Path src = file("a", "A");
        assertThrows(NoSuchFileException.class, () -> Files.copy(src, root.resolve("none/b")));
    }

    @Test
    void copyTargetExists() throws IOException {
        Path src = file("a", "A");
        Path dst = file("b", "B");
        assertThrows(FileAlreadyExistsException.class, () -> Files.copy(src, dst));
        assertEquals("B", Files.readString(dst));
    }

    @Test
    void copyReplaceFile() throws IOException {
        Path src = file("a", "A");
        Path dst = file("b", "B");
        Files.copy(src, dst, REPLACE_EXISTING);
        assertEquals("A", Files.readString(dst));
        assertEquals("A", Files.readString(src));
    }

    @Test
    void copyReplaceEmptyDir() throws IOException {
        Path src = file("a", "A");
        Path dst = dir("d");
        Files.copy(src, dst, REPLACE_EXISTING);
        assertTrue(Files.isRegularFile(dst));
        assertEquals("A", Files.readString(dst));
    }

    @Test
    void copyReplaceNonEmptyDir() throws IOException {
        Path src = file("a", "A");
        Path dst = dir("d");
        Files.writeString(dst.resolve("x"), "X");
        assertThrows(DirectoryNotEmptyException.class, () -> Files.copy(src, dst, REPLACE_EXISTING));
        assertEquals("X", Files.readString(dst.resolve("x")));
    }

    @Test
    void copyDirectoryCreatesEmptyDirectory() throws IOException {
        Path src = dir("d");
        Files.writeString(src.resolve("x"), "X");
        Path dst = root.resolve("e");
        assertEquals(dst, Files.copy(src, dst));
        assertTrue(Files.isDirectory(dst));
        assertFalse(Files.exists(dst.resolve("x")));
        assertTrue(Files.exists(src.resolve("x")));
    }

    @Test
    void copySameFile() throws IOException {
        Path src = file("a", "A");
        Files.copy(src, src);
        Files.copy(src, src, REPLACE_EXISTING);
        assertEquals("A", Files.readString(src));
    }

    @Test
    void copyAtomicMove() throws IOException {
        Path src = file("a", "A");
        assertThrows(UnsupportedOperationException.class, () -> Files.copy(src, root.resolve("b"), ATOMIC_MOVE));
    }

    @Test
    void copyBetweenFileSystems() throws IOException {
        Path src = file("a", "A");
        try (FileSystem fs2 = newFileSystem()) {
            Path dst = fs2.getPath("/b");
            Files.copy(src, dst);
            assertEquals("A", Files.readString(dst));
            assertTrue(Files.exists(src));

            Path dst2 = Files.writeString(fs2.getPath("/c"), "C");
            assertThrows(FileAlreadyExistsException.class, () -> Files.copy(src, dst2));
            Files.copy(src, dst2, REPLACE_EXISTING);
            assertEquals("A", Files.readString(dst2));
        }
    }

    // ---- move

    @Test
    void moveRename() throws IOException {
        Path src = file("a", "A");
        Path dst = root.resolve("b");
        assertEquals(dst, Files.move(src, dst));
        assertFalse(Files.exists(src));
        assertEquals("A", Files.readString(dst));
    }

    @Test
    void moveToOtherDir() throws IOException {
        Path src = file("a", "A");
        Path dst = dir("d").resolve("a");
        Files.move(src, dst);
        assertFalse(Files.exists(src));
        assertEquals("A", Files.readString(dst));
    }

    @Test
    void moveNoSource() {
        assertThrows(NoSuchFileException.class, () -> Files.move(root.resolve("none"), root.resolve("b")));
    }

    @Test
    void moveReplaceNoSource() throws IOException {
        Path dst = file("b", "B");
        assertThrows(NoSuchFileException.class, () -> Files.move(root.resolve("none"), dst, REPLACE_EXISTING));
        assertEquals("B", Files.readString(dst));
    }

    @Test
    void moveTargetFileExists() throws IOException {
        Path src = file("a", "A");
        Path dst = file("b", "B");
        assertThrows(FileAlreadyExistsException.class, () -> Files.move(src, dst));
        assertEquals("A", Files.readString(src));
        assertEquals("B", Files.readString(dst));
    }

    @Test
    void moveTargetDirExists() throws IOException {
        Path src = file("a", "A");
        Path dst = dir("d");
        // JSR-203 doesn't "move into" an existing directory
        assertThrows(FileAlreadyExistsException.class, () -> Files.move(src, dst));
        assertTrue(Files.exists(src));
        assertFalse(Files.exists(dst.resolve("a")));
    }

    @Test
    void moveReplaceFile() throws IOException {
        Path src = file("a", "A");
        Path dst = file("b", "B");
        Files.move(src, dst, REPLACE_EXISTING);
        assertFalse(Files.exists(src));
        assertEquals("A", Files.readString(dst));
    }

    @Test
    void moveReplaceFileInOtherDir() throws IOException {
        Path src = file("a", "A");
        Path d = dir("d");
        Path dst = Files.writeString(d.resolve("b"), "B");
        Files.move(src, dst, REPLACE_EXISTING);
        assertFalse(Files.exists(src));
        assertEquals("A", Files.readString(dst));
    }

    @Test
    void moveReplaceEmptyDir() throws IOException {
        Path src = file("a", "A");
        Path dst = dir("d");
        Files.move(src, dst, REPLACE_EXISTING);
        assertFalse(Files.exists(src));
        assertTrue(Files.isRegularFile(dst));
        assertEquals("A", Files.readString(dst));
    }

    @Test
    void moveReplaceNonEmptyDir() throws IOException {
        Path src = file("a", "A");
        Path dst = dir("d");
        Files.writeString(dst.resolve("x"), "X");
        assertThrows(DirectoryNotEmptyException.class, () -> Files.move(src, dst, REPLACE_EXISTING));
        assertTrue(Files.exists(src));
        assertTrue(Files.exists(dst.resolve("x")));
    }

    @Test
    void moveRenameNonEmptyDir() throws IOException {
        Path src = dir("d");
        Files.writeString(src.resolve("x"), "X");
        // fill the cache
        assertTrue(Files.exists(src.resolve("x")));
        Path dst = root.resolve("e");
        Files.move(src, dst);
        assertFalse(Files.exists(src));
        assertFalse(Files.exists(src.resolve("x")));
        assertEquals("X", Files.readString(dst.resolve("x")));
        try (Stream<Path> s = Files.list(dst)) {
            assertEquals(List.of(dst.resolve("x")), s.toList());
        }
    }

    @Test
    void moveDirToOtherDir() throws IOException {
        Path src = dir("d");
        Files.writeString(src.resolve("x"), "X");
        Path dst = dir("e").resolve("d");
        Files.move(src, dst);
        assertFalse(Files.exists(src));
        assertEquals("X", Files.readString(dst.resolve("x")));
    }

    @Test
    void moveDirNoStaleChildren() throws IOException {
        Path src = dir("d");
        Path sub = Files.createDirectory(src.resolve("s"));
        Files.writeString(sub.resolve("x"), "X");
        // fill the cache
        assertTrue(Files.exists(sub.resolve("x")));
        Files.move(src, dir("e").resolve("d"));
        assertFalse(Files.exists(src.resolve("s")));
        assertFalse(Files.exists(sub.resolve("x")));
        Files.createDirectory(src);
        assertFalse(Files.exists(src.resolve("s")));
        assertFalse(Files.exists(sub.resolve("x")));
        assertEquals("X", Files.readString(root.resolve("e/d/s/x")));
    }

    @Test
    void moveDirIntoItself() throws IOException {
        Path src = dir("d");
        Path dst = src.resolve("e");
        assertThrows(FileSystemException.class, () -> Files.move(src, dst));
        assertTrue(Files.isDirectory(src));
    }

    @Test
    void moveSameFile() throws IOException {
        Path src = file("a", "A");
        assertEquals(src, Files.move(src, src));
        Files.move(src, src, REPLACE_EXISTING);
        assertEquals("A", Files.readString(src));
    }

    @Test
    void moveAtomicMove() throws IOException {
        Path src = file("a", "A");
        assertThrows(AtomicMoveNotSupportedException.class, () -> Files.move(src, root.resolve("b"), ATOMIC_MOVE));
        assertTrue(Files.exists(src));
    }

    @Test
    void moveBetweenFileSystems() throws IOException {
        Path src = file("a", "A");
        try (FileSystem fs2 = newFileSystem()) {
            Path dst = fs2.getPath("/b");
            Files.move(src, dst);
            assertEquals("A", Files.readString(dst));
            assertFalse(Files.exists(src));

            Path src2 = file("c", "C");
            assertThrows(FileAlreadyExistsException.class, () -> Files.move(src2, dst));
            Files.move(src2, dst, REPLACE_EXISTING);
            assertEquals("C", Files.readString(dst));
            assertFalse(Files.exists(src2));
        }
    }
}
