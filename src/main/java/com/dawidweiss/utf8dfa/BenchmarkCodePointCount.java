package com.dawidweiss.utf8dfa;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
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
        NOLOOKUP_IF
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
    private volatile int guard;

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

    public int timeCodePointCounting(int reps) throws Exception
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
        }

        throw new RuntimeException();
    }

    private static int countJava(int reps, byte [] data)
    {
        int codePoints = 0;
        CharsetDecoder decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT);

        ByteBuffer in = ByteBuffer.wrap(data);
        CharBuffer out = CharBuffer.allocate(1024);
        final char[] outArray = out.array();
        out.mark();

        for (int i = 0; i < reps; i++)
        {
            out.clear();
            while (decoder.decode(in, out, true) == CoderResult.OVERFLOW) {
                codePoints += Character.codePointCount(outArray, 0, out.position());
                out.clear();
            }
            codePoints += Character.codePointCount(outArray, 0, out.position());
        }
        
        return codePoints;
    }

    private static int countLucene(int reps, byte [] data)
    {
        int codePoints = 0;
        BytesRef bref = new BytesRef(data);
        for (int i = 0; i < reps; i++)
        {
            codePoints += UnicodeUtil.codePointCount(bref);
        }
        return codePoints;
    }

    private static int noLookupIf(int reps, byte [] data)
    {
        int codePoints = 0;
        BytesRef bref = new BytesRef(data);
        for (int i = 0; i < reps; i++)
        {
            codePoints += noLookupCodePointCount(bref);
        }
        return codePoints;
    }

    private static int countLuceneMod1(int reps, byte [] data)
    {
        int codePoints = 0;
        BytesRef bref = new BytesRef(data);
        for (int i = 0; i < reps; i++)
        {
            codePoints += codePointCount(bref);
        }
        return codePoints;
    }

    /**
     * No lookup array, if sequence. 
     */
    public static int noLookupCodePointCount(BytesRef utf8) {
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
     */
    public static int countLuceneMod1(BytesRef utf8) {
      int upto = utf8.offset;
      final int limit = utf8.offset + utf8.length;
      final byte[] bytes = utf8.bytes;
      int codePointCount = 0;
      while (upto < limit) {
        codePointCount++;
        switch (Integer.numberOfLeadingZeros((~bytes[upto]) & 0xFF) - 24) {
            case 0:
                upto += 1; break;
            case 2:
                upto += 2; break;
            case 3:
                upto += 3; break;
            case 4:
                upto += 4; break;
            default:
                // invalid.
                throw new RuntimeException("Invalid." + Integer.toHexString(bytes[upto]));
        }
      }
      return codePointCount;
    }

    static byte[] utf8CodeLength = new byte[] {
      1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
      1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
      1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
      1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
      1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
      1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
      1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
      1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
      2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
      2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
      3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
      4, 4, 4, 4, 4, 4, 4, 4 //, 5, 5, 5, 5, 6, 6, 0, 0
    };

    /** Returns the number of code points in this utf8
     *  sequence.  Behavior is undefined if the utf8 sequence
     *  is invalid.*/
    public static int codePointCount(BytesRef utf8) {
      int upto = utf8.offset;
      final int limit = utf8.offset + utf8.length;
      final byte[] bytes = utf8.bytes;
      int codePointCount = 0;
      
      int v;
      while (upto < limit) {
        codePointCount++;
        
        v = bytes[upto] & 0xFF;
        if (v < 128) {
            upto++;
            continue;
        } else {
            upto += utf8CodeLength[v];
        }
      }
      return codePointCount;
    }
    
    public static void main(String [] args)
    {
        // Sanity check.
        System.out.println("Lucene: " + countLucene(1, DATA_UNICODE));
        System.out.println("Java: " + countJava(1, DATA_UNICODE));
        System.out.println("NoLookup(if): " + noLookupIf(1, DATA_UNICODE));
        System.out.println("LuceneMod1: " + countLuceneMod1(1, DATA_UNICODE));

        // Benchmark.
        Runner.main(BenchmarkCodePointCount.class, args);
    }
}
