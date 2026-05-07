//
//  QRScannerView.swift
//  MaeumOn
//
//  Created by scoop dev on 11/4/25.
//

import SwiftUI
import AVFoundation
import MLKitBarcodeScanning
import MLKitVision

struct BarcodeScannerView: UIViewControllerRepresentable {
    @Binding var showScanner: Bool
    var onFinish: ((String) -> Void)?
    func makeUIViewController(context: Context) -> ScannerViewController {
        let vc = ScannerViewController()
        vc.delegate = context.coordinator
        return vc
    }

    func updateUIViewController(_ uiViewController: ScannerViewController, context: Context) {
        // 업데이트 필요시
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    class Coordinator: NSObject, ScannerViewControllerDelegate {
        var parent: BarcodeScannerView
        init(_ parent: BarcodeScannerView) {
            self.parent = parent
        }

        func didFind(code: String) {
            parent.showScanner = false
            parent.onFinish?(code)
        }
        
        func didCancel() {
            parent.showScanner = false
            parent.onFinish?("") // cancel 시 빈 값으로
        }
    }
}

class ScannerViewController: UIViewController {
    var delegate: ScannerViewControllerDelegate?
    private var captureSession: AVCaptureSession!
    private var barcodeScanner: BarcodeScanner!

    // ✅ 추가
    private var previewLayer: AVCaptureVideoPreviewLayer!
    private let overlayLayer = CAShapeLayer()
    private var isProcessing = false

    // 간단 추적(EMA) 용
    private var lastQuad: [CGPoint]? = nil
    private let smoothAlpha: CGFloat = 0.5 // 0~1 (높을수록 빠르게 따라감)
    
    private var navBar: UIView!
    private var backButton: UIButton!
    private var titleLabel: UILabel!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupCamera()
        setupBarcodeScanner()
        setupOverlay()
        setupNavigationBar()
    }
    
    private func setupNavigationBar() {
        navBar = UIView()
        navBar.backgroundColor = .white
        navBar.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(navBar)

        // 뒤로가기 버튼
        backButton = UIButton(type: .system)
        backButton.setImage(UIImage(systemName: "xmark"), for: .normal)
        backButton.tintColor = .systemGray
        backButton.translatesAutoresizingMaskIntoConstraints = false
        backButton.addTarget(self, action: #selector(didTapBack), for: .touchUpInside)
        navBar.addSubview(backButton)

        // 제목
        titleLabel = UILabel()
        titleLabel.text = "QR 코드 스캔"
        titleLabel.textColor = UIColor.fontMain
        titleLabel.font = UIFont(name: "Pretendard-SemmiBold", size: 18)
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        navBar.addSubview(titleLabel)
        
        // ✅ 설명 텍스트 추가
        let infoLabel = UILabel()
        infoLabel.text = "QR 코드를 스캔하시면 태그가 완료됩니다."
        infoLabel.textColor = .white
        infoLabel.font = UIFont(name: "Pretendard-SemmiBold", size: 16)
        infoLabel.backgroundColor = UIColor.black.withAlphaComponent(0.5)
        infoLabel.textAlignment = .center
        infoLabel.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(infoLabel)
        
        // ✅ 오토레이아웃
        NSLayoutConstraint.activate([
            navBar.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            navBar.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            navBar.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            navBar.heightAnchor.constraint(equalToConstant: 44),

            backButton.trailingAnchor.constraint(equalTo: navBar.trailingAnchor, constant: -16),
            backButton.centerYAnchor.constraint(equalTo: navBar.centerYAnchor),
            backButton.widthAnchor.constraint(equalToConstant: 30),
            backButton.heightAnchor.constraint(equalToConstant: 30),

            titleLabel.centerXAnchor.constraint(equalTo: navBar.centerXAnchor),
            titleLabel.centerYAnchor.constraint(equalTo: navBar.centerYAnchor),
            
            // ✅ infoLabel (설명 문구)
            infoLabel.topAnchor.constraint(equalTo: navBar.bottomAnchor),
            infoLabel.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            infoLabel.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            infoLabel.heightAnchor.constraint(equalToConstant: 44)
        ])
    }

    @objc private func didTapBack() {
        captureSession?.stopRunning()

        delegate?.didCancel()       // ✅ SwiftUI로 알림

        // SwiftUI NavigationLink일 경우 pop
        navigationController?.popViewController(animated: true)

        // fullScreenCover로 열렸을 경우 닫기용
        if presentingViewController != nil {
            dismiss(animated: true)
        }

    }
    
    func setupCamera() {
        captureSession = AVCaptureSession()
        captureSession.sessionPreset = .high

        guard let videoDevice = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back),
              let videoInput = try? AVCaptureDeviceInput(device: videoDevice),
              captureSession.canAddInput(videoInput) else {
            return
        }
        captureSession.addInput(videoInput)

        let videoOutput = AVCaptureVideoDataOutput()
        videoOutput.alwaysDiscardsLateVideoFrames = true
        videoOutput.setSampleBufferDelegate(self, queue: DispatchQueue(label: "videoQueue"))
        guard captureSession.canAddOutput(videoOutput) else { return }
        captureSession.addOutput(videoOutput)

        // 미리보기
        previewLayer = AVCaptureVideoPreviewLayer(session: captureSession)
        previewLayer.frame = view.layer.bounds
        previewLayer.videoGravity = .resizeAspectFill
        view.layer.addSublayer(previewLayer)

        // 출력 방향(세로 고정 예시)
        if let conn = videoOutput.connection(with: .video), conn.isVideoOrientationSupported {
            conn.videoOrientation = .portrait
        }

        captureSession.startRunning()
    }

    func setupBarcodeScanner() {
        let options = BarcodeScannerOptions(formats: [.qrCode])
        barcodeScanner = BarcodeScanner.barcodeScanner(options: options)
    }

    // ✅ 오버레이 레이어
    private func setupOverlay() {
        overlayLayer.frame = view.bounds
        overlayLayer.strokeColor = UIColor.systemGreen.cgColor
        overlayLayer.lineWidth = 3
        overlayLayer.fillColor = UIColor.clear.cgColor
        overlayLayer.lineJoin = .round
        view.layer.addSublayer(overlayLayer)
    }

    // ✅ 포인트 변환(MLKit 이미지 좌표 → 미리보기 레이어 좌표)
    private func convert(point: CGPoint, pixelWidth: CGFloat, pixelHeight: CGFloat) -> CGPoint {
        // 미리보기 레이어는 .resizeAspectFill
        // scale = 더 큰 축 기준, 그에 따른 offset 보정
        let viewSize = previewLayer.bounds.size
        let scale = max(viewSize.width / pixelWidth, viewSize.height / pixelHeight)
        let scaledWidth = pixelWidth * scale
        let scaledHeight = pixelHeight * scale
        let xOffset = (viewSize.width - scaledWidth) / 2.0
        let yOffset = (viewSize.height - scaledHeight) / 2.0

        // 현재 영상이 .portrait 이라고 가정 (videoOrientation = .portrait)
        // MLKit cornerPoints는 입력 이미지 좌표계(왼쪽상단 원점, x→, y↓)
        let x = point.x * scale + xOffset
        let y = point.y * scale + yOffset
        return CGPoint(x: x, y: y)
    }

    // ✅ 폴리곤 그리기 + EMA 스무딩
    private func drawOverlay(corners: [CGPoint]) {
        let smoothed: [CGPoint]
        if let last = lastQuad, last.count == corners.count {
            smoothed = zip(last, corners).map { (p0, p1) in
                CGPoint(
                    x: p0.x + smoothAlpha * (p1.x - p0.x),
                    y: p0.y + smoothAlpha * (p1.y - p0.y)
                )
            }
        } else {
            smoothed = corners
        }
        lastQuad = smoothed

        let path = UIBezierPath()
        if let first = smoothed.first {
            path.move(to: first)
            for p in smoothed.dropFirst() { path.addLine(to: p) }
            path.close()
        }
        overlayLayer.path = path.cgPath
    }
}

extension ScannerViewController: AVCaptureVideoDataOutputSampleBufferDelegate {
    func captureOutput(_ output: AVCaptureOutput,
                       didOutput sampleBuffer: CMSampleBuffer,
                       from connection: AVCaptureConnection) {

        // ✅ 프레임 과도 처리 방지
        if isProcessing { return }
        isProcessing = true
        defer { isProcessing = false }

        guard let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }
        let pixelWidth = CGFloat(CVPixelBufferGetWidth(pixelBuffer))
        let pixelHeight = CGFloat(CVPixelBufferGetHeight(pixelBuffer))

        let visionImage = VisionImage(buffer: sampleBuffer)

        // ✅ 실제 장치/카메라 방향에 맞춰 주세요 (간단히 .up 대신 아래 함수처럼 매핑 권장)
        visionImage.orientation = .up // 혹은 imageOrientation(from: UIDevice.current.orientation, cameraPosition: .back)
        if barcodeScanner != nil {
            barcodeScanner.process(visionImage) { [weak self] barcodes, error in
                guard let self = self else { return }
                guard error == nil, let barcodes = barcodes, let first = barcodes.first else {
                    // 인식 없으면 오버레이 지우기(선택 사항)
                    DispatchQueue.main.async { self.overlayLayer.path = nil }
                    return
                }
                
                // ✅ cornerPoints 사용
                if let nsValues = first.cornerPoints, !nsValues.isEmpty {
                    // MLKit은 [NSValue] (cgPointValue)로 제공
                    let imageSpacePoints = nsValues.map { $0.cgPointValue }
                    
                    // 이미지 좌표 → 레이어 좌표
                    let viewPoints = imageSpacePoints.map { pt in
                        self.convert(point: pt, pixelWidth: pixelWidth, pixelHeight: pixelHeight)
                    }
                    
                    DispatchQueue.main.async {
                        self.drawOverlay(corners: viewPoints)
                    }
                } else if first.frame != .zero {
                    // cornerPoints가 없을 때는 frame 사각형으로 대체
                    let rect = first.frame
                    let tl = self.convert(point: rect.origin, pixelWidth: pixelWidth, pixelHeight: pixelHeight)
                    let tr = self.convert(point: CGPoint(x: rect.maxX, y: rect.minY), pixelWidth: pixelWidth, pixelHeight: pixelHeight)
                    let br = self.convert(point: CGPoint(x: rect.maxX, y: rect.maxY), pixelWidth: pixelWidth, pixelHeight: pixelHeight)
                    let bl = self.convert(point: CGPoint(x: rect.minX, y: rect.maxY), pixelWidth: pixelWidth, pixelHeight: pixelHeight)
                    DispatchQueue.main.async {
                        self.drawOverlay(corners: [tl, tr, br, bl])
                    }
                }
                
                // 값 전달은 원하실 때만 (지속 추적용이라 스캔 즉시 종료 X)
                if let value = first.rawValue {
                    DispatchQueue.main.async {
                        self.delegate?.didFind(code: value) // 필요시 주석 처리 가능
                    }
                }
            }
        }
    }
}

protocol ScannerViewControllerDelegate {
    func didFind(code: String)
    func didCancel()
}
