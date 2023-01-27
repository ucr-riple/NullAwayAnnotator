FROM ubuntu:16.04

LABEL Nima Karimipour <karimipour.nima@gmail.com>

# Install basic software support
RUN apt-get update -y
RUN apt-get update && \
    apt-get install --yes software-properties-common

# Install Java 8, 11, 17
RUN apt-get update && \
    apt-get install -y --no-install-recommends software-properties-common curl

RUN add-apt-repository -y ppa:openjdk-r/ppa

RUN apt-get update && \
    apt-get install -y openjdk-8-jdk

RUN apt-get update && \
    apt-get install -y openjdk-11-jdk

RUN apt-get update && \
    apt-get install -y openjdk-17-jdk

RUN apt-get update && \
    apt-get install ca-certificates-java && \
    apt-get clean && \
    update-ca-certificates -f;

ENV JAVA_HOME /usr/lib/jvm/java-11-openjdk-amd64/
RUN export JAVA_HOME

# Install required softwares (curl & zip & wget)
RUN apt install curl -y
RUN apt install zip -y
RUN apt install wget -y
RUN apt install vim -y
# must use apt-get for git.
RUN apt-get install git -y

# Install Maven
ARG MAVEN_VERSION=3.6.3
ARG USER_HOME_DIR="/root"
ARG SHA=c35a1803a6e70a126e80b2b3ae33eed961f83ed74d18fcd16909b2d44d7dada3203f1ffe726c17ef8dcca2dcaa9fca676987befeadc9b9f759967a8cb77181c0
ARG BASE_URL=https://downloads.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries

RUN mkdir -p /usr/share/maven /usr/share/maven/ref \
  && echo "Downlaoding maven" \
  && curl -fsSL -o /tmp/apache-maven.tar.gz ${BASE_URL}/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
  \
  && echo "Checking download hash" \
  && echo "${SHA}  /tmp/apache-maven.tar.gz" | sha512sum -c - \
  \
  && echo "Unziping maven" \
  && tar -xzf /tmp/apache-maven.tar.gz -C /usr/share/maven --strip-components=1 \
  \
  && echo "Cleaning and setting links" \
  && rm -f /tmp/apache-maven.tar.gz \
  && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

ENV MAVEN_HOME /usr/share/maven
ENV MAVEN_CONFIG "$USER_HOME_DIR/.m2"

#Install Gradle
ARG GRADLE_VERSION=6.8.3
ARG GRADLE_BASE_URL=https://services.gradle.org/distributions
ARG GRADLE_SHA=7faa7198769f872826c8ef4f1450f839ec27f0b4d5d1e51bade63667cbccd205
RUN mkdir -p /usr/share/gradle /usr/share/gradle/ref \
  && echo "Downlaoding gradle hash" \
  && curl -fsSL -o /tmp/gradle.zip ${GRADLE_BASE_URL}/gradle-${GRADLE_VERSION}-bin.zip \
  \
  && echo "Checking download hash" \
  && echo "${GRADLE_SHA}  /tmp/gradle.zip" | sha256sum -c - \
  \
  && echo "Unziping gradle" \
  && unzip -d /usr/share/gradle /tmp/gradle.zip \
   \
  && echo "Cleaning and setting links" \
  && rm -f /tmp/gradle.zip \
  && ln -s /usr/share/gradle/gradle-${GRADLE_VERSION} /usr/bin/gradle
ENV GRADLE_VERSION 6.8.3
ENV GRADLE_HOME /usr/bin/gradle
ENV GRADLE_USER_HOME /cache
ENV PATH $PATH:$GRADLE_HOME/bin
VOLUME $GRADLE_USER_HOME

# Install python
RUN apt-get install -y python && \
    apt-get -y install python3-pip

#Install Android SDK
ARG ANDROID_SDK_VERSION=6858069
ENV ANDROID_SDK_ROOT /opt/android-sdk
RUN mkdir -p ${ANDROID_SDK_ROOT}/cmdline-tools && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-${ANDROID_SDK_VERSION}_latest.zip && \
    unzip *tools*linux*.zip -d ${ANDROID_SDK_ROOT}/cmdline-tools && \
    mv ${ANDROID_SDK_ROOT}/cmdline-tools/cmdline-tools ${ANDROID_SDK_ROOT}/cmdline-tools/tools && \
    rm *tools*linux*.zip

# set the environment variables
ENV PATH ${PATH}:${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/cmdline-tools/tools/bin:${ANDROID_SDK_ROOT}/platform-tools:${ANDROID_SDK_ROOT}/emulator
ENV _JAVA_OPTIONS -XX:+UnlockExperimentalVMOptions
ENV LD_LIBRARY_PATH ${ANDROID_SDK_ROOT}/emulator/lib64:${ANDROID_SDK_ROOT}/emulator/lib64/qt/lib
ENV QTWEBENGINE_DISABLE_SANDBOX 1
ENV ANDROID_HOME /opt/android-sdk

# accept the license agreements of the SDK components
ADD license_acceptor.sh /opt/
RUN chmod +x /opt/license_acceptor.sh && /opt/license_acceptor.sh $ANDROID_SDK_ROOT

# Script to run
RUN mkdir -p /var/annotator/
COPY ./start.sh /var/annotator/start.sh
COPY ./git.config /var/annotator/git.config
RUN chmod +x /var/annotator/start.sh

# To force container to be alive and access it's shell.
CMD ["tail", "-f", "/dev/null"]
