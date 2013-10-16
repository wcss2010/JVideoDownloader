/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Downloader;

import Interface.DownloadStatus;
import Interface.IDownloaderPlugin;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * HTTP列表下载模块
 *
 * @author wcss
 */
public class HTTPURLListDownloader extends IDownloaderPlugin implements Runnable {

    private static int BUFFER_SIZE = 8096; //缓冲区大小
    //ftp://tv1:tv1@xl.77ds.com:11203/%E8%BD%A9%E8%BE%95%E5%89%91%E4%B9%8B%E5%A4%A9%E4%B9%8B%E7%97%95[%E7%AC%AC2%E9%9B%86TV].rmvb
    private Boolean isRun = false;
    private String subBufDir;
    private Thread thObj = null;
    private long currentSize = 0;
    private long totalSize = 0;
    private Boolean isQueryTotalSize = true;

    @Override
    public void setMaxDownloadSpeed(int speed) {
    }

    @Override
    public void setMaxUploadSpeed(int speed) {
    }

    @Override
    public void clearBufferDir() {
        try {
            JAppToolKit.JRunHelper.runSysCmd("rm -rf " + subBufDir);
        } catch (Exception ex) {
//            Logger.getLogger(HttpDownloader.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }
    }

    @Override
    public int getMaxDownloadSpeed() {
        return -1;
    }

    @Override
    public int getMaxUploadSpeed() {
        return -1;
    }

    @Override
    public Boolean isRunning() {
        return isRun;
    }

    @Override
    public Boolean isValidUrl(String url) {
        if (url.trim().toLowerCase().startsWith("http://") || url.trim().toLowerCase().startsWith("https://")) {
            if (url.contains(".")) {
                return true;
            } else {
                return false;
            }
        } else if (url.trim().toLowerCase().startsWith("ftp://")) {
            String s = url.trim().replace("ftp://", "");
            String[] check = s.split("/");
            if (check != null && check.length > 0) {
                if (check[0].contains(":") && check[0].contains("@") && check[0].contains(".")) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean isValidUrlList(String[] urlList) {
        Boolean result = true;
        for (String s : urlList) {
            if (!isValidUrl(s)) {
                result = false;
                break;
            }
        }
        return result;
    }

    @Override
    public void open(String[] urlList, String encode) throws Exception {
        if (urlList != null && urlList.length > 0 && isValidUrlList(urlList)) {
            this.urlList = new ArrayList<String>();
            this.bufferUrlList = new ArrayList<String>();

            //添加下载地址
            for (String s : urlList) {
                this.urlList.add(s);
            }

            this.currentUrlIndex = 0;
            subBufDir = this.getBufferDir() == "" ? JAppToolKit.JRunHelper.getUserHomeDirPath() + "/.httpvideocaches" : this.getBufferDir();
            subBufDir = subBufDir + "/" + this.downloaderID;
            //清理子缓冲目录
            this.clearBufferDir();
            File ff = new File(subBufDir);
            ff.mkdirs();

            for (int i = 0; i < urlList.length; i++) {
                this.bufferUrlList.add(subBufDir + "/" + (i + 1) + "_movie");
            }
        } else {
            throw new Exception("This URL is not download!");
        }
    }

    @Override
    public void start() throws Exception {
        isRun = true;
        this.currentUrlIndex = 0;
        thObj = new Thread(this);
        thObj.setDaemon(true);
        thObj.start();
        this.onReportStatus(DownloadStatus.startDowload, "");
    }

    @Override
    public void stop() throws Exception {
        isRun = false;
        thObj.stop();
        this.onReportStatus(DownloadStatus.stopDownload, "");
    }

    /**
     * 初始化影片大小
     */
    private void initVideoSize() throws Exception {
        if (isEnabledQueryTotalSize()) {
            if (this.totalSize <= 0) {
                if (this.urlList.get(this.currentUrlIndex).trim().toLowerCase().startsWith("http")) {
                    HttpURLConnection httpUrl = null;
                    URL url = null;
                    for (int k = 0; k < this.urlList.size(); k++) {
                        this.currentUrlIndex = k;
                        //建立链接
                        url = new URL(this.urlList.get(this.currentUrlIndex).trim());
                        httpUrl = (HttpURLConnection) url.openConnection();
                        httpUrl.setConnectTimeout(this.dataConnectionTimeout);
                        httpUrl.setReadTimeout(this.dataReadTimeout);
                        //连接指定的资源
                        httpUrl.connect();
                        this.totalSize += httpUrl.getContentLength();
                        httpUrl.disconnect();
                    }
                } else {
                    this.totalSize = 0;
                }
            }
        } else {
            this.totalSize = 0;
        }
    }

    @Override
    public long getTotalSize() {
        try {
            initVideoSize();
        } catch (Exception ex) {
            ex.printStackTrace();
            //Logger.getLogger(HttpDownloader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return this.totalSize;
    }

    @Override
    public long getCurSize() {
        return this.currentSize;
    }

    @Override
    public int getDownloadProgress() {
        return (int) (((double) this.getCurSize() / (double) this.getTotalSize()) * 100);
    }

    @Override
    public String getBufferFileUrl(int fileIndex) {
        return this.bufferUrlList.get(fileIndex);
    }

    @Override
    public int getDownloadSpeed() {
        return -1;
    }

    @Override
    public String getSourceFileName(int fileIndex) {
        String[] aaa = this.urlList.get(fileIndex).split("/");
        return aaa[aaa.length - 1].split("\\.")[0];
    }

    @Override
    public IDownloaderPlugin getNewDownloader() {
        return new HTTPURLListDownloader();
    }

    /**
     * 将HTTP资源另存为文件
     *
     * @param destUrl String
     * @param fileName String
     * @throws Exception
     */
    public Boolean httpSaveToFile(String destUrl, String fileName) throws IOException {
        FileOutputStream fos = null;
        BufferedInputStream bis = null;
        HttpURLConnection httpUrl = null;
        URL url = null;
        byte[] buf = new byte[BUFFER_SIZE];
        int size = 0;
        try {

//建立链接
            url = new URL(destUrl);
            httpUrl = (HttpURLConnection) url.openConnection();

            //设置超时
            httpUrl.setConnectTimeout(this.dataConnectionTimeout);
            httpUrl.setReadTimeout(this.dataReadTimeout);

//连接指定的资源
            httpUrl.connect();
//获取网络输入流
            bis = new BufferedInputStream(httpUrl.getInputStream());
//建立文件
            fos = new FileOutputStream(fileName);

//        if (this.DEBUG)
//            System.out.println("正在获取链接[" + destUrl + "]的内容.../n将其保存为文件[" +
//                               fileName + "]");
//保存文件
            while ((size = bis.read(buf)) != -1) {
                currentSize += size;
                fos.write(buf, 0, size);
                this.onReportProgress(this.currentUrlIndex, currentSize);
            }

            return true;
        } catch (Exception ex) {
            this.onReportError(ex.toString());
            return false;
        } finally {
            try {
                fos.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            try {
                bis.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            try {
                httpUrl.disconnect();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        try {
            this.totalSize = this.getTotalSize();

            for (int k = 0; k < this.urlList.size(); k++) {
                this.currentUrlIndex = k;
                if (this.isRunning()) {
                    Boolean result = this.httpSaveToFile(this.urlList.get(this.currentUrlIndex), this.bufferUrlList.get(this.currentUrlIndex));
                    for (int f = 0; f < this.maxTryCount; f++) {
                        if (result) {
                            break;
                        } else {
                            this.onReportStatus(DownloadStatus.downloadAgain, "重新下载序号" + this.currentUrlIndex + "的文件!");
                            deleteBufferFile(this.currentUrlIndex);

                            Boolean resultAgain = this.httpSaveToFile(this.urlList.get(this.currentUrlIndex), this.bufferUrlList.get(this.currentUrlIndex));
                            if (resultAgain) {
                                result = resultAgain;
                                break;
                            }
                        }
                    }

                    if (!result) {
                        throw new Exception("下载失败!");
                    }
                }
            }

            this.onReportFinish();
            this.isRun = false;
        } catch (Exception ex) {
            this.isRun = false;
            this.onReportError(ex.toString());
        }
    }

    @Override
    public Boolean isEnabledQueryTotalSize() {
        return isQueryTotalSize;
    }

    @Override
    public void SetEnabledQueryTotalSize(Boolean result) {
        isQueryTotalSize = result;
    }

    @Override
    public Boolean deleteBufferFile(int fileIndex) {
        if (this.bufferUrlList.size() > fileIndex) {
            if (new File(this.bufferUrlList.get(fileIndex)).exists()) {
                try {
                    JAppToolKit.JRunHelper.runSysCmd("rm -rf " + this.bufferUrlList.get(fileIndex));
                    return true;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
}