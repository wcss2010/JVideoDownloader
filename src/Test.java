/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author wcss
 */
public class Test 
{
    public static void mains(String[] args)
    {
        String[]  aaa = "ftp://tv1:tv1@xl.77ds.com:11203/%E8%BD%A9%E8%BE%95%E5%89%91%E4%B9%8B%E5%A4%A9%E4%B9%8B%E7%97%95[%E7%AC%AC2%E9%9B%86TV].rmvb".split("/");
        System.out.println(aaa[aaa.length - 1].split("\\.")[0]);
    }
    
}
