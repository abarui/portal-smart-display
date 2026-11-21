package com.portal.calendar

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import android.widget.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File

/** A temporary link to one selected photo. Closing the dialog revokes it. */
@Suppress("DEPRECATION")
class PhotoExportDialog(ctx:Context, private val file:File, private val closed:()->Unit):Dialog(ctx) {
    private val sharing=App.instance.phoneSetup.photos
    private val handler=Handler(Looper.getMainLooper())
    private val qr=ImageView(ctx)
    private val status=label("",15f)
    private var ready=false
    private val tick=object:Runnable {
        override fun run() {
            if(ready) {
                val seconds=sharing.seconds()
                status.text=if(seconds>0) "Link expires in ${seconds/60}:${(seconds%60).toString().padStart(2,'0')}" else "Link expired. Tap New code to try again."
                if(seconds<=0) qr.alpha=.12f
            }
            handler.postDelayed(this,1000)
        }
    }
    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        val card=LinearLayout(ctx).apply {
            orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_HORIZONTAL
            setPadding(dp(28),dp(24),dp(28),dp(20))
            background=GradientDrawable().apply { setColor(Color.WHITE);cornerRadius=dp(24).toFloat() }
        }
        card.addView(label("Save to your phone",25f,true),row())
        card.addView(label("Scan with your phone’s camera.\nKeep both devices on the same Wi-Fi.",16f),row(12))
        qr.contentDescription="Scan this code to download the selected photo"
        qr.setBackgroundColor(Color.WHITE)
        card.addView(qr,LinearLayout.LayoutParams(dp(250),dp(250)).apply { topMargin=dp(16) })
        card.addView(status,row(14))
        card.addView(label("This code shares only this photo.\nThe link stops working when you close this screen.",14f),row(12))
        val buttons=LinearLayout(ctx).apply { gravity=Gravity.CENTER }
        buttons.addView(label("New code",17f,true).apply { setTextColor(Color.parseColor("#286CEB"));minimumHeight=dp(50);minimumWidth=dp(150);setOnClickListener { renew() } })
        buttons.addView(label("Done",17f,true).apply { setTextColor(Color.parseColor("#286CEB"));minimumHeight=dp(50);minimumWidth=dp(150);setOnClickListener { dismiss() } })
        card.addView(buttons,row(16))
        setContentView(ScrollView(ctx).apply { addView(card) })
        setOnDismissListener { handler.removeCallbacks(tick);sharing.close();closed() }
    }
    override fun show() {
        super.show()
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window?.setLayout(minOf(dp(520),context.resources.displayMetrics.widthPixels-dp(36)),WindowManager.LayoutParams.WRAP_CONTENT)
        renew();handler.post(tick)
    }
    private fun renew() {
        sharing.close();ready=false
        val wifi=context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ip=wifi.connectionInfo?.ipAddress?:0
        if(ip==0 || !App.instance.phoneSetupReady || !file.isFile) {
            qr.setImageDrawable(null)
            status.text=when { !file.isFile -> "This photo is no longer available."; ip==0 -> "Connect the Portal to Wi-Fi, then tap New code."; else -> "Reopen PortalHub and try again." }
            return
        }
        val host="${ip and 255}.${ip shr 8 and 255}.${ip shr 16 and 255}.${ip shr 24 and 255}"
        val url="http://$host:${PhoneSetupServer.PORT}/photo#key=${sharing.begin(file)}"
        val size=dp(250)
        val matrix=QRCodeWriter().encode(url,BarcodeFormat.QR_CODE,size,size,mapOf(EncodeHintType.MARGIN to 4))
        val pixels=IntArray(size*size) { if(matrix[it%size,it/size]) Color.BLACK else Color.WHITE }
        qr.setImageBitmap(Bitmap.createBitmap(pixels,size,size,Bitmap.Config.ARGB_8888));qr.alpha=1f;ready=true
        status.text="Link expires in 10:00"
    }
    private fun label(text:String,size:Float,bold:Boolean=false)=TextView(context).apply {
        this.text=text;textSize=size;gravity=Gravity.CENTER;setTextColor(Color.parseColor("#344258"))
        if(bold) typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL)
    }
    private fun row(top:Int=0)=LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(top) }
    private fun dp(value:Int)=(value*context.resources.displayMetrics.density).toInt()
}
