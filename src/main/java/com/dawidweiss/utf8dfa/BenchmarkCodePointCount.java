package com.dawidweiss.utf8dfa;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.Locale;
import java.util.Random;

import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.UnicodeUtil;

import com.carrotsearch.randomizedtesting.generators.RandomStrings;
import com.google.caliper.Param;
import com.google.caliper.Runner;
import com.google.caliper.SimpleBenchmark;
import com.google.common.base.Charsets;

/**
 * Benchmark unicode codepoint counting.
 */
public class BenchmarkCodePointCount extends SimpleBenchmark
{
    public enum Implementation
    {
        LUCENE, 
        LUCENE_MOD1,
        JAVA,
        NOLOOKUP_IF,
        NO_COUNT
    }

    public enum DataType
    {
        UNICODE,
        ASCII
    }

    @Param
    public DataType dataType;

    @Param
    public Implementation implementation;

    private static byte [] DATA_UNICODE = 
        RandomStrings.randomRealisticUnicodeOfLength(
            new Random(0xdeadbeef), 1000000 * 100).getBytes(Charsets.UTF_8);

    private static byte [] DATA_ASCII = 
        RandomStrings.randomAsciiOfLength(
            new Random(0xdeadbeef), 1000000 * 100).getBytes(Charsets.UTF_8);

    @SuppressWarnings("unused")
    private volatile long guard;

    private byte [] data;
    
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        
        switch (dataType) {
            case UNICODE: data = DATA_UNICODE; break;
            case ASCII:   data = DATA_ASCII;   break;
        }
    }

    public long timeCodePointCounting(int reps) throws Exception
    {
        final Implementation impl = implementation;
        switch (impl)
        {
            case LUCENE:
                return guard = countLucene(reps, data);

            case JAVA:
                return guard = countJava(reps, data);
                
            case NOLOOKUP_IF:
                return guard = noLookupIf(reps, data);
                
            case LUCENE_MOD1:
                return guard = countLuceneMod1(reps, data);
                
            case NO_COUNT:
                return guard = countNoCount(reps, data);
        }

        throw new RuntimeException();
    }

    private static long countJava(int reps, byte [] data)
    {
        long codePoints = 0;
        CharsetDecoder decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT);

        CharBuffer out = CharBuffer.allocate(1024);
        final char[] outArray = out.array();
        out.mark();

        for (int i = 0; i < reps; i++)
        {
            ByteBuffer in = ByteBuffer.wrap(data);
            out.clear();
            while (decoder.decode(in, out, true) == CoderResult.OVERFLOW) {
                codePoints += Character.codePointCount(outArray, 0, out.position());
                out.clear();
            }
            codePoints += Character.codePointCount(outArray, 0, out.position());
        }
        
        return codePoints;
    }

    private static long countLucene(int reps, byte [] data)
    {
        long codePoints = 0;
        BytesRef bref = new BytesRef(data);
        for (int i = 0; i < reps; i++)
        {
            codePoints += UnicodeUtil.codePointCount(bref);
        }
        return codePoints;
    }

    private static long noLookupIf(int reps, byte [] data)
    {
        long codePoints = 0;
        BytesRef bref = new BytesRef(data);
        for (int i = 0; i < reps; i++)
        {
            codePoints += noLookupCodePointCount(bref);
        }
        return codePoints;
    }

    private static long countLuceneMod1(int reps, byte [] data)
    {
        long codePoints = 0;
        BytesRef bref = new BytesRef(data);
        for (int i = 0; i < reps; i++)
        {
            codePoints += codePointCount(bref);
        }
        return codePoints;
    }


    private static long countNoCount(int reps, byte [] data)
    {
        long codePoints = 0;
        for (int i = 0; i < reps; i++)
        {
            for (int j = 0; j < data.length; j++) {
                codePoints += data[j] & 0xFF;
            }
        }
        return codePoints;
    }

    /**
     * No lookup array, if sequence. 
     */
    public static long noLookupCodePointCount(BytesRef utf8) {
      int upto = utf8.offset;
      final int limit = utf8.offset + utf8.length;
      final byte[] bytes = utf8.bytes;
      int codePointCount = 0;
      while (upto < limit) {
        codePointCount++;
        int v = bytes[upto] & 0xFF;
        if (v <= 128) {
            upto++; // ascii case.
        } else {
            if (v >= 192) {
                if (v < 224) {
                    upto += 2;
                } else if (v < 240) {
                    upto += 3;
                } else if (v < 248) {
                    upto += 4;
                } else {
                    // more than 4 bytes?
                }
            } else {
                // Invalid.
            }
        }
      }
      return codePointCount;
    }

    /** 
     * Returns the number of code points in this UTF8 sequence.
     * 
     * <p>This method assumes valid UTF8 input, the result will be invalid
     * or an assertion will be thrown on invalid UTF8 sequences. This method does *not* perform
     * full UTF8 validation, it will check only the first byte of each codepoint (for
     * multi-byte sequences any bytes after the head are skipped). 
     * 
     * @throws AssertionError If invalid codepoint header byte occurs or the content is 
     * prematurely truncated.
     */
    public static int codePointCount(BytesRef utf8) {
      int pos = utf8.offset;
      final int limit = pos + utf8.length;
      final byte[] bytes = utf8.bytes;

      int codePointCount = 0;
      for (; pos < limit; codePointCount++) {
        int v = bytes[pos] & 0xFF;
        if (v <   /* 0xxx xxxx */ 0x80) { pos += 1; continue; }
        if (v >=  /* 110x xxxx */ 0xc0) {
          if (v < /* 111x xxxx */ 0xe0) { pos += 2; continue; } 
          if (v < /* 1111 xxxx */ 0xf0) { pos += 3; continue; } 
          if (v < /* 1111 1xxx */ 0xf8) { pos += 4; continue; }
          // fallthrough, consider 5 and 6 byte sequences invalid. 
        }

        // Anything not covered above is invalid UTF8.
        assert false;
      }

      // Check if we didn't go over the limit on the last character.
      assert pos <= limit;
      return codePointCount;
    }

    public static void main(String [] args) throws Exception
    {
        // Sanity check.
        for (DataType dt : DataType.values()) {
            for (Implementation i : Implementation.values()) {
                BenchmarkCodePointCount c = new BenchmarkCodePointCount();
                c.implementation = i;
                c.dataType = dt;
                c.setUp();
                long s = System.currentTimeMillis();
                long v = c.timeCodePointCounting(10);
                long e = System.currentTimeMillis();
                
                System.out.println(String.format(Locale.ENGLISH,
                    "%10.3f <= [%10d] %s %s",
                    (e - s) / 1000.0,
                    v,
                    dt,
                    i));
            }
        }

        System.out.println("Lucene: " + countLucene(1, DATA_UNICODE));
        System.out.println("Java: " + countJava(1, DATA_UNICODE));
        System.out.println("NoLookup(if): " + noLookupIf(1, DATA_UNICODE));
        System.out.println("LuceneMod1: " + countLuceneMod1(1, DATA_UNICODE));
        System.out.println("NoCount: " + countNoCount(1, DATA_UNICODE));

        // Benchmark.
        Runner.main(BenchmarkCodePointCount.class, args);
    }
}
