package com.daoshun.lib.communication.data;

/**
 * 下载用RequestData
 */
public class DownloadParameter {

    /**
     * 保存路径
     */
    private String saveFilePath;

    /**
     * 临时文件路径
     */
    private String tempFilePath;

    /**
     * @return 保存路径
     */
    public String getSaveFilePath() {
        return saveFilePath;
    }

    /**
     * @param saveFilePath
     *            保存路径
     */
    public void setSaveFilePath(String saveFilePath) {
        this.saveFilePath = saveFilePath;
    }

    /**
     * @return 临时文件路径
     */
    public String getTempFilePath() {
        return tempFilePath;
    }

    /**
     * @param tempFilePath
     *            临时文件路径
     */
    public void setTempFilePath(String tempFilePath) {
        this.tempFilePath = tempFilePath;
    }
}
