package com.banshee.translatekeyboard.translate

import com.banshee.translatekeyboard.settings.TargetLang

class Translator(
    private val deepL: DeepLClient,
    private val myMemory: MyMemoryClient = MyMemoryClient(),
) {
    suspend fun translate(text: String, target: TargetLang): Result<String> {
        val deeplCode = target.deeplCode
        return if (deeplCode != null) {
            deepL.translate(text, deeplCode)
        } else {
            val source = SundaneseHeuristics.guessSourceFor(text)
            myMemory.translate(text, source, target.myMemoryCode)
        }
    }
}
