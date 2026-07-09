package com.example.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.example.ui.theme.*
import java.util.regex.Pattern

class PythonSyntaxHighlighter : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val rawText = text.text
        val builder = AnnotatedString.Builder(rawText)

        // 1. Highlight keywords
        val keywordPattern = Pattern.compile("\\b(def|class|import|from|if|elif|else|while|for|in|return|None|True|False|global|lambda|and|or|not)\\b")
        val keywordMatcher = keywordPattern.matcher(rawText)
        while (keywordMatcher.find()) {
            builder.addStyle(
                SpanStyle(color = CodeKeyword),
                keywordMatcher.start(),
                keywordMatcher.end()
            )
        }

        // 2. Highlight game framework functions
        val functionPattern = Pattern.compile("\\b(draw_rect|draw_circle|draw_text|draw_persistent_circle|play_sound|random_range|str|abs|print)\\b")
        val functionMatcher = functionPattern.matcher(rawText)
        while (functionMatcher.find()) {
            builder.addStyle(
                SpanStyle(color = CodeFunction),
                functionMatcher.start(),
                functionMatcher.end()
            )
        }

        // 3. Highlight numbers
        val numberPattern = Pattern.compile("\\b\\d+(\\.\\d+)?\\b")
        val numberMatcher = numberPattern.matcher(rawText)
        while (numberMatcher.find()) {
            builder.addStyle(
                SpanStyle(color = CodeNumber),
                numberMatcher.start(),
                numberMatcher.end()
            )
        }

        // 4. Highlight strings
        val stringPattern = Pattern.compile("\".*?\"|'.*?'")
        val stringMatcher = stringPattern.matcher(rawText)
        while (stringMatcher.find()) {
            builder.addStyle(
                SpanStyle(color = CodeString),
                stringMatcher.start(),
                stringMatcher.end()
            )
        }

        // 5. Highlight comments
        val commentPattern = Pattern.compile("#.*")
        val commentMatcher = commentPattern.matcher(rawText)
        while (commentMatcher.find()) {
            builder.addStyle(
                SpanStyle(color = CodeComment),
                commentMatcher.start(),
                commentMatcher.end()
            )
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}
