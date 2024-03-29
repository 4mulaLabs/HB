package com.example.heartbeat;

import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class HttpClient {
    private static final String TAG = "httpClient";

    TrustManager[] trustAllCerts;
    SSLContext sslContext;
    OkHttpClient.Builder builder;
    OkHttpClient client;

    public HttpClient() {
        trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
        };
        try {
            sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
        builder = new OkHttpClient.Builder();
        builder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0]);
        builder.hostnameVerifier((hostname, session) -> true);
        client = builder.build();
    }

    private JSONObject randomizeUniqueAttrs(String requestContent) throws JSONException {
        JSONObject updatedRequestContentObject = new JSONObject(requestContent);

        String remoteSessionId = UUID.randomUUID().toString().substring(0,32);
        updatedRequestContentObject.put("remoteSessionId", remoteSessionId);

        JSONObject attributes = (JSONObject) updatedRequestContentObject.get("attributes");
        long timestamp = System.currentTimeMillis();
        attributes.put("toolTimestamp", timestamp);
        updatedRequestContentObject.put("attributes", attributes);

        return updatedRequestContentObject;
    }

    public void send(String url, String requestContent, Callback callback) throws JSONException {
        JSONObject updatedRequestContentObject = randomizeUniqueAttrs(requestContent);

        String updatedRequestContentString = updatedRequestContentObject.toString();
        Log.d(TAG, "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        Log.d(TAG, updatedRequestContentString);
        RequestBody body = RequestBody.create(updatedRequestContentString.getBytes(StandardCharsets.UTF_8));
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        client.newCall(request).enqueue(callback);
    }


}
