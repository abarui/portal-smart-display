package com.portal.calendar

import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class PhotoSharingTest {
    private fun jpeg(value:Int)=byteArrayOf(-1,-40,-1,-38,0,2,value.toByte(),-1,-39)
    @Test fun onlySelectedPhotoAndCurrentCodeAreAccessible() {
        val root=Files.createTempDirectory("portal-photo-test").toFile()
        try {
            val a=root.resolve("a.jpg").apply { writeBytes(jpeg(1)) }
            val b=root.resolve("b.jpg").apply { writeBytes(jpeg(2)) }
            val share=PhotoSharing(root);val first=share.begin(a)
            assertArrayEquals(a.readBytes(),share.read(first))
            val second=share.begin(b)
            assertThrows(PhonePairing.Expired::class.java) { share.read(first) }
            assertArrayEquals(b.readBytes(),share.read(second))
            assertThrows(PhonePairing.Expired::class.java) { share.read("wrong") }
            share.close();assertThrows(PhonePairing.Expired::class.java) { share.read(second) }
        } finally { root.deleteRecursively() }
    }
    @Test fun expiresAndCannotPublishFilesOutsideCameraFolder() {
        val root=Files.createTempDirectory("portal-photo-test").toFile()
        var now=0L
        try {
            val folder=root.resolve("camera").apply { mkdir() };val photo=folder.resolve("a.jpg").apply { writeText("photo") }
            val secret=root.resolve("secret.jpg").apply { writeText("private") }
            val share=PhotoSharing(folder) { now }
            assertThrows(IllegalArgumentException::class.java) { share.begin(secret) }
            val token=share.begin(photo);now=600_000L
            assertThrows(PhonePairing.Expired::class.java) { share.read(token) }
        } finally { root.deleteRecursively() }
    }
}
