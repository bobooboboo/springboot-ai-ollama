package com.boboo.llm.controller;

import cn.hutool.core.io.FileUtil;
import com.boboo.llm.dto.request.ChatRequestDTO;
import com.boboo.llm.dto.request.EmbeddingRequestDTO;
import com.boboo.llm.dto.response.ChatResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.RequestResponseAdvisor;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.messages.Media;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.vectorstore.RedisVectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

/**
 * 大模型控制器
 *
 * @author: boboo
 * @Date: 2024/7/5 15:56
 **/
@RestController
@RequiredArgsConstructor
public class CommonLLMController {

    /**
     * 内存聊天记录
     */
    private final ChatMemory chatMemory = new InMemoryChatMemory();

    /**
     * spring-ai chatModel ollama实现
     */
    private final OllamaChatModel chatModel;

    /**
     * redis向量数据库。向量数据库存储了人工智能模型不知道的数据，通常是企业的内部资料。
     */
    private final RedisVectorStore redisVectorStore;

    /**
     * 聊天，非流式响应
     *
     * @param requestDTO 聊天请求类
     */
    @PostMapping(value = "/chat")
    public ChatResponseDTO chat(ChatRequestDTO requestDTO, @RequestPart(value = "file", required = false) MultipartFile file) {
        String content = getChatClientRequest(requestDTO, file)
                .call()
                .content();
        return new ChatResponseDTO(content, requestDTO.getSessionId());
    }

    /**
     * 聊天，流式响应
     *
     * @param requestDTO 聊天请求类
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChatResponseDTO>> chatStream(ChatRequestDTO requestDTO, @RequestPart(value = "file", required = false) MultipartFile file) {
        return getChatClientRequest(requestDTO, file)
                .stream()
                .content()
                .map(content -> ServerSentEvent.builder(new ChatResponseDTO(content, requestDTO.getSessionId()))
                        .event("message")
                        .build());
    }

    /**
     * 将私有内容嵌入向量数据库
     *
     * @param list 私有的资料内容
     */
    @PostMapping(value = "/embedding")
    public Boolean embedding(@RequestBody List<EmbeddingRequestDTO> list) {
        if (!CollectionUtils.isEmpty(list)) {
            redisVectorStore.add(list.stream().map(EmbeddingRequestDTO::toDocument).toList());
            return true;
        }
        return false;
    }

    /**
     * 构建ChatClientRequest
     *
     * @param requestDTO 聊天请求类
     * @param file       文件
     */
    private ChatClient.ChatClientRequest getChatClientRequest(ChatRequestDTO requestDTO, MultipartFile file) {
        if (!StringUtils.hasText(requestDTO.getSessionId())) {
            requestDTO.setSessionId(UUID.randomUUID().toString());
        }
        // 1. 如果需要存储会话和消息到数据库，自己可以实现ChatMemory接口，这里使用InMemoryChatMemory，内存存储。
        // 2. 传入会话id，MessageChatMemoryAdvisor会根据会话id去查找消息。
        // 3. 只需要携带最近20条消息，可自定义。
        RequestResponseAdvisor messageChatMemoryAdvisor = new MessageChatMemoryAdvisor(chatMemory, requestDTO.getSessionId(), 20);
        // QuestionAnswerAdvisor可以在用户发起的提问时，先向数据库查询相关的文档，再把相关的文档拼接到用户的提问中，再让模型生成答案。
        RequestResponseAdvisor vectorStoreChatMemoryAdvisor = new QuestionAnswerAdvisor(redisVectorStore, SearchRequest.defaults().withSimilarityThreshold(0.875).withTopK(3));
        ChatClient.ChatClientRequest chatClientRequest = ChatClient.builder(chatModel).build()
                .prompt()
                .user(userSpec -> {
                    userSpec.text(requestDTO.getPrompt());
                    if (file != null) {
                        String mimeType = FileUtil.getMimeType(file.getOriginalFilename());
                        if (StringUtils.hasText(mimeType)) {
                            userSpec.media(new Media(MimeType.valueOf(mimeType), file.getResource()));
                        }
                    }
                })
                // MessageChatMemoryAdvisor会在消息发送给大模型之前，从ChatMemory中获取会话的历史消息，
                // 然后一起发送给大模型。
                .advisors(messageChatMemoryAdvisor
                        , vectorStoreChatMemoryAdvisor
                );
        addChatOptionsIfPossible(chatModel, chatClientRequest, requestDTO.getModel());
        return chatClientRequest;
    }

    /**
     * 设置模型参数
     */
    private void addChatOptionsIfPossible(ChatModel chatModel, ChatClient.ChatClientRequest chatClientRequest, String model) {
        if (chatModel instanceof OllamaChatModel && StringUtils.hasText(model)) {
            chatClientRequest.options(OllamaOptions.create().withModel(model));
        }
    }

}
