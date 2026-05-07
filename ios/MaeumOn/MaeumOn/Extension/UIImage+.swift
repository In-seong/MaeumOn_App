//
//  UIImage+.swift
//  MaeumOn
//
//  Created by scoop dev on 11/19/25.
//
import SwiftUI

extension UIImage {
    func resized(height: CGFloat) -> UIImage? {
        let scale = height / size.height
        let newWidth = size.width * scale
        let newSize = CGSize(width: newWidth, height: height)

        let renderer = UIGraphicsImageRenderer(size: newSize)
        return renderer.image { _ in
            self.draw(in: CGRect(origin: .zero, size: newSize))
        }
    }
}
