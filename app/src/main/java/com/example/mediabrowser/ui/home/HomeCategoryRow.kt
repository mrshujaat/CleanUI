package com.example.mediabrowser.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.mediabrowser.domain.model.HomeCategory
import com.example.mediabrowser.domain.model.MediaType
import com.example.mediabrowser.domain.model.Post

private val CARD_WIDTH = 116.dp
private val CARD_HEIGHT = 156.dp
private val ROW_TOTAL_HEIGHT = 52.dp + CARD_HEIGHT + 16.dp // title row + cards + bottom breathing room

/**
 * One horizontally-scrollable "Netflix row" on Home: a title with a
 * chevron to open the full category grid, followed by small thumbnail
 * cards. Tapping any individual card also opens the full category grid
 * (per spec — not the single post directly). Uses an explicit total height
 * so it can never overlap whatever renders above or below it, regardless
 * of which container (grid header slot, plain Column, etc.) hosts it.
 */
@Composable
fun HomeCategoryRow(
    title: String,
    posts: List<Post>,
    onSeeAllClick: () -> Unit,
    onCardClick: ((Post) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(ROW_TOTAL_HEIGHT)
    ) {
        RowTitleHeader(title = title, onClick = onSeeAllClick)

        if (posts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(CARD_HEIGHT),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.height(22.dp))
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth().height(CARD_HEIGHT),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                itemsIndexed(posts) { index, post ->
                    HomeRowCard(post = post, onClick = { onCardClick?.invoke(post) ?: onSeeAllClick() })
                }
            }
        }
    }
}

@Composable
private fun RowTitleHeader(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = "See all $title",
            tint = Color.White,
            modifier = Modifier.height(24.dp)
        )
    }
}

@Composable
private fun HomeRowCard(post: Post, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(CARD_WIDTH)
            .height(CARD_HEIGHT)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(post.thumbnailUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(CARD_HEIGHT)
        )

        // Subtle bottom gradient so any future overlay text/icons stay legible
        // against bright thumbnails, without needing a separate scrim box.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f))
                    )
                )
        )

        if (post.fileType != MediaType.IMAGE) {
            Icon(
                imageVector = Icons.Filled.PlayCircle,
                contentDescription = "Video",
                tint = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.align(Alignment.Center).height(32.dp)
            )
        }
    }
}

/**
 * Row of franchise/series cards. Each card shows its own representative
 * thumbnail (or a loading spinner while it's still being fetched) and the
 * series display name overlaid at the bottom. Tapping any card opens that
 * specific series' full grid.
 */
@Composable
fun TopSeriesRow(
    cards: List<TopSeriesCard>,
    onCardClick: (HomeCategory.TopSeries) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(ROW_TOTAL_HEIGHT)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Top Series",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth().height(CARD_HEIGHT),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(cards, key = { it.category.tagName }) { card ->
                TopSeriesCardView(card = card, onClick = { onCardClick(card.category) })
            }
        }
    }
}

@Composable
private fun TopSeriesCardView(
    card: TopSeriesCard,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(CARD_WIDTH)
            .height(CARD_HEIGHT)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        val thumbnail = card.thumbnail
        if (thumbnail != null) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(thumbnail.thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(CARD_HEIGHT)
            )
        } else {
            Box(modifier = Modifier.fillMaxWidth().height(CARD_HEIGHT), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                    )
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            Text(
                text = card.category.displayName,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}