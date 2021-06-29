package plugin.huaweiDriveKit;

import android.content.Context;

import com.huawei.cloud.base.auth.DriveCredential;
import com.huawei.cloud.base.util.StringUtils;
import com.huawei.cloud.client.exception.DriveCode;

public class CredentialManager {

    private DriveCredential mCredential;

    private CredentialManager() {
    }

    private static class InnerHolder {
        private static CredentialManager sInstance = new CredentialManager();
    }

    static CredentialManager getInstance() {
        return InnerHolder.sInstance;
    }

    int init(String unionID, String at, DriveCredential.AccessMethod refreshAT) {
        if (StringUtils.isNullOrEmpty(unionID) || StringUtils.isNullOrEmpty(at)) {
            return DriveCode.ERROR;
        }
        DriveCredential.Builder builder = new DriveCredential.Builder(unionID, refreshAT);
        mCredential = builder.build().setAccessToken(at);
        return DriveCode.SUCCESS;
    }

    DriveCredential getCredential() {
        return mCredential;
    }

    public void exit(Context context) {
        // Delete cache files.
        deleteFile(context.getCacheDir());
        deleteFile(context.getFilesDir());
    }

    private static void deleteFile(java.io.File file) {
        if (null == file || !file.exists()) {
            return;
        }

        if (file.isDirectory()) {
            java.io.File[] files = file.listFiles();
            if (files != null) {
                for (java.io.File f : files) {
                    deleteFile(f);
                }
            }
        }
    }
}
