package com.pomodoro.tree.domain.model

data class Tag(
    val name: String,
    val color: Long
)

object DefaultTags {
    val WORK = Tag("Work", 0xFF5C7AEA)
    val STUDY = Tag("Study", 0xFF9B59B6)
    val PERSONAL = Tag("Personal", 0xFF4A6741)

    val all = listOf(WORK, STUDY, PERSONAL)
}
