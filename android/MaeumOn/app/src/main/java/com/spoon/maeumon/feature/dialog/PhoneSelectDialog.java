package com.spoon.maeumon.feature.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.spoon.maeumon.R;

public class PhoneSelectDialog extends Dialog {
   private Context context;
   private RadioGroup radioGroupPhones;
   private Button btnConfirm;
   private TextView txtMessageTwo;

   public PhoneSelectDialog(@NonNull Context context, String[] arrayPhones, OnConfirm onConfirm) {
      super(context);
      this.context = context;
      setContentView(R.layout.dialog_phone_select);

      radioGroupPhones = findViewById(R.id.radio_group_phones);
      btnConfirm = findViewById(R.id.btn_confirm);
      txtMessageTwo = findViewById(R.id.dialog_phone_select_message_2);

      for (String phone : arrayPhones) {
         RadioButton radioButton = new RadioButton(context);
         radioButton.setText(format(phone));
         radioButton.setTextSize(17);
         radioButton.setPadding(0, 20, 0, 20);
         radioButton.setGravity(Gravity.CENTER_VERTICAL);
         if (Build.VERSION.SDK_INT >= 30) {
            radioButton.setTypeface(context.getResources().getFont(R.font.pretendard_medium_font));
         }
         radioGroupPhones.addView(radioButton);
      }

      if (getWindow() != null) {
         getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
      }

      btnConfirm.setOnClickListener(v -> {
         int selectedId = radioGroupPhones.getCheckedRadioButtonId();
         if (selectedId != -1) {
            RadioButton selectedRadioButton = findViewById(selectedId);
            onConfirm.run(selectedRadioButton.getText().toString().replace("-",""));
         }
         dismiss();
      });

      String message = context.getString(R.string.dialog_phone_select_message_2);
      SpannableString ss = new SpannableString(message);
      int startQ = message.indexOf('‘');
      int endQ = message.indexOf('’') + 1;
      ss.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.gray500)), startQ, endQ, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
      txtMessageTwo.setText(ss);

      setCancelable(false);
      setCanceledOnTouchOutside(false);
   }

   public interface OnConfirm {
      void run(String phone);
   }

   private String format(String phone) {
      if (phone.length() == 11) {
         return phone.substring(0, 3) + "-" + phone.substring(3, 7) + "-" + phone.substring(7);
      }
      return phone;
   }
}