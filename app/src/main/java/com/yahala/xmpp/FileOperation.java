package com.yahala.xmpp;

import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;

import com.yahala.SQLite.Messages;
import com.yahala.android.OSUtilities;

import com.yahala.messenger.FileLog;
import com.yahala.messenger.NotificationCenter;
import com.yahala.messenger.TLRPC;
import com.yahala.messenger.UserConfig;
import com.yahala.objects.MessageObject;
import com.yahala.ui.ApplicationLoader;
import com.yahala.ui.Rows.ConnectionsManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.jivesoftware.smack.util.StringUtils;
import org.jxmpp.util.XmppStringUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Created by user on 6/4/2014.
 */
public class FileOperation {
    public float uploadProgress = 0;
    String uploadUrl = "https://188.247.90.132:9092/FileUpload/";
    TelephonyManager telephonyManager;
    int currentRequestId;
    private boolean canceled;
    private HttpGet httpget;
    private HttpPost httppost;
    private String type;
    private String filePath;
    private Messages newMsg;
    private String toJid;
    public int state = 0;
    public int operationType = 0;
    public FileUploadOperationDelegate delegate;
    HttpClient httpClient;
    private String sUrl;

    public static interface FileUploadOperationDelegate {
        public abstract void didFinishUploadingFile(FileOperation operation, int mid);

        public abstract void didFailedUploadingFile(FileOperation operation);

        public abstract void didChangedUploadProgress(FileOperation operation, float progress, int mid);
    }

    public FileOperation(String type, String filePath, Messages newMsg, String toJid) {
        this.type = type;
        this.filePath = filePath;
        this.newMsg = newMsg;
        this.toJid = toJid;
    }

    public FileOperation(final String sUrl, final Messages newMsg) {
        this.sUrl = sUrl;
        this.newMsg = newMsg;
        operationType = 1;

    }

    public void start() {
        if (state != 0) {
            return;
        }
        state = 1;
        if (operationType == 0) {
            startUploadRequest();
        } else if (operationType == 1) {
            startDownloadRequest();
        }
    }

    public void cancel() {
        FileLog.e("FileUpload", "cancel state:" + state);
        if (state != 1) {
            return;
        }
        state = 2;
        if (httppost != null) {
            httppost.abort();

        }
        if (httpget != null) {
            httpget.abort();
        }
        httpClient.getConnectionManager().shutdown();
        delegate.didFailedUploadingFile(this);
    }

    //cancelUploadFile
    public void startUploadRequest() {

        try {
            HttpParams httpParams = new BasicHttpParams();
            httpParams.setParameter(CoreProtocolPNames.HTTP_CONTENT_CHARSET, HTTP.UTF_8);
            httpParams.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);

            SchemeRegistry registry = new SchemeRegistry();

            KeyStore trustStore = KeyStore.getInstance(KeyStore
                    .getDefaultType());
            trustStore.load(null, null);
            SSLSocketFactory sf = new MySSLSocketFactory(trustStore);

            sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            registry.register(new Scheme("http", new PlainSocketFactory(), 80));
            registry.register(new Scheme("https", sf, 443));
            FileLog.e("FileUpload", "uploadFile " + type);
            //http://download.java.net/jdk8/docs/technotes/guides/security/jsse/JSSERefGuide.html

	                /* Make a thread safe connection manager for the client */
            ThreadSafeClientConnManager manager = new ThreadSafeClientConnManager(httpParams, registry);

            httpClient = new DefaultHttpClient(manager, httpParams);

            final File file = new File(filePath);
            httppost = new HttpPost(uploadUrl);

            final CustomMultiPartEntity multiPartEntity = new CustomMultiPartEntity(HttpMultipartMode.BROWSER_COMPATIBLE, new CustomMultiPartEntity.ProgressListener() {
                @Override
                public void transferred(final long currentUploaded) {


                    uploadProgress = (float) currentUploaded / (float) file.length();
                    //FileLog.e("FileUpload", "uploadProgress: " + uploadProgress);
                    if (currentUploaded >= file.length() && state != 3) {

                        uploadProgress = 1.0f;


                    }
                    delegate.didChangedUploadProgress(FileOperation.this, uploadProgress, newMsg.id);


                }
            });


            multiPartEntity.addPart("upload", new FileBody(file));
            multiPartEntity.addPart("type", new StringBody(type));
            multiPartEntity.addPart("from_jid", new StringBody(UserConfig.currentUser.phone));
            multiPartEntity.addPart("to_jid", new StringBody(XmppStringUtils.parseLocalpart(toJid)));
            httppost.setEntity(multiPartEntity);

            //FileLog.e("FileUpload", "httpClient.execute ");
            HttpResponse response = httpClient.execute(httppost);
            if (state == 2) {
                return;
            }

            while (!(XMPPManager.getInstance().isConnected() && XMPPManager.getInstance().connection.isAuthenticated())) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            httpClient.getConnectionManager().shutdown();
            FileLog.e("FileUpload", "Upload response  " + response.getStatusLine().getStatusCode());
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                XMPPManager.getInstance().sendOutOfBandMessage(toJid, newMsg, filePath);
            }
            state = 3;
            delegate.didFinishUploadingFile(FileOperation.this, newMsg.id);

            uploadProgress = 0;
            //  }
            // FileLog.e("FileUpload", "httpClient.end ");

        } catch (Exception e) {
            FileLog.e("yahala", e);
        }


    }

    public void startDownloadRequest() {
        state = 1;
        FileLog.e("FileUpload", "startDownloadRequest");
        String fileExtension = MimeTypeMap.getFileExtensionFromUrl(sUrl);
        String fileName = URLUtil.guessFileName(sUrl, null, fileExtension);
        String mimeType = URLConnection.guessContentTypeFromName(fileName);
        if (mimeType == null) {
            mimeType = "";
        }
        FileLog.e("DownloadFile", "file name: " + fileName + "  fileExtension: " + fileExtension);

        String root = Environment.getExternalStorageDirectory().toString();
        String path = "/yahala/media/yahala Images/received/";


        String fromJid = XmppStringUtils.parseBareAddress(newMsg.getJid());
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String gFileName = "VID_" + timeStamp + fileName; //"." + fileExtension;
        // FileLog.e("XMPPManager", mimeType);
        newMsg.tl_message = new TLRPC.TL_message();
        newMsg.tl_message.from_id = newMsg.getJid();


        if (mimeType.length() != 0 && mimeType.contains("image")) {

            path = "/yahala/media/yahala Images/received/";
            File myDir = new File(root + path);
            myDir.mkdirs();
            newMsg.tl_message.attachPath = root + path + fileName;

            //  FileLog.e("XMPPManager", "fileTransferRequest " + root + path + fileName);
            downloadFile(new File(root + path, fileName), sUrl, newMsg, mimeType);
        } else if (mimeType.length() != 0 && mimeType.contains("video")) {

            path = "/yahala/media/yahala Video/received/";
            File myDir = new File(root + path);
            myDir.mkdirs();
            //   FileLog.e("XMPPManager", "fileTransferRequest " + root + path + gFileName);
            newMsg.tl_message.attachPath = root + path + gFileName;

            downloadFile(new File(root + path, gFileName), sUrl, newMsg, mimeType);

        } else if (mimeType.length() != 0 && mimeType.contains("audio")) {

            path = "/yahala/media/yahala Audio/received/";

            String name = Integer.MIN_VALUE + "_" + UserConfig.lastLocalId + ".m4a";

            //File myDir = new File(root + path);
            // myDir.mkdirs();

            newMsg.tl_message.attachPath = OSUtilities.getCacheDir() + name;
            downloadFile(new File(OSUtilities.getCacheDir(), name), sUrl, newMsg, mimeType);


        } else {
            path = "/yahala/media/yahala documents/";
            File myDir = new File(root + path);
            myDir.mkdirs();

            newMsg.tl_message.attachPath = OSUtilities.getCacheDir() + "/" + fileName;

            downloadFile(new File(OSUtilities.getCacheDir(), fileName), sUrl, newMsg, mimeType);
        }
        //FileLog.e("recieveFile path", path);


        // FileLog.e("yahala fileTransferRequest", root + path + gFileName);


    }

    protected String downloadFile(File file, final String sUrl, Messages newMsg, String mimeType) {

        try {
            HttpParams httpParams = new BasicHttpParams();
            httpParams.setParameter(CoreProtocolPNames.HTTP_CONTENT_CHARSET, HTTP.UTF_8);
            httpParams.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
            // .. httpParams.setParameter(CoreProtocolPNames.HTTP_CONTENT_CHARSET,);
            SchemeRegistry registry = new SchemeRegistry();

            KeyStore trustStore = KeyStore.getInstance(KeyStore
                    .getDefaultType());
            trustStore.load(null, null);
            SSLSocketFactory sf = new MySSLSocketFactory(trustStore);

            sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            registry.register(new Scheme("http", new PlainSocketFactory(), 80));
            registry.register(new Scheme("https", sf, 443));


            ThreadSafeClientConnManager manager = new ThreadSafeClientConnManager(httpParams, registry);
            httpClient = new DefaultHttpClient(manager, httpParams);


            //String url= URLEncoder.encode(sUrl, "UTF-8").replace("+", "%20"));
            //FileLog.e("HttpGet", sUrl);
            //FileLog.e("HttpGet", sUrl.replace(" ", "%20"));
            httpget = new HttpGet(sUrl.replace(" ", "%20"));
            HttpResponse response = httpClient.execute(httpget);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                response = httpClient.execute(httpget);
            } else {
            }

            System.out.println("Response status: " + response.getStatusLine().getStatusCode() + "");
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                InputStream instream = entity.getContent();
                try {


                    BufferedInputStream bis = new BufferedInputStream(instream);

                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
                    byte data[] = new byte[4096];//new byte[4096];
                    long currentUploaded = 0;
                    long totalFileSize = entity.getContentLength();
                    int inByte;
                    final Long mid = newMsg.getId();
                    float progress = 0.0f;
                    if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                        while ((inByte = bis.read(data)) != -1) {
                            currentUploaded += inByte;
                            //bos.write(inByte);
                            bos.write(data, 0, inByte);

                            if (totalFileSize > 0) // only if total length is known
                            {
                                progress = 1.0f;
                                //FileLog.e("FileDownload", String.valueOf((int) ((total * 100 / contentLength))));

                            } else {

                                progress = (float) currentUploaded / (float) totalFileSize;

                            }
                            // final float p = progress;
                            delegate.didChangedUploadProgress(FileOperation.this, progress, (int) (long) mid);
                          /*  Utilities.RunOnUIThread(new Runnable() {
                                @Override
                                public void run() {
                                    NotificationCenter.getInstance().postNotificationName(XMPPManager.uploadProgressDidChanged, mid, p);
                                }
                            });*/

                        }

                        bis.close();
                        bos.close();
                        //delegate.didFinishUploadingFile(FileUploadOperation.this,(int)(long)mid);
                    }
                } catch (IOException ex) {
                    throw ex;
                } catch (RuntimeException ex) {
                    httpget.abort();
                    throw ex;
                } finally {
                    instream.close();
                    httpClient.getConnectionManager().shutdown();
                }


            }

            if (mimeType.contains("image")) {
                TLRPC.TL_messageMediaPhoto tl_messageMediaPhoto = new TLRPC.TL_messageMediaPhoto();
                TLRPC.TL_photo tl_photo = MessagesController.getInstance().generatePhotoSizes(newMsg.tl_message.attachPath, null);
                tl_messageMediaPhoto.photo = tl_photo;
                newMsg.tl_message.media = tl_messageMediaPhoto;
                newMsg.setType(3);
            } else if (mimeType.contains("video")) {
                newMsg.setType(7);

                String videoPath = newMsg.tl_message.attachPath;
                if (videoPath == null || videoPath.length() == 0) {
                    return "";
                }
                Bitmap thumb = ThumbnailUtils.createVideoThumbnail(videoPath, MediaStore.Video.Thumbnails.MINI_KIND);
                TLRPC.PhotoSize size = FileLoader.scaleAndSaveImage(thumb, 120, 120, 100, false);
                if (size == null) {
                    return "";
                }
                size.type = "s";
                TLRPC.TL_video video = new TLRPC.TL_video();
                video.thumb = size;
                video.caption = "";
                video.id = 0;
                video.path = videoPath;
                File temp = new File(videoPath);
                if (temp != null && temp.exists()) {
                    video.size = (int) temp.length();
                }
                UserConfig.lastLocalId--;
                UserConfig.saveConfig(false);

                MediaPlayer mp = MediaPlayer.create(ApplicationLoader.applicationContext, Uri.fromFile(new File(videoPath)));
                if (mp == null) {
                    return "";
                }
                video.duration = (int) Math.ceil(mp.getDuration() / 1000.0f);
                video.w = mp.getVideoWidth();
                video.h = mp.getVideoHeight();
                mp.release();

                MediaStore.Video.Media media = new MediaStore.Video.Media();

                newMsg.tl_message.media = new TLRPC.TL_messageMediaVideo();
                newMsg.tl_message.media.video = video;
                newMsg.setType(7);
                newMsg.tl_message.message = "-1";
                newMsg.tl_message.attachPath = video.path;

            } else if (mimeType.contains("audio")) {

                newMsg.setType(19);
                // FileLog.e("Test", "Audio != null");
                // newMsg.tl_message = new TLRPC.TL_message();

                final TLRPC.TL_audio tl_audio = new TLRPC.TL_audio();

                tl_audio.dc_id = Integer.MIN_VALUE;
                tl_audio.id = UserConfig.lastLocalId;
                tl_audio.user_id = UserConfig.clientUserId;
                UserConfig.lastLocalId--;
                UserConfig.saveConfig(false);
                tl_audio.size = (int) entity.getContentLength();
                tl_audio.path = OSUtilities.getCacheDir() + "" + Integer.MIN_VALUE + "_" + UserConfig.lastLocalId + ".m4a";
                TLRPC.TL_messageMediaAudio tl_messageMediaAudio = new TLRPC.TL_messageMediaAudio();


                //FileLog.e("Test", tl_audio.size + " , " + tl_audio.path);
                File filea = new File(OSUtilities.getCacheDir(), Integer.MIN_VALUE + "_" + UserConfig.lastLocalId + ".m4a");

                   /* MediaPlayer mp = MediaPlayer.create(ApplicationLoader.applicationContext, Uri.fromFile(file));
                    if (mp == null) {
                    return;
                    }*/

                tl_audio.duration = (int) Math.ceil(5000 / 1000.0f);
                //mp.release();
                tl_messageMediaAudio.audio = tl_audio;
                newMsg.tl_message.media = tl_messageMediaAudio;
            } else {
                newMsg.setType(17);
                TLRPC.TL_document document = new TLRPC.TL_document();
                FileLog.e("file.length()", file.length() + "");
                FileLog.e("newMsg.tl_message.attachPath", newMsg.tl_message.attachPath);
                FileLog.e("mimeType", mimeType);
                document.thumb = new TLRPC.TL_photoSizeEmpty();
                document.thumb.type = "s";
                document.id = 0;
                document.user_id = UserConfig.clientUserId;
                document.date = ConnectionsManager.getInstance().getCurrentTime();
                document.file_name = file.getName();
                document.size = (int) entity.getContentLength();
                document.dc_id = 0;
                document.mime_type = mimeType;
                document.path = newMsg.tl_message.attachPath;

                if (mimeType != "") {
                    if (mimeType != null) {
                        document.mime_type = mimeType;
                    } else {
                        document.mime_type = "application/octet-stream";
                    }
                } else {
                    document.mime_type = "application/octet-stream";
                }

                if (document.mime_type.equals("image/gif")) {
                    try {
                        Bitmap bitmap = FileLoader.loadBitmap(file.getAbsolutePath(), null, 90, 90);
                        if (bitmap != null) {
                            document.thumb = FileLoader.scaleAndSaveImage(bitmap, 90, 90, 55, false);
                            document.thumb.type = "s";
                        }
                    } catch (Exception e) {
                        FileLog.e("tmessages", e);
                    }
                }
                if (document.thumb == null) {
                    document.thumb = new TLRPC.TL_photoSizeEmpty();
                    document.thumb.type = "s";
                }
                newMsg.tl_message.media = new TLRPC.TL_messageMediaDocument();
                newMsg.tl_message.media.document = document;


            }
            newMsg.tl_message.message = "-1";
            newMsg.setMessage("-1");
            //  FileLog.e("Test   newMsg.setId", "dsadsd");
                   /* newMsg.setId(Long.parseLong(String.valueOf(UserConfig.getNewMessageId())));
                    newMsg.setJid(fromJid);
                    newMsg.setMessage("-1");
                    newMsg.tl_message.from_id = fromJid;
                    newMsg.tl_message.message = "-1";

                    newMsg.setRead_state(0);
                    newMsg.setSend_state(XmppManager.MESSAGE_SEND_STATE_AKN);
                    newMsg.setOut(0);*/
            state = 3;
            newMsg.setDate(new Date());
            newMsg.tl_message.media.progress = 100;

            MessagesStorage.getInstance().putMessage(newMsg);
            NotificationCenter.getInstance().postNotificationName(XMPPManager.didReceivedNewMessages, newMsg);
            UserConfig.saveConfig(false);
            MessageObject messagesObject = new MessageObject(newMsg, null);
            MessagesController.getInstance().showInAppNotification(messagesObject);
               /* XmppManager.getInstance().postNotification("you received a new file",
                        ContactsController.getInstance().friendsDict.get(newMsg.getJid()).first_name + " " +
                                ContactsController.getInstance().friendsDict.get(newMsg.getJid()).last_name +
                                " sent you a file"
                );*/
            //    }
        } catch (Exception e) {
            e.printStackTrace();
            return "";
            // return e.toString();
        } finally {


        }


        return null;
    }


}
