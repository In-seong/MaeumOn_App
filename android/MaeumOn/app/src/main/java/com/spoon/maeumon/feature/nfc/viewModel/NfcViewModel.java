package com.spoon.maeumon.feature.nfc.viewModel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class NfcViewModel extends ViewModel {
   private final MutableLiveData<String> nfcTag = new MutableLiveData<>();
   private final MutableLiveData<Boolean> isProcessing = new MutableLiveData<>(false);

   public LiveData<String> getNfcTag() { return nfcTag; }
   public LiveData<Boolean> getIsProcessing() { return isProcessing; }

   public void onNfcDetected(String tagText) {
      if (isProcessing.getValue() != null && isProcessing.getValue()) return; // 중복 방지
      isProcessing.postValue(true);
      nfcTag.postValue(tagText);

      Log.d("NFC","NFC Scan 결과:" + tagText);

//      // API 호출 예시
//      new Thread(() -> {
//         sendApi(tagText); // 네트워크 처리
//         isProcessing.postValue(false);
//      }).start();
   }

   public void clearTag() {
      nfcTag.postValue(null);
      isProcessing.postValue(false);
   }
}