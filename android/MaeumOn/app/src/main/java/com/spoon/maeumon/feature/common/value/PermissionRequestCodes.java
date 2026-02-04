package com.spoon.maeumon.feature.common.value;

public final class PermissionRequestCodes {
   // 우선은 같은 권한은 같은 코드 사용

   // 권한 요청 코드 (Permission Codes)
   public static final int LOCATION_PERMISSION = 100;
   public static final int CAMERA_PERMISSION = 200;
   public static final int BLE_PERMISSIONS = 300;


   // 생성자를 private으로 만들어 인스턴스화를 방지
   private PermissionRequestCodes() {
   }
}

// 사용 예시
// ActivityCompat.requestPermissions(activity, permissions, PermissionRequestCodes.LOCATION_PERMISSION);