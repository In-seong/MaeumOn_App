package com.spoon.maeumon.feature.ble;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;

import androidx.core.app.ActivityCompat;

/**
 * BLE 스캔 Class
 * <p>BLE 스캔을 편하게 사용하기 위해 제작된 class</p>
 *
 * <p>다음 권한이 필수적으로 필요
 * <p>- {@link Manifest.permission#ACCESS_FINE_LOCATION}
 * <p>- {@link Manifest.permission#ACCESS_COARSE_LOCATION}
 * <p>- {@link Manifest.permission#BLUETOOTH}
 * <p>- {@link Manifest.permission#BLUETOOTH_ADMIN}
 * <p>- {@link Manifest.permission#BLUETOOTH_SCAN}
 * <p>- {@link Manifest.permission#BLUETOOTH_CONNECT}
 */
public class BLEScanService {
   private Context context;
   private BLEListener listener;

   // BLE
   private BluetoothManager bluetoothManager;
   private BluetoothAdapter bluetoothAdapter;
   private BluetoothLeScanner bluetoothLeScanner;
   private BluetoothGatt bluetoothGatt;

   private ScanSettings settings;
   // BLE Scan Callback
   private ScanCallback scanCallback;

   private Handler bleResetHandler;
   private Runnable scanTask;
   private java.util.List<ScanFilter> scanFilters;

   public BLEScanService(Context context, BLEListener listener) {
      this.context = context;
      BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
      bluetoothAdapter = manager.getAdapter();
      bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
      this.listener = listener;
   }

   /**
    * BLE 스캔 모드 설정
    *
    * <p>모드 설정을 안할시 스캔모드는 자동으로 {@link ScanSettings#SCAN_MODE_BALANCED}로 적용됩니다.</p>
    *
    * <p>- {@link ScanSettings#SCAN_MODE_LOW_LATENCY}      :   배터리 소모가 가장 크며 스캔속도가 짧아 주변 기기를 바로 연결할때 많이 사용하는 모드</p>
    * <p>- {@link ScanSettings#SCAN_MODE_BALANCED}         :   배터리 소모와 스캔속도가 평균적으로 실시간으로 모니터링 할때 많이 사용되는 모드 </p>
    * <p>- {@link ScanSettings#SCAN_MODE_LOW_POWER}        :   배터리 소모가 적고 스캔속도가 가장 느리며 장시간 백그라운드에서 모니터링 할때 많이 사용하는 모드</p>
    * <p>- {@link ScanSettings#SCAN_MODE_OPPORTUNISTIC}    :   다른앱이 스캔할 때 따라 스캔하는 모드</p>
    *
    * @param scanMode 스캔 모드
    * @return
    */
   public BLEScanService setScanMode(int scanMode) {
      settings = new ScanSettings.Builder()
              .setScanMode(scanMode)
              .build();
      return this;
   }

   /**
    * BLE 스캔시 처리할 CallBack
    *
    * @param scanCallback
    * @return
    */
   public BLEScanService setScanCallback(ScanCallback scanCallback) {
      this.scanCallback = scanCallback;
      return this;
   }

   /**
    * 스캔 필터 설정 (이름/제조사 등)
    */
   public BLEScanService setScanFilters(java.util.List<ScanFilter> filters) {
      this.scanFilters = filters;
      return this;
   }

   /**
    * 설정된 ms 마다 스캔 재실행
    *
    * <p> - BLE 스캔을 그냥 동작시 스캔 타임아웃 / 시스템 정책 때문에 스캔이 멈추는 현상 발생하여 일정 시간마다 재실행하여 스캔이 멈추지 않게 적용
    *
    * @param ms 스캔 재시작 주기 milliseconds
    */
   public void repeatScan(int ms) {
      bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
      if (bluetoothManager != null) {
         bluetoothAdapter = bluetoothManager.getAdapter();
      }

      if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
         listener.offBluetoothListener();
         return;
      }
      if (bleResetHandler != null) {
         bleResetHandler.removeCallbacksAndMessages(null);
      }
      bleResetHandler = new Handler();
      scanTask = () -> {
         stopScan();
         startScan();
         bleResetHandler.postDelayed(scanTask, ms); // 10초뒤 재실행
      };
      bleResetHandler.post(scanTask);
   }

   /**
    * BLE 스캔 시작
    *
    * <p>- 계속 동작시 스캔 타임아웃 / 시스템 정책 때문으로 인해 스캔이 멈추는 현상 발생함</p>
    */
   public void startScan() {
      if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
         listener.offBluetoothListener();
         return;
      }
      if (settings == null) {
         settings = new ScanSettings.Builder()
                 .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                 .build();
      }
      if (bluetoothLeScanner == null) {
         BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
         bluetoothAdapter = manager.getAdapter();
         bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
         // Android 12 이상
         if (ActivityCompat.checkSelfPermission(
                 context,
                 Manifest.permission.BLUETOOTH_SCAN
         ) != PackageManager.PERMISSION_GRANTED) {
            listener.missingPermission();
            return;
         }
      } else {
         // Android 11 이하 (API 30 이하)
         if (ActivityCompat.checkSelfPermission(
                 context,
                 Manifest.permission.ACCESS_FINE_LOCATION
         ) != PackageManager.PERMISSION_GRANTED) {
            listener.missingPermission();
            return;
         }
      }
      bluetoothLeScanner.startScan(scanFilters, settings, scanCallback);
      listener.startScanListener();
   }

   /**
    * BLE 스캔 정지
    */
   public void stopScan() {
      if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
         listener.offBluetoothListener();
         return;
      }
      if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
         listener.missingPermission();
         return;
      }
      if (bleResetHandler != null) {
         bleResetHandler.removeCallbacksAndMessages(null);
      }

      try {
         bluetoothLeScanner.stopScan(scanCallback);
      } catch (Exception e) {
      }
      listener.stopScanListener();
   }

   public interface BLEListener {
      /**
       * 블루투스가 꺼져있어 동작 불가 Listener
       */
      void offBluetoothListener();

      /**
       * Permission 권한 허용이 누락 되어 발생한 Listener
       */
      void missingPermission();

      /**
       * BLE 스캔이 시작 됬을때 동작 하는 Listener
       */
      void startScanListener();

      /**
       * BLE 스캔이 종료 됬을때 동작 하는 Listener
       */
      void stopScanListener();
   }
}
