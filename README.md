[![Release](https://jitpack.io/v/umjammer/vavi-nio-file-base.svg)](https://jitpack.io/#umjammer/vavi-nio-file-base)
[![Java CI](https://github.com/umjammer/vavi-nio-file-base/actions/workflows/maven.yml/badge.svg)](https://github.com/umjammer/vavi-nio-file-base/actions)
[![CodeQL](https://github.com/umjammer/vavi-nio-file-base/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/umjammer/vavi-nio-file-base/actions/workflows/codeql-analysis.yml)
![Java](https://img.shields.io/badge/Java-17-b07219)
[![Parent](https://img.shields.io/badge/Parent-vavi--apps--fuse-pink)](https://github.com/umjammer/vavi-apps-fuse)

## vavi-nio-file-base

Java nio file (JSR-203) basics based on [java7-fs-base](https://github.com/fge/java7-fs-base).

### vavi-nio-file

* cache for file system
* utilities
    * channels for filesystems
    * input/output streams for upload/download
    * output engine input stream ✭
* base test case

## Install

https://jitpack.io/#umjammer/vavi-nio-file-base

## Usage

## TODO

 * ~~rename project to vavi-nio-file-base~~
 * JSR-107 Cache Specification
    * https://github.com/jsr107/jsr107spec
    * https://github.com/ben-manes/caffeine
 * ~~merge into java7-fs-base~~

---

# Original

This project is licensed under GPLv2.0, LGPLv3 and ASL 2.0. See file LICENSE for
more details.

## What this is

This is a package designed to ease the creation of custom Java 7
[`FileSystem`](https://docs.oracle.com/javase/7/docs/api/java/nio/file/FileSystem.html)s.

## So, what is a "Java 7 `FileSystem`" anyway?

More generally, we are talking here about the [`java.nio.file`
API](http://docs.oracle.com/javase/8/docs/api/java/nio/file/package-frame.html) which made its
appearance in Java 7.

A `FileSystem` can be, in theory, written for anything which has a filesystem-like structure or for
which you can emulate one; the JRE of course provides it for the native filesystems of the platform
you are running the JRE on, but it also provides a `FileSystem` view for ZIP files -- yes, that's
right, you can view a ZIP file (therefore also jars) as `FileSystem`s and copy from/to them, etc.

Other examples of existing, third party filesystems are:

* [a `FileSystem` over FTP](https://github.com/fge/java7-fs-ftp) (by yours truly);
* [an in-memory `FileSystem`](https://github.com/google/jimfs) (by Google).
* [a `FileSystem` over DropBox](https://github.com/fge/java7-fs-dropbox) (by yours truly; based on this package);


## So, why this package?

Well, the API is quite a beast; if you want to program a `FileSystem` from scratch, you're setting
yourself a very, very big task. Just look [how many methods are defined by
`Path`](http://docs.oracle.com/javase/8/docs/api/java/nio/file/Path.html), for instance -- and
that's only the "user level" view of the API.

This package therefore brings facilities to allow you to develop custom file systems in a much
easier way; see [the wiki](https://github.com/fge/java7-fs-base/wiki) for more information.
