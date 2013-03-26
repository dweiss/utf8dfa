package com.dawidweiss.utf8dfa;

import java.util.BitSet;

import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.UnicodeUtil;

import com.google.caliper.Runner;
import com.google.caliper.SimpleBenchmark;

public class BenchmarkAcceptPolishBooks extends SimpleBenchmark
{
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
    }

    public int timeOnPolishBooks(int reps)
    {
        int codePoints = 0;
        for (int i = 0; i < reps; i++)
        {
            byte [] data = DataPolishBooks.DATA;
            BytesRef bref = new BytesRef(data);
            codePoints += UnicodeUtil.codePointCount(bref);
        }
        return codePoints;
    }

    static int[] values = {
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0, // 00..1f
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0, // 20..3f
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0, // 40..5f
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0, // 60..7f
        1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9, // 80..9f
        7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7, // a0..bf
        8,8,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2, // c0..df
        0xa,0x3,0x3,0x3,0x3,0x3,0x3,0x3,0x3,0x3,0x3,0x3,0x3,0x4,0x3,0x3, // e0..ef
        0xb,0x6,0x6,0x6,0x5,0x8,0x8,0x8,0x8,0x8,0x8,0x8,0x8,0x8,0x8,0x8, // f0..ff
        0x0,0x1,0x2,0x3,0x5,0x8,0x7,0x1,0x1,0x1,0x4,0x6,0x1,0x1,0x1,0x1, // s0..s0
        1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,1,1,1,1,1,0,1,0,1,1,1,1,1,1, // s1..s2
        1,2,1,1,1,1,1,2,1,2,1,1,1,1,1,1,1,1,1,1,1,1,1,2,1,1,1,1,1,1,1,1, // s3..s4
        1,2,1,1,1,1,1,1,1,2,1,1,1,1,1,1,1,1,1,1,1,1,1,3,1,3,1,1,1,1,1,1, // s5..s6
        1,3,1,1,1,1,1,3,1,3,1,1,1,1,1,1,1,3,1,1,1,1,1,1,1,1,1,1,1,1,1,1, // s7..s8
    };

    public static void main(String [] args)
    {
        int lookupBits = 2;
        int lookupMask = (1 << lookupBits) - 1;

        int bits = lookupBits * 2;
        int mask = (1 << bits) - 1;

        {
            int maxk = 0;
            seedLoop: for (int seed = 1; seed <= mask; seed += 2)
            {
                System.out.println("Seed: " + seed);

                int k;
                for (k = 0; k <= mask; k++)
                {
                    int h = k;

                    h *= seed;
                    h &= mask;
                    h ^= h >>> lookupBits;

                    int v = h & lookupMask;
                    maxk = Math.max(k, maxk);
                    
                    System.out.println(
                        String.format("%3x %16s %16s %8s %3d", 
                            k,
                            Integer.toBinaryString(k), 
                            Integer.toBinaryString(h),
                            Integer.toBinaryString(v),
                            v));
                    
                    if (v != values[k]) {
                        //continue seedLoop;
                    }
                }
            }

            System.out.println("Maxk: " + maxk);
        }

        System.exit(0);

        for (int i = 0; i < values.length; i++)
        {
            System.out.println(String.format("%3x %16s %16s", i,
                Integer.toBinaryString(i), Integer.toBinaryString(values[i])));
        }

        BitSet bset = new BitSet();
        for (int k = 0; k < 40; k++)
        {
            int h = k;
            h *= 3;
            h &= 0x0f;
            h ^= h >>> 4;

            bset.set(h & 0x0f);
            if (bset.cardinality() == 0x10)
            {
                break;
            }

            System.out.println(String.format("%16s %16s %d", Integer.toBinaryString(k),
                Integer.toBinaryString(h), bset.cardinality()));
        }

        System.exit(0);
        Runner.main(BenchmarkAcceptPolishBooks.class, args);
    }
}
