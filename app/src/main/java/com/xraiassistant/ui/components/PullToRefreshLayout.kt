package com.xraiassistant.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.delay

/** Shortest time the refresh spinner stays up, so a fast reload reads as a refresh rather than a flicker. */
private const val MIN_REFRESH_MILLIS = 600L

/**
 * Pull-down-to-refresh around a scrollable list.
 *
 * The content must scroll vertically (a LazyColumn, or a Column with verticalScroll)
 * for the pull gesture to reach this layout, including when the list is empty.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullToRefreshLayout(
    onRefresh: suspend () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val state = rememberPullToRefreshState()
    val currentOnRefresh by rememberUpdatedState(onRefresh)

    if (state.isRefreshing) {
        LaunchedEffect(Unit) {
            val started = System.currentTimeMillis()
            try {
                currentOnRefresh()
                val elapsed = System.currentTimeMillis() - started
                if (elapsed < MIN_REFRESH_MILLIS) delay(MIN_REFRESH_MILLIS - elapsed)
            } finally {
                state.endRefresh()
            }
        }
    }

    Box(modifier = modifier.nestedScroll(state.nestedScrollConnection)) {
        content()

        // Material3 1.2 draws the idle indicator's shadow at the top edge, so only
        // compose it while the user is pulling or a refresh is running.
        if (state.isRefreshing || state.progress > 0f) {
            PullToRefreshContainer(
                state = state,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .semantics {
                        contentDescription = if (state.isRefreshing) "Refreshing" else "Pull to refresh"
                        liveRegion = LiveRegionMode.Polite
                    }
            )
        }
    }
}
