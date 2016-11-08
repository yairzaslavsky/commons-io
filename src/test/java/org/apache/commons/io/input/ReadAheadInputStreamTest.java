package org.apache.commons.io.input;

import static org.apache.commons.io.IOUtils.EOF;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.Test;

/**
 * JUnit Test Case for {@link ReadAheadInputStream}.
 */
public class ReadAheadInputStreamTest {


    private static final String EXPECTED = "expected";

    private ByteArrayInputStream innerStream;

    @Before
    public void setup() {
        innerStream = new ByteArrayInputStream(EXPECTED.getBytes(Charset.defaultCharset()));

    }

    @Test
    public void testReadSingleChars() {
        ReadAheadInputStream readAheadInputStream = new ReadAheadInputStream(innerStream, 1);
        try {
            for (int counter = 0; counter < EXPECTED.length(); counter++) {
                int r = readAheadInputStream.read();
                assertEquals(EXPECTED.charAt(counter), (char)r);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testReadToArrayWhereInternalBufferIsSmallerThanArray() {

        ReadAheadInputStream readAheadInputStream = new ReadAheadInputStream(innerStream, 1);
        try {
            byte[] buff = new byte[2];
            for (int counter = 0; counter < EXPECTED.length(); counter++) {
                int r = readAheadInputStream.read(buff);
                assertEquals(r, 1);
                assertEquals(EXPECTED.charAt(counter), (char)buff[0]);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testReadToArrayWhereInternalBufferIsBiggerThanArray() {

        ReadAheadInputStream readAheadInputStream = new ReadAheadInputStream(innerStream, 10);
        try {
            for (int counter = 0; counter < EXPECTED.length() / 2; counter++) {
                byte[] buff = new byte[2];
                int r = readAheadInputStream.read(buff);
                assertEquals(r, 2);
                assertEquals(EXPECTED.charAt(counter * 2),(char)buff[0]);
                assertEquals(EXPECTED.charAt(counter * 2 + 1),(char)buff[1]);

            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testReadUntilEnfOfInternalBuff() {
        boolean foundEOF = false;
        ReadAheadInputStream readAheadInputStream = new ReadAheadInputStream(innerStream, 10);
        try {
            for (int counter = 0; counter < EXPECTED.length() + 1 ; counter++) {
                int r = readAheadInputStream.read();
                if (r == EOF) {
                    foundEOF = true;
                }
            }
            assertTrue(foundEOF);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}