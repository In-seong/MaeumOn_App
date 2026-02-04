package com.spoon.maeumon.feature.ble.model;

import java.util.List;
import java.util.UUID;

public class BleDevice {
   private final String name;
   private final String address;
   private final List<UUID> serviceUuids; // BLE 서비스 UUID

   public BleDevice(String name, String address, List<UUID> serviceUuids) {
      this.name = name;
      this.address = address;
      this.serviceUuids = serviceUuids;
   }

   public String getName() {
      return name;
   }

   public String getAddress() {
      return address;
   }

   public List<UUID> getServiceUuids() {
      return serviceUuids;
   }
}
