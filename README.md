# Usability Hunter

Automated usability testing for websites — a Java RESTful API that checks common usability heuristics and returns the results as JSON.

This project was developed as a graduation thesis (*Trabalho de Graduação*) at **Faculdade de Tecnologia de São José dos Campos – ETEP**, by **Andris Araújo Andermann**, advised by **Prof. Esp. Marcelo Augusto Sudo**.

📄 Full thesis (PDF): [Usability Hunter – Teste Automatizado de Usabilidade](https://drive.google.com/file/d/13vXXaQbY6dW2SaktZVDhGMsodUhT6IJt/view?usp=sharing)

---

## What it does

Many sites are built from the developer’s own intuition instead of proven usability heuristics. That often leads to confusing navigation and a poor user experience.

**Usability Hunter** helps by automating checks for five heuristics. You send a page URL via HTTP GET; the API screenshots and analyzes the page (with Google Vision, PageSpeed, and mobile-friendliness APIs) and returns a JSON report.

```
GET /uhunter/runtests?url={url_to_test}
```

Example:

```
http://localhost:8080/uhunter/runtests?url=https://www.wikipedia.org
```

The original thesis paths still work as aliases: `/uhunter/urest/runtest` and `/rest/urest/runtest`.

Error responses are JSON (`{"error": "..."}`):

| Status | When |
| --- | --- |
| `400` | `url` missing, malformed, not `http(s)`, or points to a private/loopback/link-local address (SSRF guard) |
| `503` + `Retry-After` | All worker threads and the queue are busy; retry shortly |
| `504` | The checks exceeded `uhunter.validation-timeout` (default `4m`) |
| `500` | Any other failure |

---

## Run locally

Requires **Java 17+** and Maven (or the Maven Wrapper once generated).

1. Copy the local config template and fill in your keys:

```bash
cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties
```

2. Activate the `local` profile.

**IntelliJ:** Run → Edit Configurations… → select `UsabilityHunterApplication` → **Active profiles:** `local`

**Maven:**

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

3. Open [http://localhost:8080](http://localhost:8080).

`application-local.properties` is gitignored so secrets stay off the repo.

**Google Cloud Vision** (logo + navigation OCR) reads credentials from `uhunter.google-application-credentials`, then `GOOGLE_APPLICATION_CREDENTIALS`, then Application Default Credentials:

```bash
gcloud auth application-default login
# or set GOOGLE_APPLICATION_CREDENTIALS=/absolute/path/to/service-account.json
```

Enable the **PageSpeed Insights API** on the Google Cloud project that owns your API key.

### Screenshot provider (Strategy pattern)

Screenshots are pluggable via `uhunter.screenshot-provider`:

| Value | Behavior |
| --- | --- |
| `pagespeed` (default) | Uses Lighthouse screenshots from PageSpeed Insights (prefers **full-page**, falls back to final thumbnail) |
| `phantomjs` | Legacy PhantomJsCloud capture |

Switch in `application-local.properties`:

```properties
uhunter.screenshot-provider=pagespeed
# uhunter.screenshot-provider=phantomjs
```

---

## Heuristics tested

### 1. Logo in the top-left corner

Users often look first at the top-left to confirm they are on the right site. The logo also acts as a “home” affordance. Usability Hunter crops that region from a page screenshot and uses Google Vision in cascade: **logo detection** → **web detection** (for brands missing from the logo catalog) → **OCR text**, then matches against page brand candidates (title, host, acronyms).

<p align="center">
  <img src="imgTest/youtube.jpg" alt="YouTube homepage with logo in the top-left" width="720" />
</p>

<p align="center"><em>Example: YouTube places the brand logo in the top-left — the region Usability Hunter inspects.</em></p>

### 2. Navigation on the left

Research (e.g. Bailey, 2006) suggests left-side navigation helps users find content faster. The API analyzes the left portion of the screenshot (Vision + HTML links) to decide whether primary navigation sits on the left.

<p align="center">
  <img src="imgTest/wikipediaPage.jpg" alt="Portuguese Wikipedia with left sidebar navigation" width="720" />
</p>

<p align="center"><em>Example: Wikipedia’s left sidebar — a classic left-navigation layout used in the thesis tests.</em></p>

<p align="center">
  <img src="imgTest/wikipediaNavigation.jpg" alt="Cropped Wikipedia left navigation menu" width="220" />
</p>

<p align="center"><em>Cropped left-nav region (sample from <code>imgTest/</code>), similar to what the navigation check focuses on.</em></p>

### 3. Hick’s law

Hick’s law: more unordered choices make selection slower. When left navigation has enough items (≥ 5), Usability Hunter checks whether those items are organized in a familiar way (e.g. alphabetical order). If there are too few items, the result is `not applicable`.

<p align="center">
  <img src="imgTest/uspNavigation.jpg" alt="USP course page with a long left navigation list" width="320" />
</p>

<p align="center"><em>Example: a long left menu (USP course page). With many items, order and familiarity matter — that is what the Hick’s law check evaluates.</em></p>

### 4. Responsiveness (mobile-friendly)

Google’s Mobile-Friendly Test API was retired in 2023, so Usability Hunter now runs **local HTML heuristics** (viewport meta tag, fixed width, legacy plugins). If checks fail, `listOfIssues` explains why.

### 5. Performance

Uses **PageSpeed Insights API v5** for **desktop** and **mobile**, including:

- **optimizationPoints** — estimated improvement needed (0–100; derived from Lighthouse performance score)
- **loadingOverall** — `FAST`, `AVERAGE`, `SLOW`, or CrUX overall category when available
- **FCP** and **LCP**, using Chrome UX Report field data when present, otherwise Lighthouse lab metrics

---

## JSON response (overview)

A successful run returns four top-level sections (names as in the API):

| Key | Meaning |
| --- | --- |
| `logoOnTopLeft` | `result` (boolean) and optional `logoName` |
| `navigationOnLeftCornerTest` | `navigationOnLeftCorner` + `hicksLaw` (`true` / `false` / `"not applicable"`) |
| `isMobileFriendlyTest` | `isMobileFriendly` and optional `listOfIssues` |
| `performanceTest` | Nested `mobileTest` and `desktopTest` metrics |

The thesis walks through a full Wikipedia example in **Figura 7** of the PDF.

---

## Tech stack

| Area | Technologies |
| --- | --- |
| Language / API | Java 17, Spring Boot 3, Maven |
| Page capture | Strategy: PageSpeed/Lighthouse screenshots (default) or PhantomJsCloud |
| Vision / OCR | Google Cloud Vision |
| HTML parsing | Jsoup |
| Performance / mobile | Google PageSpeed Insights v5 + local HTML mobile heuristics |

Orchestration lives in `UsabilityValidationService` / `service.impl.UsabilityValidationServiceImpl`; the REST entry point is `UsabilityController` (`/uhunter/runtests`, plus the legacy `urest` aliases). Per request, `AnalysisContextFactory` does the shared slow setup once (HTML fetch and screenshot) and returns an immutable `AnalysisContext`; the four heuristics then run in parallel as `UsabilityCheck`s that all take just that context.

| Package | Contents |
| --- | --- |
| `api`, `api.dto` | Controller, `ApiExceptionHandler`, response records (`*Dto`) |
| `service`, `service.impl` | Orchestrator: builds the context, runs the checks in parallel with timeout and back-pressure |
| `validations` | `UsabilityCheck`, `AbstractUsabilityCheck` (shared error fallback), `AnalysisContext`, `AnalysisContextFactory` |
| `validations.{logo,navigation,responsive,performance}` | One `*Check` Spring bean each, plus the algorithm in `utils` (pure static helpers) or, for performance, `PerformanceAnalysis` / `PerformanceMetric` |
| `page` | `PageDocument`: the fetched, parsed HTML (SSRF-checked, redirects re-validated) |
| `screenshot` | `Screenshot` (immutable value: normalizes to JPEG, cuts regions), `ScreenshotProvider` strategy and its two implementations |
| `pagespeed` | `PageSpeedInsightsClient`: application-wide bean with a bounded TTL cache and in-flight de-duplication |
| `vision` | `VisionApi` interface, `GoogleVisionApi` (owns the Vision client lifecycle, lazy), `VisionTextBlock` |
| `security` | `UrlSafetyValidator` (SSRF guard) |
| `config` | `UhunterProperties`, `AsyncConfig` (shared executor), `ScreenshotConfig` (picks the provider bean) |
| `utils` | `ImageUtils` only: pure, stateless image helpers |

Vision and PageSpeed are injected, not static, so checks are unit-tested with fakes (`FakeVision`, a stubbed `PageSpeedInsightsClient`). Heuristic responses keep the thesis JSON keys and may also include optional `confidence` / `evidence` fields.

This codebase was modernized from the original thesis stack (Jersey 1 + Google App Engine WAR + vendored JARs) to Spring Boot with Maven-managed dependencies.

---

## Sample images in this repo

The `imgTest/` folder holds screenshots used while developing and validating the heuristics (Wikipedia, Google, YouTube, Stack Overflow, USP, and cropped navigation/logo regions). They are useful reference fixtures for understanding what the tool looks at.

---

## Academic context

> *“Este artigo descreve uma API RESTful, chamada de Usability Hunter, que testa cinco dessas heurísticas: logotipo e navegação à esquerda, lei de Hick, responsividade e performance.”*

Usability Hunter was built as a prototype to plug into a website development workflow: developers can call the API and get an automated first pass on heuristics that are often skipped when teams lack time or usability specialists.

---

## License / authorship

Thesis and project by **Andris Araújo Andermann**. See the [thesis PDF](https://drive.google.com/file/d/13vXXaQbY6dW2SaktZVDhGMsodUhT6IJt/view?usp=sharing) for full references and copyright notes.
