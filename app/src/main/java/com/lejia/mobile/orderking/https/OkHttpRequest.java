package com.lejia.mobile.orderking.https;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * OkHttp网络访问插件
 *
 * @author HEKE
 * @2016年11月11日
 * @下午4:35:57
 */
public class OkHttpRequest {

    // mdiatype 这个需要和服务端保持一致
    private static final MediaType MEDIA_TYPE = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");
    // mdiatype 这个需要和服务端保持一致
    @SuppressWarnings("unused")
    private static final MediaType MEDIA_TYPE_MARKDOWN = MediaType.parse("text/x-markdown; charset=utf-8");
    private static final String TAG = OkHttpRequest.class.getSimpleName();
    // 单例引用
    private static volatile OkHttpRequest mInstance;
    // get请求
    public static final int TYPE_GET = 0;
    // post请求参数为json
    public static final int TYPE_POST_JSON = 1;
    // post请求参数为表单
    public static final int TYPE_POST_FORM = 2;
    // okHttpClient 实例
    private OkHttpClient mOkHttpClient;
    // 全局处理子线程和M主线程通信
    private Handler okHttpHandler;

    /**
     * 初始化RequestManager
     *
     * @param context
     * @param readTime 读取时长
     * @param connTime 链接时长
     */
    public OkHttpRequest(Context context, int readTime, int connTime) {
        // 等于-1使用默认时长
        if (readTime == -1) {
            readTime = 10;
        } else {
            readTime = Math.abs(readTime);
        }
        if (connTime == -1) {
            connTime = 10;
        } else {
            connTime = Math.abs(connTime);
        }
        // 初始化OkHttpClient
        mOkHttpClient = new OkHttpClient().newBuilder().connectTimeout(connTime, TimeUnit.SECONDS)// 设置超时时间
                .readTimeout(readTime, TimeUnit.SECONDS)// 设置读取超时时间
                .writeTimeout(10, TimeUnit.SECONDS)// 设置写入超时时间
                .build();
        // 初始化Handler
        okHttpHandler = new Handler(context.getMainLooper());
    }

    /**
     * 获取单例引用
     *
     * @return
     */
    public static OkHttpRequest getInstance(Context context) {
        OkHttpRequest inst = mInstance;
        if (inst == null) {
            synchronized (OkHttpRequest.class) {
                inst = mInstance;
                if (inst == null) {
                    inst = new OkHttpRequest(context.getApplicationContext(), -1, -1);
                    mInstance = inst;
                }
            }
        }
        return inst;
    }

    /**
     * okHttp同步请求统一入口
     *
     * @param url         接口地址
     * @param requestType 请求类型
     * @param paramsMap   请求参数
     */
    public void requestSyn(String url, int requestType, HashMap<String, String> paramsMap) {
        switch (requestType) {
            case TYPE_GET:
                requestGetBySyn(url, paramsMap);
                break;
            case TYPE_POST_JSON:
                requestPostBySyn(url, paramsMap);
                break;
            case TYPE_POST_FORM:
                requestPostBySynWithForm(url, paramsMap);
                break;
        }
    }

    /**
     * 户型识别Post请求数据接口
     *
     * @param url    接口地址
     * @param params 请求参数
     */
    public void postJsonSyn(String url, String params, ReqCallBack<String> callBack) {
        requestPostByAsyn2(url, params, callBack);
    }

    /**
     * okHttp get同步请求
     *
     * @param url       接口地址
     * @param paramsMap 请求参数
     */
    private void requestGetBySyn(String url, HashMap<String, String> paramsMap) {
        StringBuilder tempParams = new StringBuilder();
        try {
            // 处理参数
            int pos = 0;
            if (paramsMap != null) {
                for (String key : paramsMap.keySet()) {
                    if (pos > 0) {
                        tempParams.append("&");
                    }
                    // 对参数进行URLEncoder
                    tempParams.append(String.format("%s=%s", key, URLEncoder.encode(paramsMap.get(key), "utf-8")));
                    pos++;
                }
            }
            // 补全请求地址
            String requestUrl = String.format("%s?%s", url, tempParams.toString());
            // 创建一个请求
            Request request = addHeaders().url(requestUrl).build();
            // 创建一个Call
            final Call call = mOkHttpClient.newCall(request);
            // 执行请求
            final Response response = call.execute();
            response.body().string();
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    /**
     * okHttp post同步请求
     *
     * @param url       接口地址
     * @param paramsMap 请求参数
     */
    private void requestPostBySyn(String url, HashMap<String, String> paramsMap) {
        try {
            // 处理参数
            StringBuilder tempParams = new StringBuilder();
            int pos = 0;
            if (paramsMap != null) {
                for (String key : paramsMap.keySet()) {
                    if (pos > 0) {
                        tempParams.append("&");
                    }
                    tempParams.append(String.format("%s=%s", key, URLEncoder.encode(paramsMap.get(key), "utf-8")));
                    pos++;
                }
            }
            // 补全请求地址
            String requestUrl = url;
            // 生成参数
            String params = tempParams.toString();
            // 创建一个请求实体对象 RequestBody
            RequestBody body = RequestBody.create(MEDIA_TYPE, params);
            // 创建一个请求
            final Request request = addHeaders().url(requestUrl).post(body).build();
            // 创建一个Call
            final Call call = mOkHttpClient.newCall(request);
            // 执行请求
            Response response = call.execute();
            // 请求执行成功
            if (response.isSuccessful()) {
                // 获取返回数据 可以是String，bytes ,byteStream
                Log.e(TAG, "response ----->" + response.body().string());
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    /**
     * okHttp post同步请求表单提交
     *
     * @param url       接口地址
     * @param paramsMap 请求参数
     */
    private void requestPostBySynWithForm(String url, HashMap<String, String> paramsMap) {
        try {
            // 创建一个FormBody.Builder
            FormBody.Builder builder = new FormBody.Builder();
            if (paramsMap != null) {
                for (String key : paramsMap.keySet()) {
                    // 追加表单信息
                    builder.add(key, paramsMap.get(key));
                }
            }
            // 生成表单实体对象
            RequestBody formBody = builder.build();
            // 补全请求地址
            String requestUrl = url;
            // 创建一个请求
            final Request request = addHeaders().url(requestUrl).post(formBody).build();
            // 创建一个Call
            final Call call = mOkHttpClient.newCall(request);
            // 执行请求
            Response response = call.execute();
            if (response.isSuccessful()) {
                Log.e(TAG, "response ----->" + response.body().string());
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    /**
     * okHttp异步请求统一入口
     *
     * @param actionUrl   接口地址
     * @param requestType 请求类型
     * @param paramsMap   请求参数
     * @param callBack    请求返回数据回调
     * @param <T>         数据泛型
     **/
    public <T> Call requestAsyn(String actionUrl, int requestType, HashMap<String, String> paramsMap,
                                ReqCallBack<T> callBack) {
        Call call = null;
        switch (requestType) {
            case TYPE_GET:
                call = requestGetByAsyn(actionUrl, paramsMap, callBack);
                break;
            case TYPE_POST_JSON:
                call = requestPostByAsyn(actionUrl, paramsMap, callBack);
                break;
            case TYPE_POST_FORM:
                call = requestPostByAsynWithForm(actionUrl, paramsMap, callBack);
                break;
        }
        return call;
    }

    /**
     * okHttp get异步请求
     *
     * @param url       接口地址
     * @param paramsMap 请求参数
     * @param callBack  请求返回数据回调
     * @param <T>       数据泛型
     * @return
     */
    private <T> Call requestGetByAsyn(String url, HashMap<String, String> paramsMap, final ReqCallBack<T> callBack) {
        StringBuilder tempParams = new StringBuilder();
        try {
            int pos = 0;
            if (paramsMap != null) {
                for (String key : paramsMap.keySet()) {
                    if (pos > 0) {
                        tempParams.append("&");
                    }
                    tempParams.append(String.format("%s=%s", key, URLEncoder.encode(paramsMap.get(key), "utf-8")));
                    pos++;
                }
            }
            String requestUrl = String.format("%s?%s", url, tempParams.toString());
            final Request request = addHeaders().url(requestUrl).build();
            final Call call = mOkHttpClient.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    failedCallBack("访问失败", callBack);
                    Log.e(TAG, e.toString());
                }

                @SuppressWarnings("unchecked")
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String string = response.body().string();
                        Log.e(TAG, "response ----->" + string);
                        successCallBack((T) string, callBack);
                    } else {
                        failedCallBack("服务器错误", callBack);
                    }
                }
            });
            return call;
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        return null;
    }

    /**
     * okHttp post异步请求
     *
     * @param url       接口地址
     * @param paramsMap 请求参数
     * @param callBack  请求返回数据回调
     * @param <T>       数据泛型
     * @return
     */
    private <T> Call requestPostByAsyn(String url, HashMap<String, String> paramsMap, final ReqCallBack<T> callBack) {
        try {
            StringBuilder tempParams = new StringBuilder();
            int pos = 0;
            if (paramsMap != null) {
                for (String key : paramsMap.keySet()) {
                    if (pos > 0) {
                        tempParams.append("&");
                    }
                    tempParams.append(String.format("%s=%s", key, URLEncoder.encode(paramsMap.get(key), "utf-8")));
                    pos++;
                }
            }
            String params = tempParams.toString();
            RequestBody body = RequestBody.create(MEDIA_TYPE, params);
            String requestUrl = url;
            final Request request = addHeaders().url(requestUrl).post(body).build();
            final Call call = mOkHttpClient.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    failedCallBack("访问失败", callBack);
                    Log.e(TAG, e.toString());
                }

                @SuppressWarnings("unchecked")
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String string = response.body().string();
                        Log.e(TAG, "response ----->" + string);
                        successCallBack((T) string, callBack);
                    } else {
                        failedCallBack("服务器错误", callBack);
                    }
                }
            });
            return call;
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        return null;
    }

    /**
     * okHttp post异步请求
     *
     * @param url      接口地址
     * @param params   请求参数
     * @param callBack 请求返回数据回调
     * @param <T>      数据泛型
     * @return
     */
    private <T> Call requestPostByAsyn2(String url, String params, final ReqCallBack<T> callBack) {
        try {
            RequestBody body = RequestBody.create(MEDIA_TYPE, params);
            String requestUrl = url;
            final Request request = addHeaders().url(requestUrl).post(body).build();
            final Call call = mOkHttpClient.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    failedCallBack("访问失败", callBack);
                    Log.e(TAG, e.toString());
                }

                @SuppressWarnings("unchecked")
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String string = response.body().string();
                        Log.e(TAG, "response ----->" + string);
                        successCallBack((T) string, callBack);
                    } else {
                        failedCallBack("服务器错误", callBack);
                    }
                }
            });
            return call;
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        return null;
    }

    /**
     * okHttp post异步请求表单提交
     *
     * @param url       接口地址
     * @param paramsMap 请求参数
     * @param callBack  请求返回数据回调
     * @param <T>       数据泛型
     * @return
     */
    private <T> Call requestPostByAsynWithForm(String url, HashMap<String, String> paramsMap,
                                               final ReqCallBack<T> callBack) {
        try {
            FormBody.Builder builder = new FormBody.Builder();
            if (paramsMap != null) {
                for (String key : paramsMap.keySet()) {
                    builder.add(key, paramsMap.get(key));
                }
            }
            RequestBody formBody = builder.build();
            String requestUrl = url;
            final Request request = addHeaders().url(requestUrl).post(formBody).build();
            final Call call = mOkHttpClient.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    failedCallBack("访问失败", callBack);
                    Log.e(TAG, e.toString());
                }

                @SuppressWarnings("unchecked")
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String value = response.body().string();
                        successCallBack((T) value, callBack);
                    } else {
                        failedCallBack("服务器错误", callBack);
                    }
                }
            });
            return call;
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        return null;
    }

    /**
     * 下载文件
     *
     * @param fileUrl      文件url
     * @param destFilePath 存储目标目录
     */
    @SuppressWarnings("unchecked")
    public <T> void downLoadFile(String fileUrl, final String destFilePath, final ReqCallBack<T> callBack) {
        // 检测文件保存本地是否存在
        final File file = new File(destFilePath);
        if (file.exists()) {
            successCallBack((T) file, callBack);
            return;
        }
        final Request request = new Request.Builder().url(fileUrl).build();
        final Call call = mOkHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, e.toString());
                failedCallBack("下载失败", callBack);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                InputStream is = null;
                byte[] buf = new byte[4096];
                int len = 0;
                FileOutputStream fos = null;
                try {
                    long total = response.body().contentLength();
                    Log.e(TAG, "total------>" + total);
                    long current = 0;
                    is = response.body().byteStream();
                    fos = new FileOutputStream(file);
                    while ((len = is.read(buf)) != -1) {
                        current += len;
                        fos.write(buf, 0, len);
                        Log.e(TAG, "current------>" + current);
                    }
                    fos.flush();
                    successCallBack((T) file, callBack);
                } catch (IOException e) {
                    Log.e(TAG, e.toString());
                    failedCallBack("下载失败", callBack);
                } finally {
                    try {
                        if (is != null) {
                            is.close();
                        }
                        if (fos != null) {
                            fos.close();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, e.toString());
                    }
                }
            }
        });
    }

    /**
     * 上传文件
     *
     * @param url      接口地址
     * @param filePath 本地文件地址
     */
    public <T> void upLoadFile(String url, String filePath, final ReqCallBack<T> callBack) {
        // 补全请求地址
        String requestUrl = url;
        // 创建File
        File file = new File(filePath);
        // 创建RequestBody
        RequestBody body = RequestBody.create(MediaType.parse("application/octet-stream"), file);
        // 创建Request
        final Request request = new Request.Builder().url(requestUrl).post(body).build();
        final Call call = mOkHttpClient.newBuilder().writeTimeout(50, TimeUnit.SECONDS).build().newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, e.toString());
                failedCallBack("上传失败", callBack);
            }

            @SuppressWarnings("unchecked")
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String string = response.body().string();
                    Log.e(TAG, "response ----->" + string);
                    successCallBack((T) string, callBack);
                } else {
                    failedCallBack("上传失败", callBack);
                }
            }
        });
    }

    /**
     * 上传文件
     *
     * @param url       接口地址
     * @param paramsMap 参数
     * @param callBack  回调
     * @param <T>
     */
    public <T> void upLoadFile(String url, HashMap<String, Object> paramsMap, final ReqCallBack<T> callBack) {
        try {
            // 补全请求地址
            String requestUrl = url;
            MultipartBody.Builder builder = new MultipartBody.Builder();
            // 设置类型
            builder.setType(MultipartBody.FORM);
            // 追加参数
            for (String key : paramsMap.keySet()) {
                Object object = paramsMap.get(key);
                if (!(object instanceof File)) {
                    builder.addFormDataPart(key, object.toString());
                } else {
                    File file = (File) object;
                    builder.addFormDataPart(key, file.getName(), RequestBody.create(null, file));
                }
            }
            // 创建RequestBody
            RequestBody body = builder.build();
            // 创建Request
            final Request request = new Request.Builder().url(requestUrl).post(body).build();
            // 单独设置参数 比如读取超时时间
            final Call call = mOkHttpClient.newBuilder().writeTimeout(50, TimeUnit.SECONDS).build().newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, e.toString());
                    failedCallBack("上传失败", callBack);
                }

                @SuppressWarnings("unchecked")
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String string = response.body().string();
                        Log.e(TAG, "response ----->" + string);
                        successCallBack((T) string, callBack);
                    } else {
                        failedCallBack("上传失败", callBack);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    /**
     * 统一为请求添加头信息
     *
     * @return
     */
    private Request.Builder addHeaders() {
        Request.Builder builder = new Request.Builder().addHeader("Connection", "keep-alive").addHeader("platform", "2")
                .addHeader("phoneModel", Build.MODEL).addHeader("systemVersion", Build.VERSION.RELEASE)
                .addHeader("appVersion", "3.4.2");
        return builder;
    }

    /**
     * 统一同意处理成功信息
     *
     * @param result
     * @param callBack
     * @param <T>
     */
    private <T> void successCallBack(final T result, final ReqCallBack<T> callBack) {
        okHttpHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callBack != null) {
                    callBack.onReqSuccess(result);
                }
            }
        });
    }

    /**
     * 统一处理失败信息
     *
     * @param errorMsg
     * @param callBack
     * @param <T>
     */
    private <T> void failedCallBack(final String errorMsg, final ReqCallBack<T> callBack) {
        okHttpHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callBack != null) {
                    callBack.onReqFailed(errorMsg);
                }
            }
        });
    }

}
