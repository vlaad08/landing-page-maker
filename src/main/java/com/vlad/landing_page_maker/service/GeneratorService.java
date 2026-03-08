package com.vlad.landing_page_maker.service;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class GeneratorService {
    private final ChatModel chatModel;


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
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(getPrompt());
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

    private String getPrompt(){
        return """
                You are a senior software engineer generating a complete runnable frontend project.
                
                Your task:
                Generate a complete, polished business web application using React, Vite, and JavaScript.
                
                The app may be either:
                - a single-page application, or
                - a multi-page application with client-side routing
                
                Choose the structure that best fits the business context.
                Use a multi-page structure when the business naturally benefits from separate pages such as About, Services, Contact, Menu, Portfolio, Booking, or FAQ.
                Otherwise, use a well-structured single-page application.
                
                I will provide the business name and business context.
                Use them to generate the app’s content, structure, and design.
                
                Requirements:
                
                1. Planning
                Before generating code, decide the complete project structure internally.
                Determine all files and directories before writing code.
                Once the structure is decided, do not add extra files or omit planned files in the final output.
                
                2. Technology and architecture
                - Use React with JavaScript and functional components only
                - Use Vite-compatible project structure
                - Use plain CSS only, with a single global src/index.css and additional component or page CSS files only when necessary
                - Do not use Tailwind, Bootstrap, Material UI, styled-components, Sass, or any CSS framework
                - Do not use server-side rendering
                - Do not use backend code
                - Do not use environment variables
                - Do not use dangerouslySetInnerHTML
                - Do not use eval, Function constructor, or injected scripts
                - Do not fetch remote JavaScript
                - Do not add unnecessary dependencies
                
                Allowed dependencies:
                - react
                - react-dom
                - vite
                - react-router-dom only if routing is used
                
                3. File organization
                - Do not place the whole application in App.jsx
                - Extract repeated UI into reusable components
                - Keep responsibilities separated by file
                - Do not over-abstract
                - Put page components in src/pages when routing is used
                - Put reusable UI components in src/components
                - Create src/data/siteContent.js if structured content is needed
                - Create src/utils only if truly necessary
                - If routing is used, create a Layout component with header, footer, and Outlet
                - If routing is used, use BrowserRouter, Routes, Route, Link, and NavLink appropriately
                - If routing is used, include a 404 page
                
                4. UI and content quality
                - The app must be responsive and mobile-first
                - Use semantic HTML and accessible markup
                - The design should feel modern, intentional, and realistic for the business
                - Include substantial business-relevant content
                - Avoid lorem ipsum, generic filler, and vague marketing text
                - Include sections appropriate to the business, such as hero, about, services, trust/value proposition, CTA, and footer
                - Include light interactivity where it improves the experience, but keep all data local and static
                - Do not invent external assets unless you also create them
                - Do not reference files, images, fonts, or icons that are not included in the output
                - Do not invent fake contact details unless clearly supported by the provided context
                
                5. Code quality
                - The project must be complete and runnable
                - Every import must resolve correctly
                - The code must compile without missing files or symbols
                - Keep the code readable and production-minded
                
                6. Required core files
                At minimum, include:
                - package.json
                - index.html
                - src/main.jsx
                - src/App.jsx
                - src/index.css
                
                Add other files only as needed by the chosen architecture.
                
                7. Output format
                Return only raw file blocks in exactly this format:
                
                FILE_NAME = <relative path and filename>
                <full file content>
                -----BREAK-----
                
                Do not include markdown code fences.
                Do not include explanations.
                Do not include any text before the first FILE_NAME line.
                Do not include any text after the last -----BREAK----- line.
                Do not generate empty files.
                Do not include the string -----BREAK----- inside file contents.
                
                Business name: {name}
                Business context: {context}
                """;
    }
}
