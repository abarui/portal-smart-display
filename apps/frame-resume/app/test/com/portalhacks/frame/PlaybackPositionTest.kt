package com.portalhacks.frame

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackPositionTest {
    @Test fun resumesExactPhotoInSavedShuffleOrder() {
        val saved = PlaybackPosition(listOf("c", "a", "d", "b"), "d")
        val restored = saved.ordered(listOf("b", "d", "a", "c"), true)
        assertEquals(listOf("c", "a", "d", "b"), restored)
        assertEquals(2, saved.startIndex(restored))
        assertEquals("b", restored[saved.startIndex(restored) + 1])
    }

    @Test fun removedPhotoContinuesAtNextSurvivor() {
        val saved = PlaybackPosition(listOf("a", "b", "c", "d"), "b")
        val restored = saved.ordered(listOf("a", "d", "e"), true)
        assertEquals(listOf("a", "d", "e"), restored)
        assertEquals("d", restored[saved.startIndex(restored)])
    }

    @Test fun removedLastPhotoWrapsToNextSurvivor() {
        val saved = PlaybackPosition(listOf("a", "b", "c"), "c")
        assertEquals(0, saved.startIndex(listOf("a", "b")))
    }

    @Test fun orderedModeTracksPhotoAcrossInsertionsAndReordering() {
        val saved = PlaybackPosition(listOf("a", "b", "c"), "b")
        val restored = saved.ordered(listOf("x", "c", "a", "b"), false)
        assertEquals(listOf("x", "c", "a", "b"), restored)
        assertEquals(3, saved.startIndex(restored))
    }

    @Test fun emptyOrEntirelyReplacedAlbumsHaveValidFallback() {
        val saved = PlaybackPosition(listOf("a", "b"), "b")
        assertEquals(0, saved.startIndex(emptyList()))
        assertEquals(0, saved.startIndex(listOf("x")))
        assertEquals(listOf("x"), saved.ordered(listOf("x", "x"), true))
    }

    @Test fun firstRunUsesAvailableOrder() {
        val saved = PlaybackPosition(emptyList(), null)
        assertEquals(listOf("c", "a", "b"), saved.ordered(listOf("c", "a", "b"), true))
        assertEquals(0, saved.startIndex(listOf("c", "a", "b")))
    }
}
