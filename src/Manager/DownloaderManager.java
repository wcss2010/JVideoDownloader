/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Manager;

import Downloader.HTTPURLListDownloader;
//import Downloader.HttpDownloader;
import Interface.IDownloaderPlugin;
import Interface.IDownloaderEvent;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.*;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * 下载管理器
 *
 * @author wcss
 */
public class DownloaderManager 
{
    //public static final String httpAndFTP_OnlyOne = "http";
    public static final String httpUrlList = "httplist";
    /**
     * 构造函数
     */
    public DownloaderManager()
    {
//       downloaderTypes.put("http", new HttpDownloader());
       downloaderTypes.put("httplist", new HTTPURLListDownloader());
    }

    /**
     * 静态引用
     */
    public static DownloaderManager manager = new DownloaderManager();
    /**
     * 下载器类型列表
     */
    public Hashtable<String, IDownloaderPlugin> downloaderTypes = new Hashtable<String, IDownloaderPlugin>();
    /**
     * 正在运行的下载器
     */
    public Hashtable<String, IDownloaderPlugin> downloaders = new Hashtable<String, IDownloaderPlugin>();

    /**
     * 创建下载器实例
     *
     */
    public IDownloaderPlugin createDownloader(String name, String[] urlList,String encode, String downloadtype,String bufferDir,int downloadSpeed,int uploadSpeed,IDownloaderEvent event) throws Exception {
        if (downloaderTypes.containsKey(downloadtype)) {
            IDownloaderPlugin vd = downloaderTypes.get(downloadtype).getNewDownloader();
            if (vd != null) {

                if (downloaders.containsKey(name)) {
                    throw new Exception("Downloader Name Is Added!");
                } else {
                    vd.downloaderID = name;
                    vd.workEvent = event;
                    //设置缓存目录
                    vd.setBufferDir(bufferDir);
                    //设置最大下载速度
                    vd.setMaxDownloadSpeed(downloadSpeed);
                    //设置最大上传速度
                    vd.setMaxUploadSpeed(uploadSpeed);
                    //准备下载
                    vd.open(urlList,encode);
                    this.downloaders.put(name, vd);
                    return vd;
                }
            } else {
                throw new Exception("Downloader Init Error!");
            }
        } else {
            throw new Exception("Downloader Type Not Found!");
        }        
    }

    /**
     * 停止下载
     * @param name 
     */
    public void stopDownloader(String name) throws Exception
    {
        if (downloaders.containsKey(name))
        {
            downloaders.get(name).stop();
        }else
        {
            throw new Exception("not found!");
        }
    }
    
    /**
     * 停止所有下载
     */
    public void stopAllDownloader() throws Exception
    {
        for (Iterator it = downloaders.keySet().iterator(); it.hasNext();) {
            //从ht中取  
            String key = (String) it.next();
            IDownloaderPlugin value = downloaders.get(key);
            value.stop();

        }
    }
    
    /**
     * 开始下载
     * @param name 
     */
    public void startDownloader(String name) throws Exception
    {
        if (downloaders.containsKey(name))
        {
            downloaders.get(name).start();
        }else
        {
            throw new Exception("not found!");
        }
    }
    
    /**
     * 清空下载缓存
     */
    public void clearAllDownloaderBufferDir()
    {
        for (Iterator it = downloaders.keySet().iterator(); it.hasNext();) {
            //从ht中取  
            String key = (String) it.next();
            IDownloaderPlugin value = downloaders.get(key);
            value.clearBufferDir();

        }
    }
    
    /**
     * 清理指定缓存
     * @param name
     * @throws Exception 
     */
    public void clearDownloaderBufferDir(String name) throws Exception
    {
        if (downloaders.containsKey(name))
        {
            downloaders.get(name).clearBufferDir();
        }else
        {
            throw new Exception("not found!");
        }
    }
    
    /**
     * 文件拷贝。
     *
     * @param from 源路径。
     * @param to 目标路径。
     * @exception IOException Description of the Exception0D
     */
    public static void copyFile(String from, String to)
            throws IOException {
        int BUFF_SIZE = 100000;
        byte[] buffer = new byte[BUFF_SIZE];
        InputStream in = null;
        OutputStream out = null;

        try {
            in = new FileInputStream(from);
            out = new FileOutputStream(to);
            while (true) {
                synchronized (buffer) {
                    int amountRead = in.read(buffer);

                    if (amountRead == -1) {
                        break;
                    }

                    out.write(buffer, 0, amountRead);
                }
            }
        } finally {
            if (in != null) {
                in.close();
            }

            if (out != null) {
                out.close();
            }
        }
    }

    /**
     * 获取目录使用大小
     * @param dirurl
     * @return 
     */
    public static long getDirUseSize(String dirurl) {
        try {
            long result = 0;
            Process p = JAppToolKit.JRunHelper.runSysCmd("du -c " + dirurl, false);
            InputStream is = p.getInputStream();
            p.waitFor();
            String[] team = JAppToolKit.JDataHelper.readFromInputStream(is);
            if (team.length >= 1) {
                String item = team[0];
                String[] content = item.split("\\s");
                result = Long.parseLong(content[0].trim());
            }
            is.close();
            p.destroy();
            return result * 1024;
        } catch (Exception ex) {
            ex.printStackTrace();
            return 0;
        }
    }

    /**
     * 获取系统剪切板中的文本（相当于粘贴）
     *
     * @return 系统剪切板中的文本
     */
    public static String getSysClipboardText() {
        String ret = "";
        Clipboard sysClip = Toolkit.getDefaultToolkit().getSystemClipboard();
        // 获取剪切板中的内容
        Transferable clipTf = sysClip.getContents(null);

        if (clipTf != null) {
            // 检查内容是否是文本类型
            if (clipTf.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                try {
                    ret = (String) clipTf.getTransferData(DataFlavor.stringFlavor);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return ret;
    }
    
    /**
     * 读取所有字符串从文件中
     *
     * @param file
     * @return
     * @throws Exception
     */
    public static String readAllTextFromFile(File file, String encoding) throws Exception {
        if (file != null && encoding != null && file.exists()) {
            long filelength = file.length();
            byte[] filecontent = new byte[Integer.parseInt(filelength + "")];
            try {
                FileInputStream in = new FileInputStream(file);
                in.read(filecontent);
                in.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                return new String(filecontent, encoding);
            } catch (UnsupportedEncodingException e) {
                System.err.println("The OS does not support " + encoding);
                e.printStackTrace();
                return "";
            }
        } else {
            return "";
        }
    }
}