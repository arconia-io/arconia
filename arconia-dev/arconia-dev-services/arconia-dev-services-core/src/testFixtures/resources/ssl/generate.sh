#!/usr/bin/env bash
#
# Generates the certificates and keystores the SSL tests of the Dev Services use, in this
# directory. Run it to regenerate them all; the tests don't depend on the exact content,
# only on the subjects, aliases and passwords below.
#
# Requires openssl (3.x) and keytool (JDK).

set -euo pipefail
cd "$(dirname "$0")"

# The certificates are valid for a hundred years, so that the tests never fail on expiry.
DAYS=36500
# The password of every keystore and truststore.
STORE_PASSWORD=secret

rm -f ca.crt ca.key server.key server.csr server.crt server-chain.crt server.p12 truststore.p12 \
  server-keypass.jks server-two-keys.p12 second.key second.crt second.p12

# 1. A self-signed certificate authority. Its key is only used here, to sign the server
#    certificate, and deleted at the end: the tests need the CA certificate only.
openssl req -x509 -newkey rsa:2048 -nodes -days "$DAYS" \
  -keyout ca.key -out ca.crt \
  -subj "/O=Arconia/CN=Arconia Dev Services Test CA" \
  -addext "basicConstraints=critical,CA:TRUE" \
  -addext "keyUsage=critical,keyCertSign,cRLSign"

# 2. The key and certificate a dev service presents, issued by the CA. The key is written
#    unencrypted in PKCS#8 form (BEGIN PRIVATE KEY). The subject alternative names cover the
#    hosts the application connects to a container through, since every client verifies them.
openssl req -newkey rsa:2048 -nodes \
  -keyout server.key -out server.csr \
  -subj "/O=Arconia/CN=localhost"
openssl x509 -req -days "$DAYS" \
  -in server.csr -CA ca.crt -CAkey ca.key -CAcreateserial \
  -out server.crt \
  -extfile <(printf '%s\n' \
    "subjectAltName=DNS:localhost,IP:127.0.0.1,IP:::1" \
    "basicConstraints=CA:FALSE" \
    "keyUsage=digitalSignature,keyEncipherment" \
    "extendedKeyUsage=serverAuth,clientAuth")

# 3. The certificate chain: the server certificate followed by the issuing CA certificate.
cat server.crt ca.crt > server-chain.crt

# 4. The same material as Java keystores: a PKCS12 keystore with the server key under the
#    "server" alias and the CA certificate in its chain, and a PKCS12 truststore holding the
#    CA certificate.
openssl pkcs12 -export \
  -inkey server.key -in server.crt -certfile ca.crt -name server \
  -out server.p12 -passout "pass:$STORE_PASSWORD"
keytool -importcert -noprompt \
  -alias ca -file ca.crt \
  -keystore truststore.p12 -storetype PKCS12 -storepass "$STORE_PASSWORD"

# 5. A JKS keystore whose key is protected by a password ("otherpass") different from the one
#    of the store, for the tests of the key password resolution.
keytool -importkeystore -noprompt \
  -srckeystore server.p12 -srcstoretype PKCS12 -srcstorepass "$STORE_PASSWORD" \
  -srcalias server -srckeypass "$STORE_PASSWORD" \
  -destkeystore server-keypass.jks -deststoretype JKS -deststorepass "$STORE_PASSWORD" \
  -destalias server -destkeypass otherpass

# 6. A PKCS12 keystore with two key entries, "server" and "second", for the tests requiring
#    the key alias to be set when the key store holds several keys.
openssl req -x509 -newkey rsa:2048 -nodes -days "$DAYS" \
  -keyout second.key -out second.crt \
  -subj "/O=Arconia/CN=second"
openssl pkcs12 -export \
  -inkey server.key -in server.crt -name server \
  -out server-two-keys.p12 -passout "pass:$STORE_PASSWORD"
openssl pkcs12 -export \
  -inkey second.key -in second.crt -name second \
  -out second.p12 -passout "pass:$STORE_PASSWORD"
keytool -importkeystore -noprompt \
  -srckeystore second.p12 -srcstoretype PKCS12 -srcstorepass "$STORE_PASSWORD" \
  -destkeystore server-two-keys.p12 -deststoretype PKCS12 -deststorepass "$STORE_PASSWORD"

# Only the files the tests use are kept.
rm -f ca.key ca.srl server.csr second.key second.crt second.p12
chmod 644 ca.crt server.key server.crt server-chain.crt server.p12 truststore.p12 server-keypass.jks server-two-keys.p12

echo "Generated:"
ls -1 ca.crt server.key server.crt server-chain.crt server.p12 truststore.p12 server-keypass.jks server-two-keys.p12
