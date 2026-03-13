#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail

# this function will be sourced from release.sh and be called from release_functions.sh
update_versions_modify_files() {
  newReleaseVersion="${1}"
  valuesYAML=k8s/helm/values.yaml
  componentPatchTplYAML=k8s/helm/component-patch-tpl.yaml

  yq -i ".containers.cas.image.tag = \"${newReleaseVersion}\"" "${valuesYAML}"
  yq -i ".values.images.cas |= sub(\":(([0-9]+)\.([0-9]+)\.([0-9]+)((?:-([0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*))|(?:\+[0-9A-Za-z-]+))?)\", \":${newReleaseVersion}\")" "${componentPatchTplYAML}"

  local chownImageTag
  chownImageTag=$(./.bin/yq ".initContainers.volumeChown.image.tag" "${valuesYAML}")
  yq -i ".values.images.volumeChownInit |= sub(\":(([0-9]+)\.([0-9]+)\.([0-9]+)((?:-([0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*))|(?:\+[0-9A-Za-z-]+))?)\", \":${chownImageTag}\")" "${componentPatchTplYAML}"

  local additionalMountsImageTag
  additionalMountsImageTag=$(./.bin/yq ".initContainers.additionalMounts.image.tag" "${valuesYAML}")
  yq -i ".values.images.additionalMountsInit |= sub(\":(([0-9]+)\.([0-9]+)\.([0-9]+)((?:-([0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*))|(?:\+[0-9A-Za-z-]+))?)\", \":${additionalMountsImageTag}\")" "${componentPatchTplYAML}"

  make copy-dogu-spec
}

update_versions_stage_modified_files() {
  valuesYAML=k8s/helm/values.yaml
  componentPatchTplYAML=k8s/helm/component-patch-tpl.yaml
  doguJson=k8s/helm/dogu.json

  git add "${valuesYAML}" "${componentPatchTplYAML}" "${doguJson}"
}
