package com.hhh;

import java.io.*;
import java.net.*;
import java.util.Date;
import java.util.*;
import java.text.SimpleDateFormat;
public class client {
    public static void main(String[] args) {

        long basetime = System.currentTimeMillis();
        try {

            String IP=args[1];
            String servname=args[3];
            System.out.println(IP);
            int port =Integer.parseInt(args[5]);
            int maxth =Integer.parseInt(args[7]);
            globaldata.alltime=Integer.parseInt(args[args.length-1]);
            int i=9;
            while(true)//for (int i=9;i<=args.length-3;i++)
            {
                if(System.currentTimeMillis()-basetime>=globaldata.alltime) break;
                while(Thread.activeCount()>=maxth) Thread.currentThread().sleep(1);
                String filename=args[i];
                System.out.println(filename);
                SendRequest sr=new SendRequest(IP, port ,servname, filename);
                Thread t=new Thread(sr);
                t.start();
                i++;
                if(i>args.length-3)  i=9;
            }
            Thread.currentThread().sleep(500);
            //    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(System.out));
            //    bw.write("the average of wait time:%f",globaldata.waittime/globaldata.files);
            System.out.println(globaldata.bytes);
            System.out.println(globaldata.files);
            System.out.println(globaldata.waittime);
            System.out.printf("total transaction throughput(number per second):%f\n",(double)(globaldata.files*1000/globaldata.alltime));
            System.out.printf("data rate throughput(bytes per second): %f\n",(double)globaldata.bytes*1000/globaldata.alltime);
            System.out.printf("the average of wait time(ms):%f\n",(double)globaldata.waittime/globaldata.files);
        } catch (Exception e) {

        }

    }

}
class SendRequest implements Runnable {
    private String IP;
    private int port;
    private String filename;
    private String WWW_ROOT;
    public SendRequest (String IP, int port ,String WWW_ROOT,String filename)  throws Exception
    {
        this.IP=IP;
        this.port=port;
        this.filename=filename;
        this.WWW_ROOT=WWW_ROOT;

    }
    public void run() {

        try {
            long starttime=System.currentTimeMillis();
            // 创建客户端的socket服务，指定目的主机和端口
            Socket sc = new Socket(IP,port);
            Date date=new Date();
            long datel=date.getTime()-1000000000;
            Date d4 = new Date(datel);
            SimpleDateFormat sdf = new SimpleDateFormat("EEE dd MMM yyyy HH:mm:ss z",Locale.US);
            String str = sdf.format(d4);
            System.out.println(str);
            // 为了发送数据，应该获取socket中的输出流
            OutputStream out = sc.getOutputStream();
            String agent="User-Agent: ios";
            String httpmess="GET "+filename+" HTTP/1.0\nHost: "+WWW_ROOT+"\n"+"If-Modified-Since: "+str+'\n'+agent+"\nCRLF";
            // write接收字节数据
            System.out.println(httpmess);
            out.write(httpmess.getBytes());
            BufferedReader    inFromserver =
                    new BufferedReader(new InputStreamReader(sc.getInputStream()));
            String line = inFromserver.readLine();
            globaldata.waittime+=System.currentTimeMillis()-starttime;
            globaldata.bytes+=line.getBytes().length;
            globaldata.files++;
            while ( !line.equals("") ) {

                System.out.println(line);
                line = inFromserver.readLine();
            }


            sc.close();
        }
        catch(Exception e)
        {

        }
    }

}
class globaldata{
    public static long alltime;
    public static long files;
    public static long bytes;
    public static long waittime;
}
// java client.java -server 192.168.129.1 -n server1 -p 4603 -m 2 -f file1.html file2.html file3.html file4.html -t 100