package com.kusitms.kkium.notion.dto.response;

import java.util.List;

public record NotionPageListResponse(List<NotionPageInfo> pages) {

  public record NotionPageInfo(String pageId, String title) {}
}
