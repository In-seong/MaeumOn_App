package com.spoon.maeumon.feature.webView.ui;

import static com.spoon.maeumon.feature.common.value.WebConstants.DEBUG_WebView_URL;
import static com.spoon.maeumon.feature.common.value.WebConstants.WebView_URL;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.webkit.ValueCallback;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.spoon.maeumon.BuildConfig;
import com.spoon.maeumon.R;
import com.spoon.maeumon.databinding.ActivityWebviewBinding;
import com.spoon.maeumon.feature.common.ui.BaseActivity;
import com.spoon.maeumon.feature.common.value.PermissionRequestCodes;
import com.spoon.maeumon.feature.data.BLEData;
import com.spoon.maeumon.feature.nfc.ui.NfcBottomSheet;
import com.spoon.maeumon.feature.nfc.viewModel.NfcViewModel;
import com.spoon.maeumon.feature.permission.util.PermissionManager;
import com.spoon.maeumon.feature.splash.ui.SplashActivity;
import com.spoon.maeumon.feature.webView.di.WebAppInterface;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.Region;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WebViewActivity extends BaseActivity {
   private static final String UUID = "e45c4963-5384-5856-a0ff-09f8a9901938";
   private static final String UID = "scoopRegion";
   private static final String BEACON_PARSER = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";

   /* UI */
   private WebView webView;   // 웹뷰 컴포넌드
   private ActivityWebviewBinding binding;

   /* UX */
   private long backPressedTime = 0; // 뒤로가기 2회 체크

   /* 쿠키 */
   CookieManager cookieManager;

   private NfcBottomSheet bottomSheet;

   private NfcAdapter nfcAdapter;

   /* JS와 연결 */
   /* 웹뷰 */
   private WebAppInterface webAppInterface;

   private GeolocationPermissions.Callback geoCallback;  // JS가 위치 접근을 시도할 때 요청에 대한 응답을 반환할 수 있는 callback을 전달함
   private String geoOrigin;  // 어떤 도메인이 위치 권한을 요청했는지, 해당 페이지의 url

   /* 파일 업로드 (WebView <input type="file">) */
   private ValueCallback<Uri[]> fileUploadCallback;
   private ActivityResultLauncher<Intent> fileChooserLauncher;
   private Uri cameraPhotoUri; // 카메라 촬영 사진 URI

   /* QR 스캔 */
   private ActivityResultLauncher<Intent> qrActivityLauncher;  // QR 실행 결과

   /* 앱 권한 */
   private PermissionManager pm; // 백그라운드 -> 포그라운드 전환 시 권한 체크

   /* Ble 스캔 */
   private static final String TAG = "BLE";

   /* Nfc */
   private NfcViewModel nfcViewModel;

   /* 네트워크 오류 */
   private boolean hasLoadError = false;
   private boolean shouldSendFCM = true;  //fcm을 보내야 함

   /* Beacon */
   private Region scoopRegion;
   private BeaconManager beaconManager;

   /* 위치 */
   FusedLocationProviderClient fusedLocationClient;
   private LocationCallback locationCallback;

   /* 블루투스 */
   private ActivityResultLauncher<Intent> bluetoothLauncher;

   /* os 기능 상태 체크 */
   private BroadcastReceiver networkReceiver;
   private BroadcastReceiver bluetoothReceiver;
   private BroadcastReceiver locationReceiver;

   private ConnectivityManager connectivityManager;
   private ConnectivityManager.NetworkCallback networkCallback;

   // 모달 변수
   private Dialog networkDialog;

   // 각 모달 표시 여부
   private boolean isNetworkDialogShowing = false;

   private boolean isFakeGpsDialogShown = false;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      /* UI */
      binding = ActivityWebviewBinding.inflate(getLayoutInflater());
      setContentView(binding.getRoot());

      // Android 15(API 35) 이상에서만 EdgeToEdge 적용
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
         EdgeToEdge.enable(this);
         applyWindowInsets();
      }

      setUIInitial();
      setObserve();
      setupFileChooserLauncher();
      setupQrActivityLauncher();

      setBeacon();

      clearAllWebViewCookies();
      setupCookie();
      setupWebView();
      setErrorLayoutUI();

      /* UX */
      setupBackPressedHandler(); // 뒤로가기 버튼 핸들링

      /* 권한 */
      pm = new PermissionManager(this);

      // 인터넷 / 위치 / 블루투스 체크
      connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
      setupBluetoothLauncher();

      // 인터넷
      initNetworkReceivers();
      initBluetoothReceiver();
      initLocationReceiver();

      checkInitialServices();
      Log.d("check", "onCreate called");
   }

   @Override
   public void onStart() {
      super.onStart();
      LocalBroadcastManager.getInstance(this)
              .registerReceiver(fcmTokenReceiver, new IntentFilter("FCM_TOKEN_RECEIVED"));
   }

   @Override
   public void onResume() {
      super.onResume();

      Log.d("check", "onResume called");

      // 권한 체크
      if (!pm.isAllGranted()) {
         Log.d("check", "권한 미허용 → SplashActivity로 이동");
         Intent intent = new Intent(this, SplashActivity.class);
         intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
         startActivity(intent);
         finish();
         return;
      }

      // 위치 권한 체크
      if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
              != PackageManager.PERMISSION_GRANTED) {
         return; // 권한 없음
      }

      // fusedLocationClient 초기화
      if (fusedLocationClient == null) {
         fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
      }

      // 이전 LocationCallback 제거
      if (locationCallback != null) {
         fusedLocationClient.removeLocationUpdates(locationCallback);
      }

      // LocationCallback 필드 초기화
      locationCallback = new LocationCallback() {
         @Override
         public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null) return;

            Location location = locationResult.getLastLocation();
            if (location == null) return;

            boolean isFake = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && location.isMock())
                    || location.isFromMockProvider();

            if (isFake && !BuildConfig.DEBUG) {
               showFakeGpsDialogAndBlockActivity();
               // 위치 업데이트 중지
//               if (fusedLocationClient != null) {
//                  fusedLocationClient.removeLocationUpdates(locationCallback);
//               }
               return;
            }

//            // 정상 위치 확인 후 웹뷰 초기화 (한 번만)
//            if (webViewInitialized.compareAndSet(false, true)) {
//               initWebView();
//            }

            // 필요하면 업데이트 중지
//            if (fusedLocationClient != null) {
//               fusedLocationClient.removeLocationUpdates(locationCallback);
//            }
         }
      };

      // LocationRequest 설정
      LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
              .setMinUpdateIntervalMillis(1000)
              .setMaxUpdateDelayMillis(3000)
              .build();

      // 안전하게 위치 업데이트 요청
      if (fusedLocationClient != null && locationCallback != null) {
         fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
      }

      // 쿠키 flush
      if (cookieManager != null) {
         cookieManager.flush();
      }

      // 알림 권한 JS 전달
      if (webAppInterface != null && webAppInterface.pendingNotificationCallbackId != null) {
         String callbackId = webAppInterface.pendingNotificationCallbackId;
         boolean granted = ContextCompat.checkSelfPermission(
                 this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;

         webAppInterface.sendNotificationPermissionResultToWebView(
                 callbackId,
                 granted ? "granted" : "denied",
                 granted
         );

         webAppInterface.pendingNotificationCallbackId = null;
      }
   }

   @Override
   protected void onPause() {
      super.onPause();
      if (fusedLocationClient != null && locationCallback != null) {
         fusedLocationClient.removeLocationUpdates(locationCallback);
      }
   }

   @Override
   protected void onStop() {
      super.onStop();
      // MUST-CHECK
      if (cookieManager != null) {
         cookieManager.flush(); // 백그라운드 전환 시 한 번만
      }
      LocalBroadcastManager.getInstance(this)
              .unregisterReceiver(fcmTokenReceiver);
   }

   @Override
   protected void onDestroy() {
      // 쿠키 저장 (super.onDestroy() 이전에 호출)
      if (cookieManager != null) {
         cookieManager.flush();
      }

      // NetworkCallback 해제
      if (networkCallback != null && connectivityManager != null) {
         connectivityManager.unregisterNetworkCallback(networkCallback);
         networkCallback = null;
      }

      if (networkReceiver != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
         unregisterReceiver(networkReceiver);
      }

      if (networkDialog != null && networkDialog.isShowing()) networkDialog.dismiss();

      if (bluetoothReceiver != null) {
         unregisterReceiver(bluetoothReceiver);
      }

      if (locationReceiver != null) {
         unregisterReceiver(locationReceiver);
      }

      // 보안: Interface 제거
      if (webView != null) {
         webView.removeJavascriptInterface("AndroidBridge");
      }
      if (fusedLocationClient != null && locationCallback != null) {
         fusedLocationClient.removeLocationUpdates(locationCallback);
      }
      binding = null;
      if (webView != null) {
         webView.destroy();
         webView = null; // 참조 해제
      }
      super.onDestroy();
   }

   // ====
   private void checkInitialServices() {
      if (!isNetworkConnected()) {
         showNetworkDialog();
      }
      new Handler(Looper.getMainLooper()).postDelayed(() -> {
         webAppInterface.statusBLE(isBluetoothOn() ? 1 : 0);
      }, 600);
      if (!isBluetoothOn()) {
         Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
         bluetoothLauncher.launch(enableBtIntent);
      }
      new Handler(Looper.getMainLooper()).postDelayed(() -> {
         webAppInterface.statusGPS(isLocationEnabled() ? 1 : 0);
      }, 600);

      if (!isLocationEnabled()) {
         openLocationSettings();
      }

      new Handler(Looper.getMainLooper()).postDelayed(() -> {
         webAppInterface.statusCamera(isCameraAvailable() ? 1 : 0);
      }, 600);
   }

   private boolean isBluetoothOn() {
      BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
      if (bluetoothAdapter == null) {
         // 블루투스를 지원하지 않는 기기면 꺼진 것으로 간주
         return false;
      }
      return bluetoothAdapter.isEnabled();
   }

   // ===============================
   private void showNetworkDialog() {
      if (isNetworkDialogShowing) return;
      isNetworkDialogShowing = true;

      networkDialog = new Dialog(this);
      networkDialog.setContentView(R.layout.dialog_permission_rerquired);
      if (networkDialog.getWindow() != null) {
         networkDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
      }

      ImageView icon = networkDialog.findViewById(R.id.permission_dialog_icon);
      TextView msg = networkDialog.findViewById(R.id.permission_dialog_message);

      icon.setImageResource(R.drawable.wifi_slash);
      msg.setText("인터넷 연결이 꺼져 있습니다.\n앱을 사용하기 위해 인터넷 연결을 확인해주세요.");

      networkDialog.setCancelable(false);
      networkDialog.show();

      networkDialog.setOnKeyListener((dialog, keyCode, event) -> {
         if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            finishAffinity();
            return true;
         }
         return false;
      });

      networkDialog.setOnDismissListener(dialog -> isNetworkDialogShowing = false);
   }

   private void dismissNetworkDialog() {
      if (networkDialog != null && networkDialog.isShowing()) {
         networkDialog.dismiss();
      }
   }


   //=================================
   // 네트워크 변경
   private void initNetworkReceivers() {
      // Android 7.0이 기준
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
         networkReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
               if (intent.getAction() != null && intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                  if (isNetworkConnected()) {
                     dismissNetworkDialog();
                  } else {
                     showNetworkDialog();
                  }
               }
            }
         };
         IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
         registerReceiver(networkReceiver, filter);
      } else {
         networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
               Log.d("check", "인터넷 연결됨 확인");
               runOnUiThread(() -> {
                  if (!isFinishing() && !isDestroyed()) {
                     dismissNetworkDialog();
                  }
               });
            }

            @Override
            public void onLost(Network network) {
               Log.d("check", "인터넷 끊김 확인");
               runOnUiThread(() -> {
                  if (!isFinishing() && !isDestroyed()) {
                     showNetworkDialog();
                  }
               });
            }
         };
         connectivityManager.registerDefaultNetworkCallback(networkCallback);
      }
   }

   private boolean isNetworkConnected() {
      ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
      if (cm == null) return false;

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
         // API 23 이상
         Network network = cm.getActiveNetwork();
         if (network == null) return false;

         NetworkCapabilities nc = cm.getNetworkCapabilities(network);
         return nc != null && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
      } else {
         // API 23 미만
         NetworkInfo ni = cm.getActiveNetworkInfo();
         return ni != null && ni.isConnected();
      }
   }

   //블루투스 상태 변경 감지
   private void initBluetoothReceiver() {
      bluetoothReceiver = new BroadcastReceiver() {
         @Override
         public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
               int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

               runOnUiThread(() -> {
                  if (!isFinishing() && !isDestroyed()) {
                     if (state == BluetoothAdapter.STATE_OFF) {
                        webAppInterface.statusBLE(0);

                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        bluetoothLauncher.launch(enableBtIntent);
                     } else if (state == BluetoothAdapter.STATE_ON) {
                        webAppInterface.statusBLE(1);
                     }
                  }
               });
            }
         }

      };
      ContextCompat.registerReceiver(
              this,
              bluetoothReceiver,
              new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
              ContextCompat.RECEIVER_NOT_EXPORTED
      );
   }

   /**
    * 기존 블루투스 요청용 런처 유지 (필요 시)
    */
   private void setupBluetoothLauncher() {
      bluetoothLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
         webAppInterface.statusBLE(result.getResultCode() == Activity.RESULT_OK ? 1 : 0);
         if (result.getResultCode() == Activity.RESULT_OK) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(1);
         } else {
            Toast.makeText(this, "블루투스를 켜야 앱을 사용할 수 있습니다.", Toast.LENGTH_LONG).show();
         }
      });
   }

   //=================================

   // 알림 권한 설정
   private final ActivityResultLauncher<Intent> notificationSettingsLauncher =
           registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
              if (webAppInterface.pendingNotificationCallbackId != null) {
                 boolean granted = ContextCompat.checkSelfPermission(
                         this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
                 webAppInterface.sendNotificationPermissionResultToWebView(
                         webAppInterface.pendingNotificationCallbackId,
                         granted ? "granted" : "denied",
                         granted
                 );
                 webAppInterface.pendingNotificationCallbackId = null;
              }
           });

   private boolean isNetworkAvailable() {
      ConnectivityManager connectivityManager =
              (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
      if (connectivityManager == null) return false;

      // API 29 (Android 10) 미만 호환성을 위해 NetworkInfo 사용
      // (현재 코드에 이미 import되어 있습니다)
      NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
      return activeNetworkInfo != null && activeNetworkInfo.isConnected();
   }

   /* mock gps block */
   private void showFakeGpsDialogAndBlockActivity() {
      if (isFakeGpsDialogShown) return; // 이미 다이얼로그가 보여지고 있으면 무시
      isFakeGpsDialogShown = true;

      AlertDialog dialog = new AlertDialog.Builder(this)
              .setTitle("위치 오류")
              .setMessage("모의위치 앱이 감지되어 앱을 사용하실 수 없습니다.")
              .setCancelable(false)
              .setPositiveButton("종료", (d, which) -> finishAffinity())
              .create();

      dialog.setOnShowListener(d -> {
         // Positive 버튼 가져오기
         Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
         // 원하는 색상 적용 (예: 빨간색)
         int color = ContextCompat.getColor(this, R.color.color_primary);
         positiveButton.setTextColor(color);
      });

      dialog.show();
   }

   /* UI */
   private void setErrorLayoutUI() {
      // 네트워크 - 다시 시도 버튼
      binding.btnRetry.setOnClickListener(v -> {
         if (isNetworkAvailable()) {
            shouldSendFCM = true;
            webView.reload(); // 또는 webView.loadUrl(WebView_URL);
         }
      });
   }
   /* 쿠키 지우기 */
   private void clearAllWebViewCookies() {
      CookieManager cm = CookieManager.getInstance();
      // 모든 쿠키 삭제 (비동기)
      cm.removeAllCookies(value ->
              Log.d("Cookie", "All WebView cookies cleared: " + value)
      );

      // 디스크 반영
      cm.flush();
   }

   /* 쿠키 설정 */
   private void setupCookie() {
      cookieManager = CookieManager.getInstance();
      cookieManager.setAcceptCookie(true); // 일반 쿠키
      cookieManager.setAcceptThirdPartyCookies(webView, true); // 3rd party 쿠키 (API 21+)
      cookieManager.flush();   // MUST-CHECK
   }

   public void openNotificationSettings() {
      Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
              .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
      notificationSettingsLauncher.launch(intent);
   }

   /* UI */
   private void applyWindowInsets() {
      View rootView = binding.getRoot();
      ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
         Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
         v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
         return WindowInsetsCompat.CONSUMED;
      });
   }

   private void setUIInitial() {
      webView = binding.webview;

      nfcAdapter = NfcAdapter.getDefaultAdapter(this);

      nfcViewModel = new ViewModelProvider(this).get(NfcViewModel.class);
   }

   private void setObserve() {
      // NFC 태그 읽기 관찰
      nfcViewModel.getNfcTag().observe(this, tagText -> {
         if (tagText != null) {
            if (bottomSheet != null)
               bottomSheet.dismiss();
            webAppInterface.sendNFCCodeToWebView(tagText);
            // 처리 끝난 후 초기화 -> API 호출 완료 후 초기화
            nfcViewModel.clearTag();
         }
      });
   }

   private void setupWebView() {
      // JS에서 "AndroidBridge"라는 이름으로 접근 가능하게 연결
      webAppInterface = new WebAppInterface(this, webView);
      webView.removeJavascriptInterface("AndroidBridge"); // 혹시 남아있다면 제거
      webView.addJavascriptInterface(webAppInterface, "AndroidBridge");

      WebSettings webSettings = webView.getSettings();
      /* 웹 실행 */
      webSettings.setJavaScriptEnabled(true);   // 웹뷰 내에서 자바스크립트 코드 실행 가능하게 함 (필수)
      webSettings.setDomStorageEnabled(true);   // 웹사이트에서 데이터를 클라이언트에 저장할 수 있게 함
      webSettings.setLoadsImagesAutomatically(true);  // 웹페이지 로드 시 이미지 자동 로드
      webSettings.setGeolocationEnabled(true);  //웹 페이지에서 사용자의 위치 정보 접근 가능
      /* 파일 접근 허용 (갤러리 등에서 선택한 파일을 WebView가 읽을 수 있도록) */
      webSettings.setAllowFileAccess(true);
      webSettings.setAllowContentAccess(true);
      /* 보안 네트워크 설정 */
      // 개발 중에는 항상 서버에서 최신 로드, 배포 시 LOAD_DEFAULT로 변경
      webSettings.setCacheMode(BuildConfig.DEBUG ? WebSettings.LOAD_NO_CACHE : WebSettings.LOAD_DEFAULT);
      webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW); // HTTPS만 허용
      /* 성능 */
      webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
      // MUST-CHECK (쿠키)
      webView.getSettings().setSaveFormData(true); // 세션/폼 데이터 저장 허용

      /* 브라우저 */
      webView.setWebChromeClient(new WebChromeClient() {
         // 파일 업로드 (<input type="file">) 처리
         @Override
         public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            if (fileUploadCallback != null) {
               fileUploadCallback.onReceiveValue(null);
            }
            fileUploadCallback = filePathCallback;

            // 1) 갤러리/파일 선택 Intent
            Intent contentIntent = new Intent(Intent.ACTION_GET_CONTENT);
            contentIntent.addCategory(Intent.CATEGORY_OPENABLE);
            contentIntent.setType("*/*");
            contentIntent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "application/pdf"});
            contentIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

            // 2) 카메라 촬영 Intent (FileProvider로 임시 파일 URI 생성)
            Intent cameraIntent = null;
            try {
               cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
               if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                  String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                  String fileName = "PHOTO_" + timeStamp + ".jpg";
                  File photoFile = new File(getExternalCacheDir(), fileName);
                  cameraPhotoUri = FileProvider.getUriForFile(
                          WebViewActivity.this,
                          getPackageName() + ".fileprovider",
                          photoFile);
                  cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraPhotoUri);
               }
            } catch (Exception e) {
               cameraIntent = null;
               cameraPhotoUri = null;
            }

            // 3) Chooser에 카메라를 추가 Intent로 포함
            Intent chooserIntent = Intent.createChooser(contentIntent, "파일 선택");
            if (cameraIntent != null) {
               chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{cameraIntent});
            }

            try {
               fileChooserLauncher.launch(chooserIntent);
            } catch (Exception e) {
               fileUploadCallback = null;
               cameraPhotoUri = null;
               return false;
            }
            return true;
         }

         // JS 위치 요청이 발생하면 호출됨
         @Override
         public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            // 콜백과 origin 저장 (권한 요청 후 사용)
            geoCallback = callback;
            geoOrigin = origin;

            // 위치 권한 확인
            if (!pm.isLocationGranted()) {
               ActivityCompat.requestPermissions(WebViewActivity.this,
                       new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PermissionRequestCodes.LOCATION_PERMISSION);
            } else {
               // 이미 권한 있으면 바로 허용
               // allow : origin이 Geolocation API 사용하는 것을 허락
               // retain : webview가 현재 표시 중인 webview 이후에도 권한을 유지할 것 인지
               // 성능 개선을 위해 retain을 true로 설정
               callback.invoke(origin, true, true);  //(origin, allow, retain)
            }

            // GPS 모드 체크 - 앱 위치 off한 경우 알림
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            if (!gpsEnabled) {
               runOnUiThread(() ->
                       Toast.makeText(WebViewActivity.this,
                               "GPS를 켜야 정확한 위치를 받을 수 있습니다.",
                               Toast.LENGTH_LONG).show()
               );
            }
         }
      });

      /* 로딩 */
      webView.setWebViewClient(new WebViewClient() {
         @Override
         public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();

            // intent:// 스킴 처리 (카카오톡 등)
            if (url.startsWith("intent://")) {
               try {
                  Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                  startActivity(intent);
                  return true;
               } catch (android.content.ActivityNotFoundException e) {
                  // 앱이 설치되지 않은 경우 마켓으로 이동
                  try {
                     Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                     String packageName = intent.getPackage();
                     if (packageName != null) {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=" + packageName)));
                     }
                  } catch (Exception ignored) {}
                  return true;
               } catch (Exception e) {
                  return true;
               }
            }

            // kakao:// 스킴 처리
            if (url.startsWith("kakao://") || url.startsWith("kakaotalk://")) {
               try {
                  startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                  return true;
               } catch (Exception e) {
                  return true;
               }
            }

            return false; // http/https는 WebView에서 처리
         }

         @Override
         public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            // Log.d("webViewTest", "onPageFinished 호출");   // 현재 웹에서 depth가 깊어지는 경우 새로운 Page로 바뀌는 것 같음
            if (!hasLoadError) {
               // 에러가 없는 일반적인 로딩
               hideErrorLayout();
            } else {
               hasLoadError = false;   // 다음 로딩을 위해 초기화
               return; // 오류 페이지라면 JS 호출 등 실행하지 않음
            }

            // MUST-CHECK
            String cookies = CookieManager.getInstance().getCookie(url);
            if (cookies != null && !cookies.isEmpty()) {
               cookieManager.setCookie(url, cookies);
               cookieManager.flush();
            }

            // WebView로 토큰 전달
            if (shouldSendFCM) {
               new Handler(Looper.getMainLooper()).postDelayed(() -> {
                  webAppInterface.sendCustomerPhone();
               }, 500);
               new Handler(Looper.getMainLooper()).postDelayed(() -> {
                  webAppInterface.sendDeviceType();
               }, 500);
               new Handler(Looper.getMainLooper()).postDelayed(() -> {
                  webAppInterface.sendFCMToken();
               }, 500);
               new Handler(Looper.getMainLooper()).postDelayed(() -> {
                  webAppInterface.statusBLE(isBluetoothOn() ? 1 : 0);
               }, 500);
               new Handler(Looper.getMainLooper()).postDelayed(() -> {
                  webAppInterface.statusNFC();
               }, 500);
               new Handler(Looper.getMainLooper()).postDelayed(() -> {
                  webAppInterface.statusCamera(isCameraAvailable() ? 1 : 0);
               }, 500);
               // 500ms 딜레이
            }
            shouldSendFCM = false;
         }

         @Override
         public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);

            int errorCode = error.getErrorCode();

            // 1. 서브리소스 에러는 무시 (지도 타일, 이미지, JS 등)
            if (!request.isForMainFrame()) {
               Log.w("WebViewError", "Sub-resource error ignored: " + request.getUrl());
               return;
            }

            // 2. 치명적인 에러만 처리
            if (!isFatalError(errorCode)) {
               Log.w("WebViewError", "Non-fatal error ignored: " + errorCode + " - " + error.getDescription());
               return;
            }

            // 치명적 에러일 때만 에러 화면 표시
            hasLoadError = true;

            runOnUiThread(() -> {
               Log.d("WebViewDebug", "showErrorLayout() 실행 (UI Thread)");
               if (binding == null) {
                  Log.e("BindingCheck", "showErrorLayout 호출 시 binding 객체가 null입니다.");
                  return;
               }
               showErrorLayout();
            });

            Log.e("WebViewError", "Error URL: " + request.getUrl());
            Log.e("WebViewError", "Error Code: " + errorCode);
            Log.e("WebViewError", "Description: " + error.getDescription());
         }

         @Override
         public void onReceivedHttpError(WebView view, WebResourceRequest request, android.webkit.WebResourceResponse errorResponse) {
            super.onReceivedHttpError(view, request, errorResponse);

            // 서브리소스 무시
            if (!request.isForMainFrame()) {
               return;
            }

            int statusCode = errorResponse.getStatusCode();

            // 메인 페이지의 404, 5xx 에러만 처리
            if (statusCode == 404 || statusCode >= 500) {
               hasLoadError = true;
               runOnUiThread(() -> {
                  if (binding != null) {
                     showErrorLayout();
                  }
               });
               Log.e("WebViewError", "HTTP Error: " + statusCode + " - " + request.getUrl());
            }
         }
      });

//       URL 로드
      webView.loadUrl(BuildConfig.DEBUG ? DEBUG_WebView_URL : WebView_URL);
   }

   /* UX */
   private void setupBackPressedHandler() {
      OnBackPressedCallback callback = new OnBackPressedCallback(true) {
         @Override
         public void handleOnBackPressed() {
            if (isNetworkDialogShowing) {
               finishAffinity();
               return;
            }
            if (webView != null && webView.canGoBack()) {
               webView.goBack();
            } else {
               long currentTime = System.currentTimeMillis();
               if (currentTime - backPressedTime < 2000) { // 2초 안에 두 번째 클릭
                  finish();
               } else {
                  Toast.makeText(WebViewActivity.this,
                          "뒤로가기를 한 번 더 누르면 종료됩니다",
                          Toast.LENGTH_SHORT).show();
                  backPressedTime = currentTime;
               }
            }
         }
      };
      getOnBackPressedDispatcher().addCallback(this, callback);
   }

   /* FCM */
   private final BroadcastReceiver fcmTokenReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
         String token = intent.getStringExtra("token");
         if (token != null && webAppInterface != null) {
            webAppInterface.sendFCMToken(); // JS 호출
         }
      }
   };

   // 위치(GPS) 상태 변경
   private void initLocationReceiver() {
      locationReceiver = new BroadcastReceiver() {
         @Override
         public void onReceive(Context context, Intent intent) {
            //delayedCheckServices();
            if (LocationManager.PROVIDERS_CHANGED_ACTION.equals(intent.getAction())) {
               webAppInterface.statusGPS(isLocationEnabled() ? 1 : 0);
               if (!isLocationEnabled()) {
                  openLocationSettings();
               }
            }
         }
      };
      ContextCompat.registerReceiver(
              this,
              locationReceiver,
              new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION),
              ContextCompat.RECEIVER_NOT_EXPORTED
      );
   }
   private ActivityResultLauncher<Intent> locationSettingsLauncher =
           registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
              // 설정화면에서 돌아왔을 때 위치가 켜졌는지 확인
              boolean enabled = isLocationEnabled();
              webAppInterface.statusGPS(enabled ? 1 : 0);
           });

   public void openLocationSettings() {
      Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
      locationSettingsLauncher.launch(intent);
   }

   /**
    * 위치 서비스 확인
    */
   private boolean isLocationEnabled() {
      LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
      if (locationManager != null) {
         return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                 locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
      }
      return false;
   }

   /* 에러 레이아웃 제어 */
   private void showErrorLayout() {
      binding.webview.setVisibility(View.GONE);
      binding.networkErrorLayout.setVisibility(View.VISIBLE);
   }

   private void hideErrorLayout() {
      binding.networkErrorLayout.setVisibility(View.GONE);
      binding.webview.setVisibility(View.VISIBLE);
   }

   /**
    * 치명적인 에러인지 판단
    * - 페이지 자체를 로드할 수 없는 경우만 true
    */
   private boolean isFatalError(int errorCode) {
      switch (errorCode) {
         case WebViewClient.ERROR_HOST_LOOKUP:       // -2: 호스트를 찾을 수 없음 (DNS 실패)
         case WebViewClient.ERROR_UNSUPPORTED_AUTH_SCHEME: // -3: 인증 스킴 미지원
         case WebViewClient.ERROR_AUTHENTICATION:    // -4: 인증 실패
         case WebViewClient.ERROR_CONNECT:           // -6: 서버 연결 실패
         case WebViewClient.ERROR_TIMEOUT:           // -8: 연결 시간 초과
         case WebViewClient.ERROR_FILE_NOT_FOUND:    // -14: 파일 없음
         case WebViewClient.ERROR_TOO_MANY_REQUESTS: // -15: 너무 많은 요청
            return true;

         // 치명적이지 않은 에러들
         case WebViewClient.ERROR_UNKNOWN:           // -1: 알 수 없는 에러 (보통 서브리소스)
         case WebViewClient.ERROR_BAD_URL:           // -12: 잘못된 URL (서브리소스일 가능성)
         case WebViewClient.ERROR_FAILED_SSL_HANDSHAKE: // -11: SSL 핸드셰이크 (서브리소스 SSL 문제)
         default:
            return false;
      }
   }

   private long notSearchTime = 0;
   private void setBeacon() {
      BeaconParser beaconParser = new BeaconParser().setBeaconLayout(BEACON_PARSER);
      beaconManager = BeaconManager.getInstanceForApplication(this);
      beaconManager.getBeaconParsers().add(beaconParser);
      beaconManager.setForegroundScanPeriod(1000L);
      beaconManager.setForegroundBetweenScanPeriod(0);
      beaconManager.setBackgroundScanPeriod(1000L);
      beaconManager.setBackgroundBetweenScanPeriod(0);
      try {
         beaconManager.setEnableScheduledScanJobs(false);
      } catch (Exception ignored) {
      }
      beaconManager.setNonBeaconLeScanCallback((device, rssi, scanRecord) -> {
      });
      beaconManager.setBackgroundModeInternal(false);

      beaconManager.removeAllRangeNotifiers();
      beaconManager.addRangeNotifier((beacons, region) -> {
         if (BuildConfig.DEBUG) Log.d(TAG, "setBeacon: " +( beacons != null ? beacons.size() : 0));
         if (beacons != null && !beacons.isEmpty()) {
            List<BLEData> arrayBle = new ArrayList<>();
            for (Beacon i : beacons) {
               BLEData bleData = new BLEData();
               int id = (i.getId2().toInt() * 10000) + i.getId3().toInt();
               double distance = i.getDistance();
               int rssi = i.getRssi();
               bleData.setRouteId(id);
               bleData.setRssi(rssi);
               bleData.setDistance(distance);
               arrayBle.add(bleData);
            }
            webAppInterface.sendBeaconData(arrayBle);
         } else {
            if (System.currentTimeMillis() - notSearchTime >= 5000) {
               notSearchTime = System.currentTimeMillis();
               if (BuildConfig.DEBUG) Log.d(TAG, "setBeacon: 재시작");
               restartBeacon();
            }
         }
      });
   }

   public void beaconOnOff(boolean isOn) {
      scoopRegion = new Region(UID, Identifier.parse(UUID), null, null);
      if (isOn) {
         beaconManager.startMonitoring(scoopRegion);
         beaconManager.startRangingBeacons(scoopRegion);
      } else {
         try {
            beaconManager.stopRangingBeacons(scoopRegion);
            beaconManager.stopMonitoring(scoopRegion);
         } catch (Exception e) {
            if (BuildConfig.DEBUG)
               e.printStackTrace();
         }
      }
   }

   private void restartBeacon() {
      scoopRegion = new Region(UID, Identifier.parse(UUID), null, null);
      try {
         beaconManager.stopRangingBeacons(scoopRegion);
         beaconManager.stopMonitoring(scoopRegion);
      } catch (Exception e) {
         if (BuildConfig.DEBUG)
            e.printStackTrace();
      }

      beaconManager.startMonitoring(scoopRegion);
      beaconManager.startRangingBeacons(scoopRegion);
   }

   public void startNFC() {
      if (nfcAdapter == null) {
         webAppInterface.statusNFC();
      } else if (!nfcAdapter.isEnabled() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM && !nfcAdapter.isReaderOptionEnabled())) {
         // NFC 꺼짐 활성화 필요
         Toast.makeText(this, !nfcAdapter.isEnabled() ? "NFC를 기본모드로 활성화 해주세요." : "NFC 태그 읽기/쓰기 기능을 활성화 해주세요.", Toast.LENGTH_SHORT).show();
         Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
         startActivity(intent);
      } else {
         Fragment existing = getSupportFragmentManager().findFragmentByTag("NFCBottomSheet");
         if (existing == null) {
            bottomSheet = new NfcBottomSheet();
            bottomSheet.show(getSupportFragmentManager(), "NFCBottomSheet");
         }
      }
   }

   public void stopNFC() {
      nfcViewModel.clearTag();
   }

   public void closeApp() {
      moveTaskToBack(true);
      finishAndRemoveTask();
      System.exit(0);
   }

   private void setupFileChooserLauncher() {
      fileChooserLauncher = registerForActivityResult(
              new ActivityResultContracts.StartActivityForResult(),
              result -> {
                 if (fileUploadCallback == null) return;

                 if (result.getResultCode() == Activity.RESULT_OK) {
                    Uri[] uris = null;
                    Intent data = result.getData();

                    if (data != null) {
                       // 복수 파일 선택
                       if (data.getClipData() != null) {
                          int count = data.getClipData().getItemCount();
                          uris = new Uri[count];
                          for (int i = 0; i < count; i++) {
                             uris[i] = data.getClipData().getItemAt(i).getUri();
                          }
                       }
                       // 단일 파일 선택
                       else if (data.getData() != null) {
                          uris = new Uri[]{data.getData()};
                       }
                    }

                    // 카메라 촬영: data가 null이지만 cameraPhotoUri에 사진 저장됨
                    if (uris == null && cameraPhotoUri != null) {
                       uris = new Uri[]{cameraPhotoUri};
                    }

                    fileUploadCallback.onReceiveValue(uris);
                 } else {
                    // 취소 시 null 전달 (안 하면 다음 클릭이 무시됨)
                    fileUploadCallback.onReceiveValue(null);
                 }
                 fileUploadCallback = null;
                 cameraPhotoUri = null;
              });
   }

   private void setupQrActivityLauncher() {
      qrActivityLauncher = registerForActivityResult(
              new ActivityResultContracts.StartActivityForResult(),
              result -> {
                 if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    String qrResult = result.getData().getStringExtra("qrResult");
                    if (qrResult != null && webAppInterface != null) {
                       webAppInterface.sendQRCodeToWebView(qrResult);
                    }
                 }
                 // QrActivity가 종료되면 항상 closeQRScan 호출
                 if (webAppInterface != null) {
                    webAppInterface.closeQRScan();
                 }
              });
   }

   public void startQrActivity() {
      Intent intent = new Intent(this, com.spoon.maeumon.feature.qr.ui.QrActivity.class);
      qrActivityLauncher.launch(intent);
   }

   public void stopQrActivity() {
      Intent intent = new Intent(this, com.spoon.maeumon.feature.qr.ui.QrActivity.class);
      intent.setAction("ACTION_FINISH_QR");
      intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
      startActivity(intent);
   }

   private boolean isCameraAvailable() {
      if (!pm.isCameraSupported()) {
         return false;
      }
      return pm.isCameraGranted();
   }
}