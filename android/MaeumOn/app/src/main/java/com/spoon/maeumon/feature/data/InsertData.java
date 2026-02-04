package com.spoon.maeumon.feature.data;

import android.util.Log;

import com.spoon.maeumon.BuildConfig;
import com.spoon.maeumon.feature.splash.ui.SplashActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class InsertData {
    private static final String TAG = "InsertData";
    private Disposable disposable;
    private SplashActivity splashActivity = null;

    public InsertData() {
    }

    public InsertData(SplashActivity splashActivity) {
        this.splashActivity = splashActivity;
    }

    public void certExecute(Map<String, String> map){
        disposable = Observable.fromCallable(() -> {
            String serverURL = (BuildConfig.DEBUG ? TAGList.DEBUG_URL : TAGList.URL) + "sendCode";

            try {
                if (BuildConfig.DEBUG) Log.d(TAG, map.toString());
                URL url = new URL(serverURL);
                HttpsURLConnection httpsURLConnection = (HttpsURLConnection) url.openConnection();

                httpsURLConnection.setReadTimeout(5000);
                httpsURLConnection.setConnectTimeout(5000);
                httpsURLConnection.setRequestMethod("POST");
                String boundary = "===" + System.currentTimeMillis() + "===";
                String lineEnd = "\r\n";
                String twoHyphens = "--";
                httpsURLConnection.setRequestProperty("Connection", "Keep-Alive");
                httpsURLConnection.setRequestProperty("Cache-Control", "no-cache");
                httpsURLConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                httpsURLConnection.setDoOutput(true);
                httpsURLConnection.connect();

                DataOutputStream outputStream = new DataOutputStream(httpsURLConnection.getOutputStream());
                for(Map.Entry<String, String> entry : map.entrySet()) {
                    outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                    outputStream.writeBytes("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"" + lineEnd);
                    outputStream.writeBytes("Content-Type: text/plain; charset=" + StandardCharsets.UTF_8 + lineEnd);
                    outputStream.writeBytes(lineEnd);
                    if (entry.getValue() != null)
                        outputStream.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                    outputStream.writeBytes(lineEnd);
                }

                outputStream.flush();
                outputStream.close();

                int responseStatusCode = httpsURLConnection.getResponseCode();
                Log.d("aToken", "execute: " + responseStatusCode);

                InputStream inputStream;
                if (responseStatusCode == HttpsURLConnection.HTTP_OK) {
                    inputStream = httpsURLConnection.getInputStream();
                } else {
                    inputStream = httpsURLConnection.getErrorStream();
                }

                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                }
                bufferedReader.close();
                return sb.toString();
            }catch (Exception e) {
                return ("Error: " + e.getMessage());
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(s -> {
            if (BuildConfig.DEBUG) Log.d(TAG, "값 : " + s);

            try {
                JSONObject jsonObject = new JSONObject(s);
                DataParam<String> send = new DataParam<>();
                send.setState(jsonObject.getString(TAGList.STATE));
                send.setMessage(jsonObject.getString(TAGList.MESSAGE));
                send.setData(null);
                splashActivity.viewModel.setSendCode(send);
            }catch (Exception e){
            }
            disposable.dispose();
        });
    }
   public void codeExecute(Map<String, String> map){
      disposable = Observable.fromCallable(() -> {
         String serverURL = (BuildConfig.DEBUG ? TAGList.DEBUG_URL : TAGList.URL) + "verifyCode";

         try {
            if (BuildConfig.DEBUG) Log.d(TAG, map.toString());
            URL url = new URL(serverURL);
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) url.openConnection();

            httpsURLConnection.setReadTimeout(5000);
            httpsURLConnection.setConnectTimeout(5000);
            httpsURLConnection.setRequestMethod("POST");
            String boundary = "===" + System.currentTimeMillis() + "===";
            String lineEnd = "\r\n";
            String twoHyphens = "--";
            httpsURLConnection.setRequestProperty("Connection", "Keep-Alive");
            httpsURLConnection.setRequestProperty("Cache-Control", "no-cache");
            httpsURLConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            httpsURLConnection.setDoOutput(true);
            httpsURLConnection.connect();

            DataOutputStream outputStream = new DataOutputStream(httpsURLConnection.getOutputStream());
            for(Map.Entry<String, String> entry : map.entrySet()) {
               outputStream.writeBytes(twoHyphens + boundary + lineEnd);
               outputStream.writeBytes("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"" + lineEnd);
               outputStream.writeBytes("Content-Type: text/plain; charset=" + StandardCharsets.UTF_8 + lineEnd);
               outputStream.writeBytes(lineEnd);
               if (entry.getValue() != null)
                  outputStream.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
               outputStream.writeBytes(lineEnd);
            }

            outputStream.flush();
            outputStream.close();

            int responseStatusCode = httpsURLConnection.getResponseCode();
            Log.d("aToken", "execute: " + responseStatusCode);

            InputStream inputStream;
            if (responseStatusCode == HttpsURLConnection.HTTP_OK) {
               inputStream = httpsURLConnection.getInputStream();
            } else {
               inputStream = httpsURLConnection.getErrorStream();
            }

            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = bufferedReader.readLine()) != null) {
               sb.append(line);
            }
            bufferedReader.close();
            return sb.toString();
         }catch (Exception e) {
            return ("Error: " + e.getMessage());
         }
      }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(s -> {
         if (BuildConfig.DEBUG) Log.d(TAG, "값 : " + s);

         try {
            JSONObject jsonObject = new JSONObject(s);
            DataParam<String> send = new DataParam<>();
            send.setState(jsonObject.getString(TAGList.STATE));
            send.setMessage(jsonObject.getString(TAGList.MESSAGE));
            send.setData(null);
            splashActivity.viewModel.setVerifyCode(send);
         }catch (Exception e){
         }
         disposable.dispose();
      });
   }
}
