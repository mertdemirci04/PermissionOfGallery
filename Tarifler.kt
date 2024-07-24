package com.example.recyclerviewlist.view

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.recyclerviewlist.databinding.ActivityTariflerBinding
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.recyclerviewlist.R
import com.example.recyclerviewlist.database.TarifDAO
import com.example.recyclerviewlist.database.TarifDatabase
import com.example.recyclerviewlist.model.Tarif
import com.google.android.material.snackbar.Snackbar
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.ByteArrayOutputStream

class Tarifler : AppCompatActivity() {
    private lateinit var binding: ActivityTariflerBinding
    private lateinit var permissionLauncher : ActivityResultLauncher<String> //izin istemek için
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent> //galeriye gitmek için
    private var secilenGorsel : Uri? = null //data/data/media/image.jpg gibi
    private var secilenBitmap : Bitmap? = null //jpg gibi şeylere döndürmek için
    private lateinit var db : TarifDatabase
    private lateinit var tarifDAO : TarifDAO
    private val mDisposable = CompositeDisposable()
    private var secilenYemek : Tarif? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTariflerBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        registerLauncher()
    }

    fun gorselSec(view : View){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED){
                //izin verilmemiş,izin istememiz gerekiyor
                if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_MEDIA_IMAGES)){
                    //Snack bar oluşuturup, kullanıcıdan neden izin istediğimizi belirtmemiz gerek.
                    Snackbar.make(view,"Fotoğraf yüklemek için izin gerekiyor",Snackbar.LENGTH_INDEFINITE).setAction(
                        "İzin ver",View.OnClickListener {
                            //izin istenecek
                            permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                        }
                    ).show()
                }
                else{
                    //izin istenecek.
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                }
            }
            else{
                //İzin verilmiş,galeriden fotoğraf alabiliriz.
                val intentToGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }
        }
        else{
            if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                //izin verilmemiş,izin istememiz gerekiyor
                if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)){
                    //Snack bar oluşuturup, kullanıcıdan neden izin istediğimizi belirtmemiz gerek.
                    Snackbar.make(view,"Fotoğraf yüklemek için izin gerekiyor",Snackbar.LENGTH_INDEFINITE).setAction(
                        "İzin ver",View.OnClickListener {
                            //izin istenecek
                            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                    ).show()
                }
                else{
                    //izin istenecek.
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
            else{
                //İzin verilmiş,galeriden fotoğraf alabiliriz.
                val intentToGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }
        }
    }
    private fun kucukBitmapOlustur(kullanıcıBitmap : Bitmap,maksimumBoyut : Int) : Bitmap{
        var width = kullanıcıBitmap.width
        var height = kullanıcıBitmap.height

        val bitmapOranı : Double = width.toDouble() / height.toDouble()
        if(bitmapOranı > 1){
            //gorsel yatay
            width = maksimumBoyut
            val kisaltilmisYukseklik = width / bitmapOranı
            height = kisaltilmisYukseklik.toInt()
        }
        else{
            //gorsel dikey
            height = maksimumBoyut
            val kisaltilmisGenislik = height * bitmapOranı
            width = kisaltilmisGenislik.toInt()
        }

        return Bitmap.createScaledBitmap(kullanıcıBitmap,width,height,true)
    }
    private fun registerLauncher(){
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                val intentFromResult = result.data
                if (intentFromResult != null) {
                    secilenGorsel = intentFromResult.data
                    try {
                        if (Build.VERSION.SDK_INT >= 28) {
                            val source =
                                ImageDecoder.createSource(this.contentResolver, secilenGorsel!!)
                            secilenBitmap = ImageDecoder.decodeBitmap(source)
                            binding.imageView.setImageBitmap(secilenBitmap)
                        } else {
                            secilenBitmap = MediaStore.Images.Media.getBitmap(
                                this.contentResolver,
                                secilenGorsel
                            )
                            binding.imageView.setImageBitmap(secilenBitmap)
                        }
                    } catch (e: Exception) {
                        println(e.localizedMessage)
                    }

                }
            }
        }
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){
                result ->
            if(result){
                //izin verildi galeriye git
                val intentToGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }
            else{
                //izin verilmedi
                Toast.makeText(this,"İzin verilmedi",Toast.LENGTH_LONG).show()
            }
        }
    }
}