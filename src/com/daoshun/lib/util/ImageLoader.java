package com.daoshun.lib.util;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.View;

import com.daoshun.lib.communication.data.DownloadParameter;
import com.daoshun.lib.communication.http.DownloadAccessor;

public class ImageLoader {

    private Context mContext;
    private String mImageFolder;
    private String mTempFolder;

    // 图片缓存
    private Map<String, SoftReference<Bitmap>> mImageCache =
            new HashMap<String, SoftReference<Bitmap>>();

    // 图片下载进程管理
    private static Map<String, ImageLoaderTask> mTaskManager =
            new HashMap<String, ImageLoaderTask>();

    /**
     * 构造函数
     * 
     * @param context
     *            context
     * @param imageFolder
     *            图片保存目录
     * @param tempFolder
     *            下载临时目录
     */
    public ImageLoader(Context context, String imageFolder, String tempFolder) {
        mContext = context;
        mImageFolder = imageFolder;
        mTempFolder = tempFolder;
    }

    /**
     * 读取图片
     * 
     * @param imageUrl
     *            图片地址
     * @param targetView
     *            目标View
     * @param width
     *            宽度
     * @param height
     *            高度
     * @param onLoadListener
     *            监听
     */
    public void loadImage(String imageUrl, View targetView, int width, int height,
            OnLoadListener onLoadListener) {
        if (imageUrl != null && imageUrl.length() > 0) {
            String imageName = FileUtils.getFileFullNameByUrl(imageUrl);
            String key = getKey(imageName, width, height);
            targetView.setTag(key);

            // 尝试从缓存中读取图片
            if (mImageCache.containsKey(key) && mImageCache.get(key).get() != null) {
                if (onLoadListener != null)
                    onLoadListener.onLoad(mImageCache.get(key).get(), targetView);

            } else {
                File file = new File(mImageFolder, imageName);

                // 尝试从本地读取图片
                if (file.exists()) {
                    Bitmap bitmap = BitmapUtils.getBitmapFromFile(file, width, height);
                    mImageCache.put(key, new SoftReference<Bitmap>(bitmap));
                    if (onLoadListener != null)
                        onLoadListener.onLoad(bitmap, targetView);

                } else {
                    // 从远程服务端读取图片
                    ImageLoaderHolder holder = new ImageLoaderHolder();
                    holder.targetView = targetView;
                    holder.height = height;
                    holder.width = width;
                    holder.onLoadListener = onLoadListener;

                    if (mTaskManager.containsKey(imageName)) {
                        ImageLoaderTask task = mTaskManager.get(imageName);
                        task.addHolder(holder);
                    } else {
                        ImageLoaderTask task = new ImageLoaderTask(imageUrl, imageName);
                        task.addHolder(holder);
                        task.execute();
                    }
                }
            }
        }
    }

    /**
     * 读取图片
     * 
     * @param imageUrl
     *            图片地址
     * @param targetView
     *            目标View
     * @param onLoadListener
     *            监听
     */
    public void loadImage(String imageUrl, View targetView, OnLoadListener onLoadListener) {
        loadImage(imageUrl, targetView, 0, 0, onLoadListener);
    }

    public interface OnLoadListener {

        public void onLoad(Bitmap bitmap, View targetView);
    }

    private class ImageLoaderTask extends AsyncTask<Void, Void, String> {

        private String mImageUrl;
        private String mImageName;

        private List<ImageLoaderHolder> mHolderList = new ArrayList<ImageLoaderHolder>();

        public ImageLoaderTask(String imageUrl, String imageName) {
            mImageUrl = imageUrl;
            mImageName = imageName;
        }

        @Override
        protected void onPreExecute() {
            mTaskManager.put(mImageName, this);
        }

        public void addHolder(ImageLoaderHolder holder) {
            mHolderList.add(holder);
        }

        @Override
        protected String doInBackground(Void... params) {
            if (mImageUrl != null && mImageUrl.length() > 0) {
                DownloadParameter parameter = new DownloadParameter();
                parameter.setSaveFilePath(mImageFolder + File.separator + mImageName);
                parameter.setTempFilePath(mTempFolder + File.separator + mImageName);

                DownloadAccessor accessor = new DownloadAccessor(mContext);
                Boolean result = accessor.execute(mImageUrl, parameter);
                if (result != null)
                    return result ? parameter.getSaveFilePath() : null;
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            mTaskManager.remove(mImageName);

            if (result != null) {
                for (ImageLoaderHolder holder : mHolderList) {
                    String key = getKey(mImageName, holder.width, holder.height);
                    Bitmap bitmap =
                            BitmapUtils.getBitmapFromFile(new File(result), holder.width,
                                    holder.height);
                    mImageCache.put(key, new SoftReference<Bitmap>(bitmap));

                    if (holder.targetView != null
                            && key.equals(holder.targetView.getTag())
                            && holder.onLoadListener != null) {
                        holder.onLoadListener.onLoad(bitmap, holder.targetView);
                    }
                }

                mHolderList.clear();
            }
        }
    }

    private class ImageLoaderHolder {

        public View targetView;
        public int width;
        public int height;
        public OnLoadListener onLoadListener;
    }

    private String getKey(String imageName, int width, int height) {
        if (width == 0 || height == 0)
            return imageName;
        else
            return imageName + "_" + width + "x" + height;
    }
}
