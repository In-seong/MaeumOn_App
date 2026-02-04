package com.spoon.maeumon.feature.support;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.StringRes;

public class CustomToast {
   private static Toast toast = null;

   public static void show(Context context, String message, int duration) {
      cancel();
      toast = Toast.makeText(context, message, duration);
      toast.show();
   }

   public static void show(Context context, @StringRes int message, int duration) {
      cancel();
      toast = Toast.makeText(context, context.getString(message), duration);
      toast.show();
   }

   public static void cancel() {
      if (toast != null) toast.cancel();
   }
}
