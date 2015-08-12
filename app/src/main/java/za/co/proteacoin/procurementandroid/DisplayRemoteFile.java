package za.co.proteacoin.procurementandroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.ProgressBar;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


/**
 * Created by dutoitd1 on 2015/03/17.
 */
public class DisplayRemoteFile extends Activity {
    FileOutputStream fileOutput;
    File storageDir;
    File newFile;
    ProgressBar pb;
    int downloadedSize = 0;
    int totalSize = 0;
    GlobalState gs;
    private ProgressDialog pDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gs = (GlobalState) getApplication();
        if (gs.getCurrentFile() == "") {
            DeleteDownloadedFiles();
            this.finish();
        }
        storageDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "/Procurement/");

        final downloadFile downloader = new downloadFile();
        downloader.execute();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (downloader.getStatus() == AsyncTask.Status.RUNNING) {
                    downloader.cancel(true);
                    if (pDialog.isShowing()) {
                        pDialog.dismiss();
                    }
                    new AlertDialog.Builder(DisplayRemoteFile.this)
                            .setTitle("Result")
                            .setMessage("Internet connection timed out.")
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    DisplayRemoteFile.this.finish();
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                }
            }
        }, 30000);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
//        DeleteDownloadedFiles();
        this.finish();
    }

    private void DeleteDownloadedFiles() {
//        storageDir = new File(
//                Environment.getExternalStorageDirectory().getAbsolutePath(), "/Procurement/");
//        Log.d("before>>fileName", storageDir.toString());

        if (!storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                Log.d("App", "failed to create directory");
            }
        } else {
            // delete all the current leftover files
            String[] children = storageDir.list();
            for (int i = 0; i < children.length; i++) {
                new File(storageDir, children[i]).delete();
            }
        }
    }


    private class downloadFile extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            boolean mExternalStorageAvailable = false;
            boolean mExternalStorageNotWriteable = false;

            mExternalStorageAvailable = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
            mExternalStorageNotWriteable = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY);
            if (!mExternalStorageAvailable || mExternalStorageNotWriteable) {
                new AlertDialog.Builder(DisplayRemoteFile.this)
                        .setTitle("Error")
                        .setMessage("There is no SD storage card installed or it is not writable!")
                        .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                android.os.Process.killProcess(android.os.Process.myPid());
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
                return null;
            }
            try {
                URL url = new URL(gs.getInternetURL() + "apiGetDocumentBinary.php?requisitionDocumentId=" + gs.getRequisitionDocumentId());
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                urlConnection.setRequestMethod("GET");
                urlConnection.setDoOutput(true);

                //connect
                urlConnection.connect();

//                DeleteDownloadedFiles();

                String filename = gs.getCurrentFile();//String contains storageDir name
                newFile=new File(storageDir, filename);

                gs.setFileName(filename);
                fileOutput = new FileOutputStream(newFile);

                //Stream used for reading the data from the internet
                InputStream inputStream = urlConnection.getInputStream();

                //create a buffer...
                byte[] buffer = new byte[1024];
                int bufferLength = 0;

                while ((bufferLength = inputStream.read(buffer)) > 0) {
                    fileOutput.write(buffer, 0, bufferLength);
                    downloadedSize += bufferLength;
                    // update the progressbar //
                }
            } catch (final MalformedURLException e) {
                showError("Error : MalformedURLException " + e);
                e.printStackTrace();
            } catch (final IOException e) {
                showError("Error : IOException " + e);
                e.printStackTrace();
            } catch (final Exception e) {
                showError("Error : Please check your internet connection " + e);
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
                if (gs.getCurrentFile() == "") {
                DeleteDownloadedFiles();
                DisplayRemoteFile.this.finish();
                return;
            }
            pDialog = new ProgressDialog(DisplayRemoteFile.this);
            pDialog.setMessage("Please wait...");
            pDialog.setCancelable(true);
            pDialog.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if (gs.getCurrentFile() == "") {
                DeleteDownloadedFiles();
                DisplayRemoteFile.this.finish();
                return;
            }

            if (pDialog.isShowing()) {
                pDialog.dismiss();
            }

            gs.setCurrentFile("");
            Uri uri = Uri.fromFile(newFile);
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(uri, gs.getDocumentMimeType());
            startActivity(intent);
        }
    }

    void showError(final String err) {
        runOnUiThread(new Runnable() {
            public void run() {
                new AlertDialog.Builder(DisplayRemoteFile.this)
                        .setTitle("Error")
                        .setMessage("The following message occured while trying to retrieve the requisition detail: " + err)
                        .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                android.os.Process.killProcess(android.os.Process.myPid());
                                System.exit(1);
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });
    }
}