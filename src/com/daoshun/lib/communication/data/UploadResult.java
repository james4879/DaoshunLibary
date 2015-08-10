package com.daoshun.lib.communication.data;

/**
 * 上传文件返回结果类
 * 
 * @author guok
 * 
 */
public class UploadResult {

    private int code;
    private String message;
    private int fileid;// 文件ID
    private String fileurl;
    public int getFileid() {
        return fileid;
    }

    public void setFileid(int fileid) {
        this.fileid = fileid;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

	public String getFileurl() {
		return fileurl;
	}

	public void setFileurl(String fileurl) {
		this.fileurl = fileurl;
	}

}
