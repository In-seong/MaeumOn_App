package com.spoon.maeumon.feature.support;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.core.app.ActivityCompat;

import com.spoon.maeumon.BuildConfig;
import com.spoon.maeumon.R;

import java.util.List;

public class PhoneSupport {
   private final String NUM = "6";
   private final int SDK = Build.VERSION.SDK_INT;
   private Context context;

   public PhoneSupport(Context context) {
      this.context = context;
   }

   /**
    * 전화번호 문자열
    *
    * @return NULL or 11자리 문자열
    */
   public String getPhoneNumber() {
      String phoneNumber;

      phoneNumber = getNumber();
      if (phoneNumber != null) {
         return phoneNumber;
      }

      if (BuildConfig.DEBUG) {
         StringBuilder p = new StringBuilder();
         p.append("123");
         for (int i = 0; i < 8; i++) {
            p.append(NUM);
         }
         return p.toString();
      }

      return "";
   }

   /**
    * 번호가져오기
    * @return
    */
   private String getNumber() {
      try {
         if (SDK < 22 || ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return null;
         }

         if (SDK >= Build.VERSION_CODES.TIRAMISU) {
            SubscriptionManager sManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            List<SubscriptionInfo> arrayInfo = sManager.getActiveSubscriptionInfoList();
            if (arrayInfo == null || arrayInfo.isEmpty()) {
               return null;
            } else if (arrayInfo.size() == 1) {
                return replaceCountryCode(sManager.getPhoneNumber(arrayInfo.get(0).getSubscriptionId()));
            } else {
               StringBuilder sb = new StringBuilder();
               for (SubscriptionInfo info : arrayInfo) {
                   sb.append(replaceCountryCode(sManager.getPhoneNumber(info.getSubscriptionId())));
                   break;
//               sb.append(" ");
               }
               return sb.toString().trim();
            }
         } else {
            TelephonyManager tManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
               return null;
            }
            return replaceCountryCode(tManager.getLine1Number());
         }
      } catch (Exception e) {
         return null;
      }
   }

   private String replaceCountryCode(String phoneNumber) {
      return phoneNumber.replace(context.getString(R.string.phone_country_code), "0");
   }
}
