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

The Gradle plugin which constitute the main deliverable of this assignement is located under `buildSrc`.

```
├── buildSrc                  #
│   ├── build.gradle          # deliverable of the assignement
│   └── src                   #
└── example-project-intellij
│   ├── build.gradle
│   └── intellij-community    # submodule of IntelliJ IDEA community git repo to showcase the plugin's performance
└── example-project-minimal
    ├── build.gradle
    └── src                   # very simple project to tryout the plugin
```

In a real world example, we would have made the plugin part of its dedicated project instead, and add tooling to release
it as a regular Gradle plugin.
For the sake of simplicity and easy testing, this repository is a playground where example
project can be created as subprojects in Gradle.

## Assignement notes

### Requirements

- Uses the incremental ask of Gradle
  - [x] only process source files that have been changed in between two runs of the plugin
  - [x] Handle add/update/delete of source files, addind/updating/cleaning up the relevant icons
  - [x] works well on a scale of intellij-community repository
- Patterns are configurable
  - [x] File pattern, such as `Icons.java` suffix, is configurable (selects which files are being parsed)
  - [x] Icon field pattern, such as "is of type `String`",  is configurable (selects which fields are being parsed as icons)
- Failure recovery
  - [x] Files could be malformed, the plugin "must handle that properly"
  - [x] Fields could have invalid base64 string content, the plugin "must handle that properly"
- Other features
  - [x] Must support nested classes

### Assumptions made

- [validated with interviewer] Icon fields will always be `public` and `final`
- Default output directory root will be the Gradle build directory, subdirectory `icons`
- Default file pattern will be the suffix `Icons.java`
- [validated with interviewer] Default icon field pattern will be "any field of type `String`"
- Files that does not respect the file pattern will be ignored, never parsed

### Decisions

- Gradle incremental framework does not expose state of file changes, we have to maintain this state in one or more 
output files in order to fulfill the cleanup of deleted icons
- Each Java source file `Icon.java` is processed in parallel using Gradle WorkerAPI to improve performance (build time reduced by 33% on IntelliJ Community repo)

### Known limitations

- Empty output directory of delete icons are not cleaned up


