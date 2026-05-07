//
//  BeaconManager.swift
//  MobilitEasyCustomer
//
//  Created by Scoop on 2022/08/05.
//
import Combine
import CoreLocation
import SwiftUI

class BeaconManager: NSObject, ObservableObject, CLLocationManagerDelegate{
    @Published var didChange = PassthroughSubject<Void, Never>()
    public let locationManager : CLLocationManager
    @Published var lastDistance = CLProximity.unknown
    private let beaconUUID = UUID(uuidString: "e45c4963-5384-5856-a0ff-09f8a9901938")!
    @Published var constraint = CLBeaconIdentityConstraint(uuid: UUID(uuidString: "e45c4963-5384-5856-a0ff-09f8a9901938")!)
    
    @Published var isBeaconOn = false
    @Published var beacons = [CLBeacon]()
    
    @Published var authorizationStatus: CLAuthorizationStatus
    @Published var lastSeenLocation: CLLocation?
    @Published var currentPlacemark: CLPlacemark?
    
    
    override init() {
        locationManager = CLLocationManager()
        authorizationStatus = locationManager.authorizationStatus

        super.init()
        
        locationManager.delegate = self
        locationManager.requestWhenInUseAuthorization()
        locationManager.allowsBackgroundLocationUpdates = true
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationManager.distanceFilter = 0.4
        locationManager.startUpdatingHeading()
        locationManager.startUpdatingLocation()
    }
    func requestPermission() {
        locationManager.requestWhenInUseAuthorization()
    }
    
    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        authorizationStatus = manager.authorizationStatus
    }
    
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        lastSeenLocation = locations.first
        fetchCountryAndCity(for: locations.first)
    }
    
    func startLocation() {
        locationManager.startUpdatingHeading()
        locationManager.startUpdatingLocation()
    }
    func stopLocation() {
        locationManager.stopUpdatingHeading()
        locationManager.stopUpdatingLocation()
    }
    func fetchCountryAndCity(for location: CLLocation?) {
        guard let location = location else { return }
        let geocoder = CLGeocoder()
        geocoder.reverseGeocodeLocation(location) { (placemarks, error) in
            self.currentPlacemark = placemarks?.first
        }
    }
    
    func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        if status == .authorizedWhenInUse {
            if CLLocationManager.isMonitoringAvailable(for: CLBeaconRegion.self){
                if CLLocationManager.isRangingAvailable() {
                    startScanning()
                }
            }
        }
    }
    /** Beacon Scan Start */
    func startScanning() {
        self.constraint = CLBeaconIdentityConstraint(uuid: beaconUUID)
        let beaconRegion = CLBeaconRegion(beaconIdentityConstraint: constraint, identifier: "scoopRegion")
        locationManager.startMonitoring(for: beaconRegion)
        locationManager.startRangingBeacons(satisfying: constraint)
        DispatchQueue.main.async {
            self.isBeaconOn = true
        }
    }
    func reStartScanning() {
        self.constraint = CLBeaconIdentityConstraint(uuid: beaconUUID)
        let beaconRegion = CLBeaconRegion(beaconIdentityConstraint: constraint, identifier: "scoopRegion")
        locationManager.stopMonitoring(for: beaconRegion)
        locationManager.stopRangingBeacons(satisfying: constraint)
        
        locationManager.startMonitoring(for: beaconRegion)
        locationManager.startRangingBeacons(satisfying: constraint)
        DispatchQueue.main.async {
            self.isBeaconOn = true
        }
    }
    
    /** Beacon Scan Stop */
    func stopScanning() {
        let beaconRegion = CLBeaconRegion(beaconIdentityConstraint: constraint, identifier: "scoopRegion")
        locationManager.stopMonitoring(for: beaconRegion)
        locationManager.stopRangingBeacons(satisfying: constraint)
        self.constraint = CLBeaconIdentityConstraint(uuid: beaconUUID)
        beacons = [CLBeacon]()
        DispatchQueue.main.async {
            self.isBeaconOn = false
        }
    }
    func locationManager(_ manager: CLLocationManager, didRange beacons: [CLBeacon], satisfying beaconConstraint: CLBeaconIdentityConstraint) {
        self.beacons = beacons
    }
    
    func update(distance: CLProximity) {
        lastDistance = distance
        didChange.send(())

    }
}

