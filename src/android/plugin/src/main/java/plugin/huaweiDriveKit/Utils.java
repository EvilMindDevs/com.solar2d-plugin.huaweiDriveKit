package plugin.huaweiDriveKit;

import android.util.Log;

import com.huawei.cloud.services.drive.model.Comment;
import com.huawei.cloud.services.drive.model.File;
import com.huawei.cloud.services.drive.model.HistoryVersion;
import com.huawei.cloud.services.drive.model.HistoryVersionList;
import com.huawei.cloud.services.drive.model.Reply;
import com.huawei.cloud.services.drive.model.User;
import com.huawei.hms.support.api.entity.auth.Scope;
import com.huawei.hms.support.hwid.result.AuthHuaweiId;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Objects;

class Utils {

    static JSONObject authHuaweiIdToJson(AuthHuaweiId authHuaweiId) {
        JSONObject _authHuaweiId = new JSONObject();
        try {
            _authHuaweiId.put("getAccessToken", authHuaweiId.getAccessToken());
            _authHuaweiId.put("getIdToken", authHuaweiId.getIdToken());
            _authHuaweiId.put("getDisplayName", authHuaweiId.getDisplayName());
            _authHuaweiId.put("getEmail", authHuaweiId.getEmail());
            _authHuaweiId.put("getFamilyName", authHuaweiId.getFamilyName());
            _authHuaweiId.put("getGivenName", authHuaweiId.getGivenName());
            _authHuaweiId.put("getAvatarUri", authHuaweiId.getAvatarUri());
            _authHuaweiId.put("getAuthorizationCode", authHuaweiId.getAuthorizationCode());
            _authHuaweiId.put("getUnionId", authHuaweiId.getUnionId());
            _authHuaweiId.put("getOpenId", authHuaweiId.getOpenId());
            _authHuaweiId.put("getHuaweiAccount", authHuaweiId.getHuaweiAccount());
            _authHuaweiId.put("getAuthorizedScopes", authHuaweiId.getAuthorizedScopes());
            _authHuaweiId.put("getAgeRangeFlag", authHuaweiId.getAgeRangeFlag());
            _authHuaweiId.put("getAgeRange", authHuaweiId.getAgeRange());
            _authHuaweiId.put("getCountryCode", authHuaweiId.getCountryCode());
            _authHuaweiId.put("getGender", authHuaweiId.getGender());
            _authHuaweiId.put("getUid", authHuaweiId.getUid());
            _authHuaweiId.put("getHomeZone", authHuaweiId.getHomeZone());
            JSONArray AuthorizedScopes = new JSONArray();
            for (Scope scope : authHuaweiId.getAuthorizedScopes()) {
                JSONObject AuthorizedScope = new JSONObject();
                AuthorizedScope.put("getScopeUri", scope.getScopeUri());
                AuthorizedScope.put("describeContents", scope.describeContents());
                AuthorizedScope.put("hashCode", scope.hashCode());
                AuthorizedScope.put("toString", scope.toString());
                AuthorizedScopes.put(AuthorizedScope);
            }
            _authHuaweiId.put("getAuthorizedScopes", AuthorizedScopes);
        } catch (JSONException e) {
            Log.i(Constants.TAG, Objects.requireNonNull(e.getMessage()));
        }
        return _authHuaweiId;
    }

    // Comment
    static JSONArray commentToJsonArray(List<Comment> data) {
        JSONArray commentArray = new JSONArray();
        for (Comment comment : data) {
            commentArray.put(commentToJson(comment));
        }
        return commentArray;
    }

    static JSONObject commentToJson(Comment comment) {
        JSONObject _comment = new JSONObject();
        try {
            _comment.put("Position", comment.getPosition());
            _comment.put("Creator", comment.getCreator());
            _comment.put("Description", comment.getDescription());
            _comment.put("CreatedTime", comment.getCreatedTime());
            _comment.put("Deleted", comment.getDeleted());
            _comment.put("HtmlDescription", comment.getHtmlDescription());
            _comment.put("Id", comment.getId());
            _comment.put("Category", comment.getCategory());
            _comment.put("EditedTime", comment.getEditedTime());
            _comment.put("Replies", replyToJsonArray(comment.getReplies()));
            _comment.put("Resolved", comment.getResolved());
            _comment.put("Position", comment.getQuotedContent());
        } catch (Exception e) {
            Log.e(Constants.TAG, Objects.requireNonNull(e.getMessage()));
        }
        return _comment;
    }

    // HistoryVerison
    static JSONObject historyVersionListToJson(HistoryVersionList historyVersionList) {
        JSONObject _historyVersionList = new JSONObject();
        try {
            _historyVersionList.put("Category", historyVersionList.getCategory());
            _historyVersionList.put("NextCursor", historyVersionList.getNextCursor());
            _historyVersionList.put("HistoryVersions", historyVersionToJsonArray(historyVersionList.getHistoryVersions()));
        } catch (Exception e) {
            Log.e(Constants.TAG, Objects.requireNonNull(e.getMessage()));
        }
        return _historyVersionList;
    }

    private static JSONArray historyVersionToJsonArray(List<HistoryVersion> data) {
        JSONArray historyVersionArray = new JSONArray();
        for (HistoryVersion historyVersion : data) {
            historyVersionArray.put(historyVersionToJson(historyVersion));
        }
        return historyVersionArray;
    }

    static JSONObject historyVersionToJson(HistoryVersion historyVersion) {
        JSONObject _historyVersion = new JSONObject();
        try {
            _historyVersion.put("Id", historyVersion.getId());
            _historyVersion.put("KeepPermanent", historyVersion.getKeepPermanent());
            _historyVersion.put("Category", historyVersion.getCategory());
            _historyVersion.put("LastEditor", userToJson(historyVersion.getLastEditor()));
            _historyVersion.put("Sha256", historyVersion.getSha256());
            _historyVersion.put("MimeType", historyVersion.getMimeType());
            _historyVersion.put("EditedTime", historyVersion.getEditedTime());
            _historyVersion.put("OriginalFilename", historyVersion.getOriginalFilename());
            _historyVersion.put("Size", historyVersion.getSize());
        } catch (Exception e) {
            Log.e(Constants.TAG, Objects.requireNonNull(e.getMessage()));
        }
        return _historyVersion;
    }

    // User
    private static JSONArray userToJsonArray(List<User> user) {
        JSONArray userArray = new JSONArray();
        for (User data : user) {
            userArray.put(userToJson(data));
        }
        return userArray;
    }

    private static JSONObject userToJson(User user) {
        JSONObject _user = new JSONObject();
        try {
            _user.put("Category", user.getCategory());
            _user.put("DisplayName", user.getDisplayName());
            _user.put("Me", user.getMe());
            _user.put("PermissionId", user.getPermissionId());
            _user.put("ProfilePhotoLink", user.getProfilePhotoLink());
            _user.put("UserAccount", user.getUserAccount());
        } catch (Exception e) {
            Log.e(Constants.TAG, Objects.requireNonNull(e.getMessage()));
        }
        return _user;
    }

    // Reply
    static JSONArray replyToJsonArray(List<Reply> data) {
        JSONArray replyArray = new JSONArray();
        for (Reply reply : data) {
            replyArray.put(replyToJson(reply));
        }
        return replyArray;
    }

    static JSONObject replyToJson(Reply reply) {
        JSONObject _reply = new JSONObject();
        try {
            _reply.put("Operate", reply.getOperate());
            _reply.put("Creator", reply.getCreator());
            _reply.put("Description", reply.getDescription());
            _reply.put("CreatedTime", reply.getCreatedTime());
            _reply.put("Deleted", reply.getDeleted());
            _reply.put("HtmlDescription", reply.getHtmlDescription());
            _reply.put("Id", reply.getId());
            _reply.put("Category", reply.getCategory());
            _reply.put("EditedTime", reply.getEditedTime());
        } catch (Exception e) {
            Log.e(Constants.TAG, Objects.requireNonNull(e.getMessage()));
        }
        return _reply;
    }

    // File
    static JSONObject fileToJson(File file) {
        JSONObject _file = new JSONObject();
        try {
            _file.put("Category", file.getCategory());
            _file.put("Id", file.getId());
            _file.put("FileName", file.getFileName());
            _file.put("Size", file.getSize());
            _file.put("MimeType", file.getMimeType());
            _file.put("ParentFolder", file.getParentFolder().toString());
            _file.put("CreatedTime", file.getCreatedTime());
            _file.put("EditedTime", file.getEditedTime());
            _file.put("Description", file.getDescription());
            _file.put("Owners", userToJsonArray(file.getOwners()));
        } catch (Exception e) {
            Log.e(Constants.TAG, Objects.requireNonNull(e.getMessage()));
        }
        return _file;
    }

    static JSONArray fileToJsonArray(List<File> data) {
        JSONArray fileArray = new JSONArray();
        for (File file : data) {
            fileArray.put(fileToJson(file));
        }
        return fileArray;
    }


}
