
package com.hhh;

import java.io.*;
import java.util.*;
import java.net.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class server {
    private static ServerSocket welcomeSocket;
    private ServiceThread[] threads;
    private static int implemethod = 1;
    private List<Socket> connSockPool;
    private static int serverPort;

    public server(int serverPort) {
        try {
            // create server socket

            welcomeSocket = new ServerSocket(serverPort);
            System.out.println("Server started; listening at " + serverPort);
            threads = new ServiceThread[Integer.parseInt(globaldata.config.get("ThreadPoolSize"))];
            if (implemethod == 0) {
                // share welcomeSocket
                for (int i = 0; i < threads.length; i++) {
                    threads[i] = new ServiceThread(welcomeSocket);
                    threads[i].start();
                }
            } else {
                // Share pool
                connSockPool = new Vector<Socket>();
                for (int i = 0; i < threads.length; i++) {
                    threads[i] = new ServiceThread(connSockPool);
                    threads[i].start();
                }
            }

        } catch (Exception e) {
            System.out.println("Server construction failed.");
        } // end of catch
    }

    public static void main(String[] args) throws Exception {


        // see if we do not use default server port
        configserver(args[1]);
        // create server socket
        serverPort = Integer.parseInt(globaldata.config.get("Listen"));
        server server = new server(serverPort);
        server.run();
    } // end of main()

    public static void configserver(String filename) {
        String document;

        BufferedReader reader;
        String[] spline;
        try {
            reader = new BufferedReader(new FileReader(filename));
            String line = reader.readLine();
            spline = line.split("\\s+");
            globaldata.config.put("Listen", spline[1]);
            line = reader.readLine();
            spline = line.split("\\s+");
            globaldata.cache_size = Integer.parseInt(spline[1]);
            line = reader.readLine();
            spline = line.split("\\s+");
            globaldata.config.put("ThreadPoolSize", spline[1]);
            while (line != null) {
                spline = line.split("\s+");
                if (spline.length == 3) {
                    document = spline[2];
                    line = reader.readLine();
                    spline = line.split("\\s+");
                    globaldata.config.put(spline[2], document);
                }
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {

        try {
            if (implemethod == 0) {
                for (int i = 0; i < threads.length; i++) {
                    threads[i].join();
                }
                System.out.println("All threads finished. Exit");
            } else {
                Socket connSock = welcomeSocket.accept();
                System.out.println("Main thread retrieve connection from "
                        + connSock);

                // how to assign to an idle thread?
                synchronized (connSockPool) {
                    connSockPool.add(connSock);
                } // end of sync
            }
        } catch (Exception e) {
            System.out.println("Join errors");
        } // end of catch

    } // end of class TCPServer
}

class ServiceThread extends Thread {

    static boolean _DEBUG = true;
    static int reqCount = 0;

    String WWW_ROOT;
    Socket connSocket;
    private List<Socket> pool;
    BufferedReader inFromClient;
    DataOutputStream outToClient;
    ServerSocket welcomeSocket;
    String urlName;
    String fileName;
    File fileInfo;
    static int implpeMethod;

    public ServiceThread(ServerSocket welcomeSocket) throws Exception {
        this.welcomeSocket = welcomeSocket;
        implpeMethod = 0;
    }

    public ServiceThread(List<Socket> pool) throws Exception {
        this.pool = pool;
        implpeMethod = 1;
    }

    public void run() {
        System.out.println("Thread " + this + " started.");

        while (true) {
            // get a new request connection
            Socket s = null;
            while (s == null) {
                if (implpeMethod == 0) {
                    synchronized (welcomeSocket) {
                        try {
                            s = welcomeSocket.accept();
                            System.out.println("Thread " + this
                                    + " process request " + s);
                        } catch (IOException e) {
                        }
                    }
                }
               else {
                        synchronized (pool) {
                            if (pool!=null&& !pool.isEmpty()) {
                                // remove the first request
                                s = (Socket) pool.remove(0);
                                System.out.println("Thread " + this
                                        + " process request " + s);
                            }
                        } // end of sync
                    }
                }
            try {
                serveARequest(s);
                s.close();
            } catch (Exception e) {

                System.out.println("can't server this Request");
            }
            } // end while
        }
        public void serveARequest (Socket connSocket) throws Exception {
            this.reqCount++;
            this.connSocket = connSocket;
//        this.connSocket.setSoTimeout(3000);

            this.inFromClient =
                    new BufferedReader(new InputStreamReader(connSocket.getInputStream()));

            this.outToClient =
                    new DataOutputStream(connSocket.getOutputStream());

            try {
                // create read stream to get input
                while (true) {
                    try {
                        processRequest();
                    } catch (Exception e) {
                        System.out.println("????????????");
                    }
                    connSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        public void processRequest ()
        {
            try {
                mapURL2File();
                if (fileInfo != null) // found the file and knows its info
                {
                    outputResponseHeader();
                    outputResponseBody();
                } // dod not handle error
                connSocket.close();

            } catch (Exception e) {
                outputError(400, "Server error");
            }
        } // end of processARequest

        private void mapURL2File () throws Exception
        {
            String requestMessageLine = inFromClient.readLine();
            DEBUG("Request " + reqCount + ": " + requestMessageLine);
            // process the request
            String[] request = requestMessageLine.split("\\s");
            if (request.length < 2 || !request[0].equals("GET")) {
                outputError(500, "Bad request");
                return;
            }
            String hostline = inFromClient.readLine();
            String[] host = hostline.split("\\s+");
            if (host.length <= 1) ;
            else {
                WWW_ROOT = globaldata.config.get(host[1]);
            }
            // parse URL to retrieve file name
            urlName = request[1];

//        if(urlName.equals("/load"))
//        {
//            if(cursocket>=50)
//            {
//                outputError(503,"overloading...");
//            }
//            else {
//                outToClient.writeBytes("HTTP/1.0 200 \r\n");
//            }
//            return;
//        }
            if (urlName.startsWith("/") == true)
                urlName = urlName.substring(1);

            int i = 0;
            String modate;
            String line;

            while (true) {
                i++;
                if (i == 3) break;
                line = inFromClient.readLine();
                if (line.contains("If-Modified-Since")) {
                    modate = line.split(": ")[1].trim();
                    SimpleDateFormat format = new SimpleDateFormat("EEE dd MMM yyyy HH:mm:ss z", Locale.US);
                    globaldata.date = format.parse(modate);
                    //   System.out.println(globaldata.date);
                } else if (line.contains("User-Agent"))
                    globaldata.useragent = line.split(":")[1].trim();
                // System.out.println(line);
            }
            if (urlName.endsWith("/") == true) {
                if (globaldata.useragent.contains("iPhone") | globaldata.useragent.contains("ios") | globaldata.useragent.contains("Android")) {
                    if (new File(WWW_ROOT + "index_m.html").isFile()) {
                        urlName = "index_m.html";
                    } else {
                        urlName = "index.html";
                    }

                }
            }

            // map to file name
            fileName = WWW_ROOT + urlName;
            DEBUG("Map to File name: " + fileName);
            System.out.println(fileName);
            if (globaldata.cache.containsKey(fileName)) {
                System.out.println("in cache ");
                fileInfo = globaldata.cache.get(fileName);
            } else {
                fileInfo = new File(fileName);
                if (!fileInfo.isFile()) {
                    outputError(404, "Not Found");
                    fileInfo = null;

                } else if (fileInfo.length() < globaldata.cache_size * 1024) {
                    System.out.println("put in cache");
                    globaldata.cache.put(fileName, fileInfo);
                    globaldata.cache_size -= (fileInfo.length() / 1024) + 1;
                }
            }

            if (globaldata.date != null && globaldata.date.after(new Date(fileInfo.lastModified()))) {
                DEBUG("304");
                System.out.println("Not Modified\r\n");
                outputError(304, "Not Modified\r\n");

            }

//        fileInfo = new File(fileName);
//        if ( !fileInfo.isFile() )
//        {
//            outputError(404,  "Not Found");
//            fileInfo = null;
//
//        }


        } // end mapURL2file


        private void outputResponseHeader () throws Exception
        {
            outToClient.writeBytes("HTTP/1.0 200 Document Follows\r\n");
            outToClient.writeBytes("Set-Cookie: MyCool433Seq12345\r\n");
            if (urlName.endsWith(".jpg"))
                outToClient.writeBytes("Content-Type: image/jpeg\r\n");
            else if (urlName.endsWith(".gif"))
                outToClient.writeBytes("Content-Type: image/gif\r\n");
            else if (urlName.endsWith(".html") || urlName.endsWith(".htm"))
                outToClient.writeBytes("Content-Type: text/html\r\n");
            else {
                outToClient.writeBytes("Content-Type: text/plain\r\n");
            }
        }

        private void outputResponseBody () throws Exception
        {
            ArrayList<String> cgiFiles = new ArrayList<String>(Arrays.asList("py", "exe", "java", "bat", "jar"));
            String[] tmp = fileInfo.getName().split("\\.");
            String suffix = tmp[tmp.length - 1].trim();
            InetAddress loIP = InetAddress.getByName("localhost");
            if (fileInfo != null) {

                if (cgiFiles.contains(suffix)) {
                    var processBuilder = new ProcessBuilder();
                    var env = processBuilder.environment();
                    env.put("QUERY_STRING", "");
                    env.put("REMOTE_ADDR", loIP.getHostAddress());
                    env.put("REQUEST_METHOD", "GET");
                    env.put("SERVER_NAME", "server1");
                    env.put("SERVER_PROTOCOL", "HTTP/1.0");
                    env.put("SERVER_PORT", String.valueOf(4603));

                    switch (suffix) {
                        case "jar":
                        case "bat":
                        case "exe":
                            processBuilder.command(fileInfo.getName());
                            break;
                        case "py": {
                            processBuilder.command("python", fileInfo.getName());
                            break;
                        }
                        case "java":
                            processBuilder.command("java", fileInfo.getName());
                            break;
                    }
                    var process = processBuilder.start();
                    try (var reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()))) {
                        String outline = new String();
//                    System.out.println(outline);
                        while ((outline = reader.readLine()) != null) {
                            outToClient.writeBytes(outline);
                        }
                    }
                } else {
                    int numOfBytes = (int) fileInfo.length();
                    outToClient.writeBytes("Content-Length: " + numOfBytes + "\r\n");

                    // send file content
                    FileInputStream fileStream = new FileInputStream(fileName);

                    byte[] fileInBytes = new byte[numOfBytes];
                    fileStream.read(fileInBytes);
                    outToClient.write(fileInBytes, 0, numOfBytes);
                    fileStream.close();
                }
            }
        }

        void outputError ( int errCode, String errMsg)
        {
            try {
                outToClient.writeBytes("HTTP/1.0 " + errCode + " " + errMsg + "\r\n");
            } catch (Exception e) {
            }
        }

        static void DEBUG (String s)
        {
            if (_DEBUG)
                System.out.println(s);
        }
    }

    class globaldata {
        public static HashMap<String, String> config = new HashMap<String, String>();
        ;
        public static String useragent;
        public static Date date = null;
        public static Map<String, File> cache = new HashMap<>();
        public static long cache_size;
    }
// java server.java -con httpd.conf