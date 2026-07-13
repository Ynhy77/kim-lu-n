package com.agmnetwork.md5analyzer.util

object MD5Analyzer {
    
    private val md5Regex = Regex("^[a-fA-F0-9]{32}$")

    fun isValidMD5(md5: String): Boolean {
        return md5Regex.matches(md5.trim())
    }

    data class AnalysisResult(
        val dice1: Int,
        val dice2: Int,
        val dice3: Int,
        val total: Int,
        val result: String
    )

    fun analyze(md5: String): AnalysisResult {
        val normalizedHash = md5.trim().lowercase()
        
        // Extract 3 parts (2 characters each, representing bytes in hex)
        val part1 = normalizedHash.substring(0, 2)
        val part2 = normalizedHash.substring(2, 4)
        val part3 = normalizedHash.substring(4, 6)

        // Convert hex strings to integers, then map to standard 1-6 dice values
        val dice1 = (part1.toInt(16) % 6) + 1
        val dice2 = (part2.toInt(16) % 6) + 1
        val dice3 = (part3.toInt(16) % 6) + 1

        val total = dice1 + dice2 + dice3
        val result = if (total >= 11) "TÀI" else "XỈU"

        return AnalysisResult(dice1, dice2, dice3, total, result)
    }
}
