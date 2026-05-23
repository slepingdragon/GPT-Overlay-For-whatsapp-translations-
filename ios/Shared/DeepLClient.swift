import Foundation

enum DeepLError: Error, LocalizedError {
    case missingKey
    case httpError(Int, String)
    case malformedResponse

    var errorDescription: String? {
        switch self {
        case .missingKey: return "Missing DeepL API key — set it in the app."
        case .httpError(let code, let body): return "HTTP \(code): \(body)"
        case .malformedResponse: return "Unexpected response from DeepL."
        }
    }
}

struct DeepLUsage {
    let charactersUsed: Int64
    let charactersLimit: Int64

    var percentUsed: Double {
        guard charactersLimit > 0 else { return 0 }
        return min(1.0, max(0.0, Double(charactersUsed) / Double(charactersLimit)))
    }
    var charactersRemaining: Int64 {
        max(0, charactersLimit - charactersUsed)
    }
}

struct DeepLClient {
    var apiKey: () -> String = { SharedSettings.deeplApiKey }

    func getUsage() async throws -> DeepLUsage {
        let key = apiKey().trimmingCharacters(in: .whitespaces)
        guard !key.isEmpty else { throw DeepLError.missingKey }
        let host = key.hasSuffix(":fx") ? "api-free.deepl.com" : "api.deepl.com"
        var req = URLRequest(url: URL(string: "https://\(host)/v2/usage")!)
        req.httpMethod = "GET"
        req.setValue("DeepL-Auth-Key \(key)", forHTTPHeaderField: "Authorization")
        req.timeoutInterval = 8

        let (data, response) = try await URLSession.shared.data(for: req)
        guard let http = response as? HTTPURLResponse else { throw DeepLError.malformedResponse }
        guard (200..<300).contains(http.statusCode) else {
            throw DeepLError.httpError(http.statusCode, String(data: data, encoding: .utf8) ?? "")
        }
        struct Resp: Decodable {
            let character_count: Int64
            let character_limit: Int64
        }
        let decoded = try JSONDecoder().decode(Resp.self, from: data)
        return DeepLUsage(charactersUsed: decoded.character_count, charactersLimit: decoded.character_limit)
    }

    func translate(_ text: String, targetLang: TargetLang) async throws -> String {
        let key = apiKey().trimmingCharacters(in: .whitespaces)
        guard !key.isEmpty else { throw DeepLError.missingKey }
        guard !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return "" }

        let host = key.hasSuffix(":fx") ? "api-free.deepl.com" : "api.deepl.com"
        var req = URLRequest(url: URL(string: "https://\(host)/v2/translate")!)
        req.httpMethod = "POST"
        req.setValue("DeepL-Auth-Key \(key)", forHTTPHeaderField: "Authorization")
        req.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        req.timeoutInterval = 8

        let form = [
            "text": text,
            "target_lang": targetLang.deeplCode ?? targetLang.rawValue.uppercased(),
        ]
        req.httpBody = form
            .map { "\($0.key)=\(percentEncode($0.value))" }
            .joined(separator: "&")
            .data(using: .utf8)

        let (data, response) = try await URLSession.shared.data(for: req)
        guard let http = response as? HTTPURLResponse else { throw DeepLError.malformedResponse }
        guard (200..<300).contains(http.statusCode) else {
            throw DeepLError.httpError(http.statusCode, String(data: data, encoding: .utf8) ?? "")
        }
        struct Resp: Decodable { let translations: [Translation] }
        struct Translation: Decodable { let text: String }
        let decoded = try JSONDecoder().decode(Resp.self, from: data)
        guard let first = decoded.translations.first else { throw DeepLError.malformedResponse }
        return first.text
    }

    private func percentEncode(_ s: String) -> String {
        var allowed = CharacterSet.urlQueryAllowed
        allowed.remove(charactersIn: "&=+")
        return s.addingPercentEncoding(withAllowedCharacters: allowed) ?? s
    }
}
