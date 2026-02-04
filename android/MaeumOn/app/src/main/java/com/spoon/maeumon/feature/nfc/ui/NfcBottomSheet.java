package com.spoon.maeumon.feature.nfc.ui;

import android.app.Activity;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.spoon.maeumon.R;
import com.spoon.maeumon.feature.common.ui.BaseActivity;
import com.spoon.maeumon.feature.nfc.viewModel.NfcViewModel;

public class NfcBottomSheet extends BottomSheetDialogFragment {
   private NfcViewModel nfcViewModel;
   private NfcAdapter nfcAdapter;

   @Nullable
   @Override
   public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                            @Nullable Bundle savedInstanceState) {
      View view = inflater.inflate(R.layout.bottomsheet_nfc, container, false);

      nfcViewModel = new ViewModelProvider(requireActivity()).get(NfcViewModel.class);
      nfcAdapter = NfcAdapter.getDefaultAdapter(requireContext());

      // Glide로 GIF 로드 및 재생
      ImageView gifView = view.findViewById(R.id.nfc_scan_anim);
      Glide.with(this)
              .asGif()
              .load(Uri.parse("file:///android_asset/nfc.gif"))
              .into(gifView);

      // 닫기 버튼 클릭 이벤트
      Button btnClose = view.findViewById(R.id.nfc_btn_close_bottomsheet);
      btnClose.setOnClickListener(v -> dismiss()); // BottomSheet 닫기

      return view;
   }

   @Override
   public void onResume() {
      super.onResume();

      // BaseActivity ReaderMode 끄기
      if (getActivity() instanceof BaseActivity) {
         ((BaseActivity) getActivity()).disableReaderMode();
      }

      // Fragment 전용 ReaderMode 켜기
      if (getActivity() != null && nfcAdapter != null) {
         nfcAdapter.enableReaderMode(requireActivity(), tag -> {
            String tagText = readTextFromNfcTag(tag);
            if (tagText != null) {
               nfcViewModel.onNfcDetected(tagText);   // 값 저장
               dismiss();
            }
         }, NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_NFC_B, null);
      }
   }

   @Override
   public void onPause() {
      super.onPause();

      // Fragment ReaderMode 끄기
      if (nfcAdapter != null && getActivity() != null) {
         nfcAdapter.disableReaderMode(requireActivity());
      }

      // BaseActivity ReaderMode 복원
      if (getActivity() instanceof BaseActivity) {
         ((BaseActivity) getActivity()).enableReaderMode();
      }
   }

   // NDEF 텍스트 읽기
   public static String readTextFromNfcTag(Tag tag) {
      try {
         Ndef ndef = Ndef.get(tag);
         if (ndef == null) {
            Log.e("NFC", "NDEF not supported by this tag.");
            return null;
         }

         ndef.connect();
         NdefMessage ndefMessage = ndef.getNdefMessage();
         ndef.close();

         if (ndefMessage == null || ndefMessage.getRecords().length == 0) {
            Log.w("NFC", "Empty NDEF message.");
            return null;
         }

         NdefRecord record = ndefMessage.getRecords()[0];
         byte[] payload = record.getPayload();

         // Text record parsing
         String textEncoding = ((payload[0] & 0x80) == 0) ? "UTF-8" : "UTF-16";
         int languageCodeLength = payload[0] & 0x3F;

         String text = new String(payload, languageCodeLength + 1,
                 payload.length - languageCodeLength - 1, textEncoding);

         Log.d("NFC", "Read NDEF text: " + text);
         return text;

      } catch (Exception e) {
         Log.e("NFC", "Error reading NDEF", e);
         return null;
      }
   }
}
