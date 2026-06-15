package com.resumeforge.export.converter;

import java.util.List;

/**
 * Test fixtures for StructuredDocxConverter tests.
 */
public final class StructuredDocxConverterTestData {

    private StructuredDocxConverterTestData() {}

    public static StructuredDocxConverter.ResumeHeader sampleHeader() {
        return new StructuredDocxConverter.ResumeHeader(
            "John Developer",
            "Senior Software Engineer",
            "San Francisco, CA",
            "john@example.com",
            "+1-555-123-4567",
            "linkedin.com/in/johndev",
            "github.com/johndev"
        );
    }

    public static StructuredDocxConverter.ResumeStructure sampleStructure() {
        // ResumeStructure record order:
        // (professionalTitle, professionalSummary, skills, experience, previousExperienceSummary,
        //  projects, education, certifications, trainings, languages)
        return new StructuredDocxConverter.ResumeStructure(
            null, // professionalTitle (header has the title)
            "Experienced software engineer with 8+ years building scalable systems", // professionalSummary
            List.of(
                new StructuredDocxConverter.SkillCategory("Languages",
                    List.of("Java", "Python", "JavaScript")),
                new StructuredDocxConverter.SkillCategory("Frameworks",
                    List.of("Spring Boot", "React", "FastAPI"))
            ),
            List.of(
                new StructuredDocxConverter.Experience(
                    "Tech Corp",
                    "Senior Software Engineer",
                    "San Francisco, CA",
                    "Jan 2020",
                    "Present",
                    List.of(
                        "Led development of microservices architecture serving 1M+ users",
                        "Reduced API latency by 40% through caching strategies",
                        "Mentored 5 junior developers on best practices"
                    )
                ),
                new StructuredDocxConverter.Experience(
                    "StartupXYZ",
                    "Software Engineer",
                    "Remote",
                    "Jun 2017",
                    "Dec 2019",
                    List.of(
                        "Built CI/CD pipeline reducing deploy time from 2 hours to 15 minutes",
                        "Implemented automated testing achieving 85% code coverage"
                    )
                )
            ),
            null, // previousExperienceSummary
            List.of(
                new StructuredDocxConverter.Project(
                    "OpenSource Tool",
                    "CLI tool for developers",
                    List.of("Go", "Docker", "GitHub Actions")
                )
            ),
            List.of(
                new StructuredDocxConverter.Education(
                    "MIT",
                    "BS Computer Science",
                    "2013-2017"
                )
            ),
            List.of(
                new StructuredDocxConverter.Certification(
                    "AWS Solutions Architect",
                    "Amazon Web Services",
                    "2023"
                )
            ),
            null, // trainings
            List.of(
                new StructuredDocxConverter.Language("English", "Native"),
                new StructuredDocxConverter.Language("Spanish", "Conversational")
            )
        );
    }

    public static StructuredDocxConverter.ResumeStructure minimalStructure() {
        return new StructuredDocxConverter.ResumeStructure(
            null, // professionalTitle
            "Short summary", // professionalSummary
            null, // skills
            null, // experience
            null, // previousExperienceSummary
            null, // projects
            null, // education
            null, // certifications
            null, // trainings
            null  // languages
        );
    }
}
