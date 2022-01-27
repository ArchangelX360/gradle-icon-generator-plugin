# `large-generated-project`

`large-generated-project` is made for checking performance (scale, incrementality, parallelism) of the plugin, answering
questions such as:

- how long does it take for the plugin to process a large number of matched sources?
- is the plugin heavily impacted by the presence of a lot of non-matched files?
- how long does it take for the plugin to process X files, then X additional more, and again X additional files, and
  what about compared to processing 3X files in one go?

## What is provided in this project?

`large-generated-project` is not generated when the repo is cloned, you have a few ways recommended ways of generating.
`large-generated-project` is delivered with 3 additional gradle tasks:

- `:examples:large-generated-project:generateIntelliJIDEACommunityLikeRepository` to generate a repository that has the
  same kind of structure as `intellij-community` repository
    - low number of matched source files (54)
    - high number of non-matched source files (231023)
- `:examples:large-generated-project:generateLargeRepository` to generate a large repository
    - high number of matched source files (2000, with between 1 and 5 fields per class)
    - low number of non-matched source files (100)
- `:examples:large-generated-project:cleanGeneratedSources` to clean the all the generated sources, to restart some testing

## Run the example

First, clone the repository and publish the plugin locally, if not already done:
```
git clone https://github.com/ArchangelX360/gradle-icon-generator-plugin.git
cd gradle-icon-generator-plugin
```

### Run 1: the `intellij-community`-like repository

Running the example reproducing an `intellij-community`-like repository:

```
# (optional) cleanup any previously generated sources and outputs
./gradlew :examples:large-generated-project:cleanIcons
./gradlew :examples:large-generated-project:cleanGeneratedSources

# generate the repository, IntelliJ IDEA community has 54 source files containing icons, and 231023 other files
./gradlew :examples:large-generated-project:generateSources -PiconSourcesCount=54 -PirrelevantSourcesCount=231023

# Run the plugin's icon generation
./gradlew :examples:large-generated-project:generateIcons
```

What is interesting to see in this example, is the fingerprinting time of the task.
Indeed, this example is generated with a lot of files unmatched by the plugin file pattern.
If the task was configured differently and the matching done inside the task instead of filtering the inputs themselves,
before they are pass to the `generateIcons` task, the fingerprinting could take an observed 3 times additional time to
execute in the IntelliJ example!

### Run 2: a large repository

A more interesting stress test is to increase the number of `iconSourcesCount`, to see how the plugin efficiently
parallelize the processing of a large number of matched sources.

```
# (optional) cleanup any previously generated sources and outputs
./gradlew :examples:large-generated-project:cleanIcons
./gradlew :examples:large-generated-project:cleanGeneratedSources

# generate the repository
./gradlew :examples:large-generated-project:generateSources -PiconSourcesCount=2000 -PirrelevantSourcesCount=200

# Run the plugin's icon generation
./gradlew :examples:large-generated-project:generateIcons   
```

#### Incremental comparison test

The `generateLargeRepository` (and `generateIntelliJIDEACommunityLikeRepository` for that matter) does not clean their
generated sources between two runs, an explicit `cleanGeneratedSources` is needed.
This is made on purpose for some additional tests to be run, for example, the incremental comparison test.

We invite you to run also this incremental comparison test:
```
# Generation for 3X
./gradlew :examples:large-generated-project:cleanIcons
./gradlew :examples:large-generated-project:cleanGeneratedSources
./gradlew :examples:large-generated-project:generateSources -PiconSourcesCount=6000 -PirrelevantSourcesCount=600
./gradlew :examples:large-generated-project:generateIcons

# Generation for incremental 3X
./gradlew :examples:large-generated-project:cleanIcons
./gradlew :examples:large-generated-project:cleanGeneratedSources
./gradlew :examples:large-generated-project:generateSources -PiconSourcesCount=2000 -PirrelevantSourcesCount=200
./gradlew :examples:large-generated-project:generateIcons
./gradlew :examples:large-generated-project:generateSources -PiconSourcesCount=2000 -PirrelevantSourcesCount=200
./gradlew :examples:large-generated-project:generateIcons
./gradlew :examples:large-generated-project:generateSources -PiconSourcesCount=2000 -PirrelevantSourcesCount=200
./gradlew :examples:large-generated-project:generateIcons
```

Compare the runtime of the `generateIcons` task of the "3X" vs. the last runtime of the "incremental 3X".
You should see that the "incremental 3X", while technically running on the same number of file during the last run,
only processed the new batched of X files, taking thus way less time than the "3X" run.

If the example is not large enough for your taste, we invite you to bump the number of files.
