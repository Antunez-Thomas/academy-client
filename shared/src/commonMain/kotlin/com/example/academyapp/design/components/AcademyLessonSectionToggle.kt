package com.example.academyapp.design.components

/** Stable icon direction for a curriculum section's expanded state. */
enum class AcademySectionToggleDirection {
    ExpandedUp,
    CollapsedRight,
}

fun sectionToggleDirection(expanded: Boolean): AcademySectionToggleDirection =
    if (expanded) AcademySectionToggleDirection.ExpandedUp else AcademySectionToggleDirection.CollapsedRight
