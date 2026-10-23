# Development Mode

For local testing of some Dogus, it may be necessary to set CAS to development mode.
This causes all applications to be able to authenticate via CAS, even if they are not
configured there.

``kubectl edit -n ecosystem configmap global-config``
`````yaml
  data:
    config.yaml: |
      stage: development
  `````
