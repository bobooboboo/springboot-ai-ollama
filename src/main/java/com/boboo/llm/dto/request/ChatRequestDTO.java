package com.boboo.llm.dto.request;

import lombok.Data;

/**
 * 聊天请求类
 *
 * @author: boboo
 * @Date: 2024/7/9 16:06
 **/
@Data
public class ChatRequestDTO {
    /**
     * 聊天内容
     */
    private String prompt;

    /**
     * 会话Id，用来维持聊天上下文
     */
    private String sessionId;

    /**
     * 指定的model，默认为qwen2
     */
    private String model;
}
