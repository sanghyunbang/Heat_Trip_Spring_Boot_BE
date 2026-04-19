# PR #7 Review Log

PR: `#7`  
Title: `[refactor] Part of #2 migrate curation LLM DTOs to Kotlin`  
Issue linkage: `Part of #2`  
Branch: `refactor/kotlin-curation-llm-dto`

## Purpose

This document records the review outcome for PR `#7`, the follow-up fixes applied after review, and the technical background behind the issue.

The review focused on one risk area with real runtime impact:

- Kotlin DTO migration changed the Jackson annotation model.
- The LLM response DTO is deserialized from JSON returned by the external recommender.
- If annotation targets are wrong, the application can compile and tests can pass at a high level while the runtime payload is silently mapped to `null` or default values.

## What Was Reviewed

The PR originally did the following:

- moved recommendation request/response DTOs into `curation/dto`
- replaced direct client dependency with `RecommendationPort`
- introduced `OpenAiRecommendationAdapter`
- migrated `RecommendResultDTO.java` to `RecommendResultDto.kt`
- removed `llm/RecommenderClient.java`
- added tests and JaCoCo support

The review found one important runtime compatibility risk in:

- `src/main/java/com/heattrip/heat_trip_backend/curation/dto/LlmRecommendResponse.kt`

## Review Finding

### Summary

`LlmRecommendResponse.kt` used `@get:JsonProperty(...)` for snake_case fields such as:

- `schema_version`
- `emotion_diagnosis`
- `theme_name`
- `theme_description`
- `category_groups`
- `comfort_letter`

and the nested `CategoryGroup.groupName` field used the same pattern.

### Why That Is Risky

This DTO is not only serialized out. It is also deserialized in from the LLM HTTP response through:

- `WebClient.bodyToMono(LlmRecommendResponse::class.java)`

In Kotlin data classes, Jackson has to bind JSON values into the primary constructor.

If the annotation is only attached to the generated getter:

- serialization can still appear to work
- but constructor-based deserialization may not resolve the JSON property name correctly
- snake_case fields may end up as `null`, empty collections, or default values

That means this kind of runtime failure is possible:

1. LLM server returns a valid JSON payload
2. HTTP call itself succeeds
3. `LlmRecommendResponse` is created with missing values
4. downstream logic interprets the payload incorrectly
5. recommendation quality degrades or behaves inconsistently

This is the kind of bug that is easy to miss with manual smoke testing because:

- the endpoint still responds
- no compile error occurs
- no obvious exception is guaranteed
- only some fields become incorrect

## Fix Applied

The following changes were applied after review.

### 1. Changed annotation targets on `LlmRecommendResponse`

Updated fields from:

- `@get:JsonProperty(...)`

to:

- `@field:JsonProperty(...)`
- `@param:JsonProperty(...)`

This was done for:

- `schemaVersion`
- `emotionDiagnosis`
- `themeName`
- `themeDescription`
- `categoryGroups`
- `comfortLetter`

### 2. Changed annotation targets on nested `CategoryGroup`

The same fix was applied to:

- `CategoryGroup.groupName`

This matters because nested DTOs are part of the same deserialization chain. Fixing only the root DTO would still leave partial mapping risk inside `category_groups`.

### 3. Added a dedicated regression test

Added test:

- `src/test/java/com/heattrip/heat_trip_backend/curation/dto/LlmRecommendResponseJsonTest.java`

What it validates:

- a snake_case JSON payload is read into `LlmRecommendResponse`
- nested `category_groups.group_name` maps correctly
- lists like `activities` and `keywords` are preserved
- all important fields are asserted explicitly

This test is valuable because it locks the actual integration contract:

- JSON shape
- Kotlin DTO constructor mapping
- Jackson Kotlin module behavior

## Concept Explanation

### 1. `@get:` vs `@field:` vs `@param:` in Kotlin

Kotlin properties are not the same thing as plain Java fields.

One Kotlin property can produce multiple JVM elements:

- constructor parameter
- backing field
- getter method
- setter method for mutable properties

So when you write an annotation in Kotlin, you sometimes need to tell the compiler where that annotation should be placed.

Common use-site targets:

- `@get:...` attaches the annotation to the getter
- `@field:...` attaches it to the backing field
- `@param:...` attaches it to the constructor parameter

### 2. Why JSON libraries care about the target

Jackson can inspect different places depending on how the class is instantiated:

- getter/setter-based binding
- field-based binding
- constructor-based binding

Kotlin data classes strongly encourage constructor-based binding.

That means if the JSON property name needs to override the Kotlin property name, constructor parameter metadata often matters the most.

Using only `@get:JsonProperty` is frequently insufficient for safe deserialization in Kotlin DTOs, especially when:

- JSON uses snake_case
- Kotlin properties use camelCase
- the class is a data class
- nested DTOs are also Kotlin data classes

### 3. Why this can pass compilation and still fail at runtime

The compiler only checks syntax and types.

It does not verify:

- whether the external JSON contract matches your annotations
- whether Jackson will inspect the getter, field, or constructor parameter
- whether missing values will silently fall back to defaults

So this category of issue is a runtime contract bug, not a compile-time bug.

### 4. Why a dedicated deserialization test is better than only service tests

Service tests can prove:

- orchestration logic works
- dependencies are called correctly
- output structure is assembled correctly

But if the service test creates DTOs manually in code, it does not verify the JSON contract.

That is why a deserialization test is necessary here.

It catches:

- annotation target mistakes
- property name mismatches
- nested DTO mapping problems
- Jackson Kotlin module regressions

## Relationship To Manual Testing

Manual Postman testing is still useful.

It verifies:

- the endpoint can be reached
- security flow works
- the happy path functions end-to-end

But it is not enough for this class of issue because:

- only certain payload fields may be wrong
- the application may still respond `200 OK`
- a partial deserialization failure can look like a business-logic problem rather than a DTO problem

The regression test added here closes that gap.

## Final State After Follow-Up

After the review follow-up, PR `#7` now includes:

- corrected Jackson annotation targets for Kotlin deserialization
- corrected nested DTO mapping for `CategoryGroup`
- an automated regression test for the LLM response JSON contract

## Recommended Ongoing Practice

For Kotlin DTO migration work in this repository, the safer baseline is:

- use `@field:JsonProperty` and/or `@param:JsonProperty` for externally consumed JSON DTOs
- add at least one direct JSON round-trip or deserialization test for each critical external contract
- distinguish between:
  - service logic tests
  - controller tests
  - serialization/deserialization contract tests

These three test types catch different failure modes.

## Files Touched In Follow-Up

- `src/main/java/com/heattrip/heat_trip_backend/curation/dto/LlmRecommendResponse.kt`
- `src/test/java/com/heattrip/heat_trip_backend/curation/dto/LlmRecommendResponseJsonTest.java`

## Review Status

- PR reviewed
- review comment posted on PR `#7`
- follow-up fixes applied locally
- regression test added
