import java.io.*;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Scanner;


public class Laptop {
    private static OutputStream os = null;
    private static DataInputStream dis = null;
    private static DataOutputStream dos = null;
    private static ArrayList<File> filestoSend = null;
    private static Socket sock = null;
    private static final String DIRECTORY_PATH = "filesonlaptop";
    private static File[] directoryListing = null;

    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        System.out.println("Upload you files to Cloud");
        System.out.println("Enter the Server IP Address (Default is 52.25.47.190)");
        String serverIP = scan.next();
        try {
            sock = new Socket(serverIP, 3333);
            System.out.println("Successfully Connected to Server");
            Thread.sleep(1000);
        } catch (Exception ex) {
            System.out.println("Unable to Connect to Server");
        }
        try {
            init(sock);
            dos.writeInt(0);
            dos.flush();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {

            System.out.println("Scanning the files present in " + DIRECTORY_PATH + " folder.");
            Thread.sleep(1000);
            File dir = new File(DIRECTORY_PATH);
            directoryListing = dir.listFiles();
            System.out.println("Number of files in Client Folder: " + directoryListing.length);
            Thread.sleep(1000);
            System.out.println("Comparing it with laptop folder on Server");
            Thread.sleep(1000);
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
        try {
            dos.writeInt(directoryListing.length);
            dos.flush();
            Boolean fileExists, isSame;
            if (directoryListing != null) {
                System.out.println("Processing Each File and Comparing MD5 and Last Modified with Server");
                Thread.sleep(1000);
                for (File currFile : directoryListing) {
                    System.out.println("Processing File: " + currFile.getName());
                    Thread.sleep(1000);
                    System.out.println("Checking if file exist on Server");
                    Thread.sleep(1000);
                    dos.writeUTF(currFile.getName());
                    dos.flush();
                    fileExists = dis.readBoolean();
                    if (fileExists) {//get md5 and date modified from server
                        System.out.println("File exist on Server. Checking if it is the same.");
                        Thread.sleep(1000);
                        String md5FromServer = dis.readUTF();
                        Long lastmodifiedfromServer = dis.readLong();
                        if ((md5String(currFile)).equals(md5FromServer) && currFile.lastModified() == lastmodifiedfromServer) {
                            isSame = true;
                            System.out.println("The file is Same, No need to upload!");
                            Thread.sleep(1000);
                        } else {
                            isSame = false;
                            filestoSend.add(currFile);
                            System.out.println("The file is different, Adding to the Upload List");
                            Thread.sleep(1000);

                        }

                        dos.writeBoolean(isSame);
                        dos.flush();
                    } else {
                        System.out.println("File does not exist on Server, Adding to the Upload List");
                        Thread.sleep(1000);
                        filestoSend.add(currFile);
                    }
                }
                System.out.println("All files Processed");
                Thread.sleep(1000);

                if (!filestoSend.isEmpty()) {
                    ObjectOutputStream oos = new ObjectOutputStream(os);
                    oos.writeObject(filestoSend);
                    oos.flush();
                    System.out.println("Uploading of files Starting");
                    Thread.sleep(1000);
                    System.out.println("Total no of Files to Upload: " + filestoSend.size());
                    Thread.sleep(1000);
                    System.out.println("List of Files to upload: " + filestoSend);
                    Thread.sleep(1000);

                    SendArrayList(filestoSend, sock);
                } else {
                    System.out.println("Client already in sync with Server. Uploading not required.");
                    Thread.sleep(1000);
                }
            }

            System.out.println("Closing Connection with Server");
            Thread.sleep(1000);
            os.close();
            sock.close();
            System.out.println("Successfully closed connection with Server.");
            Thread.sleep(1000);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public static void SendArrayList(ArrayList<File> arrayListtoSend, Socket sock) throws Exception {

        OutputStream os = sock.getOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(os);
        DataInputStream dis = new DataInputStream(sock.getInputStream());
        for (File currFile : arrayListtoSend) {
            byte[] myByteArray = new byte[1024];
            dataOutputStream.writeUTF(currFile.getName());
            dataOutputStream.writeLong(currFile.length());
            dataOutputStream.writeUTF(md5String(currFile));
            dataOutputStream.flush();

            System.out.println("Uploading File: " + currFile.getName() + " Size ( " + currFile.length() / 1024 + " Kb ) MD5 (" + md5String(currFile) + ")");
            Thread.sleep(1000);
            FileInputStream fis = new FileInputStream(currFile);
            BufferedInputStream bis = new BufferedInputStream(fis);
            int count;
            while ((count = bis.read(myByteArray, 0, myByteArray.length)) != -1) {
                os.write(myByteArray, 0, count);
            }
            boolean receivedSuccessfully = dis.readBoolean();
            if (receivedSuccessfully) {
                System.out.println("Md5 Matched, Uploaded Successfully");
                Thread.sleep(1000);
                dos.writeLong(currFile.lastModified());
            } else {
                System.out.println("MD5 of Uploaded File did not match, Please run program again");
                Thread.sleep(1000);
            }
        }

        System.out.println("Uploaded all the Files. Removing Temp Files...");
        Thread.sleep(1000);
    }


    private static void init(Socket sock) throws Exception {
        System.out.println("Initializing I/O streams");
        Thread.sleep(1000);
        InputStream is = sock.getInputStream();
        os = sock.getOutputStream();
        dis = new DataInputStream(is);
        dos = new DataOutputStream(os);
        filestoSend = new ArrayList<>();
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

}