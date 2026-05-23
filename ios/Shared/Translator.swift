import Foundation

struct Translator {
    var deepL = DeepLClient()
    var myMemory = MyMemoryClient()

    func translate(_ text: String, target: TargetLang) async throws -> String {
        if let deeplCode = target.deeplCode {
            _ = deeplCode
            return try await deepL.translate(text, targetLang: target)
        } else {
            let source = SundaneseHeuristics.guessSourceFor(text)
            return try await myMemory.translate(text, sourceLang: source, targetLang: target.myMemoryCode)
        }
    }
}
