package com.daoshun.lib.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 文件处理工具
 */
public class FileUtils {

    /**
     * 保存字节流至文件
     * 
     * @param bytes
     *            字节流
     * @param file
     *            目标文件
     */
    public static final boolean saveBytesToFile(byte[] bytes, File file) {
        if (bytes == null) {
            return false;
        }

        ByteArrayInputStream bais = null;
        BufferedOutputStream bos = null;
        try {
            file.getParentFile().mkdirs();
            file.createNewFile();

            bais = new ByteArrayInputStream(bytes);
            bos = new BufferedOutputStream(new FileOutputStream(file));

            int size;
            byte[] temp = new byte[1024];
            while ((size = bais.read(temp, 0, temp.length)) != -1) {
                bos.write(temp, 0, size);
            }

            bos.flush();

            return true;

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                bos = null;
            }
            if (bais != null) {
                try {
                    bais.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                bais = null;
            }
        }
        return false;
    }

    /**
     * 复制文件
     * 
     * @param srcFile
     *            源文件
     * @param destFile
     *            目标文件
     */
    public static final boolean copyFile(File srcFile, File destFile) {
        if (!srcFile.exists()) {
            return false;
        }

        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            destFile.getParentFile().mkdirs();
            destFile.createNewFile();

            bis = new BufferedInputStream(new FileInputStream(srcFile));
            bos = new BufferedOutputStream(new FileOutputStream(destFile));

            int size;
            byte[] temp = new byte[1024];
            while ((size = bis.read(temp, 0, temp.length)) != -1) {
                bos.write(temp, 0, size);
            }

            bos.flush();

            return true;

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                bos = null;
            }
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                bis = null;
            }
        }
        return false;
    }

    /**
     * 根据文件路径获得全文件名
     * 
     * @param path
     *            文件路径
     */
    public static final String getFileFullNameByPath(String path) {
        String name = null;
        if (path != null) {
            int start = path.lastIndexOf(File.separator);
            name = path.substring(start == -1 ? 0 : start + 1);
        }
        return name;
    }

    /**
     * 根据文件路径获得文件名
     * 
     * @param path
     *            文件路径
     */
    public static final String getFileNameByPath(String path) {
        String name = null;
        if (path != null) {
            int start = path.lastIndexOf(File.separator);
            int end = path.lastIndexOf(".");
            name = path.substring(start == -1 ? 0 : start + 1, end == -1 ? path.length() : end);
        }
        return name;
    }

    /**
     * 根据文件路径获得后缀名
     * 
     * @param path
     *            文件路径
     */
    public static final String getFileTypeByPath(String path) {
        String type = null;
        if (path != null) {
            int start = path.lastIndexOf(".");
            if (start != -1) {
                type = path.substring(start + 1);
            }
        }
        return type;
    }

    /**
     * 根据URL获得全文件名
     * 
     * @param url
     *            URL
     */
    public static final String getFileFullNameByUrl(String url) {
        String name = null;
        if (url != null) {
            int start = url.lastIndexOf("/");
            int end = url.lastIndexOf("?");
            name = url.substring(start == -1 ? 0 : start + 1, end == -1 ? url.length() : end);
        }
        return name;
    }

    /**
     * 根据URL获得文件名
     * 
     * @param url
     *            URL
     */
    public static final String getFileNameByUrl(String url) {
        String name = null;
        if (url != null) {
            int start = url.lastIndexOf("/");
            int end = url.lastIndexOf(".");
            int end2 = url.lastIndexOf("?");
            name =
                    url.substring(start == -1 ? 0 : start + 1,
                            end == -1 ? (end2 == -1 ? url.length() : end2) : end);
        }
        return name;
    }

    /**
     * 根据URL获得后缀名
     * 
     * @param url
     *            URL
     */
    public static final String getFileTypeByUrl(String url) {
        String type = null;
        if (url != null) {
            int start = url.lastIndexOf(".");
            int end = url.lastIndexOf("?");
            if (start != -1) {
                type = url.substring(start + 1, end == -1 ? url.length() : end);
            }
        }
        return type;
    }
}
