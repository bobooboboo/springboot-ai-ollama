package com.boboo.llm.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天响应类
 *
 * @author: boboo
 * @Date: 2024/7/9 16:47
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponseDTO {
    /**
     * 响应内容
     */
    private String content;

    /**
     * 会话id
     */
    private String sessionId;
}
