MAKEFILES_VERSION=9.3.2

.DEFAULT_GOAL:=dogu-release

include build/make/variables.mk
include build/make/self-update.mk
include build/make/release.mk
include build/make/k8s-dogu.mk
include build/make/bats.mk

NPM_REGISTRY_RELEASE=ecosystem.cloudogu.com/nexus/repository/npm-releases/
NPM_URL_RELEASE=https://${NPM_REGISTRY_RELEASE}
NPM_REGISTRY_RC=ecosystem.cloudogu.com/nexus/repository/npm-releasecandidates/
NPM_URL_RC=https://${NPM_REGISTRY_RC}

.PHONY gen-npmrc-release:
gen-npmrc-release:
	@rm -f .npmrc
	@echo "email=jenkins@cloudogu.com" >> .npmrc
	@echo "always-auth=true" >> .npmrc
	@echo "//${NPM_REGISTRY_RELEASE}:_auth=\"$(shell bash -c 'read -p "Username: " usrname;read -s -p "Password: " pwd;echo -n "$$usrname:$$pwd" | openssl base64')\"" >> .npmrc
	@echo "@cloudogu:registry=${NPM_URL_RELEASE}" >> .npmrc
	@echo "@cloudogu:registry=https://registry.npmjs.org" > integrationTests/.npmrc

.PHONY gen-npmrc-prerelease:
gen-npmrc-prerelease:
	@rm -f .npmrc
	@echo "email=jenkins@cloudogu.com" >> .npmrc
	@echo "always-auth=true" >> .npmrc
	@echo "//${NPM_REGISTRY_RC}:_auth= \"$(shell bash -c 'read -p "Username: " usrname;read -s -p "Password: " pwd;echo -n "$$usrname:$$pwd" | openssl base64')\"" >> .npmrc
	@echo "@cloudogu:registry=${NPM_URL_RC}" >> .npmrc
	@echo "@cloudogu:registry=https://registry.npmjs.org" > integrationTests/.npmrc
