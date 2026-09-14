# Service Registry

CAS allows you to declare services that may be used for CAS authentication. The service registry stores the services
together with metadata and thus controls the behavior of the CAS.

There are [various implementations](https://apereo.github.io/cas/7.0.x/services/Service-Management.html#storage) that can be used for the service registry.
We currently use the [JSON Service Registry](https://apereo.github.io/cas/7.0.x/services/JSON-Service-Management.html), in which the services are stored as JSON and are loaded into the memory at runtime.

A central storage location is defined for the registry, but this may differ depending on the [Stage](develop_stage_en.md) currently used.
If the CAS is in production mode, the services are loaded from the path `/etc/cas/services/production`. The services are created dynamically based on the actually installed Dogus. 
For [development mode](develop_stage_en.md), static services for the protocols CAS, OAUTH and OIDC  are declared under the path `/etc/cas/services/development`, which apply generically for the respective protocols.
There is no differentiation between individual applications.

Services are usually created during the installation process of a dogu. If a dogu has a dependency on the CAS, the ExposedCommand `service-account-create` is called during the installation, which creates the service as a JSON configuration in the service registry.
Configuration templates are used for this, which are stored under the path `/etc/cas/config/services`. Depending on the input parameters and the type of protocol used, various [Properties](https://apereo.github.io/cas/7.0.x/services/Configuring-Service-Custom-Properties.html) are filled,
which are then used by [CAS internal templates](https://apereo.github.io/cas/7.0.x/services/Configuring-Service-Template-Definitions.html). 
These templates are referenced by their name and serve as a template for the CAS services generated in the memory. They can be found under `/etc/cas/services/templates`. 
The final generated JSON files in the service registry always follow the naming convention `<application>-<serviceID>.json`.

Production generator templates must write `name` and `serviceId` as top-level fields in every generated service record. CAS also supplies these fields through the internal `BaseService` template during normal service-manager loading, but registry records can be read before that expansion. Keeping both fields in the generated JSON ensures such records remain valid for service matching and sorting. `serviceId` is a regular expression; the production generators derive it from `FQDN` and `ServiceName` using the same expression as `BaseService.json`.
