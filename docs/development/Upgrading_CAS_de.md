# CAS aktualisieren

Ein Leitfaden, um dieses Dogu auf ein neues Apereo-CAS-Release zu heben. Er beschreibt, **was zu tun
ist und in welcher Reihenfolge**, und — genauso wichtig — **was erneut zu prüfen ist**, denn dieses Overlay
bringt eigenen Java-Code und eigene Templates mit, die von CAS-Interna abhängen, welche sich zwischen
Versionen verändern können.

---

## 0. Was beim CAS-Upgrade zu beachten ist

Diese Tatsachen prägen das gesamte Upgrade:

1. **Die CAS-Version gibt die Spring-Boot- und Tomcat-Versionen vor.** `app/build.gradle` zieht
   `enforcedPlatform("org.apereo.cas:cas-server-support-bom:<cas.version>")`. Hebe Spring Boot oder das
   gebündelte Tomcat **nicht** unabhängig an — richte sie an dem aus, was das Ziel-CAS-Release erwartet.
2. **Jede Anpassung ist eine Kopplung an CAS.** Eigener Java-Code überschreibt CAS-Beans/Webflow namentlich;
   eigene Templates rendern CAS-Modellobjekte und posten an CAS-Webflow-Events. Ein CAS-Upgrade kann eine
   Bean, einen Webflow-State, eine Property oder ein Template-Modellfeld umbenennen und eine Anpassung
   **stillschweigend** brechen (kein Compile-Fehler). Versuche solche Abweichungen zu minimieren.
3. Unsere Anpassung liefert keine Material-Styles aus, sondern unsere eigenen. Rechne also bei neuen
   UI-Features mit der Notwendigkeit, **nachzustylen**.

---

## 1. Die Versionen anheben (als ein gekoppeltes Set)

| Was | Wo (in diesem Repo) |
|---|---|
| CAS | `app/gradle.properties` → `cas.version` |
| Spring-Boot-Plugin | `app/gradle.properties` → `springBootVersion` (muss zur CAS-BOM passen) |
| Tomcat (Embedded-Referenz) | `app/gradle.properties` → `tomcatVersion` |
| Tomcat (standalone, ins Image gebacken — das, das tatsächlich ausliefert) | `Dockerfile` → `TOMCAT_MAJOR_VERSION`, `TOMCAT_VERSION`, **und** `TOMCAT_TARGZ_SHA512` |
| Java | `gradle.properties`, `system.properties`, beide Docker-Base-Images |

> Hinweis zu Tomcat: `app/build.gradle` schließt `org.apache.tomcat.embed` aus, die War wird also **nicht**
> auf dem eingebetteten Tomcat ausgeführt — sie wird in das **standalone**-Tomcat deployt, das vom
> `Dockerfile` ins Image gebacken wird. Die `Dockerfile`-ARGs sind daher die maßgebliche Tomcat-Version.
> Halte `tomcatVersion` in `app/gradle.properties` trotzdem abgeglichen (vermeidet Verwirrung), und
> **aktualisiere immer `TOMCAT_TARGZ_SHA512`** — es ist die SHA-512 der heruntergeladenen
> `apache-tomcat-<version>.tar.gz`; eine veraltete Prüfsumme lässt den Image-Build fehlschlagen. Beziehe die
> neue Prüfsumme aus Apaches veröffentlichter `.sha512`-Datei für dieses Tomcat-Release.

### Wo man die *korrekten* Spring-Boot- und Tomcat-Versionen für ein CAS-Release nachliest

Rate nicht und hebe diese nicht unabhängig an. Die Versionen, gegen die ein bestimmtes CAS-Release gebaut und
getestet wird, kommen von upstream, in dieser Präferenzreihenfolge:

1. **Das Apereo-Repo `cas-overlay-template` — die kanonische Quelle.** Dieses Projekt ist danach aufgebaut.
   Checke den Branch aus, der zur **Minor**-Linie des Ziel-CAS passt (z. B. `7.3`), und lies dessen
   `gradle.properties`: es pinnt `springBootVersion` und referenziert `tomcatVersion` (die Tomcat-Zeile ist
   als Kommentar vorhanden, z. B. `# tomcatVersion=11.0.22`) für genau diese CAS-Linie.
   → `https://github.com/apereo/cas-overlay-template/blob/<minor>/gradle.properties`
2. **Gegenprüfen anhand des `cas-server-support-bom`-POM** für die exakte Patch-Version — es ist das, was
   `enforcedPlatform(...)` zur Build-Zeit auflöst, also die Grundwahrheit für die *Library*-Versionen, die
   auf dem Classpath landen (hier ist die Patch-Version enthalten, z. B. `7.3.7`).
   → `https://repo1.maven.org/maven2/org/apereo/cas/cas-server-support-bom/<cas.version>/cas-server-support-bom-<cas.version>.pom`
3. **Die CAS-Release-Notes / `What's New`-Doku** für die wichtigsten Spring-Boot-/Java-Anforderungen und
   etwaige Breaking Changes (siehe §8, wie man zwischen Versionen difft).

Setze `cas.version` auf den Ziel-Patch, dann setze `springBootVersion` (und die Tomcat-Versionen) auf das,
was diese CAS-Linie gemäß der obigen Quelle pinnt. Java ergibt sich aus der Spring-Boot-Baseline (Spring Boot
3.x → JDK 17+, dieses Overlay nutzt 21).

---

## 2. Build + Unit-Tests (schnelle innere Schleife — kein laufendes CAS nötig)

Der Build benötigt **JDK 21+** (das Spring-Boot-Plugin verlangt es). Vom Repo-Root aus:

```bash
cd app && JAVA_HOME=<path-to-jdk-21> ./gradlew clean build   # kompilieren + vollständige Unit-Test-Suite
cd app && JAVA_HOME=<path-to-jdk-21> ./gradlew test          # nur die Tests
```

CI macht dasselbe in der Jenkins-Stage **Gradle Build & Test**: `clean build`
dann `test`.

---

## 3. Das Theme-CSS neu bauen, *falls du irgendein Template angefasst hast*

`app/src/main/resources/static/css/ces-theme-tailwind.css` ist ein **generiertes Artefakt**. Tailwind gibt
nur Regeln für Klassen-Strings aus, die es beim Scannen der Templates findet, daher ist jede Klasse, die du
hinzufügst (inklusive beliebiger Werte wie `w-[20px]`), ein stilles No-op, bis das CSS neu generiert wird.

```bash
yarn install     # einmalig, um node_modules zu befüllen
yarn tw          # regeneriert static/css/ces-theme-tailwind.css
```

Committe das neu generierte CSS zusammen mit der Template-Änderung und mache dann beim Testen im Browser ein
Hard-Refresh.

---

## 4. Integrationstests — Single Node

Die Cypress-Suite (`integrationTests/`) läuft gegen ein **bereits laufendes** CAS; sie bootet CAS nicht
selbst. Bringe CAS zuerst hoch (manuell für einen lokalen Lauf, oder die Pipeline tut es in der CI), siehe
[IT-Docs](Setup_Integrationtests_de.md)

---

## 5. Integrationstests — Multinode (Jenkins-Parameter)

Die Pipeline führt die Suite auch auf einem **Multinode-(k8s-)Ecosystem** aus. Das sind die `MN-*`-Stages
(`MN-Run Integration Tests` usw.), die über `pipe.agentMultinode` im `Jenkinsfile` an den Multinode-Agent
gehängt werden.

Der Multinode-Lauf ist **durch den Build-Parameter `PipelineMode`** gesteuert, der von `DoguPipe`
(pipe-build-lib) geliefert wird, nicht im `Jenkinsfile` hartcodiert. Um ihn auszuführen:

1. Öffne in Jenkins den Job und wähle **Build with Parameters**.
2. Setze **`PipelineMode`** auf **`INTEGRATIONMULTINODE`**.
3. Starte den Build. Die `MN-*`-Stage-Gruppe provisioniert das Multinode-Ecosystem, richtet Keycloak
   (OIDC-Provider für die OIDC/OAuth-Specs) über die `integrationTests/k8s`-Manifeste ein, deployt das
   CAS-Dogu und führt dieselben Cypress-Specs aus.

---

## 6. Smoke-Test (manuell — die Dinge, die CI nicht abdecken kann)

Nachdem die automatisierten Suites durchlaufen, mache einen kurzen manuellen Durchgang gegen eine laufende
Instanz. Minimum:

- **Einfaches Login / Logout.**
- **CAS, OAuth und OIDC**: Installiere mindestens ein cas-Typ-, ein oauth-Typ- und ein oidc-Typ-Dogu und
  teste Single-Login / Single-Logout mit ihnen (z. B. Redmine, Bluespice/Teamscale, ??? (OAuth aktuell nicht
  vorhanden)).
- **Passwortänderung** während eines erzwungenen „muss ändern"-Logins (lege einen neuen Benutzer mit
  „Passwort beim nächsten Login ändern" an).
- **gauth MFA** (erfordert in der CAS-Config aktiviertes MFA + einen CAS-Neustart — **sehr wichtig**, da
  nicht von Cypress abgedeckt):
  - ein Gerät registrieren (falscher Token → Inline-Fehler, bleibt auf der Seite; korrekter Token →
    registriert);
  - **ein Gerät löschen**: ein *falscher* Code muss die Bestätigungsansicht erneut mit einem roten Fehler
    zeigen und das Gerät behalten — **kein** HTTP 500; ein gültiger OTP-/Scratch-Code entfernt es. *(Ein 500
    hier ist das Signal, dass das Override der gauth-Bean nicht mehr funktioniert.)*
- **Föderiertes Login** (erfordert einen in der CAS-Config konfigurierten föderierten Provider + einen
  CAS-Neustart):
  - **Login** mit einem föderierten Provider;
  - **Logout** mit einem föderierten Provider.

---

## 7. Anpassungs-Inventar — Dateien, auf die zu achten ist

- Eigener Java-Code (`app/src/main/java/de/triology/cas/`)
- Eigene Templates & Properties (`app/src/main/resources/`)
- Config-Templates (`resources/etc/cas/config/*.tpl`)
  - `cas.properties.tpl` enthält bewusste Overrides von CAS-Defaults. Wenn CAS einen Default ändert,
    bewerte neu, ob das Override noch nötig ist oder nun in Konflikt steht.

---

## 8. Tipps — wie man tatsächlich findet, was sich geändert hat

Der schwierige Teil eines CAS-Upgrades ist nicht das Anheben der Version; es ist das Finden der **stillen**
Breakages, bei denen CAS eine Bean umbenannt, einen Webflow-State verschoben, einen Property-Default geändert
oder ein Standard-Template/eine Standard-Action bearbeitet hat, das/die wir kopiert und angepasst haben.
Keine davon lässt den Compile fehlschlagen. Die obigen Tests sollten diese Probleme aufdecken. Zum Analysieren können 
folgende Tipps und Techniken helfen:

### 8.1 Zuerst das upstream `cas-overlay-template` zwischen den beiden Linien diffen

Das ist der günstigste Schritt mit dem höchsten Signal. Das Overlay-Template ist klein und zeigt genau, was
das *Projekt-Gerüst* geändert hat (Gradle, Plugins, Dockerfile, Base-Config).

⚠️ **GitHubs `/compare/<old>...<new>` funktioniert hier NICHT.** Die per-Linie-Branches des Overlay-Templates
sind jeweils *automatisch generiert* aus dem CAS-Initializr — sie teilen sich **keine gemeinsame
Commit-Historie**, daher meldet GitHub „there isn't anything to compare / entirely different commit
histories" und zeigt einen leeren Diff. Verlasse dich nicht darauf.

Diffe stattdessen die **Datei-Bäume** direkt. Klone einmal und vergleiche die Working-Trees der beiden
Branches (die Historie ist irrelevant, daher umgeht `--no-index` das Problem der unverwandten Historien):

```bash
git clone https://github.com/apereo/cas-overlay-template.git
cd cas-overlay-template
git worktree add ../ovl-old origin/<old-minor>     # z. B. 7.2
git worktree add ../ovl-new origin/<new-minor>     # z. B. 7.3
git --no-pager diff --no-index ../ovl-old ../ovl-new
# oder einfach die wenigen Dateien anschauen, die zählen:
#   gradle.properties  build.gradle  settings.gradle  Dockerfile  gradle/  src/main/resources/
```

(Für einen schnellen Blick ohne Klonen: öffne die Schlüsseldateien auf jedem Branch und vergleiche per Auge —
die relevante Menge ist klein.) Alles, was sich verschoben hat (ein neuer Exclude, ein geändertes Plugin,
eine neue Property, ein Tomcat-Bump), musst du fast sicher nachziehen.

### 8.2 Die CAS-„Release Notes" / „What's New"-Seiten lesen, Minor für Minor

CAS dokumentiert Breaking Changes pro **Minor**-Release. Wenn du mehr als ein Minor überspringst, lies
**jede** dazwischenliegende Seite, nicht nur das Ziel — Breaking Changes akkumulieren sich.

- Release-Notes-Index: `https://apereo.github.io/cas/<minor>.x/release_notes/` (z. B. `7.3.x`).
- Diese benennen entfernte/umbenannte Properties, weggefallene Module und Verhaltensänderungen (so wurden
  der Flip des pac4j-Session-Replication-Defaults und die Current-Password-Anforderung der
  `PasswordChangeAction` gefunden).

### 8.3 Anpassung gegen die CAS-Quellen, an die sie gekoppelt ist, erneut prüfen

Um den CAS-Standard-Quellcode zu lesen, auf den ein Kommentar zeigt, ziehe ihn aus den
Dependency-Jars, die du bereits heruntergeladen hast (bevorzuge das `-sources`-Jar — es enthält das echte
Java/die echten Templates, kein Dekompilieren nötig):

```bash
# das Sources-Jar für ein Modul finden, dann eine Datei ausgeben
find ~/.gradle/caches/modules-2 -name 'cas-server-support-gauth-core-*-sources.jar'
unzip -p <that-sources.jar> org/apereo/cas/gauth/web/flow/GoogleAuthenticatorDeleteAccountAction.java
unzip -Z1 <that-sources.jar> | grep -i deleteaccount   # zuerst den Pfad finden, falls unsicher
```

Du kannst denselben Quellcode auch auf GitHub am Tag durchstöbern: `https://github.com/apereo/cas/tree/v<version>`
(Actions unter `support/cas-server-support-<module>/src/main/java`, Templates unter `.../resources/templates`).

#### Threeway-Diff für angepasste CAS-Dateien

1. Extrahiere das Standard-Original an **beiden** Versionen, der alten und der neuen, aus den jeweiligen
   `-sources`-Jars (Befehl oben).
2. Diffe **alt-Standard vs. neu-Standard** → was CAS geändert hat (und unsere Kopie brechen könnte).
3. Diffe **alt-Standard vs. unsere** → unsere Anpassung.
4. Wende unser Delta erneut auf die neue Standard-Datei an.

### 8.4 Die laufende App über tote Properties berichten lassen

Dieses Overlay hängt bereits von `spring-boot-properties-migrator` ab (`app/build.gradle`). Beim **Start**
loggt es jede Property in deiner Config, die das neue Spring Boot/CAS **umbenannt oder entfernt** hat, mit dem
Ersatz-Key. Also:

- Boote das aktualisierte CAS einmal und **lies das Startup-Log** vor allem anderen. Grep nach `migrat`,
  `deprecat` und `WARN`.
- CAS selbst loggt ebenfalls unbekannte/lose Config-Keys. Behandle jede solche Warnung als To-do gegen
  `resources/etc/cas/config/*.tpl`.
- Entferne den Migrator erst wieder, wenn du willst — er ist harmlos, aber laut; wir behalten ihn während
  Upgrades.

---
