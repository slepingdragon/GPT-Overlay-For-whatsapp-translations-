import Foundation
import UIKit

enum AppGroup {
    static let identifier = "group.com.banshee.translatekeyboard"
}

enum SharedKeys {
    static let deeplApiKey = "deepl_api_key"
    static let targetLang = "target_lang"
    static let defaultContactNumber = "default_contact_number"
    static let copyButtonColorHex = "copy_button_color_hex"
    static let sendButtonColorHex = "send_button_color_hex"
    static let inputBoxColorHex = "input_box_color_hex"
    static let accentColorHex = "accent_color_hex"
    static let customColors = "custom_colors"
    static let autoTranslate = "auto_translate"
    static let favoriteLanguages = "favorite_languages"
    static let savedSource = "saved_source"
    static let savedTranslation = "saved_translation"
    static let history = "history_v1"
}

enum SharedColorPresets {
    static let all: [String] = [
        "#00E5FF", "#1E88E5", "#7C4DFF", "#BA68C8",
        "#FF4081", "#EF5350", "#FF7043", "#FFB300",
        "#FFD600", "#66BB6A", "#26A69A", "#00ACC1",
        "#FFFFFF", "#B0BEC5", "#607D8B", "#111111",
    ]
    static let defaultCopy = "#00E5FF"
    static let defaultSend = "#FFFFFF"
    static let defaultInput = "#8C1A1D24"
}

struct HistoryEntry: Identifiable, Codable, Equatable {
    let id: String
    let source: String
    let translation: String
    let targetLang: String
    let timestamp: Double
    var favorite: Bool

    static func create(source: String, translation: String, target: TargetLang) -> HistoryEntry {
        HistoryEntry(
            id: UUID().uuidString,
            source: source,
            translation: translation,
            targetLang: target.rawValue,
            timestamp: Date().timeIntervalSince1970,
            favorite: false,
        )
    }
}

enum TargetLang: String, CaseIterable {
    case en, id, su, es, fr, de, it, pt, nl, pl, ru, ja, ko, zh, ar, tr

    var label: String {
        switch self {
        case .en: return "EN"
        case .id: return "ID"
        case .su: return "SU"
        case .es: return "ES"
        case .fr: return "FR"
        case .de: return "DE"
        case .it: return "IT"
        case .pt: return "PT"
        case .nl: return "NL"
        case .pl: return "PL"
        case .ru: return "RU"
        case .ja: return "JA"
        case .ko: return "KO"
        case .zh: return "ZH"
        case .ar: return "AR"
        case .tr: return "TR"
        }
    }
    var fullName: String {
        switch self {
        case .en: return "English"
        case .id: return "Indonesian"
        case .su: return "Sundanese"
        case .es: return "Spanish"
        case .fr: return "French"
        case .de: return "German"
        case .it: return "Italian"
        case .pt: return "Portuguese"
        case .nl: return "Dutch"
        case .pl: return "Polish"
        case .ru: return "Russian"
        case .ja: return "Japanese"
        case .ko: return "Korean"
        case .zh: return "Chinese"
        case .ar: return "Arabic"
        case .tr: return "Turkish"
        }
    }
    var deeplCode: String? {
        switch self {
        case .en: return "EN-US"
        case .id: return "ID"
        case .su: return nil
        case .es: return "ES"
        case .fr: return "FR"
        case .de: return "DE"
        case .it: return "IT"
        case .pt: return "PT-BR"
        case .nl: return "NL"
        case .pl: return "PL"
        case .ru: return "RU"
        case .ja: return "JA"
        case .ko: return "KO"
        case .zh: return "ZH"
        case .ar: return "AR"
        case .tr: return "TR"
        }
    }
    var myMemoryCode: String {
        switch self {
        case .en: return "en"
        case .id: return "id"
        case .su: return "su"
        case .es: return "es"
        case .fr: return "fr"
        case .de: return "de"
        case .it: return "it"
        case .pt: return "pt"
        case .nl: return "nl"
        case .pl: return "pl"
        case .ru: return "ru"
        case .ja: return "ja"
        case .ko: return "ko"
        case .zh: return "zh"
        case .ar: return "ar"
        case .tr: return "tr"
        }
    }
}

struct SharedSettings {
    private static var defaults: UserDefaults {
        UserDefaults(suiteName: AppGroup.identifier) ?? .standard
    }

    static var deeplApiKey: String {
        get { defaults.string(forKey: SharedKeys.deeplApiKey) ?? "" }
        set { defaults.set(newValue.trimmingCharacters(in: .whitespaces), forKey: SharedKeys.deeplApiKey) }
    }

    static var targetLang: TargetLang {
        get {
            let raw = defaults.string(forKey: SharedKeys.targetLang) ?? TargetLang.id.rawValue
            return TargetLang(rawValue: raw) ?? .id
        }
        set { defaults.set(newValue.rawValue, forKey: SharedKeys.targetLang) }
    }

    static var defaultContactNumber: String {
        get { defaults.string(forKey: SharedKeys.defaultContactNumber) ?? "" }
        set {
            let digits = newValue.filter { $0.isNumber }
            defaults.set(digits, forKey: SharedKeys.defaultContactNumber)
        }
    }

    static var copyButtonColorHex: String {
        get { defaults.string(forKey: SharedKeys.copyButtonColorHex) ?? SharedColorPresets.defaultCopy }
        set { defaults.set(newValue, forKey: SharedKeys.copyButtonColorHex) }
    }

    static var sendButtonColorHex: String {
        get { defaults.string(forKey: SharedKeys.sendButtonColorHex) ?? SharedColorPresets.defaultSend }
        set { defaults.set(newValue, forKey: SharedKeys.sendButtonColorHex) }
    }

    static var inputBoxColorHex: String {
        get { defaults.string(forKey: SharedKeys.inputBoxColorHex) ?? SharedColorPresets.defaultInput }
        set { defaults.set(newValue, forKey: SharedKeys.inputBoxColorHex) }
    }

    static var accentColorHex: String {
        get { defaults.string(forKey: SharedKeys.accentColorHex) ?? SharedColorPresets.defaultCopy }
        set { defaults.set(newValue, forKey: SharedKeys.accentColorHex) }
    }

    static var customColorHexes: [String] {
        get {
            (defaults.string(forKey: SharedKeys.customColors) ?? "")
                .split(separator: ",")
                .map { String($0).trimmingCharacters(in: .whitespaces) }
                .filter { !$0.isEmpty }
        }
        set {
            defaults.set(newValue.joined(separator: ","), forKey: SharedKeys.customColors)
        }
    }

    static func addCustomColor(_ hex: String) {
        var list = customColorHexes
        list.removeAll { $0.caseInsensitiveCompare(hex) == .orderedSame }
        list.insert(hex, at: 0)
        customColorHexes = Array(list.prefix(24))
    }

    static func removeCustomColor(_ hex: String) {
        customColorHexes = customColorHexes.filter { $0.caseInsensitiveCompare(hex) != .orderedSame }
    }

    static var autoTranslate: Bool {
        get {
            if defaults.object(forKey: SharedKeys.autoTranslate) == nil { return true }
            return defaults.bool(forKey: SharedKeys.autoTranslate)
        }
        set { defaults.set(newValue, forKey: SharedKeys.autoTranslate) }
    }

    static var favoriteLanguages: [TargetLang] {
        get {
            let raw = defaults.string(forKey: SharedKeys.favoriteLanguages)
            if raw == nil || raw!.isEmpty {
                return [.en, .id, .su]
            }
            let parsed = raw!.split(separator: ",").compactMap {
                TargetLang(rawValue: String($0).trimmingCharacters(in: .whitespaces))
            }
            return parsed.isEmpty ? [.en, .id, .su] : parsed
        }
        set {
            let list = newValue.isEmpty ? [TargetLang.en] : newValue
            defaults.set(list.map { $0.rawValue }.joined(separator: ","), forKey: SharedKeys.favoriteLanguages)
        }
    }

    static func toggleFavoriteLanguage(_ lang: TargetLang) {
        var list = favoriteLanguages
        if list.contains(lang) { list.removeAll { $0 == lang } } else { list.append(lang) }
        favoriteLanguages = list
    }

    static var backgroundImageURL: URL? {
        guard let container = FileManager.default
            .containerURL(forSecurityApplicationGroupIdentifier: AppGroup.identifier)
        else { return nil }
        return container.appendingPathComponent("background.jpg")
    }

    static var hasBackgroundImage: Bool {
        guard let url = backgroundImageURL else { return false }
        return FileManager.default.fileExists(atPath: url.path)
    }

    static func writeBackgroundImage(_ data: Data?) {
        guard let url = backgroundImageURL else { return }
        if let data {
            try? data.write(to: url, options: .atomic)
        } else {
            try? FileManager.default.removeItem(at: url)
        }
    }

    static func loadBackgroundImage() -> UIImage? {
        guard let url = backgroundImageURL,
              FileManager.default.fileExists(atPath: url.path)
        else { return nil }
        return UIImage(contentsOfFile: url.path)
    }

    static var savedSource: String {
        get { defaults.string(forKey: SharedKeys.savedSource) ?? "" }
        set { defaults.set(newValue, forKey: SharedKeys.savedSource) }
    }

    static var savedTranslation: String {
        get { defaults.string(forKey: SharedKeys.savedTranslation) ?? "" }
        set { defaults.set(newValue, forKey: SharedKeys.savedTranslation) }
    }

    static func loadHistory() -> [HistoryEntry] {
        guard let data = defaults.data(forKey: SharedKeys.history),
              let entries = try? JSONDecoder().decode([HistoryEntry].self, from: data)
        else { return [] }
        return entries
    }

    static func saveHistory(_ entries: [HistoryEntry]) {
        let capped = Array(entries.prefix(500))
        if let data = try? JSONEncoder().encode(capped) {
            defaults.set(data, forKey: SharedKeys.history)
        }
    }

    static func addHistoryEntry(source: String, translation: String, target: TargetLang) {
        guard !source.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
              !translation.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        else { return }
        var existing = loadHistory()
        let trimmedSource = source.trimmingCharacters(in: .whitespaces)
        if let idx = existing.firstIndex(where: {
            $0.source.trimmingCharacters(in: .whitespaces) == trimmedSource
                && $0.targetLang == target.rawValue
        }) {
            var merged = existing[idx]
            merged = HistoryEntry(
                id: merged.id,
                source: source,
                translation: translation,
                targetLang: target.rawValue,
                timestamp: Date().timeIntervalSince1970,
                favorite: merged.favorite,
            )
            existing.remove(at: idx)
            existing.insert(merged, at: 0)
        } else {
            existing.insert(HistoryEntry.create(source: source, translation: translation, target: target), at: 0)
        }
        saveHistory(existing)
    }

    static func deleteHistoryEntry(id: String) {
        saveHistory(loadHistory().filter { $0.id != id })
    }

    static func toggleFavorite(id: String) {
        let updated = loadHistory().map { entry -> HistoryEntry in
            guard entry.id == id else { return entry }
            var e = entry; e.favorite.toggle(); return e
        }
        saveHistory(updated)
    }

    static func clearHistory() {
        saveHistory([])
    }
}
