package com.kusitms.kkium.notion.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NotionTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("workspace_id") String workspaceId,
        @JsonProperty("workspace_name") String workspaceName
) {}
