package com.resumeforge.generation.specification;

import com.resumeforge.generation.entity.GeneratedResume;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GeneratedResumeSpecification {

    public static Specification<GeneratedResume> withFilters(
            UUID userId,
            String companyName,
            String jobTitle,
            UUID jobApplicationId,
            UUID resumeProfileId,
            OffsetDateTime dateFrom,
            OffsetDateTime dateTo,
            Boolean isCurrent) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // User filter (required)
            if (userId != null) {
                Join<?, ?> resumeProfile = root.join("resumeProfile");
                predicates.add(cb.equal(resumeProfile.get("user").get("id"), userId));
            }

            // Soft delete filter
            predicates.add(cb.isNull(root.get("deletedAt")));

            // jobApplicationId filter
            if (jobApplicationId != null) {
                predicates.add(cb.equal(root.get("jobApplication").get("id"), jobApplicationId));
            }

            // resumeProfileId filter
            if (resumeProfileId != null) {
                predicates.add(cb.equal(root.get("resumeProfile").get("id"), resumeProfileId));
            }

            // isCurrent filter
            if (isCurrent != null) {
                predicates.add(cb.equal(root.get("isCurrent"), isCurrent));
            }

            // dateFrom filter
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom));
            }

            // dateTo filter
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), dateTo));
            }

            // companyName filter - using LEFT JOIN on jobApplication
            if (companyName != null && !companyName.isBlank()) {
                Join<?, ?> jobApplication = root.join("jobApplication", jakarta.persistence.criteria.JoinType.LEFT);
                predicates.add(cb.like(cb.lower(jobApplication.get("companyName")), "%" + companyName.toLowerCase() + "%"));
            }

            // jobTitle filter - using LEFT JOIN on jobApplication
            if (jobTitle != null && !jobTitle.isBlank()) {
                Join<?, ?> jobApplication = root.join("jobApplication", jakarta.persistence.criteria.JoinType.LEFT);
                predicates.add(cb.like(cb.lower(jobApplication.get("jobTitle")), "%" + jobTitle.toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
