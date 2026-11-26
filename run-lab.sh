#!/usr/bin/env bash
set -euo pipefail
SERVICE="src/main/java/mn/edu/must/sqat/CourseRegistrationService.java"
ORIGINAL="$(mktemp)"
cp "$SERVICE" "$ORIGINAL"
restore() { cp "$ORIGINAL" "$SERVICE"; rm -f "$ORIGINAL"; }
trap restore EXIT
mkdir -p results

# Эцсийн ногоон тестийн гаралт
cp "$ORIGINAL" "$SERVICE"
mvn test 2>&1 | tee results/mvn-test.txt

# Мутант 1: идэвхгүй оюутны шалгалтыг хасах
cp "$ORIGINAL" "$SERVICE"
sed -i 's/if (!student.active()) return "ERROR_INACTIVE_STUDENT";/\/\/ MUTANT 1: active check removed/' "$SERVICE"
(mvn test 2>&1 || true) | tee results/mvn-test-mutant-1.txt

# Мутант 2: хадгалалтын дуудалтыг хасах
cp "$ORIGINAL" "$SERVICE"
sed -i 's/repository.saveRegistration(studentId, courseId);/\/\/ MUTANT 2: saveRegistration removed/' "$SERVICE"
(mvn test 2>&1 || true) | tee results/mvn-test-mutant-2.txt

# Мутант 3: зөвхөн courseId == null хэсгийг хасах
cp "$ORIGINAL" "$SERVICE"
sed -i 's/studentId == null || courseId == null/studentId == null/' "$SERVICE"
(mvn test 2>&1 || true) | tee results/mvn-test-mutant-3.txt

# Эх кодыг сэргээж ногоон тестийг дахин батлах
cp "$ORIGINAL" "$SERVICE"
mvn test 2>&1 | tee results/mvn-test.txt
restore
trap - EXIT
