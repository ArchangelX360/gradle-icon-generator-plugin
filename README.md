# gradle-icon-generator-plugin

A Gradle plugin to generate PNG files from Base64 encoded strings declared in Java sources.

## How to review?

## Review using the examples

The repository has two example projects, declared as Gradle subprojects for convenience:

- `small-project` a manually generated project with some classic source files, go to its
  [README.md](https://github.com/ArchangelX360/gradle-icon-generator-plugin/blob/main/examples/small-project/README.md) 
  for instruction on how to run the example
- `large-generated-project` an auto-generated large project, go to its 
  [README.md](https://github.com/ArchangelX360/gradle-icon-generator-plugin/blob/main/examples/large-generated-project/README.md)
  for instruction on how to run the example

We encourage reviewers to use the examples to try out the plugin, we invite them also to read the test suite
of the plugin in `src/test/kotlin`, especially the [integration tests](https://github.com/ArchangelX360/gradle-icon-generator-plugin/blob/main/src/test/kotlin/se/dorne/IconGeneratorPluginTest.kt).

## Review using the plugin on a different project

First, clone the repository and publish the plugin locally, if not already done:
```
git clone https://github.com/ArchangelX360/gradle-icon-generator-plugin.git
cd gradle-icon-generator-plugin

# The plugin is not yet published to Gradle Plugin Portal, so you have to publish it locally
./gradlew publishToMavenLocal
```

Set up *at the very top* of your `settings.gradle.kts` the `mavenLocal()` plugin repository:
```kotlin
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}
```

Then, in your `build.gradle.kts`, add the plugin like such:

```kotlin
plugins {
    id("se.dorne.icon-generator") version "0.0.1"
}
```

Configure which directories the plugin should process by adding a `generateIconsForSources` block

```kotlin
generateIconsForSources {
    sources.setFrom(
        project.layout.projectDirectory.dir("the-directory-containing-the-sources-you-want-to-process"),
        project.layout.projectDirectory.dir("another-one"),
    )
}
```

The plugin will generate two tasks:

- `generateIcons` to generate icons of sources configured through `generateIconsForSources` extension
- `cleanIcons` to cleanup the outputs of `generateIcons` task

### Configuration

Some additional configuration are available such as:

- `patternFilterable` the File pattern that selects which java files will be parsed, defaults
  to `include("**/*Icons.java")`
- `iconFieldType` the icon field pattern that selects which fields are being parsed as icons by filtering on the field's
  type, defaults to `String`
- `outputDirectory` the root directory of the generated icons

Example of configuration:

```kotlin
generateIconsForSources {
    sources.setFrom(
        project.layout.projectDirectory.dir("my-source-directory"),
    )
    iconFieldType.set("Icon")
    patternFilterable.set(
        PatternSet()
            .include("**/*.java")
    )
}
```

## Assignement notes

### Requirements

- Generate PNG image files based on icon fields in Java sources
    - [x] Output filepath pattern is `<packageName>/<className>/<iconFieldName>.png`
- Uses the incremental ask of Gradle
    - [x] only process source files that have been changed in between two runs of the plugin
    - [x] Handle add/update/delete of source files, addind/updating/cleaning up the relevant icons
    - [x] works well on a scale of intellij-community repository
- Patterns are configurable
    - [x] File pattern, such as `Icons.java` suffix, is configurable (selects which files are being parsed)
    - [x] Icon field pattern, such as "is of type `String`", is configurable (selects which fields are being parsed as
      icons)
- Failure recovery
    - [x] Files could be malformed, the plugin "must handle that properly"
    - [x] Fields could have invalid base64 string content, the plugin "must handle that properly"
- Other features
    - [x] Must support nested classes

### Assumptions made

- *[validated with interviewer]* Icon fields will always be `public` and `final`
- Default output directory root will be the Gradle build directory, subdirectory `icons`, and under that respecting the
  directory structure of the project directory
- *[validated with interviewer]* Default file pattern will be the suffix `Icons.java`
- *[validated with interviewer]* Default icon field pattern will be "any field of type `String`"
- *[validated with interviewer]* Files that does not respect the file pattern will be ignored, never parsed
- *[validated with interviewer]* Plugin only supports top-level public class and their nested classes, it will warn and
  ignore other top-level classes

### Optimisations

The plugin uses several optimisations

- `generateIcons` is an incremental task, it will only process changed (added/modified/deleted) files since the last run
  of the task
- `generateIcons` is cacheable, if the task ran for a sets of inputs, a subsequent run of the task for the same set of
  inputs will be instant recovering the outputs from the Gradle cache
- `generateIcons` is executing in parallel, leveraging the WorkerAPI where each source file is processed in parallel to
  improve performance (for example, build time reduced by 33% on IntelliJ Community repo)
- the plugin's extension exposes a `PatternFilterable` property that is used at task configuration to reduce the
  potential high number of inputs for the `generateIcons` task by filtering file *before* setting them as inputs of the
  task. Large amount of inputs leads to long "fingerprinting" during the very first run of the task which impact
  performance. For example, this change reduced the "fingerprinting" time of the task from 20s to 4s.

### Known limitations

- Incrementality works at file level, not at output icon level. A change in a file will clean up previously generated
  icons and regenerate the ones that are still in the code, whether they were part of the modification or not.
- Plugin only supports top-level public class and their nested classes, we previously implemented a state manager to 
  support it, but it was judged as too complicated for our use-case, you can still see the state manager in the branch
  `ArchangelX360/state_manager`
