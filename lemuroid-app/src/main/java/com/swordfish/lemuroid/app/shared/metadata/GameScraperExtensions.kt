package com.swordfish.lemuroid.app.shared.metadata

import com.swordfish.lemuroid.lib.library.db.entity.Game

fun Game.copyWithScrapeResult(result: ScrapeResult): Game =
    copy(
        title = if (title == fileName) result.title ?: title else title,
        description = result.description ?: description,
        genre = result.genre ?: genre,
        developer = result.developer ?: developer,
        year = result.year ?: year,
        rating = result.rating ?: rating,
        coverFrontUrl = result.coverUrl ?: coverFrontUrl,
        screenshotUrl = result.screenshotUrl ?: screenshotUrl,
    )
