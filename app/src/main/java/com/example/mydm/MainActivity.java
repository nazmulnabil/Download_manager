package com.example.mydm;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    String  smallURL = "https://upload.wikimedia.org/wikipedia/commons/d/de/Greeley_opportunity_5000.jpg";
    private static final int REQUEST_CODE =1 ;
    private DownloadManager downloadManager;
    private long downloadID;
    private Uri Download_Uri;
    Button downloadbtn;
    Thread thread;
    int dl_progress;
    ProgressDialog progressBarDialog;


    //broadcast_reviever for action download_complete
         BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            //Fetching the download id received with the broadcast
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            //Checking if the received broadcast is for our enqueued download by matching download id
            if (downloadID == id) {
                Toast.makeText(MainActivity.this, "Download Completed", Toast.LENGTH_SHORT).show();
            }
           }
          };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

         downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
         downloadbtn=findViewById(R.id.download);
         Download_Uri = Uri.parse(smallURL);





        //progressdialog initialization
         progressBarDialog= new ProgressDialog(MainActivity.this);
         progressBarDialog.setTitle("Download App Data, Please Wait");
         progressBarDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
          progressBarDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK",new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog,
                                int whichButton){
                // Toast.makeText(getBaseContext(),
                //       "OK clicked!", Toast.LENGTH_SHORT).show();
             }
        });

         progressBarDialog.setProgress(0);




          //setting onclick on button
            downloadbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {



                start_download(Download_Uri);
                MyClass thread = new MyClass();
                if(!thread.isAlive())
                 thread.start();
                progressBarDialog.show();

            }
        });

         
          //registering reciever...
       registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));





    }

    //starting_download
     private void start_download(Uri download_uri) {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this,"download starting",Toast.LENGTH_SHORT).show();

            DownloadManager.Request request = new DownloadManager.Request(Download_Uri);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            }


                request.setTitle("Download Manager");
                request.setDestinationInExternalPublicDir(Environment.getExternalStorageState(), "Myfile");
                downloadID = downloadManager.enqueue(request);




        } else if ((ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE))) {
            new AlertDialog.Builder(this)
                    .setTitle("Permission needed")
                    .setMessage("This permission is needed because to download the file")
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE);
                        }
                    })
                    .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create().show();



        } else {
            // You can directly ask for the permission.
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE);
        }

    }


    //permission_request_result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE)  {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission GRANTED", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission DENIED", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
       unregisterReceiver(onDownloadComplete);
    }

    //thread for showing progress inside app
      public class MyClass extends Thread {
        public void run() {
            boolean downloading = true;

             DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
             while (downloading) {

                DownloadManager.Query q = new DownloadManager.Query();
                q.setFilterById(downloadID); //filter by id which you have receieved when reqesting download from download manager
                Cursor cursor = manager.query(q);
                cursor.moveToFirst();
                if(cursor != null && cursor.moveToFirst()&&cursor.getCount()>0) {
                    int bytes_downloaded = cursor.getInt(cursor
                            .getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    int bytes_total = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));


                    dl_progress = (int) ((bytes_downloaded * 100) / bytes_total);
               //publishing progress on ui thread

                          runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            progressBarDialog.setProgress((int) dl_progress);
                            if(dl_progress==100)
                            {
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }



                            }

                        }
                    });
                    if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
                        downloading = false;
                    }
                    // Log.d(Constants.MAIN_VIEW_ACTIVITY, statusMessage(cursor));
                    cursor.close();

                }

            }
        }
    }

}