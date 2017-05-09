# silent-support
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/me.tatarka.silentsupport/silent-support/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/me.tatarka.silentsupport/silent-support)
Backport new android api calls to support lib versions.

The support lib provideds many useful wrappers and backports of android apis. However, using them requires 
you to change your code. And as you bump your min sdk, you need to change them back to the platform versions.
This plugin will automatically rewrite calls to their support lib equivlents when necssary. The `NewApi` lint
is replaced with a custom lint which will ignore successfully backported calls.

## Usage

Currently requires android gradle plugin `2.4.0-alpha7`

```
buildscript {
   repositories {
      mavenCentral()
   }

   dependencies {
      classpath "me.tatarka.silentsupport:silent-support-gradle-plugin:0.1"
   }
}

apply plugin: 'me.tatarka.silent-support'
```

## Supported Methods/Classes

Right now only calls that can be converted from `foo.bar(baz)` to `FooCompat.bar(foo, baz)` are supported.
