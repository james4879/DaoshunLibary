package com.daoshun.lib.communication.data;

import java.io.File;

/**
 * 上传文件请求参数类
 * 
 * @author guok
 * 
 */
public class UploadParam {

    private File file;// 上传文件

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

}
