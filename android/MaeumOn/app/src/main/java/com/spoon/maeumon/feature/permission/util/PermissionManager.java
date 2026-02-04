package com.spoon.maeumon.feature.permission.util;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionManager {
   private final Context context;
   private final Activity activity;

   public PermissionManager(Activity activity) {
      this.activity = activity;
      this.context = activity.getApplicationContext();
   }

   /* 앱 전역 */
   // ----- 지원 여부 -----
   public boolean isLocationSupported() {
      return context.getPackageManager()
              .hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
   }

   public boolean isBleSupported() {
      BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
      return adapter != null;
   }

   public boolean isNotificationSupported() {
//      return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;
      // Android 12 이하도 NotificationManagerCompat로 알림 허용 여부 확인 가능
      return true;
   }

   public boolean isCameraSupported() {
      return context.getPackageManager()
              .hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
   }

   // ----- 권한 허용 여부 -----
   public boolean isLocationGranted() {
      return ContextCompat.checkSelfPermission(
              context, Manifest.permission.ACCESS_FINE_LOCATION
      ) == PackageManager.PERMISSION_GRANTED;
   }

   public boolean isPhoneGranted() {
      return (((Build.VERSION.SDK_INT > 25 && ContextCompat.checkSelfPermission(
              context, Manifest.permission.READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED) || (Build.VERSION.SDK_INT > 28 && ContextCompat.checkSelfPermission(
              context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED)) && ContextCompat.checkSelfPermission(
              context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED);
   }

   public boolean isBleGranted() {
      if (!isBleSupported()) return true;
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
         return isLocationGranted(); // 12 미만은 위치 권한

      return ContextCompat.checkSelfPermission(
              context, Manifest.permission.BLUETOOTH_SCAN
      ) == PackageManager.PERMISSION_GRANTED
              &&
              ContextCompat.checkSelfPermission(
                      context, Manifest.permission.BLUETOOTH_CONNECT
              ) == PackageManager.PERMISSION_GRANTED;
   }

   public boolean isNotificationGranted() {
      if (!isNotificationSupported()) return true;
//      return ContextCompat.checkSelfPermission(
//              context, Manifest.permission.POST_NOTIFICATIONS
//      ) == PackageManager.PERMISSION_GRANTED;
      // NotificationManagerCompat를 사용해 실제 알림 허용 여부 체크
      NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
      return notificationManager.areNotificationsEnabled();
   }

   // ----- First Time (처음 권한 요청 가능 여부) -----
   public boolean isLocationFirstTime() {
      return isLocationSupported()
              && !isLocationGranted()
              && !ActivityCompat.shouldShowRequestPermissionRationale(
              activity, Manifest.permission.ACCESS_FINE_LOCATION
      );
   }
   public boolean isPhoneFirstTime() {
      return !isPhoneGranted()
              && !ActivityCompat.shouldShowRequestPermissionRationale(
              activity, Manifest.permission.READ_PHONE_STATE
      );
   }

   public boolean isBleFirstTime() {
      if (!isBleSupported()) return true;
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;

      return !isBleGranted()
              && !ActivityCompat.shouldShowRequestPermissionRationale(
              activity, Manifest.permission.BLUETOOTH_SCAN
      );
   }

   public boolean isNotificationFirstTime() {
      if (!isNotificationSupported()) return true;

      return !isNotificationGranted()
              && !ActivityCompat.shouldShowRequestPermissionRationale(
              activity, Manifest.permission.POST_NOTIFICATIONS
      );
   }

   // ----- 요청해야 하는 권한 목록 -----
   public String[] getPermissionsToRequest() {
      List<String> list = new ArrayList<>();

      if (isPhoneFirstTime()) {
         if (Build.VERSION.SDK_INT > 25) {
            list.add(Manifest.permission.READ_PHONE_NUMBERS);
         }
         list.add(Manifest.permission.READ_PHONE_STATE);
         list.add(Manifest.permission.CALL_PHONE);
      }

      if (isLocationFirstTime()) {
         list.add(Manifest.permission.ACCESS_FINE_LOCATION);
         list.add(Manifest.permission.ACCESS_COARSE_LOCATION);
      }

      if (isBleFirstTime() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
         list.add(Manifest.permission.BLUETOOTH_SCAN);
         list.add(Manifest.permission.BLUETOOTH_CONNECT);
      }

      if (isCameraFirstTime()) {
         list.add(Manifest.permission.CAMERA);
      }

      if (isNotificationFirstTime() && Build.VERSION.SDK_INT > 32)
         list.add(Manifest.permission.POST_NOTIFICATIONS);

      return list.toArray(new String[0]);
   }


   public boolean shouldGoToSettings() {
      boolean phoneSettings = !isPhoneFirstTime() && !isPhoneGranted();
      boolean locationSettings = isLocationSupported() && !isLocationFirstTime() && !isLocationGranted();
      boolean bleSettings = isBleSupported() && !isBleFirstTime() && !isBleGranted();
      boolean cameraSettings = isCameraSupported() && !isCameraFirstTime() && !isCameraGranted();

//      boolean notificationSettings;
//      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//         // Android 13 이상에서는 런타임 권한 체크
//         notificationSettings = !isNotificationGranted() && !isNotificationFirstTime();
//      } else {
//         // Android 12 이하에서는 NotificationManagerCompat 상태 체크
//         NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
//         notificationSettings = !notificationManager.areNotificationsEnabled();
//      }
//
//      return locationSettings || bleSettings || notificationSettings;
      return phoneSettings || locationSettings || bleSettings || cameraSettings;
   }


   // ----- 모든 권한 허용 여부 -----
   public boolean isAllGranted() {
      //return isLocationGranted() && isBleGranted() && isNotificationGranted();
      return isLocationGranted() && isBleGranted() && isCameraGranted();
   }

   /* QR 스캔 */
   public boolean isCameraGranted() {
      if (!isCameraSupported()) return true; // 카메라가 없으면 권한 필요 없음
      return ContextCompat.checkSelfPermission(
              context, Manifest.permission.CAMERA
      ) == PackageManager.PERMISSION_GRANTED;
   }

   public boolean isCameraFirstTime() {
      if (!isCameraSupported()) return true; // 카메라가 없으면 권한 요청 불필요
      return !isCameraGranted()
              && !ActivityCompat.shouldShowRequestPermissionRationale(
              activity, Manifest.permission.CAMERA
      );
   }
}
