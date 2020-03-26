#!/usr/bin/env bash

openssl aes-256-cbc -K $encrypted_4ab987b020bf_key -iv $encrypted_4ab987b020bf_iv -in .ci/sonatype.gpg.enc -out sonatype.gpg -d
if [[ ! -s sonatype.gpg ]]
   then echo "Decryption failed."
   exit 1
fi

./gradlew uploadPublished --info -PsonatypeUsername=$SONATYPE_USERNAME -PsonatypePassword=$SONATYPE_PASSWORD -Psigning.keyId=$GPG_KEYNAME -Psigning.password=$GPG_PASSPHRASE -Psigning.secretKeyRingFile=$TRAVIS_BUILD_DIR/sonatype.gpg
