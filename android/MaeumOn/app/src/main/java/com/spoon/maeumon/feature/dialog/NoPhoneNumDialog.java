package com.spoon.maeumon.feature.dialog;

import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.spoon.maeumon.R;
import com.spoon.maeumon.feature.splash.ui.SplashActivity;
import com.spoon.maeumon.feature.support.CustomToast;

public class NoPhoneNumDialog extends BasicDialog {
   private ConstraintLayout layoutBack;
   private ConstraintLayout layoutTest;
   private EditText editTest;
   private TextView btnConfirm;

   private int clickCountA = 0;
   private boolean longClickB = false;
   private int clickCountC = 0;

   public NoPhoneNumDialog(@NonNull SplashActivity activity) {
      super(activity);
      layoutBack = findViewById(R.id.layout_back);
      layoutTest = findViewById(R.id.layout_test);
      editTest = findViewById(R.id.edit_test);
      btnConfirm = findViewById(R.id.btn_confirm);

      layoutBack.setOnClickListener(v -> {
         if (!longClickB) {
            if (clickCountA < 9) {
               clickCountA++;
               Log.d("test", String.valueOf(clickCountA));
            } else {
               layoutBack.setOnLongClickListener(v1 -> {
                  longClickB = true;
                  Log.d("test", String.valueOf(true));
                  return true;
               });
            }
         } else {
            if (clickCountC < 9) {
               clickCountC++;
               Log.d("test", String.valueOf(clickCountC));
            } else {
               layoutBack.setOnLongClickListener(v1 -> {
                  CustomToast.show(activity, "구글 테스트용입니다.", Toast.LENGTH_SHORT);
                  layoutTest.setVisibility(View.VISIBLE);
                  return true;
               });
            }
         }
      });

      btnConfirm.setOnClickListener(v -> {
         if (editTest.getText().toString().equals("*google_test*")) {
            activity.viewModel.setPhone("00000000000");
            dismiss();
         }
      });
   }
}
