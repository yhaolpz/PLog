package com.wyh.plog.util;

import android.text.TextUtils;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by wyh on 2019/3/22.
 */
public class ZipUtil {

    private ZipUtil() {
    }

    /**
     * 对文件列表压缩加密
     */
    public static File doZipFilesWithPassword(ArrayList<File> srcfile, String destZipFile, String password) {
        if (srcfile == null || srcfile.size() == 0) {
            return null;
        }
        ZipParameters parameters = new ZipParameters();
        // 压缩方式
        parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
        // 压缩级别
        parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);
        // 加密方式
        if (!TextUtils.isEmpty(password)) {
            parameters.setEncryptFiles(true);
            parameters.setEncryptionMethod(Zip4jConstants.ENC_METHOD_STANDARD);
            parameters.setPassword(password);
        }
        try {
            ZipFile zipFile = new ZipFile(destZipFile);
            zipFile.addFiles(srcfile, parameters);
            return zipFile.getFile();
        } catch (ZipException e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * 对文件夹加密
     */
    public static File doZipFilesWithPassword(File folder, String destZipFile, String password) {
        if (!folder.exists()) {
            return null;
        }
        ZipParameters parameters = new ZipParameters();
        // 压缩方式
        parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
        // 压缩级别
        parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);
        // 加密方式
        if (!TextUtils.isEmpty(password)) {
            parameters.setEncryptFiles(true);//
            parameters.setEncryptionMethod(Zip4jConstants.ENC_METHOD_STANDARD);
            parameters.setPassword(password);
        }
        try {
            ZipFile zipFile = new ZipFile(destZipFile);
            zipFile.addFolder(folder, parameters);
            return zipFile.getFile();
        } catch (ZipException e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * 单文件压缩并加密
     *
     * @param file        要压缩的zip文件
     * @param destZipFile zip保存路径
     * @param password    密码   可以为null
     */
    public static File doZipSingleFileWithPassword(File file, String destZipFile, String password) {
        if (!file.exists()) {
            return null;
        }
        ZipParameters parameters = new ZipParameters();
        // 压缩方式
        parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
        // 压缩级别
        parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);
        // 加密方式
        if (!TextUtils.isEmpty(password)) {
            parameters.setEncryptFiles(true);//
            parameters.setEncryptionMethod(Zip4jConstants.ENC_METHOD_STANDARD);
            parameters.setPassword(password);
        }
        try {
            ZipFile zipFile = new ZipFile(destZipFile);
            zipFile.addFile(file, parameters);
            return zipFile.getFile();
        } catch (ZipException e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * 解压文件
     * File：目标zip文件
     * password：密码，如果没有可以传null
     * path：解压到的目录路径
     */
    public static boolean unZip(File file, String password, String path) {
        boolean res = false;
        try {
            ZipFile zipFile = new ZipFile(file);
            if (zipFile.isEncrypted()) {
                if (password != null && !password.isEmpty()) {
                    zipFile.setPassword(password);
                }
            }
            zipFile.extractAll(path);
            res = true;
        } catch (ZipException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return res;

    }
}
