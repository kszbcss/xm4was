package com.googlecode.xm4was.pmi.proc;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import org.junit.Test;

public class ProcUtilsTest {
    @Test
    public void testGetLongValue() throws Exception {
        File file = File.createTempFile("xm4was", "tmp");
        try {
            PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "ascii"));
            try {
                out.println("2542 (java) S 1 2542 2470 0 -1 4194304 155825 0 20 0 7052 512 0 0 25 0 132 0 12626 1089028096 141012 4294967295 134512640 134557368 3213829648 3213820924 1115138 0 0 3149824 17663 4294967295 0 0 17 0 0 0 0");
            } finally {
                out.close();
            }
            assertEquals(1089028096, ProcUtils.getLongValue(file, 22));
        } finally {
            file.delete();
        }
    }
}
