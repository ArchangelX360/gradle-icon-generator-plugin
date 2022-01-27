# `large-generated-project`

`large-generated-project` is made for checking performance (scale, incrementality, parallelism) of the plugin, answering
questions such as:

- how long does it take for the plugin to process a large number of matched sources?
- is the plugin heavily impacted by the presence of a lot of non-matched files?
- how long does it take for the plugin to process X files, then X additional more, and again X additional files, and
  what about compared to processing 3X files in one go?

## What is provided in this project?

`large-generated-project` is not generated when the repository is cloned, you are required to run the source generation 
task.
`large-generated-project` is delivered with 2 additional gradle tasks:

- `generateSources` to generate sources for the plugin to run on, it has 2 properties
  - `-PiconSourcesCount` the number of sources generated that represents a Java source containing icons, they are
    matched by the icon generation task of the plugin
  - `-PirrelevantSourcesCount` the number of sources generated that represents other files, that are not matched by the
    icon generation task of the plugin, they are just noise
- `cleanGeneratedSources` to clean the all the generated sources, to do another test

## Run the example

First, clone the repository, if not already done:
```
git clone https://github.com/ArchangelX360/gradle-icon-generator-plugin.git
cd gradle-icon-generator-plugin
```

Second, go to the example directory:
```
cd examples/large-generated-project/
```

### Run 1: the `intellij-community`-like repository

Running the example reproducing an `intellij-community`-like repository:

```
# (optional) cleanup any previously generated sources and outputs
./gradlew cleanIcons
./gradlew cleanGeneratedSources

# generate the repository, IntelliJ IDEA community has 54 source files containing icons, and 231023 other files
./gradlew generateSources -PiconSourcesCount=54 -PirrelevantSourcesCount=231023

# Run the plugin's icon generation
./gradlew generateIcons
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
./gradlew cleanIcons
./gradlew cleanGeneratedSources

# generate the repository
./gradlew generateSources -PiconSourcesCount=2000 -PirrelevantSourcesCount=200

# Run the plugin's icon generation
./gradlew generateIcons   
```

#### Incremental comparison test

The `generateSources` task does not clean the generated sources between two runs, an explicit `cleanGeneratedSources` is
required to be run.
This is made on purpose for some additional tests to be run, for example, the incremental comparison test.

We invite you to run also this incremental comparison test:
```
# Generation for 3X
./gradlew cleanIcons
./gradlew cleanGeneratedSources
./gradlew generateSources -PiconSourcesCount=6000 -PirrelevantSourcesCount=600
./gradlew generateIcons

# Generation for incremental 3X
./gradlew cleanIcons
./gradlew cleanGeneratedSources
./gradlew generateSources -PiconSourcesCount=2000 -PirrelevantSourcesCount=200
./gradlew generateIcons
./gradlew generateSources -PiconSourcesCount=2000 -PirrelevantSourcesCount=200
./gradlew generateIcons
./gradlew generateSources -PiconSourcesCount=2000 -PirrelevantSourcesCount=200
./gradlew generateIcons
```

Compare the runtime of the `generateIcons` task of the "3X" vs. the last runtime of the "incremental 3X".
You should see that the "incremental 3X", while technically running on the same number of file during the last run,
only processed the new batched of X files, taking thus way less time than the "3X" run.

If the example is not large enough for your taste, we invite you to bump the number of files.
