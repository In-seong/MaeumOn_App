package com.spoon.maeumon.feature.data;

public class BLEData {
   private int routeId;
   private int rssi;
   private double distance;

   public int getRouteId() {
      return routeId;
   }

   public void setRouteId(int routeId) {
      this.routeId = routeId;
   }

   public int getRssi() {
      return rssi;
   }

   public void setRssi(int rssi) {
      this.rssi = rssi;
   }

   public double getDistance() {
      return distance;
   }

   public void setDistance(double distance) {
      this.distance = distance;
   }
}
