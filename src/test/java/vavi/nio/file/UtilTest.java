/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import vavi.util.Debug;
import vavix.util.Checksum;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * UtilTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-07-20 nsano initial version <br>
 */
public class UtilTest {

    class Uploader {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Path p;
        Uploader(Path p) {
            this.p = p;
        }
        public void upload() throws IOException {
            Files.copy(p, baos);
        }
    }

    @Test
    void test1() throws Exception {
        Path path = Paths.get("src/test/resources/Hello.java");
        OutputStream os = new Util.StealingOutputStreamForUploading<ByteArrayOutputStream>() {

            @Override
            protected ByteArrayOutputStream upload() throws IOException {
                Uploader uploader = new Uploader(null);
                setOutputStream(uploader.baos);
                return uploader.baos;
            }

            @Override
            protected void onClosed(ByteArrayOutputStream baos) {
                try {
                    assertEquals(Checksum.getChecksum(path),
                            Checksum.getChecksum(new ByteArrayInputStream(baos.toByteArray())));
Debug.println("\n" + baos);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        };

        Files.copy(path, os);
        os.close();
    }
}
