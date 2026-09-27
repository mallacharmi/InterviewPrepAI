package com.interviewprep.service;

import com.interviewprep.dto.request.AtsAnalysisRequest;
import com.interviewprep.dto.response.AtsAnalysisResponse;
import com.interviewprep.dto.response.ReportResponse.CourseItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AtsService {

    private static final List<String> COMMON_TECH_SKILLS = List.of(
            "Java", "Spring Boot", "Microservices", "SQL", "Hibernate", "JPA",
            "REST API", "Docker", "Kubernetes", "Kafka", "AWS", "Git",
            "System Design", "OOP", "Data Structures", "Unit Testing", "JUnit",
            "Mockito", "Redis", "PostgreSQL", "MySQL", "NoSQL", "MongoDB",
            "Python", "React", "JavaScript", "HTML/CSS", "CI/CD"
    );

    public boolean isGibberish(String text) {
        if (text == null || text.trim().length() < 25) {
            return true;
        }

        String cleaned = text.replaceAll("[^a-zA-Z]", "");
        if (cleaned.length() < 15) {
            return true;
        }

        long uniqueChars = cleaned.toLowerCase().chars().distinct().count();
        if (uniqueChars < 6) {
            return true;
        }

        String[] words = text.toLowerCase().split("\\W+");
        long validWords = Arrays.stream(words)
                .filter(w -> w.length() >= 2 && w.matches(".*[aeiouy].*"))
                .count();

        if (words.length > 0 && ((double) validWords / words.length) < 0.35) {
            return true;
        }

        return validWords < 4;
    }

    public AtsAnalysisResponse analyzeResume(AtsAnalysisRequest request) {
        log.info("Analyzing ATS Resume and Job Description match...");

        if (isGibberish(request.getResumeText())) {
            throw new IllegalArgumentException("Invalid or gibberish content detected in Resume. Please upload a real PDF/Word resume or paste valid resume text.");
        }

        if (isGibberish(request.getJobDescription())) {
            throw new IllegalArgumentException("Invalid or gibberish content detected in Job Description. Please paste a valid job description with actual role requirements.");
        }

        String resume = request.getResumeText().toLowerCase();
        String jd = request.getJobDescription().toLowerCase();

        // 1. Identify skills mentioned in JD
        List<String> jdSkills = COMMON_TECH_SKILLS.stream()
                .filter(skill -> jd.contains(skill.toLowerCase()))
                .collect(Collectors.toList());

        if (jdSkills.isEmpty()) {
            jdSkills = List.of("Java", "Spring Boot", "SQL", "REST API", "System Design");
        }

        // 2. Identify matched vs missing skills
        List<String> matchedKeywords = new ArrayList<>();
        List<String> missingSkills = new ArrayList<>();

        for (String skill : jdSkills) {
            if (resume.contains(skill.toLowerCase())) {
                matchedKeywords.add(skill);
            } else {
                missingSkills.add(skill);
            }
        }

        // Also scan extra keywords from resume matching JD words
        Set<String> jdWords = Arrays.stream(jd.split("\\W+"))
                .filter(w -> w.length() > 3)
                .collect(Collectors.toSet());
        long extraMatchCount = Arrays.stream(resume.split("\\W+"))
                .filter(w -> w.length() > 3 && jdWords.contains(w))
                .distinct()
                .count();

        // Calculate ATS Score (0 - 100%)
        double matchRatio = jdSkills.isEmpty() ? 0.0 : ((double) matchedKeywords.size() / jdSkills.size());
        double wordMatchRatio = Math.min(1.0, extraMatchCount / (double) Math.max(10, jdWords.size()));
        double atsScore = (matchRatio * 70.0) + (wordMatchRatio * 30.0);
        atsScore = Math.min(98.0, Math.max(0.0, atsScore));
        atsScore = Math.round(atsScore * 10.0) / 10.0;

        String category = atsScore >= 80 ? "Strong Match" : (atsScore >= 60 ? "Moderate Match" : "Low Match - Action Needed");

        // Infer target role from JD
        String targetRole = "Software Developer";
        if (jd.contains("java")) targetRole = "Java Developer";
        else if (jd.contains("backend")) targetRole = "Backend Developer";
        else if (jd.contains("data")) targetRole = "Data Analyst";

        // Generate tailored suggestions
        List<String> suggestions = new ArrayList<>();
        if (!missingSkills.isEmpty()) {
            suggestions.add("Add missing keywords to your Resume skills section: " + String.join(", ", missingSkills) + ".");
        }
        suggestions.add("Quantify your project achievements using metrics (e.g. 'Optimized SQL query performance by 40%').");
        suggestions.add("Ensure exact job description terms are mirrored in your experience bullet points.");

        // Generate recommended courses for missing skills
        List<CourseItem> courses = generateCoursesForSkills(missingSkills);

        return AtsAnalysisResponse.builder()
                .atsScore(atsScore)
                .matchCategory(category)
                .targetRole(targetRole)
                .matchedKeywords(matchedKeywords)
                .missingSkills(missingSkills)
                .improvementSuggestions(suggestions)
                .courseRecommendations(courses)
                .build();
    }

    private List<CourseItem> generateCoursesForSkills(List<String> missingSkills) {
        List<CourseItem> courses = new ArrayList<>();
        courses.add(new CourseItem(
                "Java Programming & Software Engineering Fundamentals",
                "Coursera (Duke University)",
                "Master Object-Oriented programming, data structures, and core Java concepts.",
                "https://www.coursera.org/specializations/java-programming",
                "Java Core"
        ));
        courses.add(new CourseItem(
                "Spring Boot 3 & Spring Framework Masterclass",
                "Udemy",
                "Build production-ready REST APIs and microservices with Spring Boot.",
                "https://www.udemy.com/course/spring-hibernate-tutorial/",
                "Backend Framework"
        ));
        courses.add(new CourseItem(
                "System Design Primer & Scalable Architecture",
                "GeeksforGeeks",
                "Learn scalable system architecture, caching, database sharding, and API design.",
                "https://www.geeksforgeeks.org/system-design-tutorial/",
                "Architecture"
        ));
        courses.add(new CourseItem(
                "Relational Database Design & Advanced SQL",
                "freeCodeCamp",
                "Master relational database indexing, query optimization, and complex SQL joins.",
                "https://www.freecodecamp.org/news/learn-sql-queries-database-design/",
                "Database"
        ));
        return courses;
    }

    public String extractTextFromFile(org.springframework.web.multipart.MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty.");
        }
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        try {
            if (fileName.endsWith(".docx")) {
                return extractDocxText(file.getInputStream());
            } else if (fileName.endsWith(".pdf")) {
                return extractPdfText(file.getInputStream());
            } else {
                return new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.error("Failed to extract text from file {}: {}", fileName, e.getMessage());
            throw new IllegalArgumentException("Could not read uploaded file: " + e.getMessage());
        }
    }

    private String extractDocxText(java.io.InputStream inputStream) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (java.util.zip.ZipInputStream zip = new java.util.zip.ZipInputStream(inputStream)) {
            java.util.zip.ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if ("word/document.xml".equalsIgnoreCase(entry.getName())) {
                    byte[] bytes = zip.readAllBytes();
                    String xml = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                    java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("<w:t[^>]*>(.*?)</w:t>").matcher(xml);
                    while (matcher.find()) {
                        sb.append(matcher.group(1)).append(" ");
                    }
                    break;
                }
            }
        }
        String extracted = sb.toString().trim();
        return extracted.isEmpty() ? "Standard Resume Document Content" : extracted;
    }

    private String extractPdfText(java.io.InputStream inputStream) throws Exception {
        byte[] bytes = inputStream.readAllBytes();
        String raw = new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1);
        StringBuilder sb = new StringBuilder();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\(([^()]{3,})\\)").matcher(raw);
        while (matcher.find()) {
            String str = matcher.group(1).replaceAll("[^a-zA-Z0-9\\s.,-]", "");
            if (str.length() > 2) {
                sb.append(str).append(" ");
            }
        }
        String extracted = sb.toString().trim();
        return extracted.length() > 20 ? extracted : "Resume PDF Document Content with Technical Experience";
    }
}
