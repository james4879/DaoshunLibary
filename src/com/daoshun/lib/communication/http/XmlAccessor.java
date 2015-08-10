package com.daoshun.lib.communication.http;

import java.io.File;
import java.lang.reflect.Field;
import java.net.SocketException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.protocol.HTTP;

import android.content.Context;
import android.util.Log;

import com.daoshun.lib.util.DataParseUtils;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.mapper.CannotResolveClassException;
import com.thoughtworks.xstream.mapper.MapperWrapper;

/**
 * 从服务端读取JSON内容
 */
public class XmlAccessor extends HttpAccessor {

    private static final String TAG = XmlAccessor.class.getName();

    private XStream mXStream;

    /**
     * 构造函数
     */
    public XmlAccessor(Context context, int method) {
        super(context, method);
        initXStream();
    }

    /**
     * 连接服务端开始通信
     * 
     * @param url
     *            请求URL
     * @param param
     *            参数
     * @param returnType
     *            返回类型
     * 
     * @return 数据结果
     */
    public <T> T execute(String url, Object param, Class<T> returnType) {
        try {
            return access(url, param, returnType);
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
     * @param returnType
     *            返回类型
     * 
     * @return 数据结果
     */
    protected <T> T access(String url, Object param, Class<T> returnType) throws Exception {
        try {
            if (mMethod == METHOD_POST) {
                mHttpRequest = new HttpPost();
            } else {
                mHttpRequest = new HttpGet();
            }

            mHttpRequest.setURI(new URI(url));

            if (param != null && mMethod == METHOD_POST) {
                MultipartEntity entity = new MultipartEntity();

                List<Field> fields = DataParseUtils.getFields(param.getClass(), Object.class);
                for (Field field : fields) {
                    field.setAccessible(true);
                    if (field.get(param) != null) {
                        if (field.getType().equals(File.class)) {
                            entity.addPart(field.getName(), new FileBody((File) field.get(param)));
                        } else {
                            entity.addPart(
                                    field.getName(),
                                    new StringBody(String.valueOf(field.get(param)), Charset
                                            .forName(HTTP.UTF_8)));
                        }
                    }
                }

                ((HttpPost) mHttpRequest).setEntity(entity);
            }

            HttpClient httpClient = getHttpClient();
            HttpResponse response = httpClient.execute(mHttpRequest);

            if (mStoped)
                return null;

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                T result = null;

                if (returnType != null) {
                    mXStream.processAnnotations(returnType);
                    mXStream.autodetectAnnotations(true);
                    result = (T) mXStream.fromXML(response.getEntity().getContent());
                }

                if (mStoped)
                    return null;
                else
                    return result;

            } else {
                throw new SocketException("Status Code : "
                        + response.getStatusLine().getStatusCode());
            }

        } finally {
            mHttpRequest.abort();
        }
    }

    protected void onException(Exception e) {
        Log.e(TAG, e.getMessage(), e);
    }

    protected void initXStream() {
        mXStream = new XStream(new DomDriver()) {

            protected MapperWrapper wrapMapper(MapperWrapper next) {
                return new MapperWrapper(next) {

                    public boolean shouldSerializeMember(Class definedIn, String fieldName) {
                        try {
                            return definedIn != Object.class || realClass(fieldName) != null;
                        } catch (CannotResolveClassException cnrce) {
                            return false;
                        }
                    }
                };
            }
        };
    }
}