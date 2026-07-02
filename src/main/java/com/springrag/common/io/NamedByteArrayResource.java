package com.springrag.common.io;

import org.springframework.core.io.ByteArrayResource;

/**
 * In-memory resource that retains the original file name so content-type detection
 * (e.g. Apache Tika) can use the filename hint. Lets ingestion read an uploaded file
 * straight from memory without staging it to a temp file.
 */
public class NamedByteArrayResource extends ByteArrayResource {

    private final String fileName;

    public NamedByteArrayResource(byte[] byteArray, String fileName) {
        super(byteArray);
        this.fileName = fileName;
    }

    @Override
    public String getFilename() {
        return fileName;
    }
}
