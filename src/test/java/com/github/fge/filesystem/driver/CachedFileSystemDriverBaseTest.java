/*
 * Copyright (c) 2021 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package com.github.fge.filesystem.driver;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import vavi.util.Debug;

class CachedFileSystemDriverBaseTest {

    @Test
    void test() throws Exception {
        Path path = Paths.get("/aa/bb/cc/dd");

        for (int i = 0; i < path.getNameCount(); i++) {
            Path name = path.getName(i);
            Path sub = path.subpath(0, i + 1);
            Path parent = sub.getParent() != null ? sub.getParent() : path.getFileSystem().getPath("/");
            List<Path> bros = getDirectoryEntries(parent);
            Optional<Path> found = bros.stream().filter(p -> p.getFileName().equals(name)).findFirst();
System.err.println("name: " + name + ", sub: " + sub + ", parent: " + parent + ", found: " + found + ", list: " + bros);
            if (found.isPresent()) {
                continue;
            } else {
                Debug.println("not found 2");
                return;
            }
        }
        Debug.println("found: " + path);
    }

    List<Path> getDirectoryEntries(Path parent) {
        String p = parent.toString().charAt(0) != '/' ? "/" + parent.toString() : "/";
System.err.println("get dir list: " + p);
        switch (p) {
        case "/": return Arrays.asList("a1", "a2", "aa", "a3").stream().map(Paths::get).collect(Collectors.toList());
        case "/aa": return Arrays.asList("b1", "bb").stream().map(Paths::get).collect(Collectors.toList());
        case "/aa/bb": return Arrays.asList("c1", "c2", "c3", "cc").stream().map(Paths::get).collect(Collectors.toList());
        case "/aa/bb/cc": return Arrays.asList("d1", "d2", "d3", "d4", "d0", "d5").stream().map(Paths::get).collect(Collectors.toList());
        default: return Collections.emptyList();
        }
    }
}

/* */