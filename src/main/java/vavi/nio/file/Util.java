/*
 * Copyright (c) 2017 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.reflect.Field;
import java.net.URI;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import vavi.io.Seekable;

import static java.lang.System.getLogger;


/**
 * Util.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2017/03/19 umjammer initial version <br>
 */
public interface Util {

    Logger logger = getLogger(Util.class.getName());

    /** to NFC string */
    static String toPathString(Path path) throws IOException {
        return toNormalizedString(path.toRealPath().toString());
    }

    /** to NFC string */
    static String toFilenameString(Path path) throws IOException {
        return toNormalizedString(path.toRealPath().getFileName().toString());
    }

    /** to NFC string */
    static String toNormalizedString(String string) throws IOException {
        return Normalizer.normalize(string, Form.NFC);
    }

    /**
     * @see "ignoreAppleDouble"
     */
    static boolean isAppleDouble(Path path) throws IOException {
//logger.log(Level.TRACE, "path.toRealPath(): " + path.toRealPath());
//logger.log(Level.TRACE, "path.getFileName(): " + path.getFileName());
        return isAppleDouble(path.getFileName().toString());
    }

    /**
     * TODO out source
     *
     * @see "ignoreAppleDouble"
     */
    static boolean isAppleDouble(String filename) {
        return filename.startsWith("._") ||
               filename.equals(".DS_Store") ||
               filename.equals(".localized") ||
               filename.equals(".hidden");
    }

    /**
     * @return nullable
     */
    static <T extends U, U> T getOneOfOptions(Class<T> clazz, Set<? extends U> options) {
        if (options != null && options.stream().anyMatch(clazz::isInstance)) {
            return clazz.cast(options.stream().filter(clazz::isInstance).findFirst().get());
        } else {
            return null;
        }
    }

    /**
     * @see java.nio.file.Files#newDirectoryStream(Path, java.nio.file.DirectoryStream.Filter)
     */
    static DirectoryStream<Path> newDirectoryStream(List<Path> list,
                                                    DirectoryStream.Filter<? super Path> filter) {
        List<Path> filtered = filter != null ? list.stream().filter(p -> {
            try {
                return filter.accept(p);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }).toList() : list;

        return new DirectoryStream<>() {
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

    /**
     * @see java.nio.file.Files#newByteChannel(Path, Set, java.nio.file.attribute.FileAttribute...)
     */
    abstract class SeekableByteChannelForWriting implements SeekableByteChannel {
        OutputStream out;
        protected long written;
        private final WritableByteChannel wbc;

        public SeekableByteChannelForWriting(OutputStream out) throws IOException {
            this.out = out;
            this.wbc = Channels.newChannel(out);
            this.written = getLeftOver();
        }

        /** */
        protected abstract long getLeftOver() throws IOException;

        @Override
        public boolean isOpen() {
            return wbc.isOpen();
        }

        @Override
        public long position() throws IOException {
            if (out instanceof Seekable) {
                // see com.github.fge.filesystem.driver.DoubleCachedFileSystemDriver#downloadEntry
                written = ((Seekable) out).position();
logger.log(Level.DEBUG, "SeekableByteChannelForWriting: get position by vavi.io.Seekable: " + written);
            } else if (wbc instanceof SeekableByteChannel) {
                written = ((SeekableByteChannel) wbc).position();
logger.log(Level.DEBUG, "SeekableByteChannelForWriting: get position by java.nio.channels.SeekableByteChannel: " + written);
            } else {
logger.log(Level.WARNING, "SeekableByteChannelForWriting: get position: " + written + ", " + out.getClass().getName());
            }

            return written;
        }

        @Override
        public SeekableByteChannel position(long pos) throws IOException {
            if (out instanceof Seekable) {
                // see com.github.fge.filesystem.driver.DoubleCachedFileSystemDriver#downloadEntry
logger.log(Level.DEBUG, "SeekableByteChannelForWriting: set position by vavi.io.Seekable: " + pos);
                ((Seekable) out).position(pos);
            } else if (wbc instanceof SeekableByteChannel) {
logger.log(Level.DEBUG, "SeekableByteChannelForWriting: set position by java.nio.channels.SeekableByteChannel: " + pos);
                ((SeekableByteChannel) wbc).position(pos);
            } else {
logger.log(Level.WARNING, "SeekableByteChannelForWriting: set position: " + pos + ", " + out.getClass().getName());
            }

            written = pos;
            return this;
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            throw new NonReadableChannelException();
        }

        @Override
        public SeekableByteChannel truncate(long size) throws IOException {
logger.log(Level.WARNING, "SeekableByteChannelForWriting: truncate: WIP");
logger.log(Level.DEBUG, "SeekableByteChannelForWriting: truncate: " + size + ", " + written + ", " + out.getClass().getName());
            // TODO implement correctly

            if (written > size) {
                written = size;
            }

            return this;
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            int n = wbc.write(src);
logger.log(Level.DEBUG, "SeekableByteChannelForWriting: write: " + n + "/" + written + " -> " + (written + n));
            written += n;
            return n;
        }

        @Override
        public long size() throws IOException {
logger.log(Level.DEBUG, "SeekableByteChannelForWriting: size: " + written);
            return written;
        }

        @Override
        public void close() throws IOException {
logger.log(Level.DEBUG, "SeekableByteChannelForWriting: close");
            wbc.close();
        }
    }

    /**
     * @see java.nio.file.Files#newByteChannel(Path, Set, java.nio.file.attribute.FileAttribute...)
     */
    abstract class SeekableByteChannelForReading implements SeekableByteChannel {
        private long read = 0;
        private final ReadableByteChannel rbc;
        private final long size;
        InputStream in;

        public SeekableByteChannelForReading(InputStream in) throws IOException {
            this.in = in;
            this.rbc = Channels.newChannel(in);
            this.size = getSize();
        }

        /** */
        protected abstract long getSize() throws IOException;

        @Override
        public boolean isOpen() {
            return rbc.isOpen();
        }

        @Override
        public long position() throws IOException {
            if (in instanceof Seekable) {
                // see com.github.fge.filesystem.driver.DoubleCachedFileSystemDriver#downloadEntry
                read = ((Seekable) in).position();
logger.log(Level.DEBUG, "SeekableByteChannelForReading: get position by vavi.io.Seekable: " + read);
            } else if (rbc instanceof SeekableByteChannel) {
                read = ((SeekableByteChannel) rbc).position();
logger.log(Level.DEBUG, "SeekableByteChannelForReading: get position by java.nio.channels.SeekableByteChannel: " + read);
            } else {
logger.log(Level.WARNING, "SeekableByteChannelForReading: get position: non seekable input: " + read + ", " + in.getClass().getName());
            }
            return read;
        }

        @Override
        public SeekableByteChannel position(long pos) throws IOException {
            if (in instanceof Seekable) {
                // see com.github.fge.filesystem.driver.DoubleCachedFileSystemDriver#downloadEntry
logger.log(Level.DEBUG, "SeekableByteChannelForReading: set position by vavi.io.Seekable: " + pos);
                ((Seekable) in).position(pos);
            } else if (rbc instanceof SeekableByteChannel) {
logger.log(Level.DEBUG, "SeekableByteChannelForReading: set position by java.nio.channels.SeekableByteChannel: " + pos);
                ((SeekableByteChannel) rbc).position(pos);
            } else {
logger.log(Level.WARNING, "SeekableByteChannelForReading: set position: non seekable input: " + pos + ", " + in.getClass().getName());
            }

            read = pos;
            return this;
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            int n = rbc.read(dst);
            if (n > 0) {
logger.log(Level.TRACE, "SeekableByteChannelForReading: read: " + n + "/" + read + " -> " + (read + n));
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

    /**
     * Uses for the case that after closing processing is necessary like caching, resource closing etc.
     *
     * @see java.nio.file.Files#newInputStream(Path, OpenOption...)
     */
    abstract class InputStreamForDownloading extends FilterInputStream {
        private final AtomicBoolean closed = new AtomicBoolean();

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
logger.log(Level.DEBUG, "Skip double close of stream %s".formatted(this));
                return;
            }

            if (closeOnCloseInternal) {
                in.close();
            }

            onClosed();
        }

        /** write process after closing */
        protected abstract void onClosed() throws IOException;
    }

    /**
     * Uses for the case that after closing processing is necessary like caching, resource closing etc.
     * TODO limited under 2GB
     *
     * @see java.nio.file.Files#newOutputStream(Path, OpenOption...)
     */
    abstract class OutputStreamForUploading extends FilterOutputStream {
        private final AtomicBoolean closed = new AtomicBoolean();

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
logger.log(Level.DEBUG, "Skip double close of stream %s".formatted(this));
                return;
            }

            if (closeOnCloseInternal) {
                out.close();
            }

            onClosed();
        }

        protected InputStream getInputStream() {
            if (out instanceof ByteArrayOutputStream) {
                // TODO engine
                return new ByteArrayInputStream(((ByteArrayOutputStream) out).toByteArray());
            } else {
                throw new IllegalStateException("out is not ByteArrayOutputStream: " + out.getClass().getName());
            }
        }

        /** write process after closing */
        protected abstract void onClosed() throws IOException;
    }

    /**
     * @param <T> type for the argument of {@link StealingOutputStreamForUploading#onClosed(T)}.
     *
     * @see java.nio.file.Files#newOutputStream(Path, OpenOption...)
     */
    abstract class StealingOutputStreamForUploading<T> extends OutputStreamForUploading {
        // TODO pool
        private final ExecutorService executor = Executors.newSingleThreadExecutor();
        private Future<T> future;
        private final CountDownLatch latch1 = new CountDownLatch(1);
        private final CountDownLatch latch2 = new CountDownLatch(1);
        private final CountDownLatch latch3 = new CountDownLatch(1);

        /** */
        public StealingOutputStreamForUploading() {
            super(null, false);
        }

        /** this method must be called at {@link #upload()} method */
        protected void setOutputStream(OutputStream os) {
            out = os;
            latch1.countDown();
            try { latch2.await(); } catch (InterruptedException e) { throw new IllegalStateException(e); }
        }

        /** must call {@link #setOutputStream(OutputStream)} in this method */
        protected abstract T upload() throws IOException;

        /** set #out */
        private void init() {
            future = executor.submit(() -> {
                try {
                    T newEntry = upload();
                    latch3.countDown();
                    return newEntry;
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
            try { latch1.await(); } catch (InterruptedException e) { throw new IllegalStateException(e); }
        }

        @Override
        public void write(int b) throws IOException {
            try {
                super.write(b);
            } catch (NullPointerException e) {
                init();
                super.write(b);
            }
        }

        /** write process after closing */
        protected abstract void onClosed(T newEntry);

        @Override
        protected void onClosed() throws IOException {
            try {
                latch2.countDown();
                latch3.await();
                out.close();

                onClosed(future.get());

                executor.shutdown();
            } catch (InterruptedException | ExecutionException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /** default buffer size for transfer */
    int BUFFER_SIZE = 4 * 1024 * 1024;

    /**
     * @see #BUFFER_SIZE
     */
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

    /**
     * @see #BUFFER_SIZE
     */
    static void transfer(SeekableByteChannel in, SeekableByteChannel out) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
        while (in.read(buffer) != -1 || buffer.position() > 0) {
            buffer.flip();
            out.write(buffer);
            buffer.compact();
        }
    }

    /**
     * Gets a source path from an input stream.
     * <p>
     * java runtime option
     * <ol>
     *  <ul>{@code --add-opens=java.base/java.io=ALL-UNNAMED}</ul>
     *  <ul>{@code --add-opens=java.base/sun.nio.ch=ALL-UNNAMED}</ul>
     * </ol>
     *
     * @param object input stream
     * @return source object as URI, nullable
     */
    static URI getSource(Object object) {
        Class<?> c = object.getClass();
        try {
            do {
logger.log(Level.DEBUG, "object1: " + c.getName());
                if (object instanceof BufferedInputStream) {
                    Field pathField = FilterInputStream.class.getDeclaredField("in");
                    pathField.setAccessible(true);
                    object = pathField.get(object);
                }
                if (object instanceof java.io.FileInputStream) {
                    Field pathField = object.getClass().getDeclaredField("path");
                    pathField.setAccessible(true);
                    String path = (String) pathField.get(object);
logger.log(Level.DEBUG, "source: java.io.FileInputStream: path : " + path);
                    return path != null ? URI.create(path) : null;
                }
                if (object.getClass().getName().equals("sun.nio.ch.ChannelInputStream")) { // because it's package private
                    Field pathField = object.getClass().getDeclaredField("ch");
                    pathField.setAccessible(true);
                    object = pathField.get(object);
                }
                if (object.getClass().getName().equals("sun.nio.ch.FileChannelImpl")) { // because it's package private
                    Field pathField = object.getClass().getDeclaredField("path");
                    pathField.setAccessible(true);
                    String path = (String) pathField.get(object);
logger.log(Level.DEBUG, "source: sun.nio.ch.FileChannelImpl: path : " + path);
                    return path != null ? URI.create(path) : null;
                }
logger.log(Level.DEBUG, "object2: " + object.getClass().getName());
                c = c.getSuperclass();
            } while (c.getSuperclass() != null);
        } catch (Exception e) {
            logger.log(Logger.Level.WARNING, e.getMessage(), e);
        }
        return null;
    }
}
