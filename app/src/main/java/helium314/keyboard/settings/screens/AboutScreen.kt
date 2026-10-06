
// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import helium314.keyboard.latin.BuildConfig
import helium314.keyboard.latin.R
import helium314.keyboard.latin.common.Links
import helium314.keyboard.latin.utils.Theme
import helium314.keyboard.latin.utils.previewDark

@Composable
fun AboutScreen(
    onClickBack: () -> Unit,
) {
    val context = LocalContext.current

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = stringResource(R.string.english_ime_name),
                style = MaterialTheme.typography.headlineLarge,
            )

            Text(
                text = stringResource(R.string.about_creator_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = "mangoloads",
                style = MaterialTheme.typography.titleLarge,
            )

            HorizontalDivider()

            AboutLinkButton(
                label = stringResource(R.string.about_website),
                onClick = { openUrl(context, Links.CREATOR_WEBSITE) },
            )

            AboutLinkButton(
                label = stringResource(R.string.about_contact),
                onClick = { openUrl(context, Links.CREATOR_CONTACT) },
            )

            AboutLinkButton(
                label = stringResource(R.string.privacy_policy),
                onClick = { openUrl(context, Links.PRIVACY_POLICY) },
            )

            AboutLinkButton(
                label = stringResource(R.string.about_github_link),
                onClick = { openUrl(context, Links.GITHUB) },
            )

            HorizontalDivider()

            Text(
                text = stringResource(R.string.about_build_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = BuildConfig.VERSION_NAME,
                style = MaterialTheme.typography.bodyLarge,
            )

            Text(
                text = stringResource(R.string.about_open_source_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = stringResource(R.string.about_open_source_credits),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(4.dp))

            OutlinedButton(
                onClick = onClickBack,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 13.dp),
            ) {
                Text(stringResource(R.string.dialog_close))
            }
        }
    }
}

@Composable
private fun AboutLinkButton(
    label: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 13.dp),
    ) {
        Text(label)
    }
}

private fun openUrl(context: Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
}

@Preview
@Composable
private fun Preview() {
    Theme(previewDark) {
        Surface {
            AboutScreen(onClickBack = {})
        }
    }
}
