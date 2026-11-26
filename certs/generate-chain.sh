#!/usr/bin/env bash
#
# Построение цепочки сертификатов для задания 6.
#
#   Root CA (предоставлен)
#   └── Issuing CA, alias "courseca" (предоставлен, PKCS12)
#       └── Промежуточный CA 1БКС24044 (создаётся здесь, pathlen:0)
#           └── Сертификат сервера 1БКС24044 (создаётся здесь)
#
# Запуск из корня проекта (пароли задаются переменными окружения):
#
#   INTERMEDIATE_PASS=... SERVER_PASS=... \
#   PROVIDED_P12=/путь/course-issuing-ca.p12 ROOT_CRT=/путь/course-root-ca.crt \
#   ./certs/generate-chain.sh
#
# Закрытые ключи и CSR остаются в certs/build/ (в Git не попадают).
# В проект копируются только публичные .crt и хранилище сервера.

set -euo pipefail

# Кириллица в -dname должна передаваться в UTF-8.
export LC_ALL="${LC_ALL:-C.UTF-8}"

STUDENT_ID_CN="1БКС24044"   # номер студенческого билета (как в application.yml)
STUDENT_ID="1bks24044"      # он же латиницей: для alias и имён файлов

PROVIDED_P12="${PROVIDED_P12:-course-issuing-ca.p12}"
PROVIDED_ALIAS="courseca"
PROVIDED_PASS="courseCAPass"   # пароль хранилища и ключа задан в задании
ROOT_CRT="${ROOT_CRT:-course-root-ca.crt}"

: "${INTERMEDIATE_PASS:?Задайте INTERMEDIATE_PASS (пароль хранилища промежуточного CA)}"
: "${SERVER_PASS:?Задайте SERVER_PASS (пароль хранилища сервера)}"

# Предоставленные корень и Issuing CA действуют до 19.01.2027,
# поэтому сроки дочерних сертификатов короче.
INTERMEDIATE_VALIDITY_DAYS=50
SERVER_VALIDITY_DAYS=45

BUILD_DIR="certs/build"
CRT_DIR="certs"
RESOURCES_DIR="src/main/resources"

INTERMEDIATE_ALIAS="${STUDENT_ID}-intermediate-ca"
SERVER_ALIAS="${STUDENT_ID}-server"
INTERMEDIATE_P12="${BUILD_DIR}/${INTERMEDIATE_ALIAS}.p12"
SERVER_P12="${BUILD_DIR}/${SERVER_ALIAS}.p12"

mkdir -p "$BUILD_DIR"
rm -f "$INTERMEDIATE_P12" "$SERVER_P12"

echo "== 1. Публичный сертификат предоставленного Issuing CA"
keytool -exportcert -rfc -alias "$PROVIDED_ALIAS" \
  -storetype PKCS12 -keystore "$PROVIDED_P12" -storepass "$PROVIDED_PASS" \
  -file "${BUILD_DIR}/course-issuing-ca.crt"

echo "== 2. Ключевая пара и CSR промежуточного CA"
keytool -genkeypair -alias "$INTERMEDIATE_ALIAS" -keyalg RSA -keysize 2048 \
  -validity "$INTERMEDIATE_VALIDITY_DAYS" -storetype PKCS12 \
  -keystore "$INTERMEDIATE_P12" -storepass "$INTERMEDIATE_PASS" \
  -dname "CN=${STUDENT_ID_CN} Intermediate CA, OU=Student, O=RBPO, C=RU"

keytool -certreq -alias "$INTERMEDIATE_ALIAS" \
  -keystore "$INTERMEDIATE_P12" -storepass "$INTERMEDIATE_PASS" \
  -file "${BUILD_DIR}/${INTERMEDIATE_ALIAS}.csr"

echo "== 3. Подпись промежуточного CA ключом courseca (CA:true, pathlen:0)"
keytool -gencert -rfc -alias "$PROVIDED_ALIAS" \
  -storetype PKCS12 -keystore "$PROVIDED_P12" -storepass "$PROVIDED_PASS" \
  -infile "${BUILD_DIR}/${INTERMEDIATE_ALIAS}.csr" \
  -outfile "${BUILD_DIR}/${INTERMEDIATE_ALIAS}.crt" \
  -validity "$INTERMEDIATE_VALIDITY_DAYS" \
  -ext "bc:critical=ca:true,pathlen:0" \
  -ext "KeyUsage=digitalSignature,keyCertSign"

echo "== 4. Сборка цепочки в хранилище промежуточного CA"
keytool -importcert -noprompt -alias course-root-ca \
  -keystore "$INTERMEDIATE_P12" -storepass "$INTERMEDIATE_PASS" -file "$ROOT_CRT"
keytool -importcert -noprompt -alias course-issuing-ca \
  -keystore "$INTERMEDIATE_P12" -storepass "$INTERMEDIATE_PASS" \
  -file "${BUILD_DIR}/course-issuing-ca.crt"
keytool -importcert -noprompt -alias "$INTERMEDIATE_ALIAS" \
  -keystore "$INTERMEDIATE_P12" -storepass "$INTERMEDIATE_PASS" \
  -file "${BUILD_DIR}/${INTERMEDIATE_ALIAS}.crt"

echo "== 5. Ключевая пара и CSR сервера"
keytool -genkeypair -alias "$SERVER_ALIAS" -keyalg RSA -keysize 2048 \
  -validity "$SERVER_VALIDITY_DAYS" -storetype PKCS12 \
  -keystore "$SERVER_P12" -storepass "$SERVER_PASS" \
  -dname "CN=${STUDENT_ID_CN} Server, OU=Student, O=RBPO, C=RU"

keytool -certreq -alias "$SERVER_ALIAS" \
  -keystore "$SERVER_P12" -storepass "$SERVER_PASS" \
  -file "${BUILD_DIR}/${SERVER_ALIAS}.csr"

echo "== 6. Подпись сертификата сервера промежуточным CA"
keytool -gencert -rfc -alias "$INTERMEDIATE_ALIAS" \
  -keystore "$INTERMEDIATE_P12" -storepass "$INTERMEDIATE_PASS" \
  -infile "${BUILD_DIR}/${SERVER_ALIAS}.csr" \
  -outfile "${BUILD_DIR}/${SERVER_ALIAS}.crt" \
  -validity "$SERVER_VALIDITY_DAYS" \
  -ext "KeyUsage=digitalSignature,keyEncipherment" \
  -ext "EKU=serverAuth" \
  -ext "bc:critical=ca:false" \
  -ext "san=dns:localhost,ip:127.0.0.1"

echo "== 7. Сборка цепочки в хранилище сервера"
keytool -importcert -noprompt -alias course-root-ca \
  -keystore "$SERVER_P12" -storepass "$SERVER_PASS" -file "$ROOT_CRT"
keytool -importcert -noprompt -alias course-issuing-ca \
  -keystore "$SERVER_P12" -storepass "$SERVER_PASS" \
  -file "${BUILD_DIR}/course-issuing-ca.crt"
keytool -importcert -noprompt -alias "$INTERMEDIATE_ALIAS" \
  -keystore "$SERVER_P12" -storepass "$SERVER_PASS" \
  -file "${BUILD_DIR}/${INTERMEDIATE_ALIAS}.crt"
keytool -importcert -noprompt -alias "$SERVER_ALIAS" \
  -keystore "$SERVER_P12" -storepass "$SERVER_PASS" \
  -file "${BUILD_DIR}/${SERVER_ALIAS}.crt"

echo "== 8. Копирование результата в проект"
cp "$ROOT_CRT" "${CRT_DIR}/course-root-ca.crt"
cp "${BUILD_DIR}/course-issuing-ca.crt" "${CRT_DIR}/course-issuing-ca.crt"
cp "${BUILD_DIR}/${INTERMEDIATE_ALIAS}.crt" "${CRT_DIR}/${INTERMEDIATE_ALIAS}.crt"
cp "${BUILD_DIR}/${SERVER_ALIAS}.crt" "${CRT_DIR}/${SERVER_ALIAS}.crt"
cp "$SERVER_P12" "${RESOURCES_DIR}/${SERVER_ALIAS}.p12"

echo "== Готово. Цепочка записи ${SERVER_ALIAS}:"
keytool -list -v -alias "$SERVER_ALIAS" \
  -keystore "${RESOURCES_DIR}/${SERVER_ALIAS}.p12" -storepass "$SERVER_PASS" \
  | grep -E "Alias name|Entry type|Certificate chain length|Owner:|Issuer:|Valid from"
