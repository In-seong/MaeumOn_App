package com.spoon.maeumon.feature.splash.model;

import android.os.Looper;

import com.spoon.maeumon.feature.data.DataParam;
import com.spoon.maeumon.feature.splash.ui.SplashActivity;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SplashViewModel extends ViewModel {
   public SplashActivity activity;
   private MutableLiveData<String> phone = new MutableLiveData<>(null);
   /**
    * 사용자 폰 번호
    * @return
    */
   public LiveData<String> getPhone() {
      return phone;
   }
   /**
    * 사용자 번호
    * @return  ***-****-****
    */
   public String getFormatPhone() {
      StringBuffer buffer = new StringBuffer();
      buffer.append(phone.getValue());
      buffer.insert(7,"-");
      buffer.insert(3,"-");
      return buffer.toString();
   }
   /**
    * 사용자 폰 번호
    * @param phone
    */
   public void setPhone(String phone) {
      if (Looper.myLooper() == Looper.getMainLooper()) {
         this.phone.setValue(phone);
      } else {
         this.phone.postValue(phone);
      }
   }

   private MutableLiveData<DataParam<String>> sendCode = new MutableLiveData<>();

   public LiveData<DataParam<String>> getSendCode() {
      return sendCode;
   }

   public void setSendCode(DataParam<String> sendCode) {
      if (Looper.getMainLooper() == Looper.myLooper()) {
         this.sendCode.setValue(sendCode);
      } else {
         this.sendCode.postValue(sendCode);
      }
   }

   private MutableLiveData<DataParam<String>> verifyCode = new MutableLiveData<>();

   public LiveData<DataParam<String>> getVerifyCode() {
      return verifyCode;
   }

   public void setVerifyCode(DataParam<String> verifyCode) {
      if (Looper.getMainLooper() == Looper.myLooper()) {
         this.verifyCode.setValue(verifyCode);
      } else {
         this.verifyCode.postValue(verifyCode);
      }
   }
}
