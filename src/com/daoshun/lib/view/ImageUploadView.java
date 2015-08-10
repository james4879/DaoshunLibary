package com.daoshun.lib.view;

import android.content.Context;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.daoshun.lib.R;
import com.daoshun.lib.communication.data.UploadParam;
import com.daoshun.lib.communication.data.UploadResult;
import com.daoshun.lib.communication.http.HttpAccessor;
import com.daoshun.lib.communication.http.JSONAccessor;

/**
 * 上传图片控件，包括一张缩略图和一个ProgressBar
 * 
 * 如果上传参数只需要一个文件，且返回参数只包括fileid,则文件的上传可以完全交由View自身实现。如果需求有其它参数则本View只更新自身状态，不实现上传功能。
 * 
 * @author guok
 * 
 */
public class ImageUploadView extends FrameLayout {

    private ImageView mImageView;// 图片缩略图
    private ProgressBar mProgressBar;// 上传进度条
    private int mUploadState = 0;// 上传状态 0：未开始上传 1：正在上传 2：上传完成 3：上传失败
    private OnUploadCompleteListener mOnUploadCompleteListener;// 文件上传完成的监听事件

    public ImageUploadView(Context context) {
        super(context);
        init();
    }

    public ImageUploadView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ImageUploadView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        View root = LayoutInflater.from(getContext()).inflate(R.layout.image_upload, this);
        mImageView = (ImageView) root.findViewById(R.id.image_view);
        mProgressBar = (ProgressBar) root.findViewById(R.id.progress_bar);
    }

    /**
     * 设置ProgressBar的大小
     * 
     * @param width
     *            宽度
     * @param height
     *            高度
     */
    public void setProgressSize(int width, int height) {
        mProgressBar.getLayoutParams().width = width;
        mProgressBar.getLayoutParams().height = height;
    }

    /**
     * 得到ImageView
     * 
     * @return
     */
    public ImageView getImageView() {
        return mImageView;
    }

    /**
     * 得到ProgressBar
     * 
     * @return
     */
    public ProgressBar getProgressBar() {
        return mProgressBar;
    }

    /**
     * 取得文件上传状态
     * 
     * @return
     */

    public int getUploadState() {
        return mUploadState;
    }

    /**
     * 开始上传
     */
    public void startUpload() {
        mProgressBar.setVisibility(View.VISIBLE);// 显示ProgressBar
        mUploadState = 1;
    }

    /**
     * 上传完成
     * 
     * @param result
     *            上传结果 2：上传完成 3：上传失败
     */
    public void UploadComplete(int result) {
        mProgressBar.setVisibility(View.GONE);// 隐藏ProgressBar
        mUploadState = result;
    }

    public void startUpload(String uploadUrl, UploadParam uploadparam,
            OnUploadCompleteListener mOnUploadCompleteListener) {
        mProgressBar.setVisibility(View.VISIBLE);// 显示ProgressBar
        mUploadState = 1;
        this.mOnUploadCompleteListener = mOnUploadCompleteListener;
        new UploadFileTask(uploadparam).execute(uploadUrl);
    }

    /**
     * 图片上传异步类
     * 
     * @author guok
     * 
     */
    class UploadFileTask extends AsyncTask<String, Void, UploadResult> {

        private UploadParam uploadparam;
        private JSONAccessor mAccessor;

        public UploadFileTask(UploadParam uploadparam) {
            this.uploadparam = uploadparam;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        // 文件上传
        @Override
        protected UploadResult doInBackground(String... params) {
            mAccessor = new JSONAccessor(getContext(), HttpAccessor.METHOD_POST_MULTIPART);
            return mAccessor.execute(params[0], uploadparam, UploadResult.class);
        }

        @Override
        protected void onPostExecute(UploadResult result) {

            mProgressBar.setVisibility(View.GONE);// 隐藏ProgressBar
            if (result != null && result.getCode() == 1)
                mUploadState = 2;
            else
                mUploadState = 3;

            if (mOnUploadCompleteListener != null)
                mOnUploadCompleteListener.onComplete(result);
            super.onPostExecute(result);
        }

        // 中断通信
        public void stop() {
            if (mAccessor != null) {
                mAccessor.stop();
            }
        }

    }
}
