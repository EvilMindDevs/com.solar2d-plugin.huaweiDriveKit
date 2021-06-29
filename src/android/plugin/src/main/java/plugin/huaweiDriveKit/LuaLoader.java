package plugin.huaweiDriveKit;

import android.util.Log;

import com.ansca.corona.CoronaEnvironment;
import com.ansca.corona.CoronaLua;
import com.ansca.corona.CoronaRuntime;
import com.ansca.corona.CoronaRuntimeListener;
import com.ansca.corona.CoronaRuntimeTask;
import com.ansca.corona.CoronaRuntimeTaskDispatcher;
import com.huawei.agconnect.config.AGConnectServicesConfig;
import com.huawei.cloud.base.auth.DriveCredential;
import com.huawei.cloud.base.http.FileContent;
import com.huawei.cloud.base.media.MediaHttpDownloader;
import com.huawei.cloud.base.media.MediaHttpDownloaderProgressListener;
import com.huawei.cloud.base.util.DateTime;
import com.huawei.cloud.base.util.StringUtils;
import com.huawei.cloud.client.exception.DriveCode;
import com.huawei.cloud.services.drive.Drive;
import com.huawei.cloud.services.drive.model.About;
import com.huawei.cloud.services.drive.model.Comment;
import com.huawei.cloud.services.drive.model.CommentList;
import com.huawei.cloud.services.drive.model.File;
import com.huawei.cloud.services.drive.model.FileList;
import com.huawei.cloud.services.drive.model.HistoryVersion;
import com.huawei.cloud.services.drive.model.HistoryVersionList;
import com.huawei.cloud.services.drive.model.Reply;
import com.huawei.cloud.services.drive.model.ReplyList;
import com.naef.jnlua.JavaFunction;
import com.naef.jnlua.LuaState;
import com.naef.jnlua.LuaType;
import com.naef.jnlua.NamedJavaFunction;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static plugin.huaweiDriveKit.Constants.DIRECT_DOWNLOAD_MAX_SIZE;
import static plugin.huaweiDriveKit.Constants.DIRECT_UPLOAD_MAX_SIZE;
import static plugin.huaweiDriveKit.Utils.commentToJson;
import static plugin.huaweiDriveKit.Utils.commentToJsonArray;
import static plugin.huaweiDriveKit.Utils.fileToJson;
import static plugin.huaweiDriveKit.Utils.fileToJsonArray;
import static plugin.huaweiDriveKit.Utils.historyVersionListToJson;
import static plugin.huaweiDriveKit.Utils.historyVersionToJson;
import static plugin.huaweiDriveKit.Utils.replyToJson;
import static plugin.huaweiDriveKit.Utils.replyToJsonArray;


@SuppressWarnings("WeakerAccess")
public class LuaLoader implements JavaFunction, CoronaRuntimeListener {
    private static int fListener;
    public static CoronaRuntimeTaskDispatcher fDispatcher = null;

    private static Drive drive;
    private static About about;

    private static final Map<String, String> MIME_TYPE_MAP = new HashMap<String, String>();

    static {
        MIME_TYPE_MAP.put(".doc", "application/msword");
        MIME_TYPE_MAP.put(".jpg", "image/jpeg");
        MIME_TYPE_MAP.put(".mp3", "audio/x-mpeg");
        MIME_TYPE_MAP.put(".mp4", "video/mp4");
        MIME_TYPE_MAP.put(".pdf", "application/pdf");
        MIME_TYPE_MAP.put(".png", "image/png");
        MIME_TYPE_MAP.put(".txt", "text/plain");
    }

    private static DriveCredential.AccessMethod refreshAT = new DriveCredential.AccessMethod() {
        @Override
        public String refreshToken() {
            dispatchEvent(false, "refreshToken", "", Constants.pluginName);
            return null;
        }
    };

    private static void initializeDrive() {
        if (drive == null) {
            try {
                drive = new Drive.Builder(CredentialManager.getInstance().getCredential(), CoronaEnvironment.getApplicationContext()).build();
            } catch (Exception e) {
                Log.e(Constants.TAG, "buildDrive error: " + e.getMessage());
            }
        }
    }

    private static void initializeAbout() {
        initializeDrive();
        if (about == null) {
            try {
                about = drive.about().get().setFields("*").execute();
                Log.d(Constants.TAG, "About " + about.toString());
            } catch (Exception e) {
                Log.e(Constants.TAG, "getAbout error: " + e.toString());
            }
        }
    }

    private static final String EVENT_NAME = "HMSDriveKit";

    @SuppressWarnings("unused")
    public LuaLoader() {
        fListener = CoronaLua.REFNIL;
        CoronaEnvironment.addRuntimeListener(this);
    }

    @Override
    public int invoke(LuaState L) {
        NamedJavaFunction[] luaFunctions = new NamedJavaFunction[]{
                new init(),
                new createDirectory(),
                new createFile(),
                new updateFile(),
                new deleteFile(),
                new downloadFile(),
                new getFileList(),
                new createComments(),
                new listComments(),
                new getComments(),
                new updateComments(),
                new deleteComments(),
                new createReplies(),
                new listReplies(),
                new getReplies(),
                new updateReplies(),
                new deleteReplies(),
                new listHistoryVersions(),
                new getHistoryVersions(),
                new deleteHistoryVersions(),
        };
        String libName = L.toString(1);
        L.register(libName, luaFunctions);

        return 1;
    }

    @Override
    public void onLoaded(CoronaRuntime runtime) {

    }

    @Override
    public void onStarted(CoronaRuntime runtime) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            requestPermissions(PERMISSIONS_STORAGE, 1);
//        }
    }

    @Override
    public void onSuspended(CoronaRuntime runtime) {
    }

    @Override
    public void onResumed(CoronaRuntime runtime) {
    }

    @Override
    public void onExiting(CoronaRuntime runtime) {
        CoronaLua.deleteRef(runtime.getLuaState(), fListener);
        fListener = CoronaLua.REFNIL;
    }

    @SuppressWarnings("unused")
    public void dispatchEvent(final String message) {
        CoronaEnvironment.getCoronaActivity().getRuntimeTaskDispatcher().send(new CoronaRuntimeTask() {
            @Override
            public void executeUsing(CoronaRuntime runtime) {
                LuaState L = runtime.getLuaState();

                CoronaLua.newEvent(L, EVENT_NAME);

                L.pushString(message);
                L.setField(-2, "message");

                try {
                    CoronaLua.dispatchEvent(L, fListener, 0);
                } catch (Exception ignored) {
                }
            }
        });
    }

    private static class init implements NamedJavaFunction {

        @Override
        public String getName() {
            return "init";
        }

        @Override
        public int invoke(LuaState L) {

            if (CoronaEnvironment.getCoronaActivity() == null) {
                return 0;
            }

            AGConnectServicesConfig config = AGConnectServicesConfig.fromContext(CoronaEnvironment.getApplicationContext());
            config.overlayWith(new HmsLazyInputStream(CoronaEnvironment.getApplicationContext()).get(CoronaEnvironment.getApplicationContext()));

            fDispatcher = new CoronaRuntimeTaskDispatcher(L);

            final int listener = CoronaLua.isListener(L, Constants.listenerIndex3, Constants.eventName) ? CoronaLua.newRef(L, Constants.listenerIndex3) : CoronaLua.REFNIL;
            String unionId, accessToken;

            if (L.type(2) == LuaType.TABLE || L.tableSize(2) != 0) {
                L.getField(2, "unionId");
                if (L.isString(-1)) {
                    unionId = L.toString(-1);
                    L.pop(1);
                } else {
                    sendDispatcher(listener, true, "initDrive({unionId(String), accessToken(String)}) expected", Constants.initDrive, Constants.pluginName);
                    dispatchEvent(true, "initDrive({unionId(String), accessToken(String)}) expected", Constants.initDrive, Constants.pluginName);
                    return 0;
                }

                L.getField(2, "accessToken");
                if (L.isString(-1)) {
                    accessToken = L.toString(-1);
                } else {
                    sendDispatcher(listener, true, "initDrive({unionId(String), accessToken(String)}) expected", Constants.initDrive, Constants.pluginName);
                    dispatchEvent(true, "initDrive({unionId(String), accessToken(String)}) expected", Constants.initDrive, Constants.pluginName);
                    return 0;
                }
            } else {
                sendDispatcher(listener, true, "initDrive({unionId(String), accessToken(String)}) expected", Constants.initDrive, Constants.pluginName);
                dispatchEvent(true, "initDrive({unionId(String), accessToken(String)}) expected", Constants.initDrive, Constants.pluginName);
                return 0;
            }

            if (DriveCode.SUCCESS == CredentialManager.getInstance().init(unionId, accessToken, refreshAT)) {
                sendDispatcher(listener, false, "", Constants.initDrive, Constants.pluginName);
                dispatchEvent(false, "", Constants.initDrive, Constants.pluginName);
            } else {
                sendDispatcher(listener, true, "initDrive error", Constants.initDrive, Constants.pluginName);
                dispatchEvent(true, "initDrive error", Constants.initDrive, Constants.pluginName);
            }

            int listenerIndex = 1;

            if (CoronaLua.isListener(L, listenerIndex, EVENT_NAME)) {
                fListener = CoronaLua.newRef(L, listenerIndex);
            }

            return 0;
        }
    }

    private static class createDirectory implements NamedJavaFunction {
        @Override
        public String getName() {
            return Constants.createDirectory;
        }

        @Override
        public int invoke(LuaState L) {
            if (CoronaEnvironment.getCoronaActivity() == null) {
                return 0;
            }
            initializeDrive();

            String dirName;
            final int listener = CoronaLua.isListener(L, Constants.listenerIndex2, Constants.eventName) ? CoronaLua.newRef(L, Constants.listenerIndex2) : CoronaLua.REFNIL;

            if (L.type(1) == LuaType.TABLE || L.tableSize(1) != 0) {
                L.getField(1, "dirName");
                if (L.isString(-1)) {
                    dirName = L.toString(-1);
                    L.pop(1);
                } else {
                    sendDispatcher(listener, true, "createDirectory({dirName(String)) expected", Constants.createDirectory, Constants.pluginName);
                    dispatchEvent(true, "createDirectory({dirName(String)}) expected", Constants.createDirectory, Constants.pluginName);
                    return 0;
                }
            } else {
                sendDispatcher(listener, true, "createDirectory({dirName(String)) expected", Constants.createDirectory, Constants.pluginName);
                dispatchEvent(true, "createDirectory({dirName(String)}) expected", Constants.createDirectory, Constants.pluginName);
                return 0;
            }

            try {
                Map<String, String> appProperties = new HashMap<>();
                appProperties.put("appProperties", "property");

                File file = new File();
                file.setFileName(dirName)
                        .setAppSettings(appProperties)
                        .setMimeType("application/vnd.huawei-apps.folder");

                File directory = drive.files().create(file).execute();

                sendDispatcher(listener, false, "", Constants.createDirectory, Constants.pluginName, fileToJson(directory), directory);
                dispatchEvent(false, "", Constants.createDirectory, Constants.pluginName, fileToJson(directory));
            } catch (Exception e) {
                sendDispatcher(listener, true, "createDirectory error: " + e.toString(), Constants.createDirectory, Constants.pluginName);
                dispatchEvent(true, "createDirectory error: " + e.toString(), Constants.createDirectory, Constants.pluginName);
            }

            return 0;
        }
    }

    private static class createFile implements NamedJavaFunction {

        @Override
        public String getName() {
            return Constants.createFile;
        }

        @Override
        public int invoke(LuaState L) {
            if (CoronaEnvironment.getCoronaActivity() == null) {
                return 0;
            }

            initializeDrive();

            String filePath, parentId, thumbnailMimeType;
            byte[] thumbnailImageBuffer;

            final int listener = CoronaLua.isListener(L, Constants.listenerIndex2, Constants.eventName) ? CoronaLua.newRef(L, Constants.listenerIndex2) : CoronaLua.REFNIL;

            if (L.type(1) == LuaType.TABLE || L.tableSize(1) != 0) {
                L.getField(1, "filePath");
                if (L.isString(-1)) {
                    filePath = L.toString(-1);
                    L.pop(1);
                } else {
                    sendDispatcher(listener, true, "createFile({filePath(String), parentId(String), thumbnailMimeType(String), thumbnailImageBuffer(byte[])}) expected", Constants.createFile, Constants.pluginName);
                    dispatchEvent(true, "createFile({filePath(String), parentId(String), thumbnailMimeType(String), thumbnailImageBuffer(byte[])}) expected", Constants.createFile, Constants.pluginName);
                    return 0;
                }

                L.getField(1, "parentId");
                if (L.isString(-1)) {
                    parentId = L.toString(-1);
                    L.pop(1);
                } else {
                    sendDispatcher(listener, true, "createFile({filePath(String), parentId(String), thumbnailMimeType(String), thumbnailImageBuffer(byte[])}) expected", Constants.createFile, Constants.pluginName);
                    dispatchEvent(true, "createFile({filePath(String), parentId(String), thumbnailMimeType(String), thumbnailImageBuffer(byte[])}) expected", Constants.createFile, Constants.pluginName);
                    return 0;
                }

                L.getField(1, "thumbnailMimeType");
                if (L.isString(-1)) {
                    thumbnailMimeType = L.toString(-1);
                    L.pop(1);
                } else {
                    sendDispatcher(listener, true, "createFile({filePath(String), parentId(String), thumbnailMimeType(String), thumbnailImageBuffer(byte[])}) expected", Constants.createFile, Constants.pluginName);
                    dispatchEvent(true, "createFile({filePath(String), parentId(String), thumbnailMimeType(String), thumbnailImageBuffer(byte[])}) expected", Constants.createFile, Constants.pluginName);
                    return 0;
                }

//                L.getField(1, "thumbnailImageBuffer");
//                if (L.isString(-1)) {
//                    thumbnailImageBuffer = L.toByteArray(-1);
//                    L.pop(1);
//                } else {
//                    sendDispatcher(listener, true, "createFile({filePath(String), parentId(String), thumbnailMimeType(String), thumbnailImageBuffer(byte[])}) expected", Constants.createFile, Constants.pluginName);
//                    dispatchEvent(true, "createFile({filePath(String), parentId(String), thumbnailMimeType(String), thumbnailImageBuffer(byte[])}) expected", Constants.createFile, Constants.pluginName);
//                    return 0;
//                }
            } else {
                sendDispatcher(listener, true, "createFile({filePath(String), parentId(String), thumbnailMimeType(String), thumbnailImageBuffer(byte[])}) expected", Constants.createFile, Constants.pluginName);
                dispatchEvent(true, "createFile({filePath(String), parentId(String), thumbnailMimeType(String), thumbnailImageBuffer(byte[])}) expected", Constants.createFile, Constants.pluginName);
                return 0;
            }

            try {
                java.io.File file = new java.io.File(filePath);
                FileContent fileContent = new FileContent(null, file);

                // Set thumbnail data.
                File.ContentExtras contentExtra = new File.ContentExtras();
                File.ContentExtras.Thumbnail thumbnail = new File.ContentExtras.Thumbnail();
                //thumbnail.setContent(Base64.encodeBase64String(thumbnailImageBuffer));
                thumbnail.setMimeType(thumbnailMimeType);
                contentExtra.setThumbnail(thumbnail);

                File content = new File()
                        .setFileName(file.getName())
                        .setParentFolder(Collections.singletonList(parentId))
                        .setContentExtras(contentExtra);

                Drive.Files.Create request = drive.files().create(content, fileContent);
                boolean isDirectUpload = false;
                // Directly upload the file if it is smaller than 20 MB.
                if (file.length() < DIRECT_UPLOAD_MAX_SIZE) {
                    isDirectUpload = true;
                }

                // Set the upload mode. By default, resumable upload is used. If the file is smaller than 20 MB, set this parameter to true.
                request.getMediaHttpUploader().setDirectUploadEnabled(isDirectUpload);

                request.execute();

                sendDispatcher(listener, false, "", Constants.createFile, Constants.pluginName);
                dispatchEvent(false, "", Constants.createFile, Constants.pluginName);

            } catch (Exception e) {
                sendDispatcher(listener, true, "createFile exception: " + filePath + e.toString(), Constants.createFile, Constants.pluginName);
                dispatchEvent(true, "createFile exception: " + filePath + e.toString(), Constants.createFile, Constants.pluginName);
            }

            return 0;
        }
    }

    private static class updateFile implements NamedJavaFunction {

        @Override
        public String getName() {
            return Constants.copyFile;
        }

        @Override
        public int invoke(LuaState L) {
            if (CoronaEnvironment.getCoronaActivity() == null) {
                return 0;
            }
            initializeAbout();

            String fileId, newFilePath;

            final int listener = CoronaLua.isListener(L, Constants.listenerIndex2, Constants.eventName) ? CoronaLua.newRef(L, Constants.listenerIndex2) : CoronaLua.REFNIL;

            if (L.type(1) == LuaType.TABLE || L.tableSize(1) != 0) {
                L.getField(1, "fileId");
                if (L.isString(-1)) {
                    fileId = L.toString(-1);
                    L.pop(1);
                } else {
                    sendDispatcher(listener, true, "updateFile({fileId(String), newFilePath(String)}) expected", Constants.updateFile, Constants.pluginName);
                    dispatchEvent(true, "updateFile({fileId(String), newFilePath(String)}) expected", Constants.updateFile, Constants.pluginName);
                    return 0;
                }

                L.getField(1, "newFilePath");
                if (L.isString(-1)) {
                    newFilePath = L.toString(-1);
                    L.pop(1);
                } else {
                    sendDispatcher(listener, true, "updateFile({fileId(String), newFilePath(String)}) expected", Constants.updateFile, Constants.pluginName);
                    dispatchEvent(true, "updateFile({fileId(String), newFilePath(String)}) expected", Constants.updateFile, Constants.pluginName);
                    return 0;
                }
            } else {
                sendDispatcher(listener, true, "updateFile({fileId(String), newFilePath(String)}) expected", Constants.updateFile, Constants.pluginName);
                dispatchEvent(true, "updateFile({fileId(String), newFilePath(String)}) expected", Constants.updateFile, Constants.pluginName);
                return 0;
            }

            try {
                java.io.File sourceFile = new java.io.File(newFilePath);
                FileContent fileContent = new FileContent(null, sourceFile);
                File updateFile = new File();
                updateFile.setFileName(sourceFile.getName())
                        .setDescription("update folder")
                        .setFavorite(true);
                Drive.Files.Update update = drive.files().update(fileId, updateFile, fileContent);
                boolean isDirectUpload = false;
                // Directly upload the file if it is smaller than 20 MB.
                if (sourceFile.length() < DIRECT_UPLOAD_MAX_SIZE) {
                    isDirectUpload = true;
                }
                // Set the upload mode. By default, resumable upload is used. If the file is smaller than 20 MB, set this parameter to true.
                update.getMediaHttpUploader().setDirectUploadEnabled(isDirectUpload);
                update.execute();

                sendDispatcher(listener, false, "", Constants.updateFile, Constants.pluginName);
                dispatchEvent(false, "", Constants.updateFile, Constants.pluginName);
            } catch (Exception ex) {
                sendDispatcher(listener, true, "updateFile error: " + ex.toString(), Constants.updateFile, Constants.pluginName);
                dispatchEvent(true, "updateFile error: " + ex.toString(), Constants.updateFile, Constants.pluginName);
            }
            return 0;
        }
    }

    private static class deleteFile implements NamedJavaFunction {

        @Override
        public String getName() {
            return Constants.deleteFile;
        }

        @Override
        public int invoke(LuaState L) {
            if (CoronaEnvironment.getCoronaActivity() == null) {
                return 0;
            }
            initializeAbout();

            String fileId;

            final int listener = CoronaLua.isListener(L, Constants.listenerIndex2, Constants.eventName) ? CoronaLua.newRef(L, Constants.listenerIndex2) : CoronaLua.REFNIL;

            if (L.type(1) == LuaType.TABLE || L.tableSize(1) != 0) {
                L.getField(1, "fileId");
                if (L.isString(-1)) {
                    fileId = L.toString(-1);
                    L.pop(1);
                } else {
                    sendDispatcher(listener, true, "deleteFile({fileId(String)}) expected", Constants.deleteFile, Constants.pluginName);
                    dispatchEvent(true, "deleteFile({fileId(String)}) expected", Constants.deleteFile, Constants.pluginName);
                    return 0;
                }
            } else {
                sendDispatcher(listener, true, "deleteFile({fileId(String)}) expected", Constants.deleteFile, Constants.pluginName);
                dispatchEvent(true, "deleteFile({fileId(String)}) expected", Constants.deleteFile, Constants.pluginName);
                return 0;
            }

            try {
                Drive.Files.Delete deleteFile = drive.files().delete(fileId);
                deleteFile.execute();
                sendDispatcher(listener, false, "", Constants.deleteFile, Constants.pluginName);
                dispatchEvent(false, "", Constants.deleteFile, Constants.pluginName);
            } catch (IOException ex) {
                sendDispatcher(listener, true, "deleteFile error: " + ex.toString(), Constants.deleteFile, Constants.pluginName);
                dispatchEvent(true, "deleteFile error: " + ex.toString(), Constants.deleteFile, Constants.pluginName);
            }
            return 0;
        }
    }

    private static class downloadFile implements NamedJavaFunction {

        @Override
        public String getName() {
            return Constants.downloadFile;
        }

        @Override
        public int invoke(LuaState L) {
            if (CoronaEnvironment.getCoronaActivity() == null) {
                return 0;
            }
            initializeAbout();

            String fileId;

            final int listener = CoronaLua.isListener(L, Constants.listenerIndex2, Constants.eventName) ? CoronaLua.newRef(L, Constants.listenerIndex2) : CoronaLua.REFNIL;

            if (L.type(1) == LuaType.TABLE || L.tableSize(1) != 0) {
                L.getField(1, "fileId");
                if (L.isString(-1)) {
                    fileId = L.toString(-1);
                    L.pop(1);
                } else {
                    sendDispatcher(listener, true, "downLoadFile({fileId(String)}) expected", Constants.downloadFile, Constants.pluginName);
                    dispatchEvent(true, "downLoadFile({fileId(String)}) expected", Constants.downloadFile, Constants.pluginName);
                    return 0;
                }
            } else {
                sendDispatcher(listener, true, "downLoadFile({fileId(String)}) expected", Constants.downloadFile, Constants.pluginName);
                dispatchEvent(true, "downLoadFile({fileId(String)}) expected", Constants.downloadFile, Constants.pluginName);
                return 0;
            }

            try {
                String imagePath = "/storage/emulated/0/DCIM/Camera/";
                // Obtain file metadata.
                Drive.Files.Get request = drive.files().get(fileId);
                request.setFields("id,size");
                File res = request.execute();
                // Download a file.
                long size = res.getSize();
                Drive.Files.Get get = drive.files().get(fileId);
                get.setForm("content");
                MediaHttpDownloader downloader = get.getMediaHttpDownloader();

                boolean isDirectDownload = false;
                if (size < DIRECT_DOWNLOAD_MAX_SIZE) {
                    isDirectDownload = true;
                }
                downloader.setContentRange(0, size - 1).setDirectDownloadEnabled(isDirectDownload);
                downloader.setProgressListener(new MediaHttpDownloaderProgressListener() {
                    @Override
                    public void progressChanged(MediaHttpDownloader mediaHttpDownloader) throws IOException {
                        // The download subthread calls this method to process the download progress.
                        double progress = mediaHttpDownloader.getProgress();
                        Log.d(Constants.TAG, "progressChanged => " + progress);
                    }
                });
                java.io.File f = new java.io.File(imagePath + "download.jpg");
                get.executeContentAndDownloadTo(new FileOutputStream(f));
                sendDispatcher(listener, false, "", Constants.downloadFile, Constants.pluginName);
                dispatchEvent(false, "", Constants.downloadFile, Constants.pluginName);
            } catch (Exception e) {
                sendDispatcher(listener, true, "executeFilesGet exception: " + e.toString(), Constants.downloadFile, Constants.pluginName);
                dispatchEvent(true, "executeFilesGet exception: " + e.toString(), Constants.downloadFile, Constants.pluginName);

            }

            return 0;
        }
    }

    private static class getFileList implements NamedJavaFunction {

        @Override
        public String getName() {
            return Constants.getFileList;
        }

        @Override
        public int invoke(LuaState L) {
            if (CoronaEnvironment.getCoronaActivity() == null) {
                return 0;
            }
            initializeAbout();
            String query, orderBy, fields;
            int pageSize;

            final int listener = CoronaLua.isListener(L, Constants.listenerIndex2, Constants.eventName) ? CoronaLua.newRef(L, Constants.listenerIndex2) : CoronaLua.REFNIL;

            if (L.type(1) == LuaType.TABLE || L.tableSize(1) != 0) {
                L.getField(1, "query");
                if (L.isString(-1)) {
                    query = L.toString(-1);
                    L.pop(1);
                } else {
                    sendDispatcher(listener, true, "getFileList({query(String), orderBy(String), fields(String), pageSize(Integer)}) expected", Constants.getFileList, Constants.pluginName);
                    dispatchEvent(true, "getFileList({query(String), orderBy(String), fields(String), pageSize(Integer)}) expected", Constants.getFileList, Constants.pluginName);
                    return 0;
                }

                L.getField(1, "orderBy");
                if (L.isString(-1)) {
                    orderBy = L.toString(-1);
                    L.pop(1);
                } else {
                    sendDispatcher(listener, true, "getFileList({query(String), orderBy(String), fields(String), pageSize(Integer)}) expected", Constants.getFileList, Constants.pluginName);
                    dispatchEvent(true, "getFileList({query(String), orderBy(String), fields(String), pageSize(Integer)}) expected", Constants.getFileList, Constants.pluginName);
                    return 0;
                }

                L.getField(1, "fields");
                if (L.isString(-1)) {
                    fields = L.toString(-1);
                    L.pop(1);
                } else {
                    sendDispatcher(listener, true, "getFileList({query(String), orderBy(String), fields(String), pageSize(Integer)}) expected", Constants.getFileList, Constants.pluginName);
                    dispatchEvent(true, "getFileList({query(String), orderBy(String), fields(String), pageSize(Integer)}) expected", Constants.getFileList, Constants.pluginName);
                    return 0;
                }

                L.getField(1, "pageSize");
                if (L.isNumber(-1)) {
                    pageSize = L.toInteger(-1);
                    L.pop(1);
                } else {
                    sendDispatcher(listener, true, "getFileList({query(String), orderBy(String), fields(String), pageSize(Integer)}) expected", Constants.getFileList, Constants.pluginName);
                    dispatchEvent(true, "getFileList({query(String), orderBy(String), fields(String), pageSize(Integer)}) expected", Constants.getFileList, Constants.pluginName);
                    return 0;
                }
            } else {
                sendDispatcher(listener, true, "getFileList({query(String), orderBy(String), fields(String), pageSize(Integer)}) expected", Constants.getFileList, Constants.pluginName);
                dispatchEvent(true, "getFileList({query(String), orderBy(String), fields(String), pageSize(Integer)}) expected", Constants.getFileList, Constants.pluginName);
                return 0;
            }

            List<File> fileList = null;
            try {

                Drive.Files.List request = drive.files().list();
                String cursor = null;
                fileList = new ArrayList<>();
                do {
                    FileList result = null;
                    result = request.setQueryParam(query)
                            .setOrderBy(orderBy)
                            .setPageSize(pageSize)
                            .setFields(fields)
                            .execute();
                    fileList.addAll(result.getFiles());
                    cursor = result.getNextCursor();
                    request.setCursor(cursor);
                } while (!StringUtils.isNullOrEmpty(cursor));
                sendDispatcher(listener, false, "", Constants.getComments, Constants.pluginName, fileToJsonArray(fileList));
                dispatchEvent(false, "", Constants.getComments, Constants.pluginName, fileToJsonArray(fileList));
            } catch (Exception e) {
                sendDispatcher(listener, true, "executeFilesList exception: " + e.toString(), Constants.getFileList, Constants.pluginName);
                dispatchEvent(true, "executeFilesList exception: " + e.toString(), Constants.getFileList, Constants.pluginName);
            }
            return 0;
        }
    }

    // Comment Operations //
    private static class createComments implements NamedJavaFunction {
        @Override
        public String getName() {
            return Constants.createComments;
        }

        @Override
        public int invoke(LuaState L) {
            if (CoronaEnvironment.getCoronaActivity() == null) {
                return 0;
            }

            String fileId, description;
            Comment comment = null;

            final int listener = CoronaLua.isListener(L, Constants.listenerIndex2, Constants.eventName) ? CoronaLua.newRef(L, Constants.listenerIndex2) : CoronaLua.REFNIL;

            if (L.type(1) == LuaType.TABLE || L.tableSize(1) != 0) {
                L.getField(1, "fileId");
                if (L.isString(-1)) {
                    fileId = L.toString(-1);
                    L.pop(1);
                } else {
                    sendDispatcher(listener, true, "createComments({fileId(String), description(String)}) expected", Constants.createComments, Constants.pluginName);
                    dispatchEvent(true, "createComments({fileId(String), description(String)}) expected", Constants.createComments, Constants.pluginName);
                    return 0;
                }

                L.getField(1, "description");
                if (L.isString(-1)) {
                    description = L.toString(-1);
                    L.pop(1);
                } else {
                    sendDispatcher(listener, true, "createComments({fileId(String), description(String)}) expected", Constants.createComments, Constants.pluginName);
                    dispatchEvent(true, "createComments({fileId(String), description(String)}) expected", Constants.createComments, Constants.pluginName);
                    return 0;
                }
            } else {
                sendDispatcher(listener, true, "createComments({fileId(String), description(String)}) expected", Constants.createComments, Constants.pluginName);
                dispatchEvent(true, "createComments({fileId(String), description(String)}) expected", Constants.createComments, Constants.pluginName);
                return 0;
            }

            try {
                Comment content = new Comment();
                content.setDescription(description);
                content.setCreatedTime(new DateTime(System.currentTimeMillis()));
                comment = drive.comments().create(fileId, content).setFields("*").execute();
                sendDispatcher(listener, false, "", Constants.createComments, Constants.pluginName, commentToJson(comment));
                dispatchEvent(false, "", Constants.createComments, Constants.pluginName, commentToJson(comment));
            } catch (Exception ex) {
                sendDispatcher(listener, true, "createComments error: " + ex.toString(), Constants.createComments, Constants.pluginName);
                dispatchEvent(true, "createComments error: " + ex.toString(), Constants.createComments, Constants.pluginName);
            }
            return 0;
        }
    }

    private static class listComments implements NamedJavaFunction {
        @Override
        public String getName() {
            return Constants.listComments;
        }

        @Override
        public int invoke(LuaState L) {
            if (CoronaEnvironment.getCoronaActivity() == null) {
                return 0;
            }

            String fileId;
            final int listener = CoronaLua.isListener(L, Constants.listenerIndex2, Constants.eventName) ? CoronaLua.newRef(L, Constants.listenerIndex2) : CoronaLua.REFNIL;

            if (L.type(1) == LuaType.TABLE || L.tableSize(1) != 0) {
                L.getField(1, "fileId");
                if (L.isString(-1)) {
                    fileId = L.toString(-1);
                    L.pop(1);
                } else {
                    sendDispatcher(listener, true, "listComments({fileId(String)}) expected", Constants.listComments, Constants.pluginName);
                    dispatchEvent(true, "listComments({fileId(String)}) expected", Constants.listComments, Constants.pluginName);
                    return 0;
                }
            } else {
                sendDispatcher(listener, true, "listComments({fileId(String)}) expected", Constants.listComments, Constants.pluginName);
                dispatchEvent(true, "listComments({fileId(String)}) expected", Constants.listComments, Constants.pluginName);
                return 0;
            }

            ArrayList<Comment> commentArrayList = new ArrayList<>();
            String nextCursor = null;
            try {
                Drive.Comments.List request = drive.comments().list(fileId);
                do {
                    if (nextCursor != null) {
                        request.setCursor(nextCursor);
                    }
                    CommentList commentList = request.setPageSize(100).setFields("*").execute();
                    ArrayList<Comment> comments = (ArrayList<Comment>) commentList.getComments();
                    if (comments == null) {
                        break;
                    }
                    commentArrayList.addAll(comments);
                    nextCursor = commentList.getNextCursor();
                } while (!StringUtils.isNullOrEmpty(nextCursor));

                sendDispatcher(listener, false, "", Constants.listComments, Constants.pluginName, commentToJsonArray(commentArrayList));
                dispatchEvent(false, "", Constants.listComments, Constants.pluginName, commentToJsonArray(commentArrayList));
            } catch (IOException e) {
                sendDispatcher(listener, true, "comments list error: " + e.toString(), Constants.listComments, Constants.pluginName);
                dispatchEvent(true, "comments list error: " + e.toString(), Constants.listComments, Constants.pluginName);
            }

            return 0;
        }
    }

    private static class getComments implements NamedJavaFunction {
        @Override
        public String getName() {
            return Constants.getComments;
        }

        @Override
        public int invoke(LuaState L) {
            if (CoronaEnvironment.getCoronaActivity() == null) {
                return 0;
            }

            String fileId, commentId;
            final int listener = CoronaLua.isListener(L, Constants.listenerIndex2, Constants.eventName) ? CoronaLua.newRef(L, Constants.listenerIndex2) : CoronaLua.REFNIL;

            if (L.type(1) == LuaType.TABLE || L.tableSize(1) != 0) {
                L.getField(1, "fileId");
                if (L.isString(-1)) {
                    fileId = L.toString(-1);
                    L.pop(1);
                } else {
                    sendDispatcher(listener, true, "getComments({fileId(String), commentId(String)}) expected", Constants.getComments, Constants.pluginName);
                    dispatchEvent(true, "getComments({fileId(String), commentId(String)}) expected", Constants.getComments, Constants.pluginName);
                    return 0;
                }
                L.getField(1, "commentId");
                if (L.isString(-1)) {
                    commentId = L.toString(-1);
                    L.pop(1);
                } else {
                    sendDispatcher(listener, true, "getComments({fileId(String), commentId(String)}) expected", Constants.getComments, Constants.pluginName);
                    dispatchEvent(true, "getComments({fileId(String), commentId(String)}) expected", Constants.getComments, Constants.pluginName);
                    return 0;
                }
            } else {
                sendDispatcher(listener, true, "getComments({fileId(String), commentId(String)}) expected", Constants.getComments, Constants.pluginName);
                dispatchEvent(true, "getComments({fileId(String), commentId(String)}) expected", Constants.getComments, Constants.pluginName);
                return 0;
            }

            try {
                Comment comment = drive.comments().get(fileId, commentId).setFields("*").execute();
                sendDispatcher(listener, false, "", Constants.getComments, Constants.pluginName, commentToJson(comment));
                dispatchEvent(false, "", Constants.getComments, Constants.pluginName, commentToJson(comment));
            } catch (Exception ex) {
                sendDispatcher(listener, true, "getComments error: " + ex.toString(), Constants.getComments, Constants.pluginName);
                dispatchEvent(true, "getComments error: " + ex.toString(), Constants.getComments, Constants.pluginName);
            }
            return 0;
        }
    }

    private static class updateComments implements NamedJavaFunction {
        @Override
        public String getName() {
            return Constants.updateComments;
        }

        @Override
        public int invoke(LuaState L) {
            if (CoronaEnvironment.getCoronaActivity() == null) {
                return 0;
            }

            String fileId, commentId, description;
            final int listener = CoronaLua.isListener(L, Constants.listenerIndex2, Constants.eventName) ? CoronaLua.newRef(L, Constants.listenerIndex2) : CoronaLua.REFNIL;

            if (L.type(1) == LuaType.TABLE || L.tableSize(1) != 0) {
                L.getField(1, "fileId");
                if (L.isString(-1)) {
                    fileId = L.toString(-1);
                    L.pop(1);
                } else {
                    sendDispatcher(listener, true, "updateComments({fileId(String), commentId(String), description(String)}) expected", Constants.updateComments, Constants.pluginName);
                    dispatchEvent(true, "updateComments({fileId(String), commentId(String), description(String)}) expected", Constants.updateComments, Constants.pluginName);
                    return 0;
                }

                L.getField(1, "commentId");
                if (L.isString(-1)) {
                    commentId = L.toString(-1);
                    L.pop(1);
                } else {
                    sendDispatcher(listener, true, "updateComments({fileId(String), commentId(String), description(String)}) expected", Constants.updateComments, Constants.pluginName);
                    dispatchEvent(true, "updateComments({fileId(String), commentId(String), description(String)}) expected", Constants.updateComments, Constants.pluginName);
                    return 0;
                }

                L.getField(1, "description");
                if (L.isString(-1)) {
                    description = L.toString(-1);
                    L.pop(1);
                } else {
                    sendDispatcher(listener, true, "updateComments({fileId(String), commentId(String), description(String)}) expected", Constants.updateComments, Constants.pluginName);
                    dispatchEvent(true, "updateComments({fileId(String), commentId(String), description(String)}) expected", Constants.updateComments, Constants.pluginName);
                    return 0;
                }
            } else {
                sendDispatcher(listener, true, "updateComments({fileId(String), commentId(String), description(String)}) expected", Constants.updateComments, Constants.pluginName);
                dispatchEvent(true, "updateComments({fileId(String), commentId(String), description(String)}) expected", Constants.updateComments, Constants.pluginName);
                return 0;
            }

            try {
                Comment content = new Comment();
                content.setDescription(description);
                drive.comments().update(fileId, commentId, content).execute();
                sendDispatcher(listener, false, "", Constants.updateComments, Constants.pluginName);
                dispatchEvent(false, "", Constants.updateComments, Constants.pluginName);
            } catch (Exception ex) {
                sendDispatcher(listener, true, "updateComments error: " + ex.toString(), Constants.updateComments, Constants.pluginName);
                dispatchEvent(true, "updateComments error: " + ex.toString(), Constants.updateComments, Constants.pluginName);
            }

            return 0;
        }
    }

    private static class deleteComments implements NamedJavaFunction {
        @Override
        public String getName() {
            return Constants.deleteComments;
        }

        @Override
        public int invoke(LuaState L) {
            if (CoronaEnvironment.getCoronaActivity() == null) {
                return 0;
            }

            String fileId, commentId;

            final int listener = CoronaLua.isListener(L, Constants.listenerIndex2, Constants.eventName) ? CoronaLua.newRef(L, Constants.listenerIndex2) : CoronaLua.REFNIL;

            if (L.type(1) == LuaType.TABLE || L.tableSize(1) != 0) {
                L.getField(1, "fileId");
                if (L.isString(-1)) {
                    fileId = L.toString(-1);
                    L.pop(1);
                } else {
                    sendDispatcher(listener, true, "deleteComments({fileId(String), commentId(String)}) expected", Constants.deleteComments, Constants.pluginName);
                    dispatchEvent(true, "deleteComments({fileId(String), commentId(String)}) expected", Constants.deleteComments, Constants.pluginName);
                    return 0;
                }

                L.getField(1, "commentId");
                if (L.isString(-1)) {
                    commentId = L.toString(-1);
                    L.pop(1);
                } else {
                    sendDispatcher(listener, true, "deleteComments({fileId(String), commentId(String)}) expected", Constants.deleteComments, Constants.pluginName);
                    dispatchEvent(true, "deleteComments({fileId(String), commentId(String)}) expected", Constants.deleteComments, Constants.pluginName);
                    return 0;
                }
            } else {
                sendDispatcher(listener, true, "deleteComments({fileId(String), commentId(String)}) expected", Constants.deleteComments, Constants.pluginName);
                dispatchEvent(true, "deleteComments({fileId(String), commentId(String)}) expected", Constants.deleteComments, Constants.pluginName);
                return 0;
            }

            try {
                drive.comments().delete(fileId, commentId).execute();
                sendDispatcher(listener, false, "", Constants.deleteComments, Constants.pluginName);
                dispatchEvent(false, "", Constants.deleteComments, Constants.pluginName);
            } catch (Exception ex) {
                sendDispatcher(listener, true, "deleteComments error: " + ex.toString(), Constants.deleteComments, Constants.pluginName);
                dispatchEvent(true, "deleteComments error: " + ex.toString(), Constants.deleteComments, Constants.pluginName);
            }

            return 0;
        }
    }

    // Reply Operations //
    private static class createReplies implements NamedJavaFunction {
        @Override
        public String getName() {
            return Constants.createReplies;
        }

        @Override
        public int invoke(LuaState L) {
            if (CoronaEnvironment.getCoronaActivity() == null) {
                return 0;
            }

            String fileId, commentId, description;


            final int listener = CoronaLua.isListener(L, Constants.listenerIndex2, Constants.eventName) ? CoronaLua.newRef(L, Constants.listenerIndex2) : CoronaLua.REFNIL;

            if (L.type(1) == LuaType.TABLE || L.tableSize(1) != 0) {
                L.getField(1, "fileId");
                if (L.isString(-1)) {
                    fileId = L.toString(-1);
                    L.pop(1);
                } else {
                    sendDispatcher(listener, true, "createReplies({fileId(String), commentId(String), description(String)}) expected", Constants.createReplies, Constants.pluginName);
                    dispatchEvent(true, "createReplies({fileId(String), commentId(String), description(String)}) expected", Constants.createReplies, Constants.pluginName);
                    return 0;
                }

                L.getField(1, "commentId");
                if (L.isString(-1)) {
                    commentId = L.toString(-1);
                    L.pop(1);
                } else {
                    sendDispatcher(listener, true, "createReplies({fileId(String), commentId(String), description(String)}) expected", Constants.createReplies, Constants.pluginName);
                    dispatchEvent(true, "createReplies({fileId(String), commentId(String), description(String)}) expected", Constants.createReplies, Constants.pluginName);
                    return 0;
                }

                L.getField(1, "description");
                if (L.isString(-1)) {
                    description = L.toString(-1);
                    L.pop(1);
                } else {
                    sendDispatcher(listener, true, "createReplies({fileId(String), commentId(String), description(String)}) expected", Constants.createReplies, Constants.pluginName);
                    dispatchEvent(true, "createReplies({fileId(String), commentId(String), description(String)}) expected", Constants.createReplies, Constants.pluginName);
                    return 0;
                }
            } else {
                sendDispatcher(listener, true, "createReplies({fileId(String), commentId(String), description(String)}) expected", Constants.createReplies, Constants.pluginName);
                dispatchEvent(true, "createReplies({fileId(String), commentId(String), description(String)}) expected", Constants.createReplies, Constants.pluginName);
                return 0;
            }

            try {
                Reply reply = new Reply();
                reply.setDescription(description);
                Reply replyResponse = drive.replies().create(fileId, commentId, reply).setFields("*").execute();
                sendDispatcher(listener, false, "", Constants.getReplies, Constants.pluginName, replyToJson(replyResponse));
                dispatchEvent(false, "", Constants.getReplies, Constants.pluginName, replyToJson(replyResponse));
            } catch (Exception ex) {
                sendDispatcher(listener, true, "createReplies error: " + ex.toString(), Constants.createReplies, Constants.pluginName);
                dispatchEvent(true, "createReplies error: " + ex.toString(), Constants.createReplies, Constants.pluginName);
            }

            return 0;
        }
    }

    private static class listReplies implements NamedJavaFunction {
        @Override
        public String getName() {
            return Constants.listReplies;
        }

        @Override
        public int invoke(LuaState L) {
            if (CoronaEnvironment.getCoronaActivity() == null) {
                return 0;
            }

            String fileId, commentId;
            List<Reply> replyResponse = new ArrayList<>();
            String nextCursor = null;

            final int listener = CoronaLua.isListener(L, Constants.listenerIndex2, Constants.eventName) ? CoronaLua.newRef(L, Constants.listenerIndex2) : CoronaLua.REFNIL;

            if (L.type(1) == LuaType.TABLE || L.tableSize(1) != 0) {
                L.getField(1, "fileId");
                if (L.isString(-1)) {
                    fileId = L.toString(-1);
                    L.pop(1);
                } else {
                    sendDispatcher(listener, true, "getHistoryVersions({fileId(String), commentId(String)}) expected", Constants.listReplies, Constants.pluginName);
                    dispatchEvent(true, "getHistoryVersions({fileId(String), commentId(String)}) expected", Constants.listReplies, Constants.pluginName);
                    return 0;
                }

                L.getField(1, "commentId");
                if (L.isString(-1)) {
                    commentId = L.toString(-1);
                    L.pop(1);
                } else {
                    sendDispatcher(listener, true, "getHistoryVersions({fileId(String), commentId(String)}) expected", Constants.listReplies, Constants.pluginName);
                    dispatchEvent(true, "getHistoryVersions({fileId(String), commentId(String)}) expected", Constants.listReplies, Constants.pluginName);
                    return 0;
                }
            } else {
                sendDispatcher(listener, true, "getHistoryVersions({fileId(String), commentId(String)}) expected", Constants.listReplies, Constants.pluginName);
                dispatchEvent(true, "getHistoryVersions({fileId(String), commentId(String)}) expected", Constants.listReplies, Constants.pluginName);
                return 0;
            }

            try {
                Drive.Replies.List listReq = drive.replies().list(fileId, commentId);
                do {
                    if (nextCursor != null) {
                        listReq.setCursor(nextCursor);
                    }
                    ReplyList replyList = listReq.setFields("*").execute();
                    List<Reply> replies = replyList.getReplies();
                    if (replies == null) {
                        break;
                    }
                    replyResponse.addAll(replies);
                    nextCursor = replyList.getNextCursor();
                } while (StringUtils.isNullOrEmpty(nextCursor));

                sendDispatcher(listener, false, "", Constants.listReplies, Constants.pluginName, replyToJsonArray(replyResponse));
                dispatchEvent(false, "", Constants.listReplies, Constants.pluginName, replyToJsonArray(replyResponse));
            } catch (Exception ex) {
                sendDispatcher(listener, true, "listReplies error: " + ex.toString(), Constants.listReplies, Constants.pluginName);
                dispatchEvent(true, "listReplies error: " + ex.toString(), Constants.listReplies, Constants.pluginName);
            }

            return 0;
        }
    }

    private static class getReplies implements NamedJavaFunction {
        @Override
        public String getName() {
            return Constants.getReplies;
        }

        @Override
        public int invoke(LuaState L) {
            if (CoronaEnvironment.getCoronaActivity() == null) {
                return 0;
            }

            String fileId, commentId, replyId;

            final int listener = CoronaLua.isListener(L, Constants.listenerIndex2, Constants.eventName) ? CoronaLua.newRef(L, Constants.listenerIndex2) : CoronaLua.REFNIL;

            if (L.type(1) == LuaType.TABLE || L.tableSize(1) != 0) {
                L.getField(1, "fileId");
                if (L.isString(-1)) {
                    fileId = L.toString(-1);
                    L.pop(1);
                } else {
                    sendDispatcher(listener, true, "getReplies({fileId(String), commentId(String), replyId(String)}) expected", Constants.getReplies, Constants.pluginName);
                    dispatchEvent(true, "getReplies({fileId(String), commentId(String), replyId(String)}) expected", Constants.getReplies, Constants.pluginName);
                    return 0;
                }

                L.getField(1, "commentId");
                if (L.isString(-1)) {
                    commentId = L.toString(-1);
                    L.pop(1);
                } else {
                    sendDispatcher(listener, true, "getReplies({fileId(String), commentId(String), replyId(String)}) expected", Constants.getReplies, Constants.pluginName);
                    dispatchEvent(true, "getReplies({fileId(String), commentId(String), replyId(String)}) expected", Constants.getReplies, Constants.pluginName);
                    return 0;
                }

                L.getField(1, "replyId");
                if (L.isString(-1)) {
                    replyId = L.toString(-1);
                    L.pop(1);
                } else {
                    sendDispatcher(listener, true, "getReplies({fileId(String), commentId(String), replyId(String)}) expected", Constants.getReplies, Constants.pluginName);
                    dispatchEvent(true, "getReplies({fileId(String), commentId(String), replyId(String)}) expected", Constants.getReplies, Constants.pluginName);
                    return 0;
                }
            } else {
                sendDispatcher(listener, true, "getReplies({fileId(String), commentId(String), replyId(String)}) expected", Constants.getReplies, Constants.pluginName);
                dispatchEvent(true, "getReplies({fileId(String), commentId(String), replyId(String)}) expected", Constants.getReplies, Constants.pluginName);
                return 0;
            }

            try {
                Reply reply = drive.replies().get(fileId, commentId, replyId).setFields("*").execute();
                sendDispatcher(listener, false, "getReplies({fileId(String), commentId(String), replyId(String)}) expected", Constants.getReplies, Constants.pluginName, replyToJson(reply));
                dispatchEvent(false, "getReplies({fileId(String), commentId(String), replyId(String)}) expected", Constants.getReplies, Constants.pluginName, replyToJson(reply));
            } catch (Exception ex) {
                sendDispatcher(listener, true, "getReplies error: " + ex.toString(), Constants.getReplies, Constants.pluginName);
                dispatchEvent(true, "getReplies error: " + ex.toString(), Constants.getReplies, Constants.pluginName);
            }

            return 0;
        }
    }

    private static class updateReplies implements NamedJavaFunction {
        @Override
        public String getName() {
            return Constants.updateReplies;
        }

        @Override
        public int invoke(LuaState L) {
            if (CoronaEnvironment.getCoronaActivity() == null) {
                return 0;
            }

            String fileId, commentId, replyId, description;
            final int listener = CoronaLua.isListener(L, Constants.listenerIndex2, Constants.eventName) ? CoronaLua.newRef(L, Constants.listenerIndex2) : CoronaLua.REFNIL;

            if (L.type(1) == LuaType.TABLE || L.tableSize(1) != 0) {
                L.getField(1, "fileId");
                if (L.isString(-1)) {
                    fileId = L.toString(-1);
                    L.pop(1);
                } else {
                    sendDispatcher(listener, true, "fileId({fileId(String), commentId(String), replyId(String), description(String)}) expected", Constants.updateReplies, Constants.pluginName);
                    dispatchEvent(true, "fileId({fileId(String), commentId(String), replyId(String), description(String)}) expected", Constants.updateReplies, Constants.pluginName);
                    return 0;
                }

                L.getField(1, "commentId");
                if (L.isString(-1)) {
                    commentId = L.toString(-1);
                    L.pop(1);
                } else {
                    sendDispatcher(listener, true, "fileId({fileId(String), commentId(String), replyId(String), description(String)}) expected", Constants.updateReplies, Constants.pluginName);
                    dispatchEvent(true, "fileId({fileId(String), commentId(String), replyId(String), description(String)}) expected", Constants.updateReplies, Constants.pluginName);
                    return 0;
                }

                L.getField(1, "replyId");
                if (L.isString(-1)) {
                    replyId = L.toString(-1);
                    L.pop(1);
                } else {
                    sendDispatcher(listener, true, "fileId({fileId(String), commentId(String), replyId(String), description(String)}) expected", Constants.updateReplies, Constants.pluginName);
                    dispatchEvent(true, "fileId({fileId(String), commentId(String), replyId(String), description(String)}) expected", Constants.updateReplies, Constants.pluginName);
                    return 0;
                }

                L.getField(1, "description");
                if (L.isString(-1)) {
                    description = L.toString(-1);
                    L.pop(1);
                } else {
                    sendDispatcher(listener, true, "fileId({fileId(String), commentId(String), replyId(String), description(String)}) expected", Constants.updateReplies, Constants.pluginName);
                    dispatchEvent(true, "fileId({fileId(String), commentId(String), replyId(String), description(String)}) expected", Constants.updateReplies, Constants.pluginName);
                    return 0;
                }
            } else {
                sendDispatcher(listener, true, "fileId({fileId(String), commentId(String), replyId(String), description(String)}) expected", Constants.updateReplies, Constants.pluginName);
                dispatchEvent(true, "fileId({fileId(String), commentId(String), replyId(String), description(String)}) expected", Constants.updateReplies, Constants.pluginName);
                return 0;
            }

            try {
                Reply reply = new Reply();
                reply.setDescription(description);
                drive.replies().update(fileId, commentId, replyId, reply).execute();
                sendDispatcher(listener, false, "", Constants.updateReplies, Constants.pluginName);
                dispatchEvent(false, "", Constants.updateReplies, Constants.pluginName);
            } catch (Exception ex) {
                sendDispatcher(listener, true, "updateReplies error: " + ex.toString(), Constants.updateReplies, Constants.pluginName);
                dispatchEvent(true, "updateReplies error: " + ex.toString(), Constants.updateReplies, Constants.pluginName);
            }

            return 0;
        }
    }

    private static class deleteReplies implements NamedJavaFunction {
        @Override
        public String getName() {
            return Constants.deleteReplies;
        }

        @Override
        public int invoke(LuaState L) {
            if (CoronaEnvironment.getCoronaActivity() == null) {
                return 0;
            }

            String fileId, commentId, replyId;
            final int listener = CoronaLua.isListener(L, Constants.listenerIndex2, Constants.eventName) ? CoronaLua.newRef(L, Constants.listenerIndex2) : CoronaLua.REFNIL;

            if (L.type(1) == LuaType.TABLE || L.tableSize(1) != 0) {
                L.getField(1, "fileId");
                if (L.isString(-1)) {
                    fileId = L.toString(-1);
                    L.pop(1);
                } else {
                    sendDispatcher(listener, true, "fileId({fileId(String), commentId(String), replyId(String)}) expected", Constants.deleteReplies, Constants.pluginName);
                    dispatchEvent(true, "fileId({fileId(String), commentId(String), replyId(String)}) expected", Constants.deleteReplies, Constants.pluginName);
                    return 0;
                }

                L.getField(1, "commentId");
                if (L.isString(-1)) {
                    commentId = L.toString(-1);
                    L.pop(1);
                } else {
                    sendDispatcher(listener, true, "fileId({fileId(String), commentId(String), replyId(String)}) expected", Constants.deleteReplies, Constants.pluginName);
                    dispatchEvent(true, "fileId({fileId(String), commentId(String), replyId(String)}) expected", Constants.deleteReplies, Constants.pluginName);
                    return 0;
                }

                L.getField(1, "replyId");
                if (L.isString(-1)) {
                    replyId = L.toString(-1);
                    L.pop(1);
                } else {
                    sendDispatcher(listener, true, "fileId({fileId(String), commentId(String), replyId(String)}) expected", Constants.deleteReplies, Constants.pluginName);
                    dispatchEvent(true, "fileId({fileId(String), commentId(String), replyId(String)}) expected", Constants.deleteReplies, Constants.pluginName);
                    return 0;
                }
            } else {
                sendDispatcher(listener, true, "fileId({fileId(String), commentId(String), replyId(String)}) expected", Constants.deleteReplies, Constants.pluginName);
                dispatchEvent(true, "fileId({fileId(String), commentId(String), replyId(String)}) expected", Constants.deleteReplies, Constants.pluginName);
                return 0;
            }

            try {
                drive.replies().delete(fileId, commentId, replyId).execute();
                sendDispatcher(listener, false, "", Constants.deleteReplies, Constants.pluginName);
                dispatchEvent(false, "", Constants.deleteReplies, Constants.pluginName);
            } catch (Exception ex) {
                sendDispatcher(listener, true, "deleteReplies error => " + ex.toString(), Constants.deleteReplies, Constants.pluginName);
                dispatchEvent(true, "deleteReplies error => " + ex.toString(), Constants.deleteReplies, Constants.pluginName);
            }

            return 0;
        }
    }

    // Historical File Versions Operations //
    private static class listHistoryVersions implements NamedJavaFunction {
        @Override
        public String getName() {
            return Constants.listHistoryVersions;
        }

        @Override
        public int invoke(LuaState L) {
            if (CoronaEnvironment.getCoronaActivity() == null) {
                return 0;
            }

            String fileId;
            final int listener = CoronaLua.isListener(L, Constants.listenerIndex2, Constants.eventName) ? CoronaLua.newRef(L, Constants.listenerIndex2) : CoronaLua.REFNIL;

            if (L.type(1) == LuaType.TABLE || L.tableSize(1) != 0) {
                L.getField(1, "fileId");
                if (L.isString(-1)) {
                    fileId = L.toString(-1);
                    L.pop(1);
                } else {
                    sendDispatcher(listener, true, "listHistoryVersions {String} expected", Constants.listHistoryVersions, Constants.pluginName);
                    dispatchEvent(true, "listHistoryVersions {String} expected", Constants.listHistoryVersions, Constants.pluginName);
                    return 0;
                }
            } else {
                sendDispatcher(listener, true, "listHistoryVersions {String} expected", Constants.listHistoryVersions, Constants.pluginName);
                dispatchEvent(true, "listHistoryVersions {String} expected", Constants.listHistoryVersions, Constants.pluginName);
                return 0;
            }

            try {
                HistoryVersionList historyVersionList = drive.historyVersions().list(fileId).setFields("*").execute();
                sendDispatcher(listener, false, "", Constants.listHistoryVersions, Constants.pluginName, historyVersionListToJson(historyVersionList));
                dispatchEvent(false, "", Constants.listHistoryVersions, Constants.pluginName);
            } catch (Exception ex) {
                sendDispatcher(listener, true, "listHistoryVersions error: " + ex.toString(), Constants.listHistoryVersions, Constants.pluginName);
                dispatchEvent(true, "listHistoryVersions error: " + ex.toString(), Constants.listHistoryVersions, Constants.pluginName);
            }

            return 0;
        }
    }

    private static class getHistoryVersions implements NamedJavaFunction {
        @Override
        public String getName() {
            return Constants.getHistoryVersions;
        }

        @Override
        public int invoke(LuaState L) {
            if (CoronaEnvironment.getCoronaActivity() == null) {
                return 0;
            }

            String fileId, historyVersionId;
            final int listener = CoronaLua.isListener(L, Constants.listenerIndex2, Constants.eventName) ? CoronaLua.newRef(L, Constants.listenerIndex2) : CoronaLua.REFNIL;

            if (L.type(1) == LuaType.TABLE || L.tableSize(1) != 0) {
                L.getField(1, "fileId");
                if (L.isString(-1)) {
                    fileId = L.toString(-1);
                    L.pop(1);
                } else {
                    sendDispatcher(listener, true, "getHistoryVersions({fileId(String), historyVersionId(String)}) expected", Constants.getHistoryVersions, Constants.pluginName);
                    dispatchEvent(true, "getHistoryVersions({fileId(String), historyVersionId(String)}) expected", Constants.getHistoryVersions, Constants.pluginName);
                    return 0;
                }

                L.getField(1, "historyVersionId");
                if (L.isString(-1)) {
                    historyVersionId = L.toString(-1);
                    L.pop(1);
                } else {
                    sendDispatcher(listener, true, "getHistoryVersions({fileId(String), historyVersionId(String)}) expected", Constants.getHistoryVersions, Constants.pluginName);
                    dispatchEvent(true, "getHistoryVersions({fileId(String), historyVersionId(String)}) expected", Constants.getHistoryVersions, Constants.pluginName);
                    return 0;
                }
            } else {
                sendDispatcher(listener, true, "getHistoryVersions({fileId(String), historyVersionId(String)}) expected", Constants.getHistoryVersions, Constants.pluginName);
                dispatchEvent(true, "getHistoryVersions({fileId(String), historyVersionId(String)}) expected", Constants.getHistoryVersions, Constants.pluginName);
                return 0;
            }

            try {
                HistoryVersion historyVersion = drive.historyVersions().get(fileId, historyVersionId).execute();
                sendDispatcher(listener, false, "", Constants.getHistoryVersions, Constants.pluginName, historyVersionToJson(historyVersion));
                dispatchEvent(false, "", Constants.getHistoryVersions, Constants.pluginName);
            } catch (Exception e) {
                sendDispatcher(listener, true, "getHistoryVersions error => " + e.getMessage(), Constants.getHistoryVersions, Constants.pluginName);
                dispatchEvent(true, "getHistoryVersions error => " + e.getMessage(), Constants.getHistoryVersions, Constants.pluginName);
            }

            return 0;
        }
    }

    private static class deleteHistoryVersions implements NamedJavaFunction {
        @Override
        public String getName() {
            return Constants.deleteHistoryVersions;
        }

        @Override
        public int invoke(LuaState L) {
            if (CoronaEnvironment.getCoronaActivity() == null) {
                return 0;
            }

            String fileId, historyVersionId;
            final int listener = CoronaLua.isListener(L, Constants.listenerIndex2, Constants.eventName) ? CoronaLua.newRef(L, Constants.listenerIndex2) : CoronaLua.REFNIL;

            if (L.type(1) == LuaType.TABLE || L.tableSize(1) != 0) {
                L.getField(1, "fileId");
                if (L.isString(-1)) {
                    fileId = L.toString(-1);
                    L.pop(1);
                } else {
                    sendDispatcher(listener, true, "deleteHistoryVersions({fileId(String), historyVersionId(String)}) expected", Constants.deleteHistoryVersions, Constants.pluginName);
                    dispatchEvent(true, "deleteHistoryVersions({fileId(String), historyVersionId(String)}) expected", Constants.deleteHistoryVersions, Constants.pluginName);
                    return 0;
                }

                L.getField(1, "historyVersionId");
                if (L.isString(-1)) {
                    historyVersionId = L.toString(-1);
                    L.pop(1);
                } else {
                    sendDispatcher(listener, true, "deleteHistoryVersions({fileId(String), historyVersionId(String)}) expected", Constants.deleteHistoryVersions, Constants.pluginName);
                    dispatchEvent(true, "deleteHistoryVersions({fileId(String), historyVersionId(String)}) expected", Constants.deleteHistoryVersions, Constants.pluginName);
                    return 0;
                }
            } else {
                sendDispatcher(listener, true, "deleteHistoryVersions({fileId(String), historyVersionId(String)}) expected", Constants.deleteHistoryVersions, Constants.pluginName);
                dispatchEvent(true, "deleteHistoryVersions({fileId(String), historyVersionId(String)}) expected", Constants.deleteHistoryVersions, Constants.pluginName);
                return 0;
            }

            try {
                drive.historyVersions().delete(fileId, historyVersionId).execute();
                sendDispatcher(listener, false, "", Constants.deleteHistoryVersions, Constants.pluginName);
                dispatchEvent(false, "", Constants.deleteHistoryVersions, Constants.pluginName);
            } catch (Exception ex) {
                sendDispatcher(listener, true, "deleteHistoryVersions error => " + ex.getMessage(), Constants.deleteHistoryVersions, Constants.pluginName);
                dispatchEvent(true, "deleteHistoryVersions error => " + ex.getMessage(), Constants.deleteHistoryVersions, Constants.pluginName);
            }

            return 0;
        }
    }

    public static void dispatchEvent(final Boolean isError, final String message, final String type, final String provider) {
        CoronaEnvironment.getCoronaActivity().getRuntimeTaskDispatcher().send(new CoronaRuntimeTask() {
            @Override
            public void executeUsing(CoronaRuntime runtime) {
                LuaState L = runtime.getLuaState();

                CoronaLua.newEvent(L, EVENT_NAME);

                L.pushString(message);
                L.setField(-2, "message");

                L.pushBoolean(isError);
                L.setField(-2, "isError");

                L.pushString(type);
                L.setField(-2, "type");

                L.pushString(provider);
                L.setField(-2, "provider");

                try {
                    CoronaLua.dispatchEvent(L, fListener, 0);
                } catch (Exception ignored) {
                }
            }
        });
    }

    public static void dispatchEvent(final Boolean isError, final String message, final String type, final String provider, final JSONObject data) {
        CoronaEnvironment.getCoronaActivity().getRuntimeTaskDispatcher().send(new CoronaRuntimeTask() {
            @Override
            public void executeUsing(CoronaRuntime runtime) {
                LuaState L = runtime.getLuaState();

                CoronaLua.newEvent(L, EVENT_NAME);

                L.pushString(message);
                L.setField(-2, "message");

                L.pushBoolean(isError);
                L.setField(-2, "isError");

                L.pushString(type);
                L.setField(-2, "type");

                L.pushString(provider);
                L.setField(-2, "provider");

                L.pushString(data.toString());
                L.setField(-2, "data");
                try {
                    CoronaLua.dispatchEvent(L, fListener, 0);
                } catch (Exception ignored) {
                }

            }
        });
    }

    public static void dispatchEvent(final Boolean isError, final String message, final String type, final String provider, final JSONArray data) {
        CoronaEnvironment.getCoronaActivity().getRuntimeTaskDispatcher().send(new CoronaRuntimeTask() {
            @Override
            public void executeUsing(CoronaRuntime runtime) {
                LuaState L = runtime.getLuaState();

                CoronaLua.newEvent(L, EVENT_NAME);

                L.pushString(message);
                L.setField(-2, "message");

                L.pushBoolean(isError);
                L.setField(-2, "isError");

                L.pushString(type);
                L.setField(-2, "type");

                L.pushString(provider);
                L.setField(-2, "provider");

                L.pushString(data.toString());
                L.setField(-2, "data");
                try {
                    CoronaLua.dispatchEvent(L, fListener, 0);
                } catch (Exception ignored) {
                }

            }
        });
    }

    public static void sendDispatcher(final int listener, final boolean isError, final String message, final String type, final String provider) {
        fDispatcher.send(new CoronaRuntimeTask() {
            @Override
            public void executeUsing(CoronaRuntime coronaRuntime) {
                if (listener != CoronaLua.REFNIL) {
                    LuaState L = coronaRuntime.getLuaState();
                    try {
                        CoronaLua.newEvent(L, Constants.eventName);

                        L.pushString(message);
                        L.setField(-2, "message");

                        L.pushBoolean(isError);
                        L.setField(-2, "isError");

                        L.pushString(type);
                        L.setField(-2, "type");

                        L.pushString(provider);
                        L.setField(-2, "provider");

                        CoronaLua.dispatchEvent(L, listener, 0);

                    } catch (Exception ex) {
                        Log.i(Constants.TAG, "Corona Error:", ex);

                    } finally {
                        CoronaLua.deleteRef(L, listener);
                    }
                }
            }
        });
    }

    public static void sendDispatcher(final int listener, final boolean isError, final String message, final String type, final String provider, final JSONArray jsonArray) {
        fDispatcher.send(new CoronaRuntimeTask() {
            @Override
            public void executeUsing(CoronaRuntime coronaRuntime) {
                if (listener != CoronaLua.REFNIL) {
                    LuaState L = coronaRuntime.getLuaState();
                    try {
                        CoronaLua.newEvent(L, Constants.eventName);

                        L.pushString(message);
                        L.setField(-2, "message");

                        L.pushBoolean(isError);
                        L.setField(-2, "isError");

                        L.pushString(type);
                        L.setField(-2, "type");

                        L.pushString(provider);
                        L.setField(-2, "provider");

                        L.pushString(jsonArray.toString());
                        L.setField(-2, "data");

                        CoronaLua.dispatchEvent(L, listener, 0);
                    } catch (Exception ex) {
                        Log.i(Constants.TAG, "Corona Error:", ex);
                    } finally {
                        CoronaLua.deleteRef(L, listener);
                    }
                }
            }
        });
    }

    public static void sendDispatcher(final int listener, final boolean isError, final String message, final String type, final String provider, final JSONObject jsonObject) {
        fDispatcher.send(new CoronaRuntimeTask() {
            @Override
            public void executeUsing(CoronaRuntime coronaRuntime) {
                if (listener != CoronaLua.REFNIL) {
                    LuaState L = coronaRuntime.getLuaState();
                    try {
                        CoronaLua.newEvent(L, Constants.eventName);

                        L.pushString(message);
                        L.setField(-2, "message");

                        L.pushBoolean(isError);
                        L.setField(-2, "isError");

                        L.pushString(type);
                        L.setField(-2, "type");

                        L.pushString(provider);
                        L.setField(-2, "provider");

                        L.pushString(jsonObject.toString());
                        L.setField(-2, "data");

                        CoronaLua.dispatchEvent(L, listener, 0);
                    } catch (Exception ex) {
                        Log.i(Constants.TAG, "Corona Error:", ex);
                    } finally {
                        CoronaLua.deleteRef(L, listener);
                    }
                }
            }
        });
    }

    public static void sendDispatcher(final int listener, final boolean isError, final String message, final String type, final String provider, final JSONObject jsonObject, final Object javaObject) {
        fDispatcher.send(new CoronaRuntimeTask() {
            @Override
            public void executeUsing(CoronaRuntime coronaRuntime) {
                if (listener != CoronaLua.REFNIL) {
                    LuaState L = coronaRuntime.getLuaState();
                    try {
                        CoronaLua.newEvent(L, Constants.eventName);

                        L.pushString(message);
                        L.setField(-2, "message");

                        L.pushBoolean(isError);
                        L.setField(-2, "isError");

                        L.pushString(type);
                        L.setField(-2, "type");

                        L.pushString(provider);
                        L.setField(-2, "provider");

                        L.pushString(jsonObject.toString());
                        L.setField(-2, "data");

                        L.pushJavaObject(javaObject);
                        L.setField(-2, "javaObject");

                        CoronaLua.dispatchEvent(L, listener, 0);
                    } catch (Exception ex) {
                        Log.i(Constants.TAG, "Corona Error:", ex);
                    } finally {
                        CoronaLua.deleteRef(L, listener);
                    }
                }
            }
        });
    }
}
