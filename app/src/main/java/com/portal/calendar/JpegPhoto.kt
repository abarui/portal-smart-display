package com.portal.calendar

import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

/** Portal's virtual camera includes unused capacity after the JPEG. Keep the encoded image, including EXIF. */
object JpegPhoto {
    fun read(source:InputStream,maxBytes:Int=25_000_000):ByteArray {
        val input=BufferedInputStream(source)
        val out=ByteArrayOutputStream()
        fun byte():Int {
            require(out.size()<maxBytes) { "This photo is too large to share." }
            val value=input.read()
            require(value>=0) { "This photo is incomplete. Take another photo and try again." }
            out.write(value);return value
        }
        require(byte()==0xFF && byte()==0xD8) { "This is not a JPEG photo." }
        var scan=false
        while(true) {
            var marker:Int
            if(scan) {
                while(true) {
                    if(byte()!=0xFF) continue
                    do { marker=byte() } while(marker==0xFF)
                    if(marker==0 || marker in 0xD0..0xD7) continue
                    break
                }
            } else {
                require(byte()==0xFF) { "This photo has an invalid JPEG marker." }
                do { marker=byte() } while(marker==0xFF)
            }
            if(marker==0xD9) return out.toByteArray()
            require(marker!=0 && marker!=0xD8) { "This photo has an invalid JPEG marker." }
            if(marker==1 || marker in 0xD0..0xD7) continue
            val length=(byte() shl 8)+byte()
            require(length>=2) { "This photo has an invalid JPEG segment." }
            repeat(length-2) { byte() }
            if(marker!=0xDC) scan=marker==0xDA
        }
    }
}
