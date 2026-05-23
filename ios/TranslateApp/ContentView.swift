import SwiftUI
import UIKit
import PhotosUI

struct ContentView: View {
    @State private var sourceText: String = SharedSettings.savedSource
    @State private var translatedText: String = SharedSettings.savedTranslation
    @State private var targetLang: TargetLang = SharedSettings.targetLang
    @State private var isLoading: Bool = false
    @State private var errorMessage: String? = nil
    @State private var showSettings: Bool = false
    @State private var showHistory: Bool = false
    @State private var showCopied: Bool = false
    @State private var debounceTask: Task<Void, Never>? = nil
    @State private var backgroundImage: UIImage? = SharedSettings.loadBackgroundImage()
    @State private var copyColor: Color = Color(hex: SharedSettings.copyButtonColorHex)
    @State private var sendColor: Color = Color(hex: SharedSettings.sendButtonColorHex)
    @State private var inputBoxColor: Color = Color(hex: SharedSettings.inputBoxColorHex)
    @State private var accentColor: Color = Color(hex: SharedSettings.accentColorHex)
    @State private var autoTranslate: Bool = SharedSettings.autoTranslate
    @State private var favoriteLanguages: [TargetLang] = SharedSettings.favoriteLanguages
    @State private var showLanguagePicker: Bool = false
    @State private var historyEntries: [HistoryEntry] = SharedSettings.loadHistory()

    private var hasBackground: Bool { backgroundImage != nil }
    private var cardBackground: Color {
        let ui = UIColor(inputBoxColor)
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        ui.getRed(&r, green: &g, blue: &b, alpha: &a)
        let capped = min(Double(a), 0.65)
        return Color(red: Double(r), green: Double(g), blue: Double(b), opacity: capped)
    }

    private func refreshHistory() { historyEntries = SharedSettings.loadHistory() }

    var body: some View {
        ZStack(alignment: .topLeading) {
            if let img = backgroundImage {
                Image(uiImage: img)
                    .resizable()
                    .scaledToFill()
                    .ignoresSafeArea()
                LinearGradient(
                    colors: [Color.black.opacity(0.55), Color.black.opacity(0.35), Color.black.opacity(0.55)],
                    startPoint: .top, endPoint: .bottom,
                )
                .ignoresSafeArea()
            } else {
                BrandColor.bg.ignoresSafeArea()
            }

            VStack(spacing: 0) {
                HStack(alignment: .top) {
                    VStack(spacing: 4) {
                        Button { showSettings = true } label: {
                            Image(systemName: "gearshape")
                                .font(.title3)
                                .foregroundStyle(.white)
                                .padding(10)
                        }
                        Button { showHistory = true } label: {
                            Image(systemName: "clock.arrow.circlepath")
                                .font(.title3)
                                .foregroundStyle(.white)
                                .padding(10)
                        }
                    }
                    Spacer()
                }
                .padding(.leading, 4)
                .padding(.top, 4)

                if historyEntries.isEmpty {
                    Spacer()
                    Text("Your translations show up here")
                        .foregroundStyle(.white.opacity(0.55))
                        .font(.footnote)
                    Spacer()
                } else {
                    ScrollViewReader { proxy in
                        ScrollView {
                            LazyVStack(spacing: 6) {
                                ForEach(historyEntries) { entry in
                                    HistoryBubble(
                                        entry: entry,
                                        onCopy: {
                                            UIPasteboard.general.string = entry.translation
                                            flashCopied()
                                        },
                                        onFavorite: {
                                            SharedSettings.toggleFavorite(id: entry.id)
                                            refreshHistory()
                                        },
                                        onUse: {
                                            sourceText = entry.source
                                            translatedText = entry.translation
                                            SharedSettings.savedSource = entry.source
                                            SharedSettings.savedTranslation = entry.translation
                                        },
                                    )
                                    .id(entry.id)
                                }
                            }
                            .padding(.horizontal, 8)
                            .padding(.vertical, 8)
                        }
                        .onChange(of: historyEntries.first?.id) { _, newId in
                            if let id = newId {
                                withAnimation { proxy.scrollTo(id, anchor: .top) }
                            }
                        }
                    }
                }
            }

            if showCopied {
                VStack {
                    Spacer()
                    copiedToast.padding(.bottom, 160)
                }
                .frame(maxWidth: .infinity)
            }
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            VStack(spacing: 8) {
                languageBar
                    .padding(.horizontal, 12)
                combinedTranslateContainer
            }
            .padding(.top, 6)
        }
        .sheet(isPresented: $showSettings, onDismiss: refreshTheme) {
            SettingsSheet()
                .presentationBackground(Color(red: 0.063, green: 0.071, blue: 0.086))
        }
        .sheet(isPresented: $showLanguagePicker) {
            LanguagePickerSheet(
                selected: targetLang,
                favorites: favoriteLanguages,
                onSelect: { picked in
                    targetLang = picked
                    SharedSettings.targetLang = picked
                    showLanguagePicker = false
                },
                onToggleFavorite: { lang in
                    SharedSettings.toggleFavoriteLanguage(lang)
                    favoriteLanguages = SharedSettings.favoriteLanguages
                },
                onDismiss: { showLanguagePicker = false },
            )
            .presentationBackground(Color(red: 0.063, green: 0.071, blue: 0.086))
        }
        .sheet(isPresented: $showHistory) {
            HistorySheet(
                onUseEntry: { entry in
                    sourceText = entry.source
                    translatedText = entry.translation
                    SharedSettings.savedSource = entry.source
                    SharedSettings.savedTranslation = entry.translation
                    showHistory = false
                },
                onCopyEntry: { entry in
                    UIPasteboard.general.string = entry.translation
                    flashCopied()
                },
            )
            .presentationBackground(Color(red: 0.063, green: 0.071, blue: 0.086))
        }
        .onChange(of: sourceText) { _, new in
            SharedSettings.savedSource = new
            scheduleTranslate()
        }
        .onChange(of: targetLang) { _, new in
            SharedSettings.targetLang = new
            scheduleTranslate(immediate: true)
        }
        .onChange(of: translatedText) { _, new in
            SharedSettings.savedTranslation = new
        }
        .tint(accentColor)
    }

    private func refreshTheme() {
        backgroundImage = SharedSettings.loadBackgroundImage()
        copyColor = Color(hex: SharedSettings.copyButtonColorHex)
        sendColor = Color(hex: SharedSettings.sendButtonColorHex)
        inputBoxColor = Color(hex: SharedSettings.inputBoxColorHex)
        accentColor = Color(hex: SharedSettings.accentColorHex)
        autoTranslate = SharedSettings.autoTranslate
        refreshHistory()
    }

    private var languageBar: some View {
        let list = favoriteLanguages.isEmpty ? [TargetLang.en] : favoriteLanguages
        return ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(list, id: \.self) { lang in
                    Button { targetLang = lang } label: {
                        LangChip(
                            label: lang.label,
                            highlighted: lang == targetLang,
                            hasBackground: hasBackground,
                        )
                    }
                    .buttonStyle(.plain)
                }
                Button { showLanguagePicker = true } label: {
                    PlusChip(hasBackground: hasBackground)
                }
                .buttonStyle(.plain)
            }
        }
    }

    private var combinedTranslateContainer: some View {
        VStack(spacing: 0) {
            // Translation result section
            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 6) {
                    Text("→ \(targetLang.fullName)")
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundStyle(BrandColor.accent)
                    if isLoading { ProgressView().scaleEffect(0.55) }
                    Spacer()
                    if !translatedText.isEmpty {
                        Button {
                            UIPasteboard.general.string = translatedText
                            SharedSettings.addHistoryEntry(source: sourceText, translation: translatedText, target: targetLang)
                            refreshHistory()
                            flashCopied()
                        } label: {
                            Image(systemName: "doc.on.doc")
                                .font(.system(size: 13))
                                .foregroundStyle(BrandColor.textMuted)
                        }
                    }
                }
                Group {
                    if let err = errorMessage {
                        Text("⚠ \(err)").foregroundStyle(BrandColor.errorRed).font(.system(size: 14))
                    } else if translatedText.isEmpty && !autoTranslate && !sourceText.isEmpty {
                        HStack {
                            Spacer()
                            Button {
                                scheduleTranslate(immediate: true, force: true)
                            } label: {
                                HStack(spacing: 6) {
                                    Image(systemName: "paperplane")
                                        .font(.system(size: 12))
                                    Text("Translate")
                                        .font(.system(size: 13))
                                }
                                .padding(.horizontal, 14)
                                .padding(.vertical, 4)
                                .overlay(
                                    Capsule().stroke(BrandColor.accent, lineWidth: 1)
                                )
                                .foregroundStyle(BrandColor.accent)
                            }
                        }
                    } else if translatedText.isEmpty {
                        Text(sourceText.isEmpty ? "Translation appears here" : "…")
                            .foregroundStyle(BrandColor.textMuted).font(.system(size: 14))
                    } else {
                        Text(translatedText)
                            .font(.system(size: 15, weight: .medium))
                            .foregroundStyle(.white)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 8)

            Rectangle()
                .fill(BrandColor.accent.opacity(0.25))
                .frame(height: 0.5)

            // Input row with send button INSIDE container
            HStack(alignment: .center, spacing: 4) {
                TextField("Type a message…", text: $sourceText, axis: .vertical)
                    .lineLimit(1...3)
                    .font(.system(size: 14))
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .foregroundStyle(.white)
                    .tint(BrandColor.accent)
                if !sourceText.isEmpty {
                    Button {
                        sourceText = ""
                        translatedText = ""
                        SharedSettings.savedSource = ""
                        SharedSettings.savedTranslation = ""
                    } label: {
                        Image(systemName: "xmark.circle.fill")
                            .font(.system(size: 17))
                            .foregroundStyle(BrandColor.textMuted)
                    }
                }
                Button {
                    guard canSend else { return }
                    SharedSettings.addHistoryEntry(source: sourceText, translation: translatedText, target: targetLang)
                    refreshHistory()
                    sendToWhatsApp(translatedText)
                } label: {
                    Image(systemName: "paperplane.fill")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(sendColor.contrastingForeground())
                        .frame(width: 36, height: 36)
                        .background(sendColor.opacity(canSend ? 1.0 : 0.30), in: Circle())
                        .shadow(color: canSend ? sendColor.opacity(0.55) : .clear, radius: 8, x: 0, y: 0)
                }
                .disabled(!canSend)
                .padding(.trailing, 6)
                .animation(.easeInOut(duration: 0.15), value: canSend)
            }
            .frame(minHeight: 48)
        }
        .background(cardBackground.ignoresSafeArea(edges: .bottom))
        .overlay(
            Rectangle()
                .fill(Color.white.opacity(0.12))
                .frame(height: 0.6)
                .frame(maxWidth: .infinity),
            alignment: .top,
        )
    }

    private var canSend: Bool {
        !sourceText.isEmpty && !translatedText.isEmpty
    }

    private var copiedToast: some View {
        Text("Copied to clipboard")
            .font(.subheadline.weight(.medium))
            .padding(.horizontal, 14).padding(.vertical, 10)
            .background(.black.opacity(0.85), in: Capsule())
            .foregroundStyle(.white)
            .padding(.bottom, 32)
            .transition(.opacity.combined(with: .move(edge: .bottom)))
    }

    private func scheduleTranslate(immediate: Bool = false, force: Bool = false) {
        guard force || autoTranslate else { return }
        debounceTask?.cancel()
        let text = sourceText
        let lang = targetLang
        if !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            isLoading = true
        }
        debounceTask = Task {
            if !immediate { try? await Task.sleep(nanoseconds: 250_000_000) }
            if Task.isCancelled { return }
            if text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                await MainActor.run { translatedText = ""; errorMessage = nil; isLoading = false }
                return
            }
            await MainActor.run { isLoading = true; errorMessage = nil }
            do {
                let out = try await Translator().translate(text, target: lang)
                if Task.isCancelled { return }
                await MainActor.run { translatedText = out; isLoading = false }
            } catch {
                if Task.isCancelled { return }
                await MainActor.run {
                    errorMessage = error.localizedDescription
                    isLoading = false
                }
            }
        }
    }

    private func sendToWhatsApp(_ text: String) {
        guard !text.isEmpty else { return }
        let encoded = text.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? text
        let digits = SharedSettings.defaultContactNumber.filter { $0.isNumber }

        if !digits.isEmpty {
            if let url = URL(string: "whatsapp://send?phone=\(digits)&text=\(encoded)"),
               UIApplication.shared.canOpenURL(url) {
                UIApplication.shared.open(url)
                return
            }
            if let url = URL(string: "https://wa.me/\(digits)?text=\(encoded)") {
                UIApplication.shared.open(url)
                return
            }
        }

        if let url = URL(string: "whatsapp://send?text=\(encoded)"),
           UIApplication.shared.canOpenURL(url) {
            UIApplication.shared.open(url)
            return
        }
        if let url = URL(string: "https://wa.me/?text=\(encoded)") {
            UIApplication.shared.open(url)
        }
    }

    private func flashCopied() {
        withAnimation { showCopied = true }
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.4) {
            withAnimation { showCopied = false }
        }
    }
}

func relativeTime(_ timestamp: Double) -> String {
    let diff = Date().timeIntervalSince1970 - timestamp
    if diff < 60 { return "just now" }
    if diff < 3600 { return "\(Int(diff / 60))m" }
    if diff < 86400 { return "\(Int(diff / 3600))h" }
    if diff < 604800 { return "\(Int(diff / 86400))d" }
    let formatter = DateFormatter()
    formatter.dateFormat = "MMM d"
    return formatter.string(from: Date(timeIntervalSince1970: timestamp))
}

private struct HistoryBubble: View {
    let entry: HistoryEntry
    let onCopy: () -> Void
    let onFavorite: () -> Void
    let onUse: () -> Void

    private var langName: String {
        TargetLang.allCases.first(where: { $0.rawValue == entry.targetLang })?.fullName
            ?? entry.targetLang
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(entry.translation)
                .font(.system(size: 16, weight: .medium))
                .foregroundStyle(.white)
                .lineLimit(3)
            Text(entry.source)
                .font(.system(size: 12))
                .foregroundStyle(.white.opacity(0.6))
                .lineLimit(2)
            HStack(alignment: .center, spacing: 0) {
                Text("\(langName) · \(relativeTime(entry.timestamp))")
                    .font(.system(size: 11, weight: .medium))
                    .foregroundStyle(BrandColor.accent.opacity(0.85))
                Spacer()
                Button(action: onFavorite) {
                    Image(systemName: entry.favorite ? "heart.fill" : "heart")
                        .font(.system(size: 12))
                        .foregroundStyle(entry.favorite ? BrandColor.accent : .white.opacity(0.55))
                        .padding(6)
                }
                .buttonStyle(.plain)
                Button(action: onCopy) {
                    Image(systemName: "doc.on.doc")
                        .font(.system(size: 12))
                        .foregroundStyle(.white.opacity(0.65))
                        .padding(6)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 6)
        .contentShape(Rectangle())
        .onTapGesture { onUse() }
    }
}

struct LanguagePickerSheet: View {
    let selected: TargetLang
    let favorites: [TargetLang]
    let onSelect: (TargetLang) -> Void
    let onToggleFavorite: (TargetLang) -> Void
    let onDismiss: () -> Void

    @State private var localFavorites: [TargetLang] = []

    var others: [TargetLang] {
        TargetLang.allCases.filter { !localFavorites.contains($0) }
    }

    var body: some View {
        NavigationStack {
            List {
                if !localFavorites.isEmpty {
                    Section("Favorites") {
                        ForEach(localFavorites, id: \.self) { lang in
                            LangPickerRow(
                                lang: lang,
                                isSelected: lang == selected,
                                isFavorite: true,
                                onTap: { onSelect(lang) },
                                onToggleFavorite: { toggle(lang) },
                            )
                        }
                    }
                }
                Section("All languages") {
                    ForEach(others, id: \.self) { lang in
                        LangPickerRow(
                            lang: lang,
                            isSelected: lang == selected,
                            isFavorite: false,
                            onTap: { onSelect(lang) },
                            onToggleFavorite: { toggle(lang) },
                        )
                    }
                }
            }
            .listStyle(.insetGrouped)
            .scrollContentBackground(.hidden)
            .navigationTitle("Select a language")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button(action: onDismiss) {
                        Image(systemName: "xmark.circle.fill")
                            .font(.title3)
                            .foregroundStyle(.secondary)
                    }
                }
            }
        }
        .onAppear { localFavorites = favorites }
    }

    private func toggle(_ lang: TargetLang) {
        onToggleFavorite(lang)
        if localFavorites.contains(lang) {
            localFavorites.removeAll { $0 == lang }
        } else {
            localFavorites.append(lang)
        }
    }
}

private struct LangPickerRow: View {
    let lang: TargetLang
    let isSelected: Bool
    let isFavorite: Bool
    let onTap: () -> Void
    let onToggleFavorite: () -> Void

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(lang.fullName)
                    .font(.system(size: 16, weight: isSelected ? .semibold : .regular))
                    .foregroundStyle(isSelected ? BrandColor.accent : .white)
                Text(lang.deeplCode != nil ? "DeepL · \(lang.label)" : "MyMemory · \(lang.label)")
                    .font(.system(size: 11))
                    .foregroundStyle(.white.opacity(0.45))
            }
            Spacer()
            if isSelected {
                Image(systemName: "checkmark")
                    .foregroundStyle(BrandColor.accent)
            }
            Button(action: onToggleFavorite) {
                Image(systemName: isFavorite ? "heart.fill" : "heart")
                    .foregroundStyle(isFavorite ? BrandColor.accent : .secondary)
            }
            .buttonStyle(.plain)
        }
        .contentShape(Rectangle())
        .onTapGesture { onTap() }
    }
}

private struct LangChip: View {
    let label: String
    let highlighted: Bool
    var hasBackground: Bool = false

    var body: some View {
        Text(label)
            .font(.system(size: 13, weight: highlighted ? .bold : .medium))
            .tracking(0.5)
            .foregroundStyle(highlighted ? BrandColor.accent : BrandColor.textMuted)
            .frame(minWidth: 64, minHeight: 42)
            .padding(.horizontal, 10)
            .background(chipBackground, in: RoundedRectangle(cornerRadius: 14))
            .overlay(
                RoundedRectangle(cornerRadius: 14)
                    .stroke(
                        highlighted ? BrandColor.accent : Color.white.opacity(0.15),
                        lineWidth: highlighted ? 1.5 : 0.8,
                    )
            )
    }

    private var chipBackground: some ShapeStyle {
        if highlighted {
            return AnyShapeStyle(Color.white.opacity(hasBackground ? 0.12 : 0.10))
        }
        if hasBackground { return AnyShapeStyle(Color.black.opacity(0.30)) }
        return AnyShapeStyle(Color.white.opacity(0.06))
    }
}

private struct PlusChip: View {
    var hasBackground: Bool = false

    var body: some View {
        Text("+")
            .font(.system(size: 16, weight: .bold))
            .foregroundStyle(BrandColor.accent)
            .frame(minWidth: 44, minHeight: 42)
            .background(
                (hasBackground ? Color.black.opacity(0.30) : Color.white.opacity(0.06)),
                in: RoundedRectangle(cornerRadius: 14),
            )
            .overlay(
                RoundedRectangle(cornerRadius: 14)
                    .stroke(BrandColor.accent.opacity(0.5), lineWidth: 0.8),
            )
    }
}

private struct SettingsSheet: View {
    @Environment(\.dismiss) private var dismiss
    @State private var apiKey: String = SharedSettings.deeplApiKey
    @State private var contactNumber: String = SharedSettings.defaultContactNumber
    @State private var copyColorHex: String = SharedSettings.copyButtonColorHex
    @State private var sendColorHex: String = SharedSettings.sendButtonColorHex
    @State private var inputColorHex: String = SharedSettings.inputBoxColorHex
    @State private var accentColorHex: String = SharedSettings.accentColorHex
    @State private var autoTranslateLocal: Bool = SharedSettings.autoTranslate
    @State private var previewImage: UIImage? = SharedSettings.loadBackgroundImage()
    @State private var pickedItem: PhotosPickerItem? = nil
    @State private var pendingImageData: Data? = nil
    @State private var pendingClearImage: Bool = false
    @State private var testing = false
    @State private var testResult: String? = nil
    @State private var usage: DeepLUsage? = nil
    @State private var usageLoading: Bool = false
    @State private var usageError: String? = nil

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Toggle("Auto-translate while typing", isOn: $autoTranslateLocal)
                } header: {
                    Text("Behavior")
                } footer: {
                    Text("Off: tap the Translate button inside the result box to translate.")
                }

                Section {
                    TextField("+62 812 3456 7890", text: $contactNumber)
                        .keyboardType(.phonePad)
                        .textContentType(.telephoneNumber)
                } header: {
                    Text("Default WhatsApp contact")
                } footer: {
                    Text("Optional. If set, Send via WhatsApp opens this chat directly with the translation pre-filled — no chat picker. Include country code.")
                }

                Section {
                    if let img = previewImage {
                        Image(uiImage: img)
                            .resizable()
                            .scaledToFill()
                            .frame(height: 120)
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                    }
                    PhotosPicker(
                        selection: $pickedItem,
                        matching: .images,
                        photoLibrary: .shared(),
                    ) {
                        Label(previewImage == nil ? "Choose background image" : "Change image", systemImage: "photo")
                    }
                    if previewImage != nil {
                        Button(role: .destructive) {
                            previewImage = nil
                            pendingImageData = nil
                            pendingClearImage = true
                        } label: {
                            Label("Remove image", systemImage: "trash")
                        }
                    }
                } header: {
                    Text("Background image")
                }

                Section {
                    ColorPickerField(label: "Accent", hex: $accentColorHex, allowAlpha: false)
                    ColorPickerField(label: "Send button", hex: $sendColorHex, allowAlpha: false)
                    ColorPickerField(label: "Copy button", hex: $copyColorHex, allowAlpha: false)
                    ColorPickerField(label: "Message box", hex: $inputColorHex, allowAlpha: true)
                } header: { Text("Colors") }

                let favoritesPreview = SharedSettings.loadHistory().filter { $0.favorite }.prefix(3)
                if !favoritesPreview.isEmpty {
                    Section {
                        ForEach(Array(favoritesPreview)) { entry in
                            VStack(alignment: .leading, spacing: 2) {
                                Text("→ \(entry.targetLang.uppercased())")
                                    .font(.system(size: 10, weight: .semibold))
                                    .foregroundStyle(BrandColor.accent)
                                Text(entry.translation)
                                    .font(.system(size: 13))
                                    .lineLimit(2)
                            }
                        }
                    } header: { Text("Favorites") }
                }

                Section {
                    Button("Save") {
                        SharedSettings.deeplApiKey = apiKey
                        SharedSettings.defaultContactNumber = contactNumber
                        SharedSettings.copyButtonColorHex = copyColorHex
                        SharedSettings.sendButtonColorHex = sendColorHex
                        SharedSettings.inputBoxColorHex = inputColorHex
                        SharedSettings.accentColorHex = accentColorHex
                        SharedSettings.autoTranslate = autoTranslateLocal
                        if pendingClearImage {
                            SharedSettings.writeBackgroundImage(nil)
                        } else if let data = pendingImageData {
                            SharedSettings.writeBackgroundImage(data)
                        }
                        dismiss()
                    }.disabled(apiKey.isEmpty)
                }

                Section {
                    SecureField("Paste DeepL key", text: $apiKey)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                    Button("Test key") {
                        Task {
                            await runTest()
                            await fetchUsage()
                        }
                    }
                    .disabled(apiKey.isEmpty || testing)
                    if let result = testResult {
                        Text(result)
                            .font(.footnote)
                            .foregroundStyle(result.hasPrefix("OK") ? .green : .red)
                    }
                    usageRow
                } header: {
                    Text("API settings")
                } footer: {
                    if usage == nil && usageError == nil {
                        Text("Get a free DeepL key at deepl.com/pro-api (500k chars/month).")
                            .font(.footnote)
                    }
                }
            }
            .scrollContentBackground(.hidden)
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { dismiss() } label: {
                        Image(systemName: "xmark.circle.fill")
                            .font(.title3)
                            .foregroundStyle(.secondary)
                    }
                }
            }
            .onChange(of: pickedItem) { _, newItem in
                Task { await loadPicked(newItem) }
            }
            .task { await fetchUsage() }
        }
    }

    @ViewBuilder
    private var usageRow: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("Usage").font(.subheadline).bold()
                Spacer()
                if usageLoading {
                    ProgressView().scaleEffect(0.7)
                } else {
                    Button { Task { await fetchUsage() } } label: {
                        Image(systemName: "arrow.clockwise").font(.caption)
                    }
                    .buttonStyle(.plain)
                    .foregroundStyle(BrandColor.accent)
                }
            }
            if let err = usageError {
                Text("⚠ \(err)").font(.footnote).foregroundStyle(.red)
            } else if let u = usage {
                ProgressView(value: u.percentUsed)
                    .tint(u.percentUsed > 0.85 ? .red : BrandColor.accent)
                HStack {
                    VStack(alignment: .leading, spacing: 0) {
                        Text("Used").font(.caption2).foregroundStyle(.secondary)
                        Text(formatChars(u.charactersUsed)).font(.callout.weight(.semibold))
                    }
                    Spacer()
                    VStack(spacing: 0) {
                        Text("Left").font(.caption2).foregroundStyle(.secondary)
                        Text(formatChars(u.charactersRemaining)).font(.callout.weight(.semibold))
                    }
                    Spacer()
                    VStack(alignment: .trailing, spacing: 0) {
                        Text("Used %").font(.caption2).foregroundStyle(.secondary)
                        Text(String(format: "%.1f%%", u.percentUsed * 100))
                            .font(.callout.weight(.semibold))
                            .foregroundStyle(u.percentUsed > 0.85 ? .red : BrandColor.accent)
                    }
                }
                Text("Plan limit: \(formatChars(u.charactersLimit)) chars")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            } else if !apiKey.isEmpty {
                Text("Tap Test key or refresh to see usage.")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 4)
    }

    private func fetchUsage() async {
        guard !apiKey.isEmpty else {
            usage = nil; usageError = nil; return
        }
        SharedSettings.deeplApiKey = apiKey
        usageLoading = true
        usageError = nil
        do {
            let result = try await DeepLClient(apiKey: { apiKey }).getUsage()
            usage = result
        } catch {
            usage = nil
            usageError = error.localizedDescription
        }
        usageLoading = false
    }

    private func formatChars(_ n: Int64) -> String {
        if n >= 1_000_000 { return String(format: "%.2fM", Double(n) / 1_000_000) }
        if n >= 1_000 {
            let fmt = NumberFormatter()
            fmt.numberStyle = .decimal
            return fmt.string(from: NSNumber(value: n)) ?? "\(n)"
        }
        return "\(n)"
    }

    private func loadPicked(_ item: PhotosPickerItem?) async {
        guard let item else { return }
        guard let data = try? await item.loadTransferable(type: Data.self) else { return }
        await MainActor.run {
            pendingImageData = data
            previewImage = UIImage(data: data)
            pendingClearImage = false
        }
    }

    private func runTest() async {
        SharedSettings.deeplApiKey = apiKey
        testing = true
        defer { testing = false }
        do {
            let out = try await DeepLClient().translate("hello", targetLang: .id)
            testResult = "OK → \(out)"
        } catch {
            testResult = "Error: \(error.localizedDescription)"
        }
    }
}

private struct ColorPickerField: View {
    let label: String
    @Binding var hex: String
    var allowAlpha: Bool = false

    @State private var wheelColor: Color = .white
    @State private var showWheel: Bool = false
    @State private var customs: [String] = SharedSettings.customColorHexes
    @State private var pendingDelete: String? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(label).font(.body).foregroundStyle(.white)
                Spacer()
                Circle()
                    .fill(Color(hex: hex))
                    .frame(width: 26, height: 26)
                    .overlay(Circle().stroke(.white.opacity(0.3), lineWidth: 0.5))
            }
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    // "+" tile — uses iOS native ColorPicker
                    ZStack {
                        Circle()
                            .fill(Color.white.opacity(0.06))
                            .frame(width: 28, height: 28)
                            .overlay(Circle().stroke(BrandColor.accent.opacity(0.7), lineWidth: 1))
                        // Hidden iOS native picker on tap
                        ColorPicker("", selection: $wheelColor, supportsOpacity: allowAlpha)
                            .labelsHidden()
                            .opacity(0.025)
                            .frame(width: 28, height: 28)
                        Text("+")
                            .font(.system(size: 18, weight: .light))
                            .foregroundStyle(BrandColor.accent)
                            .allowsHitTesting(false)
                    }
                    // Saved customs
                    ForEach(customs, id: \.self) { c in
                        let isSel = c.caseInsensitiveCompare(hex) == .orderedSame
                        Circle()
                            .fill(Color(hex: c))
                            .frame(width: isSel ? 32 : 28, height: isSel ? 32 : 28)
                            .overlay(
                                Circle().stroke(
                                    isSel ? BrandColor.accent : Color.white.opacity(0.20),
                                    lineWidth: isSel ? 2.5 : 0.8,
                                )
                            )
                            .onTapGesture { hex = c }
                            .onLongPressGesture { pendingDelete = c }
                    }
                    // Standard presets
                    ForEach(SharedColorPresets.all, id: \.self) { preset in
                        let isSel = preset.caseInsensitiveCompare(hex) == .orderedSame
                        Circle()
                            .fill(Color(hex: preset))
                            .frame(width: isSel ? 32 : 28, height: isSel ? 32 : 28)
                            .overlay(
                                Circle().stroke(
                                    isSel ? BrandColor.accent : Color.white.opacity(0.20),
                                    lineWidth: isSel ? 2.5 : 0.8,
                                )
                            )
                            .onTapGesture { hex = preset }
                    }
                }
                .padding(.vertical, 2)
            }
        }
        .padding(.vertical, 4)
        .onAppear { wheelColor = Color(hex: hex) }
        .onChange(of: wheelColor) { _, newColor in
            let newHex = newColor.toHex(includeAlpha: allowAlpha)
            if newHex != hex && newHex != "000000" {
                hex = newHex
                SharedSettings.addCustomColor(newHex)
                customs = SharedSettings.customColorHexes
            }
        }
        .alert("Delete this saved color?", isPresented: Binding(
            get: { pendingDelete != nil },
            set: { if !$0 { pendingDelete = nil } },
        )) {
            Button("Delete", role: .destructive) {
                if let p = pendingDelete {
                    SharedSettings.removeCustomColor(p)
                    customs = SharedSettings.customColorHexes
                }
                pendingDelete = nil
            }
            Button("Cancel", role: .cancel) { pendingDelete = nil }
        }
    }
}

struct HistorySheet: View {
    let onUseEntry: (HistoryEntry) -> Void
    let onCopyEntry: (HistoryEntry) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var entries: [HistoryEntry] = SharedSettings.loadHistory()
    @State private var favoritesOnly: Bool = false
    @State private var confirmClear: Bool = false

    var visible: [HistoryEntry] {
        favoritesOnly ? entries.filter { $0.favorite } : entries
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                Picker("Filter", selection: $favoritesOnly) {
                    Text("All").tag(false)
                    Text("Favorites").tag(true)
                }
                .pickerStyle(.segmented)
                .padding(.horizontal, 16).padding(.top, 12).padding(.bottom, 8)

                if visible.isEmpty {
                    VStack(spacing: 8) {
                        Image(systemName: favoritesOnly ? "heart.slash" : "clock")
                            .font(.largeTitle)
                            .foregroundStyle(.secondary)
                        Text(favoritesOnly
                             ? "No favorites yet — tap the heart on any entry to favorite it."
                             : "No history yet — tap Copy or Send via WhatsApp on a translation to save it here.")
                            .multilineTextAlignment(.center)
                            .foregroundStyle(.secondary)
                            .font(.footnote)
                            .padding(.horizontal, 32)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    List {
                        ForEach(visible) { entry in
                            HistoryRow(
                                entry: entry,
                                onUse: { onUseEntry(entry) },
                                onCopy: { onCopyEntry(entry) },
                                onFavoriteToggle: {
                                    SharedSettings.toggleFavorite(id: entry.id)
                                    entries = SharedSettings.loadHistory()
                                },
                            )
                            .swipeActions(edge: .trailing) {
                                Button(role: .destructive) {
                                    SharedSettings.deleteHistoryEntry(id: entry.id)
                                    entries = SharedSettings.loadHistory()
                                } label: { Label("Delete", systemImage: "trash") }
                            }
                        }
                    }
                    .listStyle(.plain)
                }
            }
            .scrollContentBackground(.hidden)
            .navigationTitle("History")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Done") { dismiss() }
                }
                ToolbarItem(placement: .primaryAction) {
                    if !entries.isEmpty {
                        Button { confirmClear = true } label: {
                            Image(systemName: "trash")
                        }
                    }
                }
            }
            .alert("Delete all history?", isPresented: $confirmClear) {
                Button("Delete", role: .destructive) {
                    SharedSettings.clearHistory()
                    entries = []
                }
                Button("Cancel", role: .cancel) {}
            } message: {
                Text("This cannot be undone.")
            }
        }
    }
}

private struct HistoryRow: View {
    let entry: HistoryEntry
    let onUse: () -> Void
    let onCopy: () -> Void
    let onFavoriteToggle: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text("→ \(entry.targetLang.uppercased())")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(BrandColor.accent)
                Spacer()
                Button(action: onFavoriteToggle) {
                    Image(systemName: entry.favorite ? "heart.fill" : "heart")
                        .foregroundStyle(entry.favorite ? BrandColor.accent : .secondary)
                }
                .buttonStyle(.plain)
                Button(action: onCopy) {
                    Image(systemName: "doc.on.doc").foregroundStyle(.secondary)
                }
                .buttonStyle(.plain)
            }
            Text(entry.source).font(.footnote).foregroundStyle(.secondary).lineLimit(2)
            Text(entry.translation).font(.body).lineLimit(3)
        }
        .padding(.vertical, 6)
        .contentShape(Rectangle())
        .onTapGesture { onUse() }
    }
}

#Preview { ContentView() }
