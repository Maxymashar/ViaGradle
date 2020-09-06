package server;

import java.io.*;

public class BufferedPipeStream {
    private BufferedInputStream dataIn;
    private BufferedOutputStream dataOut;
    private int contentLength;

    public BufferedPipeStream(InputStream dataIn, OutputStream dataOut,int contentLength){
        this.dataIn = new BufferedInputStream(dataIn);
        this.dataOut = new BufferedOutputStream(dataOut);
        this.contentLength = contentLength;
    }

    public void transferData() throws IOException {
        int sendBytes = 0;
        while (sendBytes < contentLength){
            dataOut.write(dataIn.read());
            sendBytes ++;
        }
        dataOut.flush();
    }

}
