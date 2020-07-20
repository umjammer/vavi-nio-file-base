/*
 * Copyright (c) 2017 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


/**
 * Cache.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2017/03/16 umjammer initial version <br>
 */
public abstract class Cache<T> {
    /** <{@link Path}, {@link T}> */
    protected Map<Path, T> entryCache = new ConcurrentHashMap<>(); // TODO refresh

    /** <{@link Path}, {@link List<Path>}> */
    protected Map<Path, List<Path>> folderCache = new ConcurrentHashMap<>(); // TODO refresh

    /** There is metadata or not. */
    public boolean containsFile(Path path) {
        return entryCache.containsKey(path);
    }

    /** */
    public T getFile(Path path) {
        return entryCache.get(path);
    }

    /** */
    public T putFile(Path path, T entry) {
        return entryCache.put(path, entry);
    }

    /** There are children's metadata or not. */
    public boolean containsFolder(Path path) {
        return folderCache.containsKey(path);
    }

    /** Gets children path. */
    public List<Path> getFolder(Path path) {
        return folderCache.get(path);
    }

    /**
     * @return -1 if path does not exist
     */
    public int getChildCount(Path path) {
        return containsFolder(path) ? getFolder(path).size() : -1;
    }

    /** */
    public List<Path> putFolder(Path path, List<Path> children) {
        return folderCache.put(path, children);
    }

    /** */
    public void addEntry(Path path, T entry) {
        entryCache.put(path, entry);
        Path parentPath = path.toAbsolutePath().getParent();
        List<Path> bros = folderCache.get(parentPath);
        if (bros == null) {
            bros = new ArrayList<>();
            folderCache.put(parentPath, bros);
        }
        bros.add(path);
    }

    /** */
    public void removeEntry(Path path) {
        entryCache.remove(path);
        Path parentPath = path.toAbsolutePath().getParent();
        List<Path> bros = folderCache.get(parentPath);
        if (bros != null) {
            bros.remove(path);
        }
    }

    /** for folder */
    public void moveEntry(Path source, Path target, T entry) {
        List<Path> children = getFolder(source);
        if (children != null) {
            folderCache.remove(source);
        }
        removeEntry(source);
        addEntry(target, entry);
        if (children != null) {
            putFolder(target, changeParent(children, target));
//getFolder(target).forEach(System.err::println);
        }
    }

    /** */
    private List<Path> changeParent(List<Path> children, Path parent) {
        return children.stream().map(p -> parent.resolve(p.getFileName())).collect(Collectors.toList());
    }

    /**
     * @throws NoSuchFileException must be thrown when the path is not found.
     */
    public abstract T getEntry(Path path) throws IOException;

    /**
     * @throws NoSuchFileException is thrown when the path is not found.
     */
    public boolean existsEntry(Path path) throws IOException {
        try {
            getEntry(path);
            return true;
        } catch (NoSuchFileException e) {
            return false;
        }
    }
}

/* */
