package com.acnproject.awscloudconnect;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private Button syncToCloud;
    private EditText serverIPEditText;
    private TextView log;
    private TextView log2;
    public String serverIP = "";
    private Button listOfFiles;
    private ProgressBar progressBar;
    private int progressStatus = 0;

    private OutputStream os = null;
    private DataInputStream dis = null;
    private DataOutputStream dos = null;
    private ArrayList<File> filestoSend = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        syncToCloud = (Button) findViewById(R.id.syncButton);
        serverIPEditText = (EditText) findViewById(R.id.serverIPEditText);
        log = (TextView) findViewById(R.id.log);
        log2 = (TextView) findViewById(R.id.log2);
        listOfFiles = (Button) findViewById(R.id.listOfFiles);
        progressBar = (ProgressBar) findViewById(R.id.progressbar);
        progressBar.setVisibility(View.INVISIBLE);
        listOfFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(v.getContext(), listFiles.class);
                startActivity(myIntent);

            }
        });

        serverIPEditText.setText("52.25.47.190");
        syncToCloud.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                serverIP = serverIPEditText.getText().toString().trim();
                progressStatus = 0;
                try {
                    if (serverIP != "") {
                        Log.d("ClientActivity", "C: Connecting...");
                        log.setText("Try to Connect to Server");
                        log2.setText("");
                        Thread clientThread = new Thread(new ClientThread());
                        clientThread.start();
                    } else {
                        log.setText("Please Enter an IP first");
                    }
                } catch (Exception ex) {
                    Log.d("Client Activity", ex.toString());
                }


            }
        });
    }

    public class ClientThread implements Runnable {

        @Override
        public void run() {
            try {
                Thread.sleep(1000);
                Socket socket = new Socket(serverIP, 3333);
                init(socket);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        log.setText("Successfully Connected");
                    }
                });
                Thread.sleep(1000);

                dos.writeInt(1);
                dos.flush();
                String SD_CARD_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
                File dir = new File(SD_CARD_PATH + File.separator + "filesonandroid");
                final File[] directoryListing = dir.listFiles();
                Log.d("No of Files", (String.valueOf(directoryListing.length)));
                progressBar.setMax(directoryListing.length);
                dos.writeInt(directoryListing.length);
                dos.flush();
                Boolean fileExists, isSame;
                if (directoryListing != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.VISIBLE);
                        }
                    });
                    for (final File currFile : directoryListing) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                log.setText("Checking File: " + currFile.getName());
                                progressBar.setProgress(progressStatus);
                            }
                        });
                        dos.writeUTF(currFile.getName());
                        dos.flush();
                        fileExists = dis.readBoolean();
                        if (fileExists) {//get md5 and date modified from server
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    log2.setText("File Exists on Server");
                                }
                            });
                            Thread.sleep(1000);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    log2.setText("Checking if its the Same");
                                }
                            });
                            Thread.sleep(1000);
                            String md5FromServer = dis.readUTF();
                            Long lastmodifiedfromServer = dis.readLong();
                            Log.d(currFile.getName(), "MD5 from Server: " + md5FromServer);
                            Log.d(currFile.getName(), "Client MD5: " + md5String(currFile));
                            Log.d(currFile.getName(), "LstmodifiedfromServer = " + lastmodifiedfromServer);
                            Log.d(currFile.getName(), "Client Last Modified: " + currFile.lastModified());
                            if ((md5String(currFile)).equals(md5FromServer) && currFile.lastModified() == lastmodifiedfromServer) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        log2.setText("Its the Same! No need to overwrite it!");
                                    }
                                });
                                Thread.sleep(1000);
                                isSame = true;
                            } else {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        log2.setText("Its not the Same! We will overwrite it!");
                                    }
                                });
                                Thread.sleep(1000);
                                isSame = false;
                                filestoSend.add(currFile);

                            }
                            dos.writeBoolean(isSame);
                            dos.flush();
                            Log.d(currFile.getName() + " Exists? ", String.valueOf(fileExists));
                            Log.d(currFile.getName() + " IsSame? ", String.valueOf(isSame));
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    log2.setText("It is not on the Server, We'll Upload it");
                                }
                            });
                            Thread.sleep(1000);
                            Log.d(currFile.getName() + " Exists? ", String.valueOf(fileExists));
                            filestoSend.add(currFile);
                        }
                        progressStatus++;
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setProgress(progressStatus);
                        }
                    });
                    Thread.sleep(1000);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.INVISIBLE);
                        }
                    });
                    if (!filestoSend.isEmpty()) {
                        ObjectOutputStream oos = new ObjectOutputStream(os);
                        oos.writeObject(filestoSend);
                        oos.flush();
                        Log.d("No of Files to Send", filestoSend.toString());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                log.setText("Sending Files, Sit Tight!!");
                                log2.setText("Total no of Files to Send: " + filestoSend.size());
                            }
                        });
                        Thread.sleep(1000);
                        progressStatus = 0;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setMax(filestoSend.size());
                            }
                        });

                        SendArrayList(filestoSend, socket);
                    }
                }

                os.close();

                Thread.sleep(1000);

                socket.close();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        log.setText("Synced all Files with Cloud");
                        log2.setText("Closed Connection with Server");
                    }
                });
            } catch (
                    Exception ex
                    ) {
                Log.d("Client Thread", ex.getMessage());
            }
        }
    }


    public void SendArrayList(final ArrayList<File> arrayListToSend, Socket sock) throws Exception {
        progressStatus = 0;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setMax(arrayListToSend.size());
            }
        });

        OutputStream os = sock.getOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(os);
        DataInputStream dis = new DataInputStream(sock.getInputStream());
        for (final File currFile : arrayListToSend) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    log.setText("Sending File: " + currFile.getName());
                    log2.setText("Size: " + currFile.length() / 1024 + " Kb");
                    progressBar.setProgress(progressStatus++);
                }
            });


            byte[] myByteArray = new byte[1024];
            dataOutputStream.writeUTF(currFile.getName());
            dataOutputStream.writeLong(currFile.length());
            dataOutputStream.writeUTF(md5String(currFile));
            dataOutputStream.flush();
            Log.d(currFile.getName(), "Sending File");
            Log.d(currFile.getName(), String.valueOf(currFile.length()));
            Log.d(currFile.getName(), String.valueOf(md5String(currFile)));
            FileInputStream fis = new FileInputStream(currFile);
            BufferedInputStream bis = new BufferedInputStream(fis);
            int count;
            int bytesSend = 0;
            while ((count = bis.read(myByteArray, 0, myByteArray.length)) != -1) {
                os.write(myByteArray, 0, count);
                bytesSend += count;
            }
            //System.out.println("bytesSent = " + bytesSend);
            boolean receivedSuccessfully = dis.readBoolean();
            if (receivedSuccessfully) {
                //System.out.println("File Sent successfully");
                dos.writeLong(currFile.lastModified());

            } else
                Log.d(currFile.getName(), "MD5 did not Match");
            //System.out.println("MD5 did not match, Please run program again");

            Thread.sleep(1000);
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setProgress(progressBar.getMax());
            }
        });
        Thread.sleep(1000);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.INVISIBLE);
                log.setText("");
                log2.setText("");
            }
        });

    }

    private void init(Socket sock) throws Exception {
        InputStream is = sock.getInputStream();
        os = sock.getOutputStream();
        dis = new DataInputStream(is);
        dos = new DataOutputStream(os);
        filestoSend = new ArrayList<>();
    }

    public String md5String(File file) {
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




