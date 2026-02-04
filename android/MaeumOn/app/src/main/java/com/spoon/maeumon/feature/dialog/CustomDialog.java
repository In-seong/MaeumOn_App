package com.spoon.maeumon.feature.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.spoon.maeumon.R;


public class CustomDialog extends Dialog {
   private Context context;
   private CardView cardDialogYes;
   private CardView cardDialogNo;
   private TextView textTitle;
   private TextView textMessage;
   private Button buttonYes;
   private Button buttonNo;

   public CustomDialog(@NonNull Context context) {
      super(context);
      this.context = context;
      setContentView(R.layout.dialog_custom);
      textTitle = findViewById(R.id.text_dialog_title);
      textMessage = findViewById(R.id.text_dialog_sub);
      buttonYes = findViewById(R.id.button_dialog_yes);
      buttonNo = findViewById(R.id.button_dialog_no);
      cardDialogYes = findViewById(R.id.card_dialog_yes);
      cardDialogNo = findViewById(R.id.card_dialog_no);


      textTitle.setVisibility(View.GONE);
      textMessage.setVisibility(View.GONE);
      cardDialogYes.setVisibility(View.GONE);
      cardDialogNo.setVisibility(View.GONE);
   }

   public CustomDialog setCustomTitle(String title) {
      textTitle.setVisibility(View.VISIBLE);
      textTitle.setText(title);
      return this;
   }

   public CustomDialog setCustomTitle(int title) {
      textTitle.setVisibility(View.VISIBLE);
      textTitle.setText(getString(title));
      return this;
   }

   public CustomDialog setCustomMessage(String message) {
      textMessage.setVisibility(View.VISIBLE);
      textMessage.setText(message);
      return this;
   }

   public CustomDialog setCustomMessage(int message) {
      textMessage.setVisibility(View.VISIBLE);
      textMessage.setText(getString(message));
      return this;
   }

   public CustomDialog setYesButton(String yesText, @Nullable OnClickDialogButtonListener listener) {
      cardDialogYes.setVisibility(View.VISIBLE);
      buttonYes.setText(yesText);
      buttonYes.setOnClickListener(view -> {
         listener.onClickListener();
         dismiss();
      });
      return this;
   }

   public CustomDialog setYesButton(int yesTextId, @Nullable OnClickDialogButtonListener listener) {
      cardDialogYes.setVisibility(View.VISIBLE);
      buttonYes.setText(getString(yesTextId));
      buttonYes.setOnClickListener(view -> {
         if (listener != null)
            listener.onClickListener();
         dismiss();
      });
      return this;
   }

   public CustomDialog setNoButton(String noText, @Nullable OnClickDialogButtonListener listener) {
      cardDialogNo.setVisibility(View.VISIBLE);
      buttonNo.setText(noText);
      buttonNo.setOnClickListener(view -> {
         if (listener != null)
            listener.onClickListener();
         dismiss();
      });
      return this;
   }

   public CustomDialog setNoButton(int noTextId, @Nullable OnClickDialogButtonListener listener) {
      cardDialogNo.setVisibility(View.VISIBLE);
      buttonNo.setText(getString(noTextId));
      buttonNo.setOnClickListener(view -> {
         if (listener != null)
            listener.onClickListener();
         dismiss();
      });
      return this;
   }

   public CustomDialog setYesNoButton(String yesText, String noText, OnClickAllDialogButtonListener listener) {
      cardDialogYes.setVisibility(View.VISIBLE);
      buttonYes.setText(yesText);
      buttonYes.setOnClickListener(view -> {
         listener.onYesClickListener();
         dismiss();
      });
      cardDialogNo.setVisibility(View.VISIBLE);
      buttonNo.setText(noText);
      buttonNo.setOnClickListener(view -> {
         listener.onNoClickListener();
         dismiss();
      });
      return this;
   }

   public CustomDialog setYesNoButton(OnClickAllDialogButtonListener listener) {
      cardDialogYes.setVisibility(View.VISIBLE);
      buttonYes.setOnClickListener(view -> {
         listener.onYesClickListener();
         dismiss();
      });
      cardDialogNo.setVisibility(View.VISIBLE);
      buttonNo.setOnClickListener(view -> {
         listener.onNoClickListener();
         dismiss();
      });
      return this;
   }

   public CustomDialog setYesNoButton(int yesTextId, int noTextId, OnClickAllDialogButtonListener listener) {
      cardDialogYes.setVisibility(View.VISIBLE);
      buttonYes.setText(getString(yesTextId));
      buttonYes.setOnClickListener(view -> {
         listener.onYesClickListener();
         dismiss();
      });
      cardDialogNo.setVisibility(View.VISIBLE);
      buttonNo.setText(getString(noTextId));
      buttonNo.setOnClickListener(view -> {
         listener.onNoClickListener();
         dismiss();
      });
      return this;
   }

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      this.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
   }

   private String getString(int textId) {
      return context.getString(textId);
   }

}
