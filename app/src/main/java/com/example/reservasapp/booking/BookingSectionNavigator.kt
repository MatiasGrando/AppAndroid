package com.example.reservasapp.booking

import com.example.reservasapp.MenuIdentity
import com.example.reservasapp.MenuSection

/**
 * Mantiene la navegacion entre secciones de detalle en un punto chico para que
 * la Activity solo conecte eventos con renderizado.
 */
class BookingSectionNavigator(
    initialSections: List<MenuSection>,
    initialSelections: Map<String, String?>,
    private val isSideEnabledResolver: (List<MenuSection>, Map<String, String?>) -> Boolean = BookingMenuCoordinator::estaGuarnicionHabilitada
) {

    private var sections: List<MenuSection> = initialSections
    private val selections = linkedMapOf<String, String?>().apply {
        putAll(initialSelections)
    }
    private var currentSectionIndex: Int = 0
    private var isSideEnabled: Boolean = isSideEnabledResolver(sections, selections)

    val currentSections: List<MenuSection>
        get() = sections

    val currentSelections: Map<String, String?>
        get() = selections

    fun currentRenderState(): BookingSectionRenderState {
        return BookingSectionRenderState(
            currentSectionIndex = currentSectionIndex,
            isContinueEnabled = hasSelectionForCurrentSection(),
            isSideEnabled = isSideEnabled
        )
    }

    fun onOptionSelected(sectionId: String, selectedOptionId: String, isDoubleTap: Boolean): BookingSectionEvent {
        selections[sectionId] = selectedOptionId

        val shouldRefreshSideSection = if (sectionId == MenuIdentity.SECTION_MAIN) {
            syncSideSectionState()
        } else {
            false
        }

        return BookingSectionEvent(
            renderState = currentRenderState(),
            shouldRefreshSideSection = shouldRefreshSideSection,
            shouldAutoAdvance = isDoubleTap
        )
    }

    fun onPageSelected(position: Int): BookingSectionRenderState {
        currentSectionIndex = position
        return currentRenderState()
    }

    fun onBackPressed(): BookingSectionNavigation {
        if (currentSectionIndex == 0) {
            return BookingSectionNavigation.Exit
        }

        val targetIndex = when {
            currentSectionIndex > sectionIndex(MenuIdentity.SECTION_SIDE) &&
                !isSideEnabled &&
                sectionIndex(MenuIdentity.SECTION_MAIN) != -1 -> sectionIndex(MenuIdentity.SECTION_MAIN)

            else -> currentSectionIndex - 1
        }

        return BookingSectionNavigation.Section(targetIndex)
    }

    fun onContinue(): BookingSectionNavigation {
        val section = sections.getOrNull(currentSectionIndex)
            ?: return BookingSectionNavigation.Stay
        if (selections[section.id].isNullOrBlank()) {
            return BookingSectionNavigation.Stay
        }

        if (section.id == MenuIdentity.SECTION_MAIN) {
            syncSideSectionState()
            val targetIndex = if (isSideEnabled) {
                sectionIndex(MenuIdentity.SECTION_SIDE)
            } else {
                sectionIndex(MenuIdentity.SECTION_DESSERT)
            }
            return BookingSectionNavigation.Section(targetIndex)
        }

        return if (currentSectionIndex < sections.lastIndex) {
            BookingSectionNavigation.Section(currentSectionIndex + 1)
        } else {
            BookingSectionNavigation.Confirmation
        }
    }

    fun onSectionsReloaded(newSections: List<MenuSection>): BookingSectionEvent {
        sections = newSections
        currentSectionIndex = 0
        val shouldRefreshSideSection = syncSideSectionState()

        return BookingSectionEvent(
            renderState = currentRenderState(),
            shouldRefreshSideSection = shouldRefreshSideSection
        )
    }

    private fun syncSideSectionState(): Boolean {
        val previousState = isSideEnabled
        isSideEnabled = isSideEnabledResolver(sections, selections)
        if (!isSideEnabled) {
            selections[MenuIdentity.SECTION_SIDE] = null
        }
        return previousState != isSideEnabled || !isSideEnabled
    }

    private fun hasSelectionForCurrentSection(): Boolean {
        val section = sections.getOrNull(currentSectionIndex) ?: return false
        return !selections[section.id].isNullOrBlank()
    }

    private fun sectionIndex(sectionId: String): Int {
        return sections.indexOfFirst { it.id == sectionId }
    }
}

data class BookingSectionRenderState(
    val currentSectionIndex: Int,
    val isContinueEnabled: Boolean,
    val isSideEnabled: Boolean
)

data class BookingSectionEvent(
    val renderState: BookingSectionRenderState,
    val shouldRefreshSideSection: Boolean = false,
    val shouldAutoAdvance: Boolean = false
)

sealed interface BookingSectionNavigation {
    data class Section(val index: Int) : BookingSectionNavigation
    data object Stay : BookingSectionNavigation
    data object Confirmation : BookingSectionNavigation
    data object Exit : BookingSectionNavigation
}
