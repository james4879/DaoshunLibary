package com.daoshun.lib.communication.http;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;

import android.content.Context;
import android.util.Log;

import com.daoshun.lib.communication.data.DownloadParameter;

/**
 * 从服务端下载图片
 */
public class DownloadAccessor extends HttpAccessor {

    private static final String TAG = DownloadAccessor.class.getName();

    private OnProgressListener mOnProgressListener;

    private int mInterval;

    /**
     * 构造函数
     */
    public DownloadAccessor(Context context) {
        super(context, HttpAccessor.METHOD_GET);
    }

    /**
     * 连接服务端开始通信
     * 
     * @param url
     *            请求URL
     * @param param
     *            参数
     * 
     * @return 数据结果
     */
    public Boolean execute(String url, DownloadParameter param) {
        try {
            return access(url, param);
        } catch (Exception e) {
            onException(e);
        }
        return null;
    }

    /**
     * 连接服务端开始通信
     * 
     * @param url
     *            请求URL
     * @param param
     *            参数
     * 
     * @return 结果
     */
    protected Boolean access(String url, DownloadParameter param) throws Exception {
        File tempFile = new File(param.getTempFilePath());
        tempFile.getParentFile().mkdirs();
        File file = new File(param.getSaveFilePath());
        file.getParentFile().mkdirs();

        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            long startTime = System.currentTimeMillis();

            if (mMethod == METHOD_POST) {
                mHttpRequest = new HttpPost();
            } else {
                mHttpRequest = new HttpGet();
            }

            mHttpRequest.setURI(new URI(url));

            HttpClient httpClient = getHttpClient();
            HttpResponse response = httpClient.execute(mHttpRequest);

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                long progress = 0;
                long total = response.getEntity().getContentLength();
                if (mOnProgressListener != null && mInterval > 0)
                    mOnProgressListener.onProgress(progress, total);

                bis = new BufferedInputStream(response.getEntity().getContent());

                tempFile.createNewFile();

                bos = new BufferedOutputStream(new FileOutputStream(tempFile));

                int size;
                byte[] temp = new byte[1024];
                while ((size = bis.read(temp, 0, temp.length)) != -1 && !mStoped) {
                    bos.write(temp, 0, size);

                    progress += size;

                    if (mOnProgressListener != null && mInterval > 0) {
                        long now = System.currentTimeMillis();
                        if (now - startTime >= mInterval) {
                            mOnProgressListener.onProgress(progress, total);
                            startTime = now;
                        }
                    }
                }

                bos.flush();
                bos.close();
                bos = null;

                if (mStoped)
                    return false;

                return tempFile.renameTo(file);

            } else {
                throw new SocketException("Status Code : "
                        + response.getStatusLine().getStatusCode());
            }

        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
                bos = null;
            }
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
                bis = null;
            }
        }
    }

    protected void onException(Exception e) {
        Log.e(TAG, e.getMessage(), e);
    }

    public void setOnProgressListener(OnProgressListener onProgressListener, int interval) {
        this.mOnProgressListener = onProgressListener;
        this.mInterval = interval;
    }

    public OnProgressListener getOnProgressListener() {
        return mOnProgressListener;
    }

    public void setInterval(int interval) {
        this.mInterval = interval;
    }

    public int getInterval() {
        return mInterval;
    }

    public interface OnProgressListener {

        public void onProgress(long progress, long total);
    }
}