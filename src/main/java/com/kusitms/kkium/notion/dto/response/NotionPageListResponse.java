package com.kusitms.kkium.notion.dto.response;

import java.util.List;

public record NotionPageListResponse(String workspaceName, List<NotionPageInfo> pages) {

  public record NotionPageInfo(String pageId, String title, String icon, String type, String lastEditedTime) {}
}
