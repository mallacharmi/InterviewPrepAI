package com.interviewprep.service.ai;

import com.interviewprep.dto.ai.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class MockAIService implements AIService {

    private final Random random = new Random();

    // Comprehensive ChatGPT/Claude style dynamic question templates
    private final Map<String, List<QuestionTemplate>> questionBank = Map.of(
            "OOP", List.of(
                    new QuestionTemplate(
                            "In designing a scalable backend system, how do you enforce Encapsulation vs Abstraction? Give a practical code structure example for a {ROLE}.",
                            "OOP", List.of("Data Hiding", "Interface Design", "Encapsulation", "Abstraction")
                    ),
                    new QuestionTemplate(
                            "Explain how Polymorphism and Dynamic Method Dispatch work at the JVM level. How does the JVM vtable optimize polymorphic method invocation?",
                            "OOP", List.of("JVM vtable", "Dynamic Dispatch", "Polymorphism", "Method Overriding")
                    ),
                    new QuestionTemplate(
                            "Walk me through the SOLID principles with emphasis on Dependency Inversion (DIP) and Open/Closed (OCP) in Spring framework applications.",
                            "OOP", List.of("SOLID", "Dependency Inversion", "Open/Closed Principle", "Spring IoC")
                    ),
                    new QuestionTemplate(
                            "Why is Composition preferred over Inheritance in modern software engineering? Describe a scenario where deep inheritance hierarchies caused maintenance issues.",
                            "OOP", List.of("Composition over Inheritance", "Coupling", "Reusability", "Design Patterns")
                    ),
                    new QuestionTemplate(
                            "How do Java 14+ Records enforce immutability and data encapsulation compared to traditional JavaBean classes with getters/setters?",
                            "OOP", List.of("Java Records", "Immutability", "Encapsulation", "Value Objects")
                    )
            ),
            "Collections", List.of(
                    new QuestionTemplate(
                            "Deep dive into HashMap internal implementation in Java 8+. Explain bucket hashing, collision resolution, and the treeification threshold when buckets convert to Red-Black Trees.",
                            "Collections", List.of("HashMap Internals", "Hash Collisions", "Red-Black Tree", "Bucket Rehash")
                    ),
                    new QuestionTemplate(
                            "Compare ConcurrentHashMap vs Collections.synchronizedMap. How does ConcurrentHashMap achieve high concurrency using Lock Striping (Java 7) vs CAS + synchronized node locks (Java 8+)?",
                            "Collections", List.of("ConcurrentHashMap", "Lock Striping", "CAS Operations", "Thread Safety")
                    ),
                    new QuestionTemplate(
                            "When should you choose ArrayDeque over LinkedList or Stack for stack/queue data structures in performance-critical applications?",
                            "Collections", List.of("ArrayDeque", "LinkedList", "Cache Locality", "Memory Overhead")
                    ),
                    new QuestionTemplate(
                            "Explain the fail-fast vs fail-safe iterator behaviors in Java Collections. How does CopyOnWriteArrayList handle concurrent modifications during iteration?",
                            "Collections", List.of("Fail-Fast", "Fail-Safe", "CopyOnWriteArrayList", "ConcurrentModificationException")
                    )
            ),
            "Multithreading", List.of(
                    new QuestionTemplate(
                            "Explain the Java Memory Model (JMM) guarantees provided by the 'volatile' keyword. How does it prevent instruction reordering and enforce visibility across CPU caches?",
                            "Multithreading", List.of("Volatile Keyword", "Java Memory Model", "Instruction Reordering", "CPU Cache Coherency")
                    ),
                    new QuestionTemplate(
                            "Compare Java 21 Virtual Threads (Project Loom) with traditional OS-bound Platform Threads. How do Virtual Threads handle blocking I/O without depleting carrier thread pools?",
                            "Multithreading", List.of("Virtual Threads", "Project Loom", "Carrier Threads", "Non-blocking I/O")
                    ),
                    new QuestionTemplate(
                            "How do ThreadPoolExecutor parameters (corePoolSize, maximumPoolSize, workQueue, RejectedExecutionHandler) operate when processing a sudden burst of asynchronous tasks?",
                            "Multithreading", List.of("ThreadPoolExecutor", "Work Queue", "Rejection Policies", "Task Schedulers")
                    ),
                    new QuestionTemplate(
                            "Differentiate ReentrantLock from synchronized blocks. Explain fair locking, tryLock with timeout, and Condition variables for thread synchronization.",
                            "Multithreading", List.of("ReentrantLock", "Synchronized Block", "Fair Lock", "Condition Variables")
                    )
            ),
            "Spring Boot", List.of(
                    new QuestionTemplate(
                            "How does Spring's @Transactional annotation work behind the scenes using Spring AOP proxies? Explain Transaction Propagation modes (REQUIRED vs REQUIRES_NEW) and rollback scenarios.",
                            "Spring Boot", List.of("@Transactional", "AOP Proxy", "Propagation Modes", "Transaction Manager")
                    ),
                    new QuestionTemplate(
                            "Walk through the Spring Bean Lifecycle from instantion to destruction. Explain where @PostConstruct, BeanPostProcessor, and Custom Initializers fit into the flow.",
                            "Spring Boot", List.of("Bean Lifecycle", "BeanPostProcessor", "@PostConstruct", "ApplicationContext")
                    ),
                    new QuestionTemplate(
                            "How do Spring Boot Auto-Configurations work via @EnableAutoConfiguration and META-INF/spring.factories (or AutoConfiguration.imports)?",
                            "Spring Boot", List.of("Auto-Configuration", "@ConditionalOnProperty", "Spring Boot Starters", "ImportSelectors")
                    )
            ),
            "Microservices", List.of(
                    new QuestionTemplate(
                            "How do you implement the Circuit Breaker and Rate Limiter patterns in Microservices using Resilience4j or Spring Cloud Gateway?",
                            "Microservices", List.of("Circuit Breaker", "Resilience4j", "Rate Limiting", "Fault Tolerance")
                    ),
                    new QuestionTemplate(
                            "Explain how Distributed Tracing (Micrometer Tracing / Zipkin) and Correlation IDs work across asynchronous microservice HTTP/gRPC boundaries.",
                            "Microservices", List.of("Distributed Tracing", "Trace ID", "Span ID", "Zipkin / Jaeger")
                    ),
                    new QuestionTemplate(
                            "Compare Event-Driven Microservice Messaging using Apache Kafka vs Synchronous REST API communication. How do you guarantee Saga transaction consistency across services?",
                            "Microservices", List.of("Saga Pattern", "Kafka", "Eventual Consistency", "Dual Writes")
                    )
            ),
            "Databases", List.of(
                    new QuestionTemplate(
                            "What is the N+1 Query Problem in Hibernate/JPA? Demonstrate how to diagnose and eliminate it using JOIN FETCH, Entity Graphs, and Batch Fetching.",
                            "Databases", List.of("N+1 Query Problem", "JOIN FETCH", "Entity Graph", "Hibernate Optimization")
                    ),
                    new QuestionTemplate(
                            "Explain Database Isolation Levels (Read Uncommitted, Read Committed, Repeatable Read, Serializable). What are Dirty Reads, Non-Repeatable Reads, and Phantom Reads?",
                            "Databases", List.of("ACID Properties", "Isolation Levels", "Phantom Reads", "Locking Strategies")
                    ),
                    new QuestionTemplate(
                            "Compare Optimistic Locking (@Version) vs Pessimistic Locking in JPA/Hibernate. In what high-concurrency production scenario would you select each approach?",
                            "Databases", List.of("Optimistic Locking", "Pessimistic Locking", "@Version", "Concurrency Control")
                    )
            ),
            "System Design", List.of(
                    new QuestionTemplate(
                            "Design a high-throughput Distributed Rate Limiter for an API Gateway processing 100,000 requests/sec. Compare Sliding Window Log, Token Bucket, and Redis-backed implementations.",
                            "System Design", List.of("Distributed Rate Limiting", "Token Bucket", "Sliding Window", "Redis Architecture")
                    ),
                    new QuestionTemplate(
                            "Explain Caching Strategies (Cache-Aside, Write-Through, Write-Behind) and how to handle Cache Avalanche, Cache Penetration, and Cache Stampede in production.",
                            "System Design", List.of("Caching Patterns", "Redis", "Cache Stampede", "Cache Penetration")
                    ),
                    new QuestionTemplate(
                            "How would you design a Scalable URL Shortener (like Bitly)? Detail the key generation strategy (Base62 vs Hash), database schema, caching layer, and redirects.",
                            "System Design", List.of("URL Shortener", "Base62 Encoding", "Database Sharding", "Scalability")
                    )
            ),
            "Behavioral", List.of(
                    new QuestionTemplate(
                            "Tell me about a time you experienced a major technical disagreement with a teammate or lead regarding software architecture. How did you resolve it and align on the final decision?",
                            "Behavioral", List.of("Conflict Resolution", "Technical Collaboration", "Decision Making", "Communication")
                    ),
                    new QuestionTemplate(
                            "Describe a production outage or critical bug you were tasked with resolving under high pressure. What was the root cause, how did you fix it, and what post-mortem steps did you take?",
                            "Behavioral", List.of("Incidents", "Root Cause Analysis", "Problem Solving", "Post-Mortem")
                    ),
                    new QuestionTemplate(
                            "Why are you interested in this {ROLE} position at our company, and what unique technical strengths and experiences do you bring to our team?",
                            "Behavioral", List.of("Role Motivation", "Self Awareness", "Strengths", "Team Fit")
                    ),
                    new QuestionTemplate(
                            "How do you handle tight project deadlines, unexpected scope changes, and high-stress workloads when multiple critical deliverables compete for your attention?",
                            "Behavioral", List.of("Time Management", "Prioritization", "Stress Management", "Agility")
                    ),
                    new QuestionTemplate(
                            "Can you tell me about a time when a project or feature you worked on failed or did not meet expectations? What went wrong, and what key lessons did you learn from the experience?",
                            "Behavioral", List.of("Accountability", "Growth Mindset", "Failure Analysis", "Resilience")
                    ),
                    new QuestionTemplate(
                            "Where do you see yourself professionally in the next 3 to 5 years, and what skills or engineering responsibilities are you actively working toward developing?",
                            "Behavioral", List.of("Career Vision", "Continuous Learning", "Ambition", "Goal Setting")
                    )
            )
    );

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public AIQuestionResponse generateQuestion(AIQuestionRequest request) {
        log.info("Generating dynamic AI question for topic: {}, role: {}, difficulty: {}",
                request.getTopic(), request.getTargetRole(), request.getDifficulty());

        String topicKey = findMatchingTopicKey(request.getTopic());
        List<QuestionTemplate> templates = questionBank.get(topicKey);

        if (templates != null && !templates.isEmpty()) {
            QuestionTemplate template = templates.get(random.nextInt(templates.size()));
            String formattedText = template.text.replace("{ROLE}", request.getTargetRole() != null ? request.getTargetRole() : "Software Engineer");
            
            // Inject dynamic scenario variations to guarantee question uniqueness like ChatGPT/Claude
            String[] variations = {
                    "",
                    " Focus on edge cases and performance implications.",
                    " Include concrete code architecture examples in your explanation.",
                    " Discuss the trade-offs between speed, scalability, and code readability.",
                    " Consider a high-concurrency production system environment."
            };
            String textWithVariation = formattedText + variations[random.nextInt(variations.length)];

            return new AIQuestionResponse(
                    textWithVariation,
                    request.getTopic(),
                    request.getDifficulty() != null ? request.getDifficulty() : "MEDIUM",
                    template.expectedConcepts
            );
        }

        // Generic fallback for custom topics
        String fallbackQuestion = String.format("As a %s, explain the fundamental principles, architectural trade-offs, and common pitfalls of %s in a production environment.",
                request.getTargetRole() != null ? request.getTargetRole() : "Software Engineer",
                request.getTopic() != null ? request.getTopic() : "General Computer Science");

        return new AIQuestionResponse(
                fallbackQuestion,
                request.getTopic() != null ? request.getTopic() : "General",
                request.getDifficulty() != null ? request.getDifficulty() : "MEDIUM",
                List.of("Core Concepts", "Best Practices", "Trade-offs", "Production Scalability")
        );
    }

    private String findMatchingTopicKey(String topic) {
        if (topic == null) return "OOP";
        String lower = topic.toLowerCase();
        if (lower.contains("oop") || lower.contains("java") || lower.contains("core")) return "OOP";
        if (lower.contains("collection") || lower.contains("array") || lower.contains("hash")) return "Collections";
        if (lower.contains("thread") || lower.contains("concur")) return "Multithreading";
        if (lower.contains("spring") || lower.contains("boot")) return "Spring Boot";
        if (lower.contains("micro") || lower.contains("api")) return "Microservices";
        if (lower.contains("sql") || lower.contains("data") || lower.contains("jpa") || lower.contains("hibernate")) return "Databases";
        if (lower.contains("design") || lower.contains("system")) return "System Design";
        if (lower.contains("behav") || lower.contains("hr") || lower.contains("soft")) return "Behavioral";
        return "OOP";
    }

    @Override
    public AIEvaluationResponse evaluateAnswer(AIEvaluationRequest request) {
        log.info("Evaluating answer for question: {}", request.getQuestionText());
        String answer = request.getAnswerText() != null ? request.getAnswerText().trim() : "";

        if (isGibberishOrInvalid(answer, request.getQuestionText())) {
            return new AIEvaluationResponse(
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    "Invalid or gibberish answer detected. The answer does not contain meaningful technical explanation related to the question.",
                    List.of("Core Technical Explanation", "Relevant Concepts", "Clarity & Depth"),
                    "Please write a technical response addressing the specific question concepts.",
                    false
            );
        }

        int wordCount = answer.split("\\s+").length;
        double baseScore = Math.min(9.5, 4.0 + (wordCount / 18.0));
        double techAcc = Math.min(9.8, baseScore + (random.nextDouble() * 1.0 - 0.3));
        double comp = Math.min(9.6, baseScore + (random.nextDouble() * 0.8 - 0.4));
        double clar = Math.min(9.7, baseScore + (random.nextDouble() * 0.6 - 0.2));
        double finalScore = Math.round(((techAcc + comp + clar) / 3.0) * 10.0) / 10.0;

        String feedback = "Solid response addressing key aspects of the question. You demonstrated good domain knowledge.";
        List<String> missing = List.of("Edge case performance analysis", "Production concurrency considerations");

        if (wordCount < 20) {
            feedback = "Your answer is brief. Adding concrete code snippets and architectural trade-offs will significantly improve your rating.";
            missing = List.of("Detailed Implementation Code", "Performance Considerations", "Trade-off Comparisons");
        }

        return new AIEvaluationResponse(
                finalScore,
                techAcc,
                comp,
                clar,
                feedback,
                missing,
                "Focus on articulating specific design patterns and memory management details.",
                finalScore < 7.0
        );
    }

    private boolean isGibberishOrInvalid(String answer, String questionText) {
        if (answer == null || answer.trim().length() < 8) {
            return true;
        }

        String cleaned = answer.replaceAll("[^a-zA-Z]", "");
        if (cleaned.length() < 5) return true;

        long uniqueChars = cleaned.toLowerCase().chars().distinct().count();
        if (uniqueChars < 5 && cleaned.length() > 8) {
            return true;
        }

        String[] words = answer.toLowerCase().split("\\s+");
        int gibberishWordCount = 0;
        for (String word : words) {
            String cleanWord = word.replaceAll("[^a-z]", "");
            if (cleanWord.isEmpty()) continue;
            if (cleanWord.length() >= 2 && !cleanWord.matches(".*[aeiou].*")) {
                gibberishWordCount++;
            }
            else if (cleanWord.matches(".*(.)\\1{2,}.*")) {
                gibberishWordCount++;
            }
        }

        if (words.length > 0 && ((double) gibberishWordCount / words.length) >= 0.3) {
            return true;
        }

        return false;
    }

    @Override
    public AIFollowUpResponse generateFollowUp(AIFollowUpRequest request) {
        log.info("Generating follow-up question for topic: {}", request != null ? request.getTopic() : null);
        String area = (request != null && request.getWeakAreas() != null && !request.getWeakAreas().isEmpty()) ? request.getWeakAreas().get(0) : "this concept";
        String topic = (request != null && request.getTopic() != null) ? request.getTopic() : "General";
        return new AIFollowUpResponse(
                "Can you elaborate on how " + area + " applies in a high-concurrency production scenario?",
                topic,
                List.of("Practical Application")
        );
    }

    @Override
    public String generateChatResponse(String systemPrompt, String userMessage, List<ChatMessageDto> history) {
        log.info("[MOCK AI SERVICE] Processing message: '{}'", userMessage);

        String lowerMsg = userMessage != null ? userMessage.toLowerCase() : "";
        String sysPromptLower = systemPrompt != null ? systemPrompt.toLowerCase() : "";

        if (sysPromptLower.contains("mode_context: resume_ats") || sysPromptLower.contains("resume_ats")) {
            return generateResumeMockReply(systemPrompt, userMessage, lowerMsg);
        } else if (sysPromptLower.contains("mode_context: session_review") || sysPromptLower.contains("session_review")) {
            return generateReviewMockReply(systemPrompt, userMessage, lowerMsg);
        } else if (sysPromptLower.contains("mode_context: prep_coach") || sysPromptLower.contains("prep_coach")) {
            return generatePrepMockReply(systemPrompt, userMessage, lowerMsg);
        }

        return "I am your AI assistant for InterviewPrep. How can I help you prepare today?";
    }

    private String generatePrepMockReply(String systemPrompt, String userMessage, String lowerMsg) {
        log.info("[MOCK AI PREP QUERY] Message: '{}'", userMessage);

        if (lowerMsg.contains("score") || lowerMsg.contains("past interview") || lowerMsg.contains("last interview") || lowerMsg.contains("session") || lowerMsg.contains("grade") || lowerMsg.contains("result")) {
            String reply = "In PREP mode, I provide general interview coaching and study guidance. I don't have access to past session score data here! To view your exact scores, submitted answers, and AI feedback for a past interview, please open the Report Card page for that specific session (REVIEW mode).";
            log.info("[PREP MODE QA VERIFICATION] Question Asked: '{}' | Answer Given: '{}'", userMessage, reply);
            return reply;
        }

        String sessionRole = "Software Engineer";
        if (systemPrompt != null && systemPrompt.contains("Session Configured Role: ")) {
            int idx = systemPrompt.indexOf("Session Configured Role: ");
            int end = systemPrompt.indexOf("\n", idx);
            if (end > idx) {
                sessionRole = systemPrompt.substring(idx + 25, end).trim();
            }
        } else if (systemPrompt != null && systemPrompt.contains("Target Role: ")) {
            int idx = systemPrompt.indexOf("Target Role: ");
            int end = systemPrompt.indexOf("\n", idx);
            if (end > idx) {
                sessionRole = systemPrompt.substring(idx + 13, end).trim();
            }
        }

        String userReqRole = null;
        if (systemPrompt != null && systemPrompt.contains("User Explicitly Requested Role in Message: ")) {
            int idx = systemPrompt.indexOf("User Explicitly Requested Role in Message: ");
            int end = systemPrompt.indexOf("\n", idx);
            if (end > idx) {
                userReqRole = systemPrompt.substring(idx + 43, end).trim();
            }
        }

        String effectiveRole = userReqRole != null ? userReqRole : sessionRole;
        String reply = null;

        // Check for role mismatch acknowledgment prefix
        String rolePrefix = "";
        if (userReqRole != null && !userReqRole.equalsIgnoreCase(sessionRole)) {
            rolePrefix = "Your current prep session role is configured as [" + sessionRole + "]. Since you asked specifically about a [" + userReqRole + "] interview:\n\n";
        }

        // --- SPECIFIC TECHNICAL CONCEPT ANSWERS (ISSUE 1 FIX) ---
        if (lowerMsg.contains("checked") && lowerMsg.contains("unchecked")) {
            reply = rolePrefix + "In Java, the key differences between Checked and Unchecked Exceptions are:\n" +
                    "1. Class Hierarchy: Checked exceptions inherit directly from java.lang.Exception (excluding RuntimeException). Unchecked exceptions inherit from java.lang.RuntimeException.\n" +
                    "2. Compile-Time vs Runtime Enforcement: Checked exceptions are checked at compile time — the compiler forces you to either catch them using try-catch or declare them using the 'throws' clause (e.g. IOException, SQLException). Unchecked exceptions occur at runtime and are not enforced by the compiler (e.g. NullPointerException, IndexOutOfBoundsException, IllegalArgumentException).\n" +
                    "3. Handling & Recovery: Use Checked exceptions for recoverable business/IO failures that callers should handle. Use Unchecked exceptions for programming errors or unrecoverable logic bugs.";
        } else if (lowerMsg.contains("arraylist") && lowerMsg.contains("linkedlist")) {
            reply = rolePrefix + "In Java Collections, the key differences between ArrayList and LinkedList are:\n" +
                    "1. Internal Data Structure: ArrayList is backed by a dynamically resizing contiguous array. LinkedList is a doubly-linked list of node objects.\n" +
                    "2. Random Access: ArrayList provides O(1) random index access via get(index). LinkedList requires O(N) traversal to reach an index.\n" +
                    "3. Insertion & Deletion: ArrayList requires O(N) array copying when inserting/deleting in the middle. LinkedList performs O(1) pointer updates once the node position is reached.\n" +
                    "4. Memory Footprint: ArrayList has minimal per-element memory overhead. LinkedList consumes higher memory per element due to node pointer objects.";
        } else if ((lowerMsg.contains("stringbuilder") || lowerMsg.contains("stringbuffer")) && lowerMsg.contains("string")) {
            reply = rolePrefix + "In Java, the key differences between String, StringBuilder, and StringBuffer are:\n" +
                    "1. Immutability: String objects are immutable — any modification creates a new String in memory. StringBuilder and StringBuffer objects are mutable and modify their internal character array directly.\n" +
                    "2. Thread Safety: String is immutable and inherently thread-safe. StringBuffer is mutable and thread-safe via synchronized methods. StringBuilder is mutable but NOT thread-safe.\n" +
                    "3. Performance: StringBuilder is the fastest for single-threaded string manipulations. StringBuffer has method synchronization overhead. String creates excessive garbage collection overhead when concatenated in loops.";
        } else if (lowerMsg.contains("synchronized") && lowerMsg.contains("volatile")) {
            reply = rolePrefix + "In Java Multithreading, the key differences between synchronized and volatile are:\n" +
                    "1. Visibility vs Atomicity: volatile guarantees memory visibility across thread CPU caches and prevents instruction reordering, but does NOT guarantee atomicity for compound operations (like count++). synchronized guarantees BOTH memory visibility and mutual exclusion (atomicity) by acquiring an intrinsic monitor lock.\n" +
                    "2. Lock & Blocking Overhead: volatile is a non-blocking field modifier with zero lock contention. synchronized locks object monitors and can block thread execution.\n" +
                    "3. Target Scope: volatile applies only to variables. synchronized applies to methods and code blocks.";
        } else if (lowerMsg.contains("final") && (lowerMsg.contains("finally") || lowerMsg.contains("finalize"))) {
            reply = rolePrefix + "In Java, final, finally, and finalize serve completely different purposes:\n" +
                    "1. final (Keyword): Applied to variables to make them immutable constants, to methods to prevent overriding, or to classes to prevent inheritance.\n" +
                    "2. finally (Block): Used in try-catch-finally control structures to guarantee execution of cleanup code (e.g. closing streams/sockets) regardless of whether an exception is thrown.\n" +
                    "3. finalize (Method): A legacy method in java.lang.Object invoked by the Garbage Collector prior to object destruction (deprecated since Java 9 due to unpredictable execution timing).";
        } else if (lowerMsg.contains("interface") && (lowerMsg.contains("abstract class") || lowerMsg.contains("abstract"))) {
            reply = rolePrefix + "In Java, the key differences between an Interface and an Abstract Class are:\n" +
                    "1. Multiple vs Single Inheritance: A class can implement multiple interfaces, but can extend only one abstract class.\n" +
                    "2. State & Fields: Interfaces only support static final constants (no instance state fields), whereas abstract classes can declare instance state fields and constructors.\n" +
                    "3. Methods & Modifiers: Interfaces support abstract methods, default methods, static methods, and private helper methods (Java 9+). Abstract classes can have methods of any access modifier (public, protected, package-private).\n" +
                    "4. Purpose: Use an Interface to define a contract behavior across unrelated classes; use an Abstract Class to share state and code implementation across closely related child classes.";
        } else if (lowerMsg.contains("process") && lowerMsg.contains("thread")) {
            reply = rolePrefix + "The key differences between a Process and a Thread are:\n" +
                    "1. Memory Space: A Process is an independent execution environment with its own private virtual address space. Threads within the same process share the process memory space (heap, global variables).\n" +
                    "2. Overhead & Cost: Processes have high creation and context switching overhead. Threads are lightweight with low context switching overhead.\n" +
                    "3. Communication: Inter-process communication (IPC) requires sockets, pipes, or shared memory. Threads communicate directly via shared memory objects.\n" +
                    "4. Fault Isolation: A process crash does not affect other processes. An unhandled exception in a thread can destabilize or crash the entire parent process.";
        } else if (lowerMsg.contains("rest") && lowerMsg.contains("grpc")) {
            reply = rolePrefix + "The key differences between REST and gRPC API architectures are:\n" +
                    "1. Protocol & Format: REST uses HTTP/1.1 with text-based JSON/XML payloads. gRPC uses HTTP/2 with binary Protocol Buffers (Protobuf) serialization.\n" +
                    "2. Performance & Streaming: gRPC is faster with lower payload footprint and natively supports bidirectional streaming via HTTP/2 multiplexing. REST relies on standard HTTP request-response cycles.\n" +
                    "3. Schema Contract: gRPC uses strictly typed .proto files for automatic client/server code generation; REST uses OpenAPI/Swagger specifications.";
        } else if (lowerMsg.contains("sql") && lowerMsg.contains("nosql")) {
            reply = rolePrefix + "The key differences between SQL (Relational) and NoSQL (Non-Relational) databases are:\n" +
                    "1. Schema & Structure: SQL databases enforce structured tables with predefined schemas and JOIN operations. NoSQL databases provide dynamic schemas (Document, Key-Value, Columnar, Graph).\n" +
                    "2. ACID vs BASE: SQL guarantees strict ACID (Atomicity, Consistency, Isolation, Durability) transactions. NoSQL prioritizes BASE (Basically Available, Soft state, Eventual consistency) for distributed scalability.\n" +
                    "3. Scaling: SQL scales vertically (more CPU/RAM); NoSQL scales horizontally across server clusters using sharding.";
        } else if (lowerMsg.contains("garbage collection") || (lowerMsg.contains("gc") && (lowerMsg.contains("jvm") || lowerMsg.contains("memory")))) {
            reply = rolePrefix + "JVM Garbage Collection is the automatic process of reclaiming Heap memory occupied by unreferenced objects:\n" +
                    "1. Mark-Sweep-Compact: GC identifies live objects starting from GC Roots, marks unreachable objects, reclaims their memory, and compacts remaining heap space.\n" +
                    "2. Heap Generations: Heap is divided into Young Generation (Eden + Survivor spaces) for short-lived objects (Minor GC) and Old Generation for long-lived objects (Major/Full GC).\n" +
                    "3. Modern Collectors: Collectors like G1GC and ZGC minimize Stop-the-World pauses using concurrent marking and region-based compaction.";
        } else if (lowerMsg.contains("@component") || lowerMsg.contains("@service") || lowerMsg.contains("@repository") || lowerMsg.contains("component vs service") || lowerMsg.contains("stereotype")) {
            reply = rolePrefix + "In Spring Framework, @Component, @Service, and @Repository are stereotype annotations used to designate Spring-managed beans:\n" +
                    "1. @Component: The generic stereotype for any Spring-managed component or bean.\n" +
                    "2. @Service: Specialization of @Component for the service layer; encapsulates business logic and service-level transactions.\n" +
                    "3. @Repository: Specialization of @Component for the persistence layer (DAOs); encapsulates database operations and provides automatic Exception Translation, converting native SQL/Hibernate exceptions into Spring's DataAccessException hierarchy.";
        } else if (lowerMsg.contains("jvm") || lowerMsg.contains("heap") || lowerMsg.contains("stack")) {
            reply = rolePrefix + "JVM Stack memory is created per thread to store execution frames, primitive variables, and references to objects. Memory is allocated and deallocated automatically as methods return. JVM Heap memory is shared across all threads and stores actual object instances created via 'new', managed by Garbage Collection. Stack access is faster with limited size, whereas Heap handles large object state allocation.";
        } else if (lowerMsg.contains("encapsulation") || lowerMsg.contains("abstraction") || lowerMsg.contains("polymorphism") || lowerMsg.contains("oop")) {
            reply = rolePrefix + "Encapsulation restricts direct access to an object's state using private access modifiers and getters/setters. Abstraction hides implementation complexity behind interfaces or abstract classes. Polymorphism allows objects of different underlying classes to respond to identical method calls dynamically via the JVM vtable.";
        } else if (lowerMsg.contains("hashmap") || lowerMsg.contains("concurrenthashmap") || lowerMsg.contains("collection")) {
            reply = rolePrefix + "HashMap uses an array of bucket nodes with key hash coding to achieve O(1) average operations. In Java 8+, buckets convert to Red-Black Trees if collision depth exceeds 8. ConcurrentHashMap guarantees thread safety using CAS operations and synchronized bucket nodes rather than locking the entire map.";
        } else if (lowerMsg.contains("aop") || lowerMsg.contains("@transactional")) {
            reply = rolePrefix + "Spring AOP creates dynamic proxy wrappers around Spring beans to intercept method invocations for cross-cutting logic like `@Transactional`. `@Transactional` configures transaction start, commit, and rollback boundaries on unchecked exceptions.";
        } else if (lowerMsg.contains("explain") || lowerMsg.contains("difference") || lowerMsg.contains("what is") || lowerMsg.contains("how does") || lowerMsg.contains("compare") || lowerMsg.contains("vs")) {
            reply = rolePrefix + "In software engineering for " + effectiveRole + " roles, regarding '" + userMessage + "':\n" +
                    "• Core Technical Definition: This concept defines specific memory, execution, and architectural contracts in the runtime environment.\n" +
                    "• Key Implementation Differences: Evaluate the trade-offs between performance latency, thread safety guarantees, memory allocation footprint, and compile-time type safety.\n" +
                    "• Production Use Cases: Choose the appropriate implementation based on your concurrency constraints, SLA requirements, and system maintainability standards.";
        }

        // --- STUDY ADVICE & ROLE SPECIFIC RESPONSES (ISSUE 2 FIX) ---
        if (reply == null) {
            if (lowerMsg.contains("focus") || lowerMsg.contains("prepare") || lowerMsg.contains("study") || lowerMsg.contains("topic") || lowerMsg.contains("tip") || lowerMsg.contains("advice")) {
                if (effectiveRole.equalsIgnoreCase("System Architect")) {
                    reply = rolePrefix + "For a System Architect interview, focus on:\n" +
                            "1. High-Level & Low-Level System Design (Microservices, Event-Driven Architecture, Caching patterns).\n" +
                            "2. Scalability & Traffic Handling (Layer 4 vs Layer 7 Load Balancers, CDN, Reverse Proxies).\n" +
                            "3. Distributed Transactions & Consensus (Saga Pattern, 2PC, Raft/Paxos consensus).\n" +
                            "4. Database Sharding vs Replication (Master-Slave, Multi-Master, Partitioning strategies).\n" +
                            "5. Fault Tolerance & SLAs (Circuit Breakers, Rate Limiting, Bulkheads, Disaster Recovery).";
                } else if (effectiveRole.equalsIgnoreCase("DevOps Engineer")) {
                    reply = rolePrefix + "For a DevOps Engineer / SRE interview, focus on:\n" +
                            "1. CI/CD Pipeline Automation (Jenkins, GitHub Actions, GitLab CI).\n" +
                            "2. Infrastructure as Code (Terraform, CloudFormation, Ansible).\n" +
                            "3. Containerization & Orchestration (Kubernetes, Docker, Helm charts).\n" +
                            "4. Cloud Infrastructure & Security (AWS/GCP networking, IAM, Secrets Management).\n" +
                            "5. Observability & Alerting (Prometheus, Grafana, ELK/OpenSearch stack).";
                } else if (effectiveRole.equalsIgnoreCase("Data Analyst")) {
                    reply = rolePrefix + "For a Data Analyst interview, focus on:\n" +
                            "1. Advanced SQL Queries (Window functions, CTEs, complex joins, aggregation).\n" +
                            "2. Business Intelligence & Dashboards (Tableau, Power BI, Looker).\n" +
                            "3. Statistical Analysis & Hypothesis Testing (p-values, A/B testing, confidence intervals).\n" +
                            "4. Data Wrangling in Python/R (Pandas, NumPy, data cleaning & transformation).\n" +
                            "5. Business Metrics & KPI Definition (Churn rate, LTV, CAC, Retention analysis).";
                } else if (effectiveRole.equalsIgnoreCase("Data Engineer")) {
                    reply = rolePrefix + "For a Data Engineer interview, focus on:\n" +
                            "1. Distributed Data Processing Frameworks (Apache Spark, Hadoop, Flink).\n" +
                            "2. Advanced SQL, Indexing, and Data Warehousing (Snowflake, BigQuery, Redshift).\n" +
                            "3. ETL Pipeline Orchestration (Airflow) and Real-Time Streaming (Kafka).\n" +
                            "4. Data Modeling (Star/Snowflake schema) and Python/Scala data manipulation.";
                } else if (effectiveRole.equalsIgnoreCase("Frontend Developer")) {
                    reply = rolePrefix + "For a Frontend Developer interview, focus on:\n" +
                            "1. Modern JavaScript (ES6+), async/await, closures, and Event Loop.\n" +
                            "2. Component State Management, Virtual DOM, and Rendering Performance (React/Vue).\n" +
                            "3. Responsive CSS Layouts (Flexbox/Grid), Web Vitals, and Performance Profiling.\n" +
                            "4. Web Security Best Practices (XSS, CORS, CSRF, CSP).";
                } else if (effectiveRole.equalsIgnoreCase("Backend Developer")) {
                    reply = rolePrefix + "For a Backend Developer interview, focus on:\n" +
                            "1. REST & gRPC API Design, status codes, and payload optimization.\n" +
                            "2. Microservice Architecture, Dependency Injection, and SOLID Design Patterns.\n" +
                            "3. Database Schema Design, SQL tuning, indexing, and ORM performance (JPA/Hibernate).\n" +
                            "4. Caching Strategies (Redis Cache-Aside) and Async Messaging (Kafka/RabbitMQ).";
                } else if (effectiveRole.equalsIgnoreCase("Full Stack Developer")) {
                    reply = rolePrefix + "For a Full Stack Developer interview, focus on:\n" +
                            "1. End-to-End Application Architecture (Client state to DB persistence).\n" +
                            "2. Frontend Frameworks (React/Angular) and State Management.\n" +
                            "3. Backend REST/GraphQL APIs and Server-Side Rendering.\n" +
                            "4. Database Integration (Relational SQL & Document NoSQL stores).\n" +
                            "5. Web Security best practices (XSS, CSRF, CORS, Auth token security).";
                } else if (effectiveRole.equalsIgnoreCase("QA Engineer")) {
                    reply = rolePrefix + "For a QA / Test Engineer interview, focus on:\n" +
                            "1. Test Automation Frameworks (Selenium, Cypress, Playwright, JUnit/TestNG).\n" +
                            "2. API Testing & Automation (Postman, REST Assured, Mocking dependencies).\n" +
                            "3. Test Strategy & Case Design (Boundary Value Analysis, Equivalence Partitioning).\n" +
                            "4. Performance & Load Testing (JMeter, k6, bottleneck identification).";
                } else if (effectiveRole.equalsIgnoreCase("Mobile Developer")) {
                    reply = rolePrefix + "For a Mobile Developer interview, focus on:\n" +
                            "1. Native Mobile Architecture (iOS Swift / Android Kotlin, MVVM, Clean Architecture).\n" +
                            "2. Mobile State Management & Reactive Programming (RxSwift, Kotlin Coroutines).\n" +
                            "3. Offline-First Caching & Data Persistence (Room, CoreData, SQLite).\n" +
                            "4. App Performance Optimization (Memory leaks, 60fps rendering, battery usage).";
                } else if (effectiveRole.equalsIgnoreCase("Data Scientist")) {
                    reply = rolePrefix + "For a Data Scientist interview, focus on:\n" +
                            "1. Machine Learning Algorithms (Supervised, Unsupervised, Ensemble methods, XGBoost).\n" +
                            "2. Deep Learning Frameworks (PyTorch, TensorFlow, Neural Network Architectures).\n" +
                            "3. Feature Engineering, Cross-Validation, and Model Evaluation (ROC-AUC, F1-Score).\n" +
                            "4. Data Processing Pipelines (Python, Scikit-Learn, Spark MLlib).";
                } else {
                    reply = rolePrefix + "For your upcoming " + effectiveRole + " interview, focus on core architecture, design patterns, data structures, and framework fundamentals. Make sure to explain your thought process clearly during technical discussions!";
                }
            } else if (lowerMsg.contains("tip") || lowerMsg.contains("advice") || lowerMsg.contains("strategy")) {
                reply = rolePrefix + "Key interview strategies for a " + effectiveRole + ":\n1. Structure answers using the STAR method for behavioral questions.\n2. State time & space complexity upfront for algorithmic questions.\n3. Discuss edge cases, concurrency controls, and scalability tradeoffs in system design scenarios.";
            } else if (lowerMsg.contains("question") || lowerMsg.contains("practice") || lowerMsg.contains("example")) {
                reply = rolePrefix + "Here is a sample technical question for a " + effectiveRole + ": 'How do you handle transaction management and data consistency in distributed environments?' Try explaining your approach step-by-step!";
            } else {
                reply = rolePrefix + "As your AI Interview Coach for " + effectiveRole + ", regarding your question ('" + userMessage + "'): focus on mastering fundamental concepts, practicing clear verbal explanations, and breaking down complex scenarios step by step. What specific topic would you like to drill into?";
            }
        }

        log.info("[PREP MODE QA VERIFICATION] Question Asked: '{}' | Answer Given: '{}'", userMessage, reply);
        return reply;
    }

    private String generateReviewMockReply(String systemPrompt, String userMessage, String lowerMsg) {
        if (systemPrompt == null) {
            return "No review session context was provided.";
        }

        if (systemPrompt.contains("NOTICE: The candidate asked about Question #") || systemPrompt.contains("DOES NOT EXIST")) {
            String noticeMarker = "NOTICE: The candidate asked about Question #";
            int idx = systemPrompt.indexOf(noticeMarker);
            String qNumStr = "requested";
            if (idx != -1) {
                int comma = systemPrompt.indexOf(",", idx);
                if (comma > idx) {
                    qNumStr = systemPrompt.substring(idx + noticeMarker.length(), comma).trim();
                }
            }
            String totalCountStr = extractSection(systemPrompt, "this session ONLY has ", " total questions");
            if (totalCountStr.equals("N/A")) {
                totalCountStr = extractSection(systemPrompt, "session has ", " total questions");
            }
            return "Question #" + qNumStr + " does not exist in this assessment session. This session only contains " + (totalCountStr.equals("N/A") ? "the completed" : totalCountStr) + " total questions. Please specify a valid question number from your session report card!";
        }

        if (systemPrompt.contains("=== TARGETED QUESTION CONTEXT")) {
            String qText = extractSection(systemPrompt, "Question Text: ", "\n");
            String candidateAns = extractSection(systemPrompt, "Candidate's Submitted Answer: ", "\n");
            String score = extractSection(systemPrompt, "Overall Score: ", "\n");
            String techAcc = extractSection(systemPrompt, "Technical Accuracy: ", "\n");
            String feedback = extractSection(systemPrompt, "Stored AI Feedback: ", "\n");
            String missing = extractSection(systemPrompt, "Missing Concepts: ", "\n");
            String imp = extractSection(systemPrompt, "Improvement Suggestion: ", "\n");

            return "Here is the detailed breakdown for your query:\n\n" +
                   "• Question: " + qText + "\n" +
                   "• Your Submitted Answer: \"" + candidateAns + "\"\n" +
                   "• Original Score Received: " + score + " (Technical Accuracy: " + techAcc + ")\n" +
                   "• Stored AI Feedback: " + feedback + "\n" +
                   "• Missing Concepts: " + missing + "\n\n" +
                   "Specific Explanation: Marks were deducted primarily because your answer (\"" + candidateAns + "\") lacked deep explanation of " + missing + ". " + imp;
        }

        // Generic fallback for session review if no single question was isolated
        String sessionScore = extractSection(systemPrompt, "Overall Session Score: ", "\n");
        return "Based on your assessment session details (Overall Session Score: " + sessionScore + "):\n" +
               "To review a specific question's score and feedback, ask about 'question 1', 'question 2', etc. I will explain your submitted answer, score breakdown, and key missing concepts for that exact question!";
    }

    private String generateResumeMockReply(String systemPrompt, String userMessage, String lowerMsg) {
        String missing = extractSection(systemPrompt, "Missing Keywords: ", "\n");
        String resumeSnippet = extractSection(systemPrompt, "Resume Snippet: ", "\n");
        String jdSnippet = extractSection(systemPrompt, "Job Description Snippet: ", "\n");

        log.info("[RESUME MODE MOCK REPLY] Processing RESUME mode question with Missing Keywords: '{}'", missing);

        if (lowerMsg.contains("reword") || lowerMsg.contains("bullet") || lowerMsg.contains("keyword") || lowerMsg.contains("fix") || lowerMsg.contains("missing")) {
            String reply = "ATS Resume Advisor Actionable Rewording for your resume ('" + (resumeSnippet.length() > 40 ? resumeSnippet.substring(0, 40) + "..." : resumeSnippet) + "'):\n" +
                    "• Original Bullet: 'Worked on software application implementation'\n" +
                    "• Improved ATS Bullet: 'Architected and optimized scalable services integrating " + missing + ", reducing deployment latency by 28%.'\n" +
                    "• Missing Keywords Integrated: " + missing + "\n" +
                    "Apply this quantified pattern across your work experience entries for target JD: " + (jdSnippet.length() > 40 ? jdSnippet.substring(0, 40) + "..." : jdSnippet);
            log.info("[RESUME MODE QA VERIFICATION] Question Asked: '{}' | Answer Given: '{}'", userMessage, reply);
            return reply;
        }

        String defaultReply = "Welcome to ATS Resume Advisor! Based on your uploaded resume ('" + (resumeSnippet.length() > 40 ? resumeSnippet.substring(0, 40) + "..." : resumeSnippet) + "') and target JD: to maximize your match score, incorporate missing keywords (" + missing + ") into high-impact experience bullets with metrics.";
        log.info("[RESUME MODE QA VERIFICATION] Question Asked: '{}' | Answer Given: '{}'", userMessage, defaultReply);
        return defaultReply;
    }

    private String extractSection(String text, String startMarker, String endMarker) {
        if (text == null || !text.contains(startMarker)) return "N/A";
        int start = text.indexOf(startMarker) + startMarker.length();
        int end = text.indexOf(endMarker, start);
        if (end == -1) end = text.length();
        return text.substring(start, end).trim();
    }

    private static class QuestionTemplate {
        final String text;
        final String topic;
        final List<String> expectedConcepts;

        QuestionTemplate(String text, String topic, List<String> expectedConcepts) {
            this.text = text;
            this.topic = topic;
            this.expectedConcepts = expectedConcepts;
        }
    }
}
