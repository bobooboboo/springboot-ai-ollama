package com.boboo.ai;

import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

/**
 * ai控制器
 *
 * @author: boboo
 * @Date: 2024/7/5 15:56
 **/
@RestController
public class AiController {

    @Autowired
    private OllamaApi api;

    @GetMapping("/ollama/chat")
    public String chat(@RequestParam("prompt") String prompt) {
        OllamaApi.GenerateRequest request = OllamaApi.GenerateRequest.builder(prompt).withStream(Boolean.FALSE).withModel("qwen2").build();
        OllamaApi.GenerateResponse response = api.generate(request);
        return response.response();
    }

    public static void main(String[] args) {
        OllamaApi.ChatRequest request = OllamaApi.ChatRequest.builder("qwen2")
                .withMessages(Collections.singletonList(new OllamaApi.Message(OllamaApi.Message.Role.USER, "你是谁", null))).withStream(false).build();
        OllamaApi.ChatResponse response = new OllamaApi("http://192.168.1.12:11435").chat(request);
        System.out.println(response.message().content());
    }
}
