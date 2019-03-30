package com.wyh.plog.util;

import java.io.Closeable;

/**
 * Created by wyh on 2019/3/10.
 */
public class IOUtil {
    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException ex) {
                throw ex;
            } catch (Exception ignored) {

            }
        }
    }
}
