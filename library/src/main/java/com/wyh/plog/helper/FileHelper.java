package com.wyh.plog.helper;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.wyh.plog.core.PLogConstant;
import com.wyh.plog.core.PLogPrint;
import com.wyh.plog.core.PLogTag;
import com.wyh.plog.util.AppUtil;
import com.wyh.plog.util.DateUtil;
import com.wyh.plog.util.IOUtil;
import com.wyh.plog.util.LogUtil;
import com.wyh.plog.util.ZipUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by wyh on 2019/3/12.
 */
public final class FileHelper {

    /**
     * 默认日志存放目录
     */
    public static File getDefaultLogDir(Context context) {
        if (context == null) {
            return null;
        }
        File dir = getSDLogDirFile(context);
        if (dir == null || !isSDEnough()) {
            dir = getCacheLogDirFile(context);
        }
        return dir;
    }

    /**
     * 获取sd卡日志存放目录
     */
    private static File getSDLogDirFile(Context context) {
        if (context == null || !Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return null;
        }
        File sdFile = new File(Environment.getExternalStorageDirectory() +
                "/Android/data/" + context.getApplicationContext().getPackageName() + "/files");
        sdFile = new File(sdFile, getDirName(context));
        if (!sdFile.exists() || !sdFile.isDirectory()) {
            sdFile.mkdirs();
        }
        return sdFile;
    }

    /**
     * 获取内置存储日志存放目录
     */
    private static File getCacheLogDirFile(Context context) {
        if (context == null) {
            return null;
        }
        File cacheFile = null;
        try {
            cacheFile = context.getCacheDir();
        } catch (Exception ex) {
            ex.printStackTrace();
            cacheFile = context.getExternalCacheDir();
        }

        if (cacheFile != null) {
            cacheFile = new File(cacheFile, getDirName(context));
            if (!cacheFile.exists() || !cacheFile.isDirectory()) {
                cacheFile.mkdirs();
            }
        }
        return cacheFile;
    }

    /**
     * 定义日志路径
     */
    private static String getDirName(Context context) {
        return PLogConstant.LOG_DIR +
                File.separator +
                AppUtil.getCurProcessName(context);
    }

    public static boolean isFileExist(File file) {
        return file != null && file.exists();
    }

    public static boolean isDirExist(File file) {
        return file != null && file.exists() && file.isDirectory();
    }

    public static boolean isFileExist(String path) {
        return isFileExist(new File(path));
    }

    public static boolean isDirExist(String path) {
        return isDirExist(new File(path));
    }

    /**
     * 清理过期文件
     */
    public static void cleanOverdueLog(Context context, String logDirPath, long overdueDayMs) {
        {
            File file = getSDLogDirFile(context);
            if (file != null) {
                filterUpLogFile(file.listFiles(), overdueDayMs);
            }
        }
        {
            File file = getCacheLogDirFile(context);
            if (file != null) {
                filterUpLogFile(file.listFiles(), overdueDayMs);
            }
        }
        {
            File file = new File(logDirPath);
            filterUpLogFile(file.listFiles(), overdueDayMs);
        }
    }

    /**
     * 文件是否过期
     */
    private static boolean isFileOverdue(File file, long overdueDayMs) {
        return System.currentTimeMillis() - file.lastModified() > overdueDayMs;
    }

    /**
     * 将类似2017-11-05的文件命名成2017-11-05-up这样的名称
     * 标记为可上传的日志文件
     */
    private static File renameToUp(File file) {
        if (file == null || !file.exists()) {
            return file;
        }

        StringBuilder renameBuilder = new StringBuilder();
        renameBuilder.append(file.getParentFile().getAbsolutePath());
        renameBuilder.append(File.separator);
        renameBuilder.append(file.getName());
        renameBuilder.append(PLogConstant.UP);

        File targetFile = new File(renameBuilder.toString());
        if (targetFile.exists()) {
            targetFile.delete();
        }
        if (file.renameTo(targetFile)) {
            return targetFile;
        }
        return file;
    }

    /**
     * 获取所有未过期的已经压缩过的日志
     * 主要是为了避免遗漏上传失败时的日志文件
     */
    @NonNull
    public static List<File> filterExistsZipFiles(String logDiaPath, long overdueDayMs) {
        final List<File> zipFiles = new LinkedList<>();
        for (File file : new File(logDiaPath).listFiles()) {
            if (!isFileExist(file)) {
                continue;
            }
            if (isFileOverdue(file, overdueDayMs)) {
                file.delete();
                continue;
            }
            if (file.getName().endsWith(PLogConstant.ZIP)) {
                zipFiles.add(file);
            }
        }
        return zipFiles;
    }

    /**
     * 压缩所有标记为"-up"的可上传的日志后返回压缩文件
     */
    @Nullable
    public static File zipAllUpLogFile(Context context, String logDirPath, String password, long overdueDayMs) {
        if (context == null) {
            return null;
        }
        final ArrayList<File> allUpLogFiles = new ArrayList<>();
        // 扫描有标记-up的日志文件
        if (!TextUtils.isEmpty(logDirPath)) {
            allUpLogFiles.addAll(filterUpLogFile(new File(logDirPath).listFiles(), overdueDayMs));
        }
        if (allUpLogFiles.size() == 0) {
            return null;
        }
        Collections.sort(allUpLogFiles, new Comparator<File>() {
            @Override
            public int compare(File lhs, File rhs) {
                return (int) (rhs.lastModified() - lhs.lastModified());
            }
        });
        StringBuilder stringBuilder = new StringBuilder();
        for (File logFile : allUpLogFiles) {
            stringBuilder.append(logFile.getName());
            stringBuilder.append("(");
            stringBuilder.append(LogUtil.b2Mb(logFile.length()));
            stringBuilder.append(") ");
            if (logFile.getName().contains(PLogConstant.MMAP)) {
                deleteMmapFileBlankContent(logFile);
            }
        }
        String zipFilePath = logDirPath + File.separator + DateUtil.getTime() + PLogConstant.ZIP;
        //压缩
        File zipFile = ZipUtil.doZipFilesWithPassword(allUpLogFiles, zipFilePath, password);
        if (zipFile != null) {
            stringBuilder.append(" compress to ");
            stringBuilder.append(zipFile.getName());
            stringBuilder.append("(");
            stringBuilder.append(LogUtil.b2Mb(zipFile.length()));
            stringBuilder.append(")");
            PLogPrint.d(PLogTag.INTERNAL_TAG, stringBuilder.toString());
            //压缩成功后才删除
            for (File logFile : allUpLogFiles) {
                logFile.delete();
            }
        }
        PLogPrint.d(PLogTag.INTERNAL_TAG, "delete allUpLogFiles");
        return zipFile;
    }

    /**
     * 过滤出所有标记"-up"的日志文件
     * 清理无效&过期文件
     */
    @NonNull
    private static List<File> filterUpLogFile(File[] files, long overdueDayMs) {
        final List<File> upLogFileList = new LinkedList<>();
        if (files == null || files.length == 0) {
            return upLogFileList;
        }
        for (File file : files) {
            if (!isFileExist(file)) {
                continue;
            }
            if (isFileOverdue(file, overdueDayMs)) {
                file.delete();
                continue;
            }
            if (file.getName().contains(PLogConstant.UP)) {
                upLogFileList.add(file);
            }
        }
        return upLogFileList;
    }


    /**
     * 触发上传时调用，重命名日志文件添加"-up"，标记为可上传的日志文件，
     * 清理无效&过期文件
     *
     * @param writeFileName 当天正在写入的日志文件，需要过滤掉。因为在closeAndRenew(true)方法中底层已经把当天的日志文件重命名添加过"-up"了
     * @param logDirPath    日志存放目录
     */
    public static void renameToUpAllIfNeed(String writeFileName, String logDirPath, long overdueDayMs) {
        File logDir = new File(logDirPath);
        final File[] files = logDir.listFiles();
        if (files == null || files.length == 0) {
            return;
        }
        for (File logFile : files) {
            if (!isFileExist(logFile)) {
                continue;
            }
            if (isFileOverdue(logFile, overdueDayMs)) {
                logFile.delete();
                continue;
            }
            final String fileName = logFile.getName();
            // skip the writing file
            if (writeFileName.equals(fileName) || fileName.contains(PLogConstant.UP) || fileName.endsWith(PLogConstant.ZIP)) {
                continue;
            }
            renameToUp(logFile);
        }
    }

    /**
     * 删除mmap文件末尾的空白
     */
    private static void deleteMmapFileBlankContent(File file) {
        if (isFileExist(file)) {
            return;
        }
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "rw");
            long len = raf.length();
            if (len <= 3) {
                return;
            }
            long pos = len - 1;
            while (pos > 0) {
                --pos;
                raf.seek(pos);
                if (raf.readByte() == '\n') {
                    break;
                }
            }
            raf.getChannel().truncate(pos > 0 ? pos + 1 : pos).close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtil.closeQuietly(raf);
        }
    }


    private static boolean isSDEnough() {
        return getSDFreeSize() >= PLogConstant.MIN_SDCARD_FREE_SPACE_MB;
    }

    private static long getSDFreeSize() {
        try {
            File path = Environment.getExternalStorageDirectory();
            StatFs sf = new StatFs(path.getPath());
            long blockSize = sf.getBlockSizeLong();
            long freeBlocks = sf.getAvailableBlocksLong();
            return (freeBlocks * blockSize) / 1024 / 1024;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return PLogConstant.MIN_SDCARD_FREE_SPACE_MB;
    }


}