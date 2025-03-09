import kotlin.math.min

fun main() {
    println(parsePlayer("hello player ice"))
    println(parsePlayer("hello player bun ice how are you"))
    println(parsePlayer("hello player ice how are you"))
    println(parsePlayer("hello player ice kid how are you"))
    println(parsePlayer("hello player ice dude sucks how are you"))
    println(parsePlayer("hello player ice dude how are you"))
}

fun parsePlayer(input: String): String {
    if (!input.contains("player")) {
        return input
    }
    val players = mutableListOf(
        "Hajimei",
        "BUNNAYA",
        "BUNNAYA2",
        "IceKid16",
        "IceDude16",
        "IceDudeSucks16",
        "thanks4theassist",
        "Luc_is_cool",
    )
    val searchDepth = 3
    val after = input.substringAfter("player ")
    val words = after.split(" ")
    println(words)
    val maxSearch = min(searchDepth, words.size)
    var matched = players
        .map { MatchedPlayer(it, it) }
        .toList()
    var matchedIndex = 0

    for (i in 0 until maxSearch) {
        // [ice, kid]
        val wordToMatch = words[i]
        println(wordToMatch)
        val newMatched = matched
            .filter {
                val matchedIndex = it.postMatchName.indexOf(wordToMatch, ignoreCase = true)
                if (matchedIndex != -1) {
                    it.postMatchName = it.postMatchName.substring(matchedIndex + wordToMatch.length)
                }
                matchedIndex != -1
            }
            .toList()
        println(newMatched)
        if (newMatched.isEmpty()) {
            break
        }
        matched = newMatched
        matchedIndex = i
        if (newMatched.size == 1) {
            break
        }
    }
    if (matched.isEmpty()) {
        return input
    }
    return input.replace("player " + words.subList(0, matchedIndex + 1).joinToString(" "), matched.first().name, ignoreCase = true)
}

data class MatchedPlayer(val name: String, var postMatchName: String)