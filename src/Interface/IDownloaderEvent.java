/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Interface;

/**
 * 下载进度事件
 *
 * @author wcss
 */
public interface IDownloaderEvent 
{
    /**
     * 报告下载进度
     * @param sender
     * @param currentlength
     * @param totallength 
     */
    void onReportProgress(IDownloaderPlugin sender, int fileIndex,long currentLength, long totalLength);

    /**
     * 报告下载状态
     * @param sender 
     */
    void onReportStatus(IDownloaderPlugin sender,int stateCode,String msg);
}
