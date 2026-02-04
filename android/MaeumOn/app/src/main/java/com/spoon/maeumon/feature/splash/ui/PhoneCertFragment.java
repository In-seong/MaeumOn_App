package com.spoon.maeumon.feature.splash.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.spoon.maeumon.R;
import com.spoon.maeumon.databinding.FragmentPhoneCertBinding;
import com.spoon.maeumon.feature.data.InsertData;
import com.spoon.maeumon.feature.dialog.CustomDialog;
import com.spoon.maeumon.feature.splash.model.SplashViewModel;

import java.util.HashMap;
import java.util.Map;

public class PhoneCertFragment extends Fragment {
   private final int CODE_COUNT_SEC = 180;
   private FragmentPhoneCertBinding binding;
   private SplashViewModel splashViewModel;
   private SplashActivity activity;

   // Timer
   private CountDownTimer countTimer;

   // Database
   private InsertData insertData;

   // Dialog
   private CustomDialog customDialog;

   // UI
   private EditText editPhone;
   private EditText editCode;
   private CardView cardBtnSend;
   private CardView cardBtnOk;
   private Button btnSend;
   private Button btnOk;
   private TextView txtCount;
   private TextView txtPhoneErr;
   private TextView txtCodeErr;

   // Default
   private InputMethodManager inputMethodManager;
   private String phone;
   private int count = 0;

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container,
                            Bundle savedInstanceState) {
      // Inflate the layout for this fragment
      binding = FragmentPhoneCertBinding.inflate(inflater, container, false);
      View root = binding.getRoot();

      setUIInitial();
      setAllListener();
      setAllViewModelObserve();

      return root;
   }

   private void setUIInitial() {
      splashViewModel = new ViewModelProvider(requireActivity()).get(SplashViewModel.class);
      activity = splashViewModel.activity;

      inputMethodManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);

      editPhone = binding.editPhone;
      editCode = binding.editCode;
      cardBtnSend = binding.cardBtnSend;
      cardBtnOk = binding.cardBtnOk;
      btnSend = binding.btnSend;
      btnOk = binding.btnOk;
      txtCount = binding.txtCount;
      txtPhoneErr = binding.txtPhoneErr;
      txtCodeErr = binding.txtCodeErr;

      txtCount.setVisibility(View.GONE);
      editPhone.setEnabled(true);
      btnSend.setEnabled(true);
      txtPhoneErr.setVisibility(View.INVISIBLE);
      txtCodeErr.setVisibility(View.INVISIBLE);
      editCode.setEnabled(false);
      cardBtnOk.setCardBackgroundColor(activity.getColor(R.color.text_secondary));
      btnOk.setEnabled(false);
   }

   private void setAllListener() {

      btnSend.setOnClickListener(v -> {
         focusClear();
         String checkPhone = editPhone.getText().toString();

         editCode.setText("");
         editCode.setEnabled(false);
         btnOk.setEnabled(false);
         txtCodeErr.setVisibility(View.INVISIBLE);

         if (checkPhone.isEmpty()) {
            txtPhoneErr.setText(getString(R.string.number_check_err_msg, "번호", "입력"));
            txtPhoneErr.setVisibility(View.VISIBLE);
         } else if (checkPhone.length() < 10) {
            txtPhoneErr.setText(getString(R.string.number_check_err_msg, "번호", "확인"));
            txtPhoneErr.setVisibility(View.VISIBLE);
         } else {
            txtPhoneErr.setVisibility(View.INVISIBLE);
            phone = checkPhone;

            Map<String, String> map = new HashMap<>();
            map.put("phone", phone);

            insertData = new InsertData(activity);
            insertData.certExecute(map);
         }
      });

      btnOk.setOnClickListener(v -> {
         focusClear();
         String checkCode = editCode.getText().toString();

         if (checkCode.isEmpty()) {
            txtCodeErr.setText(getString(R.string.number_check_err_msg, "인증번호", "입력"));
            txtCodeErr.setVisibility(View.VISIBLE);
         } else {
             Map<String, String> map = new HashMap<>();
             map.put("phone", phone);
             map.put("code", checkCode);

            insertData = new InsertData(activity);
            insertData.codeExecute(map);
         }
      });
   }

   private void setAllViewModelObserve() {
       splashViewModel.getSendCode().observe(getViewLifecycleOwner(), stringDataParam -> {
           if (stringDataParam != null) {
               if (stringDataParam.isSuccess()) {
                   // 전송 성공
                   if (!activity.isFinishing()) {
                       if (customDialog != null && customDialog.isShowing()) {
                           customDialog.dismiss();
                       }
                       customDialog = new CustomDialog(activity);
                       customDialog.setCustomTitle(R.string.number_check_send_success);
                       customDialog.setYesButton(R.string.dialog_button_yes, () -> customDialog.dismiss());
                       customDialog.show();
                   }
                   editPhone.setEnabled(false);
                   cardBtnSend.setCardBackgroundColor(activity.getColor(R.color.text_secondary));
                   btnSend.setEnabled(false);
                   btnSend.setText(R.string.number_check_resend);
                   editCode.setEnabled(true);
                   cardBtnOk.setCardBackgroundColor(activity.getColor(R.color.color_primary));
                   btnOk.setEnabled(true);

                   txtCount.setText("3:00");
                   txtCount.setVisibility(View.VISIBLE);

                   if (countTimer != null) {
                       countTimer.cancel();
                   }
                   count = 0;
                   countTimer = new CountDownTimer(180000, 1000) {
                       @Override
                       public void onTick(long millisUntilFinished) {
                           count++;
                           int showSec = CODE_COUNT_SEC - count;
                           int min = showSec / 60;
                           int sec = showSec % 60;
                           txtCount.setText(min + ":" + (sec < 10 ? ("0" + sec) : sec));
                           if (count >= 30) {
                               editPhone.setEnabled(true);
                               cardBtnSend.setCardBackgroundColor(activity.getColor(R.color.color_primary));
                               btnSend.setEnabled(true);
                           }
                       }

                       @Override
                       public void onFinish() {
                           txtCount.setVisibility(View.GONE);
                           editPhone.setEnabled(true);
                           cardBtnSend.setCardBackgroundColor(activity.getColor(R.color.color_primary));
                           btnSend.setEnabled(true);
                           btnSend.setText(R.string.number_check_send);
                           editCode.setText("");
                           editCode.setEnabled(false);
                           cardBtnOk.setCardBackgroundColor(activity.getColor(R.color.text_secondary));
                           btnOk.setEnabled(false);
                       }
                   }.start();
               } else {
                   txtCount.setVisibility(View.GONE);
                   editPhone.setEnabled(true);
                   cardBtnSend.setCardBackgroundColor(activity.getColor(R.color.color_primary));
                   btnSend.setEnabled(true);
                   btnSend.setText(R.string.number_check_send);
                   editCode.setText("");
                   editCode.setEnabled(false);
                   cardBtnOk.setCardBackgroundColor(activity.getColor(R.color.text_secondary));
                   btnOk.setEnabled(false);

                   txtCodeErr.setText(stringDataParam.getMessage());
                   txtCodeErr.setVisibility(View.VISIBLE);
               }
           }
       });

       splashViewModel.getVerifyCode().observe(getViewLifecycleOwner(), stringDataParam -> {
            if (stringDataParam != null) {
                if (stringDataParam.isSuccess()) {
                    SharedPreferences prefPhone = activity.getSharedPreferences("phone", Context.MODE_PRIVATE);
                    prefPhone.edit().putString("phone", phone).apply();
                    splashViewModel.setPhone(phone);
                    activity.successPhoneCert();
                } else {
                    txtCodeErr.setText(stringDataParam.getMessage());
                    txtCodeErr.setVisibility(View.VISIBLE);
                }
            }
       });
   }

   @Override
   public void onDestroy() {
      super.onDestroy();
      if (countTimer != null) {
         countTimer.cancel();
      }
      count = 0;
      splashViewModel.setSendCode(null);
      splashViewModel.setVerifyCode(null);
      if (customDialog != null && customDialog.isShowing()) {
         customDialog.dismiss();
      }
   }

   private void focusClear() {
      inputMethodManager.hideSoftInputFromWindow(editPhone.getWindowToken(), 0);
      editPhone.clearFocus();
      inputMethodManager.hideSoftInputFromWindow(editCode.getWindowToken(), 0);
      editCode.clearFocus();
   }
}