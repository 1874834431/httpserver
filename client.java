package com.hhh;

import java.io.*;
import java.net.*;
import java.util.Date;
import java.util.*;
import java.text.SimpleDateFormat;
public class client {
    public static String filesname;
    private static clientThread[] threads;
    public static void main(String[] args) {

        try {
            String IP=args[1];
            String servname=args[3];
            System.out.println(IP);
            int port =Integer.parseInt(args[5]);
            int maxth =Integer.parseInt(args[7]);
            clientglobaldata.alltime=Integer.parseInt(args[args.length-1]);
            filesname=args[9];
            threads = new clientThread[maxth];
            for (int i = 0; i < threads.length; i++) {
                threads[i] = new clientThread(IP, port ,filesname,servname);
                threads[i].start();
            }
            for (int i = 0; i < threads.length; i++) {
                threads[i].join();
            }
            System.out.println("send over");
            //    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(System.out));
            //    bw.write("the average of wait time:%f",globaldata.waittime/globaldata.files);
            System.out.println(clientglobaldata.bytes);
            System.out.println(clientglobaldata.files);
            System.out.println(clientglobaldata.waittime);
            System.out.printf("total transaction throughput(number per second):%f\n",(double)(clientglobaldata.files*1000/clientglobaldata.alltime));
            System.out.printf("data rate throughput(bytes per second): %f\n",(double)clientglobaldata.bytes*1000/clientglobaldata.alltime);
            System.out.printf("the average of wait time(ms):%f\n",(double)clientglobaldata.waittime/clientglobaldata.files);
        } catch (Exception e) {

        }

    }

}
class clientThread extends Thread {
    private String IP;
    private int port;
    private String filename;
    private String WWW_ROOT;
    private static String modifitime;
    public clientThread (String IP, int port ,String filename,String servname)  throws Exception
    {
        System.out.println("Thread start");
        this.IP=IP;
        this.port=port;
        this.filename=filename;
        this.WWW_ROOT=servname;
        Date date=new Date();
        long datel=date.getTime()-1000000000;
        Date d4 = new Date(datel);
        SimpleDateFormat sdf = new SimpleDateFormat("EEE dd MMM yyyy HH:mm:ss z",Locale.US);
        modifitime = sdf.format(d4);
    }
    public void run() {

        try {
            BufferedReader testFile = new BufferedReader(new FileReader(filename));
            String file;
            while ((file=testFile.readLine())!=null)
            {
                try {
                    long starttime=System.currentTimeMillis();
                    // ??????????????????socket????????????????????????????????????
                    Socket sc = new Socket(IP,port);
                    sc.setSoTimeout(3000);

                    // ?????????????????????????????????socket???????????????
                    OutputStream out = sc.getOutputStream();
                    String agent="User-Agent: ios";
                    String httpmess="GET "+file+" HTTP/1.0\nHost: "+WWW_ROOT+"\n"+"If-Modified-Since: "+modifitime+'\n'+agent+"\nCRLF";
                    // write??????????????????
                    System.out.println(httpmess);
                    out.write(httpmess.getBytes());
                    BufferedReader    inFromserver =
                            new BufferedReader(new InputStreamReader(sc.getInputStream()));
                    String line = inFromserver.readLine();
                    clientglobaldata.waittime+=System.currentTimeMillis()-starttime;
                    clientglobaldata.bytes+=line.getBytes().length;
                    clientglobaldata.files++;
                    while ( !line.equals("") ) {
                        System.out.println(line);
                        line = inFromserver.readLine();
                    }

                    sc.close();
                }
                catch(Exception e)
                {
                    System.out.println("can't send request");
                }
            }
        }
        catch (IOException e)
        {
            System.out.println("file error");
        }


    }

}
class clientglobaldata{
    public static long alltime;
    public static long files;
    public static long bytes;
    public static long waittime;
}
// java client.java -server 192.168.129.1 -n server1 -p 6789 -m 2 -f file1.html file2.html file3.html file4.html -t 100