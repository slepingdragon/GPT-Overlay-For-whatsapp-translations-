import Foundation

enum MyMemoryError: Error, LocalizedError {
    case httpError(Int, String)
    case malformedResponse

    var errorDescription: String? {
        switch self {
        case .httpError(let code, let body): return "HTTP \(code): \(body)"
        case .malformedResponse: return "Unexpected response from MyMemory."
        }
    }
}

struct MyMemoryClient {
    func translate(_ text: String, sourceLang: String, targetLang: String) async throws -> String {
        guard !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return "" }

        var components = URLComponents(string: "https://api.mymemory.translated.net/get")!
        components.queryItems = [
            URLQueryItem(name: "q", value: text),
            URLQueryItem(name: "langpair", value: "\(sourceLang)|\(targetLang)"),
        ]
        var req = URLRequest(url: components.url!)
        req.timeoutInterval = 8

        let (data, response) = try await URLSession.shared.data(for: req)
        guard let http = response as? HTTPURLResponse else { throw MyMemoryError.malformedResponse }
        guard (200..<300).contains(http.statusCode) else {
            throw MyMemoryError.httpError(http.statusCode, String(data: data, encoding: .utf8) ?? "")
        }

        struct Resp: Decodable {
            let responseData: ResponseData
            struct ResponseData: Decodable { let translatedText: String }
        }
        let decoded = try JSONDecoder().decode(Resp.self, from: data)
        return decoded.responseData.translatedText
    }
}

enum SundaneseHeuristics {
    private static let indonesianHints = [
        " yang ", " saya ", " kamu ", " tidak ", " sudah ", " kalau ",
        " saja ", " juga ", " ini ", " itu ", " bisa ", " akan ", " dan ",
    ]

    static func guessSourceFor(_ text: String) -> String {
        let padded = " \(text.lowercased()) "
        let hits = indonesianHints.reduce(0) { $0 + (padded.contains($1) ? 1 : 0) }
        return hits >= 1 ? "id" : "en"
    }
}
