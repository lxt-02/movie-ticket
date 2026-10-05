# Candidate Service Structure

Tài liệu này mô tả cấu trúc tổ chức của `candidate-service` để có thể mang sang dự án khác và yêu cầu code lại theo cùng kiến trúc.

## Architecture

`candidate-service` dùng hướng Clean Architecture / Hexagonal Architecture.

Dependency rule:

```text
api -> application -> domain
infrastructure -> application/domain
domain không phụ thuộc api, application, infrastructure
```

Ý nghĩa:

- `api`: nhận HTTP request, validate DTO, gọi input port.
- `application`: định nghĩa use case, command/query, input port, output port.
- `domain`: chứa aggregate, business rule, repository contract.
- `infrastructure`: chứa JPA entity, Spring Data repository, adapter để lưu DB.

## Folder Structure

```text
candidate-service
└── src/main/java/{basePackage}
    ├── CandidateServiceApplication.java
    │
    ├── api
    │   ├── rest
    │   │   └── CandidateController.java
    │   ├── dto
    │   │   ├── CandidateRequest.java
    │   │   ├── CandidateResponse.java
    │   │   └── ParsedCvDataResponse.java
    │   └── exception
    │       └── GlobalExceptionHandler.java
    │
    ├── application
    │   ├── command
    │   │   └── CandidateCommand.java
    │   ├── port
    │   │   ├── in
    │   │   │   └── CreateCandidatePort.java
    │   │   └── out
    │   │       └── SaveCandidatePort.java
    │   └── service
    │       └── CreateCandidateUseCase.java
    │
    ├── domain
    │   ├── aggregate
    │   │   └── CandidateAggregate.java
    │   └── repository
    │       └── CandidateRepository.java
    │
    └── infrastructure
        ├── adapter
        │   └── CreateCandidateAdapter.java
        └── persistence
            ├── entity
            │   ├── CandidateEntity.java
            │   ├── CandidateSkillEntity.java
            │   ├── CvEntity.java
            │   ├── ParsedCvData.java
            │   ├── CandidateStatus.java
            │   └── ParseStatus.java
            └── repository
                └── CandidateJpaRepository.java
```

## Layer Responsibilities

### API Layer

Package:

```text
{basePackage}.api
```

Responsibilities:

- Expose REST endpoints.
- Receive request DTOs.
- Return response DTOs.
- Call application input ports.
- Handle validation errors and business exceptions.
- Must not contain business logic.
- Must not call JPA repositories directly.

Files:

```text
api/rest/CandidateController.java
api/dto/CandidateRequest.java
api/dto/CandidateResponse.java
api/dto/ParsedCvDataResponse.java
api/exception/GlobalExceptionHandler.java
```

Example controller flow:

```text
HTTP request
-> CandidateController
-> CandidateRequest
-> CandidateCommand
-> CreateCandidatePort.execute(command)
-> CandidateResponse
```

### Application Layer

Package:

```text
{basePackage}.application
```

Responsibilities:

- Define use case input ports.
- Define output ports needed by use cases.
- Define command/query models.
- Implement use cases.
- Orchestrate domain logic.
- Must not use JPA entity directly.
- Must not depend on HTTP details.

Files:

```text
application/command/CandidateCommand.java
application/port/in/CreateCandidatePort.java
application/port/out/SaveCandidatePort.java
application/service/CreateCandidateUseCase.java
```

Use case flow:

```text
CreateCandidateUseCase
-> CandidateAggregate.create(command)
-> CandidateRepository.save(aggregate)
```

### Domain Layer

Package:

```text
{basePackage}.domain
```

Responsibilities:

- Own candidate business rules.
- Define aggregate root.
- Define repository contract.
- Validate domain invariants.
- Must not import Spring, JPA, REST DTO, or infrastructure classes.

Files:

```text
domain/model/candidate/aggregate/CandidateAggregate.java
domain/model/candidate/entity/*.java
domain/model/candidate/enums/*.java
domain/model/candidate/event/*.java
domain/model/candidate/exception/*.java
domain/model/candidate/valueobject/*.java
domain/repository/CandidateRepository.java
```

Recommended domain package format:

```text
domain
  model
    {domainModel}
      aggregate
      entity
      enums
      event
      exception
      valueobject
    {supportModel}
  repository
  shared
```

Rules:

- Put the aggregate root in `domain/model/{domainModel}/aggregate`.
- Put child entities in `domain/model/{domainModel}/entity`.
- Put enums used by the domain model in `domain/model/{domainModel}/enums`.
- Put domain exceptions for that model in `domain/model/{domainModel}/exception`.
- Put value objects in `domain/model/{domainModel}/valueobject`.
- Put domain events in `domain/model/{domainModel}/event`.
- Keep repository contracts in `domain/repository`, because they are aggregate access contracts.
- Support models such as `outbox` can live under `domain/model/outbox`.
- Do not create repositories for child entities. Load and save them through the aggregate root repository.

Example for booking aggregate:

```text
domain
  model
    booking
      aggregate
        Booking.java
      entity
        BookingSeat.java
      enums
        BookingStatus.java
        BookingSeatStatus.java
      exception
        BookingConflictException.java
        BookingNotFoundException.java
        BookingValidationException.java
        DomainException.java
        InvalidBookingStateException.java
    outbox
      OutboxEvent.java
  repository
    BookingRepository.java
```

Candidate creation rules:

- `fullName` should not be blank.
- `userId` must not be null.
- `source` must not be null.
- `source` must be one of:
  - `Facebook`
  - `LinkedIn`
  - `VietnamWorks`
  - `Zalo`
- Default `status` is `ACTIVE`.

Important improvement:

```text
CandidateStatus should live in domain if CandidateAggregate uses it.
Do not import infrastructure.persistence.entity.CandidateStatus into domain.
```

### Infrastructure Layer

Package:

```text
{basePackage}.infrastructure
```

Responsibilities:

- Implement persistence.
- Define JPA entities.
- Define Spring Data repositories.
- Implement adapters for output ports.
- Map domain aggregate to persistence entity and back.

Files:

```text
infrastructure/adapter/CreateCandidateAdapter.java
infrastructure/persistence/entity/CandidateEntity.java
infrastructure/persistence/entity/CandidateSkillEntity.java
infrastructure/persistence/entity/CvEntity.java
infrastructure/persistence/entity/ParsedCvData.java
infrastructure/persistence/entity/CandidateStatus.java
infrastructure/persistence/entity/ParseStatus.java
infrastructure/persistence/repository/CandidateJpaRepository.java
```

Adapter flow:

```text
CreateCandidateAdapter implements SaveCandidatePort
-> receives CandidateAggregate
-> maps aggregate to CandidateEntity
-> saves using CandidateJpaRepository
-> returns CandidateAggregate or mapped saved aggregate
```

## Domain Model

Candidate aggregate fields:

```text
id: Long
userId: UUID
fullName: String
status: CandidateStatus
source: String
utmSource: String
utmMedium: String
utmCampaign: String
duplicate: boolean
activeCvId: Long
parsedCvData: ParsedCvData
cvs: List<Cv>
skills: List<CandidateSkill>
```

Candidate status:

```text
ACTIVE
INACTIVE
```

CV parse status:

```text
PENDING
PARSED
FAILED
```

## Persistence Model

### candidates

```text
id bigint primary key
user_id uuid unique not null
full_name varchar(255)
status varchar(50)
source varchar(150)
utm_source varchar(150)
utm_medium varchar(150)
utm_campaign varchar(255)
is_duplicate boolean not null default false
cv_file_id bigint null
parsed_cv_data jsonb null
```

Notes:

- `user_id` is the Keycloak subject / authenticated user id.
- `cv_file_id` points to the active CV id.
- `parsed_cv_data` stores normalized CV data for fast candidate profile response.

### cvs

```text
id bigint primary key
candidate_id bigint not null
file_path varchar(1000)
file_type varchar(50)
size_bytes bigint
parsed_data jsonb
parse_status varchar(50)
uploaded_at timestamptz
```

Notes:

- `parsed_data` stores raw output from `cv-parser-service`.
- `parse_status` is `PENDING`, `PARSED`, or `FAILED`.
- Candidate service does not parse CV itself. It stores upload metadata and consumes parser result.

### candidate_skills

```text
id bigint primary key
candidate_id bigint not null
skill_id bigint not null
level varchar(20)
years_exp int
unique(candidate_id, skill_id)
```

Notes:

- `skill_id` is a cross-service reference to skill data.
- Do not map `skill_id` as a JPA relation if skill data belongs to another service/database.

## Initial APIs

### Create Candidate

```text
POST /api/v1/candidates
```

Request:

```json
{
  "fullName": "Nguyen Van A",
  "source": "LinkedIn",
  "utmSource": "linkedin",
  "utmMedium": "social",
  "utmCampaign": "fall-hiring",
  "userId": "2a8783fc-7cb7-4da2-98a0-bdc3dd1fbc62"
}
```

Behavior:

- Validate request.
- Convert request to `CandidateCommand`.
- Call `CreateCandidatePort`.
- Domain creates `CandidateAggregate`.
- Infrastructure saves `CandidateEntity`.
- Return created candidate profile.

### Get Candidate By ID

```text
GET /api/v1/candidates/{id}
```

Behavior:

- Load candidate by id.
- Return `CandidateResponse`.
- Return `404` if not found.

## Recommended APIs To Add Later

Candidate profile:

```text
PUT /api/v1/candidates/me
GET /api/v1/candidates/me
PATCH /api/v1/candidates/me
GET /api/v1/candidates/{candidateId}
```

Candidate CV:

```text
POST /api/v1/candidates/me/cvs
GET /api/v1/candidates/me/cvs
GET /api/v1/candidates/me/cvs/{cvId}
PUT /api/v1/candidates/me/cvs/{cvId}/active
DELETE /api/v1/candidates/me/cvs/{cvId}
GET /api/v1/candidates/me/cvs/{cvId}/parse-status
POST /api/v1/candidates/me/cvs/{cvId}/parse
```

Candidate skills:

```text
GET /api/v1/candidates/me/skills
PUT /api/v1/candidates/me/skills
POST /api/v1/candidates/me/skills
DELETE /api/v1/candidates/me/skills/{skillId}
```

## DTOs

### CandidateRequest

```text
fullName: String, required, max 255
source: String, max 150
utmSource: String, max 150
utmMedium: String, max 150
utmCampaign: String, max 255
userId: UUID
```

### CandidateResponse

```text
id: Long
fullName: String
status: String
source: String
utmSource: String
utmMedium: String
utmCampaign: String
duplicate: boolean
activeCvId: Long
parsedCvData: ParsedCvDataResponse
```

### ParsedCvDataResponse

```text
fullName: String
email: String
phone: String
summary: String
skills: List<String>
education: List<EducationResponse>
experience: List<ExperienceResponse>
```

Education:

```text
school: String
degree: String
fieldOfStudy: String
startDate: String
endDate: String
```

Experience:

```text
company: String
title: String
startDate: String
endDate: String
description: String
```

## Dependencies

Use Spring Boot with:

```text
spring-boot-starter-web
spring-boot-starter-data-jpa
spring-boot-starter-validation
spring-boot-starter-actuator
postgresql
lombok
mapstruct
spring-boot-starter-test
```

Java version:

```text
Java 21
```

Database:

```text
PostgreSQL
```

## Implementation Rules

- Controller must call input port, not service implementation directly.
- Use case must depend on domain repository/output port, not JPA repository.
- Domain must not depend on infrastructure.
- JPA entities must stay in `infrastructure.persistence.entity`.
- Spring Data repositories must stay in `infrastructure.persistence.repository`.
- Adapter must handle mapping between aggregate and entity.
- Business validation should be in domain aggregate or domain service.
- Request validation should be in DTO/controller boundary.
- Cross-service references should be stored as primitive IDs, not JPA relations.

## Prompt To Generate Code

```text
Build a Spring Boot candidate-service using Clean Architecture / Hexagonal Architecture.

Base package: {basePackage}
Java version: 21
Database: PostgreSQL

Create this package structure:

- api.rest
- api.dto
- api.exception
- application.command
- application.port.in
- application.port.out
- application.service
- domain.model.{domainModel}.aggregate
- domain.model.{domainModel}.entity
- domain.model.{domainModel}.enums
- domain.model.{domainModel}.event
- domain.model.{domainModel}.exception
- domain.model.{domainModel}.valueobject
- domain.repository
- infrastructure.adapter
- infrastructure.persistence.entity
- infrastructure.persistence.repository

Implement candidate creation flow:

1. CandidateController exposes POST /api/v1/candidates.
2. CandidateController receives CandidateRequest.
3. CandidateController maps CandidateRequest to CandidateCommand.
4. CandidateController calls CreateCandidatePort.execute(command).
5. CreateCandidateUseCase implements CreateCandidatePort.
6. CreateCandidateUseCase calls CandidateAggregate.create(command).
7. CandidateAggregate validates:
   - fullName is not blank
   - userId is not null
   - source is not null
   - source is one of Facebook, LinkedIn, VietnamWorks, Zalo
   - status defaults to ACTIVE
8. CreateCandidateUseCase saves through CandidateRepository.
9. Infrastructure adapter implements repository/output port.
10. Adapter maps CandidateAggregate to CandidateEntity.
11. CandidateJpaRepository saves CandidateEntity.
12. Return CandidateResponse.

Also implement GET /api/v1/candidates/{id}.

Create persistence entities:

- CandidateEntity mapped to candidates table
- CvEntity mapped to cvs table
- CandidateSkillEntity mapped to candidate_skills table
- ParsedCvData as JSON object
- CandidateStatus enum: ACTIVE, INACTIVE
- ParseStatus enum: PENDING, PARSED, FAILED

Keep domain independent:

- Do not import infrastructure classes into domain.
- Do not put JPA annotations in domain.
- Do not use Spring annotations in domain.
- Do not inject JpaRepository into application service.

Use DTO validation with jakarta.validation.
Use Lombok for getters/setters/builders.
Use MapStruct if mapping becomes large.
Add GlobalExceptionHandler for validation errors, illegal arguments, not found, and generic errors.
```
