package com.spoon.maeumon.feature.permission.ui;

import android.Manifest;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.spoon.maeumon.databinding.BottomsheetPermissionBinding;
import com.spoon.maeumon.feature.permission.util.PermissionManager;

public class PermissionBottomSheet extends BottomSheetDialogFragment {

   private BottomsheetPermissionBinding binding;
   private PermissionManager pm;

   @Override
   public void onAttach(@NonNull Context context) {
      super.onAttach(context);
      pm = new PermissionManager(requireActivity());
   }

   // Dialog 객체 생성
   @Override
   public Dialog onCreateDialog(Bundle savedInstanceState) {
      Dialog dialog = super.onCreateDialog(savedInstanceState);

      dialog.setCanceledOnTouchOutside(false);  // 바깥 영역 터치로 닫히지 않도록 함
      dialog.setCancelable(false);  // 뒤로가기 버튼으로도 닫히지 않도록 함

      dialog.setOnKeyListener((d, keyCode, event) -> {
         // 뒤로가기 버튼 누른 다음 손을 뗄 때 동작
         if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
            return true;   // event 처리 완료 의미
         }
         return false;  // 기본 동작 유지 의미
      });

      return dialog;
   }

   // Dialog 넣을 view 생성
   @Nullable
   @Override
   public View onCreateView(@NonNull LayoutInflater inflater,
                            @Nullable ViewGroup container,
                            @Nullable Bundle savedInstanceState) {
      binding = BottomsheetPermissionBinding.inflate(inflater, container, false);
      return binding.getRoot();
   }

   // ui 초기화
   @Override
   public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
      super.onViewCreated(view, savedInstanceState);

      updatePermissionsUI();
      updateButtonBehavior();
   }


   @Override
   public void onResume() {
      super.onResume();

      updatePermissionsUI();
      updateButtonBehavior();

      // 설정 화면에서 돌아왔을 때 이미 모든 권한 허용되었다면 바로 결과 전달
      if (isAllPermissionsGranted()) {
         sendResultToActivity();
      }
   }


   @Override
   public void onStart() {
      super.onStart();
      BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
      if (dialog != null) {
         FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
         if (bottomSheet != null) {
            BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            bottomSheet.setBackgroundColor(Color.TRANSPARENT);
            behavior.setDraggable(false);

         }
      }
   }

   @Override
   public void onDestroyView() {
      super.onDestroyView();
      binding = null;
   }

   // 각 권한 상태 반환 헬퍼 함수
   // ActivityCompat.shouldShowRequestPermissionRationale()
   // 반환값 true : 유저가 이전에 권한을 거부했지만 다시 권한 요청 팝업을 띄울 수 있는 상태
   // 반환값 false : 권한을 아직 요청한 적 없거나 유저가 다시는 묻지 않음 선택
   // PERMISSION_GRANTED 인데 false => 권한을 아직 요청한 적 없음
   private boolean isLocationPermissionGranted() {
      return ContextCompat.checkSelfPermission(requireContext(),
              Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
   }

   private boolean isBlePermissionGranted() {
      BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
      if (adapter == null) return true;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
         return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                 && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
      } else {
         return isLocationPermissionGranted();
      }
   }

   private boolean isNotificationPermissionGranted() {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
         return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
      } else {
         return NotificationManagerCompat.from(requireContext()).areNotificationsEnabled();
      }
   }

   private boolean isCameraPermissionGranted() {
      if (!pm.isCameraSupported()) {
         return true; // 카메라가 없으면 권한 필요 없음
      }
      return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
   }

   private boolean isAllPermissionsGranted() {
      //return isLocationPermissionGranted() && isBlePermissionGranted() && isNotificationPermissionGranted();
      return isLocationPermissionGranted() && isBlePermissionGranted() && isCameraPermissionGranted();
   }

   private void updatePermissionsUI() {
      setPermissionVisibility(binding.permissionPhoneIcon, binding.permissionPhoneTag, binding.permissionPhoneDescription, !pm.isPhoneGranted());

      setPermissionVisibility(binding.permissionLocationIcon, binding.permissionLocationTag,
              binding.permissionLocationDescription, !isLocationPermissionGranted());

      setPermissionVisibility(binding.permissionBleIcon, binding.permissionBleTag,
              binding.permissionBleDescription, !isBlePermissionGranted());

      // 카메라 권한: 카메라가 지원되고 권한이 없을 때만 표시
      setPermissionVisibility(binding.permissionCameraIcon, binding.permissionCameraTag,
              binding.permissionCameraDescription, pm.isCameraSupported() && !isCameraPermissionGranted());

      setPermissionVisibility(binding.permissionNotificationIcon, binding.permissionNotificationTag,
              binding.permissionNotificationDescription, pm.isNotificationFirstTime());   //!isNotificationPermissionGranted()

      if (!pm.isNotificationFirstTime()) {
         binding.permissionOptionalTag.setVisibility(View.GONE);
      }
   }

   private void setPermissionVisibility(View icon, View tag, View desc, boolean visible) {
      int v = visible ? View.VISIBLE : View.GONE;
      icon.setVisibility(v);
      tag.setVisibility(v);
      desc.setVisibility(v);
   }

   private void updateButtonBehavior() {

      // 버튼 텍스트 설정
      binding.permissionSettingBtn.setText(
              pm.shouldGoToSettings() ? "권한 설정하러가기" : "권한 허용 후 계속하기"
      );

// 버튼 클릭
      binding.permissionSettingBtn.setOnClickListener(v -> {
         if (pm.shouldGoToSettings()) {
            openAppSettings();
            return;
         }

         String[] perms = pm.getPermissionsToRequest();
         if (perms.length > 0) {
            requestPermissionsLauncher.launch(perms);
         }
      });
   }

   private void openAppSettings() {
      // 하나라도 거절/다시 묻지 않음 상태 → 설정 화면 이동
      startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
              .setData(Uri.parse("package:" + requireContext().getPackageName())));
      return;
   }

   private final ActivityResultLauncher<String[]> requestPermissionsLauncher =
           registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
              // 모든 권한 허용 여부 확인
              boolean allGranted = true;
              for (Boolean granted : result.values()) {
                 if (!granted) {
                    allGranted = false;
                    break;
                 }
              }

              if (allGranted) {
                 // 모든 권한 허용 시 부모로 결과 전달 후 BottomSheet 닫기
                 sendResultToActivity();
              } else {
                 // 일부 권한 미허용 시 BottomSheet 유지
              }
           });

   private void sendResultToActivity() {
      Bundle bundle = new Bundle();
      bundle.putBoolean("permissions_updated", true);
      getParentFragmentManager().setFragmentResult("permission_result", bundle);
      dismiss();
   }

}
