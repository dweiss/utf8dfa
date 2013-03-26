package com.dawidweiss.utf8dfa;

import com.google.common.io.ByteStreams;
import com.google.common.io.Resources;

public class DataPolishBooks
{
    public final static byte [] DATA;

    static
    {
        try {
            DATA = ByteStreams.toByteArray(
                Resources.newInputStreamSupplier(
                    Resources.getResource(DataPolishBooks.class, "/books-polish.txt")));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
