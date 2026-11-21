package com.portal.calendar

import org.junit.Assert.*
import org.junit.Test

class JpegPhotoTest {
    private fun bytes(vararg values:Int)=values.map { it.toByte() }.toByteArray()
    @Test fun stripsCameraPaddingAndPreservesEmbeddedExifMarkers() {
        val jpeg=bytes(255,216,255,225,0,6,1,255,217,2,255,218,0,2,42,255,0,43,255,208,44,255,217)
        val padded=jpeg+ByteArray(1024)+bytes(255,217,0,0)
        assertArrayEquals(jpeg,JpegPhoto.read(padded.inputStream()))
    }
    @Test fun retainsMultipleScans() {
        val jpeg=bytes(255,216,255,218,0,2,9,255,196,0,3,8,255,218,0,2,7,255,217)
        assertArrayEquals(jpeg,JpegPhoto.read(jpeg.inputStream()))
    }
    @Test fun rejectsIncompleteOrOversizedPhotos() {
        assertThrows(IllegalArgumentException::class.java) { JpegPhoto.read(bytes(255,216,255,218,0,2,9).inputStream()) }
        assertThrows(IllegalArgumentException::class.java) { JpegPhoto.read(bytes(255,216,255,218,0,2,9,255,217).inputStream(),5) }
        assertThrows(IllegalArgumentException::class.java) { JpegPhoto.read(bytes(255,216,255,225,0,1).inputStream()) }
    }
}
