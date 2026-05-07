//
//  WebView.swift
//  MaeumOn
//
//  Created by scoop dev on 11/3/25.
//

import SwiftUI
import WebKit
import AVFoundation

struct WebViewContainer: UIViewRepresentable {
    let url: URL?
    @Binding var webView: WKWebView?
    @Binding var isError: Bool
    @Binding var shouldReload: Bool
    var onMessage: ((String, [String: Any]) -> Void)?
    var onLoadComplete: (() -> Void)?

    func makeCoordinator() -> WebViewCoordinator {
        WebViewCoordinator(self)
    }

    func makeUIView(context: Context) -> WKWebView {

        // WebView 설정
        let config = WKWebViewConfiguration()
        config.allowsInlineMediaPlayback = true
        config.mediaTypesRequiringUserActionForPlayback = []
       
        // JS → Native 브릿지 등록
        config.userContentController.add(context.coordinator, name: "iOSBridge")

        let wv = WKWebView(frame: .zero, configuration: config)

        wv.navigationDelegate = context.coordinator
        wv.uiDelegate = context.coordinator
        wv.scrollView.bounces = false
        wv.scrollView.bouncesZoom = false
        wv.scrollView.minimumZoomScale = 1.0
        wv.scrollView.maximumZoomScale = 1.0
        wv.scrollView.pinchGestureRecognizer?.isEnabled = false
        wv.scrollView.isMultipleTouchEnabled = false
        wv.isOpaque = true
        wv.backgroundColor = .white

        // Coordinator에 webView 참조 저장
        context.coordinator.webView = wv

        // 바인딩 업데이트
        DispatchQueue.main.async {
            self.webView = wv
        }

        if let myURL = url {
            var request = URLRequest(url: myURL)
            if let cookies = HTTPCookieStorage.shared.cookies(for: myURL) {
                request.allHTTPHeaderFields = HTTPCookie.requestHeaderFields(with: cookies)
            }
            wv.load(request)
        }

        return wv
    }

    func updateUIView(_ uiView: WKWebView, context: Context) {
        // shouldReload가 true면 리로드 실행
        if shouldReload {
            DispatchQueue.main.async {
                self.shouldReload = false
            }
            if let myURL = url {
                var request = URLRequest(url: myURL)
                if let cookies = HTTPCookieStorage.shared.cookies(for: myURL) {
                    request.allHTTPHeaderFields = HTTPCookie.requestHeaderFields(with: cookies)
                }
                uiView.load(request)
            }
        }
    }
}

class WebViewCoordinator: NSObject {
    var parent: WebViewContainer
    var webView: WKWebView?

    init(_ parent: WebViewContainer) {
        self.parent = parent
    }
}

// MARK: - WebView Navigation Delegate
extension WebViewCoordinator: WKNavigationDelegate {
   // 정상 로드 시
   func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
      parent.isError = false
      
      defaultJS(webView)
      // WebView 로드 완료 콜백
      parent.onLoadComplete?()
   }
   // 에러
   func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
      parent.isError = true
   }

   func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
      parent.isError = true
   }
}

// MARK: - JS → iOS 메시지 처리
extension WebViewCoordinator: WKScriptMessageHandler {
    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        guard message.name == "iOSBridge" else { return }
        guard let body = message.body as? [String: Any],
              let action = body["action"] as? String else {
            print("Invalid message format")
            return
        }

        parent.onMessage?(action, body)
    }
}

// MARK: - WKUIDelegate (alert, confirm, prompt 처리)
extension WebViewCoordinator: WKUIDelegate {
    func webView(_ webView: WKWebView, runJavaScriptAlertPanelWithMessage message: String, initiatedByFrame frame: WKFrameInfo, completionHandler: @escaping () -> Void) {
        let alert = UIAlertController(title: nil, message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "확인", style: .default) { _ in
            completionHandler()
        })

        if let rootVC = UIApplication.shared.windows.first?.rootViewController {
            rootVC.present(alert, animated: true)
        } else {
            completionHandler()
        }
    }

    func webView(_ webView: WKWebView, runJavaScriptConfirmPanelWithMessage message: String, initiatedByFrame frame: WKFrameInfo, completionHandler: @escaping (Bool) -> Void) {
        let alert = UIAlertController(title: nil, message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "취소", style: .cancel) { _ in
            completionHandler(false)
        })
        alert.addAction(UIAlertAction(title: "확인", style: .default) { _ in
            completionHandler(true)
        })

        if let rootVC = UIApplication.shared.windows.first?.rootViewController {
            rootVC.present(alert, animated: true)
        } else {
            completionHandler(false)
        }
    }

    func webView(_ webView: WKWebView, runJavaScriptTextInputPanelWithPrompt prompt: String, defaultText: String?, initiatedByFrame frame: WKFrameInfo, completionHandler: @escaping (String?) -> Void) {
        let alert = UIAlertController(title: nil, message: prompt, preferredStyle: .alert)
        alert.addTextField { textField in
            textField.text = defaultText
        }
        alert.addAction(UIAlertAction(title: "취소", style: .cancel) { _ in
            completionHandler(nil)
        })
        alert.addAction(UIAlertAction(title: "확인", style: .default) { _ in
            completionHandler(alert.textFields?.first?.text)
        })

        if let rootVC = UIApplication.shared.windows.first?.rootViewController {
            rootVC.present(alert, animated: true)
        } else {
            completionHandler(nil)
        }
    }
}
// MARK:
extension WebViewCoordinator {
   
   // 웹뷰 줌, 더블 클릭 등 ios 기본 웹 제스쳐 제거 등...
   func defaultJS(_ webView: WKWebView) {
      var js = """
      var meta = document.createElement('meta');
      meta.name = 'viewport';
      meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no';
      document.getElementsByTagName('head')[0].appendChild(meta);
      """

      js += """
      document.documentElement.style.webkitUserSelect = 'none';
      document.documentElement.style.webkitTouchCallout = 'none';
      """

      js += """
      document.addEventListener('dragstart', function(e) {
          if (e.target.tagName === 'IMG') {
              e.preventDefault();
          }
      });
      """
      webView.evaluateJavaScript(js, completionHandler: nil)
   }
}

