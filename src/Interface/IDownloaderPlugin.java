/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Interface;

import java.util.ArrayList;

/**
 * 视频下载器
 * @author wcss
 */
public abstract class IDownloaderPlugin 
{
    
    /**
     * 下载器ID
     */
    public String downloaderID;
    
    /**
     * 工作事件
     */
    public IDownloaderEvent workEvent; 
    
    /**
     * 地址列表
     */
    public ArrayList<String> urlList;
    
    /**
     * 当前地址序号
     */
    public int currentUrlIndex;
    
    /*
     * 字符串编码
     */
    public String encode;
    
    /**
     * 下载器类型
     */
    public String downloadType;
    
    /**
     * 下载器临时数据
     */
    public Object tag;
    
    /**
     * 当前缓存文件路径
     */
    protected ArrayList<String> bufferUrlList;
    
    /**
     * 缓存目录
     */
    protected String bufferDir;
    
    /**
     * 设置缓存目录
     * @param bufferdir 
     */
    public void setBufferDir(String bufferdir)
    {
        this.bufferDir = bufferdir;
    }
    
    /**
     * 设置最大下载速度
     */
    public abstract void setMaxDownloadSpeed(int speed);
    
    /**
     * 设置最大上传速度
     */
    public abstract void setMaxUploadSpeed(int speed);
    
    /**
     * 清理缓存目录
     */
    public abstract void clearBufferDir();
    
    /**
     * 获取缓存目录
     * @return 
     */
    public String getBufferDir()
    {
        return this.bufferDir;
    }
    
    /**
     * 获取最大下载速度
     * @return 
     */
    public abstract int getMaxDownloadSpeed();
    
    /**
     * 获取最大上传速度
     * @return 
     */
    public abstract int getMaxUploadSpeed();
    
    /**
     * 是否在运行之中
     * @return 
     */
    public abstract Boolean isRunning();
    
    /**
     * 是否为有效的地址
     * @return 
     */
    public abstract Boolean isValidUrl(String url);
    
    /**
     * 打开连接
     * @param videourl 
     * @param downloadtype
     */
    public abstract void open(String[] urlList,String encode) throws Exception;
    
    /**
     * 开始下载
     */
    public abstract void start() throws Exception;
    
    /**
     * 停止下载
     */
    public abstract void stop() throws Exception;
    
    /**
     * 获取数据原始大小
     * @return 
     */
    public abstract long getTotalSize();
    
    /**
     * 获取已下载大小
     * @return 
     */
    public abstract long getCurSize();
    
    /**
     * 获取下载进度
     * @return 
     */
    public abstract int getDownloadProgress();
    
    /**
     * 获取缓存文件路径
     * @return 
     */
    public abstract String getBufferFileUrl(int fileIndex);
    
    /**
     * 获取下载速度
     * @return 
     */
    public abstract int getDownloadSpeed();
    
    /**
     * 获取文件名称
     * @return 
     */
    public abstract String getFileName(int fileIndex);
    
    /**
     * 获取一个相同类型的新下载器
     * @return 
     */
    public abstract IDownloaderPlugin getNewDownloader();
    
    /**
     *  下载错误
     * @param error 
     */
    protected void onReportError(String error)
    {
        if (this.workEvent != null)
        {
            this.workEvent.onReportStatus(this,DownloadStatus.downloadError,error);
        }
    }
    
        /**
     * 
     * @param error 
     */
    protected void onReportStatus(int stateCode,String msg)
    {
        if (this.workEvent != null)
        {
            this.workEvent.onReportStatus(this, stateCode,msg);
        }
    }
    
    /**
     * 
     * @param current 
     */
    protected void onReportProgress(int fileIndex,long current)
    {
        if (this.workEvent != null)
        {
            this.workEvent.onReportProgress(this, fileIndex,current, this.getTotalSize());
        }
    }
    
    /**
     * 
     */
    protected void onReportFinish()
    {
       onReportStatus(DownloadStatus.finishDownload,"");
    } 
}