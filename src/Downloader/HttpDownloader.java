/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Downloader;

import Interface.IDownloaderPlugin;
import Interface.DownloadStatus;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import sun.net.TelnetInputStream;
import sun.net.ftp.FtpClient;

/**
 * HTTP影片下载模块（同时支持Http协议和FTP协议）
 *
 * @author wcss
 */
public class HttpDownloader extends IDownloaderPlugin implements Runnable {

    private static int BUFFER_SIZE = 8096; //缓冲区大小
    //ftp://tv1:tv1@xl.77ds.com:11203/%E8%BD%A9%E8%BE%95%E5%89%91%E4%B9%8B%E5%A4%A9%E4%B9%8B%E7%97%95[%E7%AC%AC2%E9%9B%86TV].rmvb
    private Boolean isRun = false;
    private String subBufDir;
    private Thread thObj = null;
    private long currentSize = 0;
    private long totalSize = 0;

    public HttpDownloader() {
        this.downloadType = "http";
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

    @Override
    public void open(String[] urlList, String encode) throws Exception {
        if (urlList != null && urlList.length > 0 && this.isValidUrl(urlList[0])) {
            this.urlList = new ArrayList<String>();
            this.bufferUrlList = new ArrayList<String>();
            this.urlList.add(urlList[0]);
            this.currentUrlIndex = 0;
            subBufDir = this.getBufferDir() == "" ? JAppToolKit.JRunHelper.getUserHomeDirPath() + "/.httpvideocaches" : this.getBufferDir();
            subBufDir = subBufDir + "/" + this.downloaderID;
            //清理子缓冲目录
            this.clearBufferDir();
            File ff = new File(subBufDir);
            ff.mkdirs();
            this.bufferUrlList.add(subBufDir + "/" + this.downloaderID + "_movie");
        } else {
            throw new Exception("This URL is not download!");
        }
    }

    public String getFileName() {
        String[] aaa = this.urlList.get(this.currentUrlIndex).split("/");
        return aaa[aaa.length - 1];
    }

    public String getShowName() {
        String[] aaa = this.urlList.get(this.currentUrlIndex).split("/");
        return aaa[aaa.length - 1].split("\\.")[0];
    }

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
            Logger.getLogger(HttpDownloader.class.getName()).log(Level.SEVERE, null, ex);
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
    public void start() throws Exception
    {
        isRun = true;
        currentSize = 0;
        thObj = new Thread(this);
        thObj.setDaemon(true);
        thObj.start();
        this.onReportStatus(DownloadStatus.startDowload,"");
    }

    @Override
    public void stop() throws Exception 
    {
        isRun = false;
        thObj.stop();
        this.onReportStatus(DownloadStatus.stopDownload,"");
    }

    @Override
    public long getTotalSize()
    {
        try {
            initVideoSize();
        } catch (Exception ex) {
            ex.printStackTrace();
            Logger.getLogger(HttpDownloader.class.getName()).log(Level.SEVERE, null, ex);            
        }
        return this.totalSize;
    }

    @Override
    public long getCurSize() {
        return this.currentSize;
    }

    @Override
    public int getDownloadProgress() {
        return (int) (((double)this.getCurSize() / (double)this.getTotalSize()) * 100);
    }

    @Override
    public int getDownloadSpeed() {
        return 100;
    }

    @Override
    public String getFileName(int index) {
        return this.getShowName();
    }

    @Override
    public IDownloaderPlugin getNewDownloader() {
        return new HttpDownloader();
    }

    /**
     * 初始化影片大小
     */
    private void initVideoSize() throws Exception {
        if (this.totalSize <= 0) {
            if (this.urlList.get(this.currentUrlIndex).trim().toLowerCase().startsWith("http")) {
                HttpURLConnection httpUrl = null;
                URL url = null;
                //建立链接
                url = new URL(this.urlList.get(this.currentUrlIndex).trim());
                httpUrl = (HttpURLConnection) url.openConnection();
                //连接指定的资源
                httpUrl.connect();
                this.totalSize = httpUrl.getContentLength();
                httpUrl.disconnect();
            } else if (this.urlList.get(this.currentUrlIndex).trim().toLowerCase().startsWith("ftp")) {
                String s = this.urlList.get(this.currentUrlIndex).trim().replace("ftp://", "");
                String[] team = s.split("/");
                String[] at = team[0].split("@");
                String[] userandpass = at[0].split(":");
                String[] hostandport = at[1].split(":");
                String remoteFile = s.replace(team[0] + "/", "");

                String host = hostandport[0];
                int port = Integer.parseInt(hostandport[1]);
                String user = userandpass[0];
                String pswd = userandpass[1];
                String pathFile = "";
                if (!remoteFile.equals(team[team.length - 1])) {
                    pathFile = remoteFile.replace(team[team.length - 1], "");
                }
                FtpClient ftp = new FtpClient();
                //设置服务器的地址 
                ftp.openServer(host, port);
                ftp.login(user, pswd);

                if (!pathFile.trim().equals("")) {
                    ftp.cd(pathFile);
                }
                //获取文件大小            
                this.totalSize = this.getFileSize(ftp, team[team.length - 1]);
                ftp.closeServer();

            } else {
                this.totalSize = 0;
            }
        }
    }

    /**
     * 将HTTP资源另存为文件
     *
     * @param destUrl String
     * @param fileName String
     * @throws Exception
     */
    public void httpSaveToFile(String destUrl, String fileName) throws IOException {
        FileOutputStream fos = null;
        BufferedInputStream bis = null;
        HttpURLConnection httpUrl = null;
        URL url = null;
        byte[] buf = new byte[BUFFER_SIZE];
        int size = 0;


//建立链接
        url = new URL(destUrl);
        httpUrl = (HttpURLConnection) url.openConnection();
//连接指定的资源
        httpUrl.connect();
        this.totalSize = httpUrl.getContentLength();
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
            this.onReportProgress(this.currentUrlIndex,currentSize);
        }

        fos.close();
        bis.close();
        httpUrl.disconnect();
    }

    /**
     * 获取FTP中文件大小
     *
     * @param client
     * @param fileName
     * @return
     * @throws IOException
     */
    public long getFileSize(FtpClient client, String fileName)
            throws IOException {
        long fileSize = -1;
        String s = "SIZE " + fileName + "\r\n";
        client.sendServer(s);
        try {
            int status = client.readServerResponse();
            if (status == 213) {
                String msg = client.getResponseString();
                fileSize = Long.parseLong(msg.substring(3).trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileSize;
    }

    /**
     * 将FTP资源另存为文件
     *
     * @param destUrl
     * @param fileName
     * @throws Exception
     */
    public void ftpSavetoFile(String destUrl, String fileName) throws Exception {
        String s = destUrl.replace("ftp://", "");
        String[] team = s.split("/");
        String[] at = team[0].split("@");
        String[] userandpass = at[0].split(":");
        String[] hostandport = at[1].split(":");
        String remoteFile = s.replace(team[0] + "/", "");

        String host = hostandport[0];
        int port = Integer.parseInt(hostandport[1]);
        String user = userandpass[0];
        String pswd = userandpass[1];
        String pathFile = "";
        if (!remoteFile.equals(team[team.length - 1])) {
            pathFile = remoteFile.replace(team[team.length - 1], "");
        }
        String downFile = fileName;
        TelnetInputStream ftpIn = null;
        FileOutputStream ftpOut = null;

        FtpClient ftp = new FtpClient();
        try {
            //设置服务器的地址 
            ftp.openServer(host, port);
            ftp.login(user, pswd);

            if (!pathFile.trim().equals("")) {
                ftp.cd(pathFile);
            }

            //获取文件大小            
            this.totalSize = this.getFileSize(ftp, team[team.length - 1]);

            //设置ftp服务器上文件的传输模式
            ftp.binary();

            //数据下载       
            ftpIn = ftp.get(team[team.length - 1]);            //fileName为FTP服务器上要下载的文件名
            byte[] buf = new byte[204800];
            int bufsize = 0;
            ftpOut = new FileOutputStream(fileName);              //存放在本地硬盘的物理位置
            while ((bufsize = ftpIn.read(buf, 0, buf.length)) != -1) {
                currentSize += bufsize;
                ftpOut.write(buf, 0, bufsize);
                this.onReportProgress(this.currentUrlIndex,currentSize);

            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (ftpIn != null) {
                ftpIn.close();
            }
            if (ftpOut != null) {
                ftpOut.close();
            }
            if (ftp != null) {
                ftp.closeServer();
            }
        }

    }

    @Override
    public void run() {
        try {
            if (this.urlList.get(this.currentUrlIndex).trim().toLowerCase().startsWith("http")) {
                httpSaveToFile(this.urlList.get(this.currentUrlIndex), this.bufferUrlList.get(this.currentUrlIndex));
            } else if (this.urlList.get(this.currentUrlIndex).trim().toLowerCase().startsWith("ftp")) {
                ftpSavetoFile(this.urlList.get(this.currentUrlIndex), this.bufferUrlList.get(this.currentUrlIndex));
            }
            this.onReportFinish();
            this.isRun = false;
        } catch (Exception ex) {
            this.onReportError(ex.toString());
        }
    }

    @Override
    public String getBufferFileUrl(int index) {
        return this.bufferUrlList.get(this.currentUrlIndex);
    }
}