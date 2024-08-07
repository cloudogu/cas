# Update des "CES-Theme-Tailwind" in den thymeleaf-templates

Um das Build von CAS nicht zu kompliziert zu machen, aber dennoch das cas-theme-tailwind zu verwenden, wurde ein 
zusätzliches npm-Projekt erstellt, welches das Theme einbindet und eine css-Datei generiert, die eingecheckt werden 
muss.
In allen templates von cas kann wie gewohnt tailwind verwendet werden und anschließend muss die CSS-Datei generiert 
werden.

Um die CSS-Datei zu generieren, muss vorher noch das Theme ausgecheckt werden. Dafür im root des Projektes `make 
gen-npmrc-release` ausführen und die Credentials von ecosystem.cloudogu.com angeben.
Gegebenenfalls kann noch die Version des `ces-theme-tailwind` in der package.json angepasst werden.
Anschließend mit `yarn install` alle dependencies herunterladen.
Durch das Ausführen von `yarn tw` wird jetzt die css-Datei generiert. Es muss nichts weiter beachtet werden, als diese
Datei anschließend einzuchecken.