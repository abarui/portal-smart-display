package com.portal.calendar

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.hardware.camera2.*
import android.media.ExifInterface
import android.media.ImageReader
import android.os.*
import android.util.Size
import android.view.*
import android.widget.*
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** On-device still camera, with explicit temporary sharing of a selected photo. */
@Suppress("DEPRECATION")
class CameraActivity : HubAppActivity() {
    private val manager by lazy { getSystemService(CameraManager::class.java) }
    private val ioThread=HandlerThread("PortalCameraPhotos").apply { start() }
    private val photoIO=Handler(ioThread.looper)
    private val folder by lazy { File(filesDir,"portal-camera").apply { mkdirs() } }
    private var camera:CameraDevice?=null
    private var session:CameraCaptureSession?=null
    private var reader:ImageReader?=null
    private var previewSurface:Surface?=null
    private var characteristics:CameraCharacteristics?=null
    private var previewSize=Size(1280,720)
    private var generation=0
    private var opening=false
    private var active=false
    private var enabled=true
    private var capturing=false
    private var timer=false
    private var mirror=true
    private var galleryDepth=0
    private var physicalOrientation=0
    private var exportDialog:PhotoExportDialog?=null
    private var galleryPage=0
    private lateinit var texture:TextureView
    private lateinit var previewBox:FrameLayout
    private lateinit var previewMessage:TextView
    private lateinit var status:TextView
    private lateinit var shutter:TextView
    private lateinit var toggle:TextView
    private lateinit var galleryButton:TextView
    private val captureTimeout=Runnable { capturing=false;updateControls();status.text="Photo could not be saved. Try again." }
    private var countdown:Runnable?=null
    private val orientationWatch by lazy { object:OrientationEventListener(this) {
        override fun onOrientationChanged(value:Int) { if(value!=ORIENTATION_UNKNOWN) physicalOrientation=((value+45)/90*90)%360 }
    } }

    override fun onResume() { super.onResume();active=true;orientationWatch.enable();openCamera() }
    override fun onPause() { active=false;exportDialog?.dismiss();orientationWatch.disable();cancelCountdown();closeCamera();super.onPause() }
    override fun onDestroy() { ui.removeCallbacks(captureTimeout);closeCamera();ioThread.quitSafely();super.onDestroy() }
    override fun onWindowFocusChanged(focus:Boolean) {
        super.onWindowFocusChanged(focus)
        if(focus) openCamera() else { cancelCountdown();closeCamera() }
    }
    override fun renderScreen() {
        closeCamera()
        val root=shell("Camera")
        val body=LinearLayout(this).apply { orientation=if(wide) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL }
        previewBox=FrameLayout(this).apply {
            background=GradientDrawable().apply { setColor(Color.parseColor("#0C1424"));cornerRadius=dp(28).toFloat() }
            clipToOutline=true
        }
        texture=TextureView(this).apply {
            surfaceTextureListener=object:TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(s:SurfaceTexture,w:Int,h:Int) { configurePreview();openCamera() }
                override fun onSurfaceTextureSizeChanged(s:SurfaceTexture,w:Int,h:Int) { configureTransform() }
                override fun onSurfaceTextureDestroyed(s:SurfaceTexture):Boolean { closeCamera();return true }
                override fun onSurfaceTextureUpdated(s:SurfaceTexture) {}
            }
        }
        previewBox.addView(texture,FrameLayout.LayoutParams(MATCH,MATCH,Gravity.CENTER))
        previewMessage=label("Opening camera…",23f,true,Color.WHITE).apply { gravity=Gravity.CENTER;setPadding(dp(28),dp(24),dp(28),dp(24)) }
        previewBox.addView(previewMessage,FrameLayout.LayoutParams(MATCH,MATCH))
        previewBox.addOnLayoutChangeListener { _,l,t,r,b,ol,ot,orr,ob -> if(r-l!=orr-ol || b-t!=ob-ot) configurePreview() }
        body.addView(previewBox,if(wide) LinearLayout.LayoutParams(0,MATCH,1f).apply { rightMargin=dp(20) }
            else LinearLayout.LayoutParams(MATCH,0,1f).apply { bottomMargin=dp(18) })
        val controls=panel()
        controls.addView(label("Your camera",22f,true))
        controls.addView(label("Photos stay on this Portal.",14f,false,Palette.MUTED),lp(top=10))
        status=label("",14f,false,Palette.MUTED).apply { minHeight=dp(48);gravity=Gravity.CENTER_VERTICAL }
        controls.addView(status,lp(top=14,bottom=14))
        shutter=button("Take photo","camera",true) { beginCapture() }
        controls.addView(shutter,LinearLayout.LayoutParams(MATCH,dp(64)))
        val options=row()
        options.addView(button(if(timer) "Timer: 3s" else "Timer: off") { timer=!timer;(options.getChildAt(0) as TextView).text=if(timer) "Timer: 3s" else "Timer: off" },LinearLayout.LayoutParams(0,dp(50),1f).apply { rightMargin=dp(8) })
        options.addView(button(if(mirror) "Mirror: on" else "Mirror: off") { mirror=!mirror;texture.scaleX=if(mirror) -1f else 1f;(options.getChildAt(1) as TextView).text=if(mirror) "Mirror: on" else "Mirror: off" },LinearLayout.LayoutParams(0,dp(50),1f))
        controls.addView(options,lp(top=12))
        toggle=button(if(enabled) "Stop camera" else "Start camera") {
            enabled=!enabled;cancelCountdown()
            if(enabled) { ensurePermission();openCamera() } else closeCamera()
            updateControls()
        }
        controls.addView(toggle,lp(top=12))
        galleryButton=button("Photos (${photos().size})") { showGallery() }
        controls.addView(galleryButton,lp(top=12))
        controls.addView(label("If the preview is dark, open the Portal’s camera cover. The camera stops when you leave this app.",12f,false,Palette.MUTED),lp(top=18))
        body.addView(controls,if(wide) LinearLayout.LayoutParams(dp(326),MATCH) else lp())
        root.addView(body,LinearLayout.LayoutParams(MATCH,0,1f));setContentView(root)
        updateControls();ensurePermission()
    }
    private fun ensurePermission() {
        if(enabled && checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED) {
            previewMessage.text="Allow camera access to use the built-in camera."
            requestPermissions(arrayOf(Manifest.permission.CAMERA),10)
        }
    }
    override fun onRequestPermissionsResult(code:Int,p:Array<out String>,g:IntArray) {
        super.onRequestPermissionsResult(code,p,g)
        if(code==10) {
            if(g.firstOrNull()==PackageManager.PERMISSION_GRANTED) openCamera()
            else { enabled=false;updateControls();status.text="Camera access was not allowed. Tap Start camera to try again." }
        }
    }
    private fun openCamera() {
        if(!active || !enabled || !hasWindowFocus() || galleryDepth>0 || !::texture.isInitialized || !texture.isAvailable || opening || camera!=null) return
        if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED) return
        val epoch=++generation;opening=true;previewMessage.visibility=View.VISIBLE;previewMessage.text="Opening camera…"
        try {
            // Camera 0 is Portal's ordinary app-facing camera; camera 1 belongs to its smart-camera service.
            val id=manager.cameraIdList.firstOrNull { it=="0" }?:manager.cameraIdList.firstOrNull()?:throw IllegalStateException("No camera is available")
            val info=manager.getCameraCharacteristics(id);characteristics=info
            val map=info.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)?:throw IllegalStateException("No camera formats are available")
            val previews=map.getOutputSizes(SurfaceTexture::class.java).toList()
            previewSize=previews.firstOrNull { it.width==1280 && it.height==720 }?:previews.filter { it.width<=1920 }.maxByOrNull { it.width*it.height }?:previews.first()
            val jpeg=map.getOutputSizes(ImageFormat.JPEG).filter { it.width*it.height<=8_000_000 }.maxByOrNull { it.width*it.height }
                ?:map.getOutputSizes(ImageFormat.JPEG).first()
            reader=ImageReader.newInstance(jpeg.width,jpeg.height,ImageFormat.JPEG,2).apply {
                setOnImageAvailableListener({ source ->
                    val image=runCatching { source.acquireNextImage() }.getOrNull()?:return@setOnImageAvailableListener
                    try {
                        val buffer=image.planes[0].buffer
                        val bytes=JpegPhoto.read(object:InputStream() {
                            override fun read():Int=if(buffer.hasRemaining()) buffer.get().toInt() and 255 else -1
                            override fun read(bytes:ByteArray,offset:Int,length:Int):Int {
                                if(length==0) return 0
                                if(!buffer.hasRemaining()) return -1
                                val count=min(length,buffer.remaining());buffer.get(bytes,offset,count);return count
                            }
                        })
                        val file=File(folder,"Portal_${SimpleDateFormat("yyyyMMdd_HHmmss_SSS",Locale.US).format(Date())}.jpg")
                        file.writeBytes(bytes)
                        ui.post { if(!isDestroyed) { ui.removeCallbacks(captureTimeout);capturing=false;updateControls();status.text="Photo saved · ${photos().size} in your gallery" } }
                    } catch(e:Exception) {
                        ui.post { if(!isDestroyed) { capturing=false;updateControls();status.text="Couldn’t save the photo. Check available storage." } }
                    } finally { image.close() }
                },photoIO)
            }
            configurePreview()
            manager.openCamera(id,object:CameraDevice.StateCallback() {
                override fun onOpened(device:CameraDevice) {
                    if(epoch!=generation || !active || !enabled || !hasWindowFocus()) { device.close();return }
                    opening=false;camera=device
                    try {
                        val surfaceTexture=texture.surfaceTexture?:throw IllegalStateException("Preview not ready")
                        surfaceTexture.setDefaultBufferSize(previewSize.width,previewSize.height)
                        val surface=Surface(surfaceTexture);previewSurface=surface
                        device.createCaptureSession(listOf(surface,reader!!.surface),object:CameraCaptureSession.StateCallback() {
                            override fun onConfigured(s:CameraCaptureSession) {
                                if(epoch!=generation || camera!=device) { s.close();return }
                                session=s
                                try {
                                    val request=device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply { addTarget(surface);set(CaptureRequest.CONTROL_MODE,CaptureRequest.CONTROL_MODE_AUTO) }
                                    s.setRepeatingRequest(request.build(),null,ui)
                                    texture.alpha=1f;previewMessage.visibility=View.GONE;status.text="Live preview · Ready to take a photo";updateControls();configureTransform()
                                } catch(e:Exception) { cameraError("Couldn’t start the preview. Try Start camera.") }
                            }
                            override fun onConfigureFailed(s:CameraCaptureSession) { if(epoch==generation) cameraError("Camera preview is unavailable. Try Start camera.") }
                        },ui)
                    } catch(e:Exception) { cameraError("Couldn’t start the camera. Try again.") }
                }
                override fun onDisconnected(device:CameraDevice) { device.close();if(epoch==generation) cameraError("Camera disconnected. Check the privacy switch and try again.") }
                override fun onError(device:CameraDevice,error:Int) {
                    device.close();if(epoch==generation) cameraError(if(error==ERROR_CAMERA_DISABLED) "Camera is disabled. Check the Portal privacy switch." else "Camera is busy or unavailable. Close other camera apps and try again.")
                }
            },ui)
        } catch(e:Exception) { cameraError("Couldn’t open the camera. Check the privacy switch and try again.") }
    }
    private fun cameraError(text:String) { closeCamera();enabled=false;updateControls();status.text=text;previewMessage.text=text }
    private fun closeCamera() {
        generation++;opening=false
        runCatching { session?.close() };session=null
        runCatching { camera?.close() };camera=null
        runCatching { reader?.close() };reader=null
        previewSurface?.release();previewSurface=null
        if(::texture.isInitialized) texture.alpha=0f
        if(::shutter.isInitialized) updateControls()
    }
    private fun updateControls() {
        if(!::shutter.isInitialized) return
        shutter.isEnabled=session!=null && !capturing;shutter.alpha=if(shutter.isEnabled) 1f else .5f
        toggle.text=if(enabled) "Stop camera" else "Start camera";toggle.contentDescription=toggle.text
        galleryButton.text="Photos (${photos().size})";galleryButton.contentDescription="Photo gallery"
        if(!enabled) { previewMessage.visibility=View.VISIBLE;previewMessage.text="Camera paused";status.text="Tap Start camera for a live preview." }
    }
    private fun beginCapture() {
        if(capturing || session==null) return
        capturing=true;updateControls()
        if(!timer) { takePhoto();return }
        var remaining=3
        countdown=object:Runnable {
            override fun run() {
                if(!active || session==null) { cancelCountdown();return }
                if(remaining==0) { previewMessage.visibility=View.GONE;countdown=null;takePhoto();return }
                previewMessage.visibility=View.VISIBLE;previewMessage.text=remaining.toString();remaining--;ui.postDelayed(this,1000)
            }
        }.also { ui.post(it) }
    }
    private fun cancelCountdown() { countdown?.let { ui.removeCallbacks(it) };countdown=null;capturing=false;if(::previewMessage.isInitialized && session!=null) previewMessage.visibility=View.GONE }
    private fun takePhoto() {
        val device=camera?:return;val target=reader?.surface?:return
        try {
            val front=characteristics?.get(CameraCharacteristics.LENS_FACING)==CameraCharacteristics.LENS_FACING_FRONT
            val sensor=characteristics?.get(CameraCharacteristics.SENSOR_ORIENTATION)?:0
            val rotation=(sensor+(if(front) -physicalOrientation else physicalOrientation)+360)%360
            val request=device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                addTarget(target);set(CaptureRequest.CONTROL_MODE,CaptureRequest.CONTROL_MODE_AUTO)
                set(CaptureRequest.JPEG_ORIENTATION,rotation);set(CaptureRequest.JPEG_QUALITY,95.toByte())
            }
            status.text="Saving photo…";ui.postDelayed(captureTimeout,10_000)
            session?.capture(request.build(),object:CameraCaptureSession.CaptureCallback() {
                override fun onCaptureFailed(s:CameraCaptureSession,r:CaptureRequest,f:CaptureFailure) { ui.removeCallbacks(captureTimeout);capturing=false;updateControls();status.text="Photo failed. Try again." }
            },ui)
        } catch(e:Exception) { capturing=false;updateControls();status.text="Photo failed. Try again." }
    }
    private fun configurePreview() {
        if(!::previewBox.isInitialized || previewBox.width==0 || previewBox.height==0) return
        val rotation=windowManager.defaultDisplay.rotation
        val degrees=rotation*90
        val sensor=characteristics?.get(CameraCharacteristics.SENSOR_ORIENTATION)?:270
        val relative=(sensor-degrees+360)%360
        val rotated=relative==90 || relative==270
        val aspect=if(rotated) previewSize.height.toFloat()/previewSize.width else previewSize.width.toFloat()/previewSize.height
        val w=previewBox.width;val h=previewBox.height
        val fittedW=min(w.toFloat(),h*aspect).toInt();val fittedH=(fittedW/aspect).toInt()
        texture.layoutParams=FrameLayout.LayoutParams(fittedW,fittedH,Gravity.CENTER)
        texture.scaleX=if(mirror) -1f else 1f
        texture.post { configureTransform() }
    }
    private fun configureTransform() {
        if(!::texture.isInitialized || texture.width==0 || texture.height==0) return
        val rotation=windowManager.defaultDisplay.rotation
        val w=texture.width.toFloat();val h=texture.height.toFloat();val matrix=Matrix()
        if(rotation==Surface.ROTATION_90 || rotation==Surface.ROTATION_270) {
            val view=RectF(0f,0f,w,h);val buffer=RectF(0f,0f,previewSize.height.toFloat(),previewSize.width.toFloat())
            buffer.offset(view.centerX()-buffer.centerX(),view.centerY()-buffer.centerY())
            matrix.setRectToRect(view,buffer,Matrix.ScaleToFit.FILL)
            val scale=max(h/previewSize.height,w/previewSize.width)
            matrix.postScale(scale,scale,view.centerX(),view.centerY());matrix.postRotate(90f*(rotation-2),view.centerX(),view.centerY())
        } else if(rotation==Surface.ROTATION_180) matrix.postRotate(180f,w/2,h/2)
        texture.setTransform(matrix)
    }
    private fun photos()=folder.listFiles()?.filter { it.isFile && it.extension=="jpg" }?.sortedByDescending { it.name }?:emptyList()
    private fun bitmap(file:File,maxSide:Int):Bitmap? {
        val options=BitmapFactory.Options().apply { inJustDecodeBounds=true };BitmapFactory.decodeFile(file.path,options)
        var sample=1;while(max(options.outWidth,options.outHeight)/sample>maxSide*2) sample*=2
        val original=BitmapFactory.decodeFile(file.path,BitmapFactory.Options().apply { inSampleSize=sample })?:return null
        val orientation=runCatching { ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL) }.getOrDefault(1)
        val matrix=Matrix();when(orientation) { 3->matrix.postRotate(180f);6->matrix.postRotate(90f);8->matrix.postRotate(270f);2->matrix.postScale(-1f,1f);4->matrix.postScale(1f,-1f);5->{matrix.postRotate(90f);matrix.postScale(-1f,1f)};7->{matrix.postRotate(270f);matrix.postScale(-1f,1f)} }
        if(matrix.isIdentity) return original
        return Bitmap.createBitmap(original,0,0,original.width,original.height,matrix,true).also { if(it!==original) original.recycle() }
    }
    private fun showGallery() {
        val files=photos();galleryPage=galleryPage.coerceIn(0,max(0,(files.size-1)/20))
        val content=column().apply { setPadding(dp(20),dp(10),dp(20),dp(10)) }
        val list=column()
        lateinit var dialog:AlertDialog
        if(files.isEmpty()) list.addView(label("No photos yet. Take your first photo with the camera.",18f))
        for(file in files.drop(galleryPage*20).take(20)) {
            val item=row().apply { minimumHeight=dp(96);setPadding(0,dp(6),0,dp(6));contentDescription="View photo ${file.name}";setOnClickListener { showPhoto(file) { dialog.dismiss();showGallery() } } }
            item.addView(ImageView(this).apply { scaleType=ImageView.ScaleType.CENTER_CROP;setImageBitmap(bitmap(file,180)) },LinearLayout.LayoutParams(dp(112),dp(80)))
            item.addView(label(SimpleDateFormat("MMM d, yyyy · h:mm a",Locale.getDefault()).format(Date(file.lastModified())),16f).apply { setPadding(dp(18),0,0,0) },LinearLayout.LayoutParams(0,WRAP,1f))
            list.addView(item)
        }
        content.addView(ScrollView(this).apply { addView(list) },LinearLayout.LayoutParams(MATCH,dp(if(wide) 360 else 600)))
        dialog=AlertDialog.Builder(this).setTitle("Your photos (${files.size})").setView(content).setNegativeButton("Close",null).create()
        val pages=row();pages.addView(button("Previous") { dialog.dismiss();galleryPage--;showGallery() }.apply { isEnabled=galleryPage>0 },LinearLayout.LayoutParams(0,WRAP,1f))
        pages.addView(button("Next") { dialog.dismiss();galleryPage++;showGallery() }.apply { isEnabled=(galleryPage+1)*20<files.size },LinearLayout.LayoutParams(0,WRAP,1f))
        if(files.size>20) content.addView(pages,lp(top=12))
        galleryDepth++;closeCamera();dialog.setOnDismissListener { galleryDepth--;updateControls();openCamera() };dialog.show();styleDialog(dialog);dialog.window?.setLayout(dp(if(wide) 650 else 700),WRAP)
    }
    private fun showPhoto(file:File,onDeleted:()->Unit) {
        val content=column().apply { setPadding(dp(14),dp(10),dp(14),dp(8)) }
        content.addView(ImageView(this).apply { scaleType=ImageView.ScaleType.FIT_CENTER;setImageBitmap(bitmap(file,1400)) },LinearLayout.LayoutParams(MATCH,dp(if(wide) 400 else 700)))
        val dialog=AlertDialog.Builder(this).setTitle("Saved photo").setView(content).setNegativeButton("Close",null).create()
        val actions=row()
        actions.addView(button("Export photo") {
            galleryDepth++;closeCamera()
            exportDialog=PhotoExportDialog(this,file) { exportDialog=null;galleryDepth--;openCamera() }.also { it.show() }
        },LinearLayout.LayoutParams(0,WRAP,1f))
        actions.addView(button("Delete photo") {
            AlertDialog.Builder(this).setTitle("Delete this photo?").setMessage("This removes it from the Portal's camera gallery.")
                .setNegativeButton("Cancel",null).setPositiveButton("Delete") { _,_->
                    if(file.delete()) { dialog.dismiss();Toast.makeText(this,"Photo deleted",Toast.LENGTH_SHORT).show();updateControls();onDeleted() }
                    else Toast.makeText(this,"Couldn’t delete photo",Toast.LENGTH_SHORT).show()
                }.create().also { it.show();styleDialog(it) }
        },LinearLayout.LayoutParams(0,WRAP,1f).apply { leftMargin=dp(10) })
        content.addView(actions,lp(top=12))
        galleryDepth++;dialog.setOnDismissListener { galleryDepth--;updateControls();openCamera() };dialog.show();styleDialog(dialog);dialog.window?.setLayout(dp(if(wide) 850 else 720),WRAP)
    }
}
