package com.spoon.maeumon;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class MyApp extends Application {
   @Override
   public void onCreate() {
      super.onCreate();
      createNotificationChannel();
   }

   private void createNotificationChannel() {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
         String CHANNEL_ID = getString(R.string.fcm_channel_id);
         CharSequence name = getString(R.string.fcm_channel_name_general);
         String description = getString(R.string.fcm_channel_description_general);
         int importance = NotificationManager.IMPORTANCE_HIGH;

         NotificationChannel channel =
                 new NotificationChannel(CHANNEL_ID, name, importance);
         channel.setDescription(description);

         NotificationManager manager = getSystemService(NotificationManager.class);
         manager.createNotificationChannel(channel);
      }
   }
}
