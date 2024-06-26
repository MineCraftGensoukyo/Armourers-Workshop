package moe.plushie.armourers_workshop.library.data.global.auth;

import com.google.gson.JsonObject;
import moe.plushie.armourers_workshop.init.ModLog;
import moe.plushie.armourers_workshop.library.data.global.permission.PermissionSystem;

import java.util.UUID;

public class PlushieSession {

    private PermissionSystem.PermissionGroup permissionGroup;
    private boolean isAuth;

    private int server_id;
    private String mc_id;
    private String mc_name;
    private String accessToken;
    private long accessTokenReceivedTime;
    private int accessTokenExpiryTime;
    private int permission_group_id;

    public PlushieSession() {
        server_id = 0;
        permission_group_id = -1;
    }

    public boolean authenticate(JsonObject jsonObject) {
        if (jsonObject != null) {
            if (jsonObject.has("valid")) {
                if (jsonObject.get("valid").getAsBoolean()) {
                    server_id = jsonObject.get("server_id").getAsInt();
                    mc_id = jsonObject.get("mc_id").getAsString();
                    mc_name = jsonObject.get("mc_name").getAsString();
                    setPermission_group_id(jsonObject.get("permission_group_id").getAsInt());
                    updateToken(jsonObject);
                    return true;
                }
            }
        }
        return false;
    }

    public void updateToken(JsonObject json) {
        if (json != null && json.has("valid")) {
            if (json.has("valid")) {
                if (json.has("accessToken") & json.has("expiryTime")) {
                    setAccessToken(json.get("accessToken").getAsString(), json.get("expiryTime").getAsInt());
                    isAuth = true;
                    return;
                }
            }
        }
        isAuth = false;
    }

    public int getServerId() {
        return server_id;
    }

    public void setServerId(int serverId) {
        this.server_id = serverId;
    }

    public String getMcName() {
        return mc_name;
    }

    public UUID getMcId() {
        return UUID.fromString(mc_id);
    }

    public int getPermissionGroupID() {
        return permission_group_id;
    }

    public boolean isOwner(int userId) {
        return this.server_id == userId;
    }

    public void setPermission_group_id(int permission_group_id) {
        this.permission_group_id = permission_group_id;
        updatePermissionGroup();
    }

    public boolean hasServerId() {
        return server_id > 0;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public boolean isAuthenticated() {
        if (isAuth) {
            return accessTokenReceivedTime + (accessTokenExpiryTime * 1000L) > System.currentTimeMillis();
        }
        return false;
    }

    public int getTokenExpiryTime() {
        return (accessTokenExpiryTime * 1000) - getTimeSinceTokenUpdate();
    }

    public int getTimeSinceTokenUpdate() {
        return (int) (System.currentTimeMillis() - accessTokenReceivedTime);
    }

    public void setAccessToken(String accessToken, int expiryTime) {
        this.accessToken = accessToken;
        this.accessTokenExpiryTime = expiryTime;
        accessTokenReceivedTime = System.currentTimeMillis();
    }

    public void updatePermissionGroup() {
        PermissionSystem ps = PermissionSystem.getInstance();
        if (!isAuthenticated()) {
            this.permissionGroup = ps.groupNoLogin;
        }
        switch (permission_group_id) {
            case 0:
                this.permissionGroup = ps.groupUser;
                break;
            case 1:
                this.permissionGroup = ps.groupMod;
                break;
            case 255:
                this.permissionGroup = ps.groupAdmin;
                break;
            default:
                this.permissionGroup = ps.groupNoLogin;
                break;
        }
        ModLog.debug("Permission group set to " + this.permissionGroup.getName());
    }

    public boolean hasPermission(PermissionSystem.PlushieAction action) {
        return permissionGroup.havePermission(action);
    }
}
