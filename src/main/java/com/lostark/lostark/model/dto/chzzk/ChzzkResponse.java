package com.lostark.lostark.model.dto.chzzk;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChzzkResponse {
    
    @JsonProperty("code")
    private Integer code;

    @JsonProperty("message")
    private String message;

    @JsonProperty("content")
    private Content content;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        @JsonProperty("data")
        private List<LiveDetail> data;

        @JsonProperty("page")
        private Page page;
    }

//    @Data
//    @NoArgsConstructor
//    @AllArgsConstructor
//    public static class Page {
//        @JsonProperty("next")
////        private String next;
//        private Object next;
//    }

@Data
@NoArgsConstructor
@AllArgsConstructor
public static class Page {
    @JsonProperty("next")
    private NextPage next;
}

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NextPage {
        @JsonProperty("concurrentUserCount")
        private Integer concurrentUserCount;

        @JsonProperty("liveId")
        private Long liveId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LiveDetail {
        @JsonProperty("liveTitle")
        private String liveTitle;

        @JsonProperty("liveImageUrl")
        private String liveImageUrl;

        @JsonProperty("defaultThumbnailImageUrl")
        private String defaultThumbnailImageUrl;

        @JsonProperty("concurrentUserCount")
        private Integer concurrentUserCount;

        @JsonProperty("liveCategory")
        private String liveCategory;

        @JsonProperty("liveCategoryValue")
        private String liveCategoryValue;

        @JsonProperty("channel")
        private Channel channel;

        // 뷰 템플릿 호환성을 위해 포맷팅 이미지 URL 제공
        public String getFormattedImageUrl() {
            String url = (liveImageUrl != null) ? liveImageUrl : defaultThumbnailImageUrl;
            if (url != null && url.contains("{type}")) {
                return url.replace("{type}", "480");
            }
            return url;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Channel {
        @JsonProperty("channelId")
        private String channelId;

        @JsonProperty("channelName")
        private String channelName;

        @JsonProperty("channelImageUrl")
        private String channelImageUrl;
    }
}
