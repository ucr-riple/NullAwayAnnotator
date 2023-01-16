#!/bin/bash

# install dependencies locally
source /var/annotator/git.config

git config --global user.email "${EMAIL}"
git config --global user.name "${USERNAME}"
git config --global push.default simple
git config --global pull.rebase false

# Fix ca certificate issue
/usr/bin/printf '\xfe\xed\xfe\xed\x00\x00\x00\x02\x00\x00\x00\x00\xe2\x68\x6e\x45\xfb\x43\xdf\xa4\xd9\x92\xdd\x41\xce\xb6\xb2\x1c\x63\x30\xd7\x92' > /etc/ssl/certs/java/cacerts
/var/lib/dpkg/info/ca-certificates-java.postinst configure

pushd /tmp/ || exit
  git clone https://${USERNAME}:${KEY}@github.com/ucr-riple/NullAwayAnnotator.git
  pushd NullAwayAnnotator || exit
    git checkout nimak/cache-temp
  popd || exit
  git clone https://${USERNAME}:${KEY}@github.com/nimakarimipour/NullAwayAnnotatorArtifactEvaluation.git
  pushd NullAwayAnnotatorArtifactEvaluation || exit
    git checkout main
  popd || exit
popd || exit
