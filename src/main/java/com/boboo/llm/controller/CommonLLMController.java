package com.boboo.llm.controller;

import com.boboo.llm.dto.request.ChatRequestDTO;
import com.boboo.llm.dto.response.ChatResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.RequestResponseAdvisor;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

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
     * 聊天，非流式响应
     *
     * @param requestDTO 聊天请求类
     */
    @PostMapping(value = "/chat")
    public ChatResponseDTO chat(@RequestBody ChatRequestDTO requestDTO) {
        String content = getChatClientRequest(requestDTO)
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
    public Flux<ServerSentEvent<ChatResponseDTO>> chatStream(@RequestBody ChatRequestDTO requestDTO) {
        return getChatClientRequest(requestDTO)
                .stream()
                .content()
                .map(content -> ServerSentEvent.builder(new ChatResponseDTO(content, requestDTO.getSessionId()))
                        .event("message")
                        .build());
    }

    /**
     * 构建ChatClientRequest
     *
     * @param requestDTO 聊天请求类
     */
    private ChatClient.ChatClientRequest getChatClientRequest(ChatRequestDTO requestDTO) {
        if (!StringUtils.hasText(requestDTO.getSessionId())) {
            requestDTO.setSessionId(UUID.randomUUID().toString());
        }
        // 1. 如果需要存储会话和消息到数据库，自己可以实现ChatMemory接口，
        //    这里使用InMemoryChatMemory，内存存储。
        // 2. 传入会话id，MessageChatMemoryAdvisor会根据会话id去查找消息。
        // 3. 只需要携带最近20条消息
        RequestResponseAdvisor messageChatMemoryAdvisor = new MessageChatMemoryAdvisor(chatMemory, requestDTO.getSessionId(), 20);
        ChatClient.ChatClientRequest chatClientRequest = ChatClient.builder(chatModel)
                .build()
                .prompt()
                .user(requestDTO.getPrompt())
                // MessageChatMemoryAdvisor会在消息发送给大模型之前，从ChatMemory中获取会话的历史消息，
                // 然后一起发送给大模型。
                .advisors(messageChatMemoryAdvisor);
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
