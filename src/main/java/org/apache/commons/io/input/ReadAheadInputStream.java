package org.apache.commons.io.input;

import static java.lang.System.arraycopy;
import static org.apache.commons.io.IOUtils.EOF;

import java.io.IOException;
import java.io.InputStream;

/**
 * InputStream proxy with a buffer that uses a thread to read
 * the underlying stream up to the available space
 * of the defined buffer.
 * The proxied input stream is closed when the {@link #close()} method is
 * called on this proxy. It is configurable whether the internal input stream
 * stream will also closed.
 *
 * @version $Id$
 * @see java.io.BufferedInputStream
 */
public class ReadAheadInputStream extends InputStream {

    public static final int DEFAULT_BUFFER_SIZE = 8192;

    private final InputStream innerStream;
    private final byte[] buffer;
    private final Thread fillingThread;
    private final int size;
    private final boolean closeInternal;
    private int bytesReadFromInnerStream;
    private int numberOfBytesToReadFromInnerStream;
    private boolean threadIsAlive;


    public ReadAheadInputStream(InputStream in) {
        this(in, true);
    }

    public ReadAheadInputStream(InputStream in, boolean closeInternal) {
        this(in, DEFAULT_BUFFER_SIZE, closeInternal);
    }

    public ReadAheadInputStream(final InputStream in, int size) {
        this(in, size, true);
    }

    public ReadAheadInputStream(final InputStream in, int size, boolean closeInternal) {
        this.innerStream = in;
        if (in == null) {
            throw new IllegalArgumentException("inner stream argument cannot be null");
        }
        this.size = size;
        if (size <= 0) {
            throw new IllegalArgumentException("size argument must be positive ");
        }
        this.buffer = new byte[size];
        this.closeInternal = closeInternal;
        this.numberOfBytesToReadFromInnerStream = size;
        this.bytesReadFromInnerStream = 0;

        readFromInnerStream();
        this.fillingThread = createFillingThread();
        this.fillingThread.start();

    }

    private Thread createFillingThread() {
        threadIsAlive = true;
        return new Thread(new Runnable() {
            @Override
            public void run() {
                while (threadIsAlive) {
                    readFromInnerStream();
                }
            }
        });
    }

    private synchronized void readFromInnerStream() {
        try {
            waitIfCantReadMoreData();
            int numRead = innerStream.read(buffer, 0 ,numberOfBytesToReadFromInnerStream);
            if (numRead >= 0) {
                bytesReadFromInnerStream += numRead;
                numberOfBytesToReadFromInnerStream = size - bytesReadFromInnerStream;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized int read() throws IOException {
        if (emptyBuffer()) {
            return EOF;
        }

        int result = buffer[0];
        arraycopy(buffer, 1, buffer, 0, buffer.length -1);
        numberOfBytesToReadFromInnerStream++;
        signalItsPossibleToReadMoreData();

        bytesReadFromInnerStream--;
        readFromInnerStream();
        return result;
    }

    private boolean emptyBuffer() {
        return bytesReadFromInnerStream == 0;
    }

    @Override
    public  synchronized int read(byte b[], int off, int len) throws IOException {
        if (len - off > bytesReadFromInnerStream) {
            len = off + bytesReadFromInnerStream;
        }
        arraycopy(buffer, 0 , b, off, len - off);
        arraycopy(buffer, len - off , buffer , 0, size - (len - off));
        bytesReadFromInnerStream -= len - off;
        numberOfBytesToReadFromInnerStream = len - off;
        signalItsPossibleToReadMoreData();
        readFromInnerStream();
        return len;
    }

    @Override
    public void close() {
        threadIsAlive = false;
        try {
            fillingThread.wait();
            if (closeInternal) {
                innerStream.close();
            }
        } catch (InterruptedException  e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void signalItsPossibleToReadMoreData() {
       notify();
    }

    private void waitIfCantReadMoreData() {
        try {
            if (filledBuffer()) {
                wait();
            }
            notify();
        } catch (InterruptedException e) {
        }
    }

    private boolean filledBuffer() {
        return numberOfBytesToReadFromInnerStream == 0;
    }


}
