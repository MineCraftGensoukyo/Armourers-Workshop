package moe.plushie.armourers_workshop.library.data.global.task.user;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.exceptions.AuthenticationException;
import moe.plushie.armourers_workshop.library.data.global.MultipartForm;
import moe.plushie.armourers_workshop.library.data.global.auth.PlushieAuth;
import moe.plushie.armourers_workshop.library.data.global.auth.PlushieSession;
import moe.plushie.armourers_workshop.library.data.global.permission.PermissionSystem;
import moe.plushie.armourers_workshop.library.data.global.task.GlobalTask;
import moe.plushie.armourers_workshop.library.data.global.task.GlobalTaskResult;

import java.util.Date;

public class GlobalTaskSkinReport extends GlobalTask<GlobalTaskSkinReport.SkinReportResult> {

    private static final String URL = "user-skin-report.php";
    private final SkinReport report;

    public GlobalTaskSkinReport(SkinReport report) {
        super(PermissionSystem.PlushieAction.SKIN_REPORT, true);
        this.report = report;
    }

    @Override
    public SkinReportResult call() throws Exception {
        permissionCheck();
        if (!authenticateUser()) {
            throw new AuthenticationException();
        }
        PlushieSession plushieSession = PlushieAuth.PLUSHIE_SESSION;
        MultipartForm multipartForm = new MultipartForm(getBaseUrl() + URL);
        multipartForm.addText("userId", String.valueOf(plushieSession.getServerId()));
        multipartForm.addText("accessToken", plushieSession.getAccessToken());
        multipartForm.addText("reportSkinId", String.valueOf(report.getSkinId()));
        multipartForm.addText("reportType", report.getReportType().toString());
        multipartForm.addText("reportMessage", report.getMessage());

        String downloadString = multipartForm.upload();

        JsonObject jsonObject = new JsonParser().parse(downloadString).getAsJsonObject();
        if (jsonObject.has("valid")) {
            boolean valid = jsonObject.get("valid").getAsBoolean();
            if (valid) {
                return new SkinReportResult(GlobalTaskResult.SUCCESS);
            }
            if (jsonObject.has("reason")) {
                return new SkinReportResult(GlobalTaskResult.FAILED, jsonObject.get("reason").getAsString());
            }
        }
        return new SkinReportResult(GlobalTaskResult.FAILED, downloadString);
    }

    public static class SkinReport {

        /**
         * The user making the report.
         */
        private final int userId;

        /**
         * The skin being reported.
         */
        private final int skinId;

        /**
         * The type of report.
         */
        private final SkinReportType reportType;

        /**
         * Optional report message.
         */
        private final String message;

        /**
         * Report date.
         */
        private final Date date;

        public SkinReport(int userId, int skinId, SkinReportType reportType, String message, Date date) {
            this.userId = userId;
            this.skinId = skinId;
            this.reportType = reportType;
            this.message = message;
            this.date = date;
        }

        public SkinReport(int skinId, SkinReportType reportType, String message) {
            this.userId = -1;
            this.skinId = skinId;
            this.reportType = reportType;
            this.message = message;
            this.date = null;
        }

        public int getUserId() {
            return userId;
        }

        public int getSkinId() {
            return skinId;
        }

        public SkinReportType getReportType() {
            return reportType;
        }

        public String getMessage() {
            return message;
        }

        public Date getDate() {
            return date;
        }

        @Override
        public String toString() {
            return "SkinReport[userId=" + userId + ", skinId=" + skinId + ", reportType=" + reportType + ", message=" + message + ", date=" + date + "]";
        }

        public enum SkinReportType {
            SEXUAL, INAPPROPRIATE, STOLEN, SPAM, OTHER;

            public String getLangKey() {
                return "skin_report_type.armourers_workshop." + toString().toLowerCase();
            }
        }
    }

    public class SkinReportResult {

        private final GlobalTaskResult result;
        private final String message;

        public SkinReportResult(GlobalTaskResult result, String message) {
            this.result = result;
            this.message = message;
        }

        public SkinReportResult(GlobalTaskResult result) {
            this.result = result;
            this.message = null;
        }

        public GlobalTaskResult getResult() {
            return result;
        }

        public String getMessage() {
            return message;
        }
    }
}
