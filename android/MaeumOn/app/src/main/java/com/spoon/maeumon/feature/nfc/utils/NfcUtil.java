package com.spoon.maeumon.feature.nfc.utils;

import android.content.Context;
import android.nfc.NfcAdapter;

public class NfcUtil {
   public static boolean isSupported(Context context) {
      return NfcAdapter.getDefaultAdapter(context) != null;
   }

   public static boolean isEnabled(Context context) {
      NfcAdapter adapter = NfcAdapter.getDefaultAdapter(context);
      return adapter != null && adapter.isEnabled();
   }

   public static boolean isReady(Context context) {
      NfcAdapter adapter = NfcAdapter.getDefaultAdapter(context);
      return adapter != null && adapter.isEnabled();
   }
}
