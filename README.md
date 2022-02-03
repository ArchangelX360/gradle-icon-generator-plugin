# gradle-icon-generator-plugin

A Gradle plugin to generate PNG files from Base64 encoded strings declared in Java sources.

## How to review?

### [RECOMMENDED] Review using the examples

The repository has two example projects, declared as Gradle subprojects for convenience:

- `small-project` a manually generated project with some classic source files, go to its
  [README.md](https://github.com/ArchangelX360/gradle-icon-generator-plugin/blob/main/examples/small-project/README.md) 
  for instruction on how to run the example
- `large-generated-project` an auto-generated large project, go to its 
  [README.md](https://github.com/ArchangelX360/gradle-icon-generator-plugin/blob/main/examples/large-generated-project/README.md)
  for instruction on how to run the example

We encourage reviewers to use the examples to try out the plugin, we invite them also to read the test suite
of the plugin in `src/test/kotlin`, especially the [integration tests](https://github.com/ArchangelX360/gradle-icon-generator-plugin/blob/main/src/test/kotlin/io/github/archangelx360/IconGeneratorPluginTest.kt).

### Review using the plugin on a different project

In your `build.gradle.kts`, add the plugin like such:

```kotlin
plugins {
    id("io.github.archangelx360.icon-generator") version "1.0.1"
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

#### Configuration

Some additional configuration are available such as:

- `patternFilterable` the File pattern that selects which java files will be parsed, defaults
  to `include("**/*Icons.java")`
- `iconFieldType` the icon field pattern that selects which fields are being parsed as icons by filtering on the field's
  type, defaults to `String`
- `outputDirectory` the root directory of the generated icons, defaults to `<gradleProjectBuildDirectory>/icons`

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
    - [x] Output filepath pattern is `<fully_qualified_name_of_enclosing_class_of_the_field/slashed/instead/of/dotted>/<icon_field_name>.png`
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

Adjusted requirements (validated with interviewer):
- ~~Plugin only supports top-level public class and their nested classes, it will warn and
    ignore other top-level classes~~ Added support as we were already using state (See "Decisions" section)`
- Default icon field pattern will be "any field of type `String`"
- Plugin supports only icon fields that are `public` and `final` (the `public` part of this requirement could be lifted,
  if asked as it should not matter)

### Assumptions made

- Default output directory root will be the Gradle build directory, subdirectory `icons`
- *[validated with interviewer]* Files that does not respect the file pattern will be ignored, never parsed

### Optimisations

The plugin uses several optimisations

- `generateIcons` is an incremental task, it will only process changed (added/modified/deleted) files since the last run
  of the task
- `generateIcons` is cacheable, if the task ran for a sets of inputs, a subsequent run of the task for the same set of
  inputs will be instant recovering the outputs from the Gradle cache
- `generateIcons` is executed in parallel, leveraging the [Worker API](https://docs.gradle.org/current/userguide/custom_tasks.html#worker_api)
  where each source file is processed in parallel to improve performance (for example, build time reduced by 33% on
  IntelliJ Community repo)
- `generateIcons` [Worker API](https://docs.gradle.org/current/userguide/custom_tasks.html#worker_api) is leveraged 
  without the `WorkQueue.await()` to allow independent tasks to run in parallel while processing icon files
- the plugin's extension exposes a `PatternFilterable` property that is used at task configuration to reduce the
  potential high number of inputs for the `generateIcons` task by filtering file *before* setting them as inputs of the
  task. Large amount of inputs leads to long "fingerprinting" during the very first run of the task which impact
  performance. For example, this change reduced the "fingerprinting" time of the task from 20s to 4s.

### Known limitations

- Empty output directory of deleted icons are not cleaned up
- Incrementality works at file level, not at output icon level. A change in a file will clean up previously generated
  icons and regenerate the ones that are still in the code, whether they were part of the modification or not.
- The syntactically correct source code given in input must _compile_ in order to avoid any possible conflicts in the
  icons output (as generation is based on the `fully_qualified_name_of_enclosing_class_of_the_field`)

### Decisions

#### Introducing a state

When a source file is updated or removed, some or all of the icons generated by the previous runs have to be cleaned up
from the output directory.

A package name cannot be deduced from the filepath of a Java source, it is a good practise to make it follow the
directory structure under `src/main/java`, but it could still be a valid Java source otherwise.

The required name generation is relative to the package, in 
`<fully_qualified_name_of_enclosing_class_of_the_field/slashed/instead/of/dotted>/<icon_field_name>.png`
we can see that `fully_qualified_name_of_enclosing_class_of_the_field` is directly linked to the package name.
For that reason, we cannot possibly infer the output directory of formerly generated icons, based solely on the source 
filepath we get as input.

As our task is incremental we also do not know the current state of the repository without re-reading the whole source
file tree, which could be very inefficient, so we are discouraged get this information.

For these reasons, we have to maintain a state that links each input file to its list of outputted icons.
This way, we can fulfill the cleanup of deleted icons and the addition/update of added/changed fields easily and
selectively.

##### Tradeoffs

###### Having a state vs. no state

Maintaining a state between task runs is necessary for the reasons stated above, mainly linked to our requirements.
Some requirements of the plugin could be loosened up so that a convention could be enforced in the output path of the 
icons, but that would quite be limiting for hypothetical future of the plugin (see "Discarded options for having no
state" section).
Adding support for more Java concepts would be very tricky (like top-level non-public classes was).

###### Granular state vs. single file state

We chose to implement the state with high granularity, meaning that there is one state file per input file given to the
`generateIcons` task, this includes only files matching the file pattern.

As a drawback, it can increase significantly the number of outputs of the task, meaning that the "Build cache" section
of the task execution will be higher (Gradle will have to fingerprint/hash more output files).

As a benefit, the state is compatible with the incremental behaviour, indeed, only part of the full state is read, the
state of changed files only. This reduces the "Task action execution" time and the memory footprint of the plugin task
execution.

##### Discarded options for having no state

We could have though of having a generation that would be closely tight to the source filepath, e.g.
`<relative_directory_structure_to_the_source_file>/<file_name_without_extension>/<name_of_each_nested_class_if_any_separated_by_dots_with_following_dot><field_name>.png`.

But it comes from it load of other challenges, mainly name conflicts. As it does not rely on a concept validated by
the compilation of the sources (the fully qualified name), we cannot ensure this path generation will not have conflicts.

E.g. if we have two files:
- File A
  - package: `foo`
  - filepath relative to project directory: `src/main/java/foo/bar/A.java` (note that the repository is choosing not to respect package directory convention)
  - fieldName: `SomeIcon`
  - fully qualified name of the field's class: `foo.A.Nested1.Nested2` (note the field is under a few levels of nested classes here)
  - outputted icon: `src/main/java/foo/bar/A/Nested1.Nested2.SomeIcon.png`
- File B
  - package: `foo`
  - filepath relative to project directory: `src/main/java/foo/bar/A/B.java`
  - fieldName: `SomeIcon`
  - fully qualified name of the field's class: `foo.B`
  - outputted icon: `src/main/java/foo/bar/A/B/SomeIcon.png`

At deletion of `src/main/java/foo/bar/A.java`, we would like to cleanup `src/main/java/foo/bar/A` directory but that
would conflict with generated icon of B.

As said above, restricting more strictly the inputs could make this approach work, but it is less future-proof.
