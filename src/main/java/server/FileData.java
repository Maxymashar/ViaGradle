package server;

import com.google.gson.annotations.SerializedName;

public class FileData {
    @SerializedName("file_name")
    private String fileName;
    @SerializedName("file_size")
    private String fileSize;

    public FileData(String fileName, String fileSize) {
        this.fileName = fileName;
        this.fileSize = fileSize;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileSize() {
        return fileSize;
    }
}
