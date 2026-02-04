package com.spoon.maeumon.feature.dialog;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.spoon.maeumon.R;

public class BasicDialog extends Dialog {
   private Context context;
   private TextView textTitle;
   private TextView textMessage;
   private TextView buttonPositive;
   private TextView buttonNegative;

   public BasicDialog(@NonNull Context context) {
      super(context);
      this.context = context;
      setContentView(R.layout.dialog_basic);
      textTitle = findViewById(R.id.basic_dialog_title);
      textMessage = findViewById(R.id.basic_dialog_message);
      buttonPositive = findViewById(R.id.basic_dialog_positive);
      buttonNegative = findViewById(R.id.basic_dialog_negative);

      textTitle.setVisibility(View.GONE);
      textMessage.setVisibility(View.GONE);
      buttonPositive.setVisibility(View.GONE);
      buttonNegative.setVisibility(View.GONE);
   }

   public BasicDialog setTextTitle(String title) {
      textTitle.setVisibility(View.VISIBLE);
      textTitle.setText(title);
      return this;
   }

   public BasicDialog setTextTitle(@StringRes int title) {
      textTitle.setVisibility(View.VISIBLE);
      textTitle.setText(context.getString(title));
      return this;
   }

   public BasicDialog setTextMessage(String message) {
      textMessage.setVisibility(View.VISIBLE);
      textMessage.setText(message);
      return this;
   }

   public BasicDialog setTextMessage(@StringRes int message) {
      textMessage.setVisibility(View.VISIBLE);
      textMessage.setText(context.getString(message));
      return this;
   }

   public BasicDialog setButtonPositive(String text, @Nullable OnClickDialogButtonListener listener) {
      buttonPositive.setVisibility(View.VISIBLE);
      buttonPositive.setText(text);
      buttonPositive.setOnClickListener(view -> {
         if (listener != null)
            listener.onClickListener();
         dismiss();
      });
      return this;
   }

   public BasicDialog setButtonPositive(@StringRes int text, @Nullable OnClickDialogButtonListener listener) {
      buttonPositive.setVisibility(View.VISIBLE);
      buttonPositive.setText(context.getString(text));
      buttonPositive.setOnClickListener(view -> {
         if (listener != null)
            listener.onClickListener();
         dismiss();
      });
      return this;
   }

   public BasicDialog setButtonNegative(String text, @Nullable OnClickDialogButtonListener listener) {
      buttonNegative.setVisibility(View.VISIBLE);
      buttonNegative.setText(text);
      buttonNegative.setOnClickListener(view -> {
         if (listener != null)
            listener.onClickListener();
         dismiss();
      });
      return this;
   }

   public BasicDialog setButtonNegative(@StringRes int text, @Nullable OnClickDialogButtonListener listener) {
      buttonNegative.setVisibility(View.VISIBLE);
      buttonNegative.setText(context.getString(text));
      buttonNegative.setOnClickListener(view -> {
         if (listener != null)
            listener.onClickListener();
         dismiss();
      });
      return this;
   }

   public BasicDialog setAllButton(String positiveText, String negativeText, @Nullable OnClickAllDialogButtonListener listener) {
      buttonPositive.setVisibility(View.VISIBLE);
      buttonNegative.setVisibility(View.VISIBLE);
      buttonPositive.setText(positiveText);
      buttonPositive.setOnClickListener(view -> {
         if (listener != null)
            listener.onYesClickListener();
         dismiss();
      });
      buttonNegative.setText(negativeText);
      buttonNegative.setOnClickListener(view -> {
         if (listener != null)
            listener.onNoClickListener();
         dismiss();
      });
      return this;
   }

   public BasicDialog setAllButton(@StringRes int positiveText, @StringRes int negativeText, OnClickAllDialogButtonListener listener) {
      buttonPositive.setVisibility(View.VISIBLE);
      buttonNegative.setVisibility(View.VISIBLE);
      buttonPositive.setText(context.getString(positiveText));
      buttonPositive.setOnClickListener(view -> {
         if (listener != null)
            listener.onYesClickListener();
         dismiss();
      });
      buttonNegative.setText(context.getString(negativeText));
      buttonNegative.setOnClickListener(view -> {
         if (listener != null)
            listener.onNoClickListener();
         dismiss();
      });
      return this;
   }

}
