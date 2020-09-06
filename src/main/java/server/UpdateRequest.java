package server;

import com.google.gson.annotations.SerializedName;

public class UpdateRequest {
    @SerializedName("file_name")
    private String fileName;
    @SerializedName("file_size")
    private String fileSize;
    private String action;

    public UpdateRequest(String fileName, String fileSize, String action) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.action = action;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileSize() {
        return fileSize;
    }

    public String getAction() {
        return action;
    }
}
