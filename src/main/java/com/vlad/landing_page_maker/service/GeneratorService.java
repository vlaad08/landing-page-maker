package com.vlad.landing_page_maker.service;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class GeneratorService {
    private final ChatModel chatModel;

    private final String template = """
        You are a senior software engineer generating a project.

        Generate only raw file blocks.
        Do not include explanations.
        Do not include markdown code fences.
        Do not include any text before the first FILE_NAME line.
        Do not include any text after the last -----BREAK----- separator.

        Business name: {name}
        Business context: {context}

        Output format for every file:
        FILE_NAME = <relative-path-and-filename>
        <full file content>
        -----BREAK-----
        """;

    public GeneratorService(ChatModel chatModel)
    {
        this.chatModel = chatModel;
    }

    public byte[] generate(String context, String siteName) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(byteArrayOutputStream);
             ZipOutputStream zipOutputStream = new ZipOutputStream(bufferedOutputStream)) {

            String generatedFiles = generateFiles(context, siteName);
            Map<String, byte[]> files = breakApartFiles(generatedFiles);

            for (Map.Entry<String, byte[]> entry : files.entrySet()) {
                zipOutputStream.putNextEntry(new ZipEntry(entry.getKey()));

                try (InputStream stream = new ByteArrayInputStream(entry.getValue())) {
                    IOUtils.copy(stream, zipOutputStream);
                }

                zipOutputStream.closeEntry();
            }

            zipOutputStream.finish();
        }

        return byteArrayOutputStream.toByteArray();
    }

    private String generateFiles(String context, String siteName){
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(template);
        Message systemMessage = systemPromptTemplate.createMessage(Map.of("name", siteName, "context", context));

        Prompt prompt = new Prompt(systemMessage);
        return chatModel.call(prompt).getResult().getOutput().getText();
    }

    private Map<String, byte[]> breakApartFiles(String project) {
        Map<String, byte[]> fileMap = new HashMap<>();

        String[] chunks = project.split("-----BREAK-----");

        for (String chunk : chunks) {

            String trimmed = chunk.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            String[] lines = trimmed.split("\n", 2);

            if (lines.length < 2) {
                System.out.println("Skipping malformed chunk: " + trimmed);
                continue;
            }

            String header = lines[0].trim();

            if (!header.startsWith("FILE_NAME")) {
                System.out.println("Skipping chunk without filename: " + header);
                continue;
            }

            String[] nameParts = header.split("=", 2);

            if (nameParts.length < 2) {
                System.out.println("Skipping invalid filename header: " + header);
                continue;
            }

            String name = nameParts[1].trim();
            String content = lines[1];

            fileMap.put(name, content.getBytes(StandardCharsets.UTF_8));
        }

        return fileMap;
    }
}
