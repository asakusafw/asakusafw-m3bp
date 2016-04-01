# Asakusa Framework on M<sup>3</sup>

Asakusa on M<sup>3</sup> provides facilities that make [Asakusa](https://github.com/asakusafw/asakusafw) batch applications run on [M<sup>3</sup> for Batch Processing](https://github.com/fixstars/m3bp) (M<sup>3</sup> for BP).

This project includes the followings:

* [Asakusa DSL Compiler](https://github.com/asakusafw/asakusafw-compiler) for M<sup>3</sup> for BP
* Asakusa runtime libraries for M<sup>3</sup> for BP
* Asakusa on M<sup>3</sup> [Gradle](http://gradle.org/) plug-in

## How to build

* requirements
  * building Java libraries
    * JDK (>= 1.8)
    * Maven (>= 3.3)
  * building native libraries
    * CMake (>= 2.8)
    * Make
    * GCC-C++ for Linux x86_64 (>= 4.5)
    * Boost C++ Libraries (= 1.60.0)
    * hwloc (>= 1.8)

### Maven artifacts

```
mvn clean install -Pnative [-DskipTests]
```

* other available options
  * `-Pnative-test`
  * `-DCMAKE_BUILD_TYPE=Debug`
  * `-DCMAKE_TOOLCHAIN_FILE=/path/to/toolchain.cmake`

### Gradle plug-ins

```sh
cd gradle
./gradlew clean [build] install
```

## How to use

* requirements
  * JavaSE >= 1.8
  * CMake >= 2.8
  * Make
  * GCC-C++ = framework required version
  * hwloc = framework required version

### Gradle build script example

```groovy
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

asakusafw {
    basePackage = '<base-package-name>'
    javac {
        sourceCompatibility '1.8'
        targetCompatibility '1.8'
    }
    ...
}

...
```

## Referred Projects
* [M<sup>3</sup> for Batch Processing](https://github.com/fixstars/m3bp)
* [Asakusa Framework](https://github.com/asakusafw/asakusafw)
* [Asakusa DSL Compiler](https://github.com/asakusafw/asakusafw-compiler)
* [Asakusa DAG Toolset](https://github.com/asakusafw/asakusafw-dag)

## License
* [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
