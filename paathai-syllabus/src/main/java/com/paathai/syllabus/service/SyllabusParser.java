package com.paathai.syllabus.service;

/**
 * Interface for syllabus parsing.
 * Implementations: PdfSyllabusParser, JsonSyllabusParser.
 */
public interface SyllabusParser {

    /**
     * Parse syllabus content and return a structured hierarchy.
     *
     * @param content Raw content (text from PDF or JSON string)
     * @return Parsed syllabus hierarchy
     */
    SyllabusHierarchy parse(String content);

    /**
     * Whether this parser supports the given file type.
     */
    boolean supports(String fileType);

    /**
     * Represents a parsed syllabus hierarchy.
     */
    record SyllabusHierarchy(
        java.util.List<SubjectEntry> subjects
    ) {}

    record SubjectEntry(
        String name,
        java.util.List<UnitEntry> units
    ) {}

    record UnitEntry(
        String name,
        java.util.List<TopicEntry> topics
    ) {}

    record TopicEntry(
        String name,
        String description
    ) {}
}
