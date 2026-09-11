# Upgrading CAS

A runbook for moving this dogu to a new Apereo CAS release. It explains **what to do
and in what order**, and — just as important — **what to re-verify**, because this overlay carries custom
Java and custom templates that depend on CAS internals which can shift between versions.

---

## 0. Keep in Mind when Upgrading Cas

These facts drive the whole upgrade:

1. **The CAS version dictates the Spring Boot and Tomcat versions.** `app/build.gradle` pulls
   `enforcedPlatform("org.apereo.cas:cas-server-support-bom:<cas.version>")`. Do **not** bump Spring Boot or
   the bundled Tomcat independently — align them to what the target CAS release expects.
2. **Every customization is a coupling to CAS.** Custom Java overrides CAS beans/webflow by name; custom
   templates render CAS model objects and post to CAS webflow events. A CAS upgrade can rename a bean, a
   webflow state, a property, or a template model field and break a customization **silently** (no compile
   error). Try to minimize the number of such changes.
3. Our customization does not ship Material-styles, but our own. So expect the need to **re-style**
   on new UI-Features. 

---

## 1. Bump the versions (as one coupled set)

| What | Where (in this repo) |
|---|---|
| CAS | `app/gradle.properties` → `cas.version` |
| Spring Boot plugin | `app/gradle.properties` → `springBootVersion` (must match the CAS BOM) |
| Tomcat (embedded ref) | `app/gradle.properties` → `tomcatVersion` |
| Tomcat (standalone, baked into the image — the one that actually serves) | `Dockerfile` → `TOMCAT_MAJOR_VERSION`, `TOMCAT_VERSION`, **and** `TOMCAT_TARGZ_SHA512` |
| Java | `gradle.properties`, `system.properties`, both Docker base images |

> Note on Tomcat: `app/build.gradle` excludes `org.apache.tomcat.embed`, so the war is **not** run on the
> embedded Tomcat — it is deployed into the **standalone** Tomcat baked into the image by the `Dockerfile`.
> The `Dockerfile` ARGs are therefore the authoritative Tomcat version. Keep `tomcatVersion` in
> `app/gradle.properties` aligned anyway (avoids confusion), and **always update `TOMCAT_TARGZ_SHA512`** —
> it is the SHA-512 of the downloaded `apache-tomcat-<version>.tar.gz`; a stale checksum fails the image
> build. Get the new checksum from Apache's published `.sha512` file for that Tomcat release.

### Where to read the *correct* Spring Boot and Tomcat versions for a CAS release

Do not guess and do not bump these independently. The versions a given CAS release is built and tested
against come from upstream, in this order of preference:

1. **The Apereo `cas-overlay-template` repo — the canonical source.** This project is structured after it.
   Check out the branch matching the target CAS **minor** line (e.g. `7.3`) and read its `gradle.properties`:
   it pins `springBootVersion` and references `tomcatVersion` (the Tomcat line is present as a comment,
   e.g. `# tomcatVersion=11.0.22`) for exactly that CAS line.
   → `https://github.com/apereo/cas-overlay-template/blob/<minor>/gradle.properties`
2. **Cross-check against the `cas-server-support-bom` POM** for the exact patch version — it is what
   `enforcedPlatform(...)` resolves at build time, so it is the ground truth for the *library* versions that
   end up on the classpath (here the patch version is included e.g. `7.3.7`).
   → `https://repo1.maven.org/maven2/org/apereo/cas/cas-server-support-bom/<cas.version>/cas-server-support-bom-<cas.version>.pom`
3. **The CAS release notes / `What's New` docs** for the headline Spring Boot / Java requirements and any
   breaking changes (see §8 for how to diff between versions).

Set `cas.version` to the target patch, then set `springBootVersion` (and the Tomcat versions) to whatever
that CAS line pins per the source above. Java follows from the Spring Boot baseline (Spring Boot 3.x → JDK
17+, this overlay uses 21).

---

## 2. Build + unit tests (fast inner loop — no running CAS needed)

The build needs **JDK 21+** (the Spring Boot plugin requires it). From the repo root:

```bash
cd app && JAVA_HOME=<path-to-jdk-21> ./gradlew clean build   # compile + full unit test suite
cd app && JAVA_HOME=<path-to-jdk-21> ./gradlew test          # just the tests
```

CI does the same in the Jenkins stage **Gradle Build & Test**: `clean build`
then `test`.

---

## 3. Rebuild the theme CSS *if you touched any template*

`app/src/main/resources/static/css/ces-theme-tailwind.css` is a **generated artifact**. Tailwind only emits
rules for class strings it finds while scanning the templates, so any class you add (including arbitrary
values like `w-[20px]`) is a silent no-op until the CSS is regenerated.

```bash
yarn install     # once, to populate node_modules
yarn tw          # regenerates static/css/ces-theme-tailwind.css
```

Commit the regenerated CSS together with the template change, then hard-refresh when testing in a browser.

---

## 4. Integration tests — single node

The Cypress suite (`integrationTests/`) runs against an **already-running** CAS; it does not boot CAS
itself. Bring CAS up first (manually for a local run, or the pipeline does it in CI), see [IT-Docs](Setup_Integrationtests_en.md)

---

## 5. Integration tests — multinode (Jenkins parameter)

The pipeline also runs the suite on a **multinode (k8s) ecosystem**. These are the `MN-*` stages
(`MN-Run Integration Tests` etc.), attached to the multinode agent via `pipe.agentMultinode` in the
`Jenkinsfile`.

The multinode run is **gated by the `PipelineMode` build parameter** supplied by `DoguPipe`
(pipe-build-lib), not hardcoded in the `Jenkinsfile`. To run it:

1. In Jenkins, open the job and choose **Build with Parameters**.
2. Set **`PipelineMode`** to **`INTEGRATIONMULTINODE`**.
3. Start the build. The `MN-*` stage group provisions the multinode ecosystem, sets up Keycloak (OIDC
   provider for the OIDC/OAuth specs) via the `integrationTests/k8s` manifests, deploys the CAS dogu, and
   runs the same Cypress specs.

---

## 6. Smoke test (manual — the things CI can't cover)

After the automated suites pass, do a quick manual pass against a running instance. Minimum:

- **Plain login / logout.**
- **CAS, OAuth and OIDC**: Install at least one cas-type, one oauth-type and one oidc-type dogu and test single log-in / single log-out with them (e.g. Redmine, Bluespice/Teamscale, ??? (oauth currently not present)).
- **Password change** during a forced "must change" login (create a new user with "change password at next login" enabled.
- **gauth MFA** (requires MFA enabled in CAS config + a CAS restart — **very important**, cause not covered by Cypress):
  - enroll a device (wrong token → inline error, stays on page; correct token → registered);
  - **delete a device**: a *wrong* code must show the confirmation view again with a red error and keep the
    device — **not** an HTTP 500; a valid OTP/scratch code removes it. *(A 500 here is the signal
    that the gauth bean override stopped working)*
- **federated login** (requires a federated provider configured in CAS config + a CAS restart):
  - **login** with a federated provider;
  - **logout** with a federated provider.

---

## 7. Customization inventory — files to watch out for

- Custom Java (`app/src/main/java/de/triology/cas/`)
- Custom templates & properties (`app/src/main/resources/`)
- Config templates (`resources/etc/cas/config/*.tpl`)
  - `cas.properties.tpl` carries deliberate overrides of CAS defaults. When CAS changes a default, re-evaluate whether the override is still
    needed or now conflicts.

---

## 8. Tips — how to actually find what changed

The hard part of a CAS upgrade is not bumping the version; it is finding the **silent** breakages where CAS
renamed a bean, moved a webflow state, changed a property default, or edited a stock template/action that we
copied and customized. None of these fail the compile. The tests above should uncover the problems. The following tips 
and techniques can help you analyze these problems.

### 8.1 Diff the upstream `cas-overlay-template` between the two lines first

This is the cheapest, highest-signal step. The overlay template is small and shows exactly what the *project
scaffold* changed (Gradle, plugins, Dockerfile, base config).

⚠️ **GitHub's `/compare/<old>...<new>` does NOT work here.** The overlay template's per-line branches are each
*auto-generated* from the CAS Initializr — they share **no common commit history**, so GitHub reports "there
isn't anything to compare / entirely different commit histories" and shows an empty diff. Don't rely on it.

Diff the **file trees** directly instead. Clone once and compare the two branches' working trees (history is
irrelevant, so `--no-index` sidesteps the unrelated-histories problem):

```bash
git clone https://github.com/apereo/cas-overlay-template.git
cd cas-overlay-template
git worktree add ../ovl-old origin/<old-minor>     # e.g. 7.2
git worktree add ../ovl-new origin/<new-minor>     # e.g. 7.3
git --no-pager diff --no-index ../ovl-old ../ovl-new
# or just eyeball the few files that matter:
#   gradle.properties  build.gradle  settings.gradle  Dockerfile  gradle/  src/main/resources/
```

(For a quick look without cloning, open the key files on each branch and compare by eye — the set worth
checking is small.) Anything that moved (a new exclude, a changed plugin, a new property, a Tomcat bump) you
almost certainly need to mirror.

### 8.2 Read the CAS "Release Notes" / "What's New" pages, minor by minor

CAS documents breaking changes per **minor** release. If you cross more than one minor, read **each** page in
between, not just the target — breaking changes accumulate.

- Release notes index: `https://apereo.github.io/cas/<minor>.x/release_notes/` (e.g. `7.3.x`).
- These call out removed/renamed properties, dropped modules, and behavior changes (this is how the pac4j
  session-replication default flip and the `PasswordChangeAction` current-password requirement were found).

### 8.3 Re-verify customization against the CAS sources it couples to

To read the stock CAS source a comment points at, pull it out of the dependency jars you already downloaded
(prefer the `-sources` jar — it has the real Java/templates, no decompile needed):

```bash
# locate the sources jar for a module, then dump one file
find ~/.gradle/caches/modules-2 -name 'cas-server-support-gauth-core-*-sources.jar'
unzip -p <that-sources.jar> org/apereo/cas/gauth/web/flow/GoogleAuthenticatorDeleteAccountAction.java
unzip -Z1 <that-sources.jar> | grep -i deleteaccount   # find the path first if unsure
```

You can also browse the same source on GitHub at the tag: `https://github.com/apereo/cas/tree/v<version>`
(actions under `support/cas-server-support-<module>/src/main/java`, templates under `.../resources/templates`).

#### Threeway diff for customized cas-files

1. Extract the stock original at **both** the old and new versions from the respective `-sources` jars (command above).
2. Diff **old-stock vs new-stock** → what CAS changed (and might break our copy).
3. Diff **old-stock vs ours** → our customization.
4. Re-apply our delta onto the new stock file.

### 8.4 Let the running app tell you about dead properties

This overlay already depends on `spring-boot-properties-migrator` (`app/build.gradle`). On **startup** it
logs every property in your config that the new Spring Boot/CAS has **renamed or removed**, with the
replacement key. So:

- Boot the upgraded CAS once and **read the startup log** before anything else. Grep for `migrat`,
  `deprecat`, and `WARN`.
- CAS itself also logs unknown/loose config keys. Treat every such warning as a to-do against
  `resources/etc/cas/config/*.tpl`.
- Remove the migrator again only if you want — it is harmless but noisy; we keep it during upgrades.

---