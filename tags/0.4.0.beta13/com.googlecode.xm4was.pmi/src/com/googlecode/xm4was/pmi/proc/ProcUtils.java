package com.googlecode.xm4was.pmi.proc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public final class ProcUtils {
    private ProcUtils() {}
    
    public static long getLongValue(File file, int position) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "ascii"));
        try {
            String line = in.readLine();
            return Long.parseLong(line.split("\\s+")[position]);
        } finally {
            in.close();
        }
    }
}
