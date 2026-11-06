package com.ntdpz.pie.core.domain.settings

import com.ntdpz.pie.core.domain.pet.PetReaction
import java.time.LocalDate

enum class ReminderTone {
    SmallJoys, TiredDays, Feelings, SelfKindness, EverydayWork, Connections, SmallSteps, WindingDown,
}

internal data class ReminderPrompt(val text: String, val tone: ReminderTone)

data class ReminderPresentation(val message: String, val reaction: PetReaction)

/** Stable per day; metadata describes the prompt, never the user's diary or mood. */
object ReminderPresentations {
    fun forDay(date: LocalDate, settings: ReminderSettings): ReminderPresentation {
        val message = ReminderMessages.forDay(date, settings)
        if (settings.customMode && settings.message.isNotBlank()) {
            return ReminderPresentation(message, PetReaction.Nod)
        }
        val prompt = promptForDay(date)
        val choices = compatibleReactions(prompt.tone)
        val index = Math.floorMod(Math.floorDiv(date.toEpochDay(), 8), choices.size.toLong()).toInt()
        val selected = choices[index]
        val previous = rawReaction(date.minusDays(1))
        val reaction = if (selected == previous && choices.size > 1) choices[(index + 1) % choices.size] else selected
        return ReminderPresentation(message, reaction)
    }

    internal fun compatibleReactions(tone: ReminderTone): List<PetReaction> = when (tone) {
        ReminderTone.SmallJoys -> listOf(PetReaction.Wink, PetReaction.EarsUp, PetReaction.Cheer)
        ReminderTone.TiredDays -> listOf(PetReaction.Heart, PetReaction.Nod)
        ReminderTone.Feelings -> listOf(PetReaction.HeadTilt, PetReaction.Nod)
        ReminderTone.SelfKindness -> listOf(PetReaction.Shy, PetReaction.Heart)
        ReminderTone.EverydayWork -> listOf(PetReaction.Nod, PetReaction.EarsUp)
        ReminderTone.Connections -> listOf(PetReaction.Heart, PetReaction.Wink)
        ReminderTone.SmallSteps -> listOf(PetReaction.Cheer, PetReaction.Peekaboo)
        ReminderTone.WindingDown -> listOf(PetReaction.Stretch, PetReaction.Nod)
    }

    private fun promptForDay(date: LocalDate): ReminderPrompt {
        val entries = ReminderPromptCatalog.entries
        return entries[Math.floorMod(date.toEpochDay(), entries.size.toLong()).toInt()]
    }

    private fun rawReaction(date: LocalDate): PetReaction {
        val choices = compatibleReactions(promptForDay(date).tone)
        return choices[Math.floorMod(Math.floorDiv(date.toEpochDay(), 8), choices.size.toLong()).toInt()]
    }
}
