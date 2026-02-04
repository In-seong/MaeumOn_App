package com.spoon.maeumon.feature.common.ui;

import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

// "광동제약 통근셔틀"의 경우 nfc 사용 안하기 때문에 안드로이드 nfc 기본 화면을 막지 않음
public class BaseActivity extends AppCompatActivity {
   protected NfcAdapter nfcAdapter;

   @Override
   protected void onCreate(@Nullable Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      nfcAdapter = NfcAdapter.getDefaultAdapter(this);      // NFC 지원 x -> null
   }

   @Override
   protected void onResume() {
      super.onResume();
      enableReaderMode(); // 시스템 기본 화면 막기
   }

   @Override
   protected void onPause() {
      super.onPause();
      disableReaderMode();
   }

   // Reader Mode 활성화 (태그 읽기는 안 함)
   public void enableReaderMode() {
      if (nfcAdapter == null) {
         Log.w("NFC", "NFC를 지원하지 않는 기기입니다.");
         return;
      }
      if (nfcAdapter != null) {
         nfcAdapter.enableReaderMode(
                 this,
                 tag -> {
                    // 아무것도 안 함 → 시스템 기본 화면 막기
                 },
                 NfcAdapter.FLAG_READER_NFC_A |
                         NfcAdapter.FLAG_READER_NFC_B,   // NFC_A만 켜도 무방하지만, 둘다 지정하여 OS 시스템 NFC 화면/알럿을 최대한 차단
                 null
         );
      }
   }

   public void disableReaderMode() {
      if (nfcAdapter != null) {
         nfcAdapter.disableReaderMode(this);
      }
   }
}
