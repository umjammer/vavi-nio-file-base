/*
 * Copyright (c) 2017 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.stream.Collectors;

import vavi.util.Debug;


/**
 * Util.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2017/03/19 umjammer initial version <br>
 */
public interface Util {

    /** */
    static String toPathString(Path path) throws IOException {
        return Normalizer.normalize(path.toRealPath().toString(), Form.NFC);
    }

    /** */
    static String toFilenameString(Path path) throws IOException {
        return Normalizer.normalize(path.toRealPath().getFileName().toString(), Form.NFC);
    }

    /** @see "ignoreAppleDouble" */
    static boolean isAppleDouble(Path path) throws IOException {
//System.err.println("path.toRealPath(): " + path.toRealPath());
//System.err.println("path.getFileName(): " + path.getFileName());
        String filename = path.getFileName().toString();
        return filename.startsWith("._") ||
               filename.equals(".DS_Store") ||
               filename.equals(".localized") ||
               filename.equals(".hidden");
    }

    /**
     * @return nullable
     */
    static <T extends U, U> T getOneOfOptions(Class<T> clazz, Set<? extends U> options) {
        if (options != null && options.stream().anyMatch(o -> clazz.isInstance(o))) {
            return clazz.cast(options.stream().filter(o -> clazz.isInstance(o)).findFirst().get());
        } else {
            return null;
        }
    }

    /** */
    static DirectoryStream<Path> newDirectoryStream(final List<Path> list,
                                                    final DirectoryStream.Filter<? super Path> filter) {
        List<Path> filtered = filter != null ? list.stream().filter(p -> {
            try {
                return filter.accept(p);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }).collect(Collectors.toList()) : list;

        return new DirectoryStream<Path>() {
            private final AtomicBoolean alreadyOpen = new AtomicBoolean(false);

            @Override
            public Iterator<Path> iterator() {
                // required by the contract
                if (alreadyOpen.getAndSet(true)) {
                    throw new IllegalStateException("already open");
                }
                return filtered.iterator();
            }

            @Override
            public void close() throws IOException {
            }
        };
    }

    /** */
    static abstract class SeekableByteChannelForWriting implements SeekableByteChannel {
        protected long written;
        private WritableByteChannel wbc;

        public SeekableByteChannelForWriting(OutputStream out) throws IOException {
            this.wbc = Channels.newChannel(out);
            this.written = getLeftOver();
        }

        protected abstract long getLeftOver() throws IOException;

        @Override
        public boolean isOpen() {
            return wbc.isOpen();
        }

        @Override
        public long position() throws IOException {
Debug.println(Level.FINE, "writable byte channel: get position: " + written);
            return written;
        }

        @Override
        public SeekableByteChannel position(long pos) throws IOException {
Debug.println(Level.FINE, "writable byte channel: set position: " + pos);
            written = pos;
            return this;
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            throw new NonReadableChannelException();
        }

        @Override
        public SeekableByteChannel truncate(long size) throws IOException {
Debug.println("writable byte channel: truncate: " + size + ", " + written);
            // TODO implement correctly

            if (written > size) {
                written = size;
            }

            return this;
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            int n = wbc.write(src);
Debug.println("writable byte channel: write: " + n + "/" + written + " -> " + (written + n));
            written += n;
            return n;
        }

        @Override
        public long size() throws IOException {
Debug.println(Level.FINE, "writable byte channel: size: " + written);
            return written;
        }

        @Override
        public void close() throws IOException {
Debug.println("writable byte channel: close");
            wbc.close();
        }
    }

    /** */
    static abstract class SeekableByteChannelForReading implements SeekableByteChannel {
        private long read = 0;
        private ReadableByteChannel rbc;
        private long size;

        public SeekableByteChannelForReading(InputStream in) throws IOException {
            this.rbc = Channels.newChannel(in);
            this.size = getSize();
        }

        protected abstract long getSize() throws IOException;

        @Override
        public boolean isOpen() {
            return rbc.isOpen();
        }

        @Override
        public long position() throws IOException {
Debug.println(Level.FINE, "readable byte channel: get position: " + read);
            return read;
        }

        @Override
        public SeekableByteChannel position(long pos) throws IOException {
Debug.println(Level.FINE, "readable byte channel: set position: " + pos);
            read = pos;
            return this;
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            int n = rbc.read(dst);
            if (n > 0) {
Debug.println("readable byte channel: read: " + n + "/" + read + " -> " + (read + n));
                read += n;
            }
            return n;
        }

        @Override
        public SeekableByteChannel truncate(long size) throws IOException {
            throw new NonWritableChannelException();
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            throw new NonWritableChannelException();
        }

        @Override
        public long size() throws IOException {
            return size;
        }

        @Override
        public void close() throws IOException {
            rbc.close();
        }
    }

    /**
     * TODO
     * <ul>
     * <li> StandardOpenOption.WRITE
     * <li> StandardOpenOption.CREATE_NEW
     * <li> StandardOpenOption.CREATE
     * <li> StandardOpenOption.APPEND
     * </ul>
     */
    static boolean isWriting(Set<? extends OpenOption> options) {
        return options.contains(StandardOpenOption.WRITE) ||
                options.contains(StandardOpenOption.CREATE_NEW) ||
                options.contains(StandardOpenOption.CREATE) ||
                options.contains(StandardOpenOption.APPEND);
    }

    /** */
    static abstract class InputStreamForDownloading extends FilterInputStream {
        private AtomicBoolean closed = new AtomicBoolean();

        private boolean closeOnCloseInternal = true;

        public InputStreamForDownloading(InputStream is) {
            super(is);
        }

        public InputStreamForDownloading(InputStream is, boolean closeOnCloseInternal) {
            super(is);
            this.closeOnCloseInternal = closeOnCloseInternal;
        }

        @Override
        public void close() throws IOException {
            if (closed.getAndSet(true)) {
Debug.printf("Skip double close of stream %s", this);
                return;
            }

            if (closeOnCloseInternal) {
                in.close();
            }

            onClosed();
        }

        protected abstract void onClosed() throws IOException;
    }

    /**
     * TODO limited under 2GB
     */
    static abstract class OutputStreamForUploading extends FilterOutputStream {
        private AtomicBoolean closed = new AtomicBoolean();

        private boolean closeOnCloseInternal = true;

        public OutputStreamForUploading() {
            super(new ByteArrayOutputStream());
        }

        public OutputStreamForUploading(OutputStream os) {
            super(os);
        }

        public OutputStreamForUploading(OutputStream os, boolean closeOnCloseInternal) {
            super(os);
            this.closeOnCloseInternal = closeOnCloseInternal;
        }

        @Override
        public void close() throws IOException {
            if (closed.getAndSet(true)) {
Debug.printf("Skip double close of stream %s", this);
                return;
            }

            if (closeOnCloseInternal) {
                out.close();
            }

            onClosed();
        }

        protected InputStream getInputStream() {
            if (ByteArrayOutputStream.class.isInstance(out)) {
                // TODO engine
                return new ByteArrayInputStream(ByteArrayOutputStream.class.cast(out).toByteArray());
            } else {
                throw new IllegalStateException("out is not ByteArrayOutputStream: " + out.getClass().getName());
            }
        }

        protected abstract void onClosed() throws IOException;
    }

    /** */
    static final int BUFFER_SIZE = 4 * 1024 * 1024;

    /** */
    static void transfer(InputStream is, OutputStream os) throws IOException {
        WritableByteChannel wbc = Channels.newChannel(os);
        ReadableByteChannel rbc = Channels.newChannel(is);
        ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
        while (rbc.read(buffer) != -1 || buffer.position() > 0) {
            buffer.flip();
            wbc.write(buffer);
            buffer.compact();
        }
    }

    /** */
    static void transfer(SeekableByteChannel in, SeekableByteChannel out) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
        while (in.read(buffer) != -1 || buffer.position() > 0) {
            buffer.flip();
            out.write(buffer);
            buffer.compact();
        }
    }
}

/* */
