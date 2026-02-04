package com.spoon.maeumon.feature.splash.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;

import com.spoon.maeumon.R;
import com.spoon.maeumon.databinding.ActivitySplashBinding;
import com.spoon.maeumon.feature.common.ui.BaseActivity;
import com.spoon.maeumon.feature.permission.util.PermissionManager;
import com.spoon.maeumon.feature.splash.model.SplashViewModel;
import com.spoon.maeumon.feature.webView.ui.WebViewActivity;

public class SplashActivity extends BaseActivity {
    private static final int PERMISSION_REQUEST_CODE = 1001;

    private ActivitySplashBinding binding;
    private SplashViewModel splashViewModel;
    public SplashViewModel viewModel; // InsertData, NoPhoneNumDialog에서 접근용
    private PermissionManager pm;

    private final ActivityResultLauncher<Intent> settingsLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                checkPermissionsAndProceed();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        splashViewModel = new ViewModelProvider(this).get(SplashViewModel.class);
        splashViewModel.activity = this;
        viewModel = splashViewModel; // public 참조용

        pm = new PermissionManager(this);

        // 스플래시 화면 표시 후 권한 체크
        new Handler(Looper.getMainLooper()).postDelayed(this::checkPermissionsAndProceed, 1500);
    }

    private void checkPermissionsAndProceed() {
        if (pm.isAllGranted()) {
            checkPhoneAndProceed();
        } else {
            String[] permissionsToRequest = pm.getPermissionsToRequest();
            if (permissionsToRequest.length > 0) {
                ActivityCompat.requestPermissions(this, permissionsToRequest, PERMISSION_REQUEST_CODE);
            } else if (pm.shouldGoToSettings()) {
                openAppSettings();
            } else {
                checkPhoneAndProceed();
            }
        }
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", getPackageName(), null));
        settingsLauncher.launch(intent);
    }

    private void checkPhoneAndProceed() {
        // 핸드폰 인증 없이 바로 웹뷰로 이동
        goToWebView();

        /* 기존 인증 로직 - 비활성화
        SharedPreferences prefPhone = getSharedPreferences("phone", Context.MODE_PRIVATE);
        String phone = prefPhone.getString("phone", null);

        if (phone != null && !phone.isEmpty()) {
            // 이미 인증된 사용자 → WebView로 이동
            splashViewModel.setPhone(phone);
            goToWebView();
        } else {
            // 전화번호 인증 필요
            showPhoneCertFragment();
        }
        */
    }

    private void showPhoneCertFragment() {
        binding.fragmentView.setVisibility(View.VISIBLE);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_view, new PhoneCertFragment())
                .commit();
    }

    public void successPhoneCert() {
        goToWebView();
    }

    private void goToWebView() {
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            new Handler(Looper.getMainLooper()).postDelayed(this::checkPermissionsAndProceed, 300);
        }
    }
}