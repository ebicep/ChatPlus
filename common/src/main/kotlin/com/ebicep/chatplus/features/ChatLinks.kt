package com.ebicep.chatplus.features

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.util.ComponentUtil
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.contents.PlainTextContents
import java.net.URI
import java.util.regex.Pattern
import kotlin.jvm.JvmStatic

object ChatLinks {

    private val TRAILING_PUNCTUATION = charArrayOf('.', ',', ';', ':', '!', '?', ')', ']', '}', '"', '\'')

    private val HTTP_URL_PATTERN: Pattern = Pattern.compile(
        """https?://[^\s<>\[\]{}|\\^`"]+""",
        Pattern.CASE_INSENSITIVE
    )

    private val BARE_DOMAIN_PATTERN: Pattern = Pattern.compile(
        """(?<![/@\w])(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\.)+(?:[a-zA-Z]{2,})(?::\d+)?(?:/[^\s<>\[\]{}|\\^`"]*)?""",
        Pattern.CASE_INSENSITIVE
    )

    @JvmStatic
    fun linkify(component: MutableComponent): MutableComponent {
        if (!Config.values.enabled || !Config.values.chatLinksEnabled) {
            return component
        }
        val plain = component.string
        if (!mightContainUrl(plain)) {
            return component
        }
        val out = Component.empty()
        var changed = false
        walk(component, out) { changed = true }
        return if (changed) out else component
    }

    private fun walk(source: Component, out: MutableComponent, onChange: () -> Unit) {
        if (source is MutableComponent && source.contents is ComponentUtil.LiteralContentsIgnored) {
            out.append(source.copy())
            return
        }
        val text = literalText(source)
        if (text != null) {
            val parts = linkifyString(text, source.style)
            parts.forEach { part ->
                if (part.style.clickEvent is ClickEvent.OpenUrl) {
                    onChange()
                }
                out.append(part)
            }
        }
        source.siblings.forEach { walk(it, out, onChange) }
    }

    private fun mightContainUrl(text: String): Boolean {
        if (text.contains("://", ignoreCase = true)) {
            return true
        }
        if (!Config.values.chatLinksBareDomains) {
            return false
        }
        return text.contains('.') && text.any { it.isLetter() }
    }

    private fun preservesExistingClickEvent(style: Style): Boolean {
        val clickEvent = style.clickEvent ?: return false
        if (clickEvent is ClickEvent.OpenUrl) {
            return true
        }
        return !Config.values.chatLinksOverrideOtherClickEvents
    }

    private fun literalText(component: Component): String? {
        val contents = component.contents
        return if (contents is PlainTextContents && contents !is ComponentUtil.LiteralContentsIgnored) {
            contents.text()
        } else {
            null
        }
    }

    private fun linkifyString(text: String, style: Style): List<Component> {
        if (preservesExistingClickEvent(style)) {
            return listOf(Component.literal(text).withStyle(style))
        }
        val matches = findUrlMatches(text)
        if (matches.isEmpty()) {
            return listOf(Component.literal(text).withStyle(style))
        }
        val parts = mutableListOf<Component>()
        var lastEnd = 0
        for (match in matches) {
            if (match.start > lastEnd) {
                parts.add(Component.literal(text.substring(lastEnd, match.start)).withStyle(style))
            }
            parts.add(
                Component.literal(match.display).withStyle(
                    style
                        .withUnderlined(true)
                        .withClickEvent(ClickEvent.OpenUrl(URI(match.openUrl)))
                )
            )
            lastEnd = match.end
        }
        if (lastEnd < text.length) {
            parts.add(Component.literal(text.substring(lastEnd)).withStyle(style))
        }
        return parts
    }

    private data class UrlMatch(val start: Int, val end: Int, val display: String, val openUrl: String)

    private fun findUrlMatches(text: String): List<UrlMatch> {
        val matches = mutableListOf<UrlMatch>()
        val httpMatcher = HTTP_URL_PATTERN.matcher(text)
        while (httpMatcher.find()) {
            val trimmed = trimTrailingPunctuation(httpMatcher.group())
            if (trimmed.isEmpty()) {
                continue
            }
            val end = httpMatcher.start() + trimmed.length
            matches.add(UrlMatch(httpMatcher.start(), end, trimmed, trimmed))
        }
        if (Config.values.chatLinksBareDomains) {
            val bareMatcher = BARE_DOMAIN_PATTERN.matcher(text)
            while (bareMatcher.find()) {
                val trimmed = trimTrailingPunctuation(bareMatcher.group())
                if (trimmed.isEmpty() || !trimmed.contains('.')) {
                    continue
                }
                val start = bareMatcher.start()
                val end = bareMatcher.start() + trimmed.length
                if (overlaps(matches, start, end)) {
                    continue
                }
                matches.add(UrlMatch(start, end, trimmed, "https://$trimmed"))
            }
        }
        return matches.sortedBy { it.start }
    }

    private fun overlaps(existing: List<UrlMatch>, start: Int, end: Int): Boolean {
        return existing.any { start < it.end && end > it.start }
    }

    private fun trimTrailingPunctuation(url: String): String {
        var end = url.length
        while (end > 0 && TRAILING_PUNCTUATION.contains(url[end - 1])) {
            end--
        }
        return url.substring(0, end)
    }
}
