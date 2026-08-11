package com.arrowpuzzle.game.feature.consent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.arrowpuzzle.game.core.design.AppTheme
import com.arrowpuzzle.game.core.design.Blue500
import com.arrowpuzzle.game.core.motion.Motion
import com.arrowpuzzle.game.core.motion.enterFromBelow
import com.arrowpuzzle.game.core.ui.PrimaryPillButton

private const val TagTerms = "terms"
private const val TagPrivacy = "privacy"
private const val TagOptions = "options"

/**
 * First-run consent. Plain language, one obvious action, and the three links the
 * reference exposes. The copy avoids legalese where the law does not require it.
 */
@Composable
fun ConsentScreen(
    onAccept: () -> Unit,
    onOpenOptions: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenTerms: () -> Unit = onOpenOptions,
    onOpenPrivacy: () -> Unit = onOpenOptions
) {
    val palette = AppTheme.palette

    val body = remember {
        buildAnnotatedString {
            append("Please read and accept our ")
            pushStringAnnotation(TagTerms, TagTerms)
            withStyle(SpanStyle(color = Blue500)) { append("Terms") }
            pop()
            append(" and ")
            pushStringAnnotation(TagPrivacy, TagPrivacy)
            withStyle(SpanStyle(color = Blue500)) { append("Privacy Policy") }
            pop()
            append(
                ", which set out how the app works and how we collect, use and " +
                    "process your information, the privacy rights available to you, " +
                    "and how to exercise them — otherwise "
            )
            pushStringAnnotation(TagOptions, TagOptions)
            withStyle(SpanStyle(color = Blue500)) { append("see the options") }
            pop()
            append(" available to you.")
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))

            Text(
                text = "Welcome!",
                style = MaterialTheme.typography.headlineMedium,
                color = palette.ink,
                modifier = Modifier.enterFromBelow(delayMillis = Motion.stagger(0))
            )

            Spacer(Modifier.height(16.dp))

            ClickableText(
                text = body,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = palette.inkSoft,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.enterFromBelow(delayMillis = Motion.stagger(1)),
                onClick = { offset ->
                    val tag = listOf(TagTerms, TagPrivacy, TagOptions).firstOrNull {
                        body.getStringAnnotations(it, offset, offset).isNotEmpty()
                    }
                    when (tag) {
                        TagTerms -> onOpenTerms()
                        TagPrivacy -> onOpenPrivacy()
                        TagOptions -> onOpenOptions()
                    }
                }
            )

            Spacer(Modifier.weight(1f))

            PrimaryPillButton(
                text = "Accept",
                onClick = onAccept,
                modifier = Modifier
                    .enterFromBelow(delayMillis = Motion.stagger(2))
                    .fillMaxWidth()
            )

            Spacer(Modifier.height(28.dp))
        }
    }
}
