import UIKit
import SwiftUI
import UniformTypeIdentifiers

final class ShareViewController: UIViewController {

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor(red: 0.059, green: 0.067, blue: 0.082, alpha: 1.0)
        loadIncomingText { [weak self] incoming in
            guard let self else { return }
            let host = UIHostingController(rootView: ShareTranslateView(
                incomingText: incoming,
                onDismiss: { [weak self] in self?.close() },
            ))
            self.addChild(host)
            host.view.frame = self.view.bounds
            host.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
            host.view.backgroundColor = .clear
            self.view.addSubview(host.view)
            host.didMove(toParent: self)
        }
    }

    private func loadIncomingText(completion: @escaping (String) -> Void) {
        guard let items = extensionContext?.inputItems as? [NSExtensionItem] else {
            completion(""); return
        }
        for item in items {
            guard let attachments = item.attachments else { continue }
            for provider in attachments {
                if provider.hasItemConformingToTypeIdentifier(UTType.plainText.identifier) {
                    provider.loadItem(forTypeIdentifier: UTType.plainText.identifier, options: nil) { data, _ in
                        let text = (data as? String) ?? ""
                        DispatchQueue.main.async { completion(text) }
                    }
                    return
                }
            }
        }
        completion("")
    }

    private func close() {
        extensionContext?.completeRequest(returningItems: nil, completionHandler: nil)
    }
}

private struct ShareTranslateView: View {
    let incomingText: String
    let onDismiss: () -> Void

    @State private var translatedText: String = ""
    @State private var targetLang: TargetLang = .en
    @State private var isLoading = true
    @State private var errorMessage: String? = nil

    var body: some View {
        ZStack {
            BrandColor.bg.ignoresSafeArea()
            VStack(spacing: 16) {
                HStack {
                    Text("Translate").font(.title3.weight(.semibold)).foregroundStyle(.white)
                    Spacer()
                    Button("Done", action: onDismiss).foregroundStyle(BrandColor.accent)
                }

                HStack(spacing: 6) {
                    ForEach(TargetLang.allCases, id: \.self) { lang in
                        Button {
                            targetLang = lang
                            Task { await runTranslate() }
                        } label: {
                            Text("\(lang.flag) \(lang.label)")
                                .font(.subheadline.weight(.semibold))
                                .padding(.horizontal, 10).padding(.vertical, 6)
                                .foregroundStyle(lang == targetLang ? BrandColor.accent : .white)
                                .background(
                                    lang == targetLang
                                    ? BrandColor.accent.opacity(0.18)
                                    : BrandColor.surface,
                                    in: Capsule()
                                )
                        }
                    }
                    Spacer()
                }
                .font(.subheadline)

                ScrollView {
                    VStack(alignment: .leading, spacing: 12) {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("Original").font(.caption).foregroundStyle(BrandColor.textMuted)
                            Text(incomingText.isEmpty ? "(no text shared)" : incomingText)
                                .foregroundStyle(.white)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(14)
                        .background(BrandColor.surface, in: RoundedRectangle(cornerRadius: 12))

                        VStack(alignment: .leading, spacing: 4) {
                            HStack {
                                Text("Translation").font(.caption).foregroundStyle(BrandColor.textMuted)
                                if isLoading { ProgressView().scaleEffect(0.6) }
                            }
                            if let err = errorMessage {
                                Text("⚠ \(err)").foregroundStyle(BrandColor.errorRed)
                            } else if translatedText.isEmpty {
                                Text(isLoading ? "…" : "")
                                    .foregroundStyle(BrandColor.textMuted)
                            } else {
                                Text(translatedText)
                                    .font(.system(size: 18))
                                    .foregroundStyle(.white)
                            }
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(14)
                        .background(BrandColor.surface, in: RoundedRectangle(cornerRadius: 12))
                    }
                }

                Button {
                    UIPasteboard.general.string = translatedText
                } label: {
                    Label("Copy translation", systemImage: "doc.on.doc")
                        .frame(maxWidth: .infinity, minHeight: 44)
                }
                .buttonStyle(.borderedProminent)
                .tint(BrandColor.accent)
                .foregroundStyle(BrandColor.onAccent)
                .disabled(translatedText.isEmpty)
            }
            .padding(20)
        }
        .task {
            targetLang = guessTarget(from: incomingText)
            await runTranslate()
        }
    }

    private func runTranslate() async {
        let text = incomingText
        if text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            isLoading = false; return
        }
        isLoading = true; errorMessage = nil
        do {
            translatedText = try await Translator().translate(text, target: targetLang)
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    private func guessTarget(from text: String) -> TargetLang {
        let lower = text.lowercased()
        let indonesianHints = [" yang ", " saya ", " kamu ", " tidak ", " sudah ", " kalau ", " saja ", " juga ", " ini ", " itu "]
        let hits = indonesianHints.reduce(0) { $0 + (lower.contains($1) ? 1 : 0) }
        return hits >= 1 ? .en : .id
    }
}
