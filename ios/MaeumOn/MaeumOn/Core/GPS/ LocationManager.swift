//
//   LocationManager.swift
//  MaeumOn
//
//  Created by scoop dev on 11/24/25.
//

import CoreLocation
import Combine
struct Point: Equatable {
    let lat: Double
    let lng: Double
    let accuracy: CLLocationAccuracy
    let timestamp: Int64
}
class LocationManager: NSObject, CLLocationManagerDelegate, ObservableObject {
    @Published var point: CLLocation?
    private var locationManager: CLLocationManager?
    private var isTracking = false

    override init() {
        super.init()

        setupLocationManager()
    }

    // MARK: - Location Manager Setup

    private func setupLocationManager() {
        locationManager = CLLocationManager()
        locationManager?.delegate = self
        locationManager?.desiredAccuracy = kCLLocationAccuracyBest
        locationManager?.distanceFilter = 10 // 10미터마다 업데이트
        locationManager?.allowsBackgroundLocationUpdates = false
    }

    // MARK: - Location Tracking

    func startLocationTracking() {
        guard !isTracking else {
            print("[WebViewBridge] Already tracking location")
            return
        }

        // 권한 확인 및 요청
        let status = locationManager?.authorizationStatus ?? .notDetermined

        switch status {
        case .notDetermined:
            locationManager?.requestWhenInUseAuthorization()

        case .authorizedWhenInUse, .authorizedAlways:
            locationManager?.startUpdatingLocation()
            isTracking = true
            print("[WebViewBridge] Location tracking started")

        case .denied, .restricted:
            print("[WebViewBridge] Location permission denied")

        @unknown default:
            break
        }
    }

    func stopLocationTracking() {
        guard isTracking else {
            return
        }

        locationManager?.stopUpdatingLocation()
        isTracking = false
        print("[WebViewBridge] Location tracking stopped")
    }

    // MARK: - CLLocationManagerDelegate

    func locationManager(
        _ manager: CLLocationManager,
        didUpdateLocations locations: [CLLocation]
    ) {
        guard let location = locations.last else { return }

        DispatchQueue.main.async {
            self.point = location
        }
            
    }

    func locationManager(
        _ manager: CLLocationManager,
        didFailWithError error: Error
    ) {
        print("[WebViewBridge] Location error: \(error.localizedDescription)")
    }

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        let status = manager.authorizationStatus

        if status == .authorizedWhenInUse || status == .authorizedAlways {
            if isTracking {
                manager.startUpdatingLocation()
            }
        }
    }
}
