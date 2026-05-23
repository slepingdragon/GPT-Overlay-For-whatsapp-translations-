import SwiftUI

enum BrandColor {
    static let bg = Color(red: 0.059, green: 0.067, blue: 0.082)
    static let surface = Color(red: 0.102, green: 0.114, blue: 0.141)
    static let surfaceMuted = Color(red: 0.137, green: 0.153, blue: 0.184)
    static let accent = Color(red: 0.0, green: 0.898, blue: 1.0)
    static let onAccent = Color(red: 0.0, green: 0.063, blue: 0.090)
    static let textMuted = Color(red: 0.647, green: 0.678, blue: 0.729)
    static let errorRed = Color(red: 1.0, green: 0.42, blue: 0.42)
}

private extension Comparable {
    func clamped(to limits: ClosedRange<Self>) -> Self {
        min(max(self, limits.lowerBound), limits.upperBound)
    }
}

extension Color {
    init(hex: String) {
        let trimmed = hex.hasPrefix("#") ? String(hex.dropFirst()) : hex
        var value: UInt64 = 0
        Scanner(string: trimmed).scanHexInt64(&value)
        if trimmed.count == 8 {
            let a = Double((value >> 24) & 0xFF) / 255
            let r = Double((value >> 16) & 0xFF) / 255
            let g = Double((value >> 8) & 0xFF) / 255
            let b = Double(value & 0xFF) / 255
            self.init(.sRGB, red: r, green: g, blue: b, opacity: a)
        } else {
            let r = Double((value >> 16) & 0xFF) / 255
            let g = Double((value >> 8) & 0xFF) / 255
            let b = Double(value & 0xFF) / 255
            self.init(red: r, green: g, blue: b)
        }
    }

    func toHex(includeAlpha: Bool = false) -> String {
        let ui = UIColor(self)
        var r: CGFloat = 0; var g: CGFloat = 0; var b: CGFloat = 0; var a: CGFloat = 0
        ui.getRed(&r, green: &g, blue: &b, alpha: &a)
        let R = Int((r * 255).rounded()).clamped(to: 0...255)
        let G = Int((g * 255).rounded()).clamped(to: 0...255)
        let B = Int((b * 255).rounded()).clamped(to: 0...255)
        if includeAlpha {
            let A = Int((a * 255).rounded()).clamped(to: 0...255)
            return String(format: "%02X%02X%02X%02X", A, R, G, B)
        }
        return String(format: "%02X%02X%02X", R, G, B)
    }

    func contrastingForeground() -> Color {
        let ui = UIColor(self)
        var r: CGFloat = 0; var g: CGFloat = 0; var b: CGFloat = 0; var a: CGFloat = 0
        ui.getRed(&r, green: &g, blue: &b, alpha: &a)
        let luminance = 0.2126 * Double(r) + 0.7152 * Double(g) + 0.0722 * Double(b)
        return luminance > 0.55 ? Color(red: 0.067, green: 0.067, blue: 0.067) : .white
    }
}
