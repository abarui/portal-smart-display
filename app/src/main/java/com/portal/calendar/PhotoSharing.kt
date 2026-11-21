package com.portal.calendar

import java.io.File

/** One selected photo, a separate ten-minute capability, revoked on dialog close. */
class PhotoSharing(private val folder:File, now:()->Long = { System.nanoTime()/1_000_000L }) {
    private val access=PhonePairing(now)
    private var photo:File?=null
    @Synchronized fun begin(file:File):String {
        require(file.canonicalFile.parentFile==folder.canonicalFile && file.extension=="jpg" && file.isFile) { "Choose a saved camera photo." }
        photo=file;return access.renew()
    }
    @Synchronized fun close() { access.close();photo=null }
    @Synchronized fun seconds()=access.remainingSeconds()
    @Synchronized fun read(token:String?):ByteArray = access.authorized(token) {
        val file=photo?:throw PhonePairing.Expired()
        require(file.isFile) { "This photo is no longer available." }
        val size=file.length()
        require(size>0) { "This photo is empty. Take another photo and try again." }
        require(size<=64_000_000L) { "This photo is too large to share." }
        file.inputStream().use { JpegPhoto.read(it) }
    }
}
