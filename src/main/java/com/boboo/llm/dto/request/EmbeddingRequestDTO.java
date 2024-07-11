package com.boboo.llm.dto.request;

import lombok.Data;
import org.springframework.ai.document.Document;

import java.util.Map;

/**
 * 嵌入请求类
 * 用于将文本转换为嵌入向量
 *
 * @author: boboo
 * @Date: 2024/7/11 9:33
 **/
@Data
public class EmbeddingRequestDTO {

    /**
     * Unique ID
     */
    private String id;

    /**
     * Metadata for the document. It should not be nested and values should be restricted
     * to string, int, float, boolean for simple use with Vector Dbs.
     */
    private Map<String, Object> metadata;

    /**
     * Document content.
     */
    private String content;

    public Document toDocument() {
        return new Document(id, content, metadata);
    }
}
