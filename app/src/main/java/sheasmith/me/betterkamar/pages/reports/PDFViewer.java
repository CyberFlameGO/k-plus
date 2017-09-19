package sheasmith.me.betterkamar.pages.reports;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.github.barteksc.pdfviewer.PDFView;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import sheasmith.me.betterkamar.R;
import sheasmith.me.betterkamar.Servers;
import sheasmith.me.betterkamar.ServersObject;
import sheasmith.me.betterkamar.pages.notices.NoticesObject;
import sheasmith.me.betterkamar.pages.notices.NoticesPage;

/**
 * Created by Shea on 2/03/2017.
 */

public class PDFViewer extends Activity {
    ProgressDialog mProgressDialog;
    String reportName;
    String reportYear;


    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports_viewer);

        reportName = getIntent().getStringExtra("name");
        reportYear = getIntent().getStringExtra("year");
        String reportUrl = getIntent().getStringExtra("url");

        mProgressDialog = new ProgressDialog(PDFViewer.this);
        mProgressDialog.setTitle("Downloading Report...");
        mProgressDialog.setMessage("Downloading Report " + reportName);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(true);

        final DownloadTask downloadTask = new DownloadTask(PDFViewer.this);
        downloadTask.execute(reportUrl);

        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                downloadTask.cancel(true);
            }
        });

        //PDFView p = (PDFView) findViewById(R.id.webView);
        //p.fromUri(Uri.parse(Uri.decode("http//async5.org/moz/pdfjs.pdf"))).load();

    }



    private class DownloadTask extends AsyncTask<String, Integer, String> {
        private Context context;
        private PowerManager.WakeLock mWakeLock;

        public DownloadTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(String... sUrl) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(sUrl[0]);
                connection = (HttpURLConnection) url.openConnection();
                ServersObject s = Servers.getServersList().get(getIntent().getIntExtra("listID", -1));
                for (Map.Entry<String, String> entry : s.cookies.entrySet()) {

                    connection.addRequestProperty("Cookie", entry.getKey() + "=" + entry.getValue());
                }

                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();

                // download the file
                input = connection.getInputStream();
                String sname = s.title.replace(" ", "_");
                String category = reportYear.replace(" ", "_");
                String name = reportName.replace(" ", "_");
                File t = new File(getCacheDir(), "/betterkamar/" + sname);
                Log.d("file", "" + t.mkdirs());
                File fina = new File(t, category + "_" + name + ".pdf");

                output = new FileOutputStream(fina);

                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
            } catch (IOException e) {
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if (connection != null)
                    connection.disconnect();
            }
            return null;
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLock.acquire();
            mProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(100);
            mProgressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            mWakeLock.release();
            mProgressDialog.dismiss();
            ServersObject s = Servers.getServersList().get(getIntent().getIntExtra("listID", -1));
            if (result != null)
                Toast.makeText(context,"Download error: "+result, Toast.LENGTH_LONG).show();
            else {
                Toast.makeText(context, "File downloaded", Toast.LENGTH_SHORT).show();
                PDFView p = (PDFView) findViewById(R.id.webView);
                String sname = s.title.replace(" ", "_");
                String category = reportYear.replace(" ", "_");
                String name = reportName.replace(" ", "_");
                File f = new File(getCacheDir() + "/betterkamar/" + sname, category + "_" + name + ".pdf");
                Log.d("filename", f.getPath());
                p.fromFile(f).load();
            }
        }


    }
}
