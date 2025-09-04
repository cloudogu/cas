# Login to master realm (admin/admin)
sudo docker exec -it $(sudo docker ps -qf name=keycloak) \
  /opt/keycloak/bin/kcadm.sh config credentials \
  --server http://localhost:8080 --realm master --user admin --password admin

# Create a test realm
sudo docker exec -it $(sudo docker ps -qf name=keycloak) \
  /opt/keycloak/bin/kcadm.sh create realms -s realm=Test -s enabled=true

# Create the OIDC client "cas"
# NOTE: replace the redirect URI host with your CAS public URL/host/IP
REDIRECT='["https://192.168.56.2/cas/login*"]'

sudo docker exec -it $(sudo docker ps -qf name=keycloak) \
  /opt/keycloak/bin/kcadm.sh create clients -r Test \
  -s clientId=cas \
  -s enabled=true \
  -s protocol=openid-connect \
  -s publicClient=false \
  -s standardFlowEnabled=true \
  -s implicitFlowEnabled=false \
  -s directAccessGrantsEnabled=false \
  -s 'webOrigins=["*"]' \
  -s "redirectUris=${REDIRECT}" \
  -s 'attributes."pkce.code.challenge.method"="S256"' \
  -s 'attributes."token.endpoint.auth.method"="client_secret_post"'

# Get the client ID (UUID)
CID=$(sudo docker exec -it $(sudo docker ps -qf name=keycloak) \
  /opt/keycloak/bin/kcadm.sh get clients -r Test -q clientId=cas --fields id \
  | tr -d '\r' | awk -F\" '/"id"/{print $4}')

# Fetch the client secret
sudo docker exec -it $(sudo docker ps -qf name=keycloak) \
  /opt/keycloak/bin/kcadm.sh get clients/${CID}/client-secret -r Test
