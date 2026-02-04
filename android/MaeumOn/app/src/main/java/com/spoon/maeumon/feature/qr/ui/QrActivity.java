package com.spoon.maeumon.feature.qr.ui;

import android.content.Intent;
import android.graphics.Point;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.OptIn;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.spoon.maeumon.databinding.ActivityQrBinding;
import com.spoon.maeumon.feature.common.ui.BaseActivity;
import com.spoon.maeumon.feature.permission.util.PermissionManager;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QrActivity extends BaseActivity {

   /* UI */
   private PreviewView previewView;
   private OverlayView overlayView;
   private ActivityQrBinding binding;

   /* 비동기 */
   private ExecutorService cameraExecutor;

   /* QR */
   private BarcodeScanner scanner;

   /* CameraX */
   private ProcessCameraProvider cameraProvider;   //유틸리티
   private Preview preview;   // 비디오 스트림

   /* 앱 권한 */
   private PermissionManager pm;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      /* UI */
      binding = ActivityQrBinding.inflate(getLayoutInflater());
      setContentView(binding.getRoot());
      EdgeToEdge.enable(this);
      applyWindowInsets();

      previewView = binding.previewView;
      overlayView = binding.overlayView;

      binding.qrCloseButton.setOnClickListener(v -> {
         stopCameraSafely();
         finish();
      });

      /* QR 스캔 */
      scanner = BarcodeScanning.getClient(); // Google ML Kit 바코드 스캐닝 클라이언트 초기화
      cameraExecutor = Executors.newSingleThreadExecutor(); // 이미지 분석 위해 단일 스레드 Executor 생성

      /* 카메라 권한 */
      pm = new PermissionManager(this);
      checkCameraPermissionAndStart();
   }

   @Override
   protected void onResume() {
      super.onResume();
      checkCameraPermissionAndStart();
   }

   @Override
   protected void onPause() {
      stopCameraSafely();
      super.onPause();
   }

   @Override
   protected void onNewIntent(Intent intent) {
      super.onNewIntent(intent);
      if (intent != null && "ACTION_FINISH_QR".equals(intent.getAction())) {
         finish();
      }
   }

   @Override
   protected void onDestroy() {
      stopCameraSafely();

      if (scanner != null) {
         try {
            scanner.close();
         } catch (Exception ignored) {
         }
         scanner = null;
      }

      if (cameraProvider != null) {
         // 이미 onDestroy()가 메인 스레드에서 실행되지만,
         // 확실하게 LiveData 작업을 메인 스레드에서 하도록 보장
         cameraProvider.unbindAll();
         cameraProvider = null;
      }
      binding = null;
      super.onDestroy();
   }

   /* UI */
   private void applyWindowInsets() {
      View rootView = binding.getRoot();
      ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
         Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
         v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
         return insets;
      });
   }

   private PointF[] mapPointsToPreviewView(
           Point[] imagePoints, int imageW, int imageH,
           int rotationDegrees, boolean isFront
   ) {
      if (previewView == null) return new PointF[0];  // 안정성

      int rotW = (rotationDegrees == 90 || rotationDegrees == 270) ? imageH : imageW;
      int rotH = (rotationDegrees == 90 || rotationDegrees == 270) ? imageW : imageH;

      int viewW = previewView.getWidth();
      int viewH = previewView.getHeight();
      if (viewW == 0 || viewH == 0) return new PointF[0];

      float scale = Math.max((float) viewW / rotW, (float) viewH / rotH);
      float scaledW = rotW * scale;
      float scaledH = rotH * scale;
      float dx = (viewW - scaledW) / 2f;
      float dy = (viewH - scaledH) / 2f;

      PointF[] mapped = new PointF[imagePoints.length];

      for (int i = 0; i < imagePoints.length; i++) {
         float x = imagePoints[i].x;
         float y = imagePoints[i].y;

         // 전면카메라일 경우 좌우 반전
         if (isFront) x = rotW - x;

         // PreviewView 크기에 맞게 스케일/오프셋 적용
         mapped[i] = new PointF(
                 x * scale + dx,
                 y * scale + dy
         );
      }

      return mapped;
   }


   /* 카메라 권한 */
   private void checkCameraPermissionAndStart() {
      if (pm.isCameraGranted()) {
         startCamera();
      } else {
         // 권한이 없으면 Activity 종료
         finish();
      }
   }

   /* QR 스캔 */
   @OptIn(markerClass = ExperimentalGetImage.class)
   private void startCamera() {
      ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

      cameraProviderFuture.addListener(() -> {
         try {
            this.cameraProvider = cameraProviderFuture.get();

            ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build();

            previewView.post(() -> {   // UI 스레드에서 코드 실행 보장
               preview = new Preview.Builder().build();  // CameraX의 Preview 객체 생성
               preview.setSurfaceProvider(previewView.getSurfaceProvider());  // preview 화면에 연결

               CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;  // 카메라 선택
               this.cameraProvider.unbindAll();
               this.cameraProvider.bindToLifecycle(QrActivity.this, cameraSelector, preview, imageAnalysis);   // 카메라와 UseCase 연결
            });

            imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
               if (imageProxy.getImage() != null) {
                  int imageW = imageProxy.getWidth();
                  int imageH = imageProxy.getHeight();
                  int rotation = imageProxy.getImageInfo().getRotationDegrees();

                  InputImage inputImage = InputImage.fromMediaImage(
                          imageProxy.getImage(), rotation);

                  scanner.process(inputImage)
                          .addOnSuccessListener(barcodes -> {
                             if (barcodes != null && !barcodes.isEmpty()) {
                                Barcode b = barcodes.get(0);
                                String barcodeValue = b.getDisplayValue();

                                Point[] corners = b.getCornerPoints();
                                if (corners != null && corners.length >= 4) {
                                   PointF[] mapped = mapPointsToPreviewView(corners, imageW, imageH, rotation, false);
                                   runOnUiThread(() -> overlayView.setQrPoints(mapped));
                                }

                                // 결과 전달 및 Activity 종료(UI 스레드에서)
                                runOnUiThread(() -> {
                                   stopCameraSafely();

                                   Intent resultIntent = new Intent();
                                   resultIntent.putExtra("qrResult", barcodeValue);
                                   setResult(RESULT_OK, resultIntent);
                                   finish();
                                });

                             } else {
                                runOnUiThread(() -> overlayView.setQrPoints(null));
                             }
                          })
                          .addOnFailureListener(Throwable::printStackTrace)
                          .addOnCompleteListener(task -> imageProxy.close());

               } else {
                  imageProxy.close();
               }
            });

         } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
         }
      }, ContextCompat.getMainExecutor(this));
   }

   // CameraX 프리뷰 해제
   private void stopCameraSafely() {
      try {
         if (preview != null) {
            preview.setSurfaceProvider(null);  // 카메라 버퍼 전송 즉시 중단
         }

         if (cameraProvider != null) {
            cameraProvider.unbindAll();  // CameraX 전체 해제
         }

      } catch (Exception e) {
         Log.e("QR", "stopCameraSafely error: " + e.getMessage());
      }
   }
}
