package com.spoon.maeumon.feature.webView.di;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanSettings;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.provider.Settings;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
// import com.google.firebase.messaging.FirebaseMessaging; // Firebase 나중에 추가
import com.spoon.maeumon.BuildConfig;
import com.spoon.maeumon.feature.ble.BLEScanService;
import com.spoon.maeumon.feature.data.BLEData;
import com.spoon.maeumon.feature.webView.ui.WebViewActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// JS에서 "AndroidBridge"라는 이름으로 접근 가능한 인터페이스
public class WebAppInterface {

   // 우리 회사 단말이 송신하는 BLE 제조사 데이터 ID와 이름
   private static final int SCOOP_MANUFACTURER_ID = 0xFFFF; // 임의 할당(충돌 없도록 고정)
   private static final String SCOOP_BLE_NAME = "SCOOP-BUS";

   private final Context context;
   private final WebView webView;

   private BLEScanService bleScanService;
   private final Object bleBufferLock = new Object();
   private Map<Integer, BLEData> bleBuffer = new HashMap<>();
   private final Handler bleBufferHandler = new Handler(Looper.getMainLooper());
   private Runnable bleFlushTask;

   /* 위치 */
   private FusedLocationProviderClient fusedLocationClient;
   private LocationCallback locationCallback;
   private boolean isTracking = false;

   /* NFC */
   private NfcAdapter nfcAdapter;

   // 알림 권한 허용 callbackId 임시 저장
   public String pendingNotificationCallbackId = null;

   public WebAppInterface(Context context, WebView webView) {
      this.context = context;
      this.webView = webView;
   }

   /* 위치 */
   @JavascriptInterface
   public void startLocationTracking() {
      if (isTracking) {
         Log.d("WebAppInterface", "Already tracking location");
         return;
      }

      fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

      if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
              != PackageManager.PERMISSION_GRANTED) {
         Log.e("WebAppInterface", "Location permission not granted");
         return;
      }

      LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
              .setMinUpdateIntervalMillis(1000)
              .setMaxUpdateDelayMillis(3000)
              .build();

      locationCallback = new LocationCallback() {
         @Override
         public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null) return;

            Location location = locationResult.getLastLocation();
            if (location == null) return;

            boolean isFake = false;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
               // Android 12 이상
               isFake = location.isMock();
            } else {
               // Android 11 이하
               isFake = location.isFromMockProvider();

               if (!isFake) {
                  try {
                     int mockLocationSetting = Settings.Secure.getInt(
                             context.getContentResolver(),
                             Settings.Secure.ALLOW_MOCK_LOCATION
                     );
                     if (mockLocationSetting != 0) {
                        isFake = true;
                     }
                  } catch (Settings.SettingNotFoundException e) {
                     Log.w("WebAppInterface", "ALLOW_MOCK_LOCATION not found", e);
                  }
               }
            }

            if (isFake && !BuildConfig.DEBUG) {
               Log.w("WebAppInterface", "Mock location detected. Not sending to WebView.");
               // 필요하면 JS로 오류 전달 가능
               // sendLocationErrorToWebView("mock_location_detected");
               return; // 위치 전송하지 않음
            }

            double lat = location.getLatitude();
            double lng = location.getLongitude();
            float accuracy = location.getAccuracy();
            long timestamp = location.getTime();

            Log.d("test", "lat: " + lat + "\nlng: " + lng + "\naccuracy: " + accuracy + "\ntimestamp: " + timestamp);

            sendLocationToWebView(lat, lng, accuracy, timestamp);
         }
      };

      fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
      isTracking = true;
      Log.d("WebAppInterface", "Location tracking started");
   }

   @JavascriptInterface
   public void stopLocationTracking() {
      if (!isTracking) return;

      if (locationCallback != null) {
         fusedLocationClient.removeLocationUpdates(locationCallback);
      }

      isTracking = false;
      Log.d("WebAppInterface", "Location tracking stopped");
   }

   private void sendLocationToWebView(double lat, double lng, float accuracy, long timestamp) {
      if (webView == null) return;

      String jsCode = String.format(
              "if(window.__handleNativeLocation__) { window.__handleNativeLocation__({ lat: %f, lng: %f, accuracy: %f, timestamp: %d }); }",
              lat, lng, accuracy, timestamp
      );

      Activity activity = (Activity) context;
      activity.runOnUiThread(() -> {
         safeEvaluateJavascript(jsCode, value -> Log.d("WebAppInterface", "Location JS 실행 완료"));
      });

      Log.d("WebAppInterface", "Location sent: (" + lat + "," + lng + ") accuracy: " + accuracy);
   }

   @JavascriptInterface
   public void sendCustomerPhone() {
      SharedPreferences prefPhone =
              context.getSharedPreferences("phone",
                      Context.MODE_PRIVATE);
      String savedNumber =
              prefPhone.getString("phone", null);
      Log.d("WebAppInterface",
              "sendCustomerPhone 호출됨, 번호: " +
                      savedNumber);
      sendPhoneToWebView(savedNumber);
   }
   private void sendPhoneToWebView(String phone) {
      if (webView == null) return;

      String jsCode = String.format(
              "if(window.__handlePhonenumber__) { window.__handlePhonenumber__({ phone:'%s' }); }",
              phone
     );

      Activity activity = (Activity) context;
      activity.runOnUiThread(() -> {
         safeEvaluateJavascript(jsCode, value -> Log.d("WebAppInterface", "Phone JS 실행 완료"));
      });
   }

   @JavascriptInterface
   public void sendDeviceType() {
      sendTypeToWebView();
   }

   private void sendTypeToWebView() {
      if (webView == null) return;
      String userAgent = System.getProperty("http.agent");
      String jsCode = String.format(
              "if(window.__handleType__) { window.__handleType__({ device_type: '%s', app_version: '%s', device: 0 }); }",
              userAgent, BuildConfig.VERSION_NAME
      );

      Activity activity = (Activity) context;
      activity.runOnUiThread(() -> {
         safeEvaluateJavascript(jsCode, value -> Log.d("WebAppInterface", "Type JS 실행 완료"));
      });
   }

   @JavascriptInterface
   public void sendFCMToken() {

      SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
      String token = prefs.getString("fcm_token", null);
      if (BuildConfig.DEBUG) Log.d("TOKEN", "sendFCMToken: " + token);
      Activity activity = (Activity) context;
      activity.runOnUiThread(() -> {
         if (token != null) {
            sendTokenToWebView(token);
         } else {
            // Firebase 나중에 추가
            // 토큰 새로 가져오기
            // FirebaseMessaging.getInstance().getToken()
            //         .addOnCompleteListener(task -> {
            //            if (task.isSuccessful() && task.getResult() != null) {
            //               String newToken = task.getResult();
            //               // SharedPreferences에 저장
            //               prefs.edit().putString("fcm_token", newToken).apply();
            //               sendTokenToWebView(newToken);
            //            } else {
            //               sendNoTokenToWebView();
            //            }
            //         });
            sendNoTokenToWebView(); // 임시: Firebase 없이 처리
         }
      });
   }

   private void sendTokenToWebView(String token) {
      Log.d("FCM", "웹 뷰에 보내는 로직 실행");

      String escapedToken = token.replace("'", "\\'").replace("\n", "\\n");
      String jsCode = String.format(
              "if(window.__handleFCMToken__) { window.__handleFCMToken__('%s'); }",
              escapedToken
      );
      safeEvaluateJavascript(jsCode, value -> Log.d("WebViewDebug", "fcm 토큰 JS 실행 완료(토큰O): " + value));
   }

   private void sendNoTokenToWebView() {
      String jsCode = "console.log('[FCM] No token available');";
      safeEvaluateJavascript(jsCode, value -> Log.d("WebViewDebug", "fcm 토큰 JS 실행 완료(토큰x): " + value));
   }
   @JavascriptInterface
   public void statusBLE(int status) {
      String jsCode = String.format(
              "if(window.__handleBLEStatus__) { window.__handleBLEStatus__({ status: %d }); }",
              status
      );

      Activity activity = (Activity) context;
      activity.runOnUiThread(() -> {
         safeEvaluateJavascript(jsCode, value -> Log.d("WebAppInterface", "BLE status JS 실행 완료"));
      });
   }

   @JavascriptInterface
   public void startBeacon() {
      ((WebViewActivity) context).beaconOnOff(true);
   }

   @JavascriptInterface
   public void stopBeacon() {
      ((WebViewActivity) context).beaconOnOff(false);
   }

   @JavascriptInterface
   public void sendBeaconData(List<BLEData> list){
      if (webView == null) return;

      // JSON 문자열 만들어서 보내는 방식 추천
      JSONArray arr = new JSONArray();
      for (BLEData d : list) {
         JSONObject obj = new JSONObject();
         try {
            obj.put("routeId", d.getRouteId());
            obj.put("rssi", d.getRssi());
            obj.put("distance", d.getDistance());
         } catch (JSONException e) {
            e.printStackTrace();
         }
         arr.put(obj);
      }
      String jsCode = String.format(
              "if(window.__handleSendBeacon__) { window.__handleSendBeacon__(%s); }",
              arr.toString()
      );
      Log.d("WebAppInterface", "Beacon send 동작" + jsCode);
      Activity activity = (Activity) context;
      activity.runOnUiThread(() -> {
         safeEvaluateJavascript(jsCode, value -> Log.d("WebAppInterface", "Beacon send JS 실행 완료"));
      });
   }

   private void startBleBufferFlush() {
      stopBleBufferFlush();
      bleFlushTask = new Runnable() {
         @Override
         public void run() {
            List<BLEData> snapshot;
            synchronized (bleBufferLock) {
               if (bleBuffer.isEmpty()) {
                  bleBufferHandler.postDelayed(this, 1000);
                  return;
               }
               snapshot = new ArrayList<>(bleBuffer.values());
               bleBuffer.clear();
            }
            sendBLEToWebView(snapshot);
            bleBufferHandler.postDelayed(this, 1000);
         }
      };
      bleBufferHandler.postDelayed(bleFlushTask, 1000);
   }

   private void stopBleBufferFlush() {
      if (bleFlushTask != null) {
         bleBufferHandler.removeCallbacks(bleFlushTask);
      }
   }

   @JavascriptInterface
   public void startBLE() {
      bleBuffer.clear();
      startBleBufferFlush();

      bleScanService = new BLEScanService(context, new BLEScanService.BLEListener() {
         @Override
         public void offBluetoothListener() {

         }

         @Override
         public void missingPermission() {

         }

         @Override
         public void startScanListener() {

         }

         @Override
         public void stopScanListener() {

         }
      })
              // 필터는 콜백에서 소프트웨어로 처리 (스캔 응답까지 병합된 이름 사용)
              .setScanFilters(null)
              .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
              .setScanCallback(new ScanCallback() {
         @Override
         public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Integer routeId = extractRouteId(result);
            if (routeId == null) {
               return;
            }

            BLEData bleData = new BLEData();
            bleData.setRouteId(routeId);
            bleData.setRssi(result.getRssi());
            bleData.setDistance(estimateDistance(result, result.getRssi()));

            synchronized (bleBufferLock) {
               bleBuffer.put(routeId, bleData); // 동일 routeId는 최신으로 덮어씀
            }

         }
      });
      bleScanService.repeatScan(60_000);
   }

   @JavascriptInterface
   public void stopBLE() {
      if (bleScanService != null) {
         bleScanService.stopScan();
      }
      stopBleBufferFlush();
   }

   public void sendBLEToWebView(List<BLEData> list){
      if (webView == null) return;

      // JSON 문자열 만들어서 보내는 방식 추천
      JSONArray arr = new JSONArray();
      for (BLEData d : list) {
         JSONObject obj = new JSONObject();
         try {
            obj.put("routeId", d.getRouteId());
            obj.put("rssi", d.getRssi());
            obj.put("distance", d.getDistance());
         } catch (JSONException e) {
            e.printStackTrace();
         }
         arr.put(obj);
      }
      String jsCode = String.format(
              "if(window.__handleSendBLE__) { window.__handleSendBLE__(%s); }",
              arr.toString()
      );
      Log.d("WebAppInterface", "BLE send 동작" + jsCode);
      Activity activity = (Activity) context;
      activity.runOnUiThread(() -> {
         safeEvaluateJavascript(jsCode, value -> Log.d("WebAppInterface", "BLE send JS 실행 완료"));
      });
   }

   @JavascriptInterface
   public void statusGPS(int status) {
      String jsCode = String.format(
              "if(window.__handleGPSStatus__) { window.__handleGPSStatus__({ status: %d }); }",
              status
      );

      Activity activity = (Activity) context;
      activity.runOnUiThread(() -> {
         safeEvaluateJavascript(jsCode, value -> Log.d("WebAppInterface", "GPS status JS 실행 완료"));
      });
   }

   @JavascriptInterface
   public void statusNFC() {
      sendStatusNFC();
   }

   private void sendStatusNFC() {
      nfcAdapter =  NfcAdapter.getDefaultAdapter(context);
      String jsCode = String.format(
              "if(window.__handleNFCStatus__) { window.__handleNFCStatus__({ status: %d }); }",
              nfcAdapter == null ? 0 : 1
      );

      Activity activity = (Activity) context;
      activity.runOnUiThread(() -> {
         safeEvaluateJavascript(jsCode, value -> Log.d("WebAppInterface", "NFC status JS 실행 완료"));
      });
   }

   @JavascriptInterface
   public void startNFCScan() {
      // 스캐너 동작
      ((WebViewActivity) context).startNFC();
   }

   @JavascriptInterface
   public void sendNFCCodeToWebView(String code){
      String jsCode = String.format(
              "if(window.__handleSendNFCCode__) { window.__handleSendNFCCode__({ code: '%s' }); }",
              code
      );

      Activity activity = (Activity) context;
      activity.runOnUiThread(() -> {
         safeEvaluateJavascript(jsCode, value -> Log.d("WebAppInterface", "NFC code JS 실행 완료"));
      });
   }

   @JavascriptInterface
   public void stopNFCScan() {
      // 스캐너 정지
      ((WebViewActivity) context).stopNFC();
   }

   @JavascriptInterface
   public void statusCamera(int status) {
      String jsCode = String.format(
              "if(window.__handleCameraStatus__) { window.__handleCameraStatus__({ status: %d }); }",
              status
      );

      Activity activity = (Activity) context;
      activity.runOnUiThread(() -> {
         safeEvaluateJavascript(jsCode, value -> Log.d("WebAppInterface", "Camera status JS 실행 완료"));
      });
   }
   @JavascriptInterface
   public void startQRScan() {
      // QR 스캐너 동작
      Activity activity = (Activity) context;
      activity.runOnUiThread(() -> {
         if (activity instanceof WebViewActivity) {
            ((WebViewActivity) activity).startQrActivity();
         }
      });
   }

   public void sendQRCodeToWebView(String code) {
      // QR 스캐너로 받은 코드 웹에 전송
      String jsCode = String.format(
              "if(window.__sendQRCode__) { window.__sendQRCode__({ code: '%s' }); }",
              code
      );

      Activity activity = (Activity) context;
      activity.runOnUiThread(() -> {
         safeEvaluateJavascript(jsCode, value -> Log.d("WebAppInterface", "QR code JS 실행 완료" + code));
      });
   }

   @JavascriptInterface
   public void closeQRScan() {
      String jsCode = "if(window.__closeQRScan__) { window.__closeQRScan__({ status: 'close' }); }";

      Activity activity = (Activity) context;
      activity.runOnUiThread(() -> {
         safeEvaluateJavascript(jsCode, value -> Log.d("WebAppInterface", "close QR Scan JS 실행 완료"));
      });
   }

   @JavascriptInterface
   public void stopQRScan() {
      // QR 스캐너 정지
      Activity activity = (Activity) context;
      activity.runOnUiThread(() -> {
         if (activity instanceof WebViewActivity) {
            ((WebViewActivity) activity).stopQrActivity();
         }
      });
   }

   @JavascriptInterface
   public void getNotificationPermission(String callbackId) {
      Activity activity = (Activity) context;

      boolean granted = false;
      String status = "unknown";

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
         int permissionCheck = ContextCompat.checkSelfPermission(
                 context, Manifest.permission.POST_NOTIFICATIONS);

         // OS 알림 켜져 있는지 확인
         boolean osEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled();

         if (permissionCheck == PackageManager.PERMISSION_GRANTED && osEnabled) {
            granted = true;
            // 실제로 알림 설정이 켜져 있는지 확인
            status = NotificationManagerCompat.from(context).areNotificationsEnabled() ? "granted" : "granted";
            granted = NotificationManagerCompat.from(context).areNotificationsEnabled();
         } else {
            granted = false;
            status = "denied";
         }
      } else {
         // Android 12 이하: 권한 요청 불필요
         boolean osEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled();
         granted = osEnabled;
         status = osEnabled ? "granted" : "denied";
      }

      // 웹으로 전달
      sendNotificationPermissionResultToWebView(callbackId, status, granted);
   }

   @JavascriptInterface
   public void requestNotificationPermission(String callbackId) {
      Activity activity = (Activity) context;

      boolean osEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled();

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
         boolean permissionGranted = ContextCompat.checkSelfPermission(
                 context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;

         if (permissionGranted && osEnabled) {
            // 권한 + OS 알림 모두 허용
            sendNotificationPermissionResultToWebView(callbackId, "granted", true);
         } else {
            // 하나라도 꺼져있으면 앱 설정 화면 안내
            pendingNotificationCallbackId = callbackId;
            if (activity instanceof WebViewActivity) {
               ((WebViewActivity) activity).openNotificationSettings();
            }
         }
      } else {
         // 하나라도 꺼져있으면 앱 설정 화면 안내
         pendingNotificationCallbackId = callbackId;
         if (activity instanceof WebViewActivity) {
            ((WebViewActivity) activity).openNotificationSettings();
         }
      }
   }

   // 웹으로 알림 권한 상태 전달
   public void sendNotificationPermissionResultToWebView(String callbackId, String status, boolean enabled) {
      Activity activity = (Activity) context;
      activity.runOnUiThread(() -> {
         String jsCode = String.format(
                 "if(window.%s) { window.%s({ success: true, data: JSON.stringify({ status: '%s', enabled: %b }) }); }",
                 callbackId, callbackId, status, enabled
         );
         safeEvaluateJavascript(jsCode, value -> Log.d("WebAppInterface", "Notification permission JS sent"));
      });
   }

   private void safeEvaluateJavascript(String jsCode, ValueCallback<String> resultCallback) {
      Activity activity = (Activity) context;
      activity.runOnUiThread(() -> {
         try {
            if (webView != null) {
               webView.evaluateJavascript(jsCode, resultCallback);
            } else {
               Log.w("WebAppInterface", "WebView is null, JS call skipped");
            }
         } catch (IllegalStateException e) {
            Log.w("WebAppInterface", "WebView destroyed, JS call skipped", e);
         }
      });
   }

   @JavascriptInterface
   public void closeApp() {
      ((WebViewActivity) context).closeApp();
   }

   /**
    * 일반 BLE 광고 패킷에서 우리 회사 단말인지 확인하고 routeId를 추출한다.
    *
    * 기준:
    * 1) 광고에 제조사 데이터(SCOOP_MANUFACTURER_ID) 포함
    *    - iOS 비연결 광고에서는 로컬 네임이 빠질 수 있으므로 제조사 데이터를 우선 신뢰
    * 2) 이름이 실려 있으면 SCOOP_BLE_NAME인지 확인
    * 3) 제조사 데이터에서 routeId를 big-endian int로 추출
    *    - iOS는 manufacturer data에 회사 ID(0xFFFF)를 2바이트 앞에 넣어 송출하므로 이를 건너뛸 수 있어야 함
    */
   private Integer extractRouteId(ScanResult result) {
      ScanRecord record = result.getScanRecord();
      if (record == null) {
         return null;
      }

      // 디바이스 이름 확인 (SCOOP-BUS인지 검증)
      String name = record.getDeviceName();
      if (name == null && result.getDevice() != null) {
         if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return null;
         }
         name = result.getDevice().getName();
      }

      // 이름이 SCOOP-BUS가 아니면 무시
      if (!SCOOP_BLE_NAME.equals(name)) {
         return null;
      }

      // iOS 방식: Service UUID에서 RouteId 추출
      List<ParcelUuid> serviceUuids = record.getServiceUuids();
      if (serviceUuids != null && !serviceUuids.isEmpty()) {
         for (ParcelUuid parcelUuid : serviceUuids) {
            String uuidString = parcelUuid.getUuid().toString();

            try {
               String routeIdHex = uuidString.substring(0, 8);
               int routeId = (int) Long.parseLong(routeIdHex, 16);

               return routeId;
            } catch (Exception e) {
               Log.w("WebAppInterface", "Failed to parse routeId from UUID: " + uuidString, e);
            }
         }
      }

      // 기존 안드로이드 방식 (Manufacturer Data) - 안드로이드 간 통신용
      byte[] manufacturer = record.getManufacturerSpecificData(SCOOP_MANUFACTURER_ID);
      if (manufacturer != null && manufacturer.length >= 4) {
         return ((manufacturer[0] & 0xFF) << 24)
                 | ((manufacturer[1] & 0xFF) << 16)
                 | ((manufacturer[2] & 0xFF) << 8)
                 | (manufacturer[3] & 0xFF);
      }
      return null;
   }

   private double estimateDistance(ScanResult result, int rssi) {
      int txPower = 0; // 기본 참조값
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
         txPower = result.getTxPower() != ScanResult.TX_POWER_NOT_PRESENT
                 ? result.getTxPower()
                 : -59;
      }

      if (rssi == 0 || txPower == 0) {
         return -1;
      }

      double ratio = (txPower - rssi) / (10.0 * 2.0); // path-loss exponent ~2 (free space)
      return Math.pow(10, ratio);
   }
}
