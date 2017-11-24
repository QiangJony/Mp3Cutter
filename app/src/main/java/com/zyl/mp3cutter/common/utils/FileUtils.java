package com.zyl.mp3cutter.common.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DecimalFormat;
import java.util.List;

public class FileUtils {
    /**
     * 生成文件
     *
     * @param file
     * @param datas
     * @return
     */
    public static boolean generateFile(File file, List<byte[]> datas) {
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(file));
            for (byte[] data : datas) {
                bos.write(data);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    /**
     * 已有文件后增加字节
     *
     * @param file
     * @param datas
     * @return
     */
    public static boolean appendData(File file, byte[][] datas) {
        RandomAccessFile rfile = null;
        try {
            rfile = new RandomAccessFile(file, "rw");
            rfile.seek(file.length());
            for (byte[] data : datas) {
                rfile.write(data);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (rfile != null) {
                try {
                    rfile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }


    /**
     * 监测文件夹是否存在，不存在则创建
     *
     * @param strFolder
     * @return
     */
    public static boolean bFolder(String strFolder) {
        boolean btmp = false;
        File f = new File(strFolder);
        if (!f.exists()) {
            if (f.mkdirs()) {
                btmp = true;
            } else {
                btmp = false;
            }
        } else {
            btmp = true;
        }
        return btmp;
    }

    /**
     * 获取文件大小 单位为字节
     * @param f
     * @return
     * @throws Exception
     */
    public static long getFileSizes(File f) throws Exception {//取得文件大小
        long s = 0;
        if (f.exists()) {
            FileInputStream fis = null;
            fis = new FileInputStream(f);
            s = fis.available();
        } else {
//            f.createNewFile();
            System.out.println("文件不存在");
        }
        return s;
    }

    /**
     * 格式化文件大小
     * @param fileS
     * @return
     */
    public static String formetFileSize(long fileS) {
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        if (fileS < 1024) {
            fileSizeString = df.format((double) fileS) + "B";
        } else if (fileS < 1048576) {
            fileSizeString = df.format((double) fileS / 1024) + "K";
        } else if (fileS < 1073741824) {
            fileSizeString = df.format((double) fileS / 1048576) + "M";
        } else {
            fileSizeString = df.format((double) fileS / 1073741824) +"G";
        }
        return fileSizeString;
    }

    /**
     * 格式化返回文件大小
     * @param f
     * @return
     */
    public static String getFormatFileSizeForFile(File f){
        try {
            return formetFileSize(getFileSizes(f));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}