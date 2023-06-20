# Compile and package on Debian image
FROM debian:bookworm as build-stage

# Install the needed packages
RUN apt-get update && apt-get install -y \ 
    curl \ 
    wget \ 
    fakeroot \
    openjdk-17-jdk \
    maven \
    r-base \
    r-cran-tidyverse \
    r-cran-bookdown

# Set working directory
WORKDIR /usr/src/app

COPY src ./src
COPY res ./res
COPY test ./test
COPY templates ./templates
COPY scripts ./scripts
COPY wiki ./wiki
COPY pom.xml ./pom.xml
COPY COPYING ./COPYING

RUN bash ./scripts/installDependencies.sh

RUN mvn -DskipTests clean install

# Scratch is an empty stage
FROM scratch as export-stage
COPY --from=build-stage /usr/src/app/packages /
