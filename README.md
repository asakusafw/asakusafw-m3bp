# Asakusa on M<sup>3</sup>BP

Asakusa on M<sup>3</sup>BP provides facilities that make [Asakusa](https://github.com/asakusafw/asakusafw) batch applications run on [M<sup>3</sup> for Batch Processing](https://github.com/fixstars/m3bp).

This project includes the followings:

* Asakusa on M<sup>3</sup>BP Compiler
* Asakusa on M<sup>3</sup>BP Runtime
* Asakusa on M<sup>3</sup>BP [Gradle](http://gradle.org/) plug-in

## How to build

* requirements
  * building Java libraries
    * Java SE Development Kit (>= 1.8)
  * building native libraries
    * CMake (>= 2.8)
    * Make
    * GCC-C++ for Linux x86_64 (>= 4.5)
    * Boost C++ Libraries (= 1.60.0)
    * hwloc (>= 1.8)

### Maven artifacts

```
git submodule update --init
./mvnw clean install -Pnative [-DskipTests]
```

* other available options
  * `-Pnative-test`
  * `-DCMAKE_BUILD_TYPE=Debug`
  * `-DCMAKE_TOOLCHAIN_FILE=/path/to/toolchain.cmake`
  * `-Dmake.parallel=N-of-threads`

### Gradle plug-ins

```sh
cd gradle
./gradlew clean [build] install
```

## How to use

* requirements
  * Java SE Development Kit >= 1.8
  * CMake >= 2.8
  * Make
  * GCC-C++ = framework required version
  * hwloc = framework required version

### Gradle build script example

```groovy
group '<your-group>'

buildscript {
    repositories {
        ...
    }
    dependencies {
        classpath 'com.asakusafw.m3bp:asakusa-m3bp-gradle:<project-version>'
    }
}

apply plugin: 'asakusafw-sdk'
apply plugin: 'asakusafw-organizer'
apply plugin: 'asakusafw-m3bp'
...

```

## Referred Projects
* [M<sup>3</sup> for Batch Processing](https://github.com/fixstars/m3bp)
* [Asakusa Framework Core](https://github.com/asakusafw/asakusafw)
* [Asakusa Framework Language Toolset](https://github.com/asakusafw/asakusafw-compiler)
* [Asakusa Framework Documentation](https://github.com/asakusafw/asakusafw-documentation)

## Resources
* [Asakusa on M<sup>3</sup>BP Documentation (ja)](http://docs.asakusafw.com/asakusa-on-m3bp/)

## License
* [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
