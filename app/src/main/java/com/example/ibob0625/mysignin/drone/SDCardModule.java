package com.example.ibob0625.mysignin.drone;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.parrot.arsdk.ardatatransfer.ARDATATRANSFER_ERROR_ENUM;
import com.parrot.arsdk.ardatatransfer.ARDataTransferException;
import com.parrot.arsdk.ardatatransfer.ARDataTransferManager;
import com.parrot.arsdk.ardatatransfer.ARDataTransferMedia;
import com.parrot.arsdk.ardatatransfer.ARDataTransferMediasDownloader;
import com.parrot.arsdk.ardatatransfer.ARDataTransferMediasDownloaderCompletionListener;
import com.parrot.arsdk.ardatatransfer.ARDataTransferMediasDownloaderProgressListener;
import com.parrot.arsdk.arutils.ARUtilsManager;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;


public class SDCardModule {
    private static final String TAG = "SDCardModule";

    private static final String DRONE_MEDIA_FOLDER = "internal_000";
    private static final String MOBILE_MEDIA_FOLDER = "/ARSDKMedias/";

    public interface Listener {
        /**
         * Called before medias will be downloaded
         * Called on a separate thread
         * @param nbMedias the number of medias that will be downloaded
         */
        void onMatchingMediasFound(int nbMedias);

        /**
         * Called each time the progress of a download changes
         * Called on a separate thread
         * @param mediaName the name of the media
         * @param progress the progress of its download (from 0 to 100)
         */
        void onDownloadProgressed(String mediaName, int progress);

        /**
         * Called when a media download has ended
         * Called on a separate thread
         * @param mediaName the name of the media
         */
        void onDownloadComplete(String mediaName);
    }

    private final List<Listener> mListeners;

    private ARDataTransferManager mDataTransferManager;
    private ARUtilsManager mFtpList;
    private ARUtilsManager mFtpQueue;

    private boolean mThreadIsRunning;
    private boolean mIsCancelled;

    private int mNbMediasToDownload;
    private int mCurrentDownloadIndex;

    public SDCardModule(@NonNull ARUtilsManager ftpListManager, @NonNull ARUtilsManager ftpQueueManager) {

        mThreadIsRunning = false;
        mListeners = new ArrayList<>();
        mFtpList = ftpListManager;
        mFtpQueue = ftpQueueManager;

        ARDATATRANSFER_ERROR_ENUM result = ARDATATRANSFER_ERROR_ENUM.ARDATATRANSFER_OK;
        try {
            mDataTransferManager = new ARDataTransferManager();
        } catch (ARDataTransferException e) {
            Log.e(TAG, "Exception", e);
            result = ARDATATRANSFER_ERROR_ENUM.ARDATATRANSFER_ERROR;
        }

        if (result == ARDATATRANSFER_ERROR_ENUM.ARDATATRANSFER_OK) {
            // direct to external directory
            String externalDirectory = Environment.getExternalStorageDirectory().toString().concat(MOBILE_MEDIA_FOLDER);

            // if the directory doesn't exist, create it
            File f = new File(externalDirectory);
            if(!(f.exists() && f.isDirectory())) {
                boolean success = f.mkdir();
                if (!success) {
                    Log.e(TAG, "Failed to create the folder " + externalDirectory);
                }
            }
            try {
                mDataTransferManager.getARDataTransferMediasDownloader().createMediasDownloader(mFtpList, mFtpQueue, DRONE_MEDIA_FOLDER, externalDirectory);
            } catch (ARDataTransferException e) {
                Log.e(TAG, "Exception", e);
                result = e.getError();
            }
        }

        if (result != ARDATATRANSFER_ERROR_ENUM.ARDATATRANSFER_OK) {
            // clean up here because an error happened
            mDataTransferManager.dispose();
            mDataTransferManager = null;
        }
    }
    //建構子:
    //接收List Manager和Queue Manager作為參數
    //嘗試建立Data Transfer Manager，如建立成功再建立外部資料夾，失敗則Data Transfer狀態為ERROR
    //嘗試建立Medias Downloader，使用List Manager, Queue Manager, 飛機內部資料夾及外部資料夾為參數
    //最後如果狀態為ERROR則清除Data Transfer Manager

    //region Listener functions
    public void addListener(Listener listener) {
        mListeners.add(listener);
    }
    public void removeListener(Listener listener) {
        mListeners.remove(listener);
    }
    //endregion Listener

    public void getFlightMedias(final String runId) {
        if (!mThreadIsRunning) {
            mThreadIsRunning = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    ArrayList<ARDataTransferMedia> mediaList = getMediaList();

                    ArrayList<ARDataTransferMedia> mediasFromRun = null;
                    mNbMediasToDownload = 0;
                    if ((mediaList != null) && !mIsCancelled) {
                        mediasFromRun = getRunIdMatchingMedias(mediaList, runId);
                        mNbMediasToDownload = mediasFromRun.size();
                    }

                    notifyMatchingMediasFound(mNbMediasToDownload);

                    if ((mediasFromRun != null) && (mNbMediasToDownload != 0) && !mIsCancelled) {
                        downloadMedias(mediasFromRun);
                    }

                    mThreadIsRunning = false;
                    mIsCancelled = false;
                }
            }).start();
        }
    }
    //開始Thread，兩個List變數: mediaList, mediasFromRun
    //呼叫getMediaList() 取得mediaList
    //呼叫getRunIdMatchingMedias(mediaList, runId) 從mediaList中選擇符合Run ID的medias，取得mediasFromRun
    //將mediasFromRun的數量notify給onMatchingMediasFound監聽
    //呼叫downloadMedias以mediasFromRun為參數 (下載mediasFromRun中所有medias)

    public void getTodaysFlightMedias() {
        if (!mThreadIsRunning) {
            mThreadIsRunning = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    ArrayList<ARDataTransferMedia> mediaList = getMediaList();

                    ArrayList<ARDataTransferMedia> mediasFromDate = null;
                    mNbMediasToDownload = 0;
                    if ((mediaList != null) && !mIsCancelled) {
                        GregorianCalendar today = new GregorianCalendar();
                        mediasFromDate = getDateMatchingMedias(mediaList, today);
                        mNbMediasToDownload = mediasFromDate.size();
                    }

                    notifyMatchingMediasFound(mNbMediasToDownload);

                    if ((mediasFromDate != null) && (mNbMediasToDownload != 0) && !mIsCancelled) {
                        downloadMedias(mediasFromDate);
                    }

                    mThreadIsRunning = false;
                    mIsCancelled = false;
                }
            }).start();
        }
    }
    //開始Thread，兩個List變數: mediaList, mediasFromDate
    //呼叫getMediaList() 取得mediaList
    //呼叫getDateMatchingMedias(mediaList, runId) 從mediaList中選擇符合today的medias，取得mediasFromDate
    //將mediasFromDate的數量notify給onMatchingMediasFound監聽
    //呼叫downloadMedias以mediasFromDate為參數 (下載mediasFromDate中所有medias)

    public void cancelGetFlightMedias() {
        if (mThreadIsRunning) {
            mIsCancelled = true;
            ARDataTransferMediasDownloader mediasDownloader = null;
            if (mDataTransferManager != null) {
                mediasDownloader = mDataTransferManager.getARDataTransferMediasDownloader();
            }
            if (mediasDownloader != null) {
                mediasDownloader.cancelQueueThread();
            }
        }
    }
    //取消Data Transfer的Medias Downloader的Queue Thread

    //my version
    public String getOneMedia() {
        final ARDataTransferMedia media = getOneMediaFromDownloader();
        if (!mThreadIsRunning) {
            mThreadIsRunning = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (!mIsCancelled)
                        downloadOneMedia(media);
                    mThreadIsRunning = false;
                    mIsCancelled = false;
                }
            }).start();
        }
        String s = media.getName();
        Log.d(TAG, "return " + s);
        return s;
    }
    private ARDataTransferMedia getOneMediaFromDownloader() {
        ARDataTransferMedia media = null;

        ARDataTransferMediasDownloader mediasDownloader = null;
        if (mDataTransferManager != null)
            mediasDownloader = mDataTransferManager.getARDataTransferMediasDownloader();

        int mediaListCount = 0;
        if (mediasDownloader != null) {
            try {
                mediaListCount = mediasDownloader.getAvailableMediasSync(false);
                ARDataTransferMedia currentMedia = mediasDownloader.getAvailableMediaAtIndex(mediaListCount - 1);
                return currentMedia;
            }
            catch (ARDataTransferException e) {
                Log.e(TAG, "Exception", e);
                media = null;
            }
        }
        return null;
    }
    private void downloadOneMedia(@NonNull ARDataTransferMedia Media) {
        mCurrentDownloadIndex = 1;

        ARDataTransferMediasDownloader mediasDownloader = null;
        if (mDataTransferManager != null)
            mediasDownloader = mDataTransferManager.getARDataTransferMediasDownloader();

        if (mediasDownloader != null) {
            try {
                mediasDownloader.addMediaToQueue(Media, mDLProgressListener, null, mDLCompletionListener, null);
            }
            catch (ARDataTransferException e) {
                Log.e(TAG, "Exception", e);
            }

            if (!mIsCancelled){
                mediasDownloader.getDownloaderQueueRunnable().run();
            }
        }
    }

    private ArrayList<ARDataTransferMedia> getMediaList() {
        ArrayList<ARDataTransferMedia> mediaList = null;

        ARDataTransferMediasDownloader mediasDownloader = null;
        if (mDataTransferManager != null)
            mediasDownloader = mDataTransferManager.getARDataTransferMediasDownloader();
        if (mediasDownloader != null) {
            try {
                int mediaListCount = mediasDownloader.getAvailableMediasSync(false);
                mediaList = new ArrayList<>(mediaListCount);

                for (int i = 0; ((i < mediaListCount) && !mIsCancelled) ; i++) {
                    ARDataTransferMedia currentMedia = mediasDownloader.getAvailableMediaAtIndex(i);
                    mediaList.add(currentMedia);
                }
            }
            catch (ARDataTransferException e) {
                Log.e(TAG, "Exception", e);
                mediaList = null;
            }
        }
        return mediaList;
    }
    //取得Data Transfer的Medias Downloader
    //從Medias Downloader取得count，再建立mediaList
    //透過Medias Downloader將media一個一個加入mediaList中

    private @NonNull ArrayList<ARDataTransferMedia> getRunIdMatchingMedias(
            ArrayList<ARDataTransferMedia> mediaList,
            String runId) {
        ArrayList<ARDataTransferMedia> matchingMedias = new ArrayList<>();
        for (ARDataTransferMedia media : mediaList) {
            if (media.getName().contains(runId)) //名字中包含Run ID
                matchingMedias.add(media);
            // exit if the async task is cancelled
            if (mIsCancelled)
                break;
        }

        return matchingMedias;
    }
    //回傳名字中包含對應Run ID的medias 的清單

    private ArrayList<ARDataTransferMedia> getDateMatchingMedias(ArrayList<ARDataTransferMedia> mediaList,
                                                                 GregorianCalendar matchingCal) {
        ArrayList<ARDataTransferMedia> matchingMedias = new ArrayList<>();
        Calendar mediaCal = new GregorianCalendar();
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HHmmss", Locale.getDefault());
        for (ARDataTransferMedia media : mediaList) {
            // convert date in string to calendar
            String dateStr = media.getDate();
            try {
                Date mediaDate = dateFormatter.parse(dateStr);
                mediaCal.setTime(mediaDate);

                // if the date are the same day
                if ((mediaCal.get(Calendar.DAY_OF_MONTH) == (matchingCal.get(Calendar.DAY_OF_MONTH))) &&
                        (mediaCal.get(Calendar.MONTH) == (matchingCal.get(Calendar.MONTH))) &&
                        (mediaCal.get(Calendar.YEAR) == (matchingCal.get(Calendar.YEAR)))) {
                    matchingMedias.add(media);
                }
            } catch (ParseException e) {
                Log.e(TAG, "Exception", e);
            }

            // exit if the async task is cancelled
            if (mIsCancelled) {
                break;
            }
        }

        return matchingMedias;
    }
    //回傳符合對應日期的medias 的清單

    private void downloadMedias(@NonNull ArrayList<ARDataTransferMedia> matchingMedias) {
        mCurrentDownloadIndex = 1;

        ARDataTransferMediasDownloader mediasDownloader = null;
        if (mDataTransferManager != null)
            mediasDownloader = mDataTransferManager.getARDataTransferMediasDownloader();

        if (mediasDownloader != null) {
            for (ARDataTransferMedia media : matchingMedias) {
                try {
                    mediasDownloader.addMediaToQueue(media, mDLProgressListener, null, mDLCompletionListener, null);
                } catch (ARDataTransferException e) {
                    Log.e(TAG, "Exception", e);
                }

                // exit if the async task is cancelled
                if (mIsCancelled)
                    break;
            }

            if (!mIsCancelled)
                mediasDownloader.getDownloaderQueueRunnable().run();
        }
    }
    //取得Data Transfer的Medias Downloader
    //將要下載的medias加入Downloader 的Queue 之中

    //region notify listener block
    private void notifyMatchingMediasFound(int nbMedias) {
        List<Listener> listenersCpy = new ArrayList<>(mListeners);
        for (Listener listener : listenersCpy)
            listener.onMatchingMediasFound(nbMedias);
    }

    private void notifyDownloadProgressed(String mediaName, int progress) {
        List<Listener> listenersCpy = new ArrayList<>(mListeners);
        for (Listener listener : listenersCpy)
            listener.onDownloadProgressed(mediaName, progress);
    }

    private void notifyDownloadComplete(String mediaName) {
        List<Listener> listenersCpy = new ArrayList<>(mListeners);
        for (Listener listener : listenersCpy)
            listener.onDownloadComplete(mediaName);
    }
    //endregion notify listener block

    private final ARDataTransferMediasDownloaderProgressListener mDLProgressListener = new ARDataTransferMediasDownloaderProgressListener() {
        private int mLastProgressSent = -1;
        @Override
        public void didMediaProgress(Object arg, ARDataTransferMedia media, float percent) {
            final int progressInt = (int) Math.floor(percent);
            if (mLastProgressSent != progressInt) {
                mLastProgressSent = progressInt;
                Log.d(TAG, "download running with progress" + progressInt + "%");
                notifyDownloadProgressed(media.getName(), progressInt);
            }
        }
    };

    private final ARDataTransferMediasDownloaderCompletionListener mDLCompletionListener = new ARDataTransferMediasDownloaderCompletionListener() {
        @Override
        public void didMediaComplete(Object arg, ARDataTransferMedia media, ARDATATRANSFER_ERROR_ENUM error) {
            notifyDownloadComplete(media.getName());

            Log.d(TAG, "download complete");
            // when all download are finished, stop the download runnable
            // in order to get out of the downloadMedias function
            mCurrentDownloadIndex ++;
            if (mCurrentDownloadIndex > mNbMediasToDownload ) {
                ARDataTransferMediasDownloader mediasDownloader = null;
                if (mDataTransferManager != null)
                    mediasDownloader = mDataTransferManager.getARDataTransferMediasDownloader();

                if (mediasDownloader != null)
                    mediasDownloader.cancelQueueThread();
            }
        }
    };
}

