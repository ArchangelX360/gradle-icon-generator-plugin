# jetbrains-assignment

A Gradle plugin to generate PNG files from Base64 encoded strings declared in Java sources.

## Initialise repository

```
git clone https://github.com/ArchangelX360/jetbrains-assignment.git
cd jetbrains-assignment

# to pull IntelliJ community repository for witnessing incremental builds better
git submodule update --init --recursive
```

## Repository structure

The Gradle plugin which constitute the main deliverable of this assignement is located under `icon-generator-plugin`.

```
├── icon-generator-plugin         #
│   ├── build.gradle              # deliverable of the assignement
│   └── src                       #
└── examples
    ├── example-project-intellij
    │   ├── build.gradle
    │   └── intellij-community    # submodule of IntelliJ IDEA community git repo to showcase the plugin's performance
    └── example-project-minimal
        ├── build.gradle
        └── src                   # very simple project to tryout the plugin

```

The `examples` directory is a playground to try out the plugin, editing, adding, removing files and running the plugin 
tasks.
For the sake of simplicity for reviewing the assignment, `icon-generator-plugin` is included with `includeBuild` to 
allow usage as a `buildscript` dependency for the example projects. 

## How to use the plugin

Set your plugin block like such:
```kotlin
plugins {
    id("icon-generator-plugin")
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
- `patternFilterable` the File pattern that selects which java files will be parsed, defaults to `include("**/*Icons.java")`
- `iconFieldType` the icon field pattern that selects which fields are being parsed as icons by filtering on the field's
  type, defaults to `String`
- `outputDirectory` the root directory of the generated icons

Example of configuration:
```kotlin
generateIconsForSources {
    sources.setFrom(
        project.layout.projectDirectory.dir("intellij-community"),
    )
    iconFieldType.set("Icon")
    patternFilterable.set(
        PatternSet()
            .include("**/*.java")
    )
}
```

### For example projects

Example projects can use the plugin by declaring, in addition to the configuration of the previous sections, a 
`buildscript` delegate:
```kotlin
buildscript {
    dependencies {
        classpath("se.dorne:icon-generator-plugin")
    }
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
  - [x] Icon field pattern, such as "is of type `String`",  is configurable (selects which fields are being parsed as
    icons)
- Failure recovery
  - [x] Files could be malformed, the plugin "must handle that properly"
  - [x] Fields could have invalid base64 string content, the plugin "must handle that properly"
- Other features
  - [x] Must support nested classes

### Assumptions made

- *[validated with interviewer]* Icon fields will always be `public` and `final`
- Default output directory root will be the Gradle build directory, subdirectory `icons`
- Default file pattern will be the suffix `Icons.java`
- *[validated with interviewer]* Default icon field pattern will be "any field of type `String`"
- Files that does not respect the file pattern will be ignored, never parsed

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
  task. Large amount of inputs leads to long "fingerprinting"during the very first run of the task which impact
  performance. For example, this change reduced the "fingerprinting" time of the task from 20s to 4s.

### Known limitations

- Empty output directory of delete icons are not cleaned up

### Decisions

- Gradle incremental framework does not expose state of file changes, we have to maintain this state in one or more
  output files in order to fulfill the cleanup of deleted icons and the addition/update of added/changed icons (see
  Section below "Managing a state")
- the plugin's extension exposes a `PatternFilterable` property to give the most filtering flexibility to users

#### Managing a state

When a file is updated or removed, some of the previous outputs (some generated icons) have to be cleaned up from the
output directory.
Our generation requirements is limited in the case where a Java file declares multiple classes (one public, and one or
more non-public).
For example, take into consideration a project with two files:

```java
// in file src/main/java/foo/AIcons.java
package foo;

public class AIcons {
    public final static String AIcon = "some base 64 content AIcon";
}

class BIcons {
    public final static String BIcon = "some base 64 content BIcon";
}
```
```java
// in file src/main/java/foo/CIcons.java
package foo;

public class CIcons {
  public final static String CIcon = "some base 64 content CIcon";
}
```

The plugin will generate the following icons:
```
foo/CIcons/OtherIcon.png
foo/BIcons/BIcon.png
foo/AIcons/AIcon.png
```

Gradle incremental framework does not expose the previous state when a file changes (UPDATED/REMOVED), only the
filename of the file.

From this filename only, let's say `src/main/java/foo/AIcons.java` is modified or deleted, we can predict that the
output directory `foo/AIcons` needs to be cleaned up and re-processed.
However, we have no way of knowing that `foo/BIcons/BIcon.png` also needs to be cleaned up and re-processed.

For that reason, we have to maintain this state in one or more output files in order to fulfill the cleanup of deleted
icons and the addition/update of added/changed icons.
