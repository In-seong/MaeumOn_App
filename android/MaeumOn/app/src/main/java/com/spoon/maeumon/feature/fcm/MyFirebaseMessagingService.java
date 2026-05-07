package com.spoon.maeumon.feature.fcm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.spoon.maeumon.R;
import com.spoon.maeumon.feature.splash.ui.SplashActivity;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

   private static final String TAG = "FCM";

   @Override
   public void onCreate() {
      super.onCreate();
      createNotificationChannel();
   }

   // 알림 채널 생성 - API 26 이상에서 필수
   private void createNotificationChannel() {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
         // 채널 ID, 이름, 중요도를 정의
         CharSequence name = getString(R.string.fcm_channel_name_general);
         String description = getString(R.string.fcm_channel_description_general);
         String CHANNEL_ID = getString(R.string.fcm_channel_id);

         //int importance = NotificationManager.IMPORTANCE_DEFAULT;
         int importance = NotificationManager.IMPORTANCE_HIGH; // 헤드업 알림(팝업)

         NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
         channel.setDescription(description);

         // 시스템 채널 등록
         NotificationManager notificationManager = getSystemService(NotificationManager.class);
         notificationManager.createNotificationChannel(channel);
      }
   }

   @Override
   public void onMessageReceived(RemoteMessage remoteMessage) {
      // TODO(developer): Handle FCM messages here.
      // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
      Log.d(TAG, "From: " + remoteMessage.getFrom());

      // Check if message contains a data payload.
      if (remoteMessage.getData().size() > 0) {
         Log.d(TAG, "Message data payload: " + remoteMessage.getData());
         sendNotificationFromData(remoteMessage.getData());
         if (/* Check if data needs to be processed by long running job */ true) {
            // For long-running tasks (10 seconds or more) use WorkManager.
            //scheduleJob();
         } else {
            // Handle message within 10 seconds
            //handleNow();
         }

      }
      // Check if message contains a notification payload.
      if (remoteMessage.getNotification() != null) {
         Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
         sendNotification(remoteMessage.getNotification());
      }

      // Also if you intend on generating your own notifications as a result of a received FCM
      // message, here is where that should be initiated. See sendNotification method below.
   }

   private void sendNotification(RemoteMessage.Notification notification) {
      String CHANNEL_ID = getString(R.string.fcm_channel_id);

      // 채널 생성 (API 26 이상)
      //createNotificationChannel();

      // 요구사항
      // 앱이 완전히 꺼져 있으면 → Splash부터 시작
      // 앱이 백그라운드나 포그라운드에 실행 중이면 → 기존 상태 그대로 앞으로 가져오기 (재생성 X)
      Intent intent = new Intent(this, SplashActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

      PendingIntent pendingIntent = PendingIntent.getActivity(
              this,
              0,
              intent,
              PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
      );

      // Notification Builder 생성
      NotificationCompat.Builder notificationBuilder =
              new NotificationCompat.Builder(this, CHANNEL_ID)
                      .setPriority(NotificationCompat.PRIORITY_HIGH)
                      .setSmallIcon(R.mipmap.ic_launcher)
                      .setContentTitle(notification.getTitle())
                      .setContentText(notification.getBody())
                      .setAutoCancel(true) // 클릭 시 알림 제거
                      .setContentIntent(pendingIntent);

      // 알림 시스템에 전송
      NotificationManager notificationManager =
              (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

      // 고유한 알림 ID를 사용하여 알림 표시
      int notificationId = (int) (SystemClock.uptimeMillis() % Integer.MAX_VALUE);
      notificationManager.notify(notificationId, notificationBuilder.build());
   }

   private void sendNotificationFromData(Map<String, String> data) {
      String title = data.get("title");
      String body = data.get("body");

      Intent intent = new Intent(this, SplashActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

      PendingIntent pendingIntent = PendingIntent.getActivity(
              this,
              0,
              intent,
              PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
      );

      NotificationCompat.Builder builder = new NotificationCompat.Builder(this, getString(R.string.fcm_channel_id))
              .setSmallIcon(R.mipmap.ic_launcher)
              .setContentTitle(title)
              .setContentText(body)
              .setPriority(NotificationCompat.PRIORITY_HIGH)
              .setAutoCancel(true)
              .setContentIntent(pendingIntent);

      NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
      manager.notify((int) System.currentTimeMillis(), builder.build());
   }

   @Override
   public void onNewToken(String token) {
      super.onNewToken(token);
      Log.d("FCM", "New token received: " + token);

      // SharedPreferences 저장
      getSharedPreferences("app_prefs", MODE_PRIVATE)
              .edit()
              .putString("fcm_token", token)
              .apply();

      // WebView Activity에 전달할 Broadcast
      Intent intent = new Intent("FCM_TOKEN_RECEIVED");
      intent.putExtra("token", token);
      LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
   }
}
