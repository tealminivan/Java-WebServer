import java.io.*;
import java.net.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;


public class WebServer implements Runnable {

    static int PORT;
    // root location
    static File ROOT_LOC = new File(".");
    // default file if no file is given
    static final String INDEX_FILE = "index.html";

    // variable defined in WebServer object
    Socket mySocket;

    // constructor for WebServer object
    public WebServer(Socket connect) {
        this.mySocket = connect;
    }

    // function to handle GET request
    private void getRequest(File file, int fileLength, PrintWriter clientOut, BufferedOutputStream binaryOut) throws IOException {
        FileInputStream fileInput = null;
        // stores file data
        byte[] fileData = new byte[fileLength];

        try {
            // open input stream
            fileInput = new FileInputStream(file);
            // read file data
            fileInput.read(fileData);
        }

        finally {
            // close file input stream
            close(fileInput);
        }

        // headers when GET command is called
        clientOut.println();
        clientOut.println();
        clientOut.println("HTTP/1.1 200 OK");
        clientOut.println("Server: HTTP/1.1 server");
        clientOut.println("Date: " + new Date());
        clientOut.println("Last-Modified: " + new Date(file.lastModified()));
        clientOut.println("Content-length: " + file.length());
        clientOut.println();
        clientOut.flush();

        // write file
        binaryOut.write(fileData,0,fileLength);
        binaryOut.flush();
    }

    // function to return 404 error if file is not found
    private void NotFound(PrintWriter out, String file) {

        // headers when 404 Error is returned
        out.println("HTTP/1.1 404 Not Found");
        out.println("Server: HTTP/1.1 server");
        out.println("Date: " + new Date());
        out.println();
        // show html in browser
        out.println("<HTML>");
        out.println("<HEAD><TITLE>File Not Found</TITLE>" + "</HEAD>");
        out.println("<BODY>");
        out.println("<H2>404 Not Found: " + file + "</H2>");
        out.println("</BODY>");
        out.println("</HTML>");
        out.flush();
    }

    // close function for object stream s
    public void close(Object s) {
        if (s == null)
            return;

        // close depending on the type of object
        try {
            if (s instanceof Socket) {
                ((Socket)s).close();
            }
            if (s instanceof Reader) {
                ((Reader)s).close();
            }
            else if (s instanceof Writer) {
                ((Writer)s).close();
            }
            else if (s instanceof InputStream) {
                ((InputStream)s).close();
            }
            else if (s instanceof OutputStream) {
                ((OutputStream)s).close();
            }
        }
        catch (Exception e) {
            System.err.println("Error closing stream: " + e);
        }
    }


    public void run() {
        // variable to get name of file
        String fileRequest = null;
        // variable that reads data from client
        BufferedReader input = null;
        // variable to get output stream in characters from client
        PrintWriter clientOut = null;
        // variable to get binary output stream
        BufferedOutputStream binaryOut = null;
        // variable to hold list of headers
        List<String> request_Header = new ArrayList<>();
        // request variable
        String request = null;

        // variables to handle the ifModifiedSince
        Boolean ifModifiedSince = false;
        Date convertedDate = null;
        long millis = 0;

        try {
            // retrieve character input stream from client
            input = new BufferedReader(new InputStreamReader(mySocket.getInputStream()));
            // retrieve character output stream to client
            clientOut = new PrintWriter(mySocket.getOutputStream());
            // retrieve binary output stream to client
            binaryOut = new BufferedOutputStream(mySocket.getOutputStream());


            // while loop to put all the headers in a list
            while ((request = input.readLine()) != null && request.length() >0){
                request_Header.add(request);
            }

            // get the command
            String command = request_Header.get(0);

            // check to see if if-modified-since is a header in the request
            for(String listItem : request_Header){
                if (listItem.contains("If-Modified-Since:")) {
                    ifModifiedSince = true;
                    String parseDate = listItem.replace("If-Modified-Since: ","");
                    SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
                    try {
                        convertedDate = dateFormat.parse(parseDate);
                    } catch (ParseException e) {
                        // headers when error 400 is returned
                        clientOut.println("HTTP/1.1 400 Bad Request");
                        clientOut.println("Server: HTTP/1.1 server");
                        clientOut.println("Date: " + new Date());
                        clientOut.println();
                        // html to show up in browser
                        clientOut.println("<HTML>");
                        clientOut.println("<HEAD><TITLE>Bad Request</TITLE>" + "</HEAD>");
                        clientOut.println("<BODY>");
                        clientOut.println("<H2>Bad Request");
                        clientOut.println("</BODY></HTML>");
                        clientOut.flush();
                        return;
                    }
                    // convert date to long so we can compare it
                    millis = convertedDate.getTime();
                    break;
                }
            }

            // print variables and requests for testing
            String[] commandGet = command.split(" ");
            System.out.println("Test (requests): "+request_Header);
            System.out.println("Test (variables): "+"Command:"+commandGet[0]+", Path:"+commandGet[1]+", If-Modified-Since:"+convertedDate);
            System.out.println("\n");

            // parse request
            StringTokenizer parse = new StringTokenizer(command);
            // parse out method
            String method = parse.nextToken().toUpperCase();
            // parse out file requested
            fileRequest = parse.nextToken();
            // parse out ifModifiedSince date

            // condition to see if commands are not GET or HEAD
            if (!method.equals("HEAD") && !method.equals("GET")) {
                // headers when error 501 is returned
                clientOut.println("HTTP/1.1 501 Not Implemented");
                clientOut.println("Server: HTTP/1.1 server");
                clientOut.println("Date: " + new Date());
                clientOut.println();
                // html to show up in the browser
                clientOut.println("<HTML>");
                clientOut.println("<HEAD><TITLE>Not Implemented</TITLE>" + "</HEAD>");
                clientOut.println("<BODY>");
                clientOut.println("<H2>501 Not Implemented: " + method + " method.</H2>");
                clientOut.println("</BODY></HTML>");
                clientOut.flush();
                return;
            }

            // conditions to determine if second command argument is a file or directory
            if (ROOT_LOC.isFile()){
                String[] separated = ROOT_LOC.toString().split("/");
                fileRequest = separated[separated.length-1];
                ROOT_LOC = new File(ROOT_LOC.toString().replace(fileRequest,""));
            }
            // if a directory add index.html automatically
            if (fileRequest.endsWith("/")) {
                fileRequest += INDEX_FILE;
            }

            // create file object from request
            File file = new File(ROOT_LOC, fileRequest);
            int fileLength = (int)file.length();


            // If request is a HEAD only send the headers
            if (method.equals("HEAD")){
                // send HTTP headers
                clientOut.println();
                clientOut.println();
                clientOut.println("HTTP/1.1 200 OK");
                clientOut.println("Server: HTTP/1.1 server");
                clientOut.println("Content-length: " + file.length());
                clientOut.println("Date: " + new Date());
                clientOut.println("Last-Modified: " + new Date(file.lastModified()));
                clientOut.println(); //blank line between headers and content
                clientOut.flush(); //flush character output stream buffer
            }


            // if request is a GET send the file content
            if (method.equals("GET")) {
                // add ifModifiedSince condition
                if (ifModifiedSince){
                    // if last modified is later than ifModifiedSince then send get file
                    if (file.lastModified()>millis){
                        getRequest(file,fileLength,clientOut,binaryOut);
                    }
                    else {
                        // headers when 304 Error is returned
                        clientOut.println("HTTP/1.1 304 Not Modified");
                        clientOut.println("Server: HTTP/1.1 server");
                        clientOut.println("Date: " + new Date());
                        clientOut.println();
                        // html error to show up in the browser
                        clientOut.println("<HTML>");
                        clientOut.println("<HEAD><TITLE>Not Modified</TITLE>" + "</HEAD>");
                        clientOut.println("<BODY>");
                        clientOut.println("<H2>304 Not Modified");
                        clientOut.println("</BODY></HTML>");
                        clientOut.flush();
                    }
                }
                else{
                    // if there is no ifModifiedSince header than get file normally
                    getRequest(file,fileLength,clientOut,binaryOut);
                }
            }
        }
        
        catch (FileNotFoundException not_found) {
            // calls the not found function to return error 404
            NotFound(clientOut, fileRequest);
        }

        catch (Exception bad_request){
            // headers when error 400 is returned
            clientOut.println("HTTP/1.1 400 Bad Request");
            clientOut.println("Server: HTTP/1.1 server");
            clientOut.println("Date: " + new Date());
            clientOut.println();
            // html to show up in browser
            clientOut.println("<HTML>");
            clientOut.println("<HEAD><TITLE>Bad Request</TITLE>" + "</HEAD>");
            clientOut.println("<BODY>");
            clientOut.println("<H2>Bad Request");
            clientOut.println("</BODY></HTML>");
            clientOut.flush();
        }

        finally {
            close(input);
            close(clientOut);
            close(binaryOut);
            close(mySocket);
        }
    }


    // main function
    public static void main(String[] args) {

        // command argument for port number
        if (args.length > 0) {
            PORT = Integer.parseInt(args[0]);
        }

        // command argument for root path
        if (args.length > 1) {
            ROOT_LOC = new File(args[1]);
        }

        try {
            ServerSocket serverConnect = new ServerSocket(PORT);
            System.out.println("\nServer is listening on " + PORT + "...\n");

            while (true)  {
                // start the server
                WebServer server = new WebServer(serverConnect.accept());
                // make a new thread
                Thread threadRunner = new Thread(server);
                threadRunner.start();
            }
        }

        catch (IOException e) {
            System.err.println("Server error: " + e);
            System.exit(-1);
        }
    }
}