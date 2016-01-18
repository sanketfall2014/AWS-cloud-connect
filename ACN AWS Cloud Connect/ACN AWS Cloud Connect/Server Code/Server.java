import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.ArrayList;


public class Server {
    private static ServerSocket serverSocket = null;


    private static Socket sock = null;

    private static InputStream is = null;
    private static OutputStream os = null;
    private static DataInputStream dis = null;
    private static DataOutputStream dos = null;
    private static int whichClient;
    private static String directory;
    private static int countOfFiles = 0;
    private static ArrayList<File> filesReceived = null;

    //0 for Laptop, 1 for android

    @SuppressWarnings({"unchecked"})
    public static void main(String[] args) {
        try {
            System.out.println("Trying to Start Server");
            Thread.sleep(1000);
            serverSocket = new ServerSocket(3333);

            System.out.println("Server started @ " + serverSocket.getLocalPort());
            Thread.sleep(1000);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        while (true) {

            try {
                countOfFiles = 0;
                System.out.println("Waiting for Client Connection...");
                init(serverSocket);
                whichClient = dis.readInt();

                if (whichClient == 0) {
                    directory = "laptop/";
                    System.out.println("Client is a Laptop. Setting the Receiving folder to 'laptop'");
                } else if (whichClient == 1) {
                    directory = "android/";
                    System.out.println("Client is an Android device. Setting the Receiving folder to 'android'");
                } else {
                    System.out.println("Unknown Client");
                    System.out.println("Disconnecting...");
                    serverSocket.close();
                    serverSocket = new ServerSocket(3333);
                }


                int numOfFiles = dis.readInt();
                System.out.println("Receiving information from Client");
                System.out.println("Number of Files on Client: " + numOfFiles);
                System.out.println("Processing Each Individual File");
                String filename;
                Boolean exists, isSame;
                for (int i = 0; i < numOfFiles; i++) {
                    filename = dis.readUTF();
                    System.out.println("Checking file " + filename);
                    File myFile = new File(directory + filename);
                    exists = myFile.exists();
                    dos.writeBoolean(exists);
                    dos.flush();
                    if (exists) { //as file exists, send the file md5 and date modified
                        System.out.println("File exists. Checking if it is the same by matching MD5 and Last Modified Date");
                        dos.writeUTF(md5String(myFile));
                        dos.flush();
                        dos.writeLong(myFile.lastModified());
                        dos.flush();
                        isSame = dis.readBoolean();

                        if (!isSame) {
                            countOfFiles++;
                            System.out.println("The file present is different. Adding to Receiving list");
                        } else {
                            System.out.println("The file present is same. No need to receive.");
                        }

                    } else {
                        System.out.println("The file does not exist. Adding to the Receiving List");
                        countOfFiles++;
                    }
                }
                System.out.println("Processed all files.");

                if (countOfFiles != 0) {
                    ObjectInputStream ois = new ObjectInputStream(is);

                    filesReceived = (ArrayList<File>) ois.readObject();
                    System.out.println("List of Files to Receive: " + filesReceived);
                    System.out.println("Receiving Files...");
                    ReceiveArrayList(filesReceived, sock);
                }
                else{
                    System.out.println("Server already in sync with Client. No need to receive any files.");
                }
                System.out.println("Closing Connection with Client");
                is.close();
                sock.close();
                System.out.println("Successfully closed connection with Client.");


            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void ReceiveArrayList(ArrayList<File> filesReceived, Socket sock) throws Exception {
        InputStream is = sock.getInputStream();
        DataInputStream din = new DataInputStream(is);
        String filename;
        Long filesize;
        String md5fromClient;
        FileOutputStream fos;
        BufferedOutputStream bos;
        DataOutputStream dos = new DataOutputStream(sock.getOutputStream());
        boolean sentSuccessfully;
        byte[] buffer = new byte[1024];
        for (File currfile : filesReceived) {
            filename = din.readUTF();
            filesize = din.readLong();
            md5fromClient = din.readUTF();
            System.out.println("Receiving File: " + filename + " Size ( " + filesize / 1024 + " Kb ) MD5 ( " + md5fromClient + " )");
            fos = new FileOutputStream(directory + filename);
            bos = new BufferedOutputStream(fos);
            int bytesToRead = filesize.intValue();
            int bytesRead;
            while (bytesToRead > 0 && (bytesRead = is.read(buffer, 0, Math.min(buffer.length, bytesToRead))) > 0) {
                bos.write(buffer, 0, bytesRead);
                bytesToRead -= bytesRead;
            }
            bos.flush();
            bos.close();
            String md5OfRecieved = md5String(new File(directory + filename));
            if (md5fromClient.equals(md5OfRecieved)) {
                System.out.println("MD5 Matches, File Received Successfully");
                sentSuccessfully = true;
            } else {
                System.out.println("MD5 did not match, Run program again.");
                sentSuccessfully = false;
            }
            dos.writeBoolean(sentSuccessfully);
            if (sentSuccessfully) {
                Long lastmodifiedfromclient = din.readLong();
                File myFile = new File(directory + filename);
                myFile.setLastModified(lastmodifiedfromclient);
            }
        }
        System.out.println("Received all the files. Removing Temp Files...");
    }


    public static String md5String(File file) {
        try {
            InputStream fin = new FileInputStream(file);
            java.security.MessageDigest md5er = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[1024];
            int read;
            do {
                read = fin.read(buffer);
                if (read > 0) {
                    md5er.update(buffer, 0, read);
                }
            } while (read != -1);
            fin.close();
            byte[] digest = md5er.digest();
            if (digest == null) {
                return null;
            }
            String strDigest = "0x";
            for (int i = 0; i < digest.length; i++) {
                strDigest += Integer.toString((digest[i] & 0xff)
                        + 0x100, 16).substring(1).toUpperCase();
            }
            return strDigest;
        } catch (Exception e) {
            return null;
        }
    }


    private static void init(ServerSocket serverSocket) throws Exception {
        sock = serverSocket.accept();
        System.out.println("Receiving Request to Connect");
        System.out.println("Connected to a Client");
        System.out.println("Initializing the I/O streams...");
        is = sock.getInputStream();
        os = sock.getOutputStream();
        dis = new DataInputStream(is);
        dos = new DataOutputStream(os);
    }
}
