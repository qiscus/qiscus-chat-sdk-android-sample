package com.qiscus.mychatui.util;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Environment;

import androidx.annotation.RequiresApi;

import com.qiscus.jupuk.JupukConst;
import com.qiscus.sdk.chat.core.QiscusCore;
import com.qiscus.sdk.chat.core.data.remote.QiscusApi;
import com.qiscus.sdk.chat.core.data.remote.QiscusInterceptor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import rx.Emitter;
import rx.Observable;
import rx.exceptions.OnErrorThrowable;

/**
 * Created on : 08/01/21
 * Author     : mmnuradityo
 * GitHub     : https://github.com/mmnuradityo
 */
public class CustomDownloaderFileUtils {

    @SuppressLint("RestrictedApi")
    public static final int REQUEST_CODE_PHOTO = JupukConst.REQUEST_CODE_PHOTO;
    @SuppressLint("RestrictedApi")
    public static final int REQUEST_CODE_DOC = JupukConst.REQUEST_CODE_DOC;
    @SuppressLint("RestrictedApi")
    public static final String KEY_SELECTED_MEDIA = JupukConst.KEY_SELECTED_MEDIA;
    @SuppressLint("RestrictedApi")
    public static final String KEY_SELECTED_DOCS = JupukConst.KEY_SELECTED_DOCS;
    public static final int TAKE_PICTURE_REQUEST = 3;
    private static final int JUPUK_MEDIA_PICKER = 17;
    private static final int JUPUK_DOC_PICKER = 18;

    private static String generateExtension(String rawFileName, Headers headers) {
        if (rawFileName.lastIndexOf(".") > -1) return rawFileName;

        final String disposition = headers.get("Content-Disposition");
        final String fileName;
        if (disposition != null) {
            fileName = disposition.replaceFirst("(?i)^.*filename=\"?([^\"]+)\"?.*$", "$1");
        } else {
            fileName = rawFileName;
        }
        if (fileName.lastIndexOf(".") > -1) return fileName;

        final String type = headers.get("Content-Type");
        if (type == null) return fileName;
        final int index = type.lastIndexOf("/");
        if (index == -1) return fileName;

        return fileName + type.substring(index).replaceFirst("/", ".");
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static Observable<File> downloadFile(
            String environtment, String url, String rawFileName, QiscusApi.ProgressListener progressListener
    ) {
        return Observable.create(subscriber -> {
            String fileName = rawFileName;
            InputStream inputStream = null;
            FileOutputStream fos = null;
            try {
                Request request = new Request.Builder().url(url).build();

                Response response = new OkHttpClient.Builder().
                        connectTimeout(60L, TimeUnit.SECONDS)
                        .readTimeout(60L, TimeUnit.SECONDS)
                        .addInterceptor(QiscusInterceptor::headersInterceptor)
                        .addInterceptor(
                                QiscusInterceptor.makeLoggingInterceptor(QiscusCore.getChatConfig().isEnableLog())
                        )
                        .build()
                        .newCall(request)
                        .execute();

                fileName = generateExtension(fileName, response.headers());
                File output = generateFileToOutput(generateDirectory(environtment), fileName);
                fos = new FileOutputStream(output.getPath());

                if (!response.isSuccessful()) {
                    throw new IOException();
                } else {
                    ResponseBody responseBody = response.body();
                    long fileLength = responseBody.contentLength();

                    inputStream = responseBody.byteStream();
                    byte[] buffer = new byte[4096];
                    long total = 0;
                    int count;
                    while ((count = inputStream.read(buffer)) != -1) {
                        total += count;
                        long totalCurrent = total;
                        if (fileLength > 0) {
                            progressListener.onProgress((totalCurrent * 100 / fileLength));
                        }
                        fos.write(buffer, 0, count);
                    }
                    fos.flush();

                    subscriber.onNext(output);
                    subscriber.onCompleted();
                }
            } catch (Exception e) {
                throw OnErrorThrowable.from(OnErrorThrowable.addValueAsLastCause(e, url));
            } finally {
                try {
                    if (fos != null) {
                        fos.close();
                    }
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (IOException ignored) {
                    //Do nothing
                }
            }
        }, Emitter.BackpressureMode.BUFFER);
    }

    private static File generateFileToOutput(String pathToSave, String fileName) {
        String fileNameToResult = fileName;
        String path = pathToSave + fileName;
        File output = new File(path);

        int fileIndex;
        int fileCount = 1;
        while (output.exists()) {
            fileIndex = fileNameToResult.indexOf(".");
            if (fileIndex <= 3) return output;

            fileNameToResult = fileNameToResult.substring(0, fileCount == 1 ? fileIndex : fileIndex - 3)
                    + "(" + fileCount + ")"
                    + fileNameToResult.substring(fileIndex);

            path = pathToSave + fileNameToResult;
            output = new File(path);
            fileCount++;
        }

        return output;
    }

   /* private static Response headersInterceptor(Interceptor.Chain chain) throws IOException {
        Request.Builder builder = chain.request().newBuilder();
        JSONObject jsonCustomHeader = QiscusCore.getCustomHeader();

        builder.addHeader("QISCUS-SDK-APP-ID", QiscusCore.getAppId());
        builder.addHeader("QISCUS-SDK-TOKEN", QiscusCore.hasSetupUser() ? QiscusCore.getToken() : "");
        builder.addHeader("QISCUS-SDK-USER-EMAIL", QiscusCore.hasSetupUser() ? QiscusCore.getQiscusAccount().getEmail() : "");
        if (QiscusCore.getIsBuiltIn()) {
            builder.addHeader("QISCUS-SDK-VERSION", "ANDROID_" +
                    BuildConfig.CHAT_BUILT_IN_VERSION_MAJOR + "." +
                    BuildConfig.CHAT_BUILT_IN_VERSION_MINOR + "." +
                    BuildConfig.CHAT_BUILT_IN_VERSION_PATCH);
        } else {
            builder.addHeader("QISCUS-SDK-VERSION", "ANDROID_" +
                    "1.4.0-beta.2");
        }
        builder.addHeader("QISCUS-SDK-PLATFORM", "ANDROID");
        builder.addHeader("QISCUS-SDK-DEVICE-BRAND", Build.MANUFACTURER);
        builder.addHeader("QISCUS-SDK-DEVICE-MODEL", Build.MODEL);
        builder.addHeader("QISCUS-SDK-DEVICE-OS-VERSION", BuildVersionUtil.OS_VERSION_NAME);

        if (jsonCustomHeader != null) {
            Iterator<String> keys = jsonCustomHeader.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                try {
                    Object customHeader = jsonCustomHeader.get(key);
                    if (customHeader != null) {
                        builder.addHeader(key, customHeader.toString());
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        Request req = builder.build();

        return chain.proceed(req);
    }*/

    public static String generateDirectory(String environtment) {
        File file = new File(Environment.getExternalStoragePublicDirectory(environtment)
                .getAbsolutePath() + "/Multichannel");
        if (!file.exists()) {
            file.mkdir();
        }
        return file.getAbsolutePath() + File.separator;
    }

}
