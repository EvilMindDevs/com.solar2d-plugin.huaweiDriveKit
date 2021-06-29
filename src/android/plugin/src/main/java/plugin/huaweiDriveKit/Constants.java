package plugin.huaweiDriveKit;

class Constants {
    static final String eventName = "DriveKit";
    static final String pluginName = "DriveKit";

    static final String TAG = "HMS Drive Kit";
    static final int listenerIndex2 = 2;
    static final int listenerIndex3 = 3;

    static final String signIn = "signIn";
    static final String initDrive = "initDrive";
    static final String createDirectory = "createDirectory";
    static final String createFile = "createFile";
    static final String copyFile = "copyFile";
    static final String updateFile = "updateFile";
    static final String deleteFile = "deleteFile";
    static final String getFileMetadata = "getFileMetadata";
    static final String downloadFile = "downLoadFile";
    static final String getFileList = "getFileList";
    static final String filesWatch = "filesWatch";

    // Directly upload the file if it is smaller than 20 MB.
    static final int DIRECT_UPLOAD_MAX_SIZE = 20 * 1024 * 1024;
    static final int DIRECT_DOWNLOAD_MAX_SIZE = 20 * 1024 * 1024;

    static final String createComments = "createComments";
    static final String listComments = "listComments";
    static final String getComments = "getComments";
    static final String updateComments = "updateComments";
    static final String deleteComments = "deleteComments";

    static final String createReplies = "createReplies";
    static final String listReplies = "listReplies";
    static final String getReplies = "getReplies";
    static final String updateReplies = "updateReplies";
    static final String deleteReplies = "deleteReplies";

    static final String listHistoryVersions = "listHistoryVersions";
    static final String getHistoryVersions = "getHistoryVersions";
    static final String updateHistoryVersions = "updateHistoryVersions";
    static final String deleteHistoryVersions = "deleteHistoryVersions";
}
