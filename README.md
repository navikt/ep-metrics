# eessi-pensjon-metrics

## Releasing

This library is released using the `net.researchgate/gradle-release`-plugin.

## OWASP avhengighetssjekk

(Pass på at du kan nå `ossindex.sonatype.org` og `nvd.nist.gov` gjennom evt proxy e.l.) 

```
./gradlew dependencyCheckAnalyze && open build/reports/dependency-check-report.html
```
